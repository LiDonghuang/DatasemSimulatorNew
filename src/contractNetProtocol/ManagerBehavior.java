package contractNetProtocol;
import datasemSimulator.AbstractClass;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import serviceProviders.AbstractAgentBehavior;
import serviceProviders.ServiceProviderAgent;
import workItems.DevTask;
import workItems.WorkItemEntity;
import xtext.objectsModel.ServiceProvider;


public class ManagerBehavior extends AbstractAgentBehavior{
	public ManagerBehavior() {
		super();
	}
	public void addState(int key,String state) {
		this.StatesMap.put(key, state);
	}
	public void addAction(int key,String action) {
		this.ActionsMap.put(key, action);
	}
	
	
	public void GoToState(int n) {
		switch(StatesMap.get(n)) {
			case "CheckRequestedQ": CheckRequestedQ();
			case "TaskAnnouncement": MakeAssignments();
			case "BiddingAcceptance": SelectWIsToStart();
			case "AdvanceWIsProgress": AdvanceWIsProgress();
			case "TriggerWIsChanges": TriggerWIsChanges();
			case "CheckWIsCompletion": CheckWIsCompletion();
		}
	}
	public void DoAction(int n, AbstractClass Object) {
		switch(ActionsMap.get(n)) {
			case "enterProtocol": Action(Object);
			case "establishContract": Action(Object);
			case "terminateContract": Action(Object);
			case "changeContract": Action(Object);
		}
	}
	public void MakeAssignments() {
		
	}
}
