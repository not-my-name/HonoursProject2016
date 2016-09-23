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

    private final DescriptiveStatistics performanceStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics scoreStats = new SynchronizedDescriptiveStatistics();
    //private final DescriptiveStatistics sensorStats;

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology) {
        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;

    }

    /*
    The network gets created and evolved by the encog library
    The ML method then gets sent here (the network)
    The simulation the gets setup according to specifications and the robots are created using
    the decoded network for the controller
    The robots are then tested in the simulator and are given a score based on their performance
    */

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

        //FitnessMonitor fitnessMonitor = new FitnessMonitor(robotFactory.getNumRobots());

        // Create the simulation and run it
        //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        Simulation simulation = new Simulation(simConfig, robotFactory);
        simulation.setStopOnceCollected(true);

        /*SimulationGUI video = new SimulationGUI(simulation);
        Console console = new Console(video);
        console.setVisible(true);*/

        /*
        this is where the genomes get trained in the simulator using the robots
        */ 

        double fitness = 0;
        for (int i = 0; i < simulationRuns; i++) {
            //System.out.println("ScoreCalculator: printing from inside the simulation loop");
            simulation.run();
            fitness += simulation.getFitness();
            //System.out.println("ScoreCalculator: finished the simulation");
            //fitness += 1D / simulation.getRobotToNearestResourceDistances(); //the new fitness from Josh
            //fitness += simulation.performFitnessCalc();
            //fitness += simulation.getFitness();
            //fitness += simulation.getFitness();//simulation.getFitness().getTeamFitness(); 
            //fitness += 20 * (1.0 - simulation.getProgressFraction()); // Time bonus
            //System.out.println("ScoreCalculator: the fitness is = " + fitness);
        }

        // Get the fitness and update the total score
        double score = fitness / simulationRuns;
        scoreStats.addValue(score);

        log.debug("Score calculation completed: " + score);

        //demo(method);

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        performanceStats.addValue(duration);

        return score;
    }

    public void demo(MLMethod method) {
        // Create the robot and resource factories

        System.out.println("ScoreCalculator: starting the demo method");

        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
                simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                simConfig.getObjectsRobots());

        //FitnessMonitor fitnessMonitor = new FitnessMonitor(robotFactory.getNumRobots());

        // Create the simulation and run it
        Simulation simulation = new Simulation(simConfig, robotFactory);
        simulation.start();

        SimulationGUI video = new SimulationGUI(simulation);

        //new console which displays this simulation
        Console console = new Console(video);
        console.setVisible(true);
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
