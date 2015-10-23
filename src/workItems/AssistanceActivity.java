package workItems;

public class AssistanceActivity extends Task{
	public WorkItemEntity AssistanceObject;
	protected boolean isAggregationNode = false;
	
	public AssistanceActivity(Task task) {
		super(task);
		this.setIsAggregationNode(false);
		this.isAssistanceTask = true;
		this.SoS = task.SoS;
		this.AssistanceObject = task;
		int currentId = this.SoS.getWICount();
		this.id = currentId+1;
		this.SoS.increaseWICount();
		this.name = "Assistance."+task.getName();
		this.hasPredecessors = false;
		this.efforts = 10;
		this.value = 0;
		this.type = SoS.myWorkItemTypes.get(8);
		this.hierarchy = this.type.getHierarchy();
		this.services.add(SoS.myServices.get(5));
		this.fullName = this.fullName();
		this.maxMaturityLevels = 1;
		this.uncertainty = 0;
		this.risk = 0;		
		this.setActivated();
	}

}
