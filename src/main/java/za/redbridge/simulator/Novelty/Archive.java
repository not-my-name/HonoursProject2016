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

	private ArrayList<NoveltyBehaviour> history;
	private int archiveLength;


	public Archive() {

		archiveLength = 0;
		history = new ArrayList<NoveltyBehaviour>();

	}

	public double checkNovelty(Behaviour newStructure) {
		//method to compare the structure created by another controller
		//to all the structures in the archive

		//iterate over all the previous behaviours and 
		//compare to new one

		//will need to call the checkNovelty methods in the behaviour class for each
		//item in the history
		//the behaviour class will store different types of behaviours that can be measured
		//for novelty

		double noveltyScore = 0;

		for(Behaviour b : history) { //iterate over past novel behaviours
			noveltyScore += b.compareStructure(newStructure);
		}

		return noveltyScore;
	}

	/*
	method to add
	*/
	public void addToArchive(Behaviour newBehaviour) {
		history.add(newBehaviour);
		archiveLength++;
	}

	public int getArchiveLength() {
		return archiveLength;
	}

	/*
	method to return the Behaviour associated with a
	specific network
	*/
	public Behaviour getBehaviour(int index) {
		return history.get(index);
	}
	
}