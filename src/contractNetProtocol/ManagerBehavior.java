package contractNetProtocol;
import java.util.HashMap;
import java.util.LinkedList;

import datasemSimulator.AbstractClass;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import serviceProviders.AbstractAgentBehavior;
import serviceProviders.ServiceProviderAgent;
import workItems.DevTask;
import workItems.WorkItemEntity;
import xtext.objectsModel.ServiceProvider;


public class ManagerBehavior extends AbstractAgentBehavior{
	public LinkedList<PurposalCall> myPurposalCalls = new LinkedList<PurposalCall>();
	public LinkedList<Contract> myContracts = new LinkedList<Contract>();
	public LinkedList<Bid> receivedBids = new LinkedList<Bid>();
	
	public ManagerBehavior() {
		super();
		this.StepsMap = new HashMap<Integer,String>();
		addStep(1, "CheckRequestedQ");
		addStep(2, "TasksAnnouncement");
		addStep(3, "BiddersSelection");
		addStep(4, "SelectWIsToStart");
		addStep(5, "AdvanceWIsProgress");
		addStep(6, "TriggerWIsChanges");
		addStep(7, "CheckWIsCompletion");
		addStep(8, "CheckAggregationNodesCompletion");
		addStep(9, "DisburseWIs");
		addStep(10, "EndState");
	}

	public void GoToStep(int n) {
		if (StepsMap.containsKey(n)) {
			switch(StepsMap.get(n)) {		
			case "CheckRequestedQ": CheckRequestedQ(); break;
			case "TasksAnnouncement": TasksAnnouncement(); break;
			case "BiddersSelection": BiddersSelection(); break;
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

	public void TasksAnnouncement() {
		agent.NowRequested.clear();
		for (int i=0;i<agent.getAssignmentQ().size();i++) {
			WorkItemEntity wi = agent.getAssignmentQ().get(i);
			this.callForPurposal(wi);
			agent.getAssignmentQ().remove(wi);
			i--;
		}
	}	
	public void BiddersSelection() {
		HashMap<ServiceProviderAgent,LinkedList<WorkItemEntity>> contractsMap = new HashMap<ServiceProviderAgent,LinkedList<WorkItemEntity>>();
		for (PurposalCall p : this.myPurposalCalls) {
			WorkItemEntity wi = p.target;
			LinkedList<Bid> wi_bids = new LinkedList<Bid>();
			for (Bid bid : this.receivedBids) {
				if (bid.target.getId() == wi.getId()) {
					wi_bids.add(bid);
				}
			}
			
			if (!wi_bids.isEmpty()) {
				// Selection Rule Impl
				Bid selectedBid = wi_bids.get( (int) (Math.random()*wi_bids.size()) );
				//
				ServiceProviderAgent selectedBidder = selectedBid.bidder;
				if (contractsMap.containsKey(selectedBidder)) {
					LinkedList<WorkItemEntity> contractWIs = contractsMap.get(selectedBidder);
					contractWIs.add(wi);
					contractsMap.put(selectedBid.bidder, contractWIs);
				}
				else {
					LinkedList<WorkItemEntity> contractWIs = new LinkedList<WorkItemEntity>();
					contractWIs.add(wi);		
					contractsMap.put(selectedBid.bidder, contractWIs);
				}
			}			
		}
		for (ServiceProviderAgent contractor : contractsMap.keySet()) {
			this.buildContract(contractsMap.get(contractor), contractor);
		}
	}
	public void callForPurposal(WorkItemEntity wi) {
		PurposalCall newPurposalCall = new PurposalCall();
		newPurposalCall.target = wi;
		newPurposalCall.requester = this.agent;
		this.myPurposalCalls.add(newPurposalCall);
		for (ServiceProviderAgent sp : this.agent.assignWITo) {
			((ContractorBehavior)sp.myBehavior).receivedPurposalCalls.add(newPurposalCall);
		}
		System.out.println(this.agent.getName()+" call for purposal on: "+wi.getName());
	}
	
	public void buildContract(LinkedList<WorkItemEntity> wis, ServiceProviderAgent contractor) {
		Contract newContract = new Contract(wis, this.agent, contractor);	
		//add contract to both parties
		this.myContracts.add(newContract);
		((ContractorBehavior)contractor.myBehavior).myContracts.add(newContract);
		
		System.out.println("Contract Built on:");
		for (WorkItemEntity wi : wis) {
			contractor.acceptWI(wi);
			System.out.println(wi.fullName);
			for (int i=0;i<this.myPurposalCalls.size();i++) {
				PurposalCall p = this.myPurposalCalls.get(i);
				if (wi.getId()==p.target.getId()) {
					p.isEnded = true;
					this.myPurposalCalls.remove(p);
					i--;
				}
			}	
		}
		System.out.println(agent.getName()+" Contractor:"+contractor.getName());
	}
}
