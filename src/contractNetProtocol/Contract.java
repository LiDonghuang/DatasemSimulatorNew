package contractNetProtocol;

import java.util.LinkedList;

import processModels.ValueFunction;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;

public class Contract {
	public LinkedList<WorkItemEntity> workItems = new LinkedList<WorkItemEntity>();
	public ServiceProviderAgent requestor;
	public ServiceProviderAgent contractor;
	public ValueFunction valueFunction;
}
