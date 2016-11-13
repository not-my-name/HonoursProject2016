package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.ConstructionZone;
import org.jbox2d.common.Vec2;
import sim.field.grid.ObjectGrid2D;

/*
this is the class that will be created for each time that an individual gets tested
one of these classes per each simulation iteration

this class needs to store and manage all the necessary information and characteristics
that are needed to calculate the novelty fitness of a solution
*/

public class NoveltyBehaviour {

	//arrays to be used to check the configurations of the blocks in the construction zone
	private ArrayList<String[]> AConnectionsList;
	private ArrayList<String[]> BConnectionsList;
	private ArrayList<String[]> CConnectionsList;

	private AggregateBehaviour aggregateBehaviour;
	private double objectiveScore; //used for the hybrid calculation

	//stores the final construction at the end of the simulation
	//private ConstructionZone[] constructionZones;
	private ArrayList<ConstructionZone> constructionZones;
	private ConstructionTask constructionTask;
	private ObjectGrid2D discreteConstructionZone;

	private ArrayList<ResourceObject> connectionOrder;

	//number of samples in a trajectory
	private int numRobots;
	// private int numResources;

	private double populationNoveltyScore; //var to store the novelty of behaviour as compared to the rest of the individuals in the generation
	private double archiveNoveltyScore; //var to store the novelty of behaviour compared to behaviours stored in the archive
	private double simulationNoveltyScore; //var to store the novelty of the behaviour compared to the behaviours produced in the other simulation runs

	private ArrayList<Double> simulationNeighbourhood;
	private ArrayList<Double> populationNeighbourhood;
	private ArrayList<Double> archiveNeighbourhood;

	private ArrayList<Vec2> robotEndPositions; //arrayList to keep track of the ending points of the robots at the end of each simulation

	private boolean archived;

	private final int numNearestNeighbours = 10;

	private int envHeight = 20;
	private int envWidth = 20;

	//public NoveltyBehaviour(ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, ConstructionTask constructionTask) {
	public NoveltyBehaviour(ArrayList<RobotObject> currentRobots, ConstructionTask constructionTask) {

		this.numRobots = currentRobots.size();
		// this.numResources = currentResources.size();

		this.constructionTask = constructionTask;
		this.connectionOrder = this.constructionTask.getConstructionOrder();

		/**
		check for if this gets copied over properly
		should it be done in a loop for every individual

		change this to only work with 3 construction zones
		*/
		this.constructionZones = this.constructionTask.getConstructionZones();
		this.discreteConstructionZone = this.constructionTask.getDiscreteGrid().getGrid();

		robotEndPositions = new ArrayList<Vec2>();
		for(RobotObject robObj : currentRobots) { //creating the collection of the robot team's end positions
			robotEndPositions.add(robObj.getBody().getPosition());
		}

		//create the cumulative representation of the construction zones
		populateConnections();
		/**
		change this to work with the variable environment dimensions
		*/

		populationNoveltyScore = 0;
		archiveNoveltyScore = 0;
		simulationNoveltyScore = 0;

		archived = false;

		simulationNeighbourhood = new ArrayList<Double>();
		populationNeighbourhood = new ArrayList<Double>();
		archiveNeighbourhood = new ArrayList<Double>();

	}

	//rewriting this method to sue lists instead of arrays for the connections
	//if using arrays, creating the arrays to a max dimension and then need to keep track
	//how many of the actual positions are filled and not empty elements
	//dont need to check that with lists, just use fancy for loop over elements
	private void populateConnections() {

		/**
		make these lists global variables
		*/
		AConnectionsList = new ArrayList<String[]>();
		BConnectionsList = new ArrayList<String[]>();
		CConnectionsList = new ArrayList<String[]>();

		int numConstructionZones = getNumConstructionZones();

		//for(int k = 0; k < numConstructionZones; k++) { //iterate over all the existing construction zones
		for(ConstructionZone cZone : constructionZones) {

			//for(ResourceObject resObj : constructionZones[k].getConnectedResources()) { //iterate over the resources connected to the current construction zone
			for(ResourceObject resObj : cZone.getConnectedResources()) {

				/**
				make sure that this is moved over by value
				should i create a new array and then add that to the list? or just add the list that gets returned
				*/
				String[] sideConnections = resObj.getAdjacentResources();

				if(resObj.getType().equals("A")) {
					AConnectionsList.add(sideConnections);
				}
				else if(resObj.getType().equals("B")) {
					BConnectionsList.add(sideConnections);
				}
				else if(resObj.getType().equals("C")) {
					CConnectionsList.add(sideConnections);
				}
			}
		}
	}

	public void setObjectiveScore(double objectiveScore) {
		this.objectiveScore = objectiveScore;
	}

	public double getObjectiveScore() {
		return objectiveScore;
	}

	public void setAggregateBehaviour(AggregateBehaviour aggregateBehaviour) {
		this.aggregateBehaviour = aggregateBehaviour;
	}

	public AggregateBehaviour getAggregateBehaviour() {
		return aggregateBehaviour;
	}

	//method to calculate this behaviour's novelty compared to the other results obtained from the simulation
	//find the average distance between its neighbours
	public double calculateSimulationNovelty() {

		simulationNoveltyScore = 0;

		int neighbourCounter = 0;
		for(double d : simulationNeighbourhood) { //iterate over the distances to its neighbours

			if(neighbourCounter == numNearestNeighbours) {
				break;
			}
			neighbourCounter++;
			simulationNoveltyScore += d;
		}

		simulationNoveltyScore = simulationNoveltyScore / numNearestNeighbours;
		return simulationNoveltyScore;

	}

	//method to calculate the individuals novelty based on its average distance to k-nearest neighbours
	public double calculatePopulationNovelty() {

		populationNoveltyScore = 0;

		int neighbourCounter = 0;
		for(double d : populationNeighbourhood) { //iterate over the nearest neighbours in the population

			if(neighbourCounter == numNearestNeighbours) {
				break;
			}

			neighbourCounter++;
			populationNoveltyScore += d;
		}

		populationNoveltyScore = populationNoveltyScore / numNearestNeighbours;

		//System.out.println("NoveltyBehaviour: finished calculating population score = " + populationNoveltyScore);
		return populationNoveltyScore;
	}

	//check if the current Behaviour has been added to the archive
	public boolean isArchived() {
		return archived;
	}

	public int getNumConstructionZones() {
		//return constructionZones.length;
		return constructionZones.size();
	}

	//method to indicate that the current behaviour has been added to the archive
	public void setArchived() {
		archived = true;
	}

	public void addSimulationNeighbour(double newNeighbour) {
		simulationNeighbourhood.add(newNeighbour);
	}

	public void addPopulationNeighbour(double newNeighbour) {
		//System.out.println("NoveltyBehaviour: adding a new novelty neighbour = " + newNeighbour);
		populationNeighbourhood.add(newNeighbour);
	}

	public void addArchiveNeighbour(double newNeighbour) {
		archiveNeighbourhood.add(newNeighbour);
	}

	// public int getNumResources() {
	// 	return this.numResources;
	// }

	public ArrayList<Vec2> getRobotPositions() {
		return this.robotEndPositions;
	}

	public int getNumRobots() {
		return numRobots;
	}

	// public ConstructionZone[] getConstructionZone() {
	// 	return this.constructionZones;
	// }

	public double getSimulationNoveltyScore() {
		return simulationNoveltyScore;
	}

	public double getPopulationNoveltyScore() {
		return populationNoveltyScore;
	}

	public ArrayList<ConstructionZone> getConstructionZone() {
		return constructionZones;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public ObjectGrid2D getDiscreteConstructionZone() {
		return discreteConstructionZone;
	}

	public ArrayList<String[]> getAConnections() {
		return AConnectionsList;
	}

	public ArrayList<String[]> getBConnections() {
		return BConnectionsList;
	}

	public ArrayList<String[]> getCConnections() {
		return CConnectionsList;
	}

}
