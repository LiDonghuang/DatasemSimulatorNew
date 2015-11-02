package visualization;

import java.text.DecimalFormat;
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
	private ArrayList<Comments> commentsList= new ArrayList<Comments>();
	
	public Visualization(Context<Object> ctx, SystemOfSystems mySoS) {
		parameters = RunEnvironment.getInstance().getParameters();
		ctx.add(this);
		context = ctx;
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);	
		SoS = mySoS;
		grid2Dsize[0]=SoS.OrgLevels*20; grid2Dsize[1]=SoS.OrgSize*5;
		gridWINsize[0]=SoS.WINSize*(int)Math.pow(SoS.WINComplexity-1, SoS.WINLevels-1); gridWINsize[1]=SoS.WINLevels*10;
		
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

	@ScheduledMethod(start=0,interval=1,priority=-1)
	public void step() {
		this.context.removeAll(commentsList);
		this.commentsList.clear();
		visualizeWorkItemNetwork();
		visualizeOrganization();
		updateKanbanBoard();
	}
	public void initializeOrganization() {
		for (ServiceProviderAgent agent : SoS.myServiceProviderAgents.values()) {											
			int x=5+3*agent.hierarchy;
			int y=SoS.OrgSize*5-agent.getId()*5;
			agent.icon.location[0]=x; agent.icon.location[1]=y;			
			grid2D.moveTo(agent,x,y);
			int c = 1;
			for (ResourceEntity resource:agent.getMyResourceEntities()) {
				grid2D.moveTo(resource, agent.icon.location[0]+c+2,agent.icon.location[1]);
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
			if ((!wItem.isResolutionActivity)&&(!wItem.isAnalysisActivity)) {
				int hierarchy = wItem.hierarchy;
				count[hierarchy]++;
				//wItem.icon.location[0] = count[hierarchy]* (int)(Math.pow((SoS.WINLevels-hierarchy),2)-2*(SoS.WINLevels-hierarchy+1));
				wItem.icon.location[0] = count[hierarchy]*(int)(Math.pow(SoS.WINComplexity-1,SoS.WINLevels-hierarchy-1)+1);
				wItem.icon.location[1] = gridWINsize[1]-hierarchy*10-5;
				if (wItem.isStarted) {
					wItem.icon.color[0]=0;wItem.icon.color[1]=128;wItem.icon.color[2]=255;
					if (wItem.isSuspended) {
						wItem.icon.color[0]=255;wItem.icon.color[1]=0;wItem.icon.color[2]=0;
					}
					if (wItem.isCompleted) {
						wItem.icon.color[0]=0;wItem.icon.color[1]=255;wItem.icon.color[2]=0;
					}
				}
				else {
					wItem.icon.color[0]=224;wItem.icon.color[1]=224;wItem.icon.color[2]=224;
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
				wItem.icon.location[0] = wItem1.AnalysisObject.icon.location[0]-2;
				wItem.icon.location[1] = wItem1.AnalysisObject.icon.location[1]-2;
				wItem.icon.color[0]=0;wItem.icon.color[1]=255;wItem.icon.color[2]=0;
				gridWIN.moveTo(wItem, wItem.icon.location[0], wItem.icon.location[1]);
				netWI_Hierarchy.addEdge(wItem,wItem1.AnalysisObject);
			}
			else if (wItem.isResolutionActivity) {
				ResolutionActivity wItem1 = (ResolutionActivity)wItem;
				wItem.icon.location[0] = wItem1.ResolutionObject.icon.location[0]-2;
				wItem.icon.location[1] = wItem1.ResolutionObject.icon.location[1]-2;
				wItem.icon.color[0]=255;wItem.icon.color[1]=0;wItem.icon.color[2]=0;
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
			if (agent.getResourceUtilization()<=0.1) {
				agent.icon.color[0]=0;agent.icon.color[1]=255;agent.icon.color[2]=255;
			}
			else {
				agent.icon.color[0]=0;agent.icon.color[1]=0;agent.icon.color[2]=255;
			}
			count=1;
			for (WorkItemEntity task:agent.getBacklogQ()){
				grid2D.moveTo(task, agent.icon.location[0]+count+2,agent.icon.location[1]-2);
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getActiveQ()){
				grid2D.moveTo(task, agent.icon.location[0]+count+2,agent.icon.location[1]-1);
				for (ResourceEntity resource:task.getAllocatedResources()) {
					netAllocation.addEdge(task,resource);
				}
				count++;
			}
			count=1;
			for (WorkItemEntity task:agent.getCompleteQ()){
				grid2D.moveTo(task, 0,0);
			}
			count=1;
			for (WorkItemEntity task:agent.getComplexQ()){
				grid2D.moveTo(task, agent.icon.location[0]+count+2,agent.icon.location[1]-3);
				count++;
			}
			commentSP(agent);
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
		comments.addComment("currentValue:"+(int)wi.currentValue);
		if (!wi.isAggregationNode) {
			comments.addComment(SoS.myServices.get(wi.serviceId).getName()+" x"+wi.efforts);
		}
		this.commentsList.add(comments);
		this.context.add(comments);
		gridWIN.moveTo(comments, wi.icon.location[0],wi.icon.location[1]+1);
	}
	public void commentSP(ServiceProviderAgent sp) {
		Comments comments = new Comments();
		comments.addComment("Type:\n "+this.SoS.myServiceProviderTypes.get(sp.typeId).getName());
		this.commentsList.add(comments);
		this.context.add(comments);
		grid2D.moveTo(comments, sp.icon.location[0]-2,sp.icon.location[1]+1);
		comments.addComment("Utilization:\n "+Integer.toString((int)(sp.getResourceUtilization()*100))+"%");
		this.commentsList.add(comments);
		this.context.add(comments);
		grid2D.moveTo(comments, sp.icon.location[0]-2,sp.icon.location[1]);
		
		Comments comments1 = new Comments();
		comments1.addComment("ActiveQ");
		this.commentsList.add(comments1);
		this.context.add(comments1);
		grid2D.moveTo(comments1, sp.icon.location[0]+1,sp.icon.location[1]-1);	
		Comments comments2 = new Comments();
		comments2.addComment("Backlog");
		this.commentsList.add(comments2);
		this.context.add(comments2);
		grid2D.moveTo(comments2, sp.icon.location[0]+1,sp.icon.location[1]-2);	
		Comments comments3 = new Comments();
		comments3.addComment("ComplexQ");
		this.commentsList.add(comments3);
		this.context.add(comments3);
		grid2D.moveTo(comments3, sp.icon.location[0]+1,sp.icon.location[1]-3);
	}
}
