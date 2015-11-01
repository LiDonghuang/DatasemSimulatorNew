package visualization;

import java.util.ArrayList;

import kanbanBoard.KanbanElement;
import datasemSimulator.SystemOfSystems;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.RandomGridAdder;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.space.grid.StrictBorders;
import serviceProviders.ResourceEntity;
import serviceProviders.ServiceProviderAgent;
import workItems.AggregationNode;
import workItems.AnalysisActivity;
import workItems.ResolutionActivity;
import workItems.WorkItemEntity;

public class Visualization {
	public SystemOfSystems SoS;
	Parameters parameters;
	public Context<Object> context;
	public Grid<Object> grid3D;
	public Grid<Object> grid2D;
	public Grid<Object> gridWIN;
	public Grid<Object> gridKanban;
	public Network<Object> netOrg_Hierarchy;
	public Network<Object> netWI_Hierarchy;
	public Network<Object> netAllocation;
	private int[] grid2Dsize = new int[2];
	private int[] gridWINsize = new int[2];
	private ArrayList<Comments> comments= new ArrayList<Comments>();
	
	public Visualization(Context<Object> ctx, SystemOfSystems mySoS) {
		parameters = RunEnvironment.getInstance().getParameters();
		ctx.add(this);
		context = ctx;
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);	
		SoS = mySoS;
		grid2Dsize[0]=SoS.OrgLevels*20; grid2Dsize[1]=SoS.OrgSize*5;
		gridWINsize[0]=SoS.WINSize*20; gridWINsize[1]=SoS.WINLevels*10;
		
		int width;
		int height;
//		grid3D = gridFactory.createGrid("3DGrid", context, 
//				new GridBuilderParameters<Object>(new WrapAroundBorders(), 
//						new SimpleGridAdder<Object>(), true, width, height, depth));	
		width = grid2Dsize[0];
		height = grid2Dsize[1];
		grid2D = gridFactory.createGrid("2DGrid", context, 
				new GridBuilderParameters<Object>(new WrapAroundBorders(), 
						new SimpleGridAdder<Object>(), true, width, height));
		
		width = gridWINsize[0];
		height = gridWINsize[1];
		gridWIN = gridFactory.createGrid("WINGrid", context, 
				new GridBuilderParameters<Object>(new WrapAroundBorders(), 
						new SimpleGridAdder<Object>(), true, width, height));
		netOrg_Hierarchy = new NetworkBuilder<Object>("Org_Hierarchy", context, true).buildNetwork();
		netWI_Hierarchy = new NetworkBuilder<Object>("WI_Hierarchy", context, true).buildNetwork();
		netAllocation = new NetworkBuilder<Object>("Allocation", context, true).buildNetwork();
		
		width = 20;
		height = 20;
		gridKanban = gridFactory.createGrid("KanbanGrid", context, 
				new GridBuilderParameters<Object>(new WrapAroundBorders(), 
						new SimpleGridAdder<Object>(), true, width, height));
		
		initializeOrganization();
	}

//	public Context<Object> context = ContextUtils.getContext(this);
//	public Grid<Object> grid3D = (Grid<Object>)context.getProjection("3DGrid");	
//	public Grid<Object> grid2D = (Grid<Object>)context.getProjection("2DGrid");
//	public Grid<Object> gridWIN = (Grid<Object>)context.getProjection("WINGrid");
//	public Network<Object> netWI_Hierarchy = (Network<Object>) context.getProjection("WI_Hierarchy");	
	@ScheduledMethod(start=0,interval=1,priority=-1)
	public void step() {
		this.context.removeAll(comments);
		this.comments.clear();
		visualizeWorkItemNetwork();
		visualizeOrganization();
		updateKanbanBoard();
	}
	public void initializeOrganization() {
		for (ServiceProviderAgent agent : SoS.myServiceProviderAgents.values()) {											
			int x=5+2*agent.hierarchy;
			int y=SoS.OrgSize*5-agent.getId()*5;
			agent.icon.location[0]=x; agent.icon.location[1]=y;
			agent.icon.color[0]=100;agent.icon.color[1]=100;agent.icon.color[1]=255;
			System.out.println(agent.getName()+" loc:["+x+","+y+"]");
			grid2D.moveTo(agent,x,y);
			int c = 1;
			for (ResourceEntity resource:agent.getMyResourceEntities()) {
				grid2D.moveTo(resource, agent.icon.location[0]+c+1,agent.icon.location[1]);
				c++;
			}
		}
		for (ServiceProviderAgent agent : SoS.myServiceProviderAgents.values()) {
			for (ServiceProviderAgent target:agent.assignWITo) {
				netOrg_Hierarchy.addEdge(agent, target);
			}
		}
	}

	public void visualizeWorkItemNetwork() {
		netWI_Hierarchy.removeEdges();
		int count[] = new int[SoS.WINLevels];
		for (WorkItemEntity wItem: SoS.arrivedList.values()) {
			if (!wItem.isResolutionActivity && !wItem.isAnalysisActivity) {
				int hierarchy = wItem.hierarchy;
				count[hierarchy]++;
				wItem.icon.location[0]= count[hierarchy]* (int)(Math.pow((SoS.WINLevels-hierarchy+1),2)-2*(SoS.WINLevels-hierarchy));
				wItem.icon.location[1] = gridWINsize[1]-hierarchy*10-5;
				if (wItem.isCompleted) {
					wItem.icon.color[0]=0;wItem.icon.color[1]=255;wItem.icon.color[1]=0;
				}
				else if (wItem.isStarted) {
					wItem.icon.color[0]=100;wItem.icon.color[1]=100;wItem.icon.color[1]=255;
				}
				else {
					wItem.icon.color[0]=100;wItem.icon.color[1]=100;wItem.icon.color[1]=100;
				}				
				gridWIN.moveTo(wItem, wItem.icon.location[0], wItem.icon.location[1]);
				if (wItem.isAggregationNode) {
					for (int i=0;i<((AggregationNode)wItem).getSubtasks().size();i++) {
						WorkItemEntity wItemsTask = ((AggregationNode)wItem).getSubtasks().get(i);
						if ( SoS.arrivedList.containsKey(wItemsTask.getId()) ){
							netWI_Hierarchy.addEdge(wItem,wItemsTask);
						}
					}
				}
			}
			else if (wItem.isAnalysisActivity) {
				AnalysisActivity wItem1 = (AnalysisActivity)wItem;
				wItem.icon.location[0] = wItem1.AnalysisObject.icon.location[0]-1;
				wItem.icon.location[1] = wItem1.AnalysisObject.icon.location[1]-1;
				wItem.icon.color[0]=255;wItem.icon.color[1]=0;wItem.icon.color[1]=0;
				gridWIN.moveTo(wItem, wItem.icon.location[0], wItem.icon.location[1]);
				netWI_Hierarchy.addEdge(wItem,wItem1.AnalysisObject);
			}
			else if (wItem.isResolutionActivity) {
				ResolutionActivity wItem1 = (ResolutionActivity)wItem;
				wItem.icon.location[0] = wItem1.ResolutionObject.icon.location[0]-1;
				wItem.icon.location[1] = wItem1.ResolutionObject.icon.location[1]-1;
				wItem.icon.color[0]=255;wItem.icon.color[1]=0;wItem.icon.color[1]=0;
				gridWIN.moveTo(wItem, wItem.icon.location[0], wItem.icon.location[1]);
				netWI_Hierarchy.addEdge(wItem,wItem1.ResolutionObject);
			}
			commentWI(wItem);
		}
	}
	public void visualizeOrganization() {	
		netAllocation.removeEdges();
		for (ServiceProviderAgent agent:SoS.myServiceProviderAgents.values()){
			int count;
			count=1;
			for (WorkItemEntity task:agent.getBacklogQ()){
				task.icon.color[0]=100;task.icon.color[1]=100;task.icon.color[1]=100;
				grid2D.moveTo(task, agent.icon.location[0]+count+1,agent.icon.location[1]-2);
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getActiveQ()){
				task.icon.color[0]=100;task.icon.color[1]=255;task.icon.color[1]=100;
				grid2D.moveTo(task, agent.icon.location[0]+count+1,agent.icon.location[1]-1);
				for (ResourceEntity resource:task.getAllocatedResources()) {
					netAllocation.addEdge(task,resource);
				}
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getCompleteQ()){
				task.icon.color[0]=0;task.icon.color[1]=255;task.icon.color[1]=0;
				grid2D.moveTo(task, agent.icon.location[0],agent.icon.location[1]);
			}
			count=1;
			for (WorkItemEntity task:agent.getComplexQ()){
				task.icon.color[0]=255;task.icon.color[1]=0;task.icon.color[1]=0;
				grid2D.moveTo(task, agent.icon.location[0]+count+1,agent.icon.location[1]-3);
				count++;
			}
		}
	}
	public void updateKanbanBoard() {
		for (KanbanElement element:this.SoS.myKanbanBoard.KanbanElements) {
			context.remove(element);
		}
		this.SoS.myKanbanBoard.clearBoard();
		
		for (WorkItemEntity wi:this.SoS.arrivedList.values()) {
			this.SoS.myKanbanBoard.addCapability(wi);
		}
		this.SoS.myKanbanBoard.updateElements();
		for (KanbanElement element:this.SoS.myKanbanBoard.KanbanElements) {
			context.add(element);
			gridKanban.moveTo(element, element.location[0], 20-element.location[1]-1);
		}
	}
	public void commentWI(WorkItemEntity wi) {
		Comments comments = new Comments();
		comments.addComment("ID:"+Integer.toString(wi.getId()));
		comments.addComment("type:"+this.SoS.myWorkItemTypes.get(wi.typeId).getName());
		comments.addComment("progress:"+Integer.toString((int)(wi.getProgress()*100))+"%");
		this.comments.add(comments);
		this.context.add(comments);
		gridWIN.moveTo(comments, wi.icon.location[0]+1,wi.icon.location[1]);
	}
}
