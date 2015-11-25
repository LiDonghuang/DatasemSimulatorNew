package governanceModels;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;
import xtext.objectsModel.Mechanism;

public class WIAcceptanceRule {
	protected String ruleValue;	
	protected HashMap<String,Double>parameters;
	
	public WIAcceptanceRule() {
		ruleValue = "Unlimited";
	}
	public LinkedList<WorkItemEntity> applyRule(ServiceProviderAgent SP) {		
		//System.out.println(SP.getName()+" Applied "+this.name+" Acceptance Rule");
		return SP.getRequestedQ();
	}
	public void implementWIAcceptanceRule(Mechanism m) {
		// TODO Auto-generated method stub
		
	}
}
