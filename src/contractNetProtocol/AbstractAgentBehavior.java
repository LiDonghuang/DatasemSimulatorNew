package contractNetProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.ResolutionActivity;
import workItems.DevTask;
import workItems.Task;
import workItems.WorkItemEntity;

public class AbstractAgentBehavior {
	
	public HashMap<Integer,String> StatesMap;
	public HashMap<Integer,String> ActionsMap;
	public ServiceProviderAgent agent;		
	
	public AbstractAgentBehavior() {
	}
	public void setAgent(ServiceProviderAgent agent) {
		this.agent = agent;
	}

	public void GoToState(int n) {
		switch(StatesMap.get(n)) {
			case "CheckRequestedQ": CheckRequestedQ();
			case "MakeAssignments": MakeAssignments();
			case "SelectWIsToStart": SelectWIsToStart();
			case "AdvanceWIsProgress": AdvanceWIsProgress();
			case "TriggerWIsChanges": TriggerWIsChanges();
			case "CheckWIsCompletion": CheckWIsCompletion();
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
					agent.acceptanceDecision(wi);
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
	public void StatementImpl(String statement) {
		
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
			String decision = agent.acceptanceDecision(wi);
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
				agent.myStrategy.applyContractorSelection(agent, agent.getAssignmentQ(), candidates);
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
			if (wi.precedencyCleared()) {
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
			Task WI = agent.getActiveQ().get(i);
			if (WI.getProgress()>=1.00) {
				if (WI.isAnalysisActivity) {
					//System.out.println("\nCOMPLETED ANALYSIS @TIME:"+SoS.timeNow+" Agent "+this.name+" completed analyzing"+WI.AnalysisObject.fullName);
					AggregationNode analysisObject = (AggregationNode)((AnalysisActivity)WI).AnalysisObject;		
					analysisObject.currentAnalysisStage ++;
					//System.out.println(analysisObject.currentAnalysisStage+" "+analysisObject.totalAnalysisStage);
					if (analysisObject.currentAnalysisStage == analysisObject.totalAnalysisStage) {
						agent.releaseSubtasks(analysisObject);
						agent.SoS.myValueFunction.developValue(analysisObject);
					}
					else {
						analysisObject.serviceId = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getServiceType().getId();
						analysisObject.efforts = analysisObject.myWorkItem.getRequiredAnalysis().get(analysisObject.currentAnalysisStage).getEfforts();
						agent.getRequestedQ().add(analysisObject);
					}
				}
				else if (WI.isResolutionActivity) {
					Task suspendedTask = (Task) ((ResolutionActivity)WI).ResolutionObject;
					suspendedTask.isSuspended = false;
					//System.out.println("\nSUSPENSION CLEARED @TIME:"+this.SoS.timeNow+suspendedTask.fullName+"(suspension duration: "+(this.SoS.timeNow-suspendedTask.suspendedTime)+")");
				}
				WI.setCompleted();
				WI.withdrawAllResources();
				agent.getActiveQ().remove(WI);
				agent.getCompleteQ().add(WI);
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
			WorkItemEntity completedWI=agent.getCompleteQ().get(i);				
			//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" try to disburse"+completedWI.fullName+"...");
			if (completedWI.precedencyCleared() && completedWI.uppertasksCleared()) {
				//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" disbursed"+completedWI.fullName);
				completedWI.setEnded();	
				agent.getCompleteQ().remove(completedWI);					
				i--;			
			}
		}
	}
	public void EndState() {					
		agent.statusSummary();
	}
//	public void analyzeAggregationNode(AggregationNode aggrNode) {
//		AnalysisActivity analysisActivity = new AnalysisActivity(aggrNode);
//		//
//		@SuppressWarnings("unchecked")
//		Context<Object> context = ContextUtils.getContext(agent);	
//		context.add(analysisActivity);
//		//
//		agent.SoS.arrivedList.put(analysisActivity.getId(), analysisActivity);
//		agent.getBacklogQ().add(analysisActivity);
//		analysisActivity.setAssigned();
//		//System.out.println("\nANALYSIS AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" start analyzing"+aggrNode.fullName);
//	}	
//	public void requestAssistance(DevTask devTask) {
//		ResolutionActivity resolutionActivity = new ResolutionActivity(devTask);
//		devTask.addPredecessorTask(resolutionActivity);
//		//
//		@SuppressWarnings("unchecked")
//		Context<Object> context = ContextUtils.getContext(agent);	
//		context.add(resolutionActivity);
//		//
//		agent.SoS.arrivedList.put(resolutionActivity.getId(), resolutionActivity);
//		agent.getActiveQ().remove(devTask);
//		devTask.withdrawAllResources();
//		agent.getBacklogQ().add(devTask);	
//		agent.getRequestedQ().add(resolutionActivity);
//
//	}
//	public boolean acceptanceDecision(WorkItemEntity requestedWI) {
//		boolean accept = true;		
//		if (agent.getBacklogQ().size()>=agent.BacklogLimit) {
//			if (requestedWI.getRequester().getId() == agent.getId()) {
//				accept = true;
//			}
//			else {
//				accept = false;
//				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to BacklogLimit");
//			}				
//		}
//		if (requestedWI.isAggregationNode) {
//			double eEfficiency = ((AggregationNode)requestedWI).calculateServiceEfficiency(agent);	
//			if (eEfficiency==0) {
//				accept = false;
//				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
//			}
//		}
//		else {
//			double eEfficiency = ((Task)requestedWI).calculateServiceEfficiency(agent);	
//			if (eEfficiency==0) {
//				accept = false;
//				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
//			}
//		}
//		return accept;
//	}	
//	public void requestService(WorkItemEntity newWI, ServiceProviderAgent requestToSP) {
//		requestToSP.getRequestedQ().add(newWI);		
//		newWI.setRequester(agent);
//		//newWI.triggerCausalities();
//		//System.out.println("\nREQUEST SERVICE @TIME:"+SoS.timeNow+" Agent "+this.name+" Requested "+requestToSP.name+" to serve"+newWI.fullName);
//		//myValueManagement.manageValue(this, newWI);
//	}		
//	public void acceptWI(WorkItemEntity requestedWI) {
//		if (!requestedWI.isAggregationNode) {
//			requestedWI.setAssigned(); 
//			requestedWI.setAssignedAgent(agent);
//			//System.out.println("\nACCEPTED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted WI:"+requestedWI.fullName);
//			agent.getBacklogQ().add((Task)requestedWI);		
//			agent.getRequestedQ().remove(requestedWI);
//		}
//		else {
//			requestedWI.setAssigned(); 
//			//System.out.println("\nACCEPTED AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted AggregationNode:"+requestedWI.fullName);
//			this.analyzeAggregationNode((AggregationNode)requestedWI);
//			agent.getComplexQ().add((AggregationNode)requestedWI);								
//			agent.getRequestedQ().remove(requestedWI);
//		}
//	}
//	public void releaseSubtasks (AggregationNode wi) {
//		agent.SoS.myValueFunction.developValue(wi);
//		//-
//		@SuppressWarnings("unchecked")
//		Context<Object> context = ContextUtils.getContext(agent);	
//		//-
//		for (WorkItemEntity subtask: wi.getSubtasks()) {				
//			if (!subtask.isActivated) {
//				subtask.setActivated();				
////					for (WorkItemEntity predecessor : wi.predecessors){
////						for (WorkItemEntity predecessorSub : predecessor.subtasks) {
////							subtask.addPredecessorTask(predecessorSub);
////						}
////					}					
//				context.add(subtask);
//				agent.SoS.arrivedList.put(subtask.getId(), subtask);
//				subtask.setRequester(agent);										
//				agent.getRequestedQ().add(subtask);
//				//System.out.println(wi.fullName+" released subtask "+subtask.getName()+"(id:"+subtask.getId()+")");			
//			}
//			else {
//				//System.out.println(wi.fullName+"'s subtask"+subtask.fullName+"already activated");	
//			}
//		}
//	}
//	
//	public LinkedList<ServiceProviderAgent> findServiceProviders(WorkItemEntity wItem) {
//		int wItem_reqService_id = wItem.serviceId;
//		LinkedList<ServiceProviderAgent> serviceProviderCandidates = new LinkedList<ServiceProviderAgent>();
//		for (ServiceProviderAgent tAgent: agent.assignWITo) {
//			for (ResourceEntity sResource: tAgent.getMyResourceEntities()) {
//				for (Skill sResourceSkill: sResource.getSkillSet()) {
//					int sResource_Service_id = sResourceSkill.getService().getId();
//					if (sResource_Service_id==wItem_reqService_id) {
//						if (!serviceProviderCandidates.contains(tAgent)) {
//							serviceProviderCandidates.add(tAgent);
//							//System.out.println(" candidate for "+this.name+" to Assign "+wItem.getName()+" :"+tAgent.name);								
//						}
//						break;
//					}
//				}
//			}
//		}
//		return serviceProviderCandidates;
//	}
//	public ArrayList<ResourceEntity> findResourceEntities(WorkItemEntity wItem) {
//		int wItem_reqService_id = wItem.serviceId;
//		ArrayList<ResourceEntity> serviceResourceCandidates = new ArrayList<ResourceEntity>(0);
//		for (ResourceEntity sResource: agent.getMyResourceEntities()) {
//			for (Skill sResourceSkill: sResource.getSkillSet()) {
//				int sResource_Service_id = sResourceSkill.getService().getId();
//				if (sResource_Service_id==wItem_reqService_id) {
//					serviceResourceCandidates.add(sResource);
//				}
//			}
//		}	
//		return serviceResourceCandidates;
//	}

}
