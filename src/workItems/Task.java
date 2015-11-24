package workItems;

import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;

public class Task extends WorkItemEntity{
	protected boolean isAggregationNode = false;
	
	public Task(WorkItem wi) {
		super(wi);
	}
	public void allocateResource(ResourceEntity sR) {
		this.getAllocatedResources().add(sR);
		//System.out.println("Resource "+sR.getName()+" is Allocated to "+this.getName());
	}
	public void withdrawResource(ResourceEntity sR) {
		this.getAllocatedResources().remove(sR);
		//System.out.println("Resource "+sR.getName()+" is Withdrawed from "+this.getName());
	}
	public double calculateExtendedServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ExtendedServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	public double calculateServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	public double calculateResourceEfficiency() {
		double rEfficiency = 0;
		for (int s=0;s<this.getAllocatedResources().get(0).getSkillSet().size();s++){
			Skill skill = this.getAllocatedResources().get(0).getSkillSet().get(s);
			if (skill.getService().getId() == this.serviceId){
				rEfficiency = skill.getEfficiency();				
				break;
			}		
		}		
		return rEfficiency;
	}
	public void advanceProgress() {
		// ------------ Compute WI Progress (percentage) -----------	
		if (this.isStarted) {		
			progressRate = this.getServiceEfficiency() / efforts;
			this.cycleTime += 1;
			if (this.isResolutionActivity) {
				((ResolutionActivity)this).ResolutionObject.cycleTime += 1;
			}
			//System.out.println(this.getName()+"(CycleTime:"+this.cycleTime+") at "+this.getAllocatedResources().get(0).getName()+" progress rate "+this.progressRate);
			setProgress(getProgress() + progressRate + 0.000001);					
			if (getProgress() >= 1) {			
				setProgress(1.00);	
			}
			double doubleMaturityLevels = getProgress() * maxMaturityLevels;
			//{System.out.println(this.fullName+"Maturity Level:"+doubleMaturityLevels+" which is "+(int)(doubleMaturityLevels));}
			setCurrentMaturityLevel((int)(doubleMaturityLevels));	
		}
	}	
	public void withdrawAllResources() {
		while (!this.getAllocatedResources().isEmpty()) {
			ResourceEntity resource = this.getAllocatedResources().getFirst();
			resource.withdrawFrom(this);
		}
	}
	public void setStarted() {	
		if (this.isStarted) {
			this.isRestarted = true;
			//System.out.println("\nRE-START @TIME:"+this.SoS.timeNow+this.fullName+"restarted from progress:"+this.getProgress());
		}
		else {
			this.cycleTime = 0;
			this.isStarted= true;
			this.startTime = this.SoS.timeNow;
			//System.out.println("\nSTART @TIME:"+this.SoS.timeNow+this.fullName+"is started");
		}
	}
}
