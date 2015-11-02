package workItems;

public class ResolutionActivity extends Task{
	public WorkItemEntity ResolutionObject;
	protected boolean isAggregationNode = false;
	
	public ResolutionActivity(DevTask devTask) {
		super(devTask);
		this.setIsAggregationNode(false);
		this.isResolutionActivity = true;
		this.SoS = devTask.SoS;
		this.ResolutionObject = devTask;
		this.typeId = SoS.getWorkItemTypeId("Resolution");
		this.setType(SoS.myWorkItemTypes.get(this.typeId));
		this.hierarchy = this.getType().getHierarchy();
			
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Res."+devTask.getName();
		this.hasPredecessors = false;
		this.value = 0;
		this.initialValue = 0;
		this.currentValue =0;
		
		this.serviceId = ResolutionObject.getUppertasks().get(0).serviceId;
		this.efforts = 10;
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();
		this.fullName = this.fullName();
	}

}
