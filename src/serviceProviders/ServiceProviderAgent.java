package serviceProviders;

import governanceModels.AgentStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import contractNetProtocol.AbstractAgentBehavior;
import datasemSimulator.SystemOfSystems;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import visualization.Icon;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.ResolutionActivity;
import workItems.DevTask;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Service;
import xtext.objectsModel.ServiceProvider;
import xtext.objectsModel.Skill;
import xtext.objectsModel.impl.ServiceProviderImpl;


public class ServiceProviderAgent extends ServiceProviderImpl {

	public SystemOfSystems SoS;
	// Static Attributes
	public ServiceProvider myServiceProvider;
	public int typeId;
	public int hierarchy = 0;
	//***
	public AbstractAgentBehavior myBehavior;
	//***
	public AgentStrategy myStrategy;
	private LinkedList<ResourceEntity> myResourceEntities = new LinkedList<ResourceEntity>();
	public LinkedList<ServiceProviderAgent> assignWITo = new LinkedList<ServiceProviderAgent>();
	public LinkedList<ServiceProviderAgent> borrowResourceFrom = new LinkedList<ServiceProviderAgent>();
	public int WIPLimit = Integer.MAX_VALUE;
	public int BacklogLimit = Integer.MAX_VALUE;		
	private LinkedList<WorkItemEntity> requestedQ = new LinkedList<WorkItemEntity>();
	private LinkedList<WorkItemEntity> assignmentQ = new LinkedList<WorkItemEntity>();	
	private LinkedList<Task> backlogQ = new LinkedList<Task>();
	private LinkedList<Task> activeQ = new LinkedList<Task>();
	private LinkedList<AggregationNode> complexQ = new LinkedList<AggregationNode>();
	private LinkedList<WorkItemEntity> completeQ = new LinkedList<WorkItemEntity>();
	public LinkedList<WorkItemEntity> tempQ = new LinkedList<WorkItemEntity>();
	public HashMap<Service,Double> ServiceCapacity = new HashMap<Service,Double>();
	public HashMap<Service,Double> ExtendedServiceCapacity = new HashMap<Service,Double>();
	
	private static final int BASE_PRIORITY_1 = 200;
	private static final int SEQUENCE_CheckRequestedQ = 10;
	private static final int SEQUENCE_MakeAssignments = 20;
	private static final int SEQUENCE_SelectWIsToStart = 30;
	private static final int SEQUENCE_AdvanceWIsProgress = 40;
	private static final int SEQUENCE_TriggerWIsChanges = 50;
	private static final int SEQUENCE_CheckWIsCompletion = 60;
	private static final int SEQUENCE_CheckAggregationNodesCompletion = 70;
	private static final int SEQUENCE_DisburseWIs = 80;
	
	
	// Visualization
	public Icon icon = new Icon();
	// Dynamic Attributes
	public LinkedList<ServiceProviderAgent> NowRequested = new LinkedList<ServiceProviderAgent>();
	private double TotalWorkload;
	private double ActiveWorkload;
	private double ResourceUtilization;
	private int isBottleNeck;		
	private int BottleNeckCount;
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
			
	public ServiceProviderAgent(ServiceProvider ServiceProvider) {
		this.myServiceProvider = ServiceProvider;
		this.name = ServiceProvider.getName();
		this.id = ServiceProvider.getId();
		this.typeId = ServiceProvider.getType().getId();
		this.hierarchy = ServiceProvider.getType().getHierarchy();
		this.myStrategy = new AgentStrategy(ServiceProvider.getGovernanceStrategy());
		this.myBehavior = new AbstractAgentBehavior();this.myBehavior.setAgent(this);
		this.BacklogLimit = 20;
		this.WIPLimit = 10;
	}
	
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-1)
	public void step_1() {
		myBehavior.CheckRequestedQ();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-2)
	public void step_2() {
		myBehavior.MakeAssignments();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-3)
	public void step_3() {
		myBehavior.SelectWIsToStart();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-4)	
	public void step_4() {
		myBehavior.AdvanceWIsProgress();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-5)	
	public void step_5() {
		myBehavior.TriggerWIsChanges();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-6)	
	public void step_6() {
		myBehavior.CheckWIsCompletion();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-7)	
	public void step_7() {
		myBehavior.CheckAggregationNodesCompletion();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-8)	
	public void step_8() {
		myBehavior.DisburseWIs();
	}
	@ScheduledMethod(start=1,interval=1,shuffle=false,priority=BASE_PRIORITY_1-9)	
	public void step_9() {
		myBehavior.EndState();
	}
	
	

	
	public void analyzeAggregationNode(AggregationNode aggrNode) {
		AnalysisActivity analysisActivity = new AnalysisActivity(aggrNode);
		//
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);	
		context.add(analysisActivity);
		//
		SoS.arrivedList.put(analysisActivity.getId(), analysisActivity);
		this.backlogQ.add(analysisActivity);
		analysisActivity.setAssigned();
		//System.out.println("\nANALYSIS AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" start analyzing"+aggrNode.fullName);
	}	
	public void requestAssistance(DevTask devTask) {
		ResolutionActivity resolutionActivity = new ResolutionActivity(devTask);
		devTask.addPredecessorTask(resolutionActivity);
		//
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);	
		context.add(resolutionActivity);
		//
		SoS.arrivedList.put(resolutionActivity.getId(), resolutionActivity);
		this.activeQ.remove(devTask);
		devTask.withdrawAllResources();
		this.backlogQ.add(devTask);	
		devTask.getRequester().requestedQ.add(resolutionActivity);

	}
	public String acceptanceDecision(WorkItemEntity requestedWI) {
		String decision = "Decline";
		
		if (requestedWI.isAggregationNode) {
			double capacity = ((AggregationNode)requestedWI).calculateServiceCapacity(this);	
			if (capacity>0) {
				decision = "Accept";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
			}
			else {
				double exCapacity = ((AggregationNode)requestedWI).calculateExtendedServiceCapacity(this);	
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					if (requestedWI.getRequester().id == this.id) {
						decision = "RequestHelp";
					}
					else {
						decision = "Decline";
					}
				}
			}
		}
		else if (requestedWI.isResolutionActivity) {
			double capacity = ((Task)requestedWI).calculateServiceCapacity(this);	
			if (capacity>0) {
				decision = "Accept";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
			}
			else {
				double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(this);	
				if (exCapacity>0) {
					decision = "Outsource";
				}
				else {
					decision = "RequestHelp";
				}
			}
		}
		else {
			if (requestedWI.getRequester().id == this.id) {
				double capacity = ((Task)requestedWI).calculateServiceCapacity(this);
				if (capacity>0) {
					decision = "Accept";
					//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
				}
				else {
					double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(this);	
					if (exCapacity>0) {
						decision = "Outsource";
					}
					else {
						decision = "RequestHelp";
					}
				}
			}
			else if (backlogQ.size()>=BacklogLimit) {
					decision = "Decline";
				//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to BacklogLimit");		
			}
			else {
				double capacity = ((Task)requestedWI).calculateServiceCapacity(this);
				if (capacity>0) {
					decision = "Accept";
					//System.out.println("\nDELINED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Declined WI:"+requestedWI.fullName+"due to Inability");
				}
				else {
					double exCapacity = ((Task)requestedWI).calculateExtendedServiceCapacity(this);	
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
	public void requestService(WorkItemEntity newWI, ServiceProviderAgent requestToSP) {
		requestToSP.requestedQ.add(newWI);		
		newWI.setRequester(this);
		//newWI.triggerCausalities();
		//System.out.println("\nREQUEST SERVICE @TIME:"+SoS.timeNow+" Agent "+this.name+" Requested "+requestToSP.name+" to serve"+newWI.fullName);
		//myValueManagement.manageValue(this, newWI);
	}		
	public void acceptWI(WorkItemEntity requestedWI) {
		if (!requestedWI.isAggregationNode) {
			requestedWI.setAssigned(); 
			requestedWI.setAssignedAgent(this);
			//System.out.println("\nACCEPTED WI @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted WI:"+requestedWI.fullName);
			this.backlogQ.add((Task)requestedWI);		
			//this.requestedQ.remove(requestedWI);
		}
		else {
			requestedWI.setAssigned(); 
			//System.out.println("\nACCEPTED AGGREGATION NODE @TIME:"+SoS.timeNow+" Agent "+this.name+" Accepted AggregationNode:"+requestedWI.fullName);
			this.analyzeAggregationNode((AggregationNode)requestedWI);
			this.complexQ.add((AggregationNode)requestedWI);								
			//this.requestedQ.remove(requestedWI);
		}
	}
	public void releaseSubtasks (AggregationNode wi) {		
		//-
		@SuppressWarnings("unchecked")
		Context<Object> context = ContextUtils.getContext(this);	
		//-
		for (WorkItemEntity subtask: wi.getSubtasks()) {				
			if (!subtask.isActivated) {
				subtask.setActivated();				
//					for (WorkItemEntity predecessor : wi.predecessors){
//						for (WorkItemEntity predecessorSub : predecessor.subtasks) {
//							subtask.addPredecessorTask(predecessorSub);
//						}
//					}					
				context.add(subtask);
				SoS.arrivedList.put(subtask.getId(), subtask);
				subtask.setRequester(wi.getRequester());
				this.requestedQ.add(subtask);
				//System.out.println(wi.fullName+" released subtask "+subtask.getName()+"(id:"+subtask.getId()+")");			
			}
			else {
				//System.out.println(wi.fullName+"'s subtask"+subtask.fullName+"already activated");	
			}
		}
	}
	
	public LinkedList<ServiceProviderAgent> findServiceProviders(WorkItemEntity wItem,LinkedList<ServiceProviderAgent> SPs) {
		//int wItem_reqService_id = wItem.serviceId;
		LinkedList<ServiceProviderAgent> list = new LinkedList<ServiceProviderAgent>();
		list.addAll(SPs);
		Service reqService = SoS.myServices.get(wItem.serviceId);
		LinkedList<ServiceProviderAgent> candidates = new LinkedList<ServiceProviderAgent>();
		for (ServiceProviderAgent tAgent: list) {
			double sCapacity = tAgent.ExtendedServiceCapacity.get(reqService);
			if (sCapacity > 0) {
				candidates.add(tAgent);
				//System.out.println(" candidate for "+this.name+" to Assign "+wItem.getName()+" :"+tAgent.name);								
			}
		}
		return candidates;
	}
	public ArrayList<ResourceEntity> findResourceEntities(WorkItemEntity wItem) {
		int wItem_reqService_id = wItem.serviceId;
		ArrayList<ResourceEntity> serviceResourceCandidates = new ArrayList<ResourceEntity>(0);
		for (ResourceEntity sResource: this.getMyResourceEntities()) {
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
		for (int r=0;r<this.getMyResourceEntities().size();r++) {
			ResourceEntity serviceResource = this.getMyResourceEntities().get(r);
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
		//this.countBottleNeck();
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
			this.TotalWorkload += workItem.efforts*(1-workItem.getProgress());
		}
		for (int i=0;i<this.activeQ.size();i++) {
			WorkItemEntity workItem = this.activeQ.get(i);
			this.TotalWorkload += workItem.efforts*(1-workItem.getProgress());
			this.ActiveWorkload += workItem.efforts*(1-workItem.getProgress());
		}
		this.recordTotalWorkload.add(this.TotalWorkload);
		this.recordActiveWorkload.add(this.ActiveWorkload);
	}
	public void calculateResourceUtilization() {
		int numResources = this.getMyResourceEntities().size();
		int numActiveResources = 0;
		for (ResourceEntity serviceResource: this.getMyResourceEntities()) {
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
	public LinkedList<Task> getBacklogQ()	{
		return this.backlogQ;
	}
	public LinkedList<Task> getActiveQ()	{
		return this.activeQ;
	}
	public LinkedList<WorkItemEntity> getCompleteQ()	{
		return this.completeQ;
	}
	public LinkedList<AggregationNode> getComplexQ()	{
		return this.complexQ;
	}
	public LinkedList<WorkItemEntity> getAssignmentQ()	{
		return this.assignmentQ;
	}
	public int getTotalWICount() {
		int load = this.requestedQ.size()+this.activeQ.size()+this.backlogQ.size();
		return load;
	}
	public double estimateWorkLoad() {
		double load = 0;
		
		LinkedList<WorkItemEntity> scope = new LinkedList<WorkItemEntity>();	
		scope.addAll(this.activeQ);
		scope.addAll(this.backlogQ);
		scope.addAll(this.tempQ);
		
		for (WorkItemEntity wi:scope) {
			double efforts = wi.efforts;
			load += efforts;
		}
		
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
					this.setBottleNeckCount(this.getBottleNeckCount() + 1);
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
	public int getBottleneckCount() {
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
	public int getBottleNeckCount() {
		return BottleNeckCount;
	}
	public void setBottleNeckCount(int bottleNeckCount) {
		BottleNeckCount = bottleNeckCount;
	}
	public LinkedList<ResourceEntity> getMyResourceEntities() {
		return myResourceEntities;
	}
	public void setMyResourceEntities(LinkedList<ResourceEntity> myResourceEntities) {
		this.myResourceEntities = myResourceEntities;
	}
	
	public int getColorR() {
		return icon.color[0];
	}
	public int getColorG() {
		return icon.color[1];
	}
	public int getColorB() {
		return icon.color[2];
	}
	public int getIconSize() {
		return icon.size;
	}
}
