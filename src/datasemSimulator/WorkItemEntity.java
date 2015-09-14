package datasemSimulator;

import java.util.LinkedList;

import bsh.This;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;
import xtext.objectsModel.impl.WorkItemImpl;


public class WorkItemEntity extends WorkItemImpl {
	public SystemOfSystems SoS;
	// Static Attributes
	public WorkItem myWorkItem;
	public String fullName;
	public int hierarchy = this.type.getHierarchy();
	public WorkItemEntity AnalysisObject;
	protected LinkedList<WorkItemEntity> predecessors = new LinkedList<WorkItemEntity>();
	protected LinkedList<WorkItemEntity> successors = new LinkedList<WorkItemEntity>();
	protected LinkedList<WorkItemEntity> subtasks = new LinkedList<WorkItemEntity>();
	protected LinkedList<WorkItemEntity> uppertasks = new LinkedList<WorkItemEntity>();
	public int maxMaturityLevels = 1;
	public double uncertainty = 0;
	public double risk = 0;
	// Dynamic Attributes
	public boolean isActivated=false;
	public boolean isAssigned=false;
	public boolean isStarted=false;
	public boolean isCompleted=false;
	public boolean isEnded=false;
	
	public double estimatedEfforts=Double.MAX_VALUE;
	public double estimatedCompletionTime=Double.MAX_VALUE;
	public double activatedTime=Double.MAX_VALUE;
	public double startTime=Double.MAX_VALUE;
	public double assignedTime=Double.MAX_VALUE;
	public double completionTime=Double.MAX_VALUE;
	public double endTime=Double.MAX_VALUE;
	public double cycleTime=Double.MAX_VALUE;
	public double dueDate=Double.MAX_VALUE;
	
	protected ServiceProviderAgent requester;
	protected ServiceProviderAgent assignedAgent;
	protected LinkedList<ResourceEntity> allocatedResources = new LinkedList<ResourceEntity>();
	
	protected double serviceEfficiency = 0;
	protected double progress=0;
	protected double progressRate=0;
	protected double perceivedValue=0;
	
	protected int currentMaturityLevel = 0;
	protected int reworkCount = 0 ;
	private double previousReworkTime = 0;
	private int previousMaturityLevel = 0;
	
	public WorkItemEntity (WorkItem WorkItem) {
		this.myWorkItem = WorkItem;
		this.id = WorkItem.getId();
		this.name = WorkItem.getName();
		this.isAggregationNode = WorkItem.isIsAggregationNode();
		this.hasPredecessors = WorkItem.isHasPredecessors();
		this.efforts = WorkItem.getEfforts();
		this.value = WorkItem.getValue();
		this.type = WorkItem.getType();
		this.requiredServices = WorkItem.getRequiredServices();
		this.fullName = this.fullName();
	}
	public WorkItemEntity (WorkItemEntity aggrNode) {
		this.SoS = aggrNode.SoS;
		this.AnalysisObject = aggrNode;
		this.id = -aggrNode.id;
		this.name = aggrNode.name+".analysis";
		this.isAggregationNode = false;
		this.hasPredecessors = false;
		this.efforts = 5;
		this.value = 0;
		this.type = SoS.myWorkItemTypes.get(7);
		this.requiredServices = aggrNode.getRequiredServices();
		this.fullName = this.fullName();
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
	}
	

	public void advanceProgress() {
		if (!this.isAggregationNode) {
			// ------------ Compute WI Progress (percentage) -----------	
			if (this.isStarted) {
				progressRate = serviceEfficiency / efforts;															
				progress = progress + progressRate + 0.000001;					
				if (progress >= 1) {			
					progress=1.00;	
				}
				double doubleMaturityLevels = progress * maxMaturityLevels;
				//{System.out.println(this.fullName+"Maturity Level:"+doubleMaturityLevels+" which is "+(int)(doubleMaturityLevels));}
				currentMaturityLevel = (int)(doubleMaturityLevels);	
			}
		}
	}
	
	public void triggerChanges() {
		if (!this.isAggregationNode) {
			int incMaturityLevels = currentMaturityLevel- previousMaturityLevel;	
			previousMaturityLevel += incMaturityLevels;
			//if (incMaturityLevels>0) {System.out.println(this.fullName+"increased Maturity Level by "+incMaturityLevels+" to "+currentMaturityLevel);}
			for (int i=0; i< incMaturityLevels; i++) {				
				if (Math.random()<=this.uncertainty) {
					this.rework();						
				}
				this.changePropagation();
			}
		}
	}
	public void updateUpperTasksCompletion() {
		if (this.isCompleted) {
			for (WorkItemEntity upperTask:this.uppertasks) {
				if (upperTask.isActivated && !upperTask.isCompleted) {
					upperTask.checkSubTasksCompletion();
				}
			}
		}	
	}	
	public void checkSubTasksCompletion() {
		if (this.isAggregationNode && !this.isCompleted) {
			int unfinished = 0;			
			boolean cpl = true;
			for (WorkItemEntity subTask: this.subtasks) {	
				if (!subTask.isCompleted) {
					cpl = false;
					unfinished++;
				}
			}
			this.progress = 1-(double)unfinished/(double)this.subtasks.size();
			if (cpl == true) {
				this.setCompleted();
				this.updateUpperTasksCompletion();				
			}
		}
	}	
	
	public void removeFromContext() {
		if (this.isEnded) {
			//-
			@SuppressWarnings("unchecked")
			Context<Object> context = ContextUtils.getContext(this);	
			//-
			context.remove(this);
		}
	}

	public void changePropagation() {
		for (WorkItemEntity affectedWI : this.successors) {			
			if (!affectedWI.isAggregationNode && affectedWI.isStarted && !affectedWI.isEnded) {	
				System.out.println("\nCHANGE PROPAGATION @TIME:"+this.SoS.timeNow+this.fullName+"propagates rework to"+affectedWI.fullName);								
				affectedWI.rework();				
			}	
		}
	}
	public void rework() {
		if (this.previousReworkTime<this.SoS.timeNow && this.progress>0) {
			//System.out.println(previousReworkTime+" "+SoS.timeNow);
			this.previousReworkTime = this.SoS.timeNow;
			double previousProgress = this.progress;		
			this.reworkCount++;
			this.uncertainty *= 1;
			this.progress = Math.max((this.progress-this.risk), 0);		
			if (this.isCompleted) {
				this.isCompleted = false;
				this.assignedAgent.completeQ.remove(this);
				this.assignedAgent.backlogQ.add(this);
				System.out.println("Re-Create"+this.fullName+"at backlog of "+this.assignedAgent.getName());
			}
			this.completionTime=Double.MAX_VALUE;
			this.endTime=Double.MAX_VALUE;
			this.cycleTime=Double.MAX_VALUE;	
			System.out.println("\nREWORK @TIME:"+this.SoS.timeNow+this.fullName+"reworks from "+previousProgress+" to "+this.progress+" (rework count:"+this.reworkCount+")");
		}
	}
	public void addUpperTask(WorkItemEntity upperTask) {
		if (!this.uppertasks.contains(upperTask)){
			this.uppertasks.add(upperTask);
			upperTask.addSubTask(this);
		}
	}
	public void addSubTask(WorkItemEntity subTask) {
		if (!this.subtasks.contains(subTask)) {
			this.subtasks.add(subTask);
			subTask.addUpperTask(this);
		}
	}
	public void removeUpperTask(WorkItemEntity upperTask) {
		if (this.uppertasks.contains(upperTask)) {
			this.uppertasks.remove(upperTask);
			upperTask.removeSubTask(this);
		}
	}
	public void removeSubTask(WorkItemEntity subTask) {
		if (this.subtasks.contains(subTask)) {
			this.subtasks.remove(subTask);
			subTask.removeUpperTask(this);
		}
	}
	public void addPredecessorTask(WorkItemEntity predecessor) {
		this.predecessors.add(predecessor);
		predecessor.addSuccessorTask(this);
	}
	public void addSuccessorTask(WorkItemEntity successor) {
		this.successors.add(successor);
		successor.addPredecessorTask(this);
	}
	public boolean uppertasksCleared() {
		boolean cleared = true;
		for (WorkItemEntity upperTask:this.uppertasks) {
			if (upperTask.isActivated && !upperTask.isCompleted) {
				cleared = false;
				//System.out.println(this.fullName+"uppertask"+upperTask.fullName+"not cleared");
				break;
			}
		}	
		return cleared;
	}
	public boolean precedencyCleared() {
		boolean cleared = true;
		for (int i=0;i<this.predecessors.size();i++) {
			WorkItemEntity pTask = this.predecessors.get(i);
			if (pTask.isCompleted) {
				this.predecessors.remove(pTask);
				i--;
			}
			else {
				cleared = false;
				//System.out.println(this.fullName+"predecessor"+pTask.fullName+"not cleared");
				break;
			}
		}
		return cleared;				
	}	
	public void removeFromSuccessorTasks() {
		for (WorkItemEntity sTask:this.successors) {
			sTask.predecessors.remove(this);
		}
	}	
	/*
	public void triggerCausalities() {
		if (this.isAssigned&&this.precedencyCleared()){
			if (this.isAggregationNode) {
				for (WorkItemEntity subtask:subtasks) {
					subtask.assignedAgent = this.assignedAgent;
				}
			}
//			for (int c=0;c<this.getKSSTriggers().size();c++) {
//				KSSTrigger trigger = this.getKSSTriggers().get(c);
//				if ((this.isCompleted())||(this.progress >= trigger.getAtProgress())) {
//					double rand = Math.random();
//					if (trigger.getOnProbability() >= rand) {
//						for (int t=0;t<trigger.getTriggered().size();t++) {
//							KSSTask triggeredWI = trigger.getTriggered().get(t);
//							if (!trigger.isRepetitive() && !triggeredWI.isCreated()){
//								context.add(triggeredWI);
//								if (triggeredWI.getCOS().matches("Important")) {
//									this.getUpperTasks().get(0).addSubTask(triggeredWI);
//								}	
//								//
//								this.SoS.getArrivedList().add(triggeredWI);
//								triggeredWI.setCreated();
//								triggeredWI.setArrivalTime(this.SoS.timeNow);
//								// Put triggered WI to requestedQ of main WI's SP								
//								this.getAssignedTo().assignWI(triggeredWI);										
//							}
//						}
//					}
//					if (!trigger.isRepetitive()) {
//						this.getKSSTriggers().remove(trigger);
//					}
//				}
//			}
		}
	}
	*/
	public String fullName(){
		String full_name = " "+this.getType().getName()+" "+this.getName()+"(id:"+this.getId()+") ";
		return full_name;
	}
	public void setActivated() {
		this.isActivated= true;
		this.activatedTime = this.SoS.timeNow;
		//System.out.println("WorkItem "+this.getName()+"(id:"+this.getId()+")"+" is activated");
	}
	public void setStarted() {
		this.isStarted= true;
		this.startTime = this.SoS.timeNow;
		System.out.println("\nSTART @TIME:"+this.SoS.timeNow+this.fullName+"is started");
	}
	public void setCompleted() {
		this.isCompleted=true;		
		this.completionTime=this.SoS.timeNow;
		this.updateUpperTasksCompletion();
		this.removeFromSuccessorTasks();
		System.out.println("\nCOMPLETION @TIME:"+this.SoS.timeNow+this.fullName+"is completed"+" (rework count:"+this.reworkCount+")");
	}
	public void setEnded() {
		this.isEnded=true;
		this.endTime = this.SoS.timeNow;
		this.cycleTime = this.endTime - this.activatedTime;
		//System.out.println(this.getName()+" (id:"+this.getId()+") is Ended");
		//System.out.println("CycleTime: "+this.getCycleTime());
		if (this.type.getId()==1 | this.type.getId()==2) {
			this.SoS.waitingList.remove(this.getId());
			System.out.println("\nDELIVERY @TIME:"+this.SoS.timeNow+this.fullName+"is ended, delivered "+this.getValue()+" stakeholder value");
		}
	}
	public void setAssigned() {
		this.isAssigned=true;		
		this.assignedTime = this.SoS.timeNow;
	}
	public void setCreated() {
		this.isActivated=true;
		this.activatedTime=this.SoS.timeNow;
		//System.out.println(this.getName()+"(id:"+this.getId()+") is Created");
	}
	public void allocateResource(ResourceEntity sR) {
		this.allocatedResources.add(sR);
		//System.out.println("Resource "+sR.getName()+" is Allocated to "+this.getName());
	}
	public void withdrawResource(ResourceEntity sR) {
		this.allocatedResources.remove(sR);
		//System.out.println("Resource "+sR.getName()+" is Withdrawed from "+this.getName());
	}
	public double calculateServiceEfficiency(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		for (ResourceEntity r: sp.myResourceEntities) {
			for (Skill sk: r.getSkillSet()){
				int service_id = sk.getService().getId();
				if (service_id == this.getRequiredServices().get(0).getId()){
					sEfficiency = sk.getEfficiency();				
					break;
				}		
			}
		}
		//System.out.println(this.name+"- serviceId:"+this.getRequiredServices().get(0).getId()+", efficiency:"+sEfficiency);
		return sEfficiency;
	}
	public double calculateResourceEfficiency() {
		double rEfficiency = 0;
		for (int s=0;s<this.allocatedResources.get(0).getSkillSet().size();s++){
			Skill skill = this.allocatedResources.get(0).getSkillSet().get(s);
			if (skill.getService().getId() == this.requiredServices.get(0).getId()){
				rEfficiency = skill.getEfficiency();				
				break;
			}		
		}		
		return rEfficiency;
	}
	public double getPerceivedValue() {
		return this.perceivedValue;
	}
	public void setPerceivedValue(double v) {
		this.perceivedValue = v;
	}
}
