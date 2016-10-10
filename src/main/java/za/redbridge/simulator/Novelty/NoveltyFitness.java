package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.AggregateBehaviour;

/*
a class to monitor and manage all the necessary structures to calculate the novelty fitness
*/

public class NoveltyFitness{

	/**
	Notes from Josh
	Check the TrainEA class that is extended from the Strategy class
	Implement a noveltySearchStrategy class that has its own pre and post iteration methods defined
	Perform the novelty calculations in the postiteration because the Strategy class has access to the Trainer and then you can compare the 
	entire populations to each other

	for comparing the trajectories to each other, compare each robot in a team to the same robot in the other team then sum the differences in an array
	one array element corresponds to the total trajectory difference for a pair of robots 

	sum the values from that array into one double and theen divide by the number of robots in a team (length of the array)

	since each controller is run for several runs, find the most novel of all those runs and use that as the representative behaviour for that controller and use that to compare to
	the other individuals in the population
	the most novel individual in the population then gets compared to the archive and if its relatively novel compared to the archive then it gets added to the archive
	**/

	//order of construction zone connections (order resources are added)
	//compare the construction zone itself
	//resource's trajectory (save position every 5 time steps) regardless of whether or not its been picked up
	//trajectory of the robot  (save position every 5 time steps) regardles of whether it picked up a robot

	private float localNoveltyWeight;
	private float archiveNoveltyWeight;

	private float robotTrajectoryWeight;
	private float resourceTrajectoryWeight;
	private float constructionOrderWeight;
	private float constructionZoneWeight;

	private Archive archive; //the archive of novel behaviours

	private NoveltyBehaviour[] currentGeneration;
	private int numBehaviours; //keep track of how many behaviours there are in a generation

	private double[] finalFitnessArray; //keep track of the fitness of each of the individuals in the current generation
	//keeps track of the respective scores of the behaviours in the current generation

	private double[][] robotTrajectoryDifferences; //an array to store the position differences at each sample point
	private double[][] resourceTrajectoryDifferences; //an array to store the position differences for resources at each sample point their respective trajectories

	private int numRobotSamples; //number of position samples that make up the robots trajectory
	private int numResSamples; //number of position samples that make up the trajectory of a resource
	private int numResources; //the number of resources that were used in a simulation

	/**
	check that all these calculations are done the same way and correct
	*/

	public NoveltyFitness(NoveltyBehaviour[] currentGeneration, Archive archive) {

		localNoveltyWeight = 1;
		archiveNoveltyWeight = 1;

		robotTrajectoryWeight = 1;
		resourceTrajectoryWeight = 1;
		constructionOrderWeight = 1;
		constructionZoneWeight = 1;

		numBehaviours = currentGeneration.length;
		this.currentGeneration = new NoveltyBehaviour[numBehaviours];
		for(int k = 0; k < numBehaviours; k++) { //copying over the elements in the array to a local array
			this.currentGeneration[k] = currentGeneration[k];
		}

		this.numRobotSamples = this.currentGeneration[0].getRobotTrajectory().length;
		robotTrajectoryDifferences = new double[numBehaviours][numRobotSamples];

		this.numResources = this.currentGeneration[0].getNumResources(); //all of the simulations within a generation were run using the same number of resources
		this.numResSamples = this.currentGeneration[0].getResourceTrajectory().length; //the number of times the position was sampled to build the trajectory
		resourceTrajectoryDifferences = new double[numResources][numResSamples]; 

		finalFitnessArray = new double[numBehaviours]; //array to store the final fitnesses of each of the individuals in the current generation

		calculateRobTrajDist();
		calculateResTrajDist();

		// this.currentGeneration = new ArrayList<NoveltyBehaviour>();
		// //populating the array list to have a local copy of the current generation of novel behaviours
		// for(NoveltyBehaviour beh : currentGeneration) { 
		// 	this.currentGeneration.add(beh);
		// }

		// numBehaviours = this.currentGeneration.size();

		this.archive = archive;
	}

	/*
	method to calculate the overall novelty of the individuals behaviour
	*/
	public double[] calculateNovelty() {

		double fitness = 0;

		fitness += calcLocalFitness(currentGeneration) * localNoveltyWeight;
		fitness += calcArchiveFitness(archive) * archiveNoveltyWeight;

		return fitness;
	}

	/*
	method to calculate the fitness of an individual w.r.t the individuals in the 
	rest of the generation
	compare to each network in the population (current generation)
	*/
	private double calcLocalFitness(ArrayList<NoveltyBehaviour> currentGen) {

		double localNovelty = 0;

		localNovelty += this.compareRobotTrajectory(currentGen) * robotTrajectoryWeight;
		localNovelty += this.compareResourceTrajectory(currentGen) * resourceTrajectoryWeight;
		localNovelty += this.compareConstructionOrder(currentGen)  * constructionOrderWeight;
		localNovelty += this.compareConstructionZone(currentGen) * constructionZoneWeight;

		return localNovelty;
	}

	/*
	method to calculate the fitness of an individual by comparing it 
	to the behaviours in the archive
	*/
	private double calcArchiveFitness(ArrayList<NoveltyBehaviour> archiveCollection) { //might need to send the actual archive

		double archiveNovelty = 0;

		//store these values in an array "NoveltyArray" so that you can calculate the "distance" between
		archiveNovelty += this.compareRobotTrajectory(archiveCollection) * robotTrajectoryWeight;
		archiveNovelty += this.compareResourceTrajectory(archiveCollection) * resourceTrajectoryWeight;
		archiveNovelty += this.compareConstructionOrder(archiveCollection) * constructionOrderWeight;
		archiveNovelty += this.compareConstructionZone(archiveCollection) * constructionZoneWeight;

		return archiveNovelty;
	}

	/*
	method to calculate all the respective trajectory differences for the robots
	between all the individuals in the current generation
	*/
	private void calculateRobTrajDist() {

		for(int k = 0; k < numBehaviours-1; k++) { //iterating over all the individuals in the current generation
			Vec2[] originalTrajectory = currentGeneration[k].getRobotTrajectory();

			for(int j = k+1; j < numBehaviours; j++) { //iterating over the remaining individuals in the generation in order to be compare to each other
				Vec2[] tempTrajectory = currentGeneration[j].getRobotTrajectory();

				for(int l = 0; l < this.numRobotSamples; l++) { //iterating over all the points in the respective trajectories and calculating the distance between them
					
					Vec2 origin = originalTrajectory[l];
					Vec2 destination = tempTrajectory[l];
					float dist = calculateDistance(origin, destination);

					//update the distances between for each behaviour to prevent uneccessary looping
					robotTrajectoryDifferences[k][l] += dist; //updating the original trajectory
					robotTrajectoryDifferences[j][l] += dist; //updating the temp trajectory value
				}
			}
		}

	}

	/*
	method to calculate all the respective trajectory differences
	between all the resources in each simulation
	*/
	private void calculateResTrajDist() {

		for(int k = 0; k < numResources-1; k++) { //iterate over each individual resource that gets used in the simulations
			Vec2[] originalTrajectory = currentGeneration[k].getResourceTrajectory();

			for(int j = k+1; j < numResources; j++) { //iterate over the remaining resources
				Vec2[] tempTrajectory = currentGeneration[j].getResourceTrajectory();

				for(int l = 0; l < numResSamples; l++) { //iterating over each position in the trajectories of the resources

					Vec2 origin = originalTrajectory[l];
					Vec2 destination = tempTrajectory[l];
					float dist = calculateDistance(origin, destination);

					resourceTrajectoryDifferences[k][l] += dist;
					resourceTrajectoryDifferences[j][l] += dist;

				}
			}
		}
	}

	/*
	method to calculate the difference between the trajectory of the robots of the current 
	controllers behaviour

	calculate the distance between the corresponding points along the trajectories of the robots
	sum up the corresponding distances
	distance[0] = sum of distances between pos0 of original and pos0 of the collection

	should i sum the total differences into one double variable
	
	should i sum the total distance difference between each behaviour
	so calc distance between each point in the trajectories
	sum those distances
	repeat and store for each behaviour
	will return an array that has total distance difference between the currentBehaviour and the behaviour it is being compared to
	*/
	private double compareRobotTrajectory(NoveltyBehaviour currentBehaviour, ArrayList<NoveltyBehaviour> theCollection) {

		/**
		for each novelty behaviour, there are several trajectories (as many as robots)
			-need to average out the trajectory of all the robots to have one aggregate trajectory that then gets used for calculation
			-compare each trajectory of each robot of each novelty behaviour?
		*/

		double trajectoryDiff = 0;

		Vec2[] currentTrajectory = currentBehaviour.getRobotTrajectory();
		int numSamples = currentTrajectory.length;
		double[] posDifferences = new double[numSamples];

		for(int k = 0; k < numSamples; k++) { //resetting the values
			posDifferences[k] = 0;
		}

		for(NoveltyBehaviour novBeh : theCollection) { //iterate over the behaviours in the current generation

			if(novBeh != currentBehaviour) { //check so that it doesnt compare the behaviour with itself

				Vec2[] tempTrajectory = novBeh.getRobotTrajectory(); //gets the aggregate trajectory that represents the average movement of a team of robots

				if(currentTrajectory.size() != tempTrajectory.size()) { 
					System.out.println("NoveltyFitness: the trajectory lists are not the same size");
				}

				//calculating the distance between the respective points of the robot's trajectory
				for(int k = 0; k < numSamples; k++) { //iterate over the positions along the trajectories of the robots being compared
					Vec2 origin = currentTrajectory[k];
					Vec2 destination = tempTrajectory[k];
					float distance = calculateDistance(origin, destination); //distance between the corresponding points
					posDifferences[k] += distance;
				}
				
			}
		}

		for(int k = 0; k < posDifferences.length; k++) {
			double temp = posDifferences[k];
			posDifferences[k] = temp / theCollection.size(); //divide by the number of individuals in the generation -> get the average distance between each trajectory sample point
			trajectoryDiff += posDifferences[k]; //finding the total average distance between the current behaviour and the average behaviour
		}

		return trajectoryDiff;
	}

	/*
	method to compare the difference between the trajectories of
	the resources

	same questions as for the above method
	*/
	private double compareResourceTrajectory(NoveltyBehaviour currentBehaviour, ArrayList<NoveltyBehaviour> theCollection) {

		/**
		this seems pretty inefficient to calculate the distance between behaviour
		*/

		double trajectoryDiff = 0;

		Vec2[] currentTrajectory = currentBehaviour.getResourceTrajectory(); //getting the aggregate resource trajecory of the current behaviour/controller
		int numSamples = currentTrajectory.length; //getting the number of positions in the trajectory
		double[] posDifferences = new double[numSamples]; //creating an array to store the individual respective distances between each sample point in the trajectory

		for(int k = 0; k < numSamples; k++) {
			posDifferences[k] = 0;
		}

		for(NoveltyBehaviour novBehaviour : theCollection) {

			/** 
			check that this will work and that you dont need some sort of ID for each behaviour
			 */
			if(novBehaviour != currentBehaviour) {//check that you dont compare the same object to itself

				Vec2[] tempTrajectory = novBehaviour.getResourceTrajectory(); //getting the trajectory that needs to be compared to 

				//again, just a check in case something went wrong in one of the other classes
				if(currentTrajectory.length != tempTrajectory.length) {
					System.out.println("NoveltyFitness compareResourceTrajectory: this message should never have appeared");
				}

				for(int k = 0; k < numSamples; k++) {
					Vec2 origin = currentTrajectory[k];
					Vec2 destination = tempTrajectory[k];
					float dist = calculateDistance(origin, destination); //calculating the physical distance between these 2 controllers at the same point in time
					posDifferences[k] += dist; //summing the distances between each controller and current controller and this point in time
				}

			}
		}

		/**
		make sure that there shouldnt be a -1 for the numBehaviours to exclude the current behaviour being compared
		*/
		int numBehaviours = theCollection.size(); //number which to divide the posDifferences in order to get an average for each behaviour

		for(int k = 0; k < numSamples; k++) {
			double temp = posDifferences[k];
		}

		return trajectoryDiff;
	}

	/*
	method to calculate the difference between the order in which resources were
	connected to the construction zone
	*/
	private double compareConstructionOrder() {
		double constructionDiff = 0;

		return constructionDiff;
	}

	/*
	method to calculate the difference between the structures that
	were constructed at the end of the simulation
	*/
	private double compareConstructionZone() {
		double cZoneDiff = 0;

		return cZoneDiff;
	}

	/**
	the float being returned gets cast to a double in the compareRobotTrajectories method
	check that this does not cause some sort of error
	*/

	/*
	method to calculate the distance between 2 points
	*/
	private float calculateDistance(Vec2 origin, Vec2 destination) {

		double originX = origin.x;
		double originY = origin.y;

		double destinationX = destination.x;
		double destinationY = destination.y;

		float distance = (float) Math.sqrt(
						 Math.pow(destinationX - originX, 2) + 
						 Math.pow(destinationY - originY, 2));

		return distance;

	}
	
}