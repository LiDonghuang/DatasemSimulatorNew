package kanbanBoard;

import java.text.DecimalFormat;
import java.util.LinkedList;

import bsh.This;
import datasemSimulator.SystemOfSystems;
import serviceProviders.ServiceProviderAgent;
import workItems.WorkItemEntity;

public class KanbanBoard {
	public LinkedList<KanbanElement> KanbanElements = new LinkedList<KanbanElement>();
	public LinkedList<WorkItemEntity> Capabilities = new LinkedList<WorkItemEntity>();
	public SystemOfSystems SoS;

	public KanbanBoard(SystemOfSystems SoS) {
		this.SoS = SoS;			
	}
	public void clearBoard() {
		this.KanbanElements.clear();
		this.Capabilities.clear();
	}
	public void updateElements() {	
		addElement(0,0,"Capability\n ",10,255,255);
		addElement(1,0,"Type\n ",10,255,255);
		addElement(2,0,"Value\nDeployed",20,255,255);
		addElement(3,0,"Current\nProgress",20,255,255);
		addElement(4,0,"Progress\nRate",20,255,255);	

		int c=1;
		double value;
		int r=0;int g=0;int b=0;
		for (WorkItemEntity myCap:Capabilities) {
			if (myCap.isStarted) {
				if (myCap.isSuspended) {
					r=255;g=0;b=0;
				}
				else if (myCap.isCompleted) {
					r=0;g=255;b=0;
				}
				else {
					r=51;g=153;b=255;
				}
			}
			else {
				r=224;g=224;b=224;
			}
			addElement(0,c,myCap.getName()+'\n',r,g,b);
			addElement(1,c,SoS.myWorkItemTypes.get(myCap.typeId).getName()+'\n',r,g,b);
			value = myCap.getProgress()*myCap.currentValue;
			addElement(2,c,new DecimalFormat("##.###").format(value)+'\n',r,g,b);
			value = myCap.getProgress()*100;
			addElement(3,c,new DecimalFormat("##.#").format(value)+"%"+'\n',r,g,b);
			value = myCap.getProgressRate()*100;
			addElement(4,c,new DecimalFormat("##.#").format(value)+"%"+'\n',r,g,b);
			c++;
		}
	}
	public void addCapability(WorkItemEntity cap) {
		if ((cap.getTypeId()==SoS.getWorkItemTypeId("SoSCap"))||(cap.getTypeId()==SoS.getWorkItemTypeId("SysFuncCap"))
				||(cap.getTypeId()==SoS.getWorkItemTypeId("SysOperCap"))) {
			Capabilities.add(cap);
		}
	}
	public void addElement(int x,int y,String value,int r,int g, int b) {
		KanbanElement element = new KanbanElement(x,y,value,r,g,b);
		this.KanbanElements.add(element);
	}
}
