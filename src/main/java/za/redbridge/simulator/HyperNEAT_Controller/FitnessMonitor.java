package za.redbridge.simulator.Novelty; //just changed the package so that id get errors wherever the fitnessmonitor gets used need to remove all FitnessMonitor references

import org.jbox2d.common.Vec2;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
import java.util.ArrayList;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.object.ResourceObject;
import java.util.concurrent.CopyOnWriteArrayList;

/*
Method to evaluate the overall objective fitness of a simulation run:
    A - average distance between robot and closest resource to it
    B - average number of times that robots connected to resources (done)
    C - average distance between resources (done)
    D - the number of adjacent resources (number of resources that have been connected) getConstructionFitness()
    E - the number of adjacent resources that are in the correct schema (check which of the connected resources are in the correct schema) checkSchemaFitness()
    F - average distance between resources and the construction starting area (calculate the avg distance between )
*/

public class FitnessMonitor {

	private double totalDistanceTravelled; //sum of total distance travelled by robot team in simulation
	private double numPickups; //total number of times a robot has picked up a block in the simulation
	private double numIterations;
	//private final SimConfig config;

	//the weights that are going to be used for the final fitness calculation
	private double explorationWeighting;
	private double pickupWeight;
	private double constructionWeighting;
	private double schemaWeighting;
	private double noveltyWeighting;
	private double resourceDistanceWeighting;

	private float A_Weighting;
	private float B_Weighting;
	private float C_Weighting;

	private ConstructionZone constructionZone;
	private ConstructionTask constructionTask;

	private Archive archive;

	private int schemaConfigNumber;
	private int noveltyThreshHold;

	private CopyOnWriteArrayList<ResourceObject> placedResources;

	private Behaviour behaviour;

	public FitnessMonitor() {
		this.totalDistanceTravelled = 0;
		this.numPickups = 0;
		this.numIterations = 0;
		//this.config = config;

		pickupWeight = 1;
		explorationWeighting = 1;
		constructionWeighting = 1;
		schemaWeighting = 1;
		noveltyWeighting = 1;
		resourceDistanceWeighting = 1;

		constructionZone = null;
		constructionTask = null;
		archive = null;
		behaviour = null;

		placedResources = new CopyOnWriteArrayList<ResourceObject>();

		noveltyThreshHold = 10;
		schemaConfigNumber = -1; //made this negative
		//if i forget to reset it or assign incorrect value then the -1 will at least cause a crash and
		//i can know that it was caused by this value

		A_Weighting = 1;
		B_Weighting = 1;
		C_Weighting = 1;
		
	}

	public void savePickupCounts(ArrayList<RobotObject> currentBots) {
		//System.out.println("FitnessMonitor: printing the number pickups per robot");
		for(RobotObject bot : currentBots) {
			//System.out.println("Pickups = " + bot.getNumPickups());
			numPickups += bot.getNumPickups();
		}
		//System.out.println("");
	}

	public void setBehaviour(Behaviour behaviour) {
		this.behaviour = behaviour;
	}

	public void setArchive(Archive archive) {
		this.archive = archive;
	}

	public void setSchemaConfigNumber(int schemaConfigNumber) {
		this.schemaConfigNumber = schemaConfigNumber;
	}

	public void setConstructionTask(ConstructionTask constructionTask) {
		this.constructionTask = constructionTask;
		setConstructionZone(this.constructionTask.getConstructionZone());
	}

	private void setConstructionZone(ConstructionZone constructionZone) {
		this.constructionZone = constructionZone;
	}

	public void setNumIterations(int value) {
		System.out.println("FitnessMonitor: NUM ITERATIONS HAS BEEN CHANGED");
		numIterations = value;
	}

	//method to get the resources and their respective locations at the end of the simulation
	public void setPlacedResources(CopyOnWriteArrayList<ResourceObject> placedResources) {
		for(ResourceObject rO : placedResources) {
			this.placedResources.add(rO);
		}
	}

	//method to add up all the weighted fitness criteria into a final value
	//combine the distance travelled with the number of pickups + novelty score etc
	public double getOverallFitness() {
		double overallFitness = 0;

		//overallFitness += getTotalTravelDistance() * explorationWeighting; //not dividing by the number of robots (so total distance is per controller / runs)
		//overallFitness += numPickups * pickupWeight;
		overallFitness += calculateConstructionFitness() * constructionWeighting;
		//overallFitness += getSchemaFitness() * schemaWeighting;
		//overallFitness += calculateNoveltyFitness() * noveltyWeighting;
		//overallFitness += calculateAvgResourceDistance() * resourceDistanceWeighting;

		System.out.println("FitnessMonitor: construction fitness = " + overallFitness);

		return overallFitness;
	}

	//method to calculate the distance between 2 points
	//calculates the distance between a new point and its corresponding previous origin point stored in the history list
	public void incrementDistanceTravelled(Vec2 origin, Vec2 destination) {

		totalDistanceTravelled += calculateDistance(origin, destination);
	}

	//method to calculate the avg distance between resources at the end of the simulation
	private double calculateAvgResourceDistance() {

		double avgDistance = 0;

		for(int k = 0; k < placedResources.size()-1; k++) {
			Vec2 currentResourceLocation = placedResources.get(k).getBody().getPosition();
			for(int j = k+1; j < placedResources.size(); j++) {
				Vec2 destResourceLocation = placedResources.get(j).getBody().getPosition();
				avgDistance += calculateDistance(currentResourceLocation, destResourceLocation);
			}
		}

		avgDistance = avgDistance / placedResources.size();
		return avgDistance;

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

	//method to calculate a reward based on how many blocks are connected
	//adding different weights for bigger blocks
	private double calculateConstructionFitness() {
		double constructionFitness = constructionZone.getACount() * A_Weighting +
									 constructionZone.getBCount() * B_Weighting +
									 constructionZone.getCCount() * C_Weighting;

		return constructionFitness;
	}

	//method to check how many of the connected sides match
	//the predefined schemas
	//gets 1 point for each correctly connected resource
	private double getSchemaFitness() {
		double sFitness = 0;
		sFitness = constructionTask.checkSchema(schemaConfigNumber);
		return sFitness;
	} 

	private double calculateNoveltyFitness() {
		double novelFitness = 0;
		System.out.println("FitnessMonitor: number of connected resources = " + constructionZone.getNumberOfConnectedResources());
		Behaviour behaviour = new Behaviour(this.constructionZone);
		novelFitness = this.archive.checkNovelty(behaviour);
		if(novelFitness >= noveltyThreshHold) {
			this.archive.addToArchive(behaviour);
			System.out.println("FitnessMonitor: A NEW NOVEL BEHAVIOUR FOUND");
		}
		return novelFitness;
	}

	//method to take the total displacement of each robot
	//after each simulation step and average it over the number of generations
	//checking the total summed distance covered by a team of robots per iteration step 
	private double getTotalTravelDistance() {
		//System.out.println("FitnessMonitor: number of iterations = " + numIterations);
		return totalDistanceTravelled;
	}

	//method to reset the local variables between
	//different simulation runs
	public void reset() {

		numPickups = 0;
		//totalDistanceTravelled = 0;
		numIterations = 0;

		constructionZone = null;
		constructionTask = null;

		schemaConfigNumber = -1;

		placedResources.clear();

		A_Weighting = 1;
		B_Weighting = 1;
		C_Weighting = 1;
	}
}