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
	public double calculateServiceEfficiency(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		for (ResourceEntity r: sp.getMyResourceEntities()) {
			for (Skill sk: r.getSkillSet()){
				int service_id = sk.getService().getId();
				if (service_id == this.serviceId){
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
			progressRate = getServiceEfficiency() / efforts;															
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
}
