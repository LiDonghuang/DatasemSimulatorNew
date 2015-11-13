package workItems;

public class DevTask extends Task {
	public int maxMaturityLevels = 5;
	public double uncertainty = 0;
	public double risk = 0;
	
	public DevTask(WorkItemEntity wi) {
		super(wi);
		this.isDevTask = true;
		this.serviceId = wi.getRequiredServices().get(0).getServiceType().getId();
		this.efforts = wi.getRequiredServices().get(0).getEfforts();
	}
	public DevTask(AggregationNode upperTask, int id, String name, int serviceId, double efforts) {
		super(upperTask);
		this.isDevTask = true;
		this.SoS = upperTask.SoS;
		upperTask.getSubtasks().add(this);
		this.id = id;
		this.name = name;
		this.typeId = SoS.getWorkItemTypeId("DevTask");
		this.myType = SoS.myWorkItemTypes.get(typeId);
		this.hierarchy = this.myType.getHierarchy();		
		this.serviceId = serviceId;
		this.efforts = efforts;
		this.maxMaturityLevels = (Integer)this.SoS.parameters.getValue("TaskMaturityLevels");
		this.fullName = this.fullName();
	}
	
	public void triggerChanges() {
		int incMaturityLevels = getCurrentMaturityLevel()- getPreviousMaturityLevel();	
		setPreviousMaturityLevel(getPreviousMaturityLevel() + incMaturityLevels);
		//if (incMaturityLevels>0) {System.out.println("@TIME:"+SoS.timeNow+this.fullName+"increased Maturity Level by "+incMaturityLevels+" to "+getCurrentMaturityLevel());}
		for (int i=0; i< incMaturityLevels; i++) {				
			if (Math.random()<=SoS.ReworkRisk) {
				double reduction = 1/(SoS.TaskMaturityLevels);
				this.rework(reduction);	
			}
			this.changePropagation();
			if (Math.random()<=SoS.TaskUncertainty) {
				this.suspendForResolution();
			}
		}
	}
	public void suspendForResolution() {
		this.isSuspended = true;
		this.suspendedTime = this.SoS.timeNow;
		this.setResolutionCount(this.getResolutionCount() + 1);
		this.getAssignedAgent().setBottleNeckCount(this.getAssignedAgent().getBottleNeckCount() + 1);
		this.getAssignedAgent().requestAssistance(this);
		//System.out.println("\nSUSPENDED @TIME:"+this.SoS.timeNow+this.fullName);
	}
	public void changePropagation() {
		for (WorkItemEntity affectedWI : this.getImpactsWIs()) {	
			double likelihood = this.getImpactsLikelihood().get(affectedWI);			
			if (Math.random()<likelihood) {				
				double impact = this.getImpactsRisk().get(affectedWI);
				if (!affectedWI.isAggregationNode && affectedWI.isStarted && !affectedWI.isEnded) {	
					//System.out.println("\nCHANGE PROPAGATION @TIME:"+this.SoS.timeNow+this.fullName+"propagates rework to"+affectedWI.fullName);
					//System.out.println(likelihood+" "+impact);
					((DevTask) affectedWI).rework(impact);
					this.setChangePropagationToCount(this.getChangePropagationToCount() + 1);
					affectedWI.setChangePropagationByCount(affectedWI.getChangePropagationByCount() + 1);
				}
			}
		}
	}
	public void rework(double progressReduction) {
		if (this.getPreviousReworkTime()<this.SoS.timeNow && this.getProgress()>0) {
			//System.out.println(previousReworkTime+" "+SoS.timeNow);
			this.setPreviousReworkTime(this.SoS.timeNow);
			//this.getPreviousProgress();
			this.setReworkCount(this.getReworkCount() + 1);
			this.uncertainty *= 1; // Learning Factor
			this.setProgress(Math.max((this.getProgress()-progressReduction), 0));								
			//System.out.println("\nREWORK @TIME:"+this.SoS.timeNow+this.fullName+"reworks from "+previousProgress+" to "+this.getProgress()+" (rework count:"+this.getReworkCount()+")");
			if (this.isCompleted) {
				this.isCompleted = false;
				this.isReactivated = true;	
				this.completionTime=Integer.MIN_VALUE;
				this.endTime=Integer.MIN_VALUE;
				this.getAssignedAgent().getActiveQ().remove(this);
				this.getAssignedAgent().getCompleteQ().remove(this);
				this.getAssignedAgent().getBacklogQ().add(this);
				//System.out.println("Re-Activate"+this.fullName+"at backlog of "+this.assignedAgent.getName());
			}
		}
	}
	
}
