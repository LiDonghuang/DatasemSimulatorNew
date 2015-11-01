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
		this.hierarchy = SoS.myWorkItemTypes.get(this.typeId).getHierarchy();
			
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Res."+devTask.getName();
		this.hasPredecessors = false;
		this.value = 0;
		
		this.serviceId = this.getUppertasks().get(0).serviceId;
		this.efforts = 100;
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();
		this.fullName = this.fullName();
		System.out.println(fullName);
	}

}
