package za.redbridge.simulator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.networks.BasicNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.phenotype.Phenotype;

import za.redbridge.simulator.HyperNEATPhenotype;
import za.redbridge.simulator.Morphology;

import za.redbridge.simulator.FitnessMonitor;

import za.redbridge.simulator.factories.ConfigurableResourceFactory;
import za.redbridge.simulator.factories.ResourceFactory;

/**
 * Test runner for the simulation.
 * 
 * Created by jamie on 2014/09/09.
 */
public class ScoreCalculator implements CalculateScore {

    private static final Logger log = LoggerFactory.getLogger(ScoreCalculator.class);

    private final SimConfig simConfig;
    private final int simulationRuns;
    private final Morphology sensorMorphology;

    private int schemaConfigNum;

    private final DescriptiveStatistics performanceStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics scoreStats = new SynchronizedDescriptiveStatistics();
    //private final DescriptiveStatistics sensorStats;

    private Archive archive;

    //private ArrayList<NoveltyBehaviour> currentPopulation; //list to store the novelty functions and the aggregate behaviours of the current generation
    private int populationSize;
    private int numResults; //the number of results that will be produced for populationSize individuals being tested * numSimulationRuns (since one result per simulation) this is only for novelty
    private NoveltyBehaviour[] currentPopulation;
    private int currentBehaviour; //keep track of how many of the individuals in the generation have been processed

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, int populationSize) {
        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;
        this.populationSize = populationSize;

        //currentPopulation = new ArrayList<NoveltyBehaviour>();
        currentBehaviour = 0;
        numResults = populationSize * simulationRuns;
        currentPopulation = new NoveltyBehaviour[numResults];

        /**
        need to change the schema config to work with the variables so that the config number can change
        */
        schemaConfigNum = 0;

        archive = null;
    }

    @Override
    public double calculateScore(MLMethod method) {

        long start = System.nanoTime();

        // NEATNetwork neat_network = null;
        // RobotFactory robotFactory;

        // //System.out.println("ScoreCalculator: PHENOTYPE for NEATNetwork: " + getPhenotypeForNetwork(neat_network));
        // neat_network = (NEATNetwork) method;
        // robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
        //             simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
        //             simConfig.getObjectsRobots());

        // // Create the simulation and run it
        // //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        // // create new configurable resource factory
        // String [] resQuantity = {"0","0","0"};
        // ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        // resourceFactory.configure(simConfig.getResources(), resQuantity);

        // Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory);

        // simulation.setSchemaConfigNumber(schemaConfigNum);

        /**
        NOVELTY SEARCH
        the results of each trial run for an individual is considered as a completely separate results
        each trial run is treated like an individual
        this means the values produced in the trial runs dont need to be averaged to get a representative behaviour
        */
        // double fitness = 0;
        // for (int i = 0; i < simulationRuns; i++) {

        //     /*
        //     currentBehaviour will never be => numResults while the loop is still running
        //     when currentBehaviour == numResults its because the last simulation for the last individual has been run (SHOULD BE)
        //     */
        //     if(currentBehaviour < numResults) {
        //         currentPopulation[currentBehaviour] = simulation.runNovel(); //adding the simulation results to the array of results for the current generation
        //         currentBehaviour++; //keep track of the index that the next behaviour will be stored in
        //     }

        // }

        // if( currentBehaviour == numResults ) { //checking that the currentPopulation array is full 

            
        //     need to find the final novelty score per individual instead of having a score for each test run
        //     iterate over the array of returned values
        //     sum up the values in multiples of simulationRuns
        //     divide by the number of simulation runs for each individual
            

        //     NoveltyFitness noveltyFitnessCalculator = new FitnessMonitor(currentPopulation);
        //     double[] fitnessArray = noveltyFitnessCalculator.calculateNoveltyFitness();

        //     for(int k = 0; k < fitnessArray.length; k += simulationRuns) { //iterate in steps of simulation runs
        //         double score = 0; //the total avg fitness for an individual

        //         for(int j = k; j < k+simulationRuns; j++) {
        //             score += fitnessArray[j];
        //         }

        //         score = score / simulationRuns; //average score for an individual over simulationRuns
        //         scoreStats.addValue(score); //recording the average score of the individual
        //     }

        //     //clearing the array and resetting the counter
        //     currentBehaviour = 0;
        //     Arrays.fill(currentPopulation, null);

        // }

        /**
        all of the actual simulation runs and score calculations are performed in the preIteration methods of the NoveltySStrategy class
        when TrainEA calls iteration() and then calls .calculateScore() 
        */

        if(PerformingNoveltyCalcs) { //need to have a way of checking when performing objective or novelty

            NoveltyNetwork novNet = (NoveltyNetwork)method;
            NoveltyBehaviour beh = novNet.getNoveltyBehaviour();

            if(beh == null) { //check if this behaviour has already been "processed"
                return 0;
            }

            double noveltyScore = archive.getNovelty(beh);
            scoreStats.addValue(noveltyScore);
            return noveltyScore;

        }

        /**
        OBJECTIVE SEARCH
        */
        //creates a new aggregate behaviour for every individual in the population. represents the average behaviour of the individual over the test simulation runs
        AggregateBehaviour aggregateBehaviour = new AggregateBehaviour(simulationRuns, schemaConfigNum); //creating a new aggregate behaviour to store the test runs results for this specific individual

        double fitness = 0;
        for(int i = 0; i < simulationRuns; i++) {
            aggregateBehaviour.addSimBehaviour(simulation.runObjective());
        }

        //send the aggregate behaviour of the individual to a new objective fitness class
        ObjectiveFitness objectiveFitness = new ObjectiveFitness(aggregateBehaviour);
        double score = objectiveFitness.calculate(); //calculate the objective fitness for the individual currently being evaluated
        scoreStats.addValue(score);

        log.debug("Score calculation completed: " + score);

        //demo(method);


        /**aggregateBehaviour
        FINS OUT WHAT THE PERFORMANCE STATS IS USED FOR AND IF THIS MAKES A DIFFERENCE IN THE NOVELTY PART
        LIKE SHOULD THIS BE CALLED AT A DIFFERENT TIME IN ORDER TO WORK WITH THE POPULATION ARRAY
        */
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        performanceStats.addValue(duration);

        return score;
    }

    public void demo(MLMethod method) {
        // Create the robot and resource factories

        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
                simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                simConfig.getObjectsRobots());

        // Create the simulation and run it
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory);
        simulation.start();

        SimulationGUI video = new SimulationGUI(simulation);

        //new console which displays this simulation
        Console console = new Console(video);
        console.setVisible(true);
    }

    public void calculateNoveltyForPopulation() {
        //have access to the archive
        //in archive calculate novelty for each individual in the current generation

        archive.calculatePopoulationNovelty();

        //method in the Archive:
        //iterate over all the behaviours in the current generation
        //calculate their behavioural sparseness for each one
    }

    public NoveltyBehaviour getNoveltyBehaviour(MLMethod method) {
        /**
        run the simulation x times for the given method
        find the most novel behaviour
        return most novel

        THIS IS THE ETHOD WHERE THE SIMULATION GETS CALLED FOR NOVELTY SEARCH
        creates the Noveltybehaviour classes
        also adds the resultant behaviour to the current generation list in the archive
        */

        NEATNetwork neat_network = null;
        RobotFactory robotFactory;

        //System.out.println("ScoreCalculator: PHENOTYPE for NEATNetwork: " + getPhenotypeForNetwork(neat_network));
        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // Create the simulation and run it
        //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory);

        simulation.setSchemaConfigNumber(schemaConfigNum);

        //creating an arraylist to store the novelty behaviours that are produced at the end of each simulation run
        //this is used to calculate the most novel behaviour of the produced runs
        ArrayList<NoveltyBehaviour> simulationResults = new ArrayList<NoveltyBehaviour>();

        for(int k = 0; k < simulationRuns; k++) {

            //recording all the resultant behaviours that the network produced in the different simulation runs
            simulationResults.add(simulation.runNovel());
        }

        NoveltyBehaviour[] resultsArray = new NoveltyBehaviour[simulationResults.size()];
        simulationResults.toArray(resultsArray);
        
        // double index = archive.findMostNovel(resultsArra);
        // return resultsArray(index);
        //OR
        return archive.findMostNovel(resultsArray);
    }

    public void setSchemaConfigNumber(int i) {
        schemaConfigNum = i;
    }

    public void setArchive(Archive archive) {
        this.archive = archive;
    }

    //HyperNEAT uses the NEATnetwork as well
    private Phenotype getPhenotypeForNetwork(NEATNetwork network) {
        //System.out.println("ScoreCalculator: network input = " + network.getInputCount());
        //System.out.println("ScoreCalculator: network output = " + network.getOutputCount());
        return new HyperNEATPhenotype(network, sensorMorphology);
    }

    public boolean isEvolvingMorphology() {
        return false;
    }

    public DescriptiveStatistics getPerformanceStatistics() {
        return performanceStats;
    }

    public DescriptiveStatistics getScoreStatistics() {
        return scoreStats;
    }

    /*public DescriptiveStatistics getSensorStatistics() {
        return sensorStats;
    }*/

    @Override
    public boolean shouldMinimize() {
        return false;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }

}
