package workItems;

import xtext.objectsModel.WorkItem;

public class DevTask extends Task {

	public DevTask(WorkItem WorkItem) {
		super(WorkItem);
		this.isDevTask = true;
	}
	
	public void triggerChanges() {
		int incMaturityLevels = getCurrentMaturityLevel()- getPreviousMaturityLevel();	
		setPreviousMaturityLevel(getPreviousMaturityLevel() + incMaturityLevels);
		//if (incMaturityLevels>0) {System.out.println(this.fullName+"increased Maturity Level by "+incMaturityLevels+" to "+currentMaturityLevel);}
		for (int i=0; i< incMaturityLevels; i++) {				
			if (Math.random()<=this.uncertainty) {
				this.rework(this.risk);						
			}
			this.changePropagation();
			if (Math.random()<=0.00) {
				this.suspendForAssistance();
			}
		}
	}
	public void suspendForAssistance() {
		if (!this.isAnalysisTask && !this.isAssistanceTask) {
			this.isSuspended = true;
			this.suspendedTime = this.SoS.timeNow;
			this.getAssignedAgent().setBottleNeckCount(this.getAssignedAgent().getBottleNeckCount() + 1);
			this.getAssignedAgent().requestAssistance(this);
			//System.out.println("\nSUSPENDED @TIME:"+this.SoS.timeNow+this.fullName);
		}
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
				this.getAssignedAgent().completeQ.remove(this);
				this.getAssignedAgent().backlogQ.add(this);
				//System.out.println("Re-Activate"+this.fullName+"at backlog of "+this.assignedAgent.getName());
			}
		}
	}
	
}
