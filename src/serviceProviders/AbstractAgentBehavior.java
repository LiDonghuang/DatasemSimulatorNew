package serviceProviders;

import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.ResolutionActivity;
import workItems.DevTask;
import workItems.Task;
import workItems.WorkItemEntity;

public class AbstractAgentBehavior {	
	public HashMap<Integer,String> StepsMap;
	public ServiceProviderAgent agent;		
	public int WIPLimit = Integer.MAX_VALUE;
	public int BacklogLimit = Integer.MAX_VALUE;
	public int MultiTasking = 1;
	private int tick;
	private boolean sprintNow;
	
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
	public void GoToStep(int n) {
		if (StepsMap.containsKey(n)) {
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


	public void CheckRequestedQ() {
		tick++;
		setSprintNow(false);
		// "Sprint": to check and handle requested WIs when one of the following is true:
		if (tick==agent.myStrategy.Cadence 
			// 1. sprint time
			|| agent.getRequestedQ().isEmpty() 
			// 2. no work is currently requested
			|| (agent.getBacklogQ().isEmpty() && agent.getActiveQ().isEmpty()))  
			// 3. no work is under processing or to be processed
		{	
			tick=0;
			setSprintNow(true);
		}
		LinkedList<Task> requestedTasks = new LinkedList<Task>();
		// split requested WIs into Aggregation(Complex) WIs and Tasks
		for (int i=0;i<agent.getRequestedQ().size();i++) {
			WorkItemEntity wi = agent.getRequestedQ().get(i);
			if (isSprintNow() || agent.getId()==wi.getRequester().getId()) {
				// Aggregation WIs
				if (wi.isAggregationNode) {
					wi.setAssigned();
					wi.setAssignedAgent(agent);
					agent.getComplexQ().add((AggregationNode)wi);
					agent.analyzeAggregationNode((AggregationNode)wi); //create initial Analysis Activity					
					agent.getRequestedQ().remove(wi);
					i--;
				}
				// Tasks, to be further handled
				else {
					requestedTasks.add((Task)wi);
					agent.getRequestedQ().remove(wi);
					i--;
				}
			}
		}
		// SP needs to handle ALL requested tasks all the time
		agent.myStrategy.applyWorkPrioritization(agent, requestedTasks);
		while (!requestedTasks.isEmpty()) {
			Task task = requestedTasks.getFirst();
			String decision = this.acceptanceDecision(task);
			System.out.println(agent.getName()+" on"+task.fullName+"requested by "+task.getRequester().getName()+"("+task.SoS.myServices.get(task.serviceId).getName()+"x"+task.efforts+") Decision: "+decision);
			if (!task.precedencyCleared()) {
				if (this.predsInMyQ(task)) {
					System.out.println(this.agent.getName()+": All of task "+task.getName()+"'s uncleared predecessors are in queue");
				}
				else {
					System.out.println(this.agent.getName()+": some of task "+task.getName()+"'s uncleared predecessors are not in queue!");
				}
			}
			if (decision.matches("Accept")) {	
				task.currentDecision = 1;
				agent.acceptWI(task);
			}
			else if (decision.matches("Outsource")) {
				task.currentDecision = 2;
				agent.getAssignmentQ().add(task);
			}
			else if (decision.matches("RequestHelp")) {
				if (agent.getId()==agent.SoS.coordinator.getId()) {
					task.currentDecision = 2;
					agent.getAssignmentQ().add(task);
				}
				else {
					task.currentDecision = 3;
					agent.requestService(task, agent.SoS.coordinator);
					if (task.isAnalysisActivity) {
						AggregationNode aggr = ((AnalysisActivity)task).AnalysisObject;
						agent.SoS.coordinator.getComplexQ().add(aggr);
						agent.getComplexQ().remove(aggr);
					}
				}
			}
			else if (decision.matches("Decline")) {
				if (task.getRequester().getId()==agent.getId()) {
					String msg = "ERROR: "+agent.getName()+" declines"+task.fullName+" which is requested by itself!";
					JOptionPane.showMessageDialog(null,msg);
					throw new RuntimeException(msg);
				}
				else {
					task.currentDecision = 4;
					task.getRequester().getRequestedQ().add(task);
				}
			}
			else {
				String msg = "ERROR: "+" invalid decision -"+decision+"-!";
				JOptionPane.showMessageDialog(null,msg);
				throw new RuntimeException(msg);
			}
			requestedTasks.remove(task);
			agent.getRequestedQ().remove(task);
		}
	}

	public void MakeAssignments() {
		LinkedList<ServiceProviderAgent> candidates = agent.assignWITo;
		// create schedule which is a HashMap consists of WI-SP mappings
		HashMap<Task,ServiceProviderAgent> schedule = agent.myStrategy.applyAgentSelection(agent, agent.getAssignmentQ(), candidates);
		for (WorkItemEntity wi: schedule.keySet()) {
			ServiceProviderAgent selectedSP = schedule.get(wi);
			agent.requestService(wi, selectedSP);
			agent.getAssignmentQ().remove(wi);		
			agent.NowRequested.add(selectedSP);
			//selectedSP.checkRequestedQ();
		}
		// return unassigned WIs back to Requested Q
		agent.getRequestedQ().addAll(agent.getAssignmentQ());
		agent.getAssignmentQ().clear();		
	}
	
	public void SelectWIsToStart() {
		LinkedList<Task> readyQ = new LinkedList<Task>();
		readyQ.addAll(agent.getBacklogQ());
		agent.myStrategy.applyWorkPrioritization(agent, readyQ);
		for (int i=0;i<readyQ.size();i++) {			
			Task wi = readyQ.get(i);	
			if (!wi.precedencyCleared()) {
				readyQ.remove(wi);
				i--;
			}
			else if (this.taskCompletionHandling(wi)) {
				readyQ.remove(wi);
				i--;
			}
		}
		HashMap<Task,ResourceEntity> schedule = agent.myStrategy.applyResourceAllocation(agent, readyQ);
		for (Task wi: schedule.keySet()) {
			ResourceEntity selectedR = schedule.get(wi);
			selectedR.allocateTo(wi);
			wi.setStarted();
			agent.getActiveQ().add(wi);
			agent.getBacklogQ().remove(wi);
		}
	}
	
	public void AdvanceWIsProgress() {
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
				wi.setEnded();	
				agent.getCompleteQ().remove(wi);					
				i--;			
			}
		}
	}
	public void EndState() {	
		agent.NowRequested.clear();
		agent.statusSummary();
	}

	public String acceptanceDecision(Task task) {
		String decision;		
		double capacity = task.calculateServiceCapacity(agent);	
		double exCapacity = task.calculateExtendedServiceCapacity(agent);	
		// case 1: task is to Resolve problem of another DevTask
		if (task.isResolutionActivity) {
			if (capacity>0) {
				decision = "Accept";
			}
			else {
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					decision = "RequestHelp"; // resort to SoS coordinator
				}
			}
		}
		else {
			// case 2: task is originated by the current SP, e.g. a newly released SubTask,
			// so it can NOT be Declined
			if (task.getRequester().getId() == agent.getId()) {
				// case 2.1: SP is capable
				if (capacity>0) {
					if ((agent.getBacklogQ().size()>=this.BacklogLimit)&&(exCapacity>0)) {
						decision = "Outsource";
					}
					else {
						decision = "Accept";
					}
				}
				// case 2.2: SP is not capable
				else {
					if (exCapacity>0) {
						decision = "Outsource";
					}
					else {
						decision = "RequestHelp"; // resort to SoS coordinator
					}
				}
			}
			// case 3: task is requested by another SP
			// case 3.1: SP's Backlog is full
			else if (agent.getBacklogQ().size()>=this.BacklogLimit) {
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					decision = "Decline";	
				}
			}
			// case 3.2: SP's Backlog is Not full
			else {
				// case: SP is capable to process
				if (capacity>0) {
					decision = "Accept";
				}
				else {	
					// case: SP is not capable, but can resort to connected SPs
					if (exCapacity>0) {
						decision = "Outsource";
					}
					// case: can neither process nor resort
					else {
						decision = "Decline";
					}
				}
			}
		}
		return decision;
	}	
	// checks if all uncleared preds of a specific task is in this SP's queues
	public boolean predsInMyQ(Task task) {
		boolean inQ = true;
		for (WorkItemEntity pred: task.getPredecessors()) {
			if (!this.agent.getActiveQ().contains(pred) && !this.agent.getBacklogQ().contains(pred)) {
				inQ = false;
			}
		}
		return inQ;
	}
	
	public boolean taskCompletionHandling(Task task) {
		boolean completion = false;
		if ( (task.getProgress()>0.9999) && (!task.isSuspended) ){			
			if (task.isAnalysisActivity) {
				//System.out.println("\nCOMPLETED ANALYSIS @TIME:"+SoS.timeNow+" Agent "+this.name+" completed analyzing"+WI.AnalysisObject.fullName);
				AggregationNode analysisObject = (AggregationNode)((AnalysisActivity)task).AnalysisObject;		
				analysisObject.currentAnalysisStage ++;
				//System.out.println(analysisObject.currentAnalysisStage+" "+analysisObject.totalAnalysisStage);
				if (analysisObject.currentAnalysisStage == analysisObject.totalAnalysisStages) {				
					agent.releaseSubtasks(analysisObject);			
					analysisObject.myValueFunction.developValue(analysisObject);
				}
				else {
					agent.releaseSubtasks(analysisObject);
					analysisObject.myValueFunction.developValue(analysisObject);
					analysisObject.serviceId = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getServiceType().getId();
					analysisObject.efforts = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getEfforts();
					agent.getRequestedQ().add(analysisObject);
					agent.getComplexQ().remove(analysisObject);
				}
			}
			else if (task.isResolutionActivity) {
				Task suspendedTask = (Task) ((ResolutionActivity)task).ResolutionObject;
				suspendedTask.isSuspended = false;
				suspendedTask.getPredecessors().remove(task);
				taskCompletionHandling(suspendedTask);
				//System.out.println("\nSUSPENSION CLEARED @TIME:"+agent.SoS.timeNow+suspendedTask.fullName);
			}
			agent.getBacklogQ().remove(task);
			agent.getActiveQ().remove(task);
			agent.getCompleteQ().add(task);
			completion = true;
			task.setCompleted();
			task.withdrawAllResources();
		}
		return completion;
	}	
	
	public void setAgent(ServiceProviderAgent agent) {
		this.agent = agent;
		this.agent.myStrategy.applyWorkAcceptance(agent);
	}
	public void addStep(int key,String state) {
		this.StepsMap.put(key, state);
	}
	public boolean isSprintNow() {
		return sprintNow;
	}
	public void setSprintNow(boolean sprintNow) {
		this.sprintNow = sprintNow;
	}
}
