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
    private double teamFitness;

    /*
    these were the original constructors for this class
    commented out and using the code from Daniel to fix the concurrent modification errors

    /*public ConstructionTask(String path, ArrayList<ResourceObject> r, World world, ArrayList<RobotObject> robots){
        schema = new SchemaConfig(path,1,3);
        resources = r;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }

        currentRobots = robots;

        physicsWorld = world;
        update();
    }

    public ConstructionTask(String path, World world){
        schema = new SchemaConfig("configs/schemaConfig.yml",1,3);
        physicsWorld = world;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
    }*/

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
        update();
        fitnessStats = new FitnessStats(maxSteps);
    }

    public ConstructionTask(String path, ArrayList<RobotObject> robots, World world, int maxSteps) {
        schema = new SchemaConfig("configs/schemaConfig.yml", 1, 3);
        physicsWorld = world;
        this.currentRobots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        fitnessStats = new FitnessStats(maxSteps);
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

    /*public double getFitness() {
        // //calculate the fitness of the robot team 
        // //calculate the average distances between the robots and the resources

        // for(RobotObject tempRobot : currentRobots) { //iterating over the current robots
        //     //getting the world position of each robot
        //     Vec2 currentBotPosition = tempRobot.getBody().getPosition(); //getBody() uses Vector2
        //     for(ResourceObject currentResource : resources) { //iterating over all the resources left
        //         //getting the word position of each resource
        //         //resourceObjects use Vec2 for position
        //         Body resObjectBody = currentResource.getBody();
        //         Vec2 resourcePos = resObjectBody.getPosition();
        //         //Vec2 worldPos = resObjectBody.getWorldPosition();
        //         //Vector2 resourcePos = new Vector2(tempResPos.getX(), tempResPos.getY());
        //         //Vector2 worldPos = new Vector2(tempWorldPos.getX(), tempWorldPos.getY());
        //         System.out.println("ConstructionTask: the difference between positions");
        //         System.out.println("ContsructionTask: resource position = " + resourcePos);
        //         System.out.println("ConstructionTask: robot position: " + currentBotPosition);
        //     }
        // }

        return 10;
    }*/

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
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
    }

    public void printConnected(){
        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentResources();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    public int checkSchema(int i){
        System.out.println("ConstructionTask: This method actually gets called");
        int correct = 0;
        for(ResourceObject resource : resources){
            if(schema.checkConfig(i,resource.getType(), resource.getAdjacentResources())){
                System.out.print("Resource "+resource.getType()+" is correct -> ");
                for(int j=0;j<resource.getAdjacentResources().length;j++){
                    System.out.print(resource.getAdjacentResources()[j]+" ");
                }
                System.out.println();
                correct++;
            }
        }
        return correct;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    private double calculateFitness() {
        System.out.println("ConstructionTask: calculateFitness method called");
        double fitness = calculateDistanceFitness();
        //System.out.println("ConstructionTask: fitness value = " + fitness);
        return fitness;
    }

    private double calculateDistanceFitness() {
        //System.out.println("ConstructionTask: calculateDistanceFitness method called");
        double averageFitness = 0;
        for(RobotObject robot : currentRobots) {
            float smallestDistance = 10000;
            for(ResourceObject resource : resources) {
                float distanceBetween = robot.getBody().getPosition().sub(resource.getBody().getPosition()).length();
                if(distanceBetween <= smallestDistance) {
                    smallestDistance = distanceBetween;
                }
            }
            averageFitness += 20*(1/(1+smallestDistance));
        }
        averageFitness = averageFitness/currentRobots.size();

        //System.out.println("ConstructionTask (calculateDistanceFitness): average team fitness = " + averageFitness);
        return averageFitness;
    }

    private double calculateConstructionFitness() {
        double fitness = 0;
        update();
        int numberAdjacentResources = 0;
        for(ResourceObject resource : resources) {
            numberAdjacentResources += resource.getAdjacentResources().length;
        }
        fitness += numberAdjacentResources * 80;
        return fitness;
    }

    private double calculateSchemaFitness() {
        double fitness = 0;
        return fitness;
    }

    public double getFitness() {
        //System.out.println("ConstructionTask: getFitness method called");
        return calculateFitness();
    }
}
