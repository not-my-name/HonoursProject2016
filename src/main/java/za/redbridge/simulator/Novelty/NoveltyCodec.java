package za.redbridge.simulator;

/*
this is one of the classes that had to be extended in order to get the Novelty comparisons
working between generations
*/

import org.encog.neural.hyperneat.HyperNEATCODEC;
import org.encog.ml.MLMethod;
import org.encog.ml.ea.genome.Genome;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATLink;
import org.encog.ml.CalculateScore;

import za.redbridge.simulator.ScoreCalculator;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class NoveltyCodec extends HyperNEATCODEC {

	private List<Genome> useList;
	private Map<Genome,NoveltyBehaviour> genomePBMap;
	private ScoreCalculator scoreCalculator;
	private int currGenNumber = 0;

	public NoveltyCodec(CalculateScore scoreCalculator) {
		this.scoreCalculator = (ScoreCalculator)scoreCalculator;
		useList = new LinkedList<>();
		genomePBMap = new HashMap<>();
	}

	@Override
	public MLMethod decode(final Genome genome) {

		NoveltyNetwork novNetwork;

		/**
		this method gets called first from the NoveltySearchStrategy in order to generate the current population
		so that they can be evaluated at the same time

		this method then gets called from the iteration method in the trainer

		*/

		NEATNetwork decoded = (NEATNetwork)super.decode(genome);
		List<NEATLink> connectionArray = new LinkedList<>();
		NEATLink[] connections = decoded.getLinks();

		for (int i = 0; i < connections.length; i++) {
			connectionArray.add(connections[i]);
		}

		novNetwork = new NoveltyNetwork(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());

		if(useList.contains(genome)) {
			novNetwork.setNoveltyBehaviour(genomePBMap.get(genome)); //adding the resultant novelty behaviour to 
		}
		else {
			useList.add(genome); //keep track of which genomes have already been decodded
			genomePBMap.put(genome, scoreCalculator.getNoveltyBehaviour(novNetwork));
		}
		
		return novNetwork;
	}
	
}