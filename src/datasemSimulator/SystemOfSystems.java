package datasemSimulator;

import java.util.HashMap;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import xtext.objectsModel.Service;
import xtext.objectsModel.Skill;
import xtext.objectsModel.WorkItemType;

public class SystemOfSystems {
	protected HashMap<Integer, ServiceProviderAgent> myServiceProviderAgents = new HashMap<Integer, ServiceProviderAgent>();
	public HashMap<Integer, WorkItemEntity> myWorkItemEntities = new HashMap<Integer, WorkItemEntity>();
	public HashMap<Integer, ResourceEntity> myResourceEntities = new HashMap<Integer, ResourceEntity>();
	public HashMap<Integer, WorkItemType> myWorkItemTypes;
	public HashMap<Integer, Service> myServices;
	public HashMap<Integer, WorkItemEntity> waitingList = new HashMap<Integer, WorkItemEntity>();
	public int timeNow;
	
	private int EndTime;
	private int CountTasks;
	private double TaskReworkCount_total;
	private double TaskReworkCount_mean;
	private double TaskReworkCount_stdev;
	private double ChangePropagationCount_total;
	private double ChangePropagationCount_mean;
	private double ChangePropagationCount_stdev;
	private double CycleTimeToEffortsRatio_mean;
	private double CycleTimeToEffortsRatio_stdev;
	
	@ScheduledMethod(start=1,interval=1,priority=1000)
	public void step() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		timeNow = (int)schedule.getTickCount();
		//System.out.println("\n ============== TIME NOW: "+timeNow+" ============== ");
		if (this.waitingList.size()==0){
			RunEnvironment.getInstance().endRun();
			System.out.println("SIMULATION ENDED");
			this.EndRunIndicators();
		}
	}
	
	public int getEndTime() {
		return EndTime;
	};
	public int getCountTasks() {
		return CountTasks;
	}
	public double getTaskReworkCount_total() {
		return TaskReworkCount_total;
	};
	public double getTaskReworkCount_mean() {
		return TaskReworkCount_mean;
	};
	public double getTaskReworkCount_stdev() {
		return TaskReworkCount_stdev;
	};
	public double getChangePropagationCount_total() {
		return ChangePropagationCount_total;
	};
	public double getChangePropagationCount_mean() {
		return ChangePropagationCount_mean;
	};
	public double getChangePropagationCount_stdev() {
		return ChangePropagationCount_stdev;
	};
	public double getCycleTimeToEffortsRatio_mean() {
		return CycleTimeToEffortsRatio_mean;
	};
	public double getCycleTimeToEffortsRatio_stdev() {
		return CycleTimeToEffortsRatio_stdev;
	};
	
	public void EndRunIndicators() {
		this.EndTime = this.timeNow-1;
		int wi_count = 0;
		for (WorkItemEntity wi : myWorkItemEntities.values()) {
			if (wi.getType().getId()==6) {
				wi_count++;				
			}
		}
		CountTasks = wi_count;
		double[] TaskReworkCount = new double[wi_count];		
		double[] ChangePropagationCount = new double[wi_count];
		double[] CycleTimeToEffortsRatio = new double[wi_count];
		int i = 0;
		for (WorkItemEntity wi : myWorkItemEntities.values()) {
			if (wi.getType().getId()==6) {
				TaskReworkCount[i] = wi.ReworkCount;
				ChangePropagationCount[i] = wi.ChangePropagationByCount;
				CycleTimeToEffortsRatio[i] = wi.CycleTimeToEffortsRatio;
				i++;
			}
		}
		TaskReworkCount_total = StatUtils.sum(TaskReworkCount);
		TaskReworkCount_mean = StatUtils.mean(TaskReworkCount);
		TaskReworkCount_stdev = FastMath.sqrt(StatUtils.variance(TaskReworkCount));
		ChangePropagationCount_total = StatUtils.sum(ChangePropagationCount);
		ChangePropagationCount_mean = StatUtils.mean(ChangePropagationCount);
		ChangePropagationCount_stdev = FastMath.sqrt(StatUtils.variance(ChangePropagationCount));
		CycleTimeToEffortsRatio_mean = StatUtils.mean(CycleTimeToEffortsRatio);
		CycleTimeToEffortsRatio_stdev = FastMath.sqrt(StatUtils.variance(CycleTimeToEffortsRatio));
		System.out.println("EndTime:"+EndTime);
		System.out.println("CountTasks:"+CountTasks);
		System.out.println("TaskReworkCount_total:"+TaskReworkCount_total);
		System.out.println("TaskReworkCount_mean:"+TaskReworkCount_mean);
		System.out.println("TaskReworkCount_stdev:"+TaskReworkCount_stdev);
		System.out.println("ChangePropagationCount_total:"+ChangePropagationCount_total);
		System.out.println("ChangePropagationCount_mean:"+ChangePropagationCount_mean);
		System.out.println("ChangePropagationCount_stdev:"+ChangePropagationCount_stdev);
		System.out.println("CycleTimeToEffortsRatio_mean:"+CycleTimeToEffortsRatio_mean);
		System.out.println("CycleTimeToEffortsRatio_stdev:"+CycleTimeToEffortsRatio_stdev);
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
