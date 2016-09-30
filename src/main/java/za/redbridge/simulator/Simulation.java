package za.redbridge.simulator;

import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import java.util.Set;

import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.TargetAreaObject;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.physics.SimulationContactListener;
import za.redbridge.simulator.portrayal.DrawProxy;
import java.util.ArrayList;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;
import za.redbridge.simulator.config.SchemaConfig;

import za.redbridge.simulator.FitnessMonitor;

/**
 * The main simulation state.
 *
 * Created by jamie on 2014/07/24.
 */
public class Simulation extends SimState {

    private static final float VELOCITY_THRESHOLD = 0.000001f;

    private Continuous2D environment;
    private World physicsWorld;
    private PlacementArea placementArea;
    private DrawProxy drawProxy;

    private final SimulationContactListener contactListener = new SimulationContactListener();

    private static final float TIME_STEP = 1f / 10f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 3;

    private TargetAreaObject targetArea;
    private RobotFactory robotFactory;
    private ConstructionTask construction;
    private final SimConfig config;

    private SchemaConfig schema;

    private boolean stopOnceCollected = true;

    private double averageDistancesOverRun = 0D;

    private FitnessMonitor fitnessMonitor;

    private int schemaConfigNum;

    //private final RobotObject[] existingRobots;

    public Simulation(SimConfig config, RobotFactory robotFactory) {
        super(config.getSimulationSeed());
        this.config = config;
        this.robotFactory = robotFactory;
        Settings.velocityThreshold = VELOCITY_THRESHOLD;

        fitnessMonitor = new FitnessMonitor();
        schemaConfigNum = 0;
    }

    @Override
    public void start() {

        //System.out.println("Simulation: starting the simulation");

        //resetting the necessary values for each simulation run
        //fitnessMonitor.reset();

        fitnessMonitor.setSchemaConfigNumber(schemaConfigNum);

        super.start();

        environment =
                new Continuous2D(1.0, config.getEnvironmentWidth(), config.getEnvironmentHeight());
        drawProxy = new DrawProxy(environment.getWidth(), environment.getHeight());
        environment.setObjectLocation(drawProxy, new Double2D());

        physicsWorld = new World(new Vec2());
        placementArea =
                new PlacementArea((float) environment.getWidth(), (float) environment.getHeight());
        placementArea.setSeed(config.getSimulationSeed());
        schedule.reset();
        System.gc();

        physicsWorld.setContactListener(contactListener);

        // Create ALL the objects
        createWalls();

        // Create target area
        //createTargetArea();

        //System.out.println("Simulation: placing instances");

        robotFactory.placeInstances(placementArea.new ForType<>(), physicsWorld, config.getTargetAreaPlacement());

        //fitnessMonitor.setOriginalLocations(robotFactory.getPlacedRobots());

        /*config.getResourceFactory().setResQuantity(construction.configResQuantity(config.getConfigNumber()));
        config.getResourceFactory().placeInstances(placementArea.new ForType<>(), physicsWorld);
        construction = new ConstructionTask("configs/schemaConfig.yml", physicsWorld);
        */

        schema = new SchemaConfig("configs/schemaConfig.yml", 1, 3);
        config.getResourceFactory().setResQuantity(schema.getResQuantity(config.getConfigNumber()));
        config.getResourceFactory().placeInstances(placementArea.new ForType<>(), physicsWorld);
        construction = new ConstructionTask(schema,config.getResourceFactory().getPlacedResources(),robotFactory.getPlacedRobots(),physicsWorld, config.getSimulationIterations());
        //ArrayList<ResourceObject> resources = config.getResourceFactory().getPlacedResources();
        //construction.addResources(resources);

        //construction.printConnected();
        // construction.checkSchema(0);

        fitnessMonitor.setConstructionTask(construction);

        // Now actually add the objects that have been placed to the world and schedule
        int count = 0;
        for (PhysicalObject object : placementArea.getPlacedObjects()) {
            drawProxy.registerDrawable(object.getPortrayal());
            schedule.scheduleRepeating(object);
            /*if (object instanceof RobotObject) {
                existingRobots[count] = (RobotObject) object;
                count++;
            }*/
        }
        //System.out.println("Simulation: created the objects");

        schedule.scheduleRepeating(construction);
        //System.out.println("SImulation: schedule repeating step");

        schedule.scheduleRepeating(simState ->
            physicsWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
        );

        //System.out.println("Simulation: finished the start method");
    }

    public void setSchemaConfigNumber(int i) {
        schemaConfigNum = i;
    }

    public void setArchive(Archive archive) {
        fitnessMonitor.setArchive(archive);
    }

    // Walls are simply added to environment since they do not need updating
    private void createWalls() {
        int environmentWidth = config.getEnvironmentWidth();
        int environmentHeight = config.getEnvironmentHeight();
        // Left
        Double2D pos = new Double2D(0, environmentHeight / 2.0);
        Double2D v1 = new Double2D(0, -pos.y);
        Double2D v2 = new Double2D(0, pos.y);
        WallObject wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Right
        pos = new Double2D(environmentWidth, environmentHeight / 2.0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Top
        pos = new Double2D(environmentWidth / 2.0, 0);
        v1 = new Double2D(-pos.x, 0);
        v2 = new Double2D(pos.x, 0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Bottom
        pos = new Double2D(environmentWidth / 2.0, environmentHeight);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());
    }

    public void checkConstructionTask(){
        System.out.println("Simulation checkConstructionTask: THIS METHOD IS BEING CALLED FROM SOMEWHERE, DONT THINK IT SHOULD");
        construction.checkSchema(0);
    }

    //create target area
    private void createTargetArea() {
        int environmentWidth = config.getEnvironmentWidth();
        int environmentHeight = config.getEnvironmentHeight();

        final int width, height;
        final Vec2 position;
        // if (config.getTargetAreaPlacement() == SimConfig.Direction.SOUTH) {
        //     width = environmentWidth;
        //     height = config.getTargetAreaThickness();
        //     position = new Vec2(width / 2f, height / 2f);
        // } else if (config.getTargetAreaPlacement() == SimConfig.Direction.NORTH) {
        //     width = environmentWidth;
        //     height = config.getTargetAreaThickness();
        //     position = new Vec2(environmentWidth - width / 2f, environmentHeight - height / 2f);
        // } else if (config.getTargetAreaPlacement() == SimConfig.Direction.EAST) {
        //     width = config.getTargetAreaThickness();
        //     height = environmentHeight;
        //     position = new Vec2(environmentWidth - width / 2f, height / 2f);
        // } else if (config.getTargetAreaPlacement() == SimConfig.Direction.WEST) {
        //     width = config.getTargetAreaThickness();
        //     height = environmentHeight;
        //     position = new Vec2(width / 2f, height / 2f);
        // } else {
        //     return; // Don't know where to place this target area
        // }

        width = 3;
        height = width;
        position = new Vec2(8f, 16f);

        targetArea = new TargetAreaObject(physicsWorld, position, width, height,
                config.getResourceFactory().getTotalResourceValue(), config.getSimulationIterations());

        // Add target area to placement area (trust that space returned since nothing else placed
        // yet).
        PlacementArea.Space space = placementArea.getRectangularSpace(width, height, position, 0f);
        placementArea.placeObject(space, targetArea);
    }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        config.setSimulationSeed(seed);
    }

    private double getRobotAvgPolygonArea() {
        Set<PhysicalObject> objects = placementArea.getPlacedObjects();
        double totalArea = 0.0;

        for (PhysicalObject object: objects) {
            if (object instanceof RobotObject) {
                totalArea += ((RobotObject) object).getAverageCoveragePolgygonArea();
            }
        }
        return totalArea/config.getObjectsRobots();
    }

    /** Get the environment (forage area) for this simulation. */
    public Continuous2D getEnvironment() {
        return environment;
    }

    /**
     * Run the simulation for the number of iterations specified in the config.
     */
    public Behaviour run() {
        final int iterations = config.getSimulationIterations();
        //return runForNIterations(iterations);
        return tempRun(iterations);
    }

    //just a temp method to test another way of implementing the fitness functions
    public Behaviour tempRun(int n) {
        start();
        double distanceTravelled = 0;

        for(int i = 0; i < n; i++) {
            schedule.step(this);

            /**
            remember to change this to update the distance travelled only every other iteration
            instead of every single iteration
            */

            for(int k = 0; k < robotFactory.getPlacedRobots().size(); k++) {
                Vec2 before = robotFactory.getPlacedRobots().get(k).getPreviousPosition();
                Vec2 after = robotFactory.getPlacedRobots().get(k).getBody().getPosition();
                distanceTravelled += calculateDistance(before, after);
            }
        }

        //Behaviour behaviour = new Behaviour(construction);
        //setting up all the characteristics of the behaviour
        ArrayList<RobotObject> tempBots = robotFactory.getPlacedRobots();
        //behaviour.countPickups(tempBots);
        //behaviour.setPlacedRobots(tempBots);
        //behaviour.setPlacedResources(config.getResourceFactory().getPlacedResources()); 
        //behaviour.setDistanceTravelled(distanceTravelled);
        //behaviour.calcResToResDist();
        //behaviour.calcRobToResDist();

        Behaviour behaviour = new Behaviour(construction, tempBots, config.getResourceFactory().getPlacedResources(),
                                            distanceTravelled);

        /**
        check what happens in this finish() method and make sure that it doesnt mess with the behaviour object before you return it
        */

        finish();

        return behaviour;

    }

    private float calculateDistance(Vec2 originLocation, Vec2 destLocation) {

        double firstX = originLocation.x;
        double firstY = originLocation.y;

        double secondX = destLocation.x;
        double secondY = destLocation.y;

        //System.out.println("FitnessMonitor: the locations = " + firstX + "," + firstY + " -> " + secondX + "," + secondY);

        float distance = (float) Math.sqrt(
                        Math.pow(firstX-secondX, 2) +
                        Math.pow(firstY-secondY, 2));

        //System.out.println("FitnessMonitor: distance calculated = " + distance);

        return distance;

    }

    public double calculateObjectiveFitness(int numRuns) {
        double finalFitness = 0;

        finalFitness = objectiveFitness.calculate();

        finalFitness = finalFitness/numRuns; //average the fitness over number of times the individual was tested in the simulation
    }

    // /**
    //  * Run the simulation for a certain number of iterations.
    //  * @param n the number of iterations
    //  */
    // public void runForNIterations(int n) {
    //     start();
    //     double aveDistOverNIterations = 0D;

    //     for (int i = 0; i < n; i++) {

    //         schedule.step(this);

    //         for(int k = 0; k < robotFactory.getPlacedRobots().size(); k++) {
    //             Vec2 before = robotFactory.getPlacedRobots().get(k).getPreviousPosition();
    //             Vec2 after = robotFactory.getPlacedRobots().get(k).getBody().getPosition();
    //             fitnessMonitor.incrementDistanceTravelled(before, after);
    //         }

    //     }
    //     ArrayList<RobotObject> tempBots = robotFactory.getPlacedRobots();
    //     fitnessMonitor.savePickupCounts(tempBots);
    //     fitnessMonitor.setPlacedResources(config.getResourceFactory().getPlacedResources()); //retrieving the final positions of the resources after the simulation
    //     finish();
    // }

    public boolean allResourcesCollected() {
        return config.getResourceFactory().getNumberOfResources()
                == targetArea.getNumberOfContainedResources();
    }

    /** If true, this simulation will stop once all the resource objects have been collected. */
    public boolean isStopOnceCollected() {
        return stopOnceCollected;
    }

    /** If set true, this simulation will stop once all the resource objects have been collected. */
    public void setStopOnceCollected(boolean stopOnceCollected) {
        this.stopOnceCollected = stopOnceCollected;
    }

    //return the score at this point in the simulation
    public double getFitness() {
        return fitnessMonitor.getOverallFitness(); //not dividing by number robots for the distance travelled
    }

    /** Gets the progress of the simulation as a percentage */
    public double getProgressFraction() {
        return (double) schedule.getSteps() / config.getSimulationIterations();
    }

    /** Get the number of steps this simulation has been run for. */
    public long getStepNumber() {
        return schedule.getSteps();
    }

    /**
     * Launching the application from this main method will run the simulation in headless mode.
     */
    public static void main (String[] args) {
        doLoop(Simulation.class, args);
        System.exit(0);
    }

    /**
    Method to store the average distances over n iterations
    **/
    public void storeDistances(double distances, int n) {
        this.averageDistancesOverRun = distances/n;
    }

    public double getRobotToNearestResourceDistances () {
        return averageDistancesOverRun;
    }

    public double getAverageDistance() {
        //System.out.println("Simulation: getAverageDistance method called");
        double aveDist = 0D;

        ArrayList<RobotObject> existingRobots = robotFactory.getPlacedRobots();

        //System.out.println("Simulation: made it this far");
        //System.out.println("SIMULATION: resource factory = " + config.getResourceFactory());
        //System.out.println("SIMULATION: placed resources = " + config.getResourceFactory().getPlacedResources());
        //System.out.println("SIMULATION: number of placed resources = " + config.getResourceFactory().getPlacedResources().size());
        ResourceObject firstResource = config.getResourceFactory().getPlacedResources().get(0);
        //System.out.println("SIMULATION: first resource = " + firstResource);
        //System.out.println("SIMULATION: number of placed robots = " + existingRobots.length);
        double [] robotDistances = new double [existingRobots.size()];
        //System.out.println("Simulation: number of robots = " + existingRobots.length);
        double sumDistances = 0D;
        //System.out.println("SIMULATION: made it this far:)");
        Vec2 frPos = firstResource.getBody().getPosition();
        //System.out.println("SIMULATION: MADE IT PAST THE ERROR");
        //System.out.println("SIMULATION: this is another test = " + frPos);
        //System.out.println("SIMULATION: number of placed robots = " + existingRobots.length);
        //initiliaze distances with each robot's distance to firstResource
        for (int i = 0; i < existingRobots.size(); i++) {
            //System.out.println("SIMULATION: the current robot = " + i);
            //System.out.println("SIMULATION: the robot body = " + existingRobots[i]);
            //Vec2 robotPos = existingRobots[i].getBody().getPosition();
            Vec2 robotPos = existingRobots.get(i).getBody().getPosition();
            Vec2 dist = robotPos.add(frPos.negate());
            robotDistances[i] = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
        }
        for (int i = 0; i < existingRobots.size(); i++) {
            //Vec2 robotPos = existingRobots[i].getBody().getPosition();
            Vec2 robotPos = existingRobots.get(i).getBody().getPosition();
            int cnt = 0;
            for (ResourceObject res : config.getResourceFactory().getPlacedResources()) {
                if (cnt > 0) {
                    Vec2 resPos = res.getBody().getPosition();
                    Vec2 dist = robotPos.add(resPos.negate());
                    double distBetween = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
                    if (distBetween < robotDistances[i]) {
                        robotDistances[i] = distBetween;
                    }
                }
            }
            sumDistances += robotDistances[i];
        }

        //System.out.println("Simulation: printing the average distance = " + sumDistances/(double)existingRobots.size());
        return sumDistances/(double)existingRobots.size();
    }
}
