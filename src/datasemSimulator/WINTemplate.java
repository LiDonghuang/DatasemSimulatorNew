package datasemSimulator;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import workItems.AggregationNode;
import workItems.DevTask;
import workItems.WorkItemEntity;
import xtext.objectsModel.RequiredService;

public class WINTemplate {
	public static void generateSubtasks(WorkItemEntity wi) {
		double ComplexityFactor = 0.5;
		wi = (AggregationNode)wi;
		int c = 0;
		for (RequiredService reqSev : wi.getRequiredServices()) {
			double totalEfforts = reqSev.getEfforts();
			double interval_max = 15;
			double interval_min = 5;
			int serviceId = reqSev.getServiceType().getId();
			while (totalEfforts>0) {
				c++;
				String name = wi.getName()+"."+c;	
				double efforts = interval_min+Math.random()*(interval_max-interval_min);
				efforts = Math.min(efforts, totalEfforts);
				totalEfforts -= efforts;
				if (totalEfforts<=interval_min) {
					efforts+=totalEfforts;
					totalEfforts=0;
				}
				int currentId = wi.SoS.getWICount();
				int st_id = currentId+1;
				wi.SoS.increaseWICount();						
				new DevTask((AggregationNode)wi, st_id, name, serviceId, efforts);
			}
		}
		// Complexity
		SimUtilities.shuffle(((AggregationNode)wi).getSubtasks(), RandomHelper.getUniform()); 
		for (WorkItemEntity wis: ((AggregationNode)wi).getSubtasks()) {
			boolean loop = true;
			int loopcount = 0;
			if (Math.random()<ComplexityFactor) {
				while (loop) {
					loopcount ++;
					int t =(int) ( Math.random()* ((AggregationNode)wi).getSubtasks().size() );
					WorkItemEntity pred = ((AggregationNode)wi).getSubtasks().get(t);
					if ( (pred.getId()<wis.getId()) && (!pred.getPredecessors().contains(wis)) ) {
						wis.addPredecessorTask(pred);
						//System.out.println("Add Predecessor: "+pred.getName()+" to "+wi1s.getName());
						loop = false;
					}
					else if (loopcount >= 3) {
						loop = false;
					}
				}
			}
		}
	}
	public static void generateImpacts(WorkItemEntity wi1) {
		for (WorkItemEntity wi2 : wi1.getImpactsWIs()) {
			if (wi2.isAggregationNode) {
				double likelihood = wi1.getImpactsLikelihood().get(wi2);
				double risk = wi1.getImpactsRisk().get(wi2);
				for (WorkItemEntity wi1s: ((AggregationNode)wi1).getSubtasks()) {
					if (Math.random()<(likelihood*2)) {
						int t =(int) ( Math.random()* ((AggregationNode)wi2).getSubtasks().size() );
						WorkItemEntity wi2s = ((AggregationNode)wi2).getSubtasks().get(t);
						wi1s.getImpactsWIs().add(wi2s);
						wi1s.getImpactsLikelihood().put(wi2s, 0.5);
						wi1s.getImpactsRisk().put(wi2s, risk);
						//System.out.println("Impacts DSM: from "+wi1s.getName()+" to "+wi2s.getName());
					}
				}
			}
		}
	}
}
