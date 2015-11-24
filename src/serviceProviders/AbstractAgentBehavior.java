package serviceProviders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import datasemSimulator.AbstractClass;
import repast.simphony.random.RandomHelper;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.ResolutionActivity;
import workItems.DevTask;
import workItems.Task;
import workItems.WorkItemEntity;

public class AbstractAgentBehavior {	
	public HashMap<Integer,String> StepsMap;
	public HashMap<Integer,String> ActionsMap;
	public ServiceProviderAgent agent;		
	public int WIPLimit = Integer.MAX_VALUE;
	public int BacklogLimit = Integer.MAX_VALUE;
	
	public AbstractAgentBehavior() {
		this.StepsMap = new HashMap<Integer,String>();
		addStep(1, "CheckRequestedQ");
		addStep(2, "MakeAssignments");
		addStep(3, "SelectWIsToStart");
		addStep(4, "AdvanceWIsProgress");
		addStep(5, "TriggerWIsChanges");
		addStep(6, "CheckWIsCompletion");
		addStep(7, "CheckAggregationNodesCompletion");
		addStep(8, "DisburseWIs");
		addStep(9, "EndState");
	}
	public void setAgent(ServiceProviderAgent agent) {
		this.agent = agent;
		this.BacklogLimit = 10;
		this.WIPLimit = 10;
	}
	public void addStep(int key,String state) {
		this.StepsMap.put(key, state);
	}
	public void addAction(int key,String action) {
		this.ActionsMap.put(key, action);
	}
	public void GoToStep(int n) {
		if (StepsMap.containsKey(n)) {
			//System.out.println(agent.getName()+" step code: "+n+" step "+StepsMap.get(n));
			switch(StepsMap.get(n)) {		
				case "CheckRequestedQ": CheckRequestedQ(); break;
				case "MakeAssignments": MakeAssignments(); break;
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
	public void DoAction(int n, AbstractClass Object) {
		switch(ActionsMap.get(n)) {
			case "requestAssistance": 
				if(Object.getClass().equals("WorkItemEntity")){
					DevTask devTask = (DevTask)ObjectToWorkItemEntity(Object);
					agent.requestAssistance(devTask);
				}
			case "acceptanceDecision": 
				if(Object.getClass().equals("WorkItemEntity")){
					WorkItemEntity wi = ObjectToWorkItemEntity(Object);
					this.acceptanceDecision(wi);
				}
			case "analyzeAggregationNode": Action(Object);
			case "requestService": Action(Object);
			case "acceptWI": Action(Object);
			case "releaseSubtasks": Action(Object);
		}
	}
	public boolean CheckCondition(String condition) {	
		return false;
	}

	public void DoAction(int n) {
		switch(ActionsMap.get(n)) {
		}
	}
	public WorkItemEntity ObjectToWorkItemEntity(AbstractClass Object) {
		WorkItemEntity wi = agent.SoS.myWorkItemEntities.get(0);
		return wi;
	}
	public void Action() {
		
	}
	public void Action(AbstractClass Object) {
		
	}
	public void CheckRequestedQ() {
		//requestedQ = agent.myStrategy.applyWorkPrioritization(requestedQ);
		//for (WorkItemEntity requestedWI:agent.getRequestedQ()) {
			//myValueManagement.manageValue(this, requestedWI);
		//}
		// ------------ 1. Select WIs to Accept
		while (!agent.getRequestedQ().isEmpty()) {
			// =========== Apply WI Acceptance Rule ====================
			WorkItemEntity wi = agent.getRequestedQ().getFirst();			
			// =========== Service Efficiency Algorithm ==============
			String decision = this.acceptanceDecision(wi);
			//System.out.println("\n"+wi.fullName+"("+wi.SoS.myServices.get(wi.serviceId).getName()+"x"+wi.efforts+") Decision: "+decision);
			if (decision.matches("Accept")) {
				//System.out.println(agent.getName()+" accepts"+wi.fullName);
				agent.acceptWI(wi);
			}
			else if (decision.matches("Outsource")) {
				//System.out.println(agent.getName()+" decides to outsource"+wi.fullName);
				agent.getAssignmentQ().add(wi);
			}
			else if (decision.matches("RequestHelp")) {
				if (agent.getId()==agent.SoS.coordinator.getId()) {
					//System.out.println(agent.getName()+" handles"+wi.fullName);
					agent.getAssignmentQ().add(wi);
				}
				else {
					//System.out.println(agent.getName()+" requests help from "+agent.SoS.coordinator.getName()+" on"+wi.fullName);
					agent.SoS.coordinator.getRequestedQ().add(wi);
				}
			}
			else if (decision.matches("Decline")) {
				//System.out.println(agent.getName()+" declines"+wi.fullName+"from "+wi.getRequester().getName());
				if (wi.getRequester().getId()==agent.getId()) {
					System.out.println("ERROR: "+agent.getName()+" declines"+wi.fullName+" which is requested by itself!");
					System.exit(0);
				}
				else {
					wi.getRequester().getRequestedQ().add(wi);
				}
			}
			else {
				System.out.println("ERROR: "+" invalid decision --"+decision+"--!");
				System.exit(0);
			}
			agent.getRequestedQ().remove(wi);
		}
	}

	public void MakeAssignments() {
		agent.NowRequested.clear();
		LinkedList<ServiceProviderAgent> candidates = agent.assignWITo;
		HashMap<WorkItemEntity,ServiceProviderAgent> schedule = 
				agent.myStrategy.applyAgentSelection(agent, agent.getAssignmentQ(), candidates);
		for (WorkItemEntity wi: schedule.keySet()) {
			ServiceProviderAgent selectedSP = schedule.get(wi);
			agent.NowRequested.add(selectedSP);
			agent.requestService(wi, selectedSP);

			agent.getAssignmentQ().remove(wi);

			//System.out.println(agent.getName()+" assigned"+wi.fullName+"to "+selectedSP.getName());
			selectedSP.tempQ.clear();
			//selectedSP.checkRequestedQ();
		}
	}

	public void SelectWIsToStart() {
		LinkedList<Task> readyQ = new LinkedList<Task>();		
		readyQ = agent.myStrategy.applyWorkPrioritization(agent,agent.getBacklogQ());
		for (int i=0;i<readyQ.size();i++) {			
			// =========== Apply WI Selection Rule ====================
			Task wi = readyQ.get(i);
			if (this.taskCompletionHandling(wi)) {
				readyQ.remove(wi);
				i--;
			}
			else if (wi.precedencyCleared()) {
				// ========================================================
				ArrayList<ResourceEntity> serviceResourceCandidates = agent.findResourceEntities(wi);
				// =========== Apply Resource Allocation Rule =============
				ArrayList<ResourceEntity> idleResources = new ArrayList<ResourceEntity>();
				for (int r=0;r<serviceResourceCandidates.size();r++) {
					ResourceEntity candidateSR = serviceResourceCandidates.get(r);
					// only look at Idle Candidate Resources;
					if (!candidateSR.isBusy()) {
						idleResources.add(candidateSR);
					}
				}
				if (!idleResources.isEmpty()) {
					ResourceEntity selectedSR = idleResources.get(RandomHelper.nextIntFromTo(0, idleResources.size()-1));
					selectedSR.allocateTo(wi);				
				// ========================================================
					wi.setStarted();
					double rEfficiency = wi.calculateResourceEfficiency();	
					wi.setServiceEfficiency(rEfficiency);
				// =========== Estimate Completion ====================				
					double eEfforts = wi.efforts/rEfficiency;
					wi.estimatedEfforts = eEfforts;
					double eCompletion= eEfforts + agent.SoS.timeNow;
					wi.estimatedCompletionTime = eCompletion;
					// ====================================================
					agent.getActiveQ().add((Task)wi);
					agent.getBacklogQ().remove(wi);
				}
				else {
					//System.out.println(this.name+" :No Resources available for"+wi.fullName+"now!");				
				}
			}
			else {
				//System.out.println(this.name+" :Cannot Start"+wi.fullName+"due to Precedency");
			}
		}
	}
	
	public void AdvanceWIsProgress() {
		//System.out.println("Agent "+this.name+" checkWIsCompletion");
		for(int i=0;i<agent.getActiveQ().size();i++) {
			Task WI = agent.getActiveQ().get(i);
			WI.advanceProgress();
		}
	}
	
	public void TriggerWIsChanges() {
		//System.out.println("Agent "+this.name+" checkWIsCompletion");
		for(int i=0;i<agent.getActiveQ().size();i++) {
			Task task = agent.getActiveQ().get(i);
			if (task.isDevTask) {
				((DevTask)task).triggerChanges();
			}
		}
	}
	
	public void CheckWIsCompletion() {
		//System.out.println("Agent "+this.name+" checkWIsCompletion");
		for(int i=0;i<agent.getActiveQ().size();i++) {
			Task wi = agent.getActiveQ().get(i);
			if ( this.taskCompletionHandling(wi) ){
				i--;
			}
		}
	}		
	
	public void CheckAggregationNodesCompletion() {
		for(int i=0;i<agent.getComplexQ().size();i++) {
			AggregationNode aggrWI = agent.getComplexQ().get(i);
			aggrWI.updateUpperTasksCompletion();
			aggrWI.updateProcessModelStage();
			if (aggrWI.isCompleted) {
				agent.getComplexQ().remove(aggrWI);
				agent.getCompleteQ().add(aggrWI);
				i--;
			}
		}			
	}		
	public void DisburseWIs() {			
		for (int i=0;i<agent.getCompleteQ().size();i++) {
			WorkItemEntity wi=agent.getCompleteQ().get(i);				
			//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" try to disburse"+completedWI.fullName+"...");
			if (wi.getProgress() <= 0.9999) {
				wi.isCompleted = false;
				agent.getCompleteQ().remove(wi);	
				if (wi.isAggregationNode) {
					agent.getComplexQ().add((AggregationNode)wi);
				}
				else {
					agent.getBacklogQ().add((Task)wi);
				}				
				i--;
			}
			else if (wi.uppertasksCleared()) {
				//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" disbursed"+completedWI.fullName);
				wi.setEnded();	
				agent.getCompleteQ().remove(wi);					
				i--;			
			}
		}
	}
	public void EndState() {					
		agent.statusSummary();
	}

	public String acceptanceDecision(WorkItemEntity requestedWI) {
		String decision = "Decline";		
		if (requestedWI.isAggregationNode) {
			double capacity = ((AggregationNode)requestedWI).calculateServiceCapacity(agent);	
			if (capacity>0) {
				decision = "Accept";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
			}
			else {
				double exCapacity = ((AggregationNode)requestedWI).calculateExtendedServiceCapacity(agent);	
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					if (requestedWI.getRequester().getId() == agent.getId()) {
						decision = "RequestHelp";
					}
					else {
						decision = "Decline";
					}
				}
			}
		}
		else if (requestedWI.isResolutionActivity) {
			double capacity = ((Task)requestedWI).calculateServiceCapacity(agent);	
			if (capacity>0) {
				decision = "Accept";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
			}
			else {
				double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(agent);	
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					decision = "RequestHelp";
				}
			}
		}
		else {
			if (requestedWI.getRequester().getId() == agent.getId()) {
				double capacity = ((Task)requestedWI).calculateServiceCapacity(agent);
				if (capacity>0) {
					decision = "Accept";
					//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
				}
				else {
					double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(agent);	
					if (exCapacity>0) {
						decision = "Outsource";
					}
					else {
						decision = "RequestHelp";
					}
				}
			}
			else if (agent.getBacklogQ().size()>=this.BacklogLimit) {
				decision = "Decline";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to BacklogLimit");		
			}
			else {
				double capacity = ((Task)requestedWI).calculateServiceCapacity(agent);
				if (capacity>0) {
					decision = "Accept";
					//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
				}
				else {
					double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(agent);	
					if (exCapacity>0) {
						decision = "Outsource";
					}
					else {
						decision = "Decline";
					}
				}
			}
		}
		return decision;
	}	
	
	public boolean taskCompletionHandling(Task WI) {
		boolean completion = false;
		if ( (WI.getProgress()>0.9999) && (!WI.isSuspended) ){
			if (WI.isAnalysisActivity) {
				//System.out.println("\nCOMPLETED ANALYSIS @TIME:"+SoS.timeNow+" Agent "+this.name+" completed analyzing"+WI.AnalysisObject.fullName);
				AggregationNode analysisObject = (AggregationNode)((AnalysisActivity)WI).AnalysisObject;		
				analysisObject.currentAnalysisStage ++;
				//System.out.println(analysisObject.currentAnalysisStage+" "+analysisObject.totalAnalysisStage);
				if (analysisObject.currentAnalysisStage == analysisObject.totalAnalysisStage) {
					agent.SoS.myValueFunction.developValue(analysisObject);
					agent.releaseSubtasks(analysisObject);					
				}
				else {
					agent.releaseSubtasks(analysisObject);
					analysisObject.serviceId = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getServiceType().getId();
					analysisObject.efforts = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getEfforts();
					agent.getRequestedQ().add(analysisObject);
					agent.getComplexQ().remove(analysisObject);
				}
			}
			else if (WI.isResolutionActivity) {
				Task suspendedTask = (Task) ((ResolutionActivity)WI).ResolutionObject;
				suspendedTask.isSuspended = false;
				suspendedTask.getPredecessors().remove(WI);
				//System.out.println("\nSUSPENSION CLEARED @TIME:"+this.SoS.timeNow+suspendedTask.fullName+"(suspension duration: "+(this.SoS.timeNow-suspendedTask.suspendedTime)+")");
			}
			completion = true;
			WI.setCompleted();
			WI.withdrawAllResources();
			agent.getBacklogQ().remove(WI);
			agent.getActiveQ().remove(WI);
			agent.getCompleteQ().add(WI);
		}
		return completion;
	}	
}
