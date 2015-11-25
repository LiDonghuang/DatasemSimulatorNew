package governanceModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.AggregationNode;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.MechanismAttribute;
import xtext.objectsModel.Service;

public class WIAssignmentRule {
	protected String ruleValue;	
	protected HashMap<String,Double>parameters;
	private WorkItemEntity currentWI;
	private Service currentService;
	
	public WIAssignmentRule() {
		ruleValue = "Neutral";
	}
	public void implementWIAssignmentRule(Mechanism m) {
		String value = m.getValue();
		switch (value) {
		case "LeastLoad": 
			ruleValue = value;
			break;
		case "ExtendedCapacity": 
			ruleValue = value;
			parameters = new HashMap<String,Double>();
			for (MechanismAttribute a : m.getAttributes()) {
				addAttribute(a);
			}
			break;	
		case "Neutral":break;
		default: 
			String msg = m.getName()+" is not a valid Assignment RuleValue!" + "\n click OK to use default settings(Neutral)";
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
	public HashMap<WorkItemEntity,ServiceProviderAgent> applyRule(ServiceProviderAgent me, LinkedList<WorkItemEntity> WIs, LinkedList<ServiceProviderAgent> SPs) {
		HashMap<WorkItemEntity,ServiceProviderAgent> schedule = new HashMap<WorkItemEntity,ServiceProviderAgent>(); 
		if (!SPs.isEmpty()) {
			LinkedList<ServiceProviderAgent> list = new LinkedList<ServiceProviderAgent>();
			HashMap<ServiceProviderAgent,Boolean> scheduleLimit = new HashMap<ServiceProviderAgent,Boolean>(); 			
			//LinkedList<AggregationNode> aggrList = new LinkedList<AggregationNode>();
			LinkedList<Task> taskList = new LinkedList<Task>();
			for (int i=0;i<WIs.size();i++) {
				WorkItemEntity wi = WIs.get(i);
				if (!wi.isAggregationNode) {
					taskList.add((Task)wi);
					WIs.remove(wi);
					i--;
				}
			}
			me.myStrategy.applyWorkPrioritization(me, taskList);
			for (WorkItemEntity wi:taskList) {
//				if (wi.precedencyCleared()) {
//					WIs.add(wi);
//				}	
				WIs.add(wi);
			}
			for (ServiceProviderAgent sp:SPs) {
				list.add(sp);
				scheduleLimit.put(sp, false);
				if (sp.getBacklogQ().size()>=sp.myBehavior.BacklogLimit) {
					scheduleLimit.put(sp, true);
				}
				if (sp.getActiveQ().size()>=sp.myBehavior.WIPLimit) {
					scheduleLimit.put(sp, true);
				}
			}
			
			if (this.ruleValue.matches("Neutral")) {
				for (WorkItemEntity wi:WIs) {
					this.currentWI = wi;
					this.currentService = me.SoS.myServices.get(currentWI.serviceId);
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					for (int i=0;i<candidates.size();i++) {
						ServiceProviderAgent sp = candidates.get(i);
						if (scheduleLimit.get(sp)) {
							candidates.remove(sp);
							i--;
						}
					}
					if (!candidates.isEmpty()) {
						SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
						if (selectedSP.getBacklogQ().size()>=selectedSP.myBehavior.BacklogLimit 
								|| selectedSP.getActiveQ().size()>=selectedSP.myBehavior.WIPLimit) {
							//System.out.println(selectedR.getName()+" WIPLimit="+selectedR.WIPLimit+" reached");
							scheduleLimit.put(selectedSP, true);				
						}
					}	
				} 
			}
			else if (this.ruleValue.matches("LeastLoad")){		
				for (WorkItemEntity wi:WIs) {
					this.currentWI = wi;
					this.currentService = me.SoS.myServices.get(currentWI.serviceId);
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					for (int i=0;i<candidates.size();i++) {
						ServiceProviderAgent sp = candidates.get(i);
						if (scheduleLimit.get(sp)) {
							candidates.remove(sp);
							i--;
						}
					}
					if (!candidates.isEmpty()) {
						Collections.sort(candidates, new LessPredictedLoad());
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
						if (selectedSP.getBacklogQ().size()>=selectedSP.myBehavior.BacklogLimit 
								|| selectedSP.getActiveQ().size()>=selectedSP.myBehavior.WIPLimit) {
							//System.out.println(selectedR.getName()+" WIPLimit="+selectedR.WIPLimit+" reached");
							scheduleLimit.put(selectedSP, true);				
						}
					}	
				}
			}
			else if (this.ruleValue.matches("ExtendedCapacity")) {
				for (WorkItemEntity wi:WIs) {
					this.currentWI = wi;
					this.currentService = me.SoS.myServices.get(currentWI.serviceId);
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					for (int i=0;i<candidates.size();i++) {
						ServiceProviderAgent sp = candidates.get(i);
						if (scheduleLimit.get(sp)) {
							candidates.remove(sp);
							i--;
						}
					}
					if (!candidates.isEmpty()) {
						Collections.sort(candidates, new ExtendedCapacity());
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
						if (selectedSP.getBacklogQ().size()>=selectedSP.myBehavior.BacklogLimit 
								|| selectedSP.getActiveQ().size()>=selectedSP.myBehavior.WIPLimit) {
							//System.out.println(selectedR.getName()+" WIPLimit="+selectedR.WIPLimit+" reached");
							scheduleLimit.put(selectedSP, true);				
						}
					}	
				}			
			}
			else {
				System.out.println("Invalid SP_Selection RuleValue: "+this.ruleValue) ;
				System.out.println("ERROR!");
				System.exit(0);
			}
		}
		return schedule;
	}
	public double estimateWorkLoad(ServiceProviderAgent sp) {
		double load = 0;	
		LinkedList<WorkItemEntity> scope = new LinkedList<WorkItemEntity>();	
		scope.addAll(sp.getActiveQ());
		scope.addAll(sp.getBacklogQ());
		scope.addAll(sp.tempQ);	
		for (WorkItemEntity wi:scope) {
			double efforts = wi.efforts;
			load += efforts;
		}		
		return load;
	}
	class LessPredictedLoad implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (estimateWorkLoad(s1)<estimateWorkLoad(s2)) {
				return -1;
			}
			else if (estimateWorkLoad(s1)==estimateWorkLoad(s2)) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class ExtendedCapacity implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (s1.ExtendedServiceCapacity.get(currentService)<s2.ExtendedServiceCapacity.get(currentService)) {
				return 1;
			}
			else if (s1.ExtendedServiceCapacity.get(currentService)==s2.ExtendedServiceCapacity.get(currentService)) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
}
