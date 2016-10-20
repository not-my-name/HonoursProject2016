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
import java.util.HashSet;

/*
 *  The construction task class
 *
 */

public class ConstructionTask implements Steppable{
    //CONCURRENCY CHANGES
    private ArrayList<ResourceObject> resources;
    private SchemaConfig schema;
    private HashMap<ResourceObject, ArrayList<ResourceObject>> weldMap;
    private World physicsWorld;
    private ArrayList<RobotObject> currentRobots;

    private Vec2[] prevBotLocations;

    private FitnessStats fitnessStats;
    //private double teamFitness;

    //change this to an array of construction zones to support multiple construction zones
    private ConstructionZone[] constructionZones;
    private int numConstructionZones; //var to keep track of how many construction zones have been placed in the array
    //private ConstructionZone constructionZone;
    //private int maxSteps = 0;

    private boolean IS_FIRST_CONNECTED = true;

    private int schemaNumber;
    private HashSet<ResourceObject> movedResources;
    //private HashSet<ResourceObject> constructedResources;

    public ConstructionTask(SchemaConfig schema, ArrayList<ResourceObject> r, ArrayList<RobotObject> robots, World world, int schemaNumber) {
        this.schema = schema;
        this.schemaNumber = schemaNumber;
        resources = r;
        currentRobots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();

        for(int k = 0; k < resources.size(); k++) {
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(k), temp);
        }

        physicsWorld = world;
        //this.maxSteps = maxSteps;

        /**
        change this to work with a variable for different numbers of resources in different simulations
        */
        int numResources = 15; 
        this.constructionZones = new ConstructionZone[numResources/2]; //the maximum number of construction zones possible
        numConstructionZones = 0;

        //constructionZone = new ConstructionZone();
        //fitnessStats = new FitnessStats(maxSteps);

        //constructedResources = new HashSet<ResourceObject>();
        movedResources = new HashSet<ResourceObject>();

        update();
    }

    //CONCURRENCY
    public void addResources(ArrayList<ResourceObject> r) {
        resources = r;
        for(ResourceObject resource : resources) {
            resource.updateAdjacent(resources);
        }
        for(int k = 0; k < resources.size(); k++) {
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(k), temp);
        }
    }

    public ArrayList<ResourceObject> getSimulationResources() {
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

        System.out.println("ConstructionTask Step: the step method is being called");

        for(ResourceObject r1 : resources){

            for(ResourceObject r2 : resources){
                if( r1 != r2) {

                    if(constructionZone.getConnectedResources().isEmpty()){
                        tryCreateWeld(r1, r2);
                    }   
                    else{ 
                        if(constructionZone.getConnectedResources().contains(r1) || constructionZone.getConnectedResources().contains(r2)){
                            tryCreateWeld(r1, r2);
                        }
                    }
                }
            }
        }
    }

    // @Override
    // public void step(SimState simState) {
    //     Simulation s = (Simulation) simState;
    //     for(ResourceObject firstR : resources){
    //         for(ResourceObject secondR : resources){
    //             if(firstR != secondR){
    //                 // check if join between resources has been made before
    //                 boolean t = false;
    //                 for(int i=0;i<weldMap.get(firstR).size();i++){
    //                     if(weldMap.get(firstR).get(i)==secondR){
    //                         t = true;
    //                         break;
    //                     }
    //                 }
    //                 float distance = firstR.getBody().getPosition().sub(secondR.getBody().getPosition()).length();
    //                 if(distance < 3f && t==false){
    //                     // if(checkPotentialWeld(firstR, secondR)){
    //                     //     Joint joint = physicsWorld.createJoint(createWeld(firstR, secondR));
    //                     //     weldMap.get(firstR).add(secondR);
    //                     //     weldMap.get(secondR).add(firstR);
    //                     //     System.out.println("Create weld");
    //                     // }
    //                 }
    //             }
    //         }
    //     }
    //     //MIGHT HAVE TO UNCOMMENT THIS WITH THE UPDATE METHOD IN THE CONSTRUCTOR
    //     // if(s.schedule.getSteps() > 0) {
    //     //     update();
    //     // }
    // }

    private void tryCreateWeld(ResourceObject res1, ResourceObject r2) {

        System.out.println("ConstructionTask tryCreateWeld: this method should not be being called");

        //check if the join between the resources has been made before
        if( (res1 != res2) && ( !r1.isFullyWelded() ) && ( !res2.isFullyWelded() ) ) {

            boolean flag = false; //boolean that shows whether or not a connection between these resources already exists
            for(int k = 0; k < weldMap.get(res1).size(); k++) {

                if(weldMap.get(res1).get(k) == res2) {
                    flag = true;
                    break;
                }

            }

            float distance = r1.getBody().getPosition().sub(r2.getBody().getPosition()).length();

            if(distance < 3f && flag==false){ //check if the resources are close enough for a weld to form

                if(checkPotentialWeld(r1, r2)){

                    WeldJointDef weldDef = r1.createResourceWeldJoint(r2);
                    Joint joint = physicsWorld.createJoint(weldDef);
                    weldMap.get(r1).add(r2);
                    weldMap.get(r2).add(r1);

                    //check for existing construction zones
                    if( numConstructionZones == 0 ) { //if none of the previour resources have been connected

                        constructionZones[numConstructionZones].startConstruction(res1, res2); 
                        numConstructionZones++;
                        res1.setConstructed();
                        res2.setConstructed();

                    }
                    else { //check if either of the resources are part of an existing construction zone
                        boolean found = false; //indicate whether one of the resources was already connected to an existing construction zone

                        for(int j = 0; j < numConstructionZones; j++) {

                            if( constructionZones[j].getConnectedResources().contains(res1) ) { //if the res1 is already in a construction zone

                                //add res2 to the construction zone
                                constructionZones[j].addResource(res2);
                                res2.setConstructed();
                                found = true;
                                break;

                            }
                            else if( constructionZones[j].getConnectedResources().contains(res2) ) { //if res2 is already in a construction zone

                                //add res1 to the construction zone
                                constructionZones[j].addResource(res1);
                                res1.setConstructed();
                                found = true;
                                break;

                            }

                        }

                        if(!found) { //if the neither of the resources were connected to one of the existing construction zones

                            //need to create a new construction zone
                            constructionZones[numConstructionZones].startConstruction(res1, res2);
                            numConstructionZones++;
                            res1.setConstructed();
                            res2.setConstructed();

                        }

                    }

                    //constructedResources.add(r1);
                    //constructedResources.add(r2);

                    // constructionZone.addResource(r1);
                    // constructionZone.addResource(r2);
                    // r1.setConstructed();
                    // r2.setConstructed();

                    // TODO: work on setting static after welding
                    // r1.setStatic();
                    // r2.setStatic();
                }
            }

        }

    }

    // private void tryCreateWeld(ResourceObject r1, ResourceObject r2){
    //     if(r1 != r2 && !r1.isFullyWelded() && !r2.isFullyWelded()){
    //         // check if join between resources has been made before
    //         boolean t = false;
    //         for(int i=0;i<weldMap.get(r1).size();i++){
    //             if(weldMap.get(r1).get(i)==r2){
    //                 t = true;
    //                 break;
    //             }
    //         }

    //         float distance = r1.getBody().getPosition().sub(r2.getBody().getPosition()).length();

    //         if(distance < 3f && t==false){ //check if the resources are close enough for a weld to form
    //             if(checkPotentialWeld(r1, r2)){
    //                 WeldJointDef weldDef = r1.createResourceWeldJoint(r2);
    //                 Joint joint = physicsWorld.createJoint(weldDef);
    //                 weldMap.get(r1).add(r2);
    //                 weldMap.get(r2).add(r1);
    //                 //constructedResources.add(r1);
    //                 //constructedResources.add(r2);
    //                 constructionZone.addResource(r1);
    //                 constructionZone.addResource(r2);
    //                 r1.setConstructed();
    //                 r2.setConstructed();
    //                 // TODO: work on setting static after welding
    //                 // r1.setStatic();
    //                 // r2.setStatic();
    //             }
    //         }
    //     }
    // }


    /**
    should this not be changed to an &&
    */
    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;resAdjacentList
    }

    // private WeldJointDef createWeld(ResourceObject r1, ResourceObject r2){
    //     WeldJointDef wjd = new WeldJointDef();
    //     wjd.bodyA = r1.getBody();
    //     wjd.bodyB = r2.getBody();
    //     wjd.localAnchorA.set(wjd.bodyA.getPosition());
    //     wjd.localAnchorB.set(wjd.bodyB.getPosition());
    //     wjd.collideConnected = true;
    //     return wjd;
    // }

    public void update(ArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
    }

    /**
    below is the original update method for a single construction zone 
    */

    public void update(){  
        // System.out.println("Starting update"); 
        for(ResourceObject resource : resources){

            resource.updateAdjacent(resources);
            String [] resAdjacentList = resource.getAdjacentResources();

            for (int i = 0; i < resAdjacentList.length; i++) {

                if( !resAdjacentList[i].equals("_") ) {

                    ResourceObject[] adjObjects - resource.getAdjacentList();
                    ResourceObject neighbour = adjObjects[i];

                    if(numConstructionZones == 0) {

                        ResourceObject[] adjObjects = resource.getAdjacentList();
                        constructionZones[numConstructionZones] = new ConstructionZone();
                        constructionZones[numConstructionZones].startConstruction(resource, adjObjects[i]); //use numConstructoinZones to index since = 0
                        numConstructionZones++;
                        resource.setConstructed();
                        neighbour.setConstructed();
                    }
                    else { //check if either of the resources are in a construction zone already

                        boolean found = false;

                        for(int k = 0; k < numConstructionZones; k++) { //iterate over the different construction zones

                            if( constructionZones[k].getConnectedResources().contains(resource) ) { //if the first resource is in the construction zone

                                constructionZones[k].addResource(neighbour);
                                neighbour.setConstructed();
                                found = true;
                                break;
                            }
                            else if( constructionZones[k].getConnectedResources().contains(neighbour) ) { //if the neighbouring resource is in the construction zone

                                constructionZones[k].addResource(resource);
                                resource.setConstructed();
                                found = true;
                                break; //dont need to keep iterating over the construction Zones if the right one has been found
                            }
                        }

                        //if there are construction zones but neither of the resources are found in any of them
                        if( !found ) { //add a new construction zone and add these resources to it

                            constructionZones[numConstructionZones] = new ContructionZone();
                            constructionZones[numConstructionZones].startConstruction(resource, neighbour);
                            numConstructionZones++;
                            resource.setConstructed();
                            neighbour.setConstructed();
                        }

                    }

                }




                // if (IS_FIRST_CONNECTED) {
                //     // System.out.println("FIRST");
                //     if (!resAdjacentList[i].equals("_")) {
                //         // System.out.println("CONNECTION!!");
                //         ResourceObject otherRes = resource.getAdjacentList()[i];
                //         // System.out.println("ConstructionZone: " + constructionZone + " RESOURCE: " + resource);
                //         constructionZone.addResource(resource);
                //         constructionZone.addResource(otherRes);
                //         // System.out.println(constructionZone.getFitnessStats().getTeamFitness());
                //         //tryCreateWeld(resource, otherRes);
                //         IS_FIRST_CONNECTED = false;
                //     }
                // }
                // else {
                //     // System.out.println("AFTER");
                //     if ((!resAdjacentList[i].equals("_"))&&(!constructionZone.isInConstructionZone(resource))) {
                //         ResourceObject otherRes = resource.getAdjacentList()[i];
                //         if (constructionZone.isInConstructionZone(otherRes)) {
                //             // System.out.println("NEW CONNECTION!!");
                //             constructionZone.addResource(resource);
                //             //tryCreateWeld(resource, otherRes);
                //         }
                //     }
                // }
            }
        }
    }

    // public void update() {

    //     for(ResourceObject currentRes : resources) {

    //         currentRes.updateAdjacent(resources);
    //         String[] resAdjacentList = currentRes.getAdjacentResources();

    //         for(int k = 0; k < resAdjacentList.length; k++) { //iterate over the adjacent resources

    //             if( !resAdjacentList[k].equals("_") ){ //if the current res has another resource attached to one of its 4 sides

    //                 ResourceObject otherRes = currentRes.getAdjacentList()[k];
    //                 tryCreateWeld(currentRes, otherRes);
    //             }

    //         }
    //     }
    // }

    //method to return the total number of resources that are placed in the particular simulation
    public int getTotalNumResources() {
        return resources.size();
    }

    //para: which construction zone to find
    public int getNumConnectedResources(int cZoneIndex) {
        return constructionZones[cZoneIndex].getNumberOfConnectedResources();
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

    /**
    added this method so that the schema can be checked for each resource at a time
    this method is being called in the Behaviour class to calculate the number of correctly connected resources for each resource
    needed to be able to send through the specific resource that needed to be checked
    */
    public int checkSchema(int schemaConfigNum, ResourceObject resObjec) {

        int correctSides = 0;
        correctSides += schema.checkConfig(schemaConfigNum, resObj.getType(), resObj.getAdjacentResources());
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

    public ConstructionZone[] getConstructionZones() {
        return constructionZones;
    }

    public int getNumConstructionZones() {
        return numConstructionZones;
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
