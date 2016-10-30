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
    //private ConstructionZone[] constructionZones;
    private ArrayList<ConstructionZone> constructionZones;
    //private ConstructionZone constructionZone;
    //private int maxSteps = 0;

    private boolean IS_FIRST_CONNECTED = true;

    private int schemaNumber;
    private HashSet<ResourceObject> movedResources;
    //private HashSet<ResourceObject> constructedResources;

    private ArrayList<ResourceObject> globalConstructionOrder;

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
        int numResources = resources.size(); 
        this.constructionZones = new ArrayList<ConstructionZone>(); //the maximum number of construction zones possible
        movedResources = new HashSet<ResourceObject>();

        update();
    }

    public ArrayList<ResourceObject> getSimulationResources() {
        return resources;
    }

    @Override
    public void step(SimState simState) {
        update();
    }

    /**
    should this not be changed to an &&
    */
    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;
    }

    //this update method gets called to check if the resources that have been added to the adjacency lists
    public void update(){  

        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }

        for(ResourceObject resource : resources){

            resource.updateAdjacent(resources);
            String [] resAdjacentList = resource.getAdjacentResources();
            ResourceObject[] adjObjects = resource.getAdjacentList();

            /**
            need to add a check to see if the resources are connected according to the schema
            */

            for (int i = 0; i < resAdjacentList.length; i++) {

                if( !resAdjacentList[i].equals("_") ) {

                    ResourceObject neighbour = adjObjects[i];

                    if(constructionZones.size() == 0) {

                        ConstructionZone newZone = new ConstructionZone();
                        newZone.startConstructionZone(resource, neighbour);
                        constructionZones.add(newZone);
                    }
                    else { //check if either of the resources are in a construction zone already
                        boolean found = false;

                        for(ConstructionZone cZone : constructionZones) {

                            if( cZone.getConnectedResources().contains(resource) ) {

                                cZone.addResource(neighbour);
                                found = true;
                                break;
                            }
                            else if( cZone.getConnectedResources().contains(neighbour) ) {

                                cZone.addResource(resource);
                                found = true;
                                break;
                            }
                        }

                        //if there are construction zones but neither of the resources are found in any of them
                        if( !found ) { //add a new construction zone and add these resources to it

                            ConstructionZone tempZone = new ConstructionZone();
                            tempZone.startConstructionZone(resource, neighbour);
                            constructionZones.add(tempZone);
                        }
                    }
                }
            }
        }
    }

    //method to return the total number of resources that are placed in the particular simulation
    public int getTotalNumResources() {
        return resources.size();
    }

    // //para: which construction zone to find
    // public int getNumConnectedResources(int cZoneIndex) {
    //     return constructionZones[cZoneIndex].getNumberOfConnectedResources();
    // }

    public void printConnected() {

        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentResources();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    // public int checkSchema(int i) {

    //     int correctSides = 0;
    //     for(ResourceObject resource : resources) {
    //         correctSides += schema.checkConfig(i, resource.getType(), resource.getAdjacentResources());
    //     }

    //     return correctSides;
    // }

    /**
    added this method so that the schema can be checked for each resource at a time
    this method is being called in the Behaviour class to calculate the number of correctly connected resources for each resource
    needed to be able to send through the specific resource that needed to be checked
    */
    public int checkSchema(int schemaConfigNum, ResourceObject resObj) {

        int correctSides = 0;
        correctSides += schema.checkConfig(schemaConfigNum, resObj.getType(), resObj.getAdjacentResources());
        return correctSides;
    }

    public int checkSchema(ResourceObject resObj) {

        int correctSides = 0;
        correctSides += schema.checkConfig(schemaNumber, resObj.getType(), resObj.getAdjacentResources());
        return correctSides;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    public ArrayList<ConstructionZone> getConstructionZones() {
        return constructionZones;
    }
}
