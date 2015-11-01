package workItems;

import java.util.LinkedList;

import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItem;

public class AggregationNode extends WorkItemEntity{
	private LinkedList<WorkItemEntity> subtasks = new LinkedList<WorkItemEntity>();
	
	public AggregationNode(WorkItem wi) {
		super(wi);
		this.isAggregationNode = true;
		this.typeId = wi.getType().getId();
		if (wi.getRequiredServices().isEmpty()) {
			this.serviceId = 1;
		}
		else {
			this.serviceId = wi.getRequiredServices().get(0).getServiceType().getId();
		}
	}
	
//	public void advanceProgress() {
//	}
//	
//	public void triggerChanges() {
//	}

	public void checkSubTasksCompletion() {
		if (this.isAggregationNode && !this.isCompleted) {
			int unfinished = 0;			
			boolean cpl = true;
			for (WorkItemEntity subTask: this.getSubtasks()) {	
				if (!subTask.isCompleted) {
					cpl = false;
					unfinished++;
				}
			}
			this.setProgress(1-(double)unfinished/(double)this.getSubtasks().size());
			if (cpl == true) {
				this.setCompleted();
				this.updateUpperTasksCompletion();				
			}
		}
	}
	public LinkedList<WorkItemEntity> getSubtasks() {
		return subtasks;
	}
	public void setSubtasks(LinkedList<WorkItemEntity> subtasks) {
		this.subtasks = subtasks;
	}
	public void addSubTask(WorkItemEntity subTask) {
		if (!this.getSubtasks().contains(subTask)) {
			this.getSubtasks().add(subTask);
			subTask.addUpperTask(this);
		}
	}
	public void removeSubTask(WorkItemEntity subTask) {
		if (this.getSubtasks().contains(subTask)) {
			this.getSubtasks().remove(subTask);
			subTask.removeUpperTask(this);
		}
	}
	public double getProgress() {
		double progress = 0;
		for (WorkItemEntity subtask:this.getSubtasks()) {
			progress += subtask.getProgress();
		}
		progress = progress/this.getSubtasks().size();
		this.setProgress(progress);
		return progress;
	}
	public double getProgressRate() {
		double progressRate = 0;
		for (WorkItemEntity subtask:this.getSubtasks()) {
			progressRate += subtask.getProgressRate();
		}
		progressRate = progressRate/this.getSubtasks().size();
		this.setProgressRate(progressRate);
		return progressRate;
	}
	public double calculateServiceEfficiency(ServiceProviderAgent sp) {
		double sEfficiency = 0;
		for (ResourceEntity r: sp.getMyResourceEntities()) {
			for (Skill sk: r.getSkillSet()){
				int service_id = sk.getService().getId();
				if (service_id == this.serviceId){
					sEfficiency = sk.getEfficiency();				
					break;
				}		
			}
		}
		//System.out.println(this.name+"- serviceId:"+this.getRequiredServices().get(0).getId()+", efficiency:"+sEfficiency);
		return sEfficiency;
	}
}
