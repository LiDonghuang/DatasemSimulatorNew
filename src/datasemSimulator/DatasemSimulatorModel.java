package datasemSimulator;

import java.io.File;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;


public class DatasemSimulatorModel implements ContextBuilder<Object>{
	
	private File scenarioXmlFile = new File("SimulationScenarios/ExperimentModel.xml");
	private SimulationContextBuilder contextbuilder = new SimulationContextBuilder(scenarioXmlFile);
	public boolean VisualizationOn = true;
	
	public Context<Object> build(Context<Object> context) {
		File folder = new File("SimulationOutputs");
		deleteFolder(folder);
		//context.setId("DatasemSimulator");
		contextbuilder.ContextImplementation(context);
		Parameters p = RunEnvironment.getInstance().getParameters();
		int numReplications = (Integer)p.getValue("NumReplications");
		System.out.println("\nREPLICATION #"+numReplications+"\n");

		return context;
	}
	
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	}
}