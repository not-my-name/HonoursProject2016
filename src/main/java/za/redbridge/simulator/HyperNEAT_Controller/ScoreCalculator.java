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
    private NoveltyBehaviour[] currentPopulation
    private int currentBehaviour; //keep track of how many of the individuals in the generation have been processed

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, int populationSize) {
        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;
        this.populationSize = populationSize;

        //currentPopulation = new ArrayList<NoveltyBehaviour>();
        currentPopulation = new NoveltyBehaviour[populationSize];

        /**
        need to change the schema config to work with the variables so that the config number can change
        */
        schemaConfigNum = 0;

        archive = null;

        currentBehaviour = 0;

    }

    @Override
    public double calculateScore(MLMethod method) {
        long start = System.nanoTime();

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

        if(this.archive != null) {
            simulation.setArchive(this.archive);
        }

        /*SimulationGUI video = new SimulationGUI(simulation);
        Console console = new Console(video);
        console.setVisible(true);*/

        /*
        this is where the genomes get trained in the simulator using the robots
        */ 

        //AggregateBehaviour aggregateBehaviour = new AggregateBehaviour(simulationRuns, schemaConfigNum);

        /**
        have a couple of different ways of calculating novelty (keep the code you have and just add the different one from josh)
        see how many other ones you can come up with, try out the novelty vector thing might be something to it
        try out the different methods
        **/

        double fitness = 0;
        for (int i = 0; i < simulationRuns; i++) {
            //System.out.println("ScoreCalculator: printing from inside the simulation loop");
            //aggregateBehaviour.addSimBehaviour(simulation.run());
            currentPopulation[i] = simulation.runNovelty();
            // simulation.run();
            // fitness += simulation.getFitness();
        }

        double score = fitness / simulationRuns;
        scoreStats.addValue(score);

        /**
        this is for the objective fitness experiments
        */
        // ObjectiveFitness objectiveFitness = new ObjectiveFitness(aggregateBehaviour);
        // double score = objectiveFitness.calculate();
        // scoreStats.addValue(score);

        /**
        this is for the novelty fitness experiments
        */

        // if(currentPopulation.size() < populationSize) {
        //     currentPopulation.add(aggregateBehaviour);
        // }
        // else if(currentPopulation.size() == populationSize) {
        //     for(int k = 0; k < populationSize; k++) {
        //         NoveltyFitness noveltyFitness = new NoveltyFitness(currentPopulation.get(k), currentPopulation, k);
        //     }
        // }

        // NoveltyFitness noveltyFitness = new NoveltyFitness(aggregateBehaviour);
        // double score = noveltyFitness.calculate();
        // scoreStats.addValue(score);

        /**
        this is for the hybrid search experiments
        */
        // HybridFitness hybridFitness = new HybridFitness(aggregateBehaviour);
        // double score  = hybridFitness.calculate();
        // scoreStats.addValue(score);

        // Get the fitness and update the total score
        // double score = fitness / simulationRuns;
        // scoreStats.addValue(score);

        log.debug("Score calculation completed: " + score);

        //demo(method);

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

        //FitnessMonitor fitnessMonitor = new FitnessMonitor(robotFactory.getNumRobots());

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
