package za.redbridge.simulator;

import org.jbox2d.dynamics.World;
import org.jbox2d.common.Vec2;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.config.SchemaConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import sim.engine.Steppable;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import sim.engine.SimState;

import org.jbox2d.dynamics.Body;

import za.redbridge.simulator.object.RobotObject;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.Iterator;

import za.redbridge.simulator.ConstructionZone;

/*
 *  The construction task class
 *
 */

public class ConstructionTask implements Steppable{
    //CONCURRENCY CHANGES
    private CopyOnWriteArrayList<ResourceObject> resources;
    private SchemaConfig schema;
    private HashMap<ResourceObject, ArrayList<ResourceObject>> weldMap;
    private World physicsWorld;
    private ArrayList<RobotObject> currentRobots;

    private Vec2[] prevBotLocations;

    private FitnessStats fitnessStats;
    //private double teamFitness;

    private ConstructionZone constructionZone;
    private int maxSteps = 0;

    private boolean IS_FIRST_CONNECTED = true;

    public ConstructionTask(SchemaConfig schema, CopyOnWriteArrayList<ResourceObject> r, ArrayList<RobotObject> robots, World world, int maxSteps) {
        this.schema = schema;
        resources = r;
        currentRobots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        for(int k = 0; k < resources.size(); k++) {
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(k), temp);
        }
        physicsWorld = world;
        this.maxSteps = maxSteps;
        constructionZone = new ConstructionZone(maxSteps);
        fitnessStats = new FitnessStats(maxSteps);
        update();
    }

    public ConstructionTask(String path, World world, ArrayList<RobotObject> robots){
        // System.out.println("THIS OTHER");
        schema = new SchemaConfig(path,1,3);
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }

        //do same as above for robots

        physicsWorld = world;
        update();
    }

    public ConstructionTask(String path, ArrayList<RobotObject> robots, World world, int maxSteps) {
        //schema = new SchemaConfig("configs/schemaConfig.yml", 1, 3);
        schema = new SchemaConfig(path, 1, 3);
        physicsWorld = world;
        this.currentRobots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        fitnessStats = new FitnessStats(maxSteps);
    }

    //not sure when this gets called
    public ConstructionTask(String path, World world){
        // System.out.println("THIS OTHER 2");
        schema = new SchemaConfig("configs/schemaConfig.yml",1,3);
        physicsWorld = world;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
    }

    //CONCURRENCY
    public void addResources(CopyOnWriteArrayList<ResourceObject> r) {
        resources = r;
        for(ResourceObject resource : resources) {
            resource.updateAdjacent(resources);
        }
        for(int k = 0; k < resources.size(); k++) {
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(k), temp);
        }
    }

    public CopyOnWriteArrayList<ResourceObject> getSimulationResources() {
        return resources;
    }

    /*public void addResources(CopyOnWriteArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }
        checkPotentialWeld(r.get(0), r.get(1)); //this might need to be commented out
    }*/

    @Override
    public void step(SimState simState) {
        Simulation s = (Simulation) simState;
        /*for(ResourceObject firstR : resources){
            for(ResourceObject secondR : resources){
                if(firstR != secondR){
                    // check if join between resources has been made before
                    boolean t = false;
                    for(int i=0;i<weldMap.get(firstR).size();i++){
                        if(weldMap.get(firstR).get(i)==secondR){
                            t = true;
                            break;
                        }
                    }
                    float distance = firstR.getBody().getPosition().sub(secondR.getBody().getPosition()).length();
                    if(distance < 3f && t==false){
                        // if(checkPotentialWeld(firstR, secondR)){
                        //     Joint joint = physicsWorld.createJoint(createWeld(firstR, secondR));
                        //     weldMap.get(firstR).add(secondR);
                        //     weldMap.get(secondR).add(firstR);
                        //     System.out.println("Create weld");
                        // }
                    }
                }
            }
        }*/
        //MIGHT HAVE TO UNCOMMENT THIS WITH THE UPDATE METHOD IN THE CONSTRUCTOR
        // if(s.schedule.getSteps() > 0) {
        //     update();
        // }
    }

    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;
    }

    private WeldJointDef createWeld(ResourceObject r1, ResourceObject r2){
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = r1.getBody();
        wjd.bodyB = r2.getBody();
        wjd.localAnchorA.set(wjd.bodyA.getPosition());
        wjd.localAnchorB.set(wjd.bodyB.getPosition());
        wjd.collideConnected = true;
        return wjd;
    }

    public void update(CopyOnWriteArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
    }

    public void update(){  
        // System.out.println("Starting update"); 
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
            String [] resAdjacentList = resource.getAdjacentResources();
            for (int i = 0; i < resAdjacentList.length; i++) {
                if (IS_FIRST_CONNECTED) {
                    // System.out.println("FIRST");
                    if (!resAdjacentList[i].equals("0")) {
                        // System.out.println("CONNECTION!!");
                        ResourceObject otherRes = resource.getAdjacentList()[i];
                        // System.out.println("ConstructionZone: " + constructionZone + " RESOURCE: " + resource);
                        constructionZone.addResource(resource);
                        constructionZone.addResource(otherRes);
                        // System.out.println(constructionZone.getFitnessStats().getTeamFitness());
                        IS_FIRST_CONNECTED = false;
                    }
                }
                else {
                    // System.out.println("AFTER");
                    if ((!resAdjacentList[i].equals("0"))&&(!constructionZone.isInConstructionZone(resource))) {
                        ResourceObject otherRes = resource.getAdjacentList()[i];
                        if (constructionZone.isInConstructionZone(otherRes)) {
                            // System.out.println("NEW CONNECTION!!");
                            constructionZone.addResource(resource);
                        }
                    }
                }
            }
        }
    }

    public int getNumConnectedResources() {
        return constructionZone.getNumberOfConnectedResources();
    }

    // public void update(){
    //     for(ResourceObject resource : resources){
    //         resource.updateAdjacent(resources);
    //     }
    // }

    public void printConnected(){
        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentResources();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    public int checkSchema(int i) {
        int correctSides = 0;
        for(ResourceObject resource : resources) {
            correctSides += schema.checkConfig(i, resource.getType(), resource.getAdjacentResources());
        }

        return correctSides;
    }

    // public int checkSchema(int i){
    //     System.out.println("ConstructionTask: This method actually gets called");
    //     int correct = 0;
    //     for(ResourceObject resource : resources){
    //         if(schema.checkConfig(i,resource.getType(), resource.getAdjacentResources())){
    //             System.out.print("Resource "+resource.getType()+" is correct -> ");
    //             for(int j=0;j<resource.getAdjacentResources().length;j++){
    //                 System.out.print(resource.getAdjacentResources()[j]+" ");
    //             }
    //             System.out.println();
    //             correct++;
    //         }
    //     }
    //     return correct;
    // }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    public ConstructionZone getConstructionZone() {
        return constructionZone;
    }

    // private double calculateFitness() {
    //     System.out.println("ConstructionTask: calculateFitness method called");
    //     double fitness = calculateDistanceFitness();
    //     //System.out.println("ConstructionTask: fitness value = " + fitness);
    //     return fitness;
    // }

    // private double calculateDistanceFitness() {
    //     //System.out.println("ConstructionTask: calculateDistanceFitness method called");
    //     double averageFitness = 0;
    //     for(RobotObject robot : currentRobots) {
    //         float smallestDistance = 10000;
    //         for(ResourceObject resource : resources) {
    //             float distanceBetween = robot.getBody().getPosition().sub(resource.getBody().getPosition()).length();
    //             if(distanceBetween <= smallestDistance) {
    //                 smallestDistance = distanceBetween;
    //             }
    //         }
    //         averageFitness += 20*(1/(1+smallestDistance));
    //     }
    //     averageFitness = averageFitness/currentRobots.size();

    //     //System.out.println("ConstructionTask (calculateDistanceFitness): average team fitness = " + averageFitness);
    //     return averageFitness;
    // }

    // private double calculateConstructionFitness() {
    //     double fitness = 0;
    //     update();
    //     int numberAdjacentResources = 0;
    //     for(ResourceObject resource : resources) {
    //         numberAdjacentResources += resource.getAdjacentResources().length;
    //     }
    //     fitness += numberAdjacentResources * 80;
    //     return fitness;
    // }
}
