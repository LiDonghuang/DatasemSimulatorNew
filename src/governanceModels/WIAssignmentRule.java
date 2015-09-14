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

public class WIAssignmentRule {
	private String name;
	
	public WIAssignmentRule(String ruleName) {
		this.name = ruleName;
	}
	
	public LinkedList<WorkItemEntity> applyRule(ServiceProviderAgent SP) {	
		
		return SP.getAssignmentQ();
	}
}
