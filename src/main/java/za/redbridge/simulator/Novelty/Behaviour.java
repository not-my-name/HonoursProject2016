package za.redbridge.simulator;

import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.object.ResourceObject;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.*;
import za.redbridge.simulator.object.RobotObject;
import org.jbox2d.common.Vec2;

/*
use this class to manage and monitor all the different behaviours that are needed to find the novelty
score of an individual
*/

public class Behaviour {

	private ConstructionTask constructionTask;
	private ConstructionZone constructionZone;

	//arrays to be used to check the configurations of the blocks in the construction zone
	private String [][] AConnections;
	private String [][] BConnections;
	private String [][] CConnections;

	private int numPickups; //number of times robots picked up a resource

	private double distanceTravelled; //the total distance travelled by a team of robots in a single simulation

	private ArrayList<ResourceObject> placedResources; //positions of all the resources at the end of the simulation
	private ArrayList<RobotObject> placedRobots;

	//private ArrayList<double> robToResDist; //array to store the distance from each robot to its nearest resource

	private double avgResToResDist; //variable to store the average distance between resources at the end of the simulation
	private double avgRobToResDist; //variable to store the average distance between a robot and the nearest resource
	private double avgResToCZoneDist; //variable to store the average distance between a resource and the centre of the construction zone

	//variables to count how many different types of each block there are in the construction zone
	private int connectedA;
	private int connectedB;
	private int connectedC;

	public Behaviour(ConstructionTask constructionTask, ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, double distanceTravelled) {
		
		this.constructionTask = constructionTask;
		this.constructionZone = this.constructionTask.getConstructionZone();
		//this.distanceTravelled = distanceTravelled;

		int [] czTypeCount = this.constructionZone.getResourceTypeCount();
		AConnections = new String [czTypeCount[0]][4];
		BConnections = new String [czTypeCount[1]][4];
		CConnections = new String [czTypeCount[2]][4];

		populateConnections();

		placedResources = new ArrayList<ResourceObject>();
		placedRobots = new ArrayList<RobotObject>();

		numPickups = 0;
		distanceTravelled = 0;
		avgResToResDist = 0;
		avgRobToResDist = 0;
		avgResToCZoneDist = 0;

		connectedA = 0;
		connectedB = 0;
		connectedC = 0;

		//using methods to initialise values
		setPlacedRobots(currentRobots);
		setPlacedResources(currentResources);
		setDistanceTravelled(distanceTravelled);
		countPickups();
		countConnected();

		calcResToResDist(); 
		calcRobToResDist();
		calcResToCZoneDist();
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

	private void setDistanceTravelled(double distanceTravelled) {
		System.out.println("Behaviour: The setDistanceTravelled method is being called");
		this.distanceTravelled = distanceTravelled;
	}

	//method to get the resources and their respective locations at the end of the simulation
	private void setPlacedResources(ArrayList<ResourceObject> placedResources) {
		for(ResourceObject rO : placedResources) {
			this.placedResources.add(rO);
		}

	}

	private void setPlacedRobots(ArrayList<RobotObject> placedRobots) {

		for(RobotObject r : placedRobots) {
			this.placedRobots.add(r);
		}
	}

	/*
	method to iterate over the list of robots at the end of the simulation
	and sum the total number of times that a resource was picked up
	*/
	private void countPickups() {

		for(RobotObject bot : this.placedRobots) {
			numPickups += bot.getNumPickups();
		}

		numPickups = numPickups / this.placedRobots.size(); //average number of pickups per robot

	}

	/**
	need to make sure that this is what Geoff meant by counting the number of connected blocks
	currently only checking the connected blocks in the construction zone
	should probably check on all connected blocks somehow
	*/
	private void countConnected() {
		connectedA += constructionZone.getACount();
		connectedB += constructionZone.getBCount();
		connectedC += constructionZone.getCCount();
	}

	//method to calculate the distance between each robot and the resouurce closest to it
	private void calcRobToResDist() {

		for(RobotObject r : placedRobots) {
			double minDist = 10000; //the distance between the current robot and its nearest resource
			Vec2 robotPosition = r.getBody().getPosition();
			for(ResourceObject res : placedResources) {
				Vec2 resPosition = res.getBody().getPosition();
				double actualDist = calculateDistance(robotPosition, resPosition);

				if( actualDist < minDist ) {
					minDist = actualDist;
				}
			}
			avgRobToResDist += minDist;
		}

		avgRobToResDist = avgRobToResDist / placedRobots.size();
	}

	//method to calculate the average distance between each resource at the end of the simulation
	private void calcResToResDist() {
		/**
		check that this loop iterates over all the resources and doesnt miss the last 2
		*/
		for(int k = 0; k < placedResources.size()-1; k++) {
			Vec2 origin = placedResources.get(k).getBody().getPosition();
			for(int j = k+1; j < placedResources.size(); j++) {
				Vec2 destination = placedResources.get(j).getBody().getPosition();
				avgResToResDist += calculateDistance(origin, destination);
			}
		}

		avgResToResDist = avgResToResDist/placedResources.size();
	}

	//method to calculate the average distance between the resources and the centre of the construction zone
	private void calcResToCZoneDist() {

		Vec2 destPoint = constructionZone.getCZonePosition();

		for(ResourceObject resObj : placedResources) {
			Vec2 originPoint = resObj.getBody().getPosition();
			avgResToCZoneDist += calculateDistance(originPoint, destPoint);
		}

		avgResToCZoneDist = avgResToCZoneDist / placedResources.size(); //average distance to construction zone per resource

	}

	private float calculateDistance(Vec2 originLocation, Vec2 destLocation) {

        double firstX = originLocation.x;
        double firstY = originLocation.y;

        double secondX = destLocation.x;
        double secondY = destLocation.y;

        float distance = (float) Math.sqrt(
                        Math.pow(firstX-secondX, 2) +
                        Math.pow(firstY-secondY, 2));

        return distance;

    }

    public double getRobToResDist() {
    	return this.avgRobToResDist;
    }

    public int getNumRobots() {
    	return this.placedRobots.size();
    }

	public ConstructionZone getConstructionZone() {
		return this.constructionZone;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public ArrayList<ResourceObject> getPlacedResources() {
		return this.placedResources;
	}

	public int getNumPickups() {
		return this.numPickups;
	}

	public double getDistanceTravelled() {
		return this.distanceTravelled;
	}

	public double getAvgResToResDist() {
		return this.avgResToResDist;
	}

	public double getAvgRobToResDist() {
		return this.avgRobToResDist;
	}

	public double getAvgResToCZoneDist() {
		return this.avgResToCZoneDist;
	}

	public int getConnectedA() {
		return this.connectedA;
	}

	public int getConnectedB() {
		return this.connectedB;
	}

	public int getConnectedC() {
		return this.connectedC;
	}

	public int compareStructure (Behaviour newBehaviour) {
		return 0;
	}
}