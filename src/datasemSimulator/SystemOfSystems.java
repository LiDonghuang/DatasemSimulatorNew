package datasemSimulator;

import java.util.HashMap;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import xtext.objectsModel.Service;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItemType;

public class SystemOfSystems {
	protected HashMap<Integer, ServiceProviderAgent> myServiceProviderAgents;
	public HashMap<Integer, WorkItemEntity> myWorkItemEntities;
	public HashMap<Integer, ResourceEntity> myResourceEntities;
	public HashMap<Integer, WorkItemType> myWorkItemTypes;
	public HashMap<Integer, Service> myServices;
	public HashMap<Integer, WorkItemEntity> waitingList = new HashMap<Integer, WorkItemEntity>();
	public double timeNow;
	
	@ScheduledMethod(start=1,interval=1,priority=1000)
	public void step() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		timeNow = schedule.getTickCount();
		//System.out.println("\n ============== TIME NOW: "+timeNow+" ============== ");
		if (this.waitingList.size()==0){
			RunEnvironment.getInstance().endRun();
			System.out.println("SIMULATION ENDED");
		}
	}
	
	
	public void getSoSInformation() {
		System.out.println("\n");
		for (WorkItemEntity wi:this.myWorkItemEntities.values()) {
			System.out.println("\nWorkItemEntity: ");
			System.out.println("wiId: "+wi.getId()+" name: "+wi.getName()+" type:"+wi.getType().getName());
			System.out.println(" Required Services:");
			for (Service s:wi.getRequiredServices()) {
				System.out.println(" serviceId: "+s.getId()+" name: "+s.getName());
			}
		}
		for (ServiceProviderAgent sp:this.myServiceProviderAgents.values()) {
			System.out.println("\nServiceProviderAgent: ");
			System.out.println("serviceProviderId: "+sp.getId()+" name: "+sp.getName());
			System.out.println(" ResourceEntities:");
			for (ResourceEntity r:sp.myResourceEntities) {
				System.out.println(" resourceId: "+r.getId()+" name: "+r.getName());
				System.out.println("  SkillSet:");
				for (Skill sk:r.getSkillSet()) {
					System.out.println("  serviceId: "+sk.getService().getId()+" efficiency: "+sk.getEfficiency());
				}
			}
		}
		System.out.println("\n\n");
	}
}
