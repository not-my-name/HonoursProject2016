package za.redbridge.simulator;

/*
a class to monitor and manage all the necessary structures to calculate the novelty fitness
*/

public class NoveltyFitness{

	private Archive archive; //the archive that stores all the most novel behaviour for comparison / novelty calculations

	private AggregateBehaviour aggregateBehaviour; //stores all the results for the individual over x simulation runs
	private ArrayList<AggregateBehaviour> currentGeneration; //stores the aggregate behaviours for all individuals in the current generation
	private int index; //variable to indiciate which aggregate behaviour is currently being examined

	private float localNoveltyWeight; //the weight for the local novelty score
	private float archiveNoveltyWeight; //the weight for the novelty w.r.t the archive

	public NoveltyFitness(AggregateBehaviour aggregateBehaviour, ArrayList<AggregateBehaviour> currentGeneration, int index) {
		this.aggregateBehaviour = aggregateBehaviour;
		this.currentGeneration = currentGeneration;
		this.index = index;

		localNoveltyWeight = 1;
		archiveNoveltyWeight = 1;

	}

	//method to calculate the overall novelty fitness of the individual
	public double calculate() {

		double totalFitness = 0;

		totalFitness += calculateLocalNovelty() * localNoveltyWeight;
		totalFitness += calculateArchiveNovelty() * archiveNoveltyWeight;

		return totalFitness;

	}

	//method to calculate the novelty of an individual's behaviour relative to the other individuals in the population
	private double calculateLocalNovelty() {

		double localNovelty = 0;

		for(int k = 0; k < currentGeneration.size(); k++) {
			if(k != index) { //so you dont compare the current aggregateBehaviour against itself
				
			}
		}

		return localNovelty;

	}

	//method to calculate the novelty of an individual's behaviour relative to the behaviours in the archive
	private double calculateArchiveNovelty() {

		double archiveNovelty = 0;

		return archiveNovelty;

	}
	
}