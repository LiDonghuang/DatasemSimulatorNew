package governanceModels;

import java.util.HashMap;
import java.util.LinkedList;

import contractNetProtocol.ContractNetProtocol;
import serviceProviders.ServiceProviderAgent;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.impl.GovernanceStrategyImpl;

public class AgentStrategy extends GovernanceStrategyImpl {
	public boolean isPull=false;
	public boolean isPush=false;
	public boolean isCNP=false;
	
	protected WISelectionRule mySelectionRule;
	protected WIAcceptanceRule myAcceptanceRule;
	protected WIAssignmentRule myAssignmentRule;
	protected ResourceAllocationRule myAllocationRule;
	
	public AgentStrategy(GovernanceStrategy myGovernanceStrategy) {
		this.implementAgentStrategy(myGovernanceStrategy);
	}
	
	public void implementAgentStrategy(GovernanceStrategy myGovernanceStrategy) {
		myGovernanceStrategy.setType("Pull");
		if (myGovernanceStrategy.getType().matches("CNP")) {
			new ContractNetProtocol(myGovernanceStrategy);
			this.isCNP = true;
		}
		else if (myGovernanceStrategy.getType().matches("Pull")){
			this.implementPullStrategy(myGovernanceStrategy);
			this.isPull = true;
		}	
		else if (myGovernanceStrategy.getType().matches("Push")){
			this.implementPullStrategy(myGovernanceStrategy);
			this.isPush = true;
		}
		else {
			throw new RuntimeException(myGovernanceStrategy.getType()+" is not a Valid Strategy Type!");
		}
	}

	public void implementPullStrategy(GovernanceStrategy myGovernanceStrategy) {
		for (Mechanism m: myGovernanceStrategy.getMechanisms()) {
			implementMechanism(m);
		}
	}
	public void implementMechanism(Mechanism m) {
		if (m.getName().matches("ValueFunction")) {
			
		}
		else if (m.getName().matches("Acceptance")) {
			
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
	public LinkedList<Task> applyWorkPrioritization(ServiceProviderAgent sp,LinkedList<Task> queue) {		
		return this.mySelectionRule.applyRule(sp, queue);		
	}
	public HashMap<WorkItemEntity, ServiceProviderAgent> applyContractorSelection(ServiceProviderAgent me, LinkedList<WorkItemEntity> WIs, LinkedList<ServiceProviderAgent> SPs) {
		return this.myAssignmentRule.applyRule(me, WIs, SPs);
	}
}
