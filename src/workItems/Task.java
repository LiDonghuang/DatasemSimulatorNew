package workItems;

import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;

// To Gokhan: 
// This file inherits from WorkItemEntity.java. 
// This file is also the parent class for: DevTask/AnalysisActivity/ResolutionActivity

public class Task extends WorkItemEntity{
	//Gokhan: isAggregationNode is used for connecting DSL with simulation model
	protected boolean isAggregationNode = false;
	//Gokhan: What does current decision do
	public int currentDecision; //1=Accept,2=Outsource,3=RequestHelp,4=Decline
	//Gokhan: constructor for Task class
	public Task(WorkItem wi) {
		super(wi);
	}
	//Gokhan: add resources to task entity
	public void allocateResource(ResourceEntity sR) {
		this.getAllocatedResources().add(sR);
		//System.out.println("Resource "+sR.getName()+" is Allocated to "+this.getName());
	}
	//Gokhan: delete resources from task entity
	public void withdrawResource(ResourceEntity sR) {
		this.getAllocatedResources().remove(sR);
		//System.out.println("Resource "+sR.getName()+" is Withdrawed from "+this.getName());
	}
	//Gokhan: explain what does this method do, what is sEfficiency, also explain why you used hash map (for efficiency purpose)
	public double calculateExtendedServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ExtendedServiceCapacity.get(SoS.myServices.get(this.serviceId))
				+ sp.ServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	//Gokhan: explain what does this method do
	public double calculateServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	//Gokhan: explain what does the resource efficiency do, how it is calculated, add comments inside the method if necessary
	public double calculateResourceEfficiency() {
		double myEfficiency = 0;
		int totalResources = this.getAllocatedResources().size();
		double coop_discount = 1.0/Math.pow((double)totalResources, 0.5);		
		for (ResourceEntity r : this.getAllocatedResources()) {
			double efficiency = r.getEfficiency(serviceId);
			int totalTasks = r.getWip().size();
			double multi_discount = 1.0/Math.pow((double)totalTasks, 0.33) /((double)(r.getWip().size()));
			//System.out.println(" total tasks:"+totalTasks+" efficiency:"+efficiency);
			double finalAdd = efficiency*coop_discount*multi_discount;
			myEfficiency += Math.max(0, finalAdd);
		}
		return myEfficiency;
	}
	//Gokhan: how do you advance progress, add comment to each line below
	public void advanceProgress() {
		// ------------ Compute WI Progress (percentage) -----------	
		if (this.isStarted) {
			this.setServiceEfficiency(this.calculateResourceEfficiency());
			setProgressRate(this.getServiceEfficiency() / efforts);
			this.cycleTime += 1;
			if (this.isResolutionActivity) {
				((ResolutionActivity)this).ResolutionObject.cycleTime += 1;
			}
			//System.out.println(this.getName()+"(CycleTime:"+this.cycleTime+") at "+this.getAllocatedResources().get(0).getName()+" progress rate "+this.progressRate);
			//Gokhan: Why there is a magic number in this calculation?
			setProgress(getProgress() + getProgressRate() + 0.000001);					
			if (getProgress() >= 1) {			
				setProgress(1.00);	
			}
			double doubleMaturityLevels = getProgress() * maxMaturityLevels;
			//{System.out.println(this.fullName+"Maturity Level:"+doubleMaturityLevels+" which is "+(int)(doubleMaturityLevels));}
			setCurrentMaturityLevel((int)(doubleMaturityLevels));	
		}
	}
	//Gokhan: why there is this method, what is the need for it?
	public void withdrawAllResources() {
		while (!this.getAllocatedResources().isEmpty()) {
			ResourceEntity resource = this.getAllocatedResources().getFirst();
			resource.withdrawFrom(this);
		}
	}
	//Gokhan: when do you call this method, what is the purpose of this method
	public void setStarted() {	
		if (this.isStarted) {
			this.isRestarted = true;
			//System.out.println("\nRE-START @TIME:"+this.SoS.timeNow+this.fullName+"restarted from progress:"+this.getProgress());
		}
		else {
			this.cycleTime = 0;
			this.isStarted= true;
			this.startTime = this.SoS.timeNow;
			System.out.println("\nSTART @TIME:"+this.SoS.timeNow+this.fullName+"is Started");
		}
	}
}
