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
	private ConstructionZone[] constructionZones; 
	private ConstructionTask constructionTask;

	private ResourceObject[] connectionOrder;

	//number of samples in a trajectory
	private int numRobotPosSamples; //number of positions that get saved in order to prepresent a trajectory (size of each trajectory array)
	private int numResPosSamples; //same as above but for the resources
	private int numRobots;
	private int numResources;

	private double generationNoveltyScore; //var to store the novelty of behaviour as compared to the rest of the individuals in the generation
	private double archiveNoveltyScore; //var to store the novelty of behaviour compared to behaviours stored in the archive
	private double simulationNoveltyScore; //var to store the novelty of the behaviour compared to the behaviours produced in the other simulation runs

	// private double[] localNoveltyVector; //array to represent a vector of all the respective novelty values, used to calculate distance to other 
	// private double[] archiveNoveltyVector;
	// private double[] simulationNoveltyVector;

	private ArrayList<double> simulationNeighbourhood;
	private ArrayList<double> generationNeighbourhood;
	private ArrayList<double> archiveNeighbourhood;

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

		calcAvgTrajectory(numRobots, numRobotPosSamples, avgRobotTrajectory, robotTrajectories);
		calcAvgTrajectory(numResources, numResPosSamples, avgResourceTrajectory, resourceTrajectories);

		this.constructionTask = constructionTask;
		this.constructionZones = this.constructionTask.getConstructionZones();

		/**
		change this to work with the multiple construction zones
		*/
		int [] czTypeCount = this.constructionZones[0].getResourceTypeCount();
		AConnections = new String [czTypeCount[0]][4];
		BConnections = new String [czTypeCount[1]][4];
		CConnections = new String [czTypeCount[2]][4];

		generationNoveltyScore = 0;
		archiveNoveltyScore = 0;
		simulationNoveltyScore = 0;

		simulationNeighbourhood = new ArrayList<double>();
		generationNeighbourhood = new ArrayList<double>();
		archiveNeighbourhood = new ArrayList<double>();

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

		for (ResourceObject r : constructionZones[0].getConnectedResources()) {

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

	//method to calculate this behaviour's novelty compared to the other results obtained from the simulation
	//find the average distance between its neighbours
	public double calculateSimulationNovelty() {

		simulationNoveltyScore = 0;
		int numNeighbours = 0;

		for(double d : simulationNeighbourhood) { //iterate over the distances to its neighbours

			simulationNoveltyScore += d;
			numNeighbours++;
		}

		simulationNoveltyScore = simulationNoveltyScore / numNeighbours;
		return simulationNoveltyScore;

	}

	public void addSimulationNeighbour(double newNeighbour) {
		simulationNeighbourhood.add(newNeighbour);
	}

	public void addGenerationNeighbour(double newNeighbour) {
		generationNeighbourhood.add(newNeighbour);
	}

	public void addArchiveNeighbour(double newNeighbour) {
		archiveNeighbourhood.add(newNeighbour);
	}

	public int getNumResources() {
		return this.numResources;
	}

	public ConstructionZone[] getConstructionZone() {
		return this.constructionZones;
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