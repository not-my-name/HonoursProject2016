package za.redbridge.simulator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

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

import za.redbridge.simulator.Archive;

public class Main {

	private final static Logger log = LoggerFactory.getLogger(Main.class);
	private final static double convergenceScore = 1000;

	private final static boolean PerformingNoveltySearch;

	private static int numInputs;
	private int numOutputs = 2;
	private int populationSize;
	private int simulationRuns;
	private int numIterations;
	private int threadCount;

	private static Archive archive;

	private static int schemaConfigIndex; //the schema against which the construction zone should be checked

	private static String resConfig;

	public static void main(String args[]) throws IOException, ParseException{

		/**
		look for all the bright pink hilights to check what still needs to be done

		make sure you have the correct number of inputs for the different updated sensors (something to do with only reading the closest resource or something along those lines)

		check that all the heuristics are in place

		check that all the calculations are executed in the correct order as each others
		since there aer so many different ways of implementing it

		robots should only be able to move a resource when enough of them have attached
		does this affect the total resource pickup count in some way? should probably add some sort of check

		check you have the isAligned and isConnected checks for the resources and the constructionZone 

		what did daniel do to get his code to run so much faster

		take out the empty connections for the resources in the schema config file

		find a way to static reference the archive from this Main class so that all local copies will have the same content and get updated accordingly

		check that all the yml files are in order and 

		check that all the data structures and functions work with the correct variables. such as for the total number of resources in each simulation will change, that needs to be addressed/reference properly
		the same thing goes for the schema number and so forth


		*/

		Args options = new Args();
		new JCommander(options, args);

		log.info(options.toString());

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

		resConfig = options.environment;

		schemaConfigIndex = 0;
		ScoreCalculator scoreCalculator = new ScoreCalculator(simConfig, options.simulationRuns, 
						morphology, options.populationSize, schemaConfigIndex); //got this from the Main class in last years Controller Master folder

		if (!isBlank(options.genomePath)) {
            NEATNetwork network = (NEATNetwork) readObjectFromFile(options.genomePath);
            scoreCalculator.demo(network);
            return;
        }

		//defines the structure of the produced HyperNEAT network
		Substrate substrate = SubstrateFactory.createSubstrate(numInputs,2);

		//initialising the population
		NEATPopulation population = new NEATPopulation(substrate, options.populationSize);

		population.setInitialConnectionDensity(options.connectionDensity); //set the density based on a value that gets passed through using that Args options nested class thing in Main.java
		population.setActivationCycles(4); //THIS COULD BE IMPORTANT (FROM ONLINE EXAMPLE)
		population.reset();

		TrainEA trainer;
		if(PerformingNoveltySearch) {

			trainer = NEATUtil.constructNoveltyTrainer(population, scoreCalculator);
			trainer.addStrategy(new NoveltySearchStrategy(options.populationSize, scoreCalculator));
		}
		else {

			TrainEA trainer = NEATUtil.constructNEATTrainer(population, scoreCalculator);
		}

		trainer.setThreadCount(1);

		final StatsRecorder statsRecorder = new StatsRecorder(trainer, scoreCalculator); //this is basically where the simulation runs

		scoreCalculator.demo(trainer.getCODEC().decode(trainer.getBestGenome()));

		for(int i = 0; i < options.numGenerations; i++) { //for(int i = trainer.getIteration(); i < numIterations; i++)
			trainer.iteration(); //training the network for a single iteration
			statsRecorder.recordIterationStats();

			//once an individual has found an optimal solution, break out of the training loop
			if(trainer.getBestGenome().getScore() >= convergenceScore) {
				log.info("convergence reached at epoch(iteration): " + trainer.getIteration());
				break;
			}
		}

		scoreCalculator.demo(trainer.getCODEC().decode(trainer.getBestGenome()));
		log.debug("Training Complete");

	}

	private static class Args {
        @Parameter(names = "-c", description = "Simulation config file to load")
        private String configFile = "configs/simConfig.yml";

        @Parameter(names = "-i", description = "Number of generations to train for")
        private int numGenerations = 10;

        @Parameter(names = "-p", description = "Initial population size")
        private int populationSize = 15;

        @Parameter(names = "--sim-runs", description = "Number of simulation runs per iteration")
        private int simulationRuns = 3;

        @Parameter(names = "--conn-density", description = "Adjust the initial connection density"
                + " for the population")
        private double connectionDensity = 0.5;
        @Parameter(names = "--demo", description = "Show a GUI demo of a given genome")
        private String genomePath = null;
        //private String genomePath = "results/Hex-20160920T2134_null__NEAT/best networks/epoch-5/network.ser";
        //private String genomePath = "results/ruben-GE72-2QD-20161030T1126_null/best networks/epoch-1/network.ser";
        //private String genomePath = "results/ruben-GE72-2QD-20161102T1342_null/best networks/epoch-1/network.ser";

        @Parameter(names = "--control", description = "Run with the control case")
        private boolean control = false;

        @Parameter(names = "--advanced", description = "Run with advanced envrionment and morphology")
        private boolean advanced = true;

        @Parameter(names = "--environment", description = "Run with advanced envrionment and morphology")
        private String environment = "";

        @Parameter(names = "--morphology", description = "For use with the control case, provide"
                + " the path to a serialized MMNEATNetwork to have its morphology used for the"
                + " control case")
        private String morphologyPath = null;

        @Parameter(names = "--population", description = "To resume a previous controller, provide"
                + " the path to a serialized population")
        private String populationPath = null;

        @Override
        public String toString() {
            return "Options: \n"
                    + "\tConfig file path: " + configFile + "\n"
                    + "\tNumber of generations: " + numGenerations + "\n"
                    + "\tPopulation size: " + populationSize + "\n"
                    + "\tNumber of simulation tests per iteration: " + simulationRuns + "\n"
                    + "\tInitial connection density: " + connectionDensity + "\n"
                    + "\tDemo network config path: " + genomePath + "\n"
                    + "\tRunning with the control case: " + control + "\n"
                    + "\tMorphology path: " + morphologyPath + "\n"
                    + "\tPopulation path: " + populationPath;
        }
    }

}

