package governanceModels;

import governanceModels.WIAssignmentRule.ExtendedCapacity;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.Task;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.MechanismAttribute;
import xtext.objectsModel.Service;


public class ResourceAllocationRule {
	protected String ruleValue;	
	protected HashMap<String,Double>parameters;
	private Task currentTask;
	private Service currentService;
	
	public ResourceAllocationRule() {
		ruleValue = "Neutral";
	}
	public void implementWIAllocationRule(Mechanism m) {
		String value = m.getValue();
		switch (value) {
		case "MostEfficient": 
			ruleValue = value;
			break;
		case "Neutral":break;
		default: 
			String msg = m.getName()+" is not a valid Allocation RuleValue!" + "\n click OK to use default settings(Neutral)";
			JOptionPane.showMessageDialog(null,msg);			
			break;
		}
	}
	public void addAttribute(MechanismAttribute a) {
		parameters.put(a.getAttribute(), Double.parseDouble(a.getValue()));
	}
	public double getAttribute(String s) {
		return parameters.get(s);
	}
	
	public HashMap<Task,ResourceEntity> applyRule(ServiceProviderAgent me, LinkedList<Task> tasks) {
		HashMap<Task,ResourceEntity> schedule = new HashMap<Task,ResourceEntity>(); 
		HashMap<ResourceEntity,Integer> scheduledResourceCount = new HashMap<ResourceEntity,Integer>();
		HashMap<Task,Integer> scheduledTaskCount = new HashMap<Task,Integer>();
		HashMap<ResourceEntity,Boolean> scheduleLimit = new HashMap<ResourceEntity,Boolean>(); 		
			
		for (ResourceEntity r: me.myResourceEntities) {
			scheduledResourceCount.put(r, 0);
			if (scheduledResourceCount.get(r)>me.SoS.MultiTasking) {
				scheduleLimit.put(r, true);
			}	
			else {
				scheduleLimit.put(r, false);	
			}
		}
		
		if (this.ruleValue.matches("Neutral")) {
			for (Task task : tasks) {
				this.currentTask = task;
				this.currentService = me.SoS.myServices.get(currentTask.serviceId);
				LinkedList<ResourceEntity> candidates = me.findResourceEntities(task);	
				//System.out.println("\nTo Schedule "+task.fullName);
				for (int i=0;i<candidates.size();i++) {
					ResourceEntity r = candidates.get(i);
					if (scheduleLimit.get(r)) {
						candidates.remove(r);
						i--;
						//System.out.println("removed "+r.getName());
					}
				}
				if (!candidates.isEmpty()) {
					SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
					ResourceEntity selectedR = candidates.getFirst();
					schedule.put(task, selectedR);
					//System.out.println("Scheduled "+selectedR.getName()+" to serve"+task.fullName);
					scheduledResourceCount.put(selectedR, scheduledResourceCount.get(selectedR)+1);
					if (scheduledResourceCount.get(selectedR)>=me.SoS.MultiTasking) {
						//System.out.println(selectedR.getName()+" WIPLimit="+selectedR.WIPLimit+" reached");
						scheduleLimit.put(selectedR, true);				
					}
				}
			}
		}
		else if (this.ruleValue.matches("MostEfficient")){		
			for (Task task : tasks) {
				this.currentTask = task;
				this.currentService = me.SoS.myServices.get(currentTask.serviceId);
				LinkedList<ResourceEntity> candidates = me.findResourceEntities(task);	
				//System.out.println("\nTo Schedule "+task.fullName);
				for (int i=0;i<candidates.size();i++) {
					ResourceEntity r = candidates.get(i);
					if (scheduleLimit.get(r)) {
						candidates.remove(r);
						i--;
						//System.out.println("removed "+r.getName());
					}
				}
				if (!candidates.isEmpty()) {
					Collections.sort(candidates, new MostEfficient());
					ResourceEntity selectedR = candidates.getFirst();
					schedule.put(task, selectedR);
					//System.out.println("Scheduled "+selectedR.getName()+" to serve"+task.fullName);
					scheduledResourceCount.put(selectedR, scheduledResourceCount.get(selectedR)+1);
					if (scheduledResourceCount.get(selectedR)>=selectedR.WIPLimit) {
						//System.out.println(selectedR.getName()+" WIPLimit="+selectedR.WIPLimit+" reached");
						scheduleLimit.put(selectedR, true);				
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
