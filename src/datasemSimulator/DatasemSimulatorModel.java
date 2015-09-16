package datasemSimulator;

import java.io.File;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class DatasemSimulatorModel implements ContextBuilder<Object>{

	public Context<Object> build(Context<Object> context) {

		context.setId("DatasemSimulator");
		Parameters p = RunEnvironment.getInstance().getParameters();
		
		SimulationContextBuilder cb = new SimulationContextBuilder();
		File scenarioXmlFile = new File("/Users/Donbghuang Li/Desktop/DatasemFiles/DemoModel_ExperimentModel.xml");
		//File scenarioXmlFile = new File("SimulationScenarios/Scenario1.xml");
		cb.ReadXMLFile(scenarioXmlFile);	
		cb.ContextImplementation(context);
		return context;
	}
}