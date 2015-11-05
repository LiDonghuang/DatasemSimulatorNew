package datasemSimulator;

import java.io.File;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;


public class DatasemSimulatorModel implements ContextBuilder<Object>{
	
	private File scenarioXmlFile = new File("/Users/dzl0023/Desktop/DatasemFiles/ExperimentModel.xml");
	private SimulationContextBuilder cb = new SimulationContextBuilder(scenarioXmlFile);
	public boolean VisualizationOn = true;
	
	public Context<Object> build(Context<Object> context) {		
		//context.setId("DatasemSimulator");
		cb.ContextImplementation(context);
		Parameters p = RunEnvironment.getInstance().getParameters();
		int numReplications = (Integer)p.getValue("NumReplications");
		System.out.println("\nREPLICATION #"+numReplications+"\n");

		return context;
	}
}