// Class "Task" inherits from class "WorkItemEntity"
// While it is also the parent class for: DevTask/AnalysisActivity/ResolutionActivity

package workItems;

import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.WorkItem;

public class Task extends WorkItemEntity{
	protected boolean isAggregationNode = false;
	//Gokhan: What does current decision do
	//Li: "current decision" is conserved to track current SP's decision from 'RequestHandling'step. It does nothing now.
	public int currentDecision; //1=Accept,2=Outsource,3=RequestHelp,4=Decline
	// constructor method, which inherits class WorkItemEntity 's constructor method (WorkItem wi).
	public Task(WorkItem wi) {
		super(wi);
	}
	//add a specific resource to task entity
	public void allocateResource(ResourceEntity resource) {
		this.getAllocatedResources().add(resource);
	}
	//remove a specific resource from task entity
	public void withdrawResource(ResourceEntity resource) {
		this.getAllocatedResources().remove(resource);
	}
	//Gokhan: explain what does this method do, what is sEfficiency, also explain why you used hash map (for efficiency purpose)
	//Li: this method returns the ExtendedServiceCapacity to do the task. (assuming the task only requires single service type)
	// 	  This value represents the sum of the SP's connected SPs' capacity in completing this task.
	//Li: ServiceCapacity and ExtendedServiceCapacity are ServiceProvider attributes, the values are calculated and fixed when simulation initialized.
	//Li: SoS(SystemOfSystems) is a class which is supposed to store all scenario information.
	public double calculateExtendedServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ExtendedServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	//Li: this method returns the skill (service type and efficiency level) for a given SP to do this task (assuming the task only requires single service type)
	// 	  This value represents the SP's own capacity in completing this task. 
	public double calculateServiceCapacity(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		sEfficiency = sp.ServiceCapacity.get(SoS.myServices.get(this.serviceId));
		return sEfficiency;
	}
	//Gokhan: explain what does the resource efficiency do, how it is calculated, add comments inside the method if necessary
	//Li: this method returns the actual progress rate of this task, knowing the status of its allocated resources.
	//Li: for example, if this task requires "5" efforts of service with serviceId "2", the task it is currently allocated to a single
	//    resource which has skill {service ID "1", efficiency 0.75; service ID "2", efficiency 1.0}, which is not working on other tasks simultaneously,
	//    then, this task's progress will advance 1.0/5 = 20% (which is the returned value), and will thus be completed in 5 ticks (if no rework/change propagation/resolution is triggered)
	//Li: there are two more complicated cases: 
	//Li: 1: a single resource working on multiple tasks in parallel; 2. a single task shared by multiple resources meanwhile
	//Li: this method supports case 1 by assuming a "penalty function" (which is coined by me)
	public double calculateResourceEfficiency() {
		double myEfficiency = 0;
		int totalResources = this.getAllocatedResources().size();
		double coop_discount = 1.0/Math.pow((double)totalResources, 0.5);		
		for (ResourceEntity r : this.getAllocatedResources()) {
			double efficiency = r.getEfficiency(serviceId);
			int totalTasks = r.getWip().size();
			// "getWip()" here returns parallel in-progress tasks of a resource. In DSL the max in-parallel tasks can be changed.
			double multi_discount = 1.0/Math.pow((double)totalTasks, 0.33) /((double)(r.getWip().size()));
			double finalAdd = efficiency*coop_discount*multi_discount;
			myEfficiency += Math.max(0, finalAdd);
		}
		return myEfficiency;
	}
	//Gokhan: how do you advance progress, add comment to each line below
	//Li: for example, if this task requires "5" efforts of service with serviceId "2", the task it is currently allocated to a single
	//    resource which has skill {service ID "1", efficiency 0.75; service ID "2", efficiency 1.0}, which is not working on other tasks simultaneously,
	//    then, this task's progress will advance 1.0/5 = 20% and will thus be completed in 5 ticks (if no rework/change propagation/resolution is triggered)
	public void advanceProgress() {
		// ------------ Compute WI Progress (percentage) -----------	
		if (this.isStarted) {
			this.setServiceEfficiency(this.calculateResourceEfficiency());
			//note that the task's ServiceEfficiency is updated in every tick
			setProgressRate(this.getServiceEfficiency() / efforts);
			//ProgressRate is also updated every tick. This value (the "20%" in the example mentioned above) will be added to the current progress every tick.
			this.cycleTime += 1;
			// "cycleTime" records the total ticks when this task is being worked on by one or more resources
			if (this.isResolutionActivity) {
				((ResolutionActivity)this).ResolutionObject.cycleTime += 1;
			}
			//Gokhan: Why there is a magic number in this calculation?
			//Li: The magic number (0.000001) is for compensate math error
			// generated from rounding up processes
			setProgress(getProgress() + getProgressRate() + 0.000001);					
			if (getProgress() >= 1) {			
				setProgress(1.00);	
			}
			//maxMaturityLevels is an integer which indicates the number of discrete "milestones" of this task's progress, 
			// assuming efforts are distributed linearly across the milestones.
			// (?) seemingly this section (below) should be in Task.java's subclass: DevTask.java instead, as MaturityLevels are only related to DevTask for now. 
			//  But similar behaviors may be added to AnalysisActivity and ResolutionTask later.
			double doubleMaturityLevels = getProgress() * maxMaturityLevels;
			setCurrentMaturityLevel((int)(doubleMaturityLevels));	
		}
	}
	//Gokhan: why there is this method, what is the need for it?
	//Li: this method is called whenever a task is completed or suspended. 
	public void withdrawAllResources() {
		while (!this.getAllocatedResources().isEmpty()) {
			ResourceEntity resource = this.getAllocatedResources().getFirst();
			resource.withdrawFrom(this);
		}
	}
	public void setStarted() {	
		if (this.isStarted) {
			this.isRestarted = true;
		}
		else {
			this.cycleTime = 0;
			this.isStarted= true;
			this.startTime = this.SoS.timeNow;
			System.out.println("\nSTART @TIME:"+this.SoS.timeNow+this.fullName+"is Started");
		}
	}
}
