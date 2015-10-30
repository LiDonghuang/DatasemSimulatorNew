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
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;
import xtext.objectsModel.WorkItemType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kanbanBoard.KanbanBoard;
import kanbanBoard.KanbanElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SimulationContextBuilder {
	public HashMap<Integer, ServiceProvider> myServiceProviders = new HashMap<Integer, ServiceProvider>();
	public HashMap<Integer, WorkItem> myWorkItems = new HashMap<Integer, WorkItem>();
	public HashMap<Integer, Asset> myResources = new HashMap<Integer, Asset>();	
	public HashMap<Integer, WorkItemType> myWorkItemTypes = new HashMap<Integer, WorkItemType>();
	public HashMap<Integer, Service> myServices = new HashMap<Integer, Service>();
	Parameters parameters;
	public boolean VisualizationOn = true;
	
	public SimulationContextBuilder(File scenarioXmlFile) {
		this.ReadXMLFile(scenarioXmlFile);
		System.out.println(this.myWorkItems.size()+" WIs");
		System.out.println(this.myServiceProviders.size()+" SPs");
		System.out.println(this.myResources.size()+" Resources");
	}
	public void ContextImplementation(Context<Object> context) {
		parameters = RunEnvironment.getInstance().getParameters();
		SystemOfSystems mySoS = BuildSoS();			
		context.setId("DatasemSimulator");		
		context.add(mySoS);
		for (ServiceProviderAgent sp: mySoS.myServiceProviderAgents.values()) {
			context.add(sp);
			for (ResourceEntity r : sp.getMyResourceEntities()) {
				context.add(r);
			}
		}
		for (WorkItemEntity wi:mySoS.myWorkItemEntities.values()) {
			if ((wi.hierarchy==0 )) {	
				wi.SoS.waitingList.put(wi.getId(), wi);
				context.add(wi);
				wi.SoS.arrivedList.put(wi.getId(), wi);
				wi.isActivated = true;
				wi.activatedTime = 1;
				mySoS.myServiceProviderAgents.get(1).getRequestedQ().add(wi);
				// temp
				wi.setRequester(mySoS.myServiceProviderAgents.get(1));
				wi.setAssignedAgent(mySoS.myServiceProviderAgents.get(1));
				//
			}
			else if (wi.getType().getName().matches("SubSysReq")) {
				wi = (AggregationNode)wi;
				int c = 0;
				for (RequiredService reqSev : wi.getRequiredServices()) {
					c++;
					String name = wi.getName()+'.'+c;
					int serviceId = reqSev.getServiceType().getId();
					double efforts = reqSev.getEfforts();					
					int currentId = mySoS.getWICount();
					int st_id = currentId+1;
					mySoS.increaseWICount();
					DevTask devTask = new DevTask((AggregationNode)wi, st_id, name, serviceId, efforts);
					System.out.println(devTask);
				}
			}
		}		
		int numReplications = (Integer)parameters.getValue("NumReplications");
		if (VisualizationOn && (numReplications==1)) {
			System.out.print("initializing visualization...\n");
			new Visualization(context,mySoS);
			mySoS.printSoSInformation();		
		}
		KanbanBoard myKanbanBoard = new KanbanBoard(mySoS);
		mySoS.myKanbanBoard = myKanbanBoard;
		context.add(myKanbanBoard);
		for (KanbanElement element:myKanbanBoard.KanbanElements) {
			context.add(element);
		}
	}

	public void ReadXMLFile(File scenarioXmlFile) {
		System.out.println("\nstart parsing scenario.xml...\n");
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dbFactory.setIgnoringElementContentWhitespace(true);
			Document scenario = dBuilder.parse(scenarioXmlFile);
			scenario.getDocumentElement().normalize();	
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
	public void xmlCreateService(Element e) {
		int id = Integer.parseInt(e.getAttribute("serviceId"));
		String name = e.getAttribute("name");
		Service myService = ObjectsModelFactory.eINSTANCE.createService();
		myService.setId(id);
		myService.setName(name);
		myServices.put(id, myService); 
	}	
	public void xmlCreateWorkItemType(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiTypeId"));
		String name = e.getAttribute("name");
		int hierarchy = Integer.parseInt(e.getAttribute("hierarchy"));
		WorkItemType myWorkItemType = ObjectsModelFactory.eINSTANCE.createWorkItemType();
		myWorkItemType.setId(id);
		myWorkItemType.setName(name);
		myWorkItemType.setHierarchy(hierarchy);
		myWorkItemTypes.put(id, myWorkItemType); 
	}
	public void xmlCreateServiceProvider(Element e) {
		ServiceProvider myServiceProvider = ObjectsModelFactory.eINSTANCE.createServiceProvider();
		int id = Integer.parseInt(e.getAttribute("serviceProviderId"));
		String name = e.getAttribute("name");			
		myServiceProvider.setId(id);
		myServiceProvider.setName(name);								
		// GovernanceStrategy
		GovernanceStrategy myGovernanceStrategy = ObjectsModelFactory.eINSTANCE.createGovernanceStrategy(); 
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
		}
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
		myWorkItem.setIsAggregationNode(isAggregationNode); //System.out.println("\n"+myWorkItemEntity.getName()+" (id: "+id+")"+" Type: "+myWorkItemEntity.getType().getName());
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
	public SystemOfSystems BuildSoS() {
		Parameters p = parameters;
		int TaskMaturityLevels = (Integer)p.getValue("TaskMaturityLevels");
		double TaskUncertainty = (Double)p.getValue("TaskUncertainty");
		double TaskRisk = (Double)p.getValue("TaskRisk");
		double TaskChangePropagationUncertainty = (Double)p.getValue("TaskChangePropagationUncertainty");
		double TaskChangePropagationRisk = (Double)p.getValue("TaskChangePropagationRisk");
		
		SystemOfSystems mySoS = new SystemOfSystems();
		
		mySoS.myServices = myServices;
		mySoS.myWorkItemTypes = myWorkItemTypes;
				
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
			for (ServiceProvider sp2: sp.getOutsourceFrom()) {
				agent.borrowResourceFrom.add(mySoS.myServiceProviderAgents.get(sp2.getId()));
			}
		}				
		for (WorkItem wi: myWorkItems.values()) {
			int id = wi.getId();
			WorkItemEntity entity;
			if (wi.isIsAggregationNode()) {
				entity = new AggregationNode(wi);
			}
			else if (wi.getType().getName().matches("SubSysReq")) {
				entity = new AggregationNode(wi);
			}
			else {
				entity = new Task(wi);
				entity = new DevTask(entity);
			}
			entity.SoS = mySoS;
			entity.maxMaturityLevels = TaskMaturityLevels;
			entity.uncertainty = TaskUncertainty*TaskChangePropagationUncertainty;
			entity.risk = TaskRisk*TaskChangePropagationRisk;
			mySoS.myWorkItemEntities.put(id, entity);
			mySoS.increaseWICount();
		}
		for (WorkItemEntity entity: mySoS.myWorkItemEntities.values()) {
			int id = entity.getId();
			WorkItem wi = myWorkItems.get(id);
			for (WorkItem st: wi.getSbTasks()) {
				int st_id = st.getId();
				((AggregationNode)entity).getSubtasks().add(mySoS.myWorkItemEntities.get(st.getId()));
				mySoS.myWorkItemEntities.get(st_id).getUppertasks().add((AggregationNode)entity);
			}
			for (WorkItem pt: wi.getPdTasks()) {
				int pt_id = pt.getId();
				entity.getPredecessors().add(mySoS.myWorkItemEntities.get(pt_id));
				mySoS.myWorkItemEntities.get(pt_id).getSuccessors().add(entity);
			}
			for (Impact myImpact: wi.getImpacts()) {
				int impactWI_id = myImpact.getImpactWI().getId();
				double likelihood = myImpact.getLikelihood();
				double risk = myImpact.getRisk();
				WorkItemEntity impactEntity = mySoS.myWorkItemEntities.get(impactWI_id);
				entity.getImpactsWIs().add(impactEntity);
				entity.getImpactsValue().put(impactEntity, risk*TaskChangePropagationRisk);
				entity.getImpactsLikelihood().put(impactEntity, likelihood*TaskChangePropagationUncertainty);
			}
		}
		return mySoS;
	}
}
