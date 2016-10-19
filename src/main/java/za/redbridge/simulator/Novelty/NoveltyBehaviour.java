package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.ConstructionZone;
import org.jbox2d.common.Vec2;

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

	//arrays to be used to check the configurations of the blocks in the construction zone
	private String [][] AConnections;
	private String [][] BConnections;
	private String [][] CConnections;

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

		int [] czTypeCount = this.constructionZone.getResourceTypeCount();
		AConnections = new String [czTypeCount[0]][4];
		BConnections = new String [czTypeCount[1]][4];
		CConnections = new String [czTypeCount[2]][4];

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

	private void populateConnections() {
		int APos = 0;
		int BPos = 0;
		int CPos = 0;
		for (ResourceObject r : constructionZone.getConnectedResources()) {
			if (r.getType().equals("A")) {
				String [] sides = r.getAdjacentResources();
				for (int i = 0; i < sides.length; i++) {
					AConnections[APos][i] = sides[i];
				}
				APos++;
			}
			else if (r.getType().equals("B")) {
				String [] sides = r.getAdjacentResources();
				for (int i = 0; i < sides.length; i++) {
					BConnections[BPos][i] = sides[i];
				}
				BPos++;
			}
			else if (r.getType().equals("C")) {
				String [] sides = r.getAdjacentResources();
				for (int i = 0; i < sides.length; i++) {
					CConnections[CPos][i] = sides[i];
				}
				CPos++;
			}
		}

		System.out.println(Arrays.deepToString(AConnections));
		System.out.println(Arrays.deepToString(BConnections));
		System.out.println(Arrays.deepToString(CConnections));

		// for (int i = 0; i < AConnections.length; i++) {
		// 	AConnections[i][0] = "A";
		// 	String [] sides = constructionZone
		// 	for (int j = 0; j < AConnections[0].length; j++) {
		// 		AConnections[i][j]
		// 	}
		// }
	}

	/*
	method to calculate an average trajectory that represents the combined overall
	trajectories of all the robots in the team
	so that there is one average trajectory per controller that can be compared to 
	the trajectories of other controllers in the generation

	iterate over the trajectories of each robot in a team
	for each trajectory, sum the x and y coordinates at each respective position in the trajectory

	eg. firstPositionAvgTrajectory = firstPositionFirstRobot + firstPositionSecondRobot + firstPositionThirdRobot + firstPositionFourthRobot etc / numRobots
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

	public String[][] getAConnections() {
		return AConnections;
	}

	public String[][] getBConnections() {
		return BConnections;
	}

	public String[][] getCConnections() {
		return CConnections;
	}
	
}