package za.redbridge.simulator;

import za.redbridge.simulator.Behaviour;
import java.util.*;

/*
a class that stores and manages the behaviour of an individual over a number of runs
*/

public class AggregateBehaviour {

	private int numRuns; //the number of times that the individual controller was tested in the simulator
	private int numBehaviours; //a variable to count the number of behaviours that have been added to the collection
	private int totalPickups; //variable to store the total number of times robots connected to resources over all the simulation runs
	private double sumResToResDist; //variable to store the sum of the average distance between resources fpr each simulation rin
	private double sumRobToResDist; //average distance between a robot and its nearest resource
	private double sumConnectedResources; //variable to store the number of connected resources summed for all simulation runs
	private double sumResToCZoneDist; //variable to store the sum of average distance between construction zone and resource for each simulation run
	private double sumAdjacentScore; //variable to store the sum adjacent score for a behaviour 
	private double sumSchemaScore; // variable to store the sum correct schema score for a behaviour

	//variables to count the different types of connected blocks
	private int totalAConnected;
	private int totalBConnected;
	private int totalCConnected;

	private ArrayList<Behaviour> behaviourCollection; //a list to store the behaviour results from each of the runs

	private int schemaConfigNum;
	private double totalSchemaFitness; //variable to store the sum of the schema fitness of each behaviour (for each simulation run)

	public AggregateBehaviour(int numRuns, int schemaConfigNum) {
		
		this.numRuns = numRuns;
		this.schemaConfigNum = schemaConfigNum;
		behaviourCollection = new ArrayList<Behaviour>();

		totalPickups = 0;
		sumResToResDist = 0;
		sumRobToResDist = 0;
		sumConnectedResources = 0;
		sumResToCZoneDist = 0;
		sumAdjacentScore = 0;
		sumSchemaScore = 0;

		totalAConnected = 0;
		totalBConnected = 0;
		totalCConnected = 0;

		totalSchemaFitness = 0;
	}

	/**
	make sure that all the values are being calcualted correctly 
	check that all the averagers bening calculated in the right places using the right values
	*/

	/*
	method to add the resultant behaviour from the end of the sim run to the 
	collection of previous behaviours
	*/
	public boolean addSimBehaviour(Behaviour behaviour) {

		boolean added = false;
		if( this.behaviourCollection.add(behaviour) ) { //append the behaviour to the end of the list

			added = true;
			numBehaviours++;

			incPickups(behaviour.getNumPickups());
			incResToResDist(behaviour.getAvgResToResDist());
			incRobToResDist(behaviour.getAvgRobToResDist());
			countConnectedBlocks(behaviour);
			//calculateSchemaFitness(behaviour);
			incAvgResToCZoneDist(behaviour.getAvgResToCZoneDist());
			incAdjacentScore(behaviour.getAdjacentScore());
			incSchemaScore(behaviour.getSchemaScore());

		}
		return added;
	}

	//method to calculate the sum of all the distances between robots and their respective nearest resource for the behaviour currently being added
	private void incRobToResDist(double robToResDist) {
		sumRobToResDist += robToResDist;
	}

	//method to add avg disdt between resources to total
	private void incResToResDist(double resToResDist) {
		sumResToResDist += resToResDist;
	}

	//method to add avg pickup per robot total count
	private void incPickups(int numPickups) {
		totalPickups += numPickups;
	} 

	//method to count the total number of each type of connected block
	private void countConnectedBlocks(Behaviour behaviour) {

		totalAConnected += behaviour.getConnectedA();
		totalBConnected += behaviour.getConnectedB();
		totalCConnected += behaviour.getConnectedC();
	}

	// //method to sum the schema fitness for each behaviour's construction task
	// private void calculateSchemaFitness(Behaviour behaviour) {
	// 	totalSchemaFitness += behaviour.getConstructionTask().checkSchema(schemaConfigNum);
	// }

	private void incAvgResToCZoneDist(double avgResToCZoneDist) {
		sumResToCZoneDist += avgResToCZoneDist;
	}

	private void incAdjacentScore(double score) {
		sumAdjacentScore += score;
	}

	private void incSchemaScore(double score) {
		sumSchemaScore += score;
	}

	// public double getTotalSchemaFitness() {
	// 	return totalSchemaFitness;
	// }

	public int getTotalAConnected() {
		return totalAConnected;
	}

	public int getTotalBConnected() {
		return totalBConnected;
	}

	public int getTotalCConnected() {
		return totalCConnected;
	}

	public double getTotalPickups() {
		return totalPickups;
	}

	public double getSumResToCZDist() {
		return sumResToCZoneDist;
	}

	/*
	a method to get a previously stored behaviour
	input: simulation run that resulted in the specified behaviour
	*/
	public Behaviour getSimResult(int index) {
		return behaviourCollection.get(index);
	}

	public int getNumRuns() {
		return numRuns;
	}

	public double getRobToResDist() {
		return this.sumRobToResDist;
	}

	public double getResToResDist() {
		return this.sumResToResDist;
	}

	public double getAdjacentScore() {
		return sumAdjacentScore;
	}

	public double getSchemaScore() {
		return sumSchemaScore;
	}

		//just a random mnethod to check this incase at some point
	public boolean correlateCount() {
		if(numBehaviours == behaviourCollection.size() ) {
			return true;
		}
		return false;
	}
	
}