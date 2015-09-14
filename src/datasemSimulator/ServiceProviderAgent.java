package datasemSimulator;

import governanceModels.AgentStrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
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
		protected int backlogQLimit = Integer.MAX_VALUE;
		protected int readyQLimit = Integer.MAX_VALUE;
		protected int activeQLimit = Integer.MAX_VALUE;
		// Dynamic Attributes
		protected double totalWorkload;
		protected double activeWorkload;
		protected double resourceUtilization;
		protected LinkedList<WorkItemEntity> requestedQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> assignmentQ = new LinkedList<WorkItemEntity>();	
		protected LinkedList<WorkItemEntity> backlogQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> readyQ = new LinkedList<WorkItemEntity>();	
		protected LinkedList<WorkItemEntity> activeQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> complexQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> completeQ = new LinkedList<WorkItemEntity>();
		protected LinkedList<WorkItemEntity> coordinateQ = new LinkedList<WorkItemEntity>();
			
		private static final int BASE_PRIORITY_1 = 200;
		private static final int SEQUENCE_CheckRequestedQ = 10;
		private static final int SEQUENCE_CheckBacklogQ = 20;
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
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_CheckRequestedQ,shuffle=false)
		public void CheckRequestedQ() {
			//System.out.println("\nAgent "+this.name+" checkRequestedQ");
			//this.requestedQ = this.myStrategy.workPrioritization(this, this.requestedQ);	
			//for (WorkItemEntity requestedWI:this.requestedQ) {
				//myValueManagement.manageValue(this, requestedWI);
			//}
			// ------------ 1. Select WIs to Accept
			while (!this.requestedQ.isEmpty()) {
				// =========== Apply WI Acceptance Rule ====================
				WorkItemEntity requestedWI = this.requestedQ.get(RandomHelper.nextIntFromTo(0, requestedQ.size()-1));			
				// =========== Service Efficiency Algorithm ==============
				double eEfficiency = requestedWI.calculateServiceEfficiency(this);	
				if (eEfficiency==0) {
					ArrayList<ServiceProviderAgent>serviceProviderCandidates = this.findServiceProviders(requestedWI);
					if	(serviceProviderCandidates.size()!=0) {
						// Apply WI Assignment Rule
						ServiceProviderAgent selectedSP = serviceProviderCandidates.get(RandomHelper.nextIntFromTo(0, serviceProviderCandidates.size()-1));
						// Assign WI to other SP							
						this.requestService(requestedWI, selectedSP);
						this.requestedQ.remove(requestedWI);
						//selectedSP.checkRequestedQ();
					}
					else {
						System.out.println("Failed to Assign"+requestedWI.fullName); 
						System.out.println("ERROR!");
						System.exit(0);
					}
				}
				else {	
					this.acceptWI(requestedWI);
				}
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_CheckBacklogQ)
		public void CheckBacklogQ() {
			//System.out.println("Agent "+this.name+" checkBacklogQ");
			for (int i=0;i<this.backlogQ.size();i++) {
				WorkItemEntity wi = this.backlogQ.get(i);
				if (wi.precedencyCleared()) {					
					this.readyQ.add(wi);					
					this.backlogQ.remove(wi);
					i--;
				}
			}
		}
		@ScheduledMethod(start=1,interval=1,priority=BASE_PRIORITY_1-SEQUENCE_SelectWIsToStart)
		public void SelectWIsToStart() {
			//System.out.println("Agent "+this.name+" selectWIsToStart");
			//for (int i=0;i<this.readyQ.size();i++) {
				//WorkItemEntity readyWI = this.readyQ.get(i);
				//myValueManagement.manageValue(this, readyWI);
			//}
			//this.readyQ = this.myStrategy.workPrioritization(this, readyQ);
			for (int w=0;w<this.readyQ.size();w++) {			
				// =========== Apply WI Selection Rule ====================
				WorkItemEntity startedWI = this.readyQ.get(RandomHelper.nextIntFromTo(0, readyQ.size()-1));				
				// ========================================================
				ArrayList<ResourceEntity> serviceResourceCandidates = this.findResourceEntities(startedWI);
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
					selectedSR.allocateTo(startedWI);				
				// ========================================================
					startedWI.setStarted();					
					double rEfficiency = startedWI.calculateResourceEfficiency();	
					startedWI.serviceEfficiency = rEfficiency;
				// =========== Estimate Completion ====================				
					double eEfforts = startedWI.getEfforts()/rEfficiency;
					startedWI.estimatedEfforts = eEfforts;
					double eCompletion= eEfforts + this.SoS.timeNow;
					startedWI.estimatedCompletionTime = eCompletion;
					// ====================================================
					this.activeQ.add(startedWI);
					this.readyQ.remove(startedWI);				
					w--;
				}
				else {
					//System.out.println("No Resources available for "+startedWI.getPatternType().getName()+": "+startedWI.getName()+" now!");				
				}
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
		}
		public void createAnalysisTask(WorkItemEntity aggrNode) {
			WorkItemEntity analysisTask = new WorkItemEntity(aggrNode);
			this.backlogQ.add(analysisTask);
			//System.out.println("\nANALYSIS AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" start analyzing"+aggrNode.fullName);
		}
		public void requestService(WorkItemEntity newWI, ServiceProviderAgent requestToSP) {
			requestToSP.requestedQ.add(newWI);		
			newWI.requester = this;
			//newWI.triggerCausalities();
			//System.out.println(this.name+" Requested "+requestToSP.name+" to serve"+newWI.fullName);
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
			for (WorkItemEntity subtask: wi.subtasks) {
				//-
				@SuppressWarnings("unchecked")
				Context<Object> context = ContextUtils.getContext(this);	
				//-
				if (!subtask.isActivated) {
					subtask.setActivated();
					context.add(subtask);
					this.requestedQ.add(subtask);  
					//System.out.println(wi.fullName+" released subtask "+subtask.getName()+"(id:"+subtask.getId()+")");			
				}
				else {
					//System.out.println(wi.fullName+"'s subtask"+subtask.fullName+"already activated");	
				}
			}
		}
		
		public ArrayList<ServiceProviderAgent> findServiceProviders(WorkItemEntity wItem) {
			int wItem_reqService_id = wItem.getRequiredServices().get(0).getId();
			ArrayList<ServiceProviderAgent> serviceProviderCandidates = new ArrayList<ServiceProviderAgent>(0);
			for (ServiceProviderAgent tAgent: this.assignWITo) {
				for (ResourceEntity sResource: tAgent.myResourceEntities) {
					for (Skill sResourceSkill: sResource.getSkillSet()) {
						int sResource_Service_id = sResourceSkill.getService().getId();
						if (sResource_Service_id==wItem_reqService_id) {
							serviceProviderCandidates.add(tAgent);
							//System.out.println(" candidate for "+this.name+" to Assign "+wItem.getName()+" :"+tAgent.name);
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
			this.totalWorkload = 0;
			this.activeWorkload = 0;
			for (int i=0;i<this.backlogQ.size();i++) {
				WorkItemEntity workItem = this.backlogQ.get(i);
				this.totalWorkload += workItem.getEfforts()*(1-workItem.progress);
			}
			for (int i=0;i<this.readyQ.size();i++) {
				WorkItemEntity workItem = this.readyQ.get(i);
				this.totalWorkload += workItem.getEfforts()*(1-workItem.progress);
			}
			for (int i=0;i<this.activeQ.size();i++) {
				WorkItemEntity workItem = this.activeQ.get(i);
				this.totalWorkload += workItem.getEfforts()*(1-workItem.progress);
				this.activeWorkload += workItem.getEfforts()*(1-workItem.progress);
			}
		}
		public void calculateResourceUtilization() {
			int numResources = this.myResourceEntities.size();
			int numActiveResources = 0;
			for (ResourceEntity serviceResource: this.myResourceEntities) {
				if (serviceResource.isBusy()) {
					numActiveResources += 1;
				}
			}
			this.resourceUtilization = (double)numActiveResources/(double)numResources;	
		}
		

		
}
