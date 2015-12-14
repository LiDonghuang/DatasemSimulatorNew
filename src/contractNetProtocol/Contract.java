package contractNetProtocol;

import java.util.LinkedList;

import processModels.ValueFunction;
import serviceProviders.ServiceProviderAgent;
import workItems.Task;

public class Contract {
	public LinkedList<Task> tasks = new LinkedList<Task>();
	public ServiceProviderAgent manager;
	public ServiceProviderAgent contractor;
	public ValueFunction valueFunction;
	
	public Contract(LinkedList<Task> wis, ServiceProviderAgent manager, ServiceProviderAgent contractor) {
		this.tasks.addAll(wis);
		this.manager = manager;
		this.contractor = contractor;
	}
}
