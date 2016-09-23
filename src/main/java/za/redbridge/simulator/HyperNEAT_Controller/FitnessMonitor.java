package za.redbridge.simulator;

import org.jbox2d.common.Vec2;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
import java.util.ArrayList;
import za.redbridge.simulator.config.SimConfig;

public class FitnessMonitor {

	private double totalDistanceTravelled; //sum of total distance travelled by robot team in simulation
	private double numPickups; //total number of times a robot has picked up a block in the simulation
	private double numGenerations;
	//private final SimConfig config;

	//the weights that are going to be used for the final fitness calculation
	private float explorationWeighting;
	private float pickupWeight;

	public FitnessMonitor() {
		this.totalDistanceTravelled = 0;
		this.numPickups = 0;
		this.numGenerations = 0;
		//this.config = config;

		pickupWeight = 1;
		explorationWeighting = 1;
		
	}

	public void savePickupCounts(ArrayList<RobotObject> currentBots) {
		for(RobotObject bot : currentBots) {
			numPickups += bot.getNumPickups();
		}
	}

	public void setNumGenerations(int value) {
		numGenerations = value;
	}

	public void calculateDistance(ArrayList<RobotObject> before, ArrayList<RobotObject> after) {

		for(int k = 0; k < before.size(); k++) {
			Vec2 beforePos = before.get(k).getBody().getPosition();
			Vec2 afterPos = after.get(k).getBody().getPosition();
			totalDistanceTravelled += beforePos.sub(afterPos.negate()).length();
			//System.out.println("FitnessMonitor: the distance = " + total);
			//totalDistanceTravelled += before.get(k).getBody().getPosition().add(after.get(k).getBody().getPosition().negate());
		}

	}

	//method to add up all the weighted fitness criteria into a final value
	//combine the distance travelled with the number of pickups + novelty score etc
	public double getOverallFitness() {
		double overallFitness = 0;
		overallFitness += getTotalTravelDistance() * explorationWeighting; //not dividing by the number of robots (so total distance is per controller / runs)
		overallFitness += numPickups * pickupWeight;

		return overallFitness;
	}

	public double getTotalTravelDistance() {
		return totalDistanceTravelled/numGenerations;
	}

	//calculate the fitness of a genotype based on how much
	//the individual robots moved
	//sum up total displacement of robot team
	public double calcMovementFitness() {

		double distanceMoved = 0;

		return distanceMoved;
	}

	//calculate the fitness of a genotype based on how many
	//blocks were picked up during the simulation by members of the robot team
	public double calcPickupFitness() {

		double pickup = 0;

		return pickup;
	}

	public void reset() {
		//set all the variables back to default values
		//currently being called in the Start method of the Simulation class
		//NEED TO MAKE SURE OF WHERE TO RESET THIS CLASS
		//INSIDE WHICH LOOP ETC

		numPickups = 0;
		totalDistanceTravelled = 0;
		numGenerations = 0;
	}
}