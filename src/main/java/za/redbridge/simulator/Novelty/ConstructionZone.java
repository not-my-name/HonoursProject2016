package za.redbridge.simulator;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import sim.util.Double2D;

import sim.engine.SimState;
// import za.redbridge.simulator.FitnessStats;
// import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.Collideable;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.object.*;


import static za.redbridge.simulator.physics.AABBUtil.getAABBHeight;
import static za.redbridge.simulator.physics.AABBUtil.getAABBWidth;
import static za.redbridge.simulator.physics.AABBUtil.resizeAABB;

/**
 * Created by shsu on 2014/08/13.
 */

/*
this is the class that handles the main construction site
the first connected blocks become the construction zone

once the construction zone is created, robots only get rewarded
for connecting blocks to the zone, not for connecting separate blocks
*/

public class ConstructionZone {

    private Vec2 cZonePosition; //variable to store the coordinates of the center of the construction zone

    private static final boolean ALLOW_REMOVAL = false;

    private static final float BLAME_BOX_EXPANSION_RATE = 1.5f;
    private static final int BLAME_BOX_TRIES = 5;

    // private int width, height;
    // private final AABB aabb;

    //total resource value in this target area
    //private final FitnessStats fitnessStats;

    //hash set so that object values only get added to forage area once
    private final Set<ResourceObject> connectedResources = new HashSet<>();

    // resources never get added to watched fixtures list
    private final List<Fixture> watchedFixtures = new ArrayList<>();

    private ResourceObject[] connectionOrder = new ResourceObject[15]; //change the size to the number of resource in the zone
    private int numConnected; //variable to keep track of how many resources are in the construction zone

    int resource_count = 0;
    int ACount = 0;
    int BCount = 0;
    int CCount = 0;

    /**
    check why this constructor still creates the fitness stats object
    make sure that youre not using any of the fitness stats methods in any of the code and that youre only using the
    code from behaviour and the objective/novelty fitness classes
    */

    public ConstructionZone() {} 

    public void startConstructionZone(ResourceObject r1, ResourceObject r2) {
        Vec2 r1Pos = r1.getBody().getPosition();
        Vec2 r2Pos = r2.getBody().getPosition();
        float aveX = (r1Pos.x + r2Pos.x)/2;
        float aveY = (r1Pos.y + r2Pos.y)/2;
        cZonePosition = new Vec2(aveX, aveY);

        addResource(r1);
        addResource(r2);

        numConnected = 2;
        connectionOrder[0] = r1;
        connectionOrder[1] = r2;
    }

    /*
    Method that adds resource to the construction zone/target area
    @param ResourceObject resource: the resource that is to be added

    This method adds fitness:
        FResource(resource) = F(resource)*{1 if fitsInSchema; 0 otherwise}
    **/
    public void addResource(ResourceObject resource) {
        double FResource = 0D;
        if (connectedResources.add(resource)) { //checks if the resource hasnt already been added
            //fitnessStats.addToTeamFitness(resource.getValue());

            if(resource.getValue() > 0) {
                resource_count++;
            }

            if (resource.getType().equals("A")) {
                ACount++;
            }
            else if (resource.getType().equals("B")) {
                BCount++;
            }
            else if (resource.getType().equals("C")) {
                CCount++;
            }

            System.out.println(Arrays.toString(resource.getAdjacentList()));
            // Mark resource as collected (this breaks the joints)
            //resource.setCollected(true);
            numConnected += 1;
            resource.setConstructed();
            connectionOrder[numConnected-1] = resource; //updating the connection order once a resource can be added

            resource.getPortrayal().setPaint(Color.CYAN);
            resource.getBody().setActive(false);
        }
    }

    public int getACount() {
        return ACount;
    }

    public int getBCount() {
        return BCount;
    }

    public int getCCount() {
        return CCount;
    }

    public Vec2 getCZonePosition() {
        return cZonePosition;
    }

    public Set<ResourceObject> getConnectedResources () {
        return connectedResources;
    }

    public boolean isInConstructionZone(ResourceObject r) {
        if (connectedResources.contains(r)) {
            return true;
        }
        else {
            return false;
        }
    }

    private void removeResource(ResourceObject resource) {
        if (connectedResources.remove(resource)) {
            //fitnessStats.addToTeamFitness(-resource.getValue());

            if(resource.getValue() > 0) resource_count--;
            // Mark resource as no longer collected
            resource.setCollected(false);
            resource.getPortrayal().setPaint(Color.MAGENTA);

            // Set<RobotObject> pushingBots = findRobotsNearResource(resource);

            // if (!pushingBots.isEmpty()) {
            //     double adjustedFitness = resource.getAdjustedValue() / pushingBots.size();
            //     for (RobotObject robot : pushingBots) {
            //         fitnessStats.addToPhenotypeFitness(robot.getPhenotype(), -adjustedFitness);
            //     }
            // }
        }
    }

    /*
     * Finds robots very close to the ResourceObject that can be blamed for pushing the resource
     * in/out of target area.
     */
    // private Set<RobotObject> findRobotsNearResource(ResourceObject resource) {
    //     // Check which robots pushed the resource out based on a bounding box
    //     Fixture resourceFixture = resource.getBody().getFixtureList();
    //     AABB resourceBox = resourceFixture.getAABB(0);

    //     // Try query robots within the AABB of the resource
    //     Set<RobotObject> robots = new HashSet<>();
    //     RobotObjectQueryCallback callback = new RobotObjectQueryCallback(robots);
    //     getBody().getWorld().queryAABB(callback, resourceBox);

    //     if (!robots.isEmpty()) {
    //         return robots;
    //     }

    //     // If no robots found, iteratively expand the dimensions of the query box
    //     AABB blameBox = new AABB(resourceBox);
    //     for (int i = 0; i < BLAME_BOX_TRIES; i++) {
    //         float width = getAABBWidth(blameBox) * BLAME_BOX_EXPANSION_RATE;
    //         float height = getAABBHeight(blameBox) * BLAME_BOX_EXPANSION_RATE;
    //         resizeAABB(blameBox, width, height);
    //         getBody().getWorld().queryAABB(callback, blameBox);

    //         if (!robots.isEmpty()) {
    //             break;
    //         }
    //     }
    //     return robots;
    // }

    public int getNumberOfConnectedResources() {
        return resource_count;
        //connectedResources.size();
    }

    // public AABB getAabb() {
    //     return aabb;
    // }

    // public int getWidth() {
    //     return width;
    // }

    // public int getHeight() {
    //     return height;
    // }

    public int [] getResourceTypeCount () {
        int [] typeCount = {ACount, BCount, CCount};
        return typeCount;
    }

    // public FitnessStats getFitnessStats() {
    //     return fitnessStats;
    // }

    // @Override
    public void handleBeginContact(Contact contact, Fixture otherFixture) {
        if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
            return;
        }

        if (!watchedFixtures.contains(otherFixture)) {
            watchedFixtures.add(otherFixture);
        }
    }

    // @Override
    public void handleEndContact(Contact contact, Fixture otherFixture) {
        if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
            return;
        }

        // Remove from watch list
        watchedFixtures.remove(otherFixture);

        // Remove from the score
        if (ALLOW_REMOVAL) {
            ResourceObject resource = (ResourceObject) otherFixture.getBody().getUserData();
            removeResource(resource);
        }
    }

    private static class RobotObjectQueryCallback implements QueryCallback {

        final Set<RobotObject> robots;

        RobotObjectQueryCallback(Set<RobotObject> robots) {
            this.robots = robots;
        }

        @Override
        public boolean reportFixture(Fixture fixture) {
            if (!fixture.isSensor()) { // Don't detect robot sensors, only bodies
                Object bodyUserData = fixture.getBody().getUserData();
                if (bodyUserData instanceof RobotObject) {
                    robots.add((RobotObject) bodyUserData);
                }
            }

            return true;
        }
    }

}
