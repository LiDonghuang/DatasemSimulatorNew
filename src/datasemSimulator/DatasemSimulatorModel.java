package datasemSimulator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;


public class DatasemSimulatorModel implements ContextBuilder<Object>{
	
	private File scenarioXmlFile = new File("SimulationScenarios/ExperimentModel.xml");
	private SimulationContextBuilder contextbuilder = new SimulationContextBuilder(scenarioXmlFile);
	public boolean VisualizationOn = true;
	
	public Context<Object> build(Context<Object> context) {
		try {
			FileUtils.deleteDirectory(new File("SimulationOutputs/"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//context.setId("DatasemSimulator");
		contextbuilder.ContextImplementation(context);
		Parameters p = RunEnvironment.getInstance().getParameters();
		int numReplications = (Integer)p.getValue("NumReplications");
		System.out.println("\nREPLICATION #"+numReplications+"\n");

		return context;
	}
}