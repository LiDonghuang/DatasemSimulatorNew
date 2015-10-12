package governanceModels;

import java.util.LinkedList;

import serviceProviders.ServiceProviderAgent;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.impl.GovernanceStrategyImpl;

public class AgentStrategy extends GovernanceStrategyImpl {
	public ServiceProviderAgent myServiceProviderAgent;
	protected WISelectionRule mySelectionRule;
	protected WIAcceptanceRule myAcceptanceRule;
	protected WIAssignmentRule myAssignmentRule;
	protected ResourceAllocationRule myAllocationRule;
	
	public void implementAgentStrategy(ServiceProviderAgent myServiceProviderAgent, GovernanceStrategy myGovernanceStrategy) {
		this.myServiceProviderAgent = myServiceProviderAgent;
		for (Mechanism m: myGovernanceStrategy.getMechanisms()) {
			implementMechanism(m);
		}
		this.myServiceProviderAgent.myStrategy = this;
	}	
	public void implementMechanism(Mechanism m) {
		if (m.getName().matches("Acceptance")) {
			
		}
		else if (m.getName().matches("Selection")) {			
			mySelectionRule = new WISelectionRule();
			mySelectionRule.implementWISelectionRule(m);
		}
		else if (m.getName().matches("Assignment")) {
			myAssignmentRule = new WIAssignmentRule();
			myAssignmentRule.implementWIAssignmentRule(m);
		}
		else if (m.getName().matches("Allocation")) {
			
		}
		else if (m.getName().matches("Outsourcing")) {
			
		}
		else {
			System.out.println("Invalid Mechanism Name!");
		}		
	}
	public LinkedList<Task> applyWorkPrioritization(LinkedList<Task> queue) {		
		return this.mySelectionRule.applyRule(this.myServiceProviderAgent, queue);		
	}
	public LinkedList<ServiceProviderAgent> applyContractorSelection(LinkedList<ServiceProviderAgent> candidates) {
		return this.myAssignmentRule.applyRule(this.myServiceProviderAgent, candidates);
	}
}
