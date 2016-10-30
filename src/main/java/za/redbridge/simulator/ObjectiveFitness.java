package za.redbridge.simulator;

import za.redbridge.simulator.object.ResourceObject;
import java.util.*;

/*
a class that will manage all the necessary calculations for the objective fitness of an individual
*/

public class ObjectiveFitness {

	//private AggregateBehaviour aggregateBehaviour;

	private float robToResDistWeight;
	private float numPickupWeight;
	private float resToResDistWeight;
	private float connectedResourcesWeight;
	private float correctSchemaWeight;
	private float avgResToCZoneWeight;
	private float adjacentResourcesWeight;

	//the weights that will reward the robots for connecting more complex resources
	private float connectionAWeight;
	private float connectionBWeight;
	private float connectionCWeight;

	private double maxDistance;
	private int schemaNumber; 

	public ObjectiveFitness(int schemaNumber) {

		this.schemaNumber = schemaNumber;

		robToResDistWeight = 1;
		resToResDistWeight = 1;
		numPickupWeight = 1;
		connectedResourcesWeight = 1;
		correctSchemaWeight = 1;
		avgResToCZoneWeight = 1;
		adjacentResourcesWeight = 1;

		connectionAWeight = 1;
		connectionBWeight = 2;
		connectionCWeight = 3;

		/**
		need to change this to work with the dimensions of the environment
		needs to be the diagonal distance
		*/
		maxDistance = 20;

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
    aveResDistance = (maxDistance - getAveResourceDistance())/maxDistance; max distance between resources
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
	public double calculate(Behaviour behaviour) {

		double finalFitness = 0;

		//System.out.println("ObjectiveFitness: about to calculate");


		double temp1 = getAvgRobToResDist(behaviour) * robToResDistWeight;
		double temp2 = getAvgPickupCount(behaviour) * numPickupWeight;
		double temp3 = getAvgResToResDist(behaviour) * resToResDistWeight;
		double temp4 = calcNumConnectedResources(behaviour) * connectedResourcesWeight;
		double temp5 = getAvgResToConstZoneDist(behaviour) * avgResToCZoneWeight;

		// finalFitness += getAvgRobToResDist(behaviour) * robToResDistWeight;
		// //System.out.println("ObjectiveFitness: first calc");
		// finalFitness += getAvgPickupCount(behaviour) * numPickupWeight;
		// //System.out.println("ObjectiveFitness: second calc");
		// finalFitness += getAvgResToResDist(behaviour) * resToResDistWeight;
		// //System.out.println("ObjectiveFitness: third calc");
		// finalFitness += calcNumConnectedResources(behaviour) * connectedResourcesWeight;
		// //System.out.println("ObjectiveFitness: fourth calc");
		// finalFitness += getAvgResToConstZoneDist(behaviour) * avgResToCZoneWeight;

		finalFitness = temp1 + temp2 + temp3 + temp4 + temp5;

		//System.out.println("ObjectiveFitness: finished the calculation");
		//System.out.println("ObjectiveFitness: the finalFitness = " + finalFitness);

		System.out.println("Objective Fitness: the individual fitness components");
		System.out.println("Avg Dist between robot and resource = " + temp1);
		System.out.println("Avg pickup count = " + temp2);
		System.out.println("Avg dist between resources = " + temp3);
		System.out.println("Num connected blocks = " + temp4);
		System.out.println("Avg dist between res and cZone = " + temp5);

		return finalFitness;
	}

	private double getAvgRobToResDist(Behaviour behaviour) {

		double rawDist = behaviour.getRobToResDist(); //the non-normalised value
		double normalised = (maxDistance - rawDist) / maxDistance; //as rawVal decreases (robots move closer to resources), the normalised value increases with max when rawVal = 0 will return 1

		return normalised;
	}

	private double getAvgPickupCount(Behaviour behaviour) {

		/**
		change this maxPickups to work with a variable
		*/
		double maxPickups = 20;

		double rawPickupCount = behaviour.getNumPickups(); //avg number of resources that each robot picked up
		double normalised = rawPickupCount / maxPickups;

		return normalised;
	}

	//method to calculate the average distance between robots and the resource nearest to them at the end of the simulation
	private double getAvgResToResDist(Behaviour behaviour) {

		double rawDist = behaviour.getAvgResToResDist();
		double normalised = (maxDistance - rawDist) / maxDistance;

		return normalised;
	}

	//method to check how many resources are connected
	private double calcNumConnectedResources(Behaviour behaviour) {

		double connectionScore = 0;

		//System.out.println("ObjectiveFitness: counting the number of connected resources");

		connectionScore += behaviour.getConnectedA() * connectionAWeight;
		connectionScore += behaviour.getConnectedB() * connectionBWeight;
		connectionScore += behaviour.getConnectedC() * connectionCWeight;

		//System.out.println("ObjectiveFitness: counted all connected");

		/*
		calculate what the most ideal score is for if all the resources in the schema are connected
		*/
		double idealScore = 0;
		int totalA = 0;
		int totalB = 0;
		int totalC = 0;
		ArrayList<ResourceObject> allResources = behaviour.getPlacedResources();
		for(ResourceObject resObj : allResources) {

			String type = resObj.getType();

			if(type.equals("A")) {
				totalA++;
			}
			else if(type.equals("B")) {
				totalB++;
			}
			else if(type.equals("C")) {
				totalC++;
			}
		}

		idealScore += totalA * connectionAWeight;
		idealScore += totalB * connectionBWeight;
		idealScore += totalC * connectionCWeight;

		double normalised = connectionScore / idealScore;

		return normalised;
	}

	private double getAvgResToConstZoneDist(Behaviour behaviour) {

		double rawDist = behaviour.getAvgResToCZoneDist();
		double normalised = (maxDistance - rawDist) / maxDistance;

		return normalised;
	}
	
}