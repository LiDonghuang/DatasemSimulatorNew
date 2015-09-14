package governanceModels;

import datasemSimulator.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;

public class WIAcceptanceRule {
	private int id;
	private String name;
	
	public WIAcceptanceRule(String ruleName) {
		this.name = ruleName;
	}
	public LinkedList<WorkItemEntity> applyRule(ServiceProviderAgent SP) {		
		//System.out.println(SP.getName()+" Applied "+this.name+" Acceptance Rule");

		return SP.getRequestedQ();
	}
}
