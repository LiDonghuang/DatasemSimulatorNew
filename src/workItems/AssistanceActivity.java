package workItems;

public class AssistanceActivity extends Task{
	public WorkItemEntity AssistanceObject;
	protected boolean isAggregationNode = false;
	
	public AssistanceActivity(DevTask devTask) {
		super(devTask);
		this.setIsAggregationNode(false);
		this.isAssistanceTask = true;
		this.SoS = devTask.SoS;
		this.AssistanceObject = devTask;
		this.typeId = SoS.getWorkItemTypeId("Resolution");
		this.hierarchy = SoS.myWorkItemTypes.get(this.typeId).getHierarchy();
			
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Res."+devTask.getName();
		this.hasPredecessors = false;
		this.value = 0;
		
		this.serviceId = this.getUppertasks().get(0).serviceId;
		this.efforts = 10;
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();
		this.fullName = this.fullName();
	}

}
