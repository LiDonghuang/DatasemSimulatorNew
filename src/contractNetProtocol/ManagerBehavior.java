package contractNetProtocol;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.ServiceProvider;


public class ManagerBehavior extends AbstractAgentBehavior{
	public ManagerBehavior(ServiceProviderAgent sp) {
		super(sp);
	}
	public void implAction(String action) {
		if (action.matches("announce")) {
			
		}
	}

	public void MakeAssignments() {
		
	}
}
