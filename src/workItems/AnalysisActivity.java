package workItems;

public class AnalysisActivity extends Task{
	public WorkItemEntity AnalysisObject;
	protected boolean isAggregationNode = false;

	public AnalysisActivity (AggregationNode aggrNode) {	
		super(aggrNode);
		this.setIsAggregationNode(false); 
		this.isAnalysisTask = true;
		this.SoS = aggrNode.SoS;
		this.AnalysisObject = aggrNode;
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Analysis."+aggrNode.getName();
		this.getPredecessors().addAll(aggrNode.getPredecessors());
		this.efforts = 0;
		this.value = 0;
		this.type = SoS.myWorkItemTypes.get(7);
		this.hierarchy = this.type.getHierarchy();
		this.services.addAll(aggrNode.getServices());
		this.fullName = this.fullName();
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();	
	}

}
