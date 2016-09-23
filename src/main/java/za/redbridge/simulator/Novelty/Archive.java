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
import za.redbridge.simulator.Behaviour;

public class Archive {

	private HashMap<MLMethod,Behaviour> history;
	private int archiveLength;


	public Archive() {

		archiveLength = 0;
		history = new HashMap<MLMethod,Behaviour>();

	}

	public double checkNovelty(Behaviour newStructure) {
		//method to compare the structure created by another controller
		//to all the structures in the archive

		//iterate over all the previous behaviours and 
		//compare to new one

		double noveltyScore = 0;

		for(Behaviour b : history.values()) {
			noveltyScore += b.compareTo(newStructure);
		}

		return noveltyScore;
	}

	/*
	method to add
	*/
	public void addToArchive(MLMethod network, Behaviour newBehaviour) {
		history.put(network, newBehaviour);
		archiveLength++;
	}

	public int getArchiveLength() {
		return archiveLength;
	}

	/*
	method to return the Behaviour associated with a
	specific network
	*/
	public Behaviour getBehaviour(MLMethod network) {
		return history.get(network);
	}
	
}