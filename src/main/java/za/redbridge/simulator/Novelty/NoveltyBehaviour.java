package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.ConstructionZone;

/*
this is the class that will be created for each time that an individual gets tested
one of these classes per each simulation iteration

this class needs to store and manage all the necessary information and characteristics
that are needed to calculate the novelty fitness of a solution
*/

public class NoveltyBehaviour {

	//array list of linked lists, each linked list is a trajectory (linked list of positions)
	private ArrayList<LinkedList<Vec2>> robotTrajectories;
	private ArrayList<LinkedList<Vec2>> resourceTrajectories;

	private Vec2[] avgRobotTrajectory;
	private Vec2[] avgResourceTrajectory;

	//stores the final construction at the end of the simulation
	private ConstructionZone constructionZone; 
	private ConstructionTask constructionTask;

	private ResourceObject[] connectionOrder;

	//number of samples in a trajectory
	private int numRobotPosSamples; //number of positions that get saved in order to prepresent a trajectory (size of each trajectory array)
	private int numResPosSamples; //same as above but for the resources
	private int numRobots;
	private int numResources;

	public NoveltyBehaviour(ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, ConstructionTask constructionTask) {

		this.numRobots = currentRobots.size();
		this.numResources = currentResources.size();

		robotTrajectories = new ArrayList<LinkedList<Vec2>>(); //num elements = num robots
		resourceTrajectories = new ArrayList<LinkedList<Vec2>>(); //num elements = num resources

		initRobotTrajectories(currentRobots);
		initResourceTrajectories(currentResources);

		//get the number of samples that were saved for each trajectory
		this.numRobotPosSamples = robotTrajectories.get(0).size();
		this.numResPosSamples = resourceTrajectories.get(0).size();

		avgRobotTrajectory = new Vec2[numRobotPosSamples];
		avgResourceTrajectory = new Vec2[numResPosSamples];

		/**
		print out the average trajectories after these methods have been called in order to check that the correct arrays have been modified
		*/
		calcAvgTrajectory(numRobots, numRobotPosSamples, avgRobotTrajectory, robotTrajectories);
		calcAvgTrajectory(numResources, numResPosSamples, avgResourceTrajectory, resourceTrajectories);

		this.constructionTask = constructionTask;
		this.constructionZone = this.constructionTask.getConstructionZone();

	}

	//method to populate the list of robot trajectories
	private void initRobotTrajectories(ArrayList<RobotObject> robots) {

		for(RobotObject r : robots) {
			LinkedList<Vec2> trajectory = r.getTrajectory();
			robotTrajectories.add(trajectory); //each element in the robotTrajectories list will be a LinkedList of Vec2
		}

	}

	//method to populate the list of resource trajectories;
	private void initResourceTrajectories(ArrayList<ResourceObject> resources) {

		for(ResourceObject res : resources) {
			LinkedList<Vec2> trajectory = res.getTrajectory();
			resourceTrajectories.add(trajectory); //each element in the resourceTrajectories list will be a LinkedList of Vec2
		}

	}

	/*
	method to calculate an average trajectory that represents the combined overall
	trajectories of all the robots in the team
	so that there is one average trajectory per controller that can be compared to 
	the trajectories of other controllers in the generation

	iterate over the trajectories of each robot in a team
	for each trajectory, sum the x and y coordinates at each respective position in the trajectory

	eg. firstPositionAvgTrajectory = firstPositionFirstRobot + firstPositionSecondRobot + firstPositionThirdRobot + firstPositionFourthRobot etc \ numRobots
	*/
	private void calcAvgTrajectory(int outerBound, int innerBound, Vec2[] avgTrajectory, ArrayList<LinkedList<Vec2>> originalTrajectories) {

		float[] pathXCoords = new float[innerBound]; //store all the x coords of the positions along a trajectory
		float[] pathYCoords = new float[innerBound]; //store all the y coords of the positions along a trajectory

		for(int k = 0; k < outerBound; k++) { //robotTrajectories.size() should == numRobots
			LinkedList<Vec2> tempTrajectory = originalTrajectories.get(k);

			for(int j = 0; j < innerBound; j++) { //originalTrajectory.size() should == numRobotPosSamples
				Vec2 tempPosition = tempTrajectory.get(j); //get the coords at the current time step

				//summing the x and y values of the positions
				pathXCoords[j] += tempPosition.x;
				pathYCoords[j] += tempPosition.y;
			}
		}

		for(int k = 0; k < innerBound; k++) { //constructing the representative trajectory
			float avgXCoord = pathXCoords[k] / numRobots;
			float avgYCoord = pathYCoords[k] / numRobots;

			avgTrajectory[k] = new Vec2(avgXCoord, avgYCoord); //constructing the average trajectory
		}
	}

	public int getNumResources() {
		return this.numResources;
	}

	public ConstructionZone getConstructionZone() {
		return this.constructionZone;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public Vec2[] getResourceTrajectory() {
		return avgResourceTrajectory;
	}

	public Vec2[] getRobotTrajectory() {
		return avgRobotTrajectory;
	}
	
}