package za.redbridge.simulator;

import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.object.ResourceObject;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.*;
import za.redbridge.simulator.object.RobotObject;
import org.jbox2d.common.Vec2;

/*
use this class to manage and monitor all the different behaviours that are needed to find the novelty
score of an individual
*/

public class Behaviour {

	private ConstructionTask constructionTask;
	private ConstructionZone[] constructionZones;

	private int numConstructionZones;
	private int numPickups; //number of times robots picked up a resource
	private double distanceTravelled; //the total distance travelled by a team of robots in a single simulation
	private int schemaConfigNum; //the schema config number that is currently being used in this simulation

	private ArrayList<ResourceObject> placedResources; //positions of all the resources at the end of the simulation
	private ArrayList<RobotObject> placedRobots;

	//private ArrayList<double> robToResDist; //array to store the distance from each robot to its nearest resource

	private double avgResToResDist; //variable to store the average distance between resources at the end of the simulation
	private double avgRobToResDist; //variable to store the average distance between a robot and the nearest resource
	private double avgResToCZoneDist; //variable to store the average distance between a resource and the centre of the construction zone

	//variables to count how many different types of each block there are in the construction zone
	private int connectedA;
	private int connectedB;
	private int connectedC;

	//variables to hold the respective scores for evaluating the constructoin zones of the behaviour
	private double adjacentScore;
	private double correctSchemaScore;

	public Behaviour(ConstructionTask constructionTask, ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, double distanceTravelled, int schemaConfigNum) {
		
		this.constructionTask = constructionTask;
		this.numConstructionZones = this.constructionTask.getNumConstructionZones();
		/**
		check if this constructionZones array gets referenced properly
		*/
		this.constructionZones = this.constructionTask.getConstructionZones();
		this.schemaConfigNum = schemaConfigNum;
		this.distanceTravelled = distanceTravelled;

		placedResources = new ArrayList<ResourceObject>();
		placedRobots = new ArrayList<RobotObject>();

		numPickups = 0;
		distanceTravelled = 0;
		avgResToResDist = 0;
		avgRobToResDist = 0;
		avgResToCZoneDist = 0;
		adjacentScore = 0;
		correctSchemaScore = 0;

		connectedA = 0;
		connectedB = 0;
		connectedC = 0;

		//using methods to initialise values
		setPlacedRobots(currentRobots);
		setPlacedResources(currentResources);
		setDistanceTravelled(distanceTravelled);
		countPickups();
		countConnected();

		calcResToResDist(); 
		calcRobToResDist();
		calcResToCZoneDist();
		calcCZoneScores();
	}

	private void setDistanceTravelled(double distanceTravelled) {
		this.distanceTravelled = distanceTravelled;
	}

	//method to get the resources and their respective locations at the end of the simulation
	private void setPlacedResources(ArrayList<ResourceObject> placedResources) {

		for(ResourceObject rO : placedResources) {
			this.placedResources.add(rO);
		}

	}

	private void setPlacedRobots(ArrayList<RobotObject> placedRobots) {

		for(RobotObject r : placedRobots) {
			this.placedRobots.add(r);
		}
	}

	/*
	method to iterate over the list of robots at the end of the simulation
	and sum the total number of times that a resource was picked up
	*/
	private void countPickups() {

		for(RobotObject bot : this.placedRobots) {
			numPickups += bot.getNumPickups();
		}

		numPickups = numPickups / this.placedRobots.size(); //average number of pickups per robot

	}

	/**
	need to make sure that this is what Geoff meant by counting the number of connected blocks
	currently only checking the connected blocks in the construction zone
	should probably check on all connected blocks somehow

	THIS METHOD BELOW SHOULD PROBABLY BE REPLACED BY THE calcAdjacentScore method further down
	*/
	private void countConnected() {

		for(int k = 0; k < numConstructionZones; k++) {

			connectedA += constructionZones[k].getACount();
			connectedB += constructionZones[k].getBCount();
			connectedC += constructionZones[k].getCCount();
		}
	}

	//method to calculate the distance between each robot and the resouurce closest to it
	private void calcRobToResDist() {

		for(RobotObject r : placedRobots) {

			double minDist = 10000; //the distance between the current robot and its nearest resource
			Vec2 robotPosition = r.getBody().getPosition();

			for(ResourceObject res : placedResources) {

				Vec2 resPosition = res.getBody().getPosition();
				double actualDist = calculateDistance(robotPosition, resPosition);

				if( actualDist < minDist ) {
					minDist = actualDist;
				}
			}
			avgRobToResDist += minDist;
		}

		avgRobToResDist = avgRobToResDist / placedRobots.size();
	}

	//method to calculate the average distance between each resource at the end of the simulation
	private void calcResToResDist() {
		/**
		-also make sure that you only check the resources that are not in the construction zone
		*/
		for(int k = 0; k < placedResources.size()-1; k++) {
			Vec2 origin = placedResources.get(k).getBody().getPosition();

			for(int j = k+1; j < placedResources.size(); j++) {

				Vec2 destination = placedResources.get(j).getBody().getPosition();
				avgResToResDist += calculateDistance(origin, destination);
			}
		}

		avgResToResDist = avgResToResDist/placedResources.size();
	}

	//method to calculate the average distance between the resources and the centre of the construction zone
	private void calcResToCZoneDist() {

		/**
		should only be checking the resources that are not in the construction zone ??
		*/

		//iterate over all the resources
		//find the cZone nearest to each resource
		//use that distance for calculation

		avgResToCZoneDist = 0;
		int nearestCZ = -1;
		float smallestDist = 100000000;

		for(ResourceObject resObj : placedResources) { //iterate over all the resources

			Vec2 originPoint = resObj.getBody().getPosition();

			for(int k = 0; k < constructionZones.length; k++) { //finding the nearest construction zone

				Vec2 destPoint = constructionZones[k].getCZonePosition();
				float tempDist = calculateDistance(originPoint, destPoint);

				if(tempDist < smallestDist) {

					smallestDist = tempDist;
					nearestCZ = k;
				}
			}

			avgResToCZoneDist += smallestDist;
		}

		avgResToCZoneDist = avgResToCZoneDist / placedResources.size(); //average distance to construction zone per resource
	}

	/**
	check that all these calculations for the construction zones are done right
	*/

	//method to calculate the fitness for each construction zone
	private void calcCZoneScores() {

		double[] cZoneScores = new double[numConstructionZones];

		adjacentScore = 0; //variable to store the combined score for connected resources over all construction zones
		correctSchemaScore = 0; //variable to store the average score for resources connected according to the schema per construction zone (penalised for > 1 cZone)

		for(int k = 0; k < numConstructionZones; k++) { //initialise all scores to 0
			cZoneScore[k] = 0;
		}

		adjacentScore += calcAdjacentScore();
		correctSchemaScore += calcSchemaScore();

	}

	//method to calculate the score for the total number of resources that are connected
	//in all of the construction zones
	private double calcAdjacentscore() {

		//sum total number of connected resources over all the construction zones
		//divide by total number of resource in the simulation

		int totalNumConnected = 0;

		for(int k = 0; k < numConstructionZones; k++) {
			totalNumConnected += constructionZones[k].getNumConnected();
		}

		totalNumConnected = totalNumConnected/this.constructionTask.getTotalNumResources();

		return totalNumConnected;
	}

	//method to calculate the average schema score for this behaviour
	//average schema score per construction zone
	//penalised for having > 1 construction zone
	/**
	need to get the schema config number from somewhere
	can maybe set it from the simulation and send through to Behaviour's constructor
	schemaConfig number needs to be set in the simulation
	*/
	private double calcSchemaScore() {

		for(int k = 0; k < numConstructionZones; k++) { //iterate over the construction zones in the simulation

			ResourceObject[] resInCZone = constructionZones[k].getConnectionOrder();
			int upperBound = constructionZones[k].getNumConnected(); //the number of resources that are placed in the ordered array above
			int resScore = 0; //var to store (and sum) the scores for each individual resource
			int correctSides = 0; //var to store the number of correctly connected sides on each resource
			int totalConnected = 0; //var to keep track of how many sides of a resource has a connection on it

			for(int j = 0; j < upperBound; j++) { //iterate over all the connected resources in the current construction zone

				//var to store the number of correctly connected sides on each resource
				correctSides += constructionTask.checkSchema(schemaConfigNum, resInCZone[j]);
				//var to count the total number of sides that the current resource is connected on
				int connectedSides = ; 
			}

		}

	}

	private float calculateDistance(Vec2 originLocation, Vec2 destLocation) {

        double firstX = originLocation.x;
        double firstY = originLocation.y;

        double secondX = destLocation.x;
        double secondY = destLocation.y;

        float distance = (float) Math.sqrt(
                        Math.pow(firstX-secondX, 2) +
                        Math.pow(firstY-secondY, 2));

        return distance;

    }

    public double getAdjacentScore() {
    	return adjacentScore;
    }

    public double getSchemaScore() {
    	return correctSchemaScore;
    }

    public double getRobToResDist() {
    	return this.avgRobToResDist;
    }

    public int getNumRobots() {
    	return this.placedRobots.size();
    }

	public ConstructionZone[] getConstructionZones() {
		return this.constructionZones;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public ArrayList<ResourceObject> getPlacedResources() {
		return this.placedResources;
	}

	public int getNumPickups() {
		return this.numPickups;
	}

	public double getDistanceTravelled() {
		return this.distanceTravelled;
	}

	public double getAvgResToResDist() {
		return this.avgResToResDist;
	}

	public double getAvgRobToResDist() {
		return this.avgRobToResDist;
	}

	public double getAvgResToCZoneDist() {
		return this.avgResToCZoneDist;
	}

	public int getConnectedA() {
		return this.connectedA;
	}

	public int getConnectedB() {
		return this.connectedB;
	}

	public int getConnectedC() {
		return this.connectedC;
	}

	public int compareStructure (Behaviour newBehaviour) {
		return 0;
	}
}