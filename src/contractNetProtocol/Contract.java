package contractNetProtocol;

import java.util.LinkedList;

import processModels.ValueFunction;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;

public class Contract {
	public LinkedList<WorkItemEntity> workItems = new LinkedList<WorkItemEntity>();
	public ServiceProviderAgent manager;
	public ServiceProviderAgent contractor;
	public ValueFunction valueFunction;
	
	public Contract(LinkedList<WorkItemEntity> wis, ServiceProviderAgent manager, ServiceProviderAgent contractor) {
		this.workItems.addAll(wis);
		this.manager = manager;
		this.contractor = contractor;
	}
}
