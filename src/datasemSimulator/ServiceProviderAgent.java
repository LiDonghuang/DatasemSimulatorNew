package datasemSimulator;

import governanceModels.AgentStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.ServiceProvider;
import xtext.objectsModel.Skill;
import xtext.objectsModel.impl.ServiceProviderImpl;


public class ServiceProviderAgent extends ServiceProviderImpl {
		public SystemOfSystems SoS;
		// Static Attributes
		public ServiceProvider myServiceProvider;
		public AgentStrategy myStrategy;
		protected LinkedList<ResourceEntity> myResourceEntities = new LinkedList<ResourceEntity>();
		protected LinkedList<ServiceProviderAgent> assignWITo = new LinkedList<ServiceProviderAgent>();
		protected LinkedList<ServiceProviderAgent> borrowResourceFrom = new LinkedList<ServiceProviderAgent>();
		public int WIPLimit = Integer.MAX_VALUE;
		public int BacklogLimit = Integer.MAX_VALUE;
		// Dynamic Attributes
		protected double TotalWorkload;
		protected double ActiveWorkload;
		protected double ResourceUtilization;
		protected int isBottleNeck;		
		protected int BottleNeckCount;
		// Time Series Records
		private List<Double> recordTotalWorkload = new ArrayList<Double>();
		private List<Double> recordActiveWorkload = new ArrayList<Double>();
		private List<Double> recordResourceUtilization = new ArrayList<Double>();
		// End Run Statistics
		private double TotalWorkLoad_mean;
		private double TotalWorkLoad_stdev;
		private double ActiveWorkload_mean;
		private double ActiveWorkload_stdev;
		private double ResourceUtilization_mean;
		private double ResourceUtilization_stdev;
		
		protected LinkedList<WorkItemEntity> requestedQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> assignmentQ = new LinkedList<WorkItemEntity>();	
		protected LinkedList<WorkItemEntity> backlogQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> activeQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> complexQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> completeQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> coordinateQ = new LinkedList<WorkItemEntity>();
			
		private static final int BASE_PRIORITY_1 = 200;
		private static final int SEQUENCE_CheckRequestedQ = 10;
		private static final int SEQUENCE_MakeAssignments = 20;
		private static final int SEQUENCE_SelectWIsToStart = 30;
		private static final int SEQUENCE_AdvanceWIsProgress = 40;
		private static final int SEQUENCE_TriggerWIsChanges = 50;
		private static final int SEQUENCE_CheckWIsCompletion = 60;
		private static final int SEQUENCE_CheckAggregationNodesCompletion = 70;
		private static final int SEQUENCE_DisburseWIs = 80;
		
		
		public ServiceProviderAgent(ServiceProvider ServiceProvider) {
			this.myServiceProvider = ServiceProvider;
			this.name = ServiceProvider.getName();
			this.id = ServiceProvider.getId();
			new AgentStrategy().implementAgentStrategy(this, ServiceProvider.getGovernanceStrategy());			
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_CheckRequestedQ)
		public void CheckRequestedQ() {
			requestedQ = this.myStrategy.applyWorkPrioritization(requestedQ);
			//for (WorkItemEntity requestedWI:this.requestedQ) {
				//myValueManagement.manageValue(this, requestedWI);
			//}
			// ------------ 1. Select WIs to Accept
			while (!requestedQ.isEmpty()) {
				// =========== Apply WI Acceptance Rule ====================
				WorkItemEntity requestedWI = requestedQ.getFirst();			
				// =========== Service Efficiency Algorithm ==============
				if (!this.acceptanceDecision(requestedWI)){
					assignmentQ.add(requestedWI);
					requestedQ.remove(requestedWI);					
				}
				else {	
					this.acceptWI(requestedWI);
				}
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_MakeAssignments)
		public void MakeAssignments() {
			assignmentQ = this.myStrategy.applyWorkPrioritization(assignmentQ);
			for (int i=0;i<assignmentQ.size();i++) {
				WorkItemEntity wi = assignmentQ.get(i);
				LinkedList<ServiceProviderAgent>serviceProviderCandidates = this.findServiceProviders(wi);				
				if	(serviceProviderCandidates.size()!=0) {
					// Apply WI Assignment Rule
					serviceProviderCandidates = this.myStrategy.applyContractorSelection(serviceProviderCandidates);
					ServiceProviderAgent selectedSP = serviceProviderCandidates.getFirst();
					// Assign WI to other SP
					this.requestService(wi, selectedSP);
					this.assignmentQ.remove(wi);
					i--;
					//selectedSP.checkRequestedQ();
				}
//				else {
//					System.out.println("Failed to Assign"+wi.fullName); 
//					System.out.println("ERROR!");
//					System.exit(0);
//				}
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_SelectWIsToStart)
		public void SelectWIsToStart() {
			LinkedList<WorkItemEntity> readyQ = new LinkedList<WorkItemEntity>();		
			readyQ = this.myStrategy.applyWorkPrioritization(backlogQ);
			for (int i=0;i<readyQ.size();i++) {			
				// =========== Apply WI Selection Rule ====================
				WorkItemEntity wi = readyQ.get(i);		
				if (wi.precedencyCleared()) {
					// ========================================================
					ArrayList<ResourceEntity> serviceResourceCandidates = this.findResourceEntities(wi);
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
						wi.serviceEfficiency = rEfficiency;
					// =========== Estimate Completion ====================				
						double eEfforts = wi.getEfforts()/rEfficiency;
						wi.estimatedEfforts = eEfforts;
						double eCompletion= eEfforts + this.SoS.timeNow;
						wi.estimatedCompletionTime = eCompletion;
						// ====================================================
						this.activeQ.add(wi);
						backlogQ.remove(wi);
					}
//					else {
//						System.out.println("No Resources available for "+startedWI.getPatternType().getName()+": "+startedWI.getName()+" now!");				
//					}
				}
//				else {
//					System.out.println("Decide Not to Start"+wi.fullName+"due to Precedency");
//				}
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_AdvanceWIsProgress)	
		public void AdvanceWIsProgress() {
			//System.out.println("Agent "+this.name+" checkWIsCompletion");
			for(int i=0;i<activeQ.size();i++) {
				WorkItemEntity WI = activeQ.get(i);
				WI.advanceProgress();
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_TriggerWIsChanges)	
		public void TriggerWIsChanges() {
			//System.out.println("Agent "+this.name+" checkWIsCompletion");
			for(int i=0;i<activeQ.size();i++) {
				WorkItemEntity WI = activeQ.get(i);
				WI.triggerChanges();
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_CheckWIsCompletion)	
		public void CheckWIsCompletion() {
			//System.out.println("Agent "+this.name+" checkWIsCompletion");
			for(int i=0;i<activeQ.size();i++) {
				WorkItemEntity WI = activeQ.get(i);
				if (WI.progress>=1.00) {
					WI.setCompleted();
					for (ResourceEntity r:WI.allocatedResources) {
						r.withdrawFrom(WI);
					}
					if (WI.getType().getId()==7) {
						//System.out.println("\nCOMPLETED ANALYSIS @TIME:"+SoS.timeNow+" Agent "+this.name+" completed analyzing"+WI.AnalysisObject.fullName);
						this.releaseSubtasks(WI.AnalysisObject);						
					}
					activeQ.remove(WI);
					completeQ.add(WI);
					i--;
				}
			}
		}		
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_CheckAggregationNodesCompletion)	
		public void CheckAggregationNodesCompletion() {
			for(int i=0;i<complexQ.size();i++) {
				WorkItemEntity aggrWI = complexQ.get(i);
				aggrWI.updateUpperTasksCompletion();
				if (aggrWI.isCompleted) {
					complexQ.remove(aggrWI);
					completeQ.add(aggrWI);
					i--;
				}
			}			
		}		
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_DisburseWIs)
		public void DisburseWIs() {			
			for (int i=0;i<completeQ.size();i++) {
				WorkItemEntity completedWI=completeQ.get(i);				
				//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" try to disburse"+completedWI.fullName+"...");
				if (completedWI.precedencyCleared() && completedWI.uppertasksCleared()) {
					//System.out.println("\nDISBURSE @TIME:"+SoS.timeNow+" Agent "+this.name+" disbursed"+completedWI.fullName);
					completedWI.setEnded();	
					completeQ.remove(completedWI);					
					i--;			
				}
			}
			this.statusSummary();
		}
		public void createAnalysisTask(WorkItemEntity aggrNode) {
			WorkItemEntity analysisTask = new WorkItemEntity(aggrNode);
			//this.backlogQ.add(analysisTask);
			this.activeQ.add(analysisTask);
			analysisTask.progress=1;
			analysisTask.AnalysisObject.setStarted();
			//System.out.println("\nANALYSIS AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" start analyzing"+aggrNode.fullName);
		}	
		public boolean acceptanceDecision(WorkItemEntity requestedWI) {
			boolean accept = true;			
			if (backlogQ.size()>=BacklogLimit) {
				if (requestedWI.requester.id == this.id) {
					accept = true;
				}
				else {
					accept = false;
					//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to BacklogLimit");
				}				
			}
			double eEfficiency = requestedWI.calculateServiceEfficiency(this);	
			if (eEfficiency==0) {
				accept = false;
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
			}
			return accept;
		}	
		public void requestService(WorkItemEntity newWI, ServiceProviderAgent requestToSP) {
			requestToSP.requestedQ.add(newWI);		
			newWI.requester = this;
			//newWI.triggerCausalities();
			//System.out.println("\nREQUEST SERVICE @TIME:"+SoS.timeNow+" Agent "+this.name+" Requested "+requestToSP.name+" to serve"+newWI.fullName);
			//myValueManagement.manageValue(this, newWI);
		}		
		public void acceptWI(WorkItemEntity requestedWI) {
			if (!requestedWI.isIsAggregationNode()) {
				requestedWI.setAssigned(); 
				requestedWI.assignedAgent = this;
				//System.out.println("\nACCEPTED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted WI:"+requestedWI.fullName);
				this.backlogQ.add(requestedWI);								
				this.requestedQ.remove(requestedWI);
			}
			else {
				requestedWI.setAssigned(); 
				//System.out.println("\nACCEPTED AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted AggregationNode:"+requestedWI.fullName);
				this.createAnalysisTask(requestedWI);
				this.complexQ.add(requestedWI);								
				this.requestedQ.remove(requestedWI);
			}
		}
		public void releaseSubtasks (WorkItemEntity wi) {
			//-
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);	
			//-
			for (WorkItemEntity subtask: wi.subtasks) {				
				if (!subtask.isActivated) {
					subtask.setActivated();				
//					for (WorkItemEntity predecessor : wi.predecessors){
//						for (WorkItemEntity predecessorSub : predecessor.subtasks) {
//							subtask.addPredecessorTask(predecessorSub);
//						}
//					}					
					context.add(subtask);
					subtask.requester = this;										
					this.requestedQ.add(subtask);
					//System.out.println(wi.fullName+" released subtask "+subtask.getName()+"(id:"+subtask.getId()+")");			
				}
				else {
					//System.out.println(wi.fullName+"'s subtask"+subtask.fullName+"already activated");	
				}
			}
		}
		
		public LinkedList<ServiceProviderAgent> findServiceProviders(WorkItemEntity wItem) {
			int wItem_reqService_id = wItem.getRequiredServices().get(0).getId();
			LinkedList<ServiceProviderAgent> serviceProviderCandidates = new LinkedList<ServiceProviderAgent>();
			for (ServiceProviderAgent tAgent: this.assignWITo) {
				for (ResourceEntity sResource: tAgent.myResourceEntities) {
					for (Skill sResourceSkill: sResource.getSkillSet()) {
						int sResource_Service_id = sResourceSkill.getService().getId();
						if (sResource_Service_id==wItem_reqService_id) {
							if (!serviceProviderCandidates.contains(tAgent)) {
								serviceProviderCandidates.add(tAgent);
								//System.out.println(" candidate for "+this.name+" to Assign "+wItem.getName()+" :"+tAgent.name);								
							}
							break;
						}
					}
				}
			}
			return serviceProviderCandidates;
		}
		public ArrayList<ResourceEntity> findResourceEntities(WorkItemEntity wItem) {
			int wItem_reqService_id = wItem.getRequiredServices().get(0).getId();
			ArrayList<ResourceEntity> serviceResourceCandidates = new ArrayList<ResourceEntity>(0);
			for (ResourceEntity sResource: this.myResourceEntities) {
				for (Skill sResourceSkill: sResource.getSkillSet()) {
					int sResource_Service_id = sResourceSkill.getService().getId();
					if (sResource_Service_id==wItem_reqService_id) {
						serviceResourceCandidates.add(sResource);
					}
				}
			}	
			return serviceResourceCandidates;
		}
		public boolean hasIdleResources() {
			boolean hasIdleResources = false;
			for (int r=0;r<this.myResourceEntities.size();r++) {
				ResourceEntity serviceResource = this.myResourceEntities.get(r);
				if (!serviceResource.isBusy()) {
					hasIdleResources = true;
					break;
				}
			}
			return hasIdleResources;
		}		
		
		public void statusSummary() {
			this.calculateWorkload();
			this.calculateResourceUtilization();
			this.countBottleNeck();
//			if (this.activeQ.size()>0) {
//				this.state=1;
//				if (this.resourceUtilization==1.00) {
//				} 
//				}
//			else {
//				this.state=0;
//			}		
		}
		public void calculateWorkload() {
			this.TotalWorkload = 0;
			this.ActiveWorkload = 0;
			for (int i=0;i<this.backlogQ.size();i++) {
				WorkItemEntity workItem = this.backlogQ.get(i);
				this.TotalWorkload += workItem.getEfforts()*(1-workItem.progress);
			}
			for (int i=0;i<this.activeQ.size();i++) {
				WorkItemEntity workItem = this.activeQ.get(i);
				this.TotalWorkload += workItem.getEfforts()*(1-workItem.progress);
				this.ActiveWorkload += workItem.getEfforts()*(1-workItem.progress);
			}
			this.recordTotalWorkload.add(this.TotalWorkload);
			this.recordActiveWorkload.add(this.ActiveWorkload);
		}
		public void calculateResourceUtilization() {
			int numResources = this.myResourceEntities.size();
			int numActiveResources = 0;
			for (ResourceEntity serviceResource: this.myResourceEntities) {
				if (serviceResource.isBusy()) {
					numActiveResources += 1;
				}
			}
			this.ResourceUtilization = (double)numActiveResources/(double)numResources;	
			this.recordResourceUtilization.add(this.ResourceUtilization);
		}
		public LinkedList<WorkItemEntity> getRequestedQ()	{
			return this.requestedQ;
		}
		public LinkedList<WorkItemEntity> getBacklogQ()	{
			return this.backlogQ;
		}
		public int getTotalWICount() {
			int load = this.requestedQ.size()+this.activeQ.size()+this.backlogQ.size();
			return load;
		}		
		public double getActiveWorkload() {
			return this.ActiveWorkload;
		}
		public double getTotalWorkload() {
			return this.TotalWorkload;
		}
		public double getResourceUtilization() {
			return this.ResourceUtilization;
		}
		public int checkBottleNeck() {
			int a = this.isBottleNeck;
			this.isBottleNeck = 0;
			return a;
		}		
		public void countBottleNeck() {
			if (this.backlogQ.size()>0 && this.ResourceUtilization<1) {
				for (WorkItemEntity wi : this.backlogQ) {
					if (!wi.precedencyCleared() && this.findResourceEntities(wi).size()>0) {
						this.isBottleNeck = 1;
						this.BottleNeckCount ++;
						//System.out.println(this.name+" is BottleNecked!! (for the "+this.BottleNeckCount+" th time)");						
						break;
					}
				}
			}
		}				
		public void EndRunStatistics() {
			this.TotalWorkLoad_mean = StatMean(recordTotalWorkload);
			this.TotalWorkLoad_stdev = StatStdev(recordTotalWorkload);
			this.ActiveWorkload_mean = StatMean(recordActiveWorkload);
			this.ActiveWorkload_stdev = StatStdev(recordActiveWorkload);
			this.ResourceUtilization_mean = StatMean(recordResourceUtilization);
			this.ResourceUtilization_stdev = StatStdev(recordResourceUtilization);
		}
		public int getBottleNectCount() {
			return this.BottleNeckCount;
		}
		public double getTotalWorkLoad_mean() {
			return this.TotalWorkLoad_mean;
		}
		public double getTotalWorkLoad_stdev() {
			return this.TotalWorkLoad_stdev;
		}
		public double getActiveWorkload_mean() {
			return this.ActiveWorkload_mean;
		}
		public double getActiveWorkload_stdev() {
			return this.ActiveWorkload_stdev;
		}
		public double getResourceUtilization_mean() {
			return this.ResourceUtilization_mean;
		}
		public double getResourceUtilization_stdev() {
			return this.ResourceUtilization_stdev;
		}
		private double StatMean(List<Double> list) {
			double[] numbers = new double[list.size()];
			double value = 0;
			for (int i=0;i<list.size();i++) {
				numbers[i] = list.get(i);
			}
			value = StatUtils.mean(numbers);
			return value;
		}
		private double StatStdev(List<Double> list) {
			double[] numbers = new double[list.size()];
			double value = 0;
			for (int i=0;i<list.size();i++) {
				numbers[i] = list.get(i);
			}
			value = FastMath.sqrt(StatUtils.variance(numbers));
			return value;
		}
}
