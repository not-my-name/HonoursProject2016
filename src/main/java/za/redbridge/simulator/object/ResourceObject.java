package za.redbridge.simulator.object;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;

import java.awt.Color;
import java.awt.Paint;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
//CONCURRENCY CHANGES
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;

import sim.engine.SimState;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.PolygonPortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;

/**
 * Object to represent the resources in the environment. Has a value and a weight.
 *
 * Created by jamie on 2014/07/23.
 */
public class ResourceObject extends PhysicalObject {

    //private static final Paint DEFAULT_COLOUR = new Color(255, 235, 82);
    private static final Paint DEFAULT__TRASH_COLOUR = new Color(43, 54, 50);
    private static final Paint DEFAULT__RESOURCE_COLOUR = new Color(2, 12, 156);
    private static final boolean DEBUG = false;

    public enum Side {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private Side stickySide;

    private final AnchorPoint[] leftAnchorPoints;
    private final AnchorPoint[] rightAnchorPoints;
    private final AnchorPoint[] topAnchorPoints;
    private final AnchorPoint[] bottomAnchorPoints;

    private final WeldPoint[] leftWeldPoints;
    private final WeldPoint[] rightWeldPoints;
    private final WeldPoint[] topWeldPoints;
    private final WeldPoint[] bottomWeldPoints;
    private final int weldPointN = 2;
    private final ArrayList<WeldPoint[]> weldList;

    private final Vec2 pool = new Vec2();

    private final double width;
    private final double height;
    private final int pushingRobots;
    private final double value;
    private final String type;

    private double adjustedValue;
    private boolean isCollected = false;

    private final Map<RobotObject, JointDef> pendingJoints;
    private final Map<RobotObject, Joint> joints;

    private final DetectionPoint[] detectionPoints;
    private String[] adjacentResources;
    private boolean connected;

    private ResourceObject[] adjacentList;

    // is a hack
    // private ArrayList<ResourceObject> otherResources = new ArrayList<ResourceObject>();

    public ResourceObject(World world, Vec2 position, float angle, float width, float height,
            float mass, int pushingRobots, double value, String type) {
        super(createPortrayal(width, height, type),
                createBody(world, position, angle, width, height, mass));
        this.width = width;
        this.height = height;
        this.pushingRobots = pushingRobots;
        this.value = value;
        this.type = type;

        adjustedValue = value;

        leftAnchorPoints = new AnchorPoint[pushingRobots];
        rightAnchorPoints = new AnchorPoint[pushingRobots];
        topAnchorPoints = new AnchorPoint[pushingRobots];
        bottomAnchorPoints = new AnchorPoint[pushingRobots];
        initAnchorPoints();

        leftWeldPoints = new WeldPoint[weldPointN];
        rightWeldPoints = new WeldPoint[weldPointN];
        topWeldPoints = new WeldPoint[weldPointN];
        bottomWeldPoints = new WeldPoint[weldPointN];
        weldList = new ArrayList<WeldPoint[]>();
        initWeldPoints();

        detectionPoints = new DetectionPoint[4];
        initDetectionPoints();

        adjacentResources = new String[4];

        joints = new HashMap<>(pushingRobots);
        pendingJoints = new HashMap<>(pushingRobots);

        adjacentList = new ResourceObject[4];

        if (DEBUG) {
            getPortrayal().setChildDrawable(new DebugPortrayal(Color.BLACK, true));
        }
    }

    protected static Portrayal createPortrayal(double width, double height, String type) {
        Paint color;
        switch (type) {
            case "A":
                color = new Color(2,12,156);
                break;
            case "B":
                color = new Color(0,194,16);
                break;
            case "C":
                color = new Color(194,0,0);
                break;
            default:
                color = new Color(219,110,0);
                break;
        }
        return new RectanglePortrayal(width, height, color, true);
    }

    protected static Body createBody(World world, Vec2 position, float angle, float width,
            float height, float mass) {
        BodyBuilder bb = new BodyBuilder();
        return bb.setBodyType(BodyType.DYNAMIC)
                .setPosition(position)
                .setAngle(angle)
                .setRectangular(width, height, mass)
                .setFriction(0.3f)
                .setRestitution(0.4f)
                .setGroundFriction(0.6f, 0.1f, 0.05f, 0.01f)
                .setFilterCategoryBits(FilterConstants.CategoryBits.RESOURCE)
                .build(world);
    }

    // public void initResources(ArrayList<ResourceObject> resources){
    //     otherResources = resources;
    // }

    private void initAnchorPoints() {
        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float horizontalSpacing = (float) (width / pushingRobots);
        float verticalSpacing = (float) (height / pushingRobots);

        for (Side side : Side.values()) {
            AnchorPoint[] anchorPoints = getAnchorPointsForSide(side);
            for (int i = 0; i < pushingRobots; i++) {
                final float x, y;
                if (side == Side.LEFT) {
                    x = -halfWidth;
                    y = -halfHeight + verticalSpacing * (i + 0.5f);
                } else if (side == Side.RIGHT) {
                    x = halfWidth;
                    y = -halfHeight + verticalSpacing * (i + 0.5f);
                } else if (side == Side.TOP) {
                    x = -halfWidth + horizontalSpacing * (i + 0.5f);
                    y = halfHeight;
                } else { // Side.BOTTOM
                    x = -halfWidth + horizontalSpacing * (i + 0.5f);
                    y = -halfHeight;
                }
                anchorPoints[i] = new AnchorPoint(new Vec2(x, y), side);
            }
        }
    }

    private void initDetectionPoints(){
        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float xspacing = 0.2f;
        float yspacing = 0.1f;

        Vec2 leftPos = new Vec2(-halfWidth-xspacing, -halfHeight);
        Vec2 rightPos = new Vec2(halfWidth+xspacing, -halfHeight);
        Vec2 topPos = new Vec2(0, halfHeight+yspacing);
        Vec2 bottomPos = new Vec2(0, -halfHeight-yspacing);

        DetectionPoint leftPoint = new DetectionPoint(leftPos);
        DetectionPoint rightPoint = new DetectionPoint(rightPos);
        DetectionPoint topPoint = new DetectionPoint(topPos);
        DetectionPoint bottomPoint = new DetectionPoint(bottomPos);

        detectionPoints[0] = leftPoint;
        detectionPoints[1] = rightPoint;
        detectionPoints[2] = topPoint;
        detectionPoints[3] = bottomPoint;
    }

    private void initWeldPoints(){
        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float horizontalSpacing = (float) (width / weldPointN);
        float verticalSpacing = (float) (height / weldPointN);

        float x, y;
        for(int i=0;i<weldPointN;i++){
            x = -halfWidth;
            y = -halfHeight + verticalSpacing * (i + 0.5f);
            leftWeldPoints[i] = new WeldPoint(new Vec2(x,y));
        }

        for(int i=0;i<weldPointN;i++){
            x = halfWidth;
            y = -halfHeight + verticalSpacing * (i + 0.5f);
            rightWeldPoints[i] = new WeldPoint(new Vec2(x,y));
        }

        for(int i=0;i<weldPointN;i++){
            x = -halfWidth + horizontalSpacing * (i + 0.5f);
            y = halfHeight;
            topWeldPoints[i] = new WeldPoint(new Vec2(x,y));
        }

        for(int i=0;i<weldPointN;i++){
            x = -halfWidth + horizontalSpacing * (i + 0.5f);
            y = -halfHeight;
            bottomWeldPoints[i] = new WeldPoint(new Vec2(x,y));
        }

        weldList.add(leftWeldPoints);
        weldList.add(rightWeldPoints);
        weldList.add(topWeldPoints);
        weldList.add(bottomWeldPoints);
    }

    //CONCURRENCY CHANGES
    public void updateAdjacent(CopyOnWriteArrayList<ResourceObject> resourceArray){
        for(int i=0;i<adjacentResources.length;i++){
            adjacentResources[i] = "0";
            adjacentList[i] = null;
        }

        for(int j=0;j<resourceArray.size();j++){
            if(this != resourceArray.get(j)){
                Body resourceBody = resourceArray.get(j).getBody();
                Vec2 resourcePosition = resourceBody.getPosition();
                Vec2 resourcePositionLocal = getCachedLocalPoint(resourcePosition);
                for(int i=0;i<adjacentResources.length;i++){
                    if (resourcePositionLocal.sub(detectionPoints[i].position).length() < 0.4f) {
                        adjacentResources[i] = resourceArray.get(j).getType();
                    }
                }
            }
        }
    }

    // checks if another resource is aligned with one side
    public boolean checkPotentialWeld(ResourceObject other){
        boolean t = false;
        int [] points = {0,0,0,0};
        Body resourceBody = other.getBody();
        Vec2 resourcePosition = resourceBody.getPosition();
        Vec2 resourcePositionLocal = getCachedLocalPoint(resourcePosition);
        // System.out.println(resourcePositionLocal.sub(weldList.get(0)[0].position).length());
        // System.out.println(resourcePositionLocal.sub(weldList.get(0)[1].position).length());
        for(int j=0;j<weldList.size();j++){
            for(int i=0;i<weldPointN;i++){
                if (resourcePositionLocal.sub(weldList.get(j)[i].position).length() < 0.6f) {
                    points[j] += 1;
                }
            }
        }

        for(int i=0;i<4;i++){
            if(points[i]==2){
                t = true;
            }
        }

        return t;
    }

    public void updateAlignment(){

    }

    public String [] getAdjacentResources(){
        return adjacentResources;
    }

    public ResourceObject[] getAdjacentList() {
        return adjacentList;
    }

    public String getType(){
        return type;
    }

    private AnchorPoint[] getAnchorPointsForSide(Side side) {
        switch (side) {
            case LEFT:
                return leftAnchorPoints;
            case RIGHT:
                return rightAnchorPoints;
            case TOP:
                return topAnchorPoints;
            case BOTTOM:
                return bottomAnchorPoints;
            default:
                return null;
        }
    }

    public boolean isTrash()
    {
        return (value < 0);
    }

    public void adjustValue(SimState simState) {
        Simulation simulation = (Simulation) simState;
        this.adjustedValue = value - 0.9 * simulation.getProgressFraction() * value;
    }

    @Override
    public void step(SimState simState) {
        super.step(simState);

        if (!pendingJoints.isEmpty()) {
            // Create all the pending joints and then clear them
            for (Map.Entry<RobotObject, JointDef> entry : pendingJoints.entrySet()) {
                Joint joint = getBody().getWorld().createJoint(entry.getValue());
                joints.put(entry.getKey(), joint);
            }
            pendingJoints.clear();
        }

        // Add an additional check here in case joints fail to be destroyed
        if (isCollected && !joints.isEmpty()) {
            for (Map.Entry<RobotObject, Joint> entry : joints.entrySet()) {
                RobotObject robot = entry.getKey();
                robot.setBoundToResource(false ,0);
                getBody().getWorld().destroyJoint(entry.getValue());
            }
            joints.clear();
        }
    }


    /**
     * Try join this resource to the provided robot. If successful, a weld joint will be created
     * between the resource and the robot and the method will return true.
     * @param robot The robot trying to pick up this resource
     * @return true if the pickup attempt was successful
     */
    public boolean tryPickup(RobotObject robot) {
        // Check if already collected or max number of robots already attached
        if (!canBePickedUp()) {
            //System.out.println("Pickup failed: is collected.");
            return false;
        }

        // Check if this robot is not already attached or about to be attached
        if (joints.containsKey(robot) || pendingJoints.containsKey(robot)) {
            //System.out.println("Pickup failed: about to be joined.");
            return false;
        }

        Body robotBody = robot.getBody();
        Vec2 robotPosition = robotBody.getPosition();
        Vec2 robotPositionLocal = getCachedLocalPoint(robotPosition);

        // Check the side that the robot wants to attach to
        // If it is not the sticky side don't allow it to attach
        final Side attachSide = getSideClosestToPointLocal(robotPositionLocal);
        if (stickySide != null && stickySide != attachSide) {
            return false;
        }

        AnchorPoint closestAnchor = getClosestAnchorPointLocal(robotPositionLocal);
        if (closestAnchor == null) {
            return false; // Should not happen but apparently can...
        }

        // Check robot is not unreasonably far away
        if (robotPositionLocal.sub(closestAnchor.getPosition()).length()
                > robot.getRadius() * 2.5) {
            return false;
        }

        // Set the sticky side if unset
        if (stickySide == null) {
            stickySide = attachSide;
        }

        createPendingWeldJoint(robot, closestAnchor.position);

        // Mark the anchor as taken and the robot as bound to a resource.
        closestAnchor.markTaken();
        robot.setBoundToResource(true, pushingRobots);

        robot.incPickups();

        return true;
    }

    public boolean canBePickedUp() {
        return !isCollected && !pushedByMaxRobots();
    }

    /**
     * Creates a weld joint definition between the resource and the robot and adds it to the set of
     * pending joints to be created.
     * @param robot The robot to weld to
     * @param anchorPoint The local point on the resource to create the weld
     */
    private void createPendingWeldJoint(RobotObject robot, Vec2 anchorPoint) {
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = getBody();
        wjd.bodyB = robot.getBody();
        wjd.referenceAngle = getReferenceAngle();
        wjd.localAnchorA.set(anchorPoint);
        wjd.localAnchorB.set(robot.getRadius() + 0.01f, 0); // Attach to front of robot
        wjd.collideConnected = true;

        pendingJoints.put(robot, wjd);
    }

    /**
     * Creates a weld joint definition between two resources that have been pushed together and adds it to the set of
     * pending joints to be created.
     * @param robot The resource to weld to
     * @param anchorPoint The local point on the resource to create the weld
     */
    private WeldJointDef createResourceWeldJoint(ResourceObject resource){
        // Creates weld joint attached to the center of each resource
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = getBody();
        wjd.bodyB = resource.getBody();
        wjd.referenceAngle = getReferenceAngle();
        wjd.localAnchorA.set(wjd.bodyA.getPosition());
        wjd.localAnchorB.set(wjd.bodyB.getPosition());
        wjd.collideConnected = true;

        return wjd;
    }

    /* Get a local point from a global one. NOTE: for internal use only */
    private Vec2 getCachedLocalPoint(Vec2 worldPoint) {
        final Vec2 localPoint = pool;
        getBody().getLocalPointToOut(worldPoint, localPoint);
        return localPoint;
    }

    /**
     * Get the side of this resource closest to the given point.
     * @param point A point in world-space
     * @return the side closest to the given point
     */
    public Side getSideClosestToPoint(Vec2 point) {
        return getSideClosestToPointLocal(getCachedLocalPoint(point));
    }

    private Side getSideClosestToPointLocal(Vec2 localPoint) {
        float halfWidth = (float) (width / 2);
        float halfHeight = (float) (height / 2);
        final Side side;
        if (localPoint.y > -halfHeight && localPoint.y < halfHeight) {
            if (localPoint.x > 0) {
                side = Side.RIGHT;
            } else {
                side = Side.LEFT;
            }
        } else if (localPoint.x > -halfWidth && localPoint.x < halfWidth) {
            if (localPoint.y > 0) {
                side = Side.TOP;
            } else {
                side = Side.BOTTOM;
            }
        } else if (Math.abs(localPoint.x) - halfWidth > Math.abs(localPoint.y) - halfHeight) {
            if (localPoint.x > 0) {
                side = Side.RIGHT;
            } else {
                side = Side.LEFT;
            }
        } else {
            if (localPoint.y > 0) {
                side = Side.TOP;
            } else {
                side = Side.BOTTOM;
            }
        }
        return side;
    }

    /** Get the side that robots can currently attach to. */
    public Side getStickySide () {
        return stickySide;
    }

    /**
     * Get the position of the closest anchor point in world coordinates, or null if all anchor
     * points have been taken.
     * @param position A position in world coordinates
     * @return The position of the closest available anchor point (in world coordinates), or null if
     *          none is available.
     */
    public AnchorPoint getClosestAnchorPoint(Vec2 position) {
        return getClosestAnchorPointLocal(getCachedLocalPoint(position));
    }

    /**
     * Get the closest anchor point to a position in world space, or null if none is available.
     * @param localPoint point in local coordinates
     * @return an {@link AnchorPoint} object that has not been taken yet, or null if unavailable
     */
    private AnchorPoint getClosestAnchorPointLocal(Vec2 localPoint) {
        // Get the side and corresponding anchor points
        final Side side = stickySide != null ? stickySide : getSideClosestToPointLocal(localPoint);
        final AnchorPoint[] anchorPoints = getAnchorPointsForSide(side);

        // Fast path for single robot resource
        if (pushingRobots == 1) {
            AnchorPoint anchorPoint = anchorPoints[0];
            return !anchorPoint.taken ? anchorPoint : null;
        }

        // Else iterate through anchor points finding closest one (generally only 2 options)
        AnchorPoint closestAnchorPoint = null;
        float shortestDistance = Float.MAX_VALUE;
        for (AnchorPoint anchorPoint : anchorPoints) {
            if (anchorPoint.taken) {
                continue;
            }

            float distance = anchorPoint.position.sub(localPoint).lengthSquared();
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestAnchorPoint = anchorPoint;
            }
        }

        return closestAnchorPoint;
    }

    /** Get the reference angle for joints for the current sticky side. */
    private float getReferenceAngle() {
        final float referenceAngle;
        if (stickySide == Side.LEFT) {
            referenceAngle = 0f;
        } else if (stickySide == Side.RIGHT) {
            referenceAngle = (float) Math.PI;
        } else if (stickySide == Side.BOTTOM) {
            referenceAngle = (float) Math.PI / 2;
        } else if (stickySide == Side.TOP) {
            referenceAngle = (float) -Math.PI / 2;
        } else {
            throw new IllegalStateException("Sticky side not set yet, cannot get reference angle");
        }
        return referenceAngle;
    }

    public Vec2 getNormalToSide(Side side) {
        final Vec2 normal;
        if (side == Side.LEFT) {
            normal = new Vec2(-1, 0);
        } else if (side == Side.RIGHT) {
            normal = new Vec2(1, 0);
        } else if (side == Side.TOP) {
            normal = new Vec2(0, 1);
        } else if (side == Side.BOTTOM) {
            normal = new Vec2(0, -1);
        } else {
            return null;
        }

        getBody().getWorldVectorToOut(normal, normal);
        return normal;
    }

    /**
     * Check whether this object has been collected
     * @return true if the object has been marked as collected (it has passed into the target area)
     */
    public boolean isCollected() {
        return isCollected;
    }

    /** Mark this object as collected. i.e. mark it as being in the target area. */
    public void setCollected(boolean isCollected) {
        if (isCollected == this.isCollected) {
            return;
        }

        // Sticky side could be unset if resource "bumped" into target area without robots
        // creating joints with it
        if (isCollected && stickySide != null) {
            // Break all the joints
            for (Map.Entry<RobotObject, Joint> entry : joints.entrySet()) {
                RobotObject robot = entry.getKey();
                robot.setBoundToResource(false ,0);
                getBody().getWorld().destroyJoint(entry.getValue());
            }
            joints.clear();

            // Reset the anchor points
            AnchorPoint[] anchorPoints = getAnchorPointsForSide(stickySide);
            for (AnchorPoint anchorPoint : anchorPoints) {
                anchorPoint.taken = false;
            }

            // Reset the sticky side
            stickySide = null;
        }

        this.isCollected = isCollected;
    }

    /**
     * detach the robot from the resource regardless of whether or not the resource
     * in the target area
     */
    public void forceDetach( ){
        // Break all the joints
        for (Map.Entry<RobotObject, Joint> entry : joints.entrySet()) {
            RobotObject robot = entry.getKey();
            robot.setBoundToResource(false ,0);
            getBody().getWorld().destroyJoint(entry.getValue());
        }
        joints.clear();

        // Reset the anchor points
        if(stickySide == null){return;} // only reset them if they exist
        AnchorPoint[] anchorPoints = getAnchorPointsForSide(stickySide);
        for (AnchorPoint anchorPoint : anchorPoints) {
            anchorPoint.taken = false;
        }
    }

    /** Check whether this resource already has the max number of robots attached to it. */
    public boolean pushedByMaxRobots() {
        return getNumberPushingRobots() >= pushingRobots;
    }

    /** Get the number of robots currently pushing/attached to this resource. */
    public int getNumberPushingRobots() {
        return joints.size() + pendingJoints.size();
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Set<RobotObject> getPushingRobots(){
        return joints.keySet();
    }

    public double getValue() {
        return value;
    }

    /** Fitness value adjusted (decreased) for the amount of time the simulation has been running */
    public double getAdjustedValue() {
        return adjustedValue;
    }

    /**
     * Container class for points along the sticky edge of the resource where robots can attach to
     * the resource.
     */
    public class AnchorPoint {
        private final Vec2 position;
        private final Side side;

        private boolean taken = false;

        private Vec2 worldPosition = null;

        /** @param position position local to the resource */
        private AnchorPoint(Vec2 position, Side side) {
            this.position = position;
            this.side = side;
        }

        private void markTaken() {
            if (side != stickySide) {
                throw new IllegalStateException("Anchor point not on sticky side");
            }

            taken = true;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }

            return worldPosition;
        }

        public Side getSide() {
            return side;
        }

        public boolean isTaken() {
            return taken;
        }
    }

    public class DetectionPoint{
        private final Vec2 position;
        private boolean collided;
        private Vec2 worldPosition = null;

        private DetectionPoint(Vec2 position){
            this.position = position;
        }

        private void markColliding() {
            collided = true;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }
            return worldPosition;
        }

        public boolean isTaken() {
            return collided;
        }
    }

    public class WeldPoint {
        private final Vec2 position;
        private boolean aligned = false;
        private Vec2 worldPosition = null;
        private ResourceObject alignedResource;

        /** @param position position local to the resource */
        private WeldPoint(Vec2 position) {
            this.position = position;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }

            return worldPosition;
        }

        public void alignWeld(ResourceObject resource){
            alignedResource = resource;
        }

        public ResourceObject getAlignedResource(){
            return alignedResource;
        }

        public boolean isAligned() {
            return aligned;
        }
    }

    /*
     * Simple portrayal for drawing an additional line along the bottom of the resource to help
     * determine which way round the resource is.
     */
    private class DebugPortrayal extends PolygonPortrayal {

        public DebugPortrayal(Paint paint, boolean filled) {
            super(4, paint, filled);

            final float width = (float) getWidth() * 0.8f;
            final float height = (float) getHeight() * 0.1f;

            final float dy = (float) getHeight() * 0.3f;

            float halfWidth = width / 2;
            float halfHeight = height / 2;
            vertices[0].set(-halfWidth, -halfHeight - dy);
            vertices[1].set(halfWidth, -halfHeight - dy);
            vertices[2].set(halfWidth, halfHeight - dy);
            vertices[3].set(-halfWidth, halfHeight - dy);
        }
    }

    //return size of the resource
    public int getSize()
    {
        return pushingRobots;
    }
}
