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
	//private ConstructionZone[] constructionZones;
	private ArrayList<ConstructionZone> constructionZones;
	private ConstructionZone mostValuableCZ; //variable to hold the most valuable construction zone that the robot team created during the simulation

	private int numConstructionZones;
	private double numPickups; //number of times robots picked up a resource
	private int schemaConfigNum; //the schema config number that is currently being used in this simulation
	private double maxDist;

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

	/**
	perform calculations using the largest construction zone
	can get the largest construction zone from the construction task
	*/

	public Behaviour(ConstructionTask constructionTask, ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, int schemaConfigNum) {

		this.constructionTask = constructionTask;
		/**
		check if this constructionZones array gets referenced properly
		*/
		this.constructionZones = this.constructionTask.getConstructionZones();
		this.schemaConfigNum = schemaConfigNum;

		numConstructionZones = this.constructionZones.size();

		mostValuableCZ = this.constructionTask.getBestConstructionZone();

		placedResources = new ArrayList<ResourceObject>();
		placedRobots = new ArrayList<RobotObject>();

		numPickups = 0;
		avgResToResDist = 0;
		avgRobToResDist = 0;
		avgResToCZoneDist = 0;
		adjacentScore = 0;
		correctSchemaScore = 0;

		connectedA = 0;
		connectedB = 0;
		connectedC = 0;

		maxDist = this.constructionTask.getMaxDistance();

		//using methods to initialise values
		setPlacedRobots(currentRobots);
		setPlacedResources(currentResources);
		countPickups();
		countConnected();

		calcResToResDist();
		calcRobToResDist();
		calcResToCZoneDist();
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

	private void countConnected() {

		/**
		change this to work with the biggest construction zone
		*/

		for(ConstructionZone cZone : constructionZones) {

			connectedA += cZone.getACount();
			connectedB += cZone.getBCount();
			connectedC += cZone.getCCount();

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

				if( actualDist < minDist ) { //find the distance of closest resource
					minDist = actualDist;
				}
			}
			avgRobToResDist += minDist;
		}

		avgRobToResDist = avgRobToResDist / placedRobots.size();
	}

	//method to calculate the average distance between each resource at the end of the simulation
	private void calcResToResDist() {

		avgResToResDist = 0;

		for(int k = 0; k < placedResources.size(); k++) {
			ResourceObject currentResource = placedResources.get(k);

			if(currentResource.isConstructed()) {
				continue;
			}

			Vec2 origin = currentResource.getBody().getPosition();
			double minDist = 100000000;

			for(int j = 0; j < placedResources.size(); j++) {

				if(j != k) { //check you dont compare the same resource to itself

					ResourceObject otherResource = placedResources.get(j);
					Vec2 destination = otherResource.getBody().getPosition();
					double tempDist = calculateDistance(origin, destination);

					if(tempDist < minDist) {
						minDist = tempDist;
					}
				}
			}

			avgResToResDist += minDist;
		}

		avgResToResDist = avgResToResDist/placedResources.size();
	}

	//method to calculate the average distance between the resources and the centre of the construction zone
	private void calcResToCZoneDist() {

		int numNotConstructed = 0; //variable to keep track of the number of resources not connected in a construction zone
		avgResToCZoneDist = 0;

		if(numConstructionZones > 0) { //check if there are even any construction zones

			for(ResourceObject resObj : placedResources) { //iterate over all the resources

				if(resObj.isConstructed()) { //skip the resources that are already in a construction zone
					continue;
				}
				else {

					numNotConstructed++;
					Vec2 origin = resObj.getBody().getPosition();
					double smallestDist = 100000000;

					for(ConstructionZone cZone : constructionZones) { //iterate over all the construction zones

						Vec2 destination = cZone.getCZonePosition();
						double tempDist = calculateDistance(origin, destination);

						if(tempDist < smallestDist) {
							smallestDist = tempDist;
						}
					}

					avgResToCZoneDist += smallestDist;
				}
			}

			if (numNotConstructed > 0) { //if some resources are not connected to construction zones
				avgResToCZoneDist = avgResToCZoneDist / numNotConstructed; //average distance to construction zone per resource;
			}
			else if (numNotConstructed == 0) { //if all of the resources are connected to a construction zone
				avgResToCZoneDist = 0; //should return 0 so that the function calculates a fitness of 1 (perfect score)
			} //this else statement might not be necessary, if numNotConstructed

		}
		else if (numConstructionZones == 0) { //return the max possible distance so that the calc for fitness returns 0 (if no construction zones)
			System.out.println("Behaviour: no construction zones. score should be 0");
			avgResToCZoneDist = maxDist;
		}



	}

	//method to calculate the score for the total number of resources that are connected
	//in all of the construction zones
	private double calcAdjacentScore() {

		//sum total number of connected resources over all the construction zones
		//divide by total number of resource in the simulation

		int totalNumConnected = 0;

		for(ConstructionZone cZone : constructionZones) {
			totalNumConnected += cZone.getNumConnected();
		}

		totalNumConnected = totalNumConnected/this.constructionTask.getTotalNumResources();

		return totalNumConnected;
	}

	private float calculateDistance(Vec2 originLocation, Vec2 destLocation) {

        double firstX = originLocation.x;
        double firstY = originLocation.y;

        double secondX = destLocation.x;
        double secondY = destLocation.y;

        float distance = (float) Math.sqrt(
                        Math.pow(firstX-secondX, 2) +
                        Math.pow(firstY-secondY, 2));

				// float distance = (float)(Math.pow(firstX-secondX, 2) +
        //                 				Math.pow(firstY-secondY, 2));

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

	public ArrayList<ConstructionZone> getConstructionZones() {
		return constructionZones;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public ArrayList<ResourceObject> getPlacedResources() {
		return this.placedResources;
	}

	public double getNumPickups() {
		return this.numPickups;
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
