package contractNetProtocol;

import java.util.HashMap;
import java.util.LinkedList;

import serviceProviders.AbstractAgentBehavior;
import workItems.AggregationNode;
import workItems.Task;
import workItems.WorkItemEntity;

public class ContractorBehavior extends AbstractAgentBehavior{
	public LinkedList<PurposalCall> receivedPurposalCalls = new LinkedList<PurposalCall>();
	public LinkedList<Contract> myContracts = new LinkedList<Contract>();
	
	public ContractorBehavior() {
		super();
		this.StepsMap = new HashMap<Integer,String>();
		addStep(1, "CheckRequestedQ");
		addStep(2, "Bidding");
		addStep(3, "SelectWIsToStart");
		addStep(4, "AdvanceWIsProgress");
		addStep(5, "TriggerWIsChanges");
		addStep(6, "CheckWIsCompletion");
		addStep(7, "CheckAggregationNodesCompletion");
		addStep(8, "DisburseWIs");
		addStep(9, "EndState");
	}
	public void GoToStep(int n) {
		if (StepsMap.containsKey(n)) {
			switch(StepsMap.get(n)) {		
			case "CheckRequestedQ": CheckRequestedQ(); break;
			case "Bidding": Bidding(); break;
			case "SelectWIsToStart": SelectWIsToStart(); break;
			case "AdvanceWIsProgress": AdvanceWIsProgress();  break;
			case "TriggerWIsChanges": TriggerWIsChanges(); break;
			case "CheckWIsCompletion": CheckWIsCompletion(); break;
			case "CheckAggregationNodesCompletion": CheckAggregationNodesCompletion(); break;
			case "DisburseWIs": DisburseWIs(); break;
			case "EndState": EndState(); break;
			}
		}
	}
	public void Bidding() {
		for (int i=0;i<this.receivedPurposalCalls.size();i++) {
			PurposalCall receivedCall = this.receivedPurposalCalls.get(i);
			if (receivedCall.isEnded) {
				this.receivedPurposalCalls.remove(receivedCall);
				i--;
			}
			else {
				WorkItemEntity requestedWI = receivedCall.target;
				if (requestedWI.isAggregationNode) {
					double capacity = ((AggregationNode)requestedWI).calculateServiceCapacity(agent);
					if (capacity>0) {
						// Bidding Logic
						if (Math.random()<0.5) {
							this.bid(receivedCall);
						}
					}
				}
				else {
					double capacity = ((Task)requestedWI).calculateServiceCapacity(agent);
					if (capacity>0) {
						// Bidding Logic
						if (Math.random()<0.5) {
							this.bid(receivedCall);
						}
					}
				}	
			}
		}
	}
	public void bid(PurposalCall purposalCall) {
		Bid newBid = new Bid();
		newBid.bidder = this.agent;
		newBid.target = purposalCall.target;
		((ManagerBehavior)purposalCall.requester.myBehavior).receivedBids.add(newBid);
		System.out.println(this.agent.getName()+" responded "+purposalCall.requester.getName()+"'s call for purposal on "+purposalCall.target.getName());
	}
}
