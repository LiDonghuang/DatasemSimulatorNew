package workItems;

public class AnalysisActivity extends Task{
	public WorkItemEntity AnalysisObject;
	protected boolean isAggregationNode = false;

	public AnalysisActivity (AggregationNode aggrNode) {	
		super(aggrNode);
		this.setIsAggregationNode(false); 
		this.isAnalysisActivity = true;
		this.SoS = aggrNode.SoS;
		this.AnalysisObject = aggrNode;
		this.typeId = SoS.getWorkItemTypeId("Analysis");
		this.myType = SoS.myWorkItemTypes.get(typeId);
		this.hierarchy = this.myType.getHierarchy();	
		
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Ana."+aggrNode.getName();
		this.getPredecessors().addAll(aggrNode.getPredecessors());
		this.value = 0;
		this.initialValue = 0;
		this.currentValue =0;
		this.serviceId = aggrNode.myWorkItem.getRequiredAnalysis().get(aggrNode.currentAnalysisStage).getServiceType().getId();
		this.efforts = aggrNode.myWorkItem.getRequiredAnalysis().get(aggrNode.currentAnalysisStage).getEfforts();		
		
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();	
		this.fullName = this.fullName();
	
	}
}
