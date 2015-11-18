package contractNetProtocol;

import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;

public class PurposalCall {
	protected WorkItemEntity target;
	protected ServiceProviderAgent requester;
	protected boolean isEnded = false;
}
