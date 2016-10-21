/*
Author: Ruben Putter
Date: 22 September 2016

This is a class to maintain a history of the novel behaviours
that get produced by different controllers

Given a behaviour, compare to history of behaviours
*/

package za.redbridge.simulator;

import org.encog.ml.MLMethod;
import java.util.*;
import za.redbridge.simulator.NoveltyBehaviour;

/**
check if there will be concurrency issues with accessing the archive
*/

public class Archive {

	private ArrayList<NoveltyBehaviour> noveltyArchive;
	private ArrayList<NoveltyBehaviour> currentGeneration; //arraylist to store the individual behaviours that are part of the same generation
	private int numNearest; //number of nearest neighbours to use when calculating novelty


	public Archive() {

		noveltyArchive = new ArrayList<NoveltyBehaviour>();
		currentGeneration = new ArrayList<NoveltyBehaviour>();

		numNearest = 10;

	}

	/*
	method that takes in an array of behaviours that have already been created in the simulation and
	uses the novelty fitness class to calculate the relative novelty of the behaviours in the given collection

	this method is called from the getNoveltyBehaviour() method in scoreCalculator
	*/
	public NoveltyBehaviour findMostNovel(NoveltyBehaviour[] behaviourCollection) {

		NoveltyFitness noveltyFitness = new NoveltyFitness(behaviourCollection);
		/**
		check that the novelty behaviour that gets sent back here is the same one that was selected in the NoveltyFitness class
		do this by checking that its nearest neighbours arrays are populated
		*/
		NoveltyBehaviour mostNovel = noveltyFitness.calcSimulationLocalNovelty();
		currentGeneration.add(mostNovel); //adding the most novel result from the simulation runs to the current generation

		return mostNovel;

	}

	//method to calculate the novelty of each behaviour in the current generation
	//compare to the rest of the behaviours in the current generation
	public void calculateGenerationNovelty() {

		NoveltyBehaviour[] currentGenerationArray = new NoveltyBehaviour[currentGeneration.size()];
		

	}

	public double getNovelty(NoveltyBehaviour novBeh) {
		/**
		find the given behaviour in the current generation
		and get its behaviouralSparseness (average distance to k nearest neighbours)
		*/
	}

	public void clearCurrentGeneration() {
		currentGeneration.clear();
	}
	
}