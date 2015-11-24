package governanceModels;

import datasemSimulator.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ServiceProviderAgent;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.DevTask;
import workItems.ResolutionActivity;
import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.WorkItem;

public class WISelectionRule {
	
	protected String ruleValue;	
	
	public WISelectionRule() {
		ruleValue = "FIFO";
	}
	public void implementWISelectionRule(Mechanism m) {
		this.ruleValue = m.getValue();
	}
	
	public LinkedList<Task> applyRule(ServiceProviderAgent SP, LinkedList<Task> requestedQ) {
		//
		if (!requestedQ.isEmpty()) {
			LinkedList<Task> queue = requestedQ;
			SimUtilities.shuffle(queue, RandomHelper.getUniform()); 
			//System.out.println("\n"+SP.getName()+" Applies "+this.ruleValue+" Prioritization Rule on "+queue.size()+" WIs");
			//
			if (this.ruleValue.matches("Neutral")) {
				//SimUtilities.shuffle(queue, RandomHelper.getUniform());
			}
			else if (this.ruleValue.matches("PerceivedValue")){			
				Collections.sort(queue, new LargerPerceivedValue());	
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println("No."+i+": "+wItem.getName()+"(PerceivedValue:"+wItem.getPerceivedValue()+")");
//				}
			}		
			else if (this.ruleValue.equals("RPW")) {
				Collections.sort(queue, new LargerRPW());
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println(wItem.fullName+"(RPW:"+wItem.calculateRPW()+") ");
//				}
			}
			else if (this.ruleValue.equals("FIFO")) {
				Collections.sort(queue, new SmallerAssignedTime());
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println(wItem.fullName+"(AssignedTime:"+wItem.assignedTime+") ");
//				}
			}
			else if (this.ruleValue.matches("LIFO")) {
				Collections.sort(queue, new LargerAssignedTime());
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println("No."+i+": "+wItem.getName()+"(AssignedTime:"+wItem.assignedTime+")");
//				}
			}
			else if (this.ruleValue.matches("EDD")) {
				Collections.sort(queue, new SmallerDueDate());
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println("No."+i+": "+wItem.getName()+"(DueDate:"+wItem.getDueDate()+")");
//				}
			}
			else if (this.ruleValue.matches("SPT")) {
				Collections.sort(queue, new SmallerEstimatedEfforts());
//				for (int i=0;i<queue.size();i++) {
//					WorkItemEntity wItem = queue.get(i);
//					System.out.println("No."+i+": "+wItem.getName()+"(EstimatedEfforts:"+wItem.estimatedEfforts+")");
//				}
			}
			else {
				System.out.println("Invalid WI_Prioritization RuleValue: "+this.ruleValue) ;
				System.out.println("ERROR!");
				System.exit(0);
			}
			//
			return queue;}
		//
		else {return requestedQ;}
	}

	public double calculateRPW(WorkItemEntity wi) {
		double rpw = 0;
		double suc = wi.getSuccessors().size();
		double obj = 0;
		double imp = 0;
		double deco = 0;
		double susp = 0;

		for (WorkItemEntity impactsTarget: wi.getImpactsWIs()) {
			double likelihood = wi.getImpactsLikelihood().get(impactsTarget);
			double risk = wi.getImpactsRisk().get(impactsTarget);
			imp += likelihood*risk*5;
		}
		if (wi.isAnalysisActivity) {
			AggregationNode AnalysisObject = (AggregationNode) ((AnalysisActivity)wi).AnalysisObject;
			deco = (calculateRPW(AnalysisObject)+AnalysisObject.hierarchy) * (3-AnalysisObject.hierarchy);
		}
		if (wi.isResolutionActivity) {
			DevTask ResolutionObject = ((ResolutionActivity)wi).ResolutionObject;
			susp = calculateRPW(ResolutionObject);
		}
		rpw = suc + obj + imp + deco + susp;
		return rpw;
	}
	
	class LargerPerceivedValue implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.getPerceivedValue()<w2.getPerceivedValue()) {
				return 1;
			}
			else if (w1.getPerceivedValue()==w2.getPerceivedValue()) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
	class LargerAssignedTime implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.assignedTime<w2.assignedTime) {
				return 1;
			}
			else if (w1.assignedTime==w2.assignedTime) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
	class LargerRPW implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (calculateRPW(w1)<calculateRPW(w2)) {
				return 1;
			}
			else if (calculateRPW(w1)==calculateRPW(w2)) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
	class SmallerAssignedTime implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.assignedTime<w2.assignedTime) {
				return -1;
			}
			else if (w1.assignedTime==w2.assignedTime) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class SmallerActivatedTime implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.activatedTime<w2.activatedTime) {
				return -1;
			}
			else if (w1.activatedTime==w2.activatedTime) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class SmallerDueDate implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.getDueDate()<w2.getDueDate()) {
				return -1;
			}
			else if (w1.getDueDate()==w2.getDueDate()) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class SmallerEstimatedEfforts implements Comparator<WorkItemEntity> {
		@Override
		public int compare(WorkItemEntity w1, WorkItemEntity w2) {
			if (w1.estimatedEfforts<w2.estimatedEfforts) {
				return -1;
			}
			else if (w1.estimatedEfforts==w2.estimatedEfforts) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	
}

