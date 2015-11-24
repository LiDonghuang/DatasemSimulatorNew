package governanceModels;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.Service;


public class ResourceAllocationRule {
	protected String ruleValue;	
	private Task currentTask;
	private Service currentService;
	
	public ResourceAllocationRule() {
		ruleValue = "Neutral";
	}
	public void implementWIAssignmentRule(Mechanism m) {
		this.ruleValue = m.getValue();
	}
	
	public HashMap<Task,ResourceEntity> applyRule(ServiceProviderAgent me, LinkedList<Task> tasks) {
		HashMap<Task,ResourceEntity> schedule = new HashMap<Task,ResourceEntity>(); 
		LinkedList<ResourceEntity> list = new LinkedList<ResourceEntity>();
		HashMap<ResourceEntity,Integer> scheduledResourceCount = new HashMap<ResourceEntity,Integer>();
		HashMap<Task,Integer> scheduledTaskCount = new HashMap<Task,Integer>();
		HashMap<ResourceEntity,Boolean> scheduleLimit = new HashMap<ResourceEntity,Boolean>(); 		
			
		for (ResourceEntity r: me.myResourceEntities) {
			scheduleLimit.put(r, false);
			scheduledResourceCount.put(r, 0);
		}
		
		if (this.ruleValue.matches("Neutral")) {
			for (Task task : tasks) {
				LinkedList<ResourceEntity> candidates = me.findResourceEntities(task);
				if (!candidates.isEmpty()) {
					for (ResourceEntity r :candidates) {
						if (scheduleLimit.get(r)) {
							candidates.remove(r);
						}
					}
					if (!candidates.isEmpty()) {
						SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
						ResourceEntity selectedR = candidates.getFirst();
						schedule.put(task, selectedR);
						scheduledResourceCount.put(selectedR, scheduledResourceCount.get(selectedR)+1);
						if (scheduledResourceCount.get(selectedR)>=selectedR.WIPLimit) {
							scheduleLimit.put(selectedR, true);
						}						
					}				
				}
			} 
		}
		else if (this.ruleValue.matches("MostEfficient")){		
			for (Task task : tasks) {
				this.currentTask = task;
				this.currentService = me.SoS.myServices.get(currentTask.serviceId);
				LinkedList<ResourceEntity> candidates = me.findResourceEntities(task);
				if (!candidates.isEmpty()) {
					for (ResourceEntity r :candidates) {
						if (scheduleLimit.get(r)) {
							candidates.remove(r);
						}
					}
					if (!candidates.isEmpty()) {
						Collections.sort(candidates, new MostEfficient());
						ResourceEntity selectedR = candidates.getFirst();
						schedule.put(task, selectedR);
						scheduledResourceCount.put(selectedR, scheduledResourceCount.get(selectedR)+1);
						if (scheduledResourceCount.get(selectedR)>=selectedR.WIPLimit) {
							scheduleLimit.put(selectedR, true);
						}
					}					
				}	
			}
		}
		else {
			System.out.println("Invalid Resource_Allocation RuleValue: "+this.ruleValue) ;
			System.out.println("ERROR!");
			System.exit(0);
		}
		return schedule;
	}
	
	
	class MostEfficient implements Comparator<ResourceEntity> {
		@Override
		public int compare(ResourceEntity r1, ResourceEntity r2) {
			if ( r1.getEfficiency(currentService.getId())<r2.getEfficiency(currentService.getId()) ) {
				return 1;
			}
			else if ( r1.getEfficiency(currentService.getId())==r2.getEfficiency(currentService.getId()) ) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
}
