/*
Constructing a Neural Network
-> A simple network can quickly be constructed using BasicLayer and BasicNetwork objects

-> creating several BasicLayer objects with a default hyperbolic tangent activation function

BasicNetwork network = new BasicNetwork();
network.addLayer(new BasicLayer(2));
network.addLayer(new BasicLayer(3));
network.addLayer(new BasicLayer(1));

network.getStructure().finalizeStruture();
network.reset();

-> this network will have an input layer of 2 neurons, a hidden layer with 3 neurons and an
    output layer with 1 neuron

-> always add a bias neuron, they allow the activation function to shift off the origin of 0
   allows the neural network to produce a zero value even when the inputs are not zero

-> what is going to be used for our phenotype?
will have to use the ANNs that need to be evolved

-> Substrate factory creates the substrates for HyperNEAT
-> substrate defines the structure of the produced HyperNEAT network
-> a HyperNEAT network works by training a CPPN that produces the actual resulting NEAT network

-> HyperNEATCodec is used to decode the substrate into a NEAT network

-> HyperNEAT genome factory creates the HyperNEAT genomes

*/

import org.encog.*;

public class MainClass {

	public static void main(String args[]) {

		/*========================================================================================
		FROM AN EXAMPLE ON GITHUB

		https://github.com/encog/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/neat/boxes/VisualizeBoxesMain.java

		-> create the substrate using the substrate factory
		-> initialise the scoring mechanism
		-> initialise the population using NEATPopulation and the previously created substrate
		-> set activation cycles for the NEATPopulation
		-> reset the population
		-> create the NEAT trainer (NEATUtil.constructNEATTrainer(population, scoringMechanism))
		-> construct the speciation mechanism
		-> set compatibility threshold for speciation
		-> set speciation for the trainer
		*/

		Substrate substrate = SubstrateFactory.factorSandwichSubstrate(numInputs, numOutputs);
		ScoreCalculator calculateScore = new ScoreCalculator(); //need to decide on a score calculator
		final NEATPopulation population = NEATPopulation(substrate, populationSize); 
		population.setActivationCycles(4); //what the hell is this supposed to be
		population.reset();
		EvolutionaryAlgorithm trainer = NEATUtil.constructNEATTrainer(population, calculateScore);
		OriginalNEATSpeciation speciation = new OriginalNEATSpeciation();
		speciation.setCompatibilityThreshold(1);
		trainer.setSpeciation(speciation = new OriginalNEATSpeciation()); //why does the speciation get constructed twice
		/*
		======================================================================================
		*/






		/*=========================================================================
		FROM THE MAIN CLASS IN THE CONTROLLER MASTER FOLDER
		---------------------------------------------------
		*/
		ScoreCalculator calculateScore = new ScoreCalculator(simConfig, options.simulationRuns,
			morphology); //got this from the Main class in last years Controller Master folder

		//not sure if we are going to be using this
		//this is used to check whether or not a genome has been defined in the config
		if(!isBlank(options.genomePath)) { //if genome defined
			NEATNetwork network = (NEATNetwork) readObjectFromFile(options.genomePath); //create network using defined genome
			calculateScore.demo(network); //run a demo of the simulation using the given network
			return;	//check what the demo method of the scoring method actually does
		}

		//initialising the population
		final NEATPopulation population = new NEATPopulation(substrate, populationSize);
		population.setInitialConnectionDensity(options.connectionDensity); //set the density based on a value that gets passed through using that Args options nested class thing in Main.java
		population.reset();

		TrainEA trainer = NEATUtil.constructNEATTrainer(population, calculateScore);

		//not too sure what this means, probably the threads for multithreading
		if(thread_count > 0) {
			trainer.setThreadCount(thread_count);
		}

		final StatsRecorder statsRecorder = new StatsRecorder(trainer, calculateScore);
		statsRecorder.recordIterationStats(); //record the stats of the iterations

		for(int i = trainer.getIteration(); i < options.numIterations; i++) { //running the training loop for set number of iterations
			trainer.iteration(); //training the network for a single iteration
			statsRecorder.recordIterationStats();

			//once an individual has found an optimal solution, break out of the training loop
			if(trainer.getBestGenome().getScore() >= convergence_score) {
				log.info("convergence reached at epoch(iteration): " + trainer.getIteration());
				break;
			}
		}

		/*
		======================================================================================
		*/

	}

}

