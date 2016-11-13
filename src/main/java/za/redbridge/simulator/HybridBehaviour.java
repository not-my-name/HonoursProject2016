package za.redbridge.simulator;

/*
a class to manage the combined results of the novelty
and the objective behaviours for an individual*/

public class HybridBehaviour extends NoveltyBehaviour {

    private Behaviour objectiveBehaviour;
    private double objectiveFitness;
    private Objectiv

    public HybridBehaviour(ArrayList<RobotObject> currentRobots, ConstructionTask constructionTask, Behaviour objectiveBehaviour) {
        super(currentRobots, constructionTask);
    }

    public Behaviour getObjectiveBehaviour() {
        return objectiveBehaviour;
    }


}
