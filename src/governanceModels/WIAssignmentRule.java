package governanceModels;

import datasemSimulator.*;
import governanceModels.WISelectionRule.LargerPerceivedValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.Service;

public class WIAssignmentRule {
	protected String ruleValue;	
	private WorkItemEntity currentWI;
	private Service currentService;
	
	public void implementWIAssignmentRule(Mechanism m) {
		this.ruleValue = m.getValue();
	}
	
	public HashMap<WorkItemEntity,ServiceProviderAgent> applyRule(ServiceProviderAgent me, LinkedList<WorkItemEntity> WIs, LinkedList<ServiceProviderAgent> SPs) {
		//
		LinkedList<ServiceProviderAgent> list = new LinkedList<ServiceProviderAgent>();
		HashMap<WorkItemEntity,ServiceProviderAgent> schedule = new HashMap<WorkItemEntity,ServiceProviderAgent>(); 
		HashMap<ServiceProviderAgent,Boolean> scheduleLimit = new HashMap<ServiceProviderAgent,Boolean>(); 
		
		for (ServiceProviderAgent sp:SPs) {
			list.add(sp);
			if (sp.getBacklogQ().size()>=sp.BacklogLimit) {
				scheduleLimit.put(sp, true);
			}
			if (sp.getActiveQ().size()>=sp.WIPLimit) {
				scheduleLimit.put(sp, true);
			}
			scheduleLimit.put(sp, false);
		}			
		if (!SPs.isEmpty()) {	
			if (this.ruleValue.matches("Neutral")) {
				for (WorkItemEntity wi:WIs) {
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					if (!candidates.isEmpty()) {
//						for (ServiceProviderAgent sp:candidates) {
//							if (scheduleLimit.get(sp)) {
//								candidates.remove(sp);
//							}
//						}
						SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
					}	
				} 
			}
			else if (this.ruleValue.matches("LeastLoad")){		
				for (WorkItemEntity wi:WIs) {
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					if (!candidates.isEmpty()) {
//						for (ServiceProviderAgent sp:candidates) {
//							if (scheduleLimit.get(sp)) {
//								candidates.remove(sp);
//							}
//						}
						Collections.sort(candidates, new LessPredictedLoad());
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
//						scheduleLimit.put(selectedSP,true);
//						list.remove(selectedSP);
					}				
				} 	
			}
			else if (this.ruleValue.matches("ExtendedCapacity")) {
				for (WorkItemEntity wi:WIs) {
					this.currentWI = wi;
					this.currentService = me.SoS.myServices.get(currentWI.serviceId);
					LinkedList<ServiceProviderAgent> candidates = me.findServiceProviders(wi, list);
					if (!candidates.isEmpty()) {
//						for (ServiceProviderAgent sp:candidates) {
//							if (scheduleLimit.get(sp)) {
//								candidates.remove(sp);
//							}
//						}
						Collections.sort(candidates, new ExtendedCapacity());
						ServiceProviderAgent selectedSP = candidates.getFirst();
						schedule.put(wi, selectedSP);
						selectedSP.tempQ.add(wi);
						
//						scheduleLimit.put(selectedSP,true);
//						list.remove(selectedSP);
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
		
	class LessPredictedLoad implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (s1.estimateWorkLoad()<s2.estimateWorkLoad()) {
				return -1;
			}
			else if (s1.estimateWorkLoad()==s2.estimateWorkLoad()) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class MoreLoad implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (s1.estimateWorkLoad()<s2.estimateWorkLoad()) {
				return 1;
			}
			else if (s1.estimateWorkLoad()==s2.estimateWorkLoad()) {
				return 0;
			}
			else {
				return -1;
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
