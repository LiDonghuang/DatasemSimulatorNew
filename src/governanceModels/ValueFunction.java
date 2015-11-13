package governanceModels;

import org.apache.commons.math3.stat.StatUtils;

import workItems.AggregationNode;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.MechanismAttribute;
import xtext.objectsModel.impl.MechanismImpl;

public class ValueFunction extends MechanismImpl {
	public String type = "Fiat";
	public Mechanism mechanism;

	public ValueFunction(String type) {
		this.type = type;
	}
	public ValueFunction(Mechanism mechanism) {
		if (mechanism.getName().matches("ValueFunction")) {
			this.mechanism = mechanism;
			this.type = mechanism.getValue();
		}
		else {
			throw new RuntimeException("Mechanism "+mechanism.getName()+" is not a Value Function!");
		}
	}
	public String getAttributeValue(String attName) {
		String value = "";
		Boolean found = false;
		for (MechanismAttribute att:this.mechanism.getAttributes()) {
			if (att.getAttribute().matches(attName)) {
				value = att.getValue();
				found = true;
				break;
			}
		}
		if (!found) {
			throw new RuntimeException("Value Function Attribute "+attName+" not found!");
		}
		return value;
	}
	
	public void developValue(WorkItemEntity wi) {
		if (this.type.matches("Fiat")) {
			wi.currentValue = wi.initialValue;
		}
		// Derived Hierarchy
		else if (this.type.matches("Derived")) {
			algorithmDerivedValue(wi);
		}
	}
	
	private void algorithmDerivedValue(WorkItemEntity wi) {
			double baseValue = wi.Value;
			//if (wi.getId()==31){System.out.println("base "+baseValue);}
			double hierarchyFactor = Double.parseDouble(getAttributeValue("HierarchyFactor"));
			double precedencyFactor = Double.parseDouble(getAttributeValue("PrecedencyFactor"));
			
			double[] weights = new double[((AggregationNode)wi).getSubtasks().size()];
			for (int i=0; i<((AggregationNode)wi).getSubtasks().size();i++){	
				WorkItemEntity subtask = ((AggregationNode)wi).getSubtasks().get(i);
				weights[i] += 1;		
				for (WorkItemEntity successor:subtask.getSuccessors()) {
					if (!(successor.isAnalysisActivity||successor.isResolutionActivity)) {
						if (((AggregationNode)wi).getSubtasks().contains(successor)) {
							weights[i] += precedencyFactor;
						}
					}
				}
			}
			double totalWeights = StatUtils.sum(weights);
			for (int i=0; i<((AggregationNode)wi).getSubtasks().size();i++){
				WorkItemEntity subtask = ((AggregationNode)wi).getSubtasks().get(i);		
				double incSValue = (weights[i]/totalWeights)*baseValue*(1-hierarchyFactor);
				double previousSValue = subtask.Value;
				//if (subtask.getId()==31){System.out.println("prev "+previousValue+" inc "+increase);}
				subtask.Value += incSValue;	
				subtask.currentValue += incSValue;			
				wi.currentValue -= incSValue;
				//if (wi.getId()==31){System.out.println("base "+baseValue+"decrease "+increase);}
				if (subtask.isAggregationNode) {			
					if (subtask.isEnded) {
						subtask.SoS.deliverValue(incSValue);
					}
					else if (((AggregationNode)subtask).currentAnalysisStage==((AggregationNode)subtask).totalAnalysisStage) {
						subtask.Value = incSValue;
						developValue(subtask);
						subtask.Value += previousSValue;			
					}
				}
				else {
					if (subtask.isEnded) {
						subtask.SoS.deliverValue(incSValue);
					}
				}
			}
	}
	
}
