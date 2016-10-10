package za.redbridge.simulator;

/*
a class that will manage all the necessary calculations for the objective fitness of an individual
*/

public class ObjectiveFitness {

	/**
	remember to add the code to normalise these values before being sent back to the simulation
	*/

	private AggregateBehaviour aggregateBehaviour;

	private float robToResDistWeight;
	private float numPickupWeight;
	private float resToResDistWeight;
	private float connectedResourcesWeight;
	private float correctSchemaWeight;
	private float avgResToCZoneWeight;

	//the weights that will reward the robots for connecting more complex resources
	private float connectionAWeight;
	private float connectionBWeight;
	private float connectionCWeight;

	public ObjectiveFitness(AggregateBehaviour aggregateBehaviour) {

		this.aggregateBehaviour = aggregateBehaviour;

		robToResDistWeight = 1;
		resToResDistWeight = 1;
		numPickupWeight = 1;
		connectedResourcesWeight = 1;
		correctSchemaWeight = 1;
		avgResToCZoneWeight = 1;

		connectionAWeight = 1;
		connectionBWeight = 1;
		connectionCWeight = 1;

	}

	//method to alter the values for the various weightings
	public void setUpWeights() {

	}

	/*
Method to evaluate the overall objective fitness of a simulation run:
    A - average distance between robot and closest resource to it
    B - average number of times that robots connected to resources (done)
    C - average distance between resources (done)
    D - the number of adjacent resources (number of resources that have been connected) getConstructionFitness()
    E - the number of adjacent resources that are in the correct schema (check which of the connected resources are in the correct schema) checkSchemaFitness()
    F - average distance between resources and the construction starting area (calculate the avg distance between )
*/

    /**
    this is the code to normalise the values for the fitness function
    */

    /*
    aveRobotDistance = (maxDistance - getAveRobotDistance())/maxDistance;
    avePickupCount = getAveRobotPickups()/schema.getTotalRobotsRequired(schemaNumber);
    aveResDistance = (maxDistance - getAveResourceDistance())/maxDistance;
    numAdjacentResources = (double)getNumAdjacentResources()/resources.size();
    numCorrectlyConnected = (double)constructionZone.getNumCorrectlyConnected(schema, schemaNumber)/constructionZone.getNumberOfConnectedResources();
    if (getAveDistanceFromCZ() < 0) {
        // System.out.println(aveRobotDistance + " " + avePickupCount + " " + aveResDistance + " " + numAdjacentResources);
        return w[0]*aveRobotDistance + w[1]*avePickupCount + w[2]*aveResDistance + w[3]*numAdjacentResources;
    }
    else {
        aveResDistanceFromCZ = 1/(1 + getAveDistanceFromCZ());
        // System.out.println(aveRobotDistance + " " + avePickupCount + " " + aveResDistance + " " + numAdjacentResources + " " + numCorrectlyConnected + " " + aveResDistanceFromCZ);
        // double constructionZoneFitness = constructionZone.getFitnessStats().getTeamFitness();
        // constructionFitness += constructionZoneFitness;

        return w[0]*aveRobotDistance + w[1]*avePickupCount + w[2]*aveResDistance + w[3]*numAdjacentResources + w[4]*numCorrectlyConnected + w[5]*aveResDistanceFromCZ;
    */

    /**
    remember to check all the calculations for the following methods
    make sure that the correct resources are being counted and the right averages are being calculated
    */

	public double calculate() {
		double finalFitness = 0;

		finalFitness += getAvgRobToResDist() * robToResDistWeight;
		finalFitness += getAvgPickupCount() * numPickupWeight;
		finalFitness += getAvgResToResDist() * resToResDistWeight;
		finalFitness += calcNumConnectedResources() * connectedResourcesWeight;
		finalFitness += getSchemaScore() * correctSchemaWeight;
		finalFitness += getAvgResToConstZoneDist() * avgResToCZoneWeight;

		finalFitness = finalFitness / aggregateBehaviour.getNumRuns(); //averaging the fitness so that it works out to average fitness for an individual per simulation run

		return finalFitness;
	}

	private double getAvgRobToResDist() {
		return aggregateBehaviour.getRobToResDist();
	}

	private double getAvgPickupCount() {
		return aggregateBehaviour.getTotalPickups();
	}

	//method to calculate the average distance between robots and the resource nearest to them at the end of the simulation
	private double getAvgResToResDist() {
		return aggregateBehaviour.getResToResDist();
	}

	//method to check how many resources are connected
	private double calcNumConnectedResources() {

		double connectionScore = 0;

		connectionScore += aggregateBehaviour.getTotalAConnected() * connectionAWeight;
		connectionScore += aggregateBehaviour.getTotalBConnected() * connectionBWeight;
		connectionScore += aggregateBehaviour.getTotalCConnected() * connectionCWeight;

		return connectionScore;

	}

	private double getSchemaScore() {
		return aggregateBehaviour.getTotalSchemaFitness();
	}

	private double getAvgResToConstZoneDist() {
		double score = 0;
		return score;
	}
	
}