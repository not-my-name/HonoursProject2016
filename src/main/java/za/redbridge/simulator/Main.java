package za.redbridge.simulator;

import org.encog.neural.hyperneat.substrate.Substrate;
import za.redbridge.simulator.ScoreCalculator;
import org.encog.neural.neat.NEATPopulation; //importing the neat population
import za.redbridge.simulator.config.MorphologyConfig;
import org.encog.neural.neat.NEATNetwork;
//import org.encog.neural.hyperneat.substrate.SubstrateFactory;
import org.encog.ml.ea.train.basic.TrainEA;
import za.redbridge.simulator.StatsRecorder;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.NEATUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.text.ParseException;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;

import za.redbridge.simulator.phenotype.ChasingPhenotype;

import org.encog.ml.train.strategy.Strategy;
import java.util.List;

import static za.redbridge.simulator.Utils.isBlank;
import static za.redbridge.simulator.Utils.readObjectFromFile;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	private static final double convergenceScore = 1000;

	private int numInputs = 3;
	private int numOutputs = 2;
	private int populationSize = 5;
	private int simulationRuns = 3;
	private int numIterations = 15;
	private int threadCount = 1;

	private static String resConfig;

	public static void main(String args[]) throws IOException, ParseException{

		Args options = new Args();
		new JCommander(options, args);

		log.info(options.toString);

		double connectionDensity = 0.5;

		String simConfigFP = "configs/simConfig.yml";
		String experimentConfigFP = "configs/experimentConfig.yml";
		String morphologyConfigFP = "configs/morphologyConfig.yml";

		MorphologyConfig morphologyConfig = new MorphologyConfig(morphologyConfigFP);
		Morphology morphology = morphologyConfig.getMorphology(1);
		numInputs = morphology.getNumSensors();

		SimConfig simConfig;
		if( !isBlank(options.configFile) ) {
			simConfig = new SimConfig(options.configFile);
		}
		else {
			simConfig = new SimConfig();
		}
		//simConfig.setRobotCount(populationSize);

		ScoreCalculator scoreCalculator = new ScoreCalculator(simConfig, simulationRuns, 
						morphology); //got this from the Main class in last years Controller Master folder

		//defines the structure of the produced HyperNEAT network
		Substrate substrate = SubstrateFactory.createSubstrate(3,2);
		//substrate.setActivationCycles(4); These get set by default

		//System.out.println("Main: SUBSTRATE input count after: " + substrate.getInputCount());
		//System.out.println("Main: SUBSTRATE output count after: " + substrate.getOutputCount());


		//initialising the population
		NEATPopulation population = new NEATPopulation(substrate, populationSize);

		//System.out.println("Main: POPULATION input count after: " + population.getInputCount());
		//System.out.println("Main: POPULATION output count after: " + population.getOutputCount());
		population.setInitialConnectionDensity(connectionDensity); //set the density based on a value that gets passed through using that Args options nested class thing in Main.java
		population.setActivationCycles(4); //THIS COULD BE IMPORTANT (FROM ONLINE EXAMPLE)
		population.reset();

		//System.out.println("MainClass: check if the population is HyperNEAT: " + population.isHyperNEAT());

		//constructNEATTrainer creates a HyperNEAT trainer since a NEAT population is being sent as parameter
		TrainEA trainer = NEATUtil.constructNEATTrainer(population, scoreCalculator);
		//System.out.println("MainClass: Constructing the trainer");

		//================================================================================

		// OriginalNEATSpeciation speciation = new OriginalNEATSpeciation();
		// speciation.setCompatibilityThreshold(1);
		// trainer.setSpeciation(speciation);

		//System.out.println("MainClass: Checking the thread count");
		// if(thread_count > 0) {
		// 	trainer.setThreadCount(thread_count);
		// }

		trainer.setThreadCount(1);

		//System.out.println("Main: pre iteration species check (should be 25 members) = " + population.getSpecies().size());

		final StatsRecorder statsRecorder = new StatsRecorder(trainer, scoreCalculator); //this is basically where the simulation runs
		//statsRecorder.recordIterationStats(); //record the stats of the iterations

		//System.out.println("MainClass: checking the iteration of simulation = " + trainer.getIteration());

		scoreCalculator.demo(trainer.getCODEC().decode(trainer.getBestGenome()));

		// for(int i = 0; i < numIterations; i++) { //for(int i = trainer.getIteration(); i < numIterations; i++)
		// 	trainer.iteration(); //training the network for a single iteration
		// 	//System.out.println("size of array of species: " + trainer.getPopulation().getSpecies().size());
		// 	statsRecorder.recordIterationStats();
		// 	//System.out.println("made it past the stats recorder");

		// 	//once an individual has found an optimal solution, break out of the training loop
		// 	if(trainer.getBestGenome().getScore() >= convergenceScore) {
		// 		log.info("convergence reached at epoch(iteration): " + trainer.getIteration());
		// 		break;
		// 	}
		// }

		// //System.out.println("made it this far");

		// scoreCalculator.demo(trainer.getCODEC().decode(trainer.getBestGenome()));

	}

}

