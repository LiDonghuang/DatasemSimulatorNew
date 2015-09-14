package governanceModels;

import xtext.objectsModel.GovernanceStrategy;
import xtext.objectsModel.Mechanism;
import xtext.objectsModel.impl.GovernanceStrategyImpl;
import datasemSimulator.ServiceProviderAgent;

public class AgentStrategy extends GovernanceStrategyImpl {
	
	public GovernanceStrategy myGovernanceStrategy;
	public ServiceProviderAgent myServiceProviderAgent;
	
	public AgentStrategy(ServiceProviderAgent myServiceProviderAgent, GovernanceStrategy myGovernanceStrategy) {
		for (Mechanism m: myGovernanceStrategy.getMechanisms()) {
			implementMechanism(m);
		}
		myServiceProviderAgent.myStrategy = this;
	}
	
	public void implementMechanism(Mechanism m) {
		if (m.getName().matches("Acceptance")) {
			
		}
		else if (m.getName().matches("Selection")) {
			
		}
		else if (m.getName().matches("Assignment")) {
			
		}
		else if (m.getName().matches("Allocation")) {
			
		}
		else if (m.getName().matches("Outsourcing")) {
			
		}
		else {
				System.out.println("Invalid Mechanism Name!");
		}
		
	}
}
