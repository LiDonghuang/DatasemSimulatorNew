package workItems;

public class DevTask extends Task {
	public int maxMaturityLevels = 5;
	public double uncertainty = 0.2;
	public double risk = 0.2;
	
	public DevTask(WorkItemEntity wi) {
		super(wi);
		this.isDevTask = true;
		this.isAggregationNode = false;
		this.typeId = wi.getType().getId();
		this.serviceId = wi.getRequiredServices().get(0).getServiceType().getId();
		this.efforts = wi.getRequiredServices().get(0).getEfforts();
	}
	public DevTask(AggregationNode upperTask, int id, String name, int serviceId, double efforts) {
		super(upperTask);
		this.isDevTask = true;
		this.isAggregationNode = false;
		this.SoS = upperTask.SoS;
		this.addUpperTask(upperTask);
		this.id = id;
		this.name = name;		
		this.typeId = SoS.getWorkItemTypeId("DevTask");
		this.setType(SoS.myWorkItemTypes.get(this.typeId));
		this.hierarchy = upperTask.hierarchy+1;		
		this.serviceId = serviceId;
		this.efforts = efforts;
		this.maxMaturityLevels = 5;
		this.uncertainty = 0.2;
		this.risk = 0.2;
		this.fullName = this.fullName();
	}
	
	public void triggerChanges() {
		int incMaturityLevels = getCurrentMaturityLevel()- getPreviousMaturityLevel();	
		setPreviousMaturityLevel(getPreviousMaturityLevel() + incMaturityLevels);
		//if (incMaturityLevels>0) {System.out.println(this.fullName+"increased Maturity Level by "+incMaturityLevels+" to "+getCurrentMaturityLevel());}
		for (int i=0; i< incMaturityLevels; i++) {				
			if (Math.random()<=this.uncertainty) {
				this.rework(this.risk);						
			}
			this.changePropagation();
			if (Math.random()<=0.2) {
				this.suspendForResolution();
			}
		}
	}
	public void suspendForResolution() {
		this.isSuspended = true;
		this.suspendedTime = this.SoS.timeNow;
		this.getAssignedAgent().setBottleNeckCount(this.getAssignedAgent().getBottleNeckCount() + 1);
		this.getAssignedAgent().requestAssistance(this);
		//System.out.println("\nSUSPENDED @TIME:"+this.SoS.timeNow+this.fullName);
	}
	public void changePropagation() {
		for (WorkItemEntity affectedWI : this.getImpactsWIs()) {	
			double likelihood = this.getImpactsLikelihood().get(affectedWI);			
			if (Math.random()<likelihood) {
				double impact = this.getImpactsValue().get(affectedWI);
				if (!affectedWI.isIsAggregationNode() && affectedWI.isStarted && !affectedWI.isEnded) {	
					//System.out.println("\nCHANGE PROPAGATION @TIME:"+this.SoS.timeNow+this.fullName+"propagates rework to"+affectedWI.fullName);								
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
			double previousProgress = this.getProgress();		
			this.setReworkCount(this.getReworkCount() + 1);
			this.uncertainty *= 1;
			this.setProgress(Math.max((this.getProgress()-progressReduction), 0));								
			//System.out.println("\nREWORK @TIME:"+this.SoS.timeNow+this.fullName+"reworks from "+previousProgress+" to "+this.progress+" (rework count:"+this.reworkCount+")");
			if (this.isCompleted) {
				this.isCompleted = false;
				this.isReactivated = true;	
				this.completionTime=Integer.MIN_VALUE;
				this.endTime=Integer.MIN_VALUE; 							
				this.getAssignedAgent().getCompleteQ().remove(this);
				this.getAssignedAgent().getBacklogQ().add(this);
				//System.out.println("Re-Activate"+this.fullName+"at backlog of "+this.assignedAgent.getName());
			}
		}
	}
	
}
