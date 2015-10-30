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
import workItems.WorkItemEntity;

public class Visualization {
	public SystemOfSystems SoS;
	Parameters parameters;
	public Context<Object> context;
	public Grid<Object> grid3D;
	public Grid<Object> grid2D;
	public Grid<Object> gridWIN;
	public Grid<Object> gridKanban;
	public Network<Object> netWI_Hierarchy;
	public Network<Object> netAllocation;
	private int[] grid2Dsize = new int[2];
	private int[] gridWINsize = new int[2];
	
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
		netWI_Hierarchy = new NetworkBuilder<Object>("WI_Hierarchy", context, true).buildNetwork();
		netAllocation = new NetworkBuilder<Object>("Allocation", context, true).buildNetwork();
		
		width = 10;
		height = 10;
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
		visualizeWorkItemNetwork();
		visualizeOrganization();
		updateKanbanBoard();
	}
	public void initializeOrganization() {
		for (ServiceProviderAgent agent : SoS.myServiceProviderAgents.values()) {											
			int x=5;
			int y=SoS.OrgSize*5-agent.getId()*5;
			agent.location[0]=x; agent.location[1]=y;
			System.out.println(agent.getName()+" loc:["+x+","+y+"]");
			grid2D.moveTo(agent,x,y);
			int c = 1;
			for (ResourceEntity resource:agent.getMyResourceEntities()) {
				grid2D.moveTo(resource, agent.location[0]+c+1,agent.location[1]);
				c++;
			}
		}
	}

	public void visualizeWorkItemNetwork() {
		netWI_Hierarchy.removeEdges();
		int count[] = new int[SoS.WINLevels];
		for (WorkItemEntity wItem: SoS.arrivedList.values()) {
			if (!wItem.isAssistanceTask && !wItem.isAnalysisTask) {
				int hierarchy = wItem.hierarchy;
				count[hierarchy]++;
				wItem.location[0]= count[hierarchy]* (int)(Math.pow((SoS.WINLevels-hierarchy+1),2)-2*(SoS.WINLevels-hierarchy));
				wItem.location[1] = gridWINsize[1]-hierarchy*10-5;
				gridWIN.moveTo(wItem, wItem.location[0], wItem.location[1]);
				if (wItem.isAggregationNode) {
					for (int i=0;i<((AggregationNode)wItem).getSubtasks().size();i++) {
						WorkItemEntity wItemsTask = ((AggregationNode)wItem).getSubtasks().get(i);
						if ( SoS.arrivedList.containsKey(wItemsTask.getId()) ){
							netWI_Hierarchy.addEdge(wItem,wItemsTask);
						}
					}
				}
			}
		}
	}
	public void visualizeOrganization() {	
		netAllocation.removeEdges();
		for (ServiceProviderAgent agent:SoS.myServiceProviderAgents.values()){
			int count;
			count=1;
			for (WorkItemEntity task:agent.getBacklogQ()){
				grid2D.moveTo(task, agent.location[0]+count+1,agent.location[1]-2);
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getActiveQ()){
				grid2D.moveTo(task, agent.location[0]+count+1,agent.location[1]-1);
				for (ResourceEntity resource:task.getAllocatedResources()) {
					netAllocation.addEdge(task,resource);
				}
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getCompleteQ()){
				grid2D.moveTo(task, agent.location[0],agent.location[1]);
			}
			for (WorkItemEntity task:agent.getComplexQ()){
				grid2D.moveTo(task, agent.location[0]+count+1,agent.location[1]-3);
				count++;
			}
		}
	}
	public void updateKanbanBoard() {
		for (KanbanElement element:this.SoS.myKanbanBoard.KanbanElements) {
			gridKanban.moveTo(element, element.location[0], 20-element.location[1]-1);
		}
	}
}
