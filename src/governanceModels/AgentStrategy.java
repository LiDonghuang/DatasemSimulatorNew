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
		if (myGovernanceStrategy.getType().matches("cnp")) {
			new ContractNetProtocol(myGovernanceStrategy);
			this.isCNP = true;
			this.implementPullStrategy(myGovernanceStrategy);
		}
		else if (myGovernanceStrategy.getType().matches("pull")){
			this.implementPullStrategy(myGovernanceStrategy);
			this.isPull = true;
		}	
		else if (myGovernanceStrategy.getType().matches("push")){
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
	public HashMap<WorkItemEntity, ServiceProviderAgent> applyAgentSelection(ServiceProviderAgent me, LinkedList<WorkItemEntity> WIs, LinkedList<ServiceProviderAgent> SPs) {
		return this.myAssignmentRule.applyRule(me, WIs, SPs);
	}
}
