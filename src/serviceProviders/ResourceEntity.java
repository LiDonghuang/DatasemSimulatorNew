package serviceProviders;

import java.util.LinkedList;

import workItems.Task;
import workItems.WorkItemEntity;
import xtext.objectsModel.Asset;
import xtext.objectsModel.impl.AssetImpl;

public class ResourceEntity extends AssetImpl{
	public Asset myResource;
	public int WIPLimit = 1;
	
	public boolean busy = false;
	protected LinkedList<WorkItemEntity> activeQ = new LinkedList<WorkItemEntity>();
	
	public ResourceEntity(Asset myResource) {
		this.myResource = myResource;
		this.id = myResource.getId();
		this.name = myResource.getName();
		this.skillSet = myResource.getSkillSet();
	}
	
	public void allocateTo(Task t) {
	    t.allocateResource(this);
		this.activeQ.add(t);
		this.setBusy();
	}
	public void withdrawFrom(Task t) {
		t.withdrawResource(this);
		this.activeQ.remove(t);
		if (this.activeQ.size()==0) {
			this.setIdle();
		}
	}
	public void setBusy () {
		this.busy = true;
	}
	public void setIdle () {
		this.busy = false;
	}
	public Boolean isBusy () {
		return this.busy;
	}
}
