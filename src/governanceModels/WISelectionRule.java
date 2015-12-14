package governanceModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

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
import xtext.objectsModel.MechanismAttribute;

public class WISelectionRule {
	protected String ruleValue;	
	protected HashMap<String,Double>parameters;
	
	public WISelectionRule() {
		ruleValue = "Neutral";
	}
	public void implementWISelectionRule(Mechanism m) {
		String value = m.getValue();
		switch (value) {
		case "FIFO": 
			ruleValue = value;
			break;
		case "LIFO": 
			ruleValue = value;
			break;
		case "RPW": 
			ruleValue = value;
			parameters = new HashMap<String,Double>();
			for (MechanismAttribute a : m.getAttributes()) {
				addAttribute(a);
			}
			break;	
		case "PerceivedValue": 
			ruleValue = value;
			break;
		case "EDD": 
			ruleValue = value;
			break;
		case "SPT": 
			ruleValue = value;
			break;
		case "Neutral":break;
		default: 
			String msg = m.getName()+" is not a valid Prioritization RuleValue!" + "\n click OK to use default settings(Neutral)";
			JOptionPane.showMessageDialog(null,msg);			
			break;
		}
	}
	public void addAttribute(MechanismAttribute a) {
		parameters.put(a.getAttribute(), Double.parseDouble(a.getValue()));
		//System.out.println(a.getAttribute()+"="+Double.parseDouble(a.getValue()));
	}
	public double getAttribute(String s) {
		if (!parameters.containsKey(s)) {
			System.out.println(parameters);
		}	
		return parameters.get(s);
	}
	public LinkedList<Task> applyRule(ServiceProviderAgent SP, LinkedList<Task> requestedQ) {
		if (!requestedQ.isEmpty()) {
			LinkedList<Task> queue = requestedQ;
			SimUtilities.shuffle(queue, RandomHelper.getUniform()); 
			//System.out.println("\n"+SP.getName()+" Applies "+this.ruleValue+" Prioritization Rule on "+queue.size()+" WIs");

			if (this.ruleValue.matches("Neutral")) {
				//SimUtilities.shuffle(queue, RandomHelper.getUniform());
			}
			else if (this.ruleValue.matches("PerceivedValue")){			
				Collections.sort(queue, new LargerPerceivedValue());	
			}		
			else if (this.ruleValue.equals("RPW")) {
				Collections.sort(queue, new LargerRPW());
			}
			else if (this.ruleValue.equals("FIFO")) {
				Collections.sort(queue, new SmallerAssignedTime());
			}
			else if (this.ruleValue.matches("LIFO")) {
				Collections.sort(queue, new LargerAssignedTime());
			}
			else if (this.ruleValue.matches("EDD")) {
				Collections.sort(queue, new SmallerDueDate());
			}
			else if (this.ruleValue.matches("SPT")) {
				Collections.sort(queue, new SmallerEstimatedEfforts());
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
		double suc = wi.getSuccessors().size() *this.getAttribute("WeightPrecedency");
		double obj = 0;
		double imp = 0;
		double deco = 0;
		double susp = 0;

		for (WorkItemEntity impactsTarget: wi.getImpactsWIs()) {
			double likelihood = wi.getImpactsLikelihood().get(impactsTarget);
			double risk = wi.getImpactsRisk().get(impactsTarget);
			imp += likelihood*risk* this.getAttribute("WeightImpact");
		}
		if (wi.isAnalysisActivity) {
			AggregationNode AnalysisObject = (AggregationNode) ((AnalysisActivity)wi).AnalysisObject;
			deco = (calculateRPW(AnalysisObject)+AnalysisObject.hierarchy) * AnalysisObject.hierarchy *this.getAttribute("WeightHierarchy");
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
			if (w1.currentValue <w2.currentValue) {
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

