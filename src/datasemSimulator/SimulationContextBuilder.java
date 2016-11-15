/*
This script reads XML model information from /SimulationScenarios/ExperimentModel.xml, create instances accordingly 
and implement them to the context, and set simulation parameters
 */
package datasemSimulator;

import java.io.File;
import java.util.HashMap;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import visualization.Visualization;
import workItems.AggregationNode;
import workItems.DevTask;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Asset;
import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.Impact;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.MechanismAttribute;
import xtext.objectsModel.ObjectsModelFactory;
import xtext.objectsModel.RequiredService;
import xtext.objectsModel.Service;
import xtext.objectsModel.ServiceProvider;
import xtext.objectsModel.ServiceProviderType;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;
import xtext.objectsModel.WorkItemType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kanbanBoard.KanbanBoard;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import processModels.ProcessModel;
import processModels.ValueFunction;


public class SimulationContextBuilder {
	public String ModelBuilder;
	// void HashMaps which would be filled by instances (with IDs) created from ExperimentModel.xml file
	public HashMap<Integer, ServiceProvider> myServiceProviders = new HashMap<Integer, ServiceProvider>();
	public HashMap<Integer, WorkItem> myWorkItems = new HashMap<Integer, WorkItem>();
	public HashMap<Integer, Asset> myResources = new HashMap<Integer, Asset>();	
	public HashMap<Integer, ServiceProviderType> myServiceProviderTypes = new HashMap<Integer, ServiceProviderType>();
	public HashMap<Integer, WorkItemType> myWorkItemTypes = new HashMap<Integer, WorkItemType>();
	public HashMap<Integer, Service> myServices = new HashMap<Integer, Service>();
	public HashMap<String, String> myExperimentParameters = new HashMap<String, String>();
	// Simulation Parameters
	public Parameters parameters;
	// Turn on/off animation
	public String Visualization;
	
	public SimulationContextBuilder(File scenarioXmlFile) {
		this.ReadXMLFile(scenarioXmlFile);
		System.out.println(this.myWorkItems.size()+" WIs");
		System.out.println(this.myServiceProviders.size()+" SPs");
		System.out.println(this.myResources.size()+" Resources");
	}
	
	// F- Context Implementation
	// This function works after XML is read and model instances are created.
	// It add the created instances to the context, and initialize the context to be ready for simulation
	public void ContextImplementation(Context<Object> context) {
		parameters = RunEnvironment.getInstance().getParameters();
		SystemOfSystems mySoS = BuildSoS(context);	
		context.setId("DatasemSimulator");		
		context.add(mySoS);
		// Add SPs to context
		for (ServiceProviderAgent sp: mySoS.myServiceProviderAgents.values()) {
			context.add(sp);	
			for (ResourceEntity r : sp.getMyResourceEntities()) {
				context.add(r);
			}
		}
		
		// P- Extended Service Capacities
		// This part calculates the Sum of skill levels of resources of all Service Providers which a Service Provider can DIRECTLY assign work to,
		// plus the skill levels of resources of the current Service Provider, prior to start of simulation
		// This is an important concept used in SP behaviors to evaluate the "actual" capacity of an SP
		for (ServiceProviderAgent sp: mySoS.myServiceProviderAgents.values()) {
			for (Service service:this.myServices.values()) {
				sp.ServiceCapacity.put(service, 0.0);
				sp.ExtendedServiceCapacity.put(service, 0.0);
			}
			for (ResourceEntity r : sp.getMyResourceEntities()) {
				for (Skill sk:r.getSkillSet()) {
					Service service = sk.getService();
					double sCapacity = sp.ServiceCapacity.get(service);
					sCapacity += sk.getEfficiency(); 
					sp.ServiceCapacity.put(service,sCapacity);
				}
			}
		}
		for (ServiceProviderAgent sp: mySoS.myServiceProviderAgents.values()) {
			for (Service service: mySoS.myServices.values()) {
				double exCapacity = 0;
				for (ServiceProviderAgent assignToSP : sp.assignWITo) {
					exCapacity += assignToSP.ServiceCapacity.get(service);
				}
				sp.ExtendedServiceCapacity.put(service, exCapacity);
			}
		}
		// note that: only direct AssignTo relations are counted in the calculation
		// P- end Extended Service Capacities
		
		// P- !! Initial Assignment
		// The initial work items are assigned to the FIRST SP described in the model
		for (WorkItemEntity wi:mySoS.myWorkItemEntities.values()) {		
			if ((wi.arrivalTime>0 )) {						
				wi.SoS.initialList.put(wi.getId(), wi);
			}
		}	
		mySoS.coordinator = mySoS.myServiceProviderAgents.get(1);
		// P- end Initial Assignment
		
		// P- Experiment and Visualization	
		// This part handles the window size and object locations in animation
		// according to size and complexity of the model
		int numReplications = (Integer)parameters.getValue("NumReplications");
		Visualization = (String)parameters.getValue("Visualization");
		if (Visualization.matches("on") && (numReplications==1)) {
			System.out.print("\ninitializing visualization...\n");		
			for (ServiceProviderType t : mySoS.myServiceProviderTypes.values()) {
				if (t.getHierarchy()> (mySoS.OrgLevels-1)) {
					mySoS.OrgLevels = t.getHierarchy()+1;
				}
			}
			for (WorkItemType t : mySoS.myWorkItemTypes.values()) {
				if (t.getHierarchy()> (mySoS.WINLevels-1)) {
					mySoS.WINLevels = t.getHierarchy()+1;
				}
			}
			if (this.ModelBuilder.matches("DevelopmentOrganizationModel")) {
				mySoS.OrgComplexity = 5;
				mySoS.WINComplexity = 3;
				mySoS.WINSize = 10;				
			}
			else if (this.ModelBuilder.matches("ThreeProgrammersThreeTasksModel")) {
				mySoS.OrgComplexity = 3;
				mySoS.WINComplexity = 4;
				mySoS.WINSize = 3;
			}
			else if (this.ModelBuilder.matches("AerospaceAndDefenceProjects")) {
				mySoS.OrgComplexity = 5;
				mySoS.WINComplexity = 4;
				mySoS.WINSize = 2;
			}
			else {
				mySoS.OrgComplexity = 3;
				mySoS.WINComplexity = 3;
				mySoS.WINSize = 2;
			}			
			new Visualization(context,mySoS);
			KanbanBoard myKanbanBoard = new KanbanBoard(mySoS);
			mySoS.myKanbanBoard = myKanbanBoard;
			context.add(myKanbanBoard);
			System.out.println("OrgLevels="+mySoS.OrgLevels);
			System.out.println("WINLevels="+mySoS.WINLevels);			
			mySoS.printSoSInformation();		
		}
		// P- end Experiment and Visualization	
	}
	// F- end Context Implementation

	
	// F - Read XML File
	// Read the XML Model document and build instances by calling various "xmlBuild..." functions
	public void ReadXMLFile(File scenarioXmlFile) {
		System.out.println("\nstart parsing scenario.xml...\n");
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dbFactory.setIgnoringElementContentWhitespace(true);
			Document scenario = dBuilder.parse(scenarioXmlFile);
			scenario.getDocumentElement().normalize();	
			//
			Node modelNode = scenario.getElementsByTagName("ExperimentModel").item(0);
			Element modelElement = (Element)modelNode;
			this.ModelBuilder = modelElement.getAttribute("name");
			System.out.println("Model Builder: "+this.ModelBuilder);
			//
			Node serviceProviderTypesNode = scenario.getElementsByTagName("ServiceProviderTypes").item(0);
			Element ServiceProviderTypesElement = (Element)serviceProviderTypesNode;
			NodeList ServiceProviderTypeList = ServiceProviderTypesElement.getElementsByTagName("ServiceProviderType");
			for (int i=0;i<ServiceProviderTypeList.getLength();i++) {
				Node n = ServiceProviderTypeList.item(i);
				Element e = (Element)n;
				xmlCreateServiceProviderType(e);
			}
			//
			Node workItemTypesNode = scenario.getElementsByTagName("WorkItemTypes").item(0);
			Element workItemTypesElement = (Element)workItemTypesNode;
			NodeList workItemTypeList = workItemTypesElement.getElementsByTagName("WorkItemType");
			for (int i=0;i<workItemTypeList.getLength();i++) {
				Node n = workItemTypeList.item(i);
				Element e = (Element)n;
				xmlCreateWorkItemType(e);
			}
			//
			Node servicesNode = scenario.getElementsByTagName("Services").item(0);
			Element servicesElement = (Element)servicesNode;
			NodeList serviceList = servicesElement.getElementsByTagName("Service");
			for (int i=0;i<serviceList.getLength();i++) {
				Node n = serviceList.item(i);
				Element e = (Element)n;
				xmlCreateService(e);
			}
			//
			Node experimentParametersNode = scenario.getElementsByTagName("ExperimentParameters").item(0);
			Element experimentParametersElement = (Element)experimentParametersNode;
			NodeList experimentParameterList = experimentParametersElement.getElementsByTagName("ExperimentParameter");
			for (int i=0;i<experimentParameterList.getLength();i++) {
				Node n = experimentParameterList.item(i);
				Element e = (Element)n;
				xmlImplementExperimentParameter(e);
			}
			//
			Node serviceProvidersNode = scenario.getElementsByTagName("ServiceProviders").item(0);
			Element serviceProvidersElement = (Element)serviceProvidersNode;
			NodeList serviceProviderList = serviceProvidersElement.getElementsByTagName("ServiceProvider");
			for (int i=0;i<serviceProviderList.getLength();i++) {
				Node n = serviceProviderList.item(i);
				Element e = (Element)n;
				xmlCreateServiceProvider(e);
			}
			//
			Node workItemsNode = scenario.getElementsByTagName("WorkItems").item(0);
			Element workItemsElement = (Element)workItemsNode;
			NodeList workItemList = workItemsElement.getElementsByTagName("WorkItem");		
			for (int i=0;i<workItemList.getLength();i++) {				
				Node n = workItemList.item(i);
				Element e = (Element)n;
				xmlCreateWorkItem(e);				
			}
			// ------------------------------------------------
			for (int i=0;i<serviceProviderList.getLength();i++) {
				Node node = serviceProviderList.item(i);
				Element e = (Element)node;
				xmlBuildServiceProviderTopology(e);
			}
			//
			for (int i=0;i<workItemList.getLength();i++) {
				Node node = workItemList.item(i);
				Element e = (Element)node;
				xmlBuildWorkItemTopology(e);	
			}
			// -------------------------------------------------
			}		
			catch (Exception e) {
				e.printStackTrace();
			}
		System.out.println("\nscenario parsing completed\n");
	}	
	// end F - Read XML File
	
	public void xmlCreateService(Element e) {
		int id = Integer.parseInt(e.getAttribute("serviceId"));
		String name = e.getAttribute("name");
		Service myService = ObjectsModelFactory.eINSTANCE.createService();
		myService.setId(id);
		myService.setName(name);
		myServices.put(id, myService); 
	}	
	public void xmlImplementExperimentParameter(Element e) {
		String name = e.getAttribute("name");
		String value = e.getAttribute("value");
		System.out.println(name+"= "+value);
		this.myExperimentParameters.put(name,value);
	}
	public void xmlCreateServiceProviderType(Element e) {
		int id = Integer.parseInt(e.getAttribute("spTypeId"));
		String name = e.getAttribute("name");
		int hierarchy = Integer.parseInt(e.getAttribute("hierarchy"));
		ServiceProviderType myServiceProviderType = ObjectsModelFactory.eINSTANCE.createServiceProviderType();
		myServiceProviderType.setId(id);
		myServiceProviderType.setName(name);
		myServiceProviderType.setHierarchy(hierarchy);
		myServiceProviderTypes.put(id, myServiceProviderType); 
	}
	public void xmlCreateWorkItemType(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiTypeId"));
		String name = e.getAttribute("name");		
		int hierarchy = Integer.parseInt(e.getAttribute("hierarchy"));
		WorkItemType myWorkItemType = ObjectsModelFactory.eINSTANCE.createWorkItemType();
		myWorkItemType.setId(id);
		myWorkItemType.setName(name);
		myWorkItemType.setHierarchy(hierarchy);
		Node mechanisms_node = e.getElementsByTagName("Mechanisms").item(0);
		Element mechanisms_element = (Element)mechanisms_node;
		NodeList mechanism_nodeList = mechanisms_element.getElementsByTagName("Mechanism");
		for (int i=0;i<mechanism_nodeList.getLength();i++) {
			// Mechanism
			Mechanism myMechanism = ObjectsModelFactory.eINSTANCE.createMechanism();
			Node mechanism_node = mechanism_nodeList.item(i);
			Element mechanism_element = (Element)mechanism_node;
			String mechanism_name = mechanism_element.getAttribute("name");
			String mechanism_value = mechanism_element.getAttribute("value");	
			myMechanism.setName(mechanism_name);
			myMechanism.setValue(mechanism_value);
			NodeList attribute_nodeList = mechanism_element.getElementsByTagName("Attribute");
			for (int i1=0;i1<attribute_nodeList.getLength();i1++) {
				// Mechanism Attribute
				MechanismAttribute myAttribute = ObjectsModelFactory.eINSTANCE.createMechanismAttribute();
				Node attribute_node = attribute_nodeList.item(i1);
				Element attribute_element = (Element)attribute_node;
				String attribute_name = attribute_element.getAttribute("name");//System.out.println(attribute_name);
				String attribute_value = attribute_element.getAttribute("value");//System.out.println(attribute_value);
				myAttribute.setAttribute(attribute_name);
				myAttribute.setValue(attribute_value);
				myMechanism.getAttributes().add(myAttribute);
				myWorkItemType.getMechanisms().add(myMechanism);
			}		
		}
		myWorkItemTypes.put(id, myWorkItemType);
	}
	public void xmlCreateServiceProvider(Element e) {
		ServiceProvider myServiceProvider = ObjectsModelFactory.eINSTANCE.createServiceProvider();
		int id = Integer.parseInt(e.getAttribute("serviceProviderId"));
		String name = e.getAttribute("name");	
		int spId = Integer.parseInt(e.getAttribute("typeId"));
		myServiceProvider.setId(id);
		myServiceProvider.setName(name);	
		myServiceProvider.setType(myServiceProviderTypes.get(spId));
		
		// GovernanceStrategy
		Node stg_node = e.getElementsByTagName("GovernanceStrategy").item(0);
		Element stg_element = (Element)stg_node;
		String stgType = stg_element.getAttribute("type");
		GovernanceStrategy myGovernanceStrategy = ObjectsModelFactory.eINSTANCE.createGovernanceStrategy(); 		
		myGovernanceStrategy.setType(stgType);
		Node mechanisms_node = e.getElementsByTagName("Mechanisms").item(0);
		Element mechanisms_element = (Element)mechanisms_node;
		NodeList mechanism_nodeList = mechanisms_element.getElementsByTagName("Mechanism");
		for (int i=0;i<mechanism_nodeList.getLength();i++) {
			// Mechanism
			Mechanism myMechanism = ObjectsModelFactory.eINSTANCE.createMechanism();
			Node mechanism_node = mechanism_nodeList.item(i);
			Element mechanism_element = (Element)mechanism_node;
			String mechanism_name = mechanism_element.getAttribute("name");
			String mechanism_value = mechanism_element.getAttribute("value");	
			myMechanism.setName(mechanism_name);
			myMechanism.setValue(mechanism_value);
			NodeList attribute_nodeList = mechanism_element.getElementsByTagName("Attribute");
			for (int i1=0;i1<attribute_nodeList.getLength();i1++) {
				// Mechanism Attribute
				MechanismAttribute myAttribute = ObjectsModelFactory.eINSTANCE.createMechanismAttribute();
				Node attribute_node = attribute_nodeList.item(i1);
				Element attribute_element = (Element)attribute_node;
				String attribute_name = attribute_element.getAttribute("name");
				String attribute_value = attribute_element.getAttribute("value");
				myAttribute.setAttribute(attribute_name);
				myAttribute.setValue(attribute_value);
				myMechanism.getAttributes().add(myAttribute);
			}
			myGovernanceStrategy.getMechanisms().add(myMechanism);			
		}
		
		myServiceProvider.setGovernanceStrategy(myGovernanceStrategy);
		myServiceProviders.put(id, myServiceProvider);
		// Resources
		Node resources_node = e.getElementsByTagName("Resources").item(0);
		Element resources_element = (Element)resources_node;
		NodeList resource_nodeList = resources_element.getElementsByTagName("Resource");
		for (int i1=0;i1<resource_nodeList.getLength();i1++) {
			Node resource_node = resource_nodeList.item(i1);
			Element resource_element= (Element)resource_node;
			int resource_id = Integer.parseInt(resource_element.getAttribute("resourceId")); 
			String resource_name = resource_element.getAttribute("name");
			Asset myResource = ObjectsModelFactory.eINSTANCE.createAsset();
			myResource.setId(resource_id);
			myResource.setName(resource_name);	
			// SkillSet
			Node skillset_node = resource_element.getElementsByTagName("SkillSet").item(0); 
			Element skillset_element = (Element)skillset_node;
			NodeList skills_nodeList = skillset_element.getElementsByTagName("Skill"); 
			for (int i2=0;i2<skills_nodeList.getLength();i2++) {
				Node skill_node = skills_nodeList.item(i2);
				Element skill_element= (Element)skill_node;
				int skill_serviceid = Integer.parseInt(skill_element.getAttribute("serviceId")); 
				double skill_efficiency = Double.parseDouble(skill_element.getAttribute("efficiency")); 
				Skill mySkill = ObjectsModelFactory.eINSTANCE.createSkill();
				mySkill.setService(myServices.get(skill_serviceid));
				mySkill.setEfficiency(skill_efficiency);
				myResource.getSkillSet().add(mySkill);
			}
			myServiceProvider.getResources().add(myResource);
			myResources.put(resource_id, myResource);	
			myServiceProvider.getResources().add(myResource);
		}
	}
	public void xmlCreateWorkItem(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiId"));
		String name = e.getAttribute("name");
		int typeId = Integer.parseInt(e.getAttribute("typeId")); 				
		WorkItem myWorkItem = ObjectsModelFactory.eINSTANCE.createWorkItem();
		myWorkItem.setId(id);
		myWorkItem.setName(name);
		myWorkItem.setType(myWorkItemTypes.get(typeId));
		// RequiredAnalysis
		Node requiredAnalysis_node = e.getElementsByTagName("RequiredAnalysis").item(0);
		Element requiredAnalysis_element = (Element)requiredAnalysis_node;
		NodeList requiredAnalysis_nodeList = requiredAnalysis_element.getElementsByTagName("RequiredService");
		for (int i1=0;i1<requiredAnalysis_nodeList.getLength();i1++) {
			Node node1 = requiredAnalysis_nodeList.item(i1);
			Element e1= (Element)node1;
			int service_id = Integer.parseInt(e1.getAttribute("serviceId")); 
			double efforts = Double.parseDouble(e1.getAttribute("efforts")); 
			RequiredService myRequiredService = ObjectsModelFactory.eINSTANCE.createRequiredService();
			myRequiredService.setServiceType(myServices.get(service_id));
			myRequiredService.setEfforts(efforts);
			myWorkItem.getRequiredAnalysis().add(myRequiredService);
		}
		// RequiredServices
		Node requiredServices_node = e.getElementsByTagName("RequiredServices").item(0);
		Element requiredServices_element = (Element)requiredServices_node;
		NodeList requiredServices_nodeList = requiredServices_element.getElementsByTagName("RequiredService");
		for (int i1=0;i1<requiredServices_nodeList.getLength();i1++) {
			Node node1 = requiredServices_nodeList.item(i1);
			Element e1= (Element)node1;
			int service_id = Integer.parseInt(e1.getAttribute("serviceId")); 
			double efforts = Double.parseDouble(e1.getAttribute("efforts")); 
			RequiredService myRequiredService = ObjectsModelFactory.eINSTANCE.createRequiredService();
			myRequiredService.setServiceType(myServices.get(service_id));
			myRequiredService.setEfforts(efforts);
			myWorkItem.getRequiredServices().add(myRequiredService);
		}
		// GovernanceAttributes
		Node governanceAttributes_node = e.getElementsByTagName("GovernanceAttributes").item(0);
		Element governanceAttributes_element = (Element)governanceAttributes_node;
		NodeList governanceAttribute_nodeList = governanceAttributes_element.getElementsByTagName("GovernanceAttribute");
		for (int i1=0;i1<governanceAttribute_nodeList.getLength();i1++) {
			Node node1 = governanceAttribute_nodeList.item(i1);
			Element e1= (Element)node1;
			String att_name = e1.getAttribute("name");
			if (att_name.matches("Value")) {				
				double att_value = Double.parseDouble(e1.getAttribute("value")); 
				myWorkItem.setValue(att_value);
			}
			else if (att_name.matches("ArrivalTime")) {				
				int att_value = Integer.parseInt(e1.getAttribute("value")); 
				myWorkItem.setArrivalTime(att_value);
			}
			else if (att_name.matches("DueDate")) {				
				int att_value = Integer.parseInt(e1.getAttribute("value")); 
				myWorkItem.setDueDate(att_value);
			}
		}
		//System.out.println(myWorkItem.getName()+" value:"+myWorkItem.getValue()+" arr:"+myWorkItem.getArrivalTime()+" due:"+myWorkItem.getDueDate());
		myWorkItems.put(id, myWorkItem);
	}
	public void xmlBuildServiceProviderTopology(Element e) {
		// ID reference
		int id = Integer.parseInt(e.getAttribute("serviceProviderId"));
		ServiceProvider myServiceProvider = this.myServiceProviders.get(id); 
		// AssignWITO
		Node AssignWITo_node = e.getElementsByTagName("AssignWITo").item(0);
		Element AssignWITo_element = (Element)AssignWITo_node;
		NodeList AssignWITo_nodeList = AssignWITo_element.getElementsByTagName("serviceProviderId");
		for (int i=0;i<AssignWITo_nodeList.getLength();i++) {
			Node node1 = AssignWITo_nodeList.item(i);
			Element e1= (Element)node1;
			int AssignWITo_id = Integer.parseInt(e1.getTextContent());
			myServiceProvider.getAssignTo().add(myServiceProviders.get(AssignWITo_id));
		}	
		// BorrowResourceFrom
		Node BorrowResourceFrom_node = e.getElementsByTagName("BorrowResourceFrom").item(0);
		Element BorrowResourceFrom_element = (Element)BorrowResourceFrom_node;
		NodeList BorrowResourceFrom_nodeList = BorrowResourceFrom_element.getElementsByTagName("serviceProviderId");
		for (int i=0;i<BorrowResourceFrom_nodeList.getLength();i++) {
			Node node1 = BorrowResourceFrom_nodeList.item(i);
			Element e1 = (Element)node1;
			int BorrowResourceFrom_id = Integer.parseInt(e1.getTextContent());
			myServiceProvider.getOutsourceFrom().add(myServiceProviders.get(BorrowResourceFrom_id));
		}
		
	}
	public void xmlBuildWorkItemTopology(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiId"));
		WorkItem myWorkItem = this.myWorkItems.get(id);
		// Hierarchy
		boolean isAggregationNode = Boolean.parseBoolean(e.getAttribute("isAggregationNode")); 
		myWorkItem.setIsAggregationNode(isAggregationNode); 
		if (myWorkItem.isIsAggregationNode()) {
			Node Subtasks_node = e.getElementsByTagName("Subtasks").item(0);
			Element Subtasks_element = (Element)Subtasks_node;
			NodeList Subtasks_nodeList = Subtasks_element.getElementsByTagName("workItemId");
			for (int i=0;i<Subtasks_nodeList.getLength();i++) {
				Node node1 = Subtasks_nodeList.item(i);
				Element e1= (Element)node1;
				int Subtask_id = Integer.parseInt(e1.getTextContent());
				myWorkItem.getSbTasks().add(myWorkItems.get(Subtask_id));
			}
		}
		// Precedency
		boolean hasPredecessors = Boolean.parseBoolean(e.getAttribute("hasPredecessors"));
		myWorkItem.setHasPredecessors(hasPredecessors);
		if (myWorkItem.isHasPredecessors()) {
			Node Predecessors_node = e.getElementsByTagName("Predecessors").item(0);
			Element Predecessors_element = (Element)Predecessors_node;
			NodeList Predecessor_nodeList = Predecessors_element.getElementsByTagName("workItemId");
			for (int i=0;i<Predecessor_nodeList.getLength();i++) {
				Node node1 = Predecessor_nodeList.item(i);
				Element e1= (Element)node1;
				int Predecessor_id = Integer.parseInt(e1.getTextContent());
				myWorkItem.getPdTasks().add(myWorkItems.get(Predecessor_id));
			}
		}
		// Impacts
		boolean hasImpacts = Boolean.parseBoolean(e.getAttribute("hasImpacts"));
		myWorkItem.setHasImpacts(hasImpacts);
		if (myWorkItem.isHasImpacts()) {
			Node Impacts_node = e.getElementsByTagName("Impacts").item(0);
			Element Impacts_element = (Element)Impacts_node;
			NodeList Impact_nodeList = Impacts_element.getElementsByTagName("Impact");			
			for (int i=0;i<Impact_nodeList.getLength();i++) {
				Node node1 = Impact_nodeList.item(i);
				Element e1= (Element)node1;
				int impactWI_id = Integer.parseInt(e1.getAttribute("workItemId"));
				double likelihood = Double.parseDouble(e1.getAttribute("likelihood"));
				double risk = Double.parseDouble(e1.getAttribute("risk"));
				Impact myImpact = ObjectsModelFactory.eINSTANCE.createImpact();
				myImpact.setImpactWI(myWorkItems.get(impactWI_id));
				myImpact.setLikelihood(likelihood);
				myImpact.setRisk(risk);
				myWorkItem.getImpacts().add(myImpact);
			}
		}
	}
	
	// F - Build SoS
	// Creat an SoS(System-of-System) instance and add all instances created to it
	// Read Simulation Parameters
	public SystemOfSystems BuildSoS(Context<Object> context) {		
		SystemOfSystems mySoS = new SystemOfSystems(context);
		mySoS.parameters = this.parameters;		
		int TaskMaturityLevels = (Integer)mySoS.parameters.getValue("TaskMaturityLevels");
		double TaskUncertainty = (Double)mySoS.parameters.getValue("TaskUncertainty");
		double ReworkRisk = (Double)mySoS.parameters.getValue("ReworkRisk");
		double ChangePropagationFactor = (Double)mySoS.parameters.getValue("ChangePropagationFactor");
		double ROR = (Double)mySoS.parameters.getValue("ROR");
		double MultiTaskingPenalty = (Double)mySoS.parameters.getValue("MultiTaskingPenalty");
		double LearningFactor = (Double)mySoS.parameters.getValue("LearningFactor");
		double VolatilityLevel = ((double)(Integer)mySoS.parameters.getValue("Volatility"))/2;
		
		TaskMaturityLevels = Integer.valueOf((this.myExperimentParameters.get("TaskMaturityLevels")));
		TaskUncertainty = Double.valueOf((this.myExperimentParameters.get("TaskUncertainty")));
		ReworkRisk = Double.valueOf((this.myExperimentParameters.get("ReworkRisk")));
		ChangePropagationFactor = Double.valueOf((this.myExperimentParameters.get("ChangePropagationFactor")));
		ROR = Double.valueOf((this.myExperimentParameters.get("RateOfReturn")));
		MultiTaskingPenalty = Double.valueOf((this.myExperimentParameters.get("MultiTaskingPenalty")));
		LearningFactor = Double.valueOf((this.myExperimentParameters.get("LearningFactor")));
		//VolatilityLevel = Double.valueOf((this.myExperimentParameters.get("Volatility"))) /2;
		
		mySoS.TaskMaturityLevels = TaskMaturityLevels;
		mySoS.TaskUncertainty = TaskUncertainty*(VolatilityLevel);
		mySoS.ReworkRisk = ReworkRisk*(VolatilityLevel);
		mySoS.MultiTaskingPenalty = MultiTaskingPenalty;
		mySoS.LearningFactor = LearningFactor;
		mySoS.ROR = ROR;
		
		mySoS.myServices = this.myServices;
		mySoS.myServiceProviderTypes = this.myServiceProviderTypes;		
		mySoS.myWorkItemTypes = this.myWorkItemTypes;
			
		for (Asset r: myResources.values()) {
			int id = r.getId();
			ResourceEntity res = new ResourceEntity(r);
			mySoS.myResourceEntities.put(id, res);
		}		
		for (ServiceProvider sp: myServiceProviders.values()) {
			int id = sp.getId();
			ServiceProviderAgent agent = new ServiceProviderAgent(sp);		
			mySoS.myServiceProviderAgents.put(id, agent);
		}
		for (ServiceProviderAgent agent: mySoS.myServiceProviderAgents.values()) {
			int id = agent.getId();
			ServiceProvider sp = myServiceProviders.get(id);
			agent.SoS = mySoS;
			for (Asset r: sp.getResources()) {
				agent.getMyResourceEntities().add(mySoS.myResourceEntities.get(r.getId()));
			}
			for (ServiceProvider sp1: sp.getAssignTo()) {
				agent.assignWITo.add(mySoS.myServiceProviderAgents.get(sp1.getId()));
			}
		}				
		
		for (WorkItem wi: myWorkItems.values()) {
			int id = wi.getId();
			WorkItemEntity entity;
			if (wi.isIsAggregationNode()) {
				entity = new AggregationNode(wi);
			}
			else {
				entity = new Task(wi);
				entity = new DevTask(entity);
			}
			entity.SoS = mySoS;
			entity.maxMaturityLevels = TaskMaturityLevels;
			entity.uncertainty = TaskUncertainty;
			entity.risk = ReworkRisk;
			mySoS.myWorkItemEntities.put(id, entity);
			mySoS.increaseWICount();
		}
		for (WorkItemEntity entity: mySoS.myWorkItemEntities.values()) {
			int id = entity.getId();
			WorkItem wi = myWorkItems.get(id);
			if (wi.isIsAggregationNode()) {
				for (WorkItem st: wi.getSbTasks()) {
					((AggregationNode)entity).getSubtasks().add(mySoS.myWorkItemEntities.get(st.getId()));
				}
			}
			if (wi.isHasPredecessors()) {
				for (WorkItem pt: wi.getPdTasks()) {
					int pt_id = pt.getId();
					entity.getPredecessors().add(mySoS.myWorkItemEntities.get(pt_id));
					mySoS.myWorkItemEntities.get(pt_id).getSuccessors().add(entity);
				}
			}
			if (wi.isHasImpacts()) {
				for (Impact myImpact: wi.getImpacts()) {
					int impactWI_id = myImpact.getImpactWI().getId();
					double likelihood = myImpact.getLikelihood();
					double risk = myImpact.getRisk();
					WorkItemEntity impactEntity = mySoS.myWorkItemEntities.get(impactWI_id);
					entity.getImpactsWIs().add(impactEntity);
					entity.getImpactsRisk().put(impactEntity, risk*(VolatilityLevel));
					entity.getImpactsLikelihood().put(impactEntity, ChangePropagationFactor*likelihood);
				}
			}
		}
		return mySoS;
	}
	// end F - Build SoS
}
