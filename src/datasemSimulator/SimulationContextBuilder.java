package datasemSimulator;

import java.io.File;
import java.io.Console;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.grid.Grid;
import xtext.objectsModel.Asset;
import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.MechanismAttribute;
import xtext.objectsModel.ObjectsModelFactory;
import xtext.objectsModel.Service;
import xtext.objectsModel.ServiceProvider;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;
import xtext.objectsModel.WorkItemType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SimulationContextBuilder {
	public HashMap<Integer, ServiceProviderAgent> myServiceProviderAgents = new HashMap<Integer, ServiceProviderAgent>();
	public HashMap<Integer, WorkItemEntity> myWorkItemEntities = new HashMap<Integer, WorkItemEntity>();
	public HashMap<Integer, ResourceEntity> myResourceEntities = new HashMap<Integer, ResourceEntity>();
	
	public HashMap<Integer, WorkItemType> myWorkItemTypes = new HashMap<Integer, WorkItemType>();
	public HashMap<Integer, Service> myServices = new HashMap<Integer, Service>();

	

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
				xmlCreateServiceProviderAgent(e);
			}
			//
			Node workItemsNode = scenario.getElementsByTagName("WorkItems").item(0);
			Element workItemsElement = (Element)workItemsNode;
			NodeList workItemList = workItemsElement.getElementsByTagName("WorkItem");		
			for (int i=0;i<workItemList.getLength();i++) {				
				Node n = workItemList.item(i);
				Element e = (Element)n;
				xmlCreateWorkItemEntity(e);				
			}
			// ------------------------------------------------
			for (int i=0;i<serviceProviderList.getLength();i++) {
				Node node = serviceProviderList.item(i);
				Element e = (Element)node;
				xmlBuildServiceProviderAgentTopology(e);
			}
			//
			for (int i=0;i<workItemList.getLength();i++) {
				Node node = workItemList.item(i);
				Element e = (Element)node;
				xmlBuildWorkItemEntityTopology(e);	
			}
			// -------------------------------------------------
			}		
			catch (Exception e) {
				e.printStackTrace();
			}
		System.out.println("\nscenario parsing completed");
	}
	
	public void ContextImplementation(Context<Object> context) {
		SystemOfSystems mySoS = BuildSoS();
		mySoS.getSoSInformation();
		context.add(mySoS);
		for (ServiceProviderAgent sp: myServiceProviderAgents.values()) {
			sp.SoS = mySoS;
			context.add(sp);
		}
		context.addAll(myWorkItemEntities.values());
		for (WorkItemEntity wi:myWorkItemEntities.values()) {
			wi.SoS = mySoS;
			if (wi.getType().getId()==1 | wi.getType().getId()==2) {
				context.add(wi);
				wi.SoS.waitingList.put(wi.getId(), wi);
				wi.isActivated = true;
				wi.activatedTime = 1;
				myServiceProviderAgents.get(1).requestedQ.add(wi);
				wi.requester = myServiceProviderAgents.get(1);
				wi.assignedAgent = myServiceProviderAgents.get(1);
			}
		}
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
	public void xmlCreateServiceProviderAgent(Element e) {
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
		// Create Runtime Extend
		ServiceProviderAgent myServiceProviderAgent = new ServiceProviderAgent(myServiceProvider);
		myServiceProviderAgents.put(id, myServiceProviderAgent);
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
			// Create Runtime Extend
			ResourceEntity myResourceEntity = new ResourceEntity(myResource);
			myResourceEntities.put(resource_id, myResourceEntity);	
			myServiceProviderAgent.myResourceEntities.add(myResourceEntity);
		}
	}
	public void xmlCreateWorkItemEntity(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiId"));
		String name = e.getAttribute("name");
		double efforts = Double.parseDouble(e.getAttribute("efforts"));
		double value = Double.parseDouble(e.getAttribute("value"));
		int typeId = Integer.parseInt(e.getAttribute("typeId")); 				
		WorkItem myWorkItem = ObjectsModelFactory.eINSTANCE.createWorkItem();
		myWorkItem.setId(id);
		myWorkItem.setName(name);
		myWorkItem.setEfforts(efforts);
		myWorkItem.setValue(value);
		myWorkItem.setType(myWorkItemTypes.get(typeId));
		// RequiredServices
		Node requiredServices_node = e.getElementsByTagName("RequiredServices").item(0);
		Element requiredServices_element = (Element)requiredServices_node;
		NodeList requiredServices_nodeList = requiredServices_element.getElementsByTagName("serviceId");
		for (int i1=0;i1<requiredServices_nodeList.getLength();i1++) {
			Node node1 = requiredServices_nodeList.item(i1);
			Element e1= (Element)node1;
			int service_id = Integer.parseInt(e1.getTextContent()); 
			myWorkItem.getRequiredServices().add(myServices.get(service_id));
		}
		// Create Runtime Extend
		WorkItemEntity myWorkItemEntity = new WorkItemEntity(myWorkItem);
		myWorkItemEntities.put(id, myWorkItemEntity);
	}
	public void xmlBuildServiceProviderAgentTopology(Element e) {
		// ID reference
		int id = Integer.parseInt(e.getAttribute("serviceProviderId"));
		ServiceProviderAgent myServiceProviderAgent = this.myServiceProviderAgents.get(id); //System.out.println("\n"+myServiceProviderAgent.getName());
		// AssignWITO
		Node AssignWITo_node = e.getElementsByTagName("AssignWITo").item(0);
		Element AssignWITo_element = (Element)AssignWITo_node;
		NodeList AssignWITo_nodeList = AssignWITo_element.getElementsByTagName("serviceProviderId");
		for (int i=0;i<AssignWITo_nodeList.getLength();i++) {
			Node node1 = AssignWITo_nodeList.item(i);
			Element e1= (Element)node1;
			int AssignWITo_id = Integer.parseInt(e1.getTextContent());
			myServiceProviderAgent.assignWITo.add(myServiceProviderAgents.get(AssignWITo_id)); //System.out.println("AssignTo "+myServiceProviderAgents.get(AssignWITo_id).getName()+" spId: "+myServiceProviderAgents.get(AssignWITo_id).getId());
		}	
		// BorrowResourceFrom
		Node BorrowResourceFrom_node = e.getElementsByTagName("BorrowResourceFrom").item(0);
		Element BorrowResourceFrom_element = (Element)BorrowResourceFrom_node;
		NodeList BorrowResourceFrom_nodeList = BorrowResourceFrom_element.getElementsByTagName("serviceProviderId");
		for (int i=0;i<BorrowResourceFrom_nodeList.getLength();i++) {
			Node node1 = BorrowResourceFrom_nodeList.item(i);
			Element e1 = (Element)node1;
			int BorrowResourceFrom_id = Integer.parseInt(e1.getTextContent());
			myServiceProviderAgent.borrowResourceFrom.add(myServiceProviderAgents.get(BorrowResourceFrom_id)); //System.out.println("OutsourceFrom "+myServiceProviderAgents.get(BorrowResourceFrom_id).getName()+" spId: "+myServiceProviderAgents.get(BorrowResourceFrom_id).getId());
		}
		
	}
	public void xmlBuildWorkItemEntityTopology(Element e) {
		int id = Integer.parseInt(e.getAttribute("wiId"));
		WorkItemEntity myWorkItemEntity = this.myWorkItemEntities.get(id);
		// Hierarchy
		boolean isAggregationNode = Boolean.parseBoolean(e.getAttribute("isAggregationNode")); 
		myWorkItemEntity.setIsAggregationNode(isAggregationNode); //System.out.println("\n"+myWorkItemEntity.getName()+" (id: "+id+")"+" Type: "+myWorkItemEntity.getType().getName());
		if (myWorkItemEntity.isIsAggregationNode()) {
			Node Subtasks_node = e.getElementsByTagName("Subtasks").item(0);
			Element Subtasks_element = (Element)Subtasks_node;
			NodeList Subtasks_nodeList = Subtasks_element.getElementsByTagName("workItemId");
			for (int i=0;i<Subtasks_nodeList.getLength();i++) {
				Node node1 = Subtasks_nodeList.item(i);
				Element e1= (Element)node1;
				int Subtask_id = Integer.parseInt(e1.getTextContent());
				myWorkItemEntity.subtasks.add(myWorkItemEntities.get(Subtask_id)); //System.out.println("Subtask "+myWorkItemEntities.get(Subtask_id).getName()+" wiId: "+myWorkItemEntities.get(Subtask_id).getId());
				myWorkItemEntities.get(Subtask_id).uppertasks.add(myWorkItemEntity);
			}
		}
		// Precedency
		boolean hasPredecessors = Boolean.parseBoolean(e.getAttribute("hasPredecessors"));
		myWorkItemEntity.setHasPredecessors(hasPredecessors);
		if (myWorkItemEntity.isHasPredecessors()) {
			Node Predecessor_node = e.getElementsByTagName("Predecessors").item(0);
			Element Predecessor_element = (Element)Predecessor_node;
			NodeList Predecessor_nodeList = Predecessor_element.getElementsByTagName("workItemId");
			for (int i=0;i<Predecessor_nodeList.getLength();i++) {
				Node node1 = Predecessor_nodeList.item(i);
				Element e1= (Element)node1;
				int Predecessor_id = Integer.parseInt(e1.getTextContent());
				myWorkItemEntity.predecessors.add(myWorkItemEntities.get(Predecessor_id)); //System.out.println("Predecessor "+myWorkItemEntities.get(Predecessor_id).getName()+" wiId: "+Predecessor_id);
				myWorkItemEntities.get(Predecessor_id).successors.add(myWorkItemEntity);
			}
		}
	}
	public SystemOfSystems BuildSoS() {
		SystemOfSystems mySoS = new SystemOfSystems();
		mySoS.myServiceProviderAgents = myServiceProviderAgents;
		mySoS.myWorkItemEntities = myWorkItemEntities;
		mySoS.myResourceEntities = myResourceEntities;
		mySoS.myServices = myServices;
		mySoS.myWorkItemTypes = myWorkItemTypes;
		return mySoS;
	}
}
