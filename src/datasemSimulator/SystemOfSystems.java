package datasemSimulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;
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
	private int WICount;
	
	// Time Series Records
	private List<Double> recordAgentsResourceUtilization_cov = new ArrayList<Double>();	
	// End Run Statistics
	private int EndTime;	
	private int CountTasks;
	private double ValueDelivered;
	private double TaskReworkCount_total;
	private double TaskReworkCount_mean;
	private double TaskReworkCount_stdev;
	private double ChangePropagationCount_total;
	private double ChangePropagationCount_mean;
	private double ChangePropagationCount_stdev;
	private double CycleTimeToEffortsRatio_mean;
	private double CycleTimeToEffortsRatio_stdev;
	private double AgentsBottleNeckCount_total;
	private double AgentsBottleNeckCount_mean;
	private double AgentsBottleNeckCount_stdev;
	private double AgentsWorkloadInbalance;
	private double AgentsResourceUtilization_cov;
	private double AgentsResourceUtilization_mean;
	
	
	
	
	@ScheduledMethod(start=1,interval=1,priority=1000)
	public void EndRunCondition() {
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		timeNow = (int)schedule.getTickCount();
		//System.out.println("\n ============== TIME NOW: "+timeNow+" ============== ");
		if (this.waitingList.size()==0){
			RunEnvironment.getInstance().endRun();
			System.out.println("\nSIMULATION ENDED: All WIs Completed\n");
			this.EndRunIndicators();
			this.EndRunAgentsStatistics();
			System.out.println("\n----------------------------------------------------");
		}
	}
	@ScheduledMethod(start=1,interval=1,priority=0)
	public void RecordData() {
		double[] AgentsResourceUtilization = new double[this.myServiceProviderAgents.size()];
		double AgentsResourceUtilization_mean = 0;
		double AgentsResourceUtilization_stdev = 0;
		double AgentsResourceUtilization_cov = 0;
		for (int i=0; i< this.myServiceProviderAgents.size(); i++) {
			ServiceProviderAgent sp = this.myServiceProviderAgents.get(i+1);
			AgentsResourceUtilization[i] = sp.getResourceUtilization();
		}
		AgentsResourceUtilization_mean = StatUtils.mean(AgentsResourceUtilization);
		AgentsResourceUtilization_stdev	= FastMath.sqrt(StatUtils.variance(AgentsResourceUtilization));
		if (AgentsResourceUtilization_stdev!=0) {
			AgentsResourceUtilization_cov = AgentsResourceUtilization_stdev/AgentsResourceUtilization_mean;
		}		
		this.AgentsResourceUtilization_mean = AgentsResourceUtilization_mean;
		this.AgentsResourceUtilization_cov = AgentsResourceUtilization_cov;
		recordAgentsResourceUtilization_cov.add(AgentsResourceUtilization_cov);
		//System.out.println("AgentsResourceUtilization:\n mean="+AgentsResourceUtilization_mean+" stdev="+AgentsResourceUtilization_stdev+" cov="+AgentsResourceUtilization_cov);
	}
	public int getWICount() {
		return this.WICount;
	}
	public void increaseWICount() {
		this.WICount++;
	}
	public double getValueDelivered() {
		return this.ValueDelivered;
	}
	public void deliverValue(double addValue) {
		this.ValueDelivered+=addValue;
	}
	public double getAgentsResourceUtilization_mean() {
		return this.AgentsResourceUtilization_mean;
	}
	public double getAgentsResourceUtilization_cov() {
		return this.AgentsResourceUtilization_cov;
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
				TaskReworkCount[i] = wi.getReworkCount();
				ChangePropagationCount[i] = wi.getChangePropagationByCount();
				CycleTimeToEffortsRatio[i] = wi.getCycleTimeToEffortsRatio();
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
		AgentsWorkloadInbalance = StatMean(recordAgentsResourceUtilization_cov);
		
		double[] AgentsBottleNeckCount = new double[wi_count];
		int j = 0;
		for (ServiceProviderAgent sp : myServiceProviderAgents.values()) {
			AgentsBottleNeckCount[j] = sp.getBottleneckCount();
			j++;
		}
		AgentsBottleNeckCount_total = StatUtils.sum(AgentsBottleNeckCount);
		AgentsBottleNeckCount_mean = StatUtils.mean(AgentsBottleNeckCount);
		AgentsBottleNeckCount_stdev = FastMath.sqrt(StatUtils.variance(AgentsBottleNeckCount));
		
		System.out.println("EndTime:"+EndTime);
		System.out.println("CountTasks:"+CountTasks);
		System.out.println("TaskReworkCount:\n total: "+TaskReworkCount_total+" mean: "+TaskReworkCount_mean+" stdev: "+TaskReworkCount_stdev);
		System.out.println("ChangePropagationCount:\n total: "+ChangePropagationCount_total+" mean: "+ChangePropagationCount_mean+" stdev: "+ChangePropagationCount_stdev);
		System.out.println("CycleTimeToEffortsRatio:\n mean: "+CycleTimeToEffortsRatio_mean+" stdev: "+CycleTimeToEffortsRatio_stdev);
		System.out.println("AgentsBottleNeckCount:\n total: "+AgentsBottleNeckCount_total+" mean: "+AgentsBottleNeckCount_mean+" stdev: "+AgentsBottleNeckCount_stdev);
		System.out.println("AgentsWorkloadInbalance: "+AgentsWorkloadInbalance);
		System.out.println("\n");
	}
	
	public void EndRunAgentsStatistics() {
		//System.out.println("\nAgents Statistics:");
		for (int i=0; i< this.myServiceProviderAgents.size(); i++) {
			ServiceProviderAgent sp = this.myServiceProviderAgents.get(i+1);
			sp.EndRunStatistics();			
//			System.out.println("\n-- ServiceProviderAgent id:"+sp.getId()+" name:"+sp.getName());
//			System.out.println("TotalWorkLoad_mean: "+sp.getTotalWorkLoad_mean());
//			System.out.println("TotalWorkLoad_stdev: "+sp.getTotalWorkLoad_stdev());
//			System.out.println("ActiveWorkload_mean: "+sp.getActiveWorkload_mean());
//			System.out.println("ActiveWorkload_stdev: "+sp.getActiveWorkload_stdev());
//			System.out.println("ResourceUtilization_mean: "+sp.getResourceUtilization_mean());
//			System.out.println("ResourceUtilization_stdev: "+sp.getResourceUtilization_stdev());
//			System.out.println("BottleNeckCount: "+sp.getBottleNectCount());
		}
	}
	
	public void getSoSInformation() {
		System.out.println("\n");
		for (WorkItemEntity wi:this.myWorkItemEntities.values()) {
			System.out.println("\nWorkItemEntity: ");
			System.out.println("wiId: "+wi.getId()+" name: "+wi.getName()+" type:"+wi.getType().getName());
			System.out.println(" Required Services:");
			for (Service s:wi.getServices()) {
				System.out.println(" serviceId: "+s.getId()+" name: "+s.getName());
			}
		}
		for (ServiceProviderAgent sp:this.myServiceProviderAgents.values()) {
			System.out.println("\nServiceProviderAgent: ");
			System.out.println("serviceProviderId: "+sp.getId()+" name: "+sp.getName());
			System.out.println(" ResourceEntities:");
			for (ResourceEntity r:sp.getMyResourceEntities()) {
				System.out.println(" resourceId: "+r.getId()+" name: "+r.getName());
				System.out.println("  SkillSet:");
				for (Skill sk:r.getSkillSet()) {
					System.out.println("  serviceId: "+sk.getService().getId()+" efficiency: "+sk.getEfficiency());
				}
			}
		}
		System.out.println("\n\n");
	}
	
	
	private double StatMean(List<Double> list) {
		double[] numbers = new double[list.size()];
		double value = 0;
		for (int i=0;i<list.size();i++) {
			numbers[i] = list.get(i);
		}
		value = StatUtils.mean(numbers);
		return value;
	}
	private double StatStdev(List<Double> list) {
		double[] numbers = new double[list.size()];
		double value = 0;
		for (int i=0;i<list.size();i++) {
			numbers[i] = list.get(i);
		}
		value = FastMath.sqrt(StatUtils.variance(numbers));
		return value;
	}
}
