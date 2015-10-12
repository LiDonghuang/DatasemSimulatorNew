package governanceModels;

import datasemSimulator.*;
import governanceModels.WISelectionRule.LargerPerceivedValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.LinkedList;

import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;
import serviceProviders.ServiceProviderAgent;
import xtext.objectsModel.Mechanism;

public class WIAssignmentRule {

protected String ruleValue;	
	
	public void implementWIAssignmentRule(Mechanism m) {
		this.ruleValue = m.getValue();
	}
	
	public LinkedList<ServiceProviderAgent> applyRule(ServiceProviderAgent SP, LinkedList<ServiceProviderAgent> oldcandidates) {
		//
		if (!oldcandidates.isEmpty()) {
			LinkedList<ServiceProviderAgent> candidates = oldcandidates;
			SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
			//System.out.println("\n"+SP.getName()+" Applies "+this.ruleValue+" Assignment Rule on "+candidates.size()+" SPs");
			//
			if (this.ruleValue.matches("Neutral")) {
				//SimUtilities.shuffle(candidates, RandomHelper.getUniform()); 
			}
			else if (this.ruleValue.matches("LeastLoad")){			
				Collections.sort(candidates, new LessLoad());	
//				for (int i=0;i<candidates.size();i++) {
//					ServiceProviderAgent sp = candidates.get(i);
//					System.out.println("No."+i+": "+sp.getName()+"(TotalWorkLoad:"+sp.getTotalWorkLoad()+")");
//				}
			}
			else {
				System.out.println("Invalid SP_Selection RuleValue: "+this.ruleValue) ;
				System.out.println("ERROR!");
				System.exit(0);
			}
			return candidates;
		}
		else {return oldcandidates;}
	}
		
	class LessLoad implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (s1.getTotalWICount()<s2.getTotalWICount()) {
				return -1;
			}
			else if (s1.getTotalWICount()==s2.getTotalWICount()) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	class MoreLoad implements Comparator<ServiceProviderAgent> {
		@Override
		public int compare(ServiceProviderAgent s1, ServiceProviderAgent s2) {
			if (s1.getTotalWICount()<s2.getTotalWICount()) {
				return 1;
			}
			else if (s1.getTotalWICount()==s2.getTotalWICount()) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}
}
