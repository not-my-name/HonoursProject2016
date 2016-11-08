package za.redbridge.simulator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.population.Population;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.training.NEATGenome;
import org.encog.neural.networks.BasicNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import za.redbridge.controller.NEAT.NEATPopulation;
import org.encog.neural.neat.NEATPopulation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static za.redbridge.simulator.Utils.getLoggingDirectory;
//import static za.redbridge.simulator.Utils.getParamLoggingDirectory;
import static za.redbridge.simulator.Utils.saveObjectToFile;

public class StatsRecorder {
	private static final Logger log = LoggerFactory.getLogger(StatsRecorder.class);

	private final EvolutionaryAlgorithm trainer;
	private final ScoreCalculator calculator;

	private Path performance_StatsFile;
	private Path fitness_StatsFile;
	private Path numAConnected_StatsFile;
	private Path numBConnected_StatsFile;
	private Path numCConnected_StatsFile;
	private Path totalBlocksConnected_StatsFile;
	private Path numConstructionZones_StatsFile;
	private Path normNumBlocksConnected_StatsFile;

	private Path evaluationDirectory; //directory in which to store the stats for the above files

	//private Genome currentBestGenome;
	private double currentBestScore = 0;
	private Path rootDirectory;
	private Path populationDirectory;
	private Path bestNetworkDirectory;

	private Genome currentBestGenome;

	private String params;

	public StatsRecorder(EvolutionaryAlgorithm trainer, ScoreCalculator calculator) {
	    this.trainer = trainer;
	    this.calculator = calculator;

	    //this.envValues = envValues;
	    initFiles();
	}

	private void initFiles() {
	    initDirectories();
	    initStatsFiles();
	}

	private void initDirectories() {

		rootDirectory = getLoggingDirectory();

	    initDirectory(rootDirectory);

		evaluationDirectory = rootDirectory.resolve("Experiment_Results");
		initDirectory(evaluationDirectory);

	    populationDirectory = rootDirectory.resolve("populations");
	    initDirectory(populationDirectory);

	    bestNetworkDirectory = rootDirectory.resolve("best networks");
	    initDirectory(bestNetworkDirectory);
	}

	private static void initDirectory(Path path) {
	    try {
	        Files.createDirectories(path);
	    } catch (IOException e) {
	        log.error("Unable to create directories", e);
	    }
	}

	private void initStatsFiles() {

		performance_StatsFile = rootDirectory.resolve("performanceStats.csv");
		initStatsFile(performance_StatsFile);

		fitness_StatsFile = evaluationDirectory.resolve("fitnessStats.csv");
		initStatsFile(fitness_StatsFile);

		numAConnected_StatsFile = evaluationDirectory.resolve("numConnectedAStats.csv");
		initStatsFile(numAConnected_StatsFile);

		numBConnected_StatsFile = evaluationDirectory.resolve("numConnectedBStats.csv");
		initStatsFile(numBConnected_StatsFile);

		numCConnected_StatsFile = evaluationDirectory.resolve("numConnectedCStats.csv");
		initStatsFile(numCConnected_StatsFile);

		totalBlocksConnected_StatsFile = evaluationDirectory.resolve("totalBlocksConnected.csv");
		initStatsFile(totalBlocksConnected_StatsFile);

		numConstructionZones_StatsFile = evaluationDirectory.resolve("numConstructionZones.csv");
		initStatsFile(numConstructionZones_StatsFile);

		normNumBlocksConnected_StatsFile = evaluationDirectory.resolve("normalisedTotalNumBlocksConnected.csv");
		initStatsFile(normNumBlocksConnected_StatsFile);
	}

	private static void initParamFile(Path path, String params) {
	    try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
	        writer.write(params);
	    } catch (IOException e) {
	        log.error("Unable to initialize stats file", e);
	    }
	}

	private static void initStatsFile(Path path) {
	    try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
	        writer.write("gemeratopm, max, min, mean, standev\n");
	    } catch (IOException e) {
	        log.error("Unable to initialize stats file", e);
	    }
	}

	public void recordIterationStats() {
	    int generation = trainer.getIteration();
	    log.info("generation " + generation + " complete");

	    recordStats(calculator.getPerformanceStatsFile(), generation, performance_StatsFile);
	    recordStats(calculator.getFitnessStatsFile(), generation, fitness_StatsFile);
	    recordStats(calculator.getConnectedAFile(), generation, numAConnected_StatsFile);
	    recordStats(calculator.getConnectedBFile(), generation, numBConnected_StatsFile);
	    recordStats(calculator.getConnectedCFile(), generation, numCConnected_StatsFile);
	    recordStats(calculator.getAvgBlocksConnectedFile(), generation, totalBlocksConnected_StatsFile);
	    recordStats(calculator.getNumConstructionZonesFile(), generation, numConstructionZones_StatsFile);
	    recordStats(calculator.getNormNumConnectedFile(), generation, normNumBlocksConnected_StatsFile);

        //savePopulation((NEATPopulation) trainer.getPopulation(), generation);

        // Check if new best network and save it if so
        NEATGenome newBestGenome = (NEATGenome) trainer.getBestGenome();
        // System.out.println("Best genome: " + trainer.getBestGenome());
        if (newBestGenome != currentBestGenome) {
            saveGenome(newBestGenome, generation);
            currentBestGenome = newBestGenome;
        }
	}

	private void savePopulation(Population population, int generation) {
	    String filename = "generation-" + generation + ".ser";
	    Path path = populationDirectory.resolve(filename);
	    saveObjectToFile(population, path);
	}


	private NEATNetwork decodeNeatGenome(Genome genome) {
	    return (NEATNetwork) trainer.getCODEC().decode(genome);
	}

	private void saveGenome(NEATGenome genome, int epoch) {
	    Path directory = bestNetworkDirectory.resolve("epoch-" + epoch);
	    initDirectory(directory);

	    String txt;

	    log.info("New best genome! Epoch: " + epoch + ", score: "  + genome.getScore());
	    txt = String.format("epoch: %d, fitness: %f", epoch, genome.getScore());

	    Path txtPath = directory.resolve("info.txt");
	    try (BufferedWriter writer = Files.newBufferedWriter(txtPath, Charset.defaultCharset())) {
	        writer.write(txt);
	    } catch (IOException e) {
	        log.error("Error writing best network info file", e);
	    }

	    NEATNetwork network = decodeNeatGenome(genome);
	    saveObjectToFile(network, directory.resolve("network.ser"));

	    //GraphvizEngine.saveGenome(genome, directory.resolve("graph.dot"));
	}

	private void recordStats(DescriptiveStatistics stats, int generation, Path filepath) {
	    double max = stats.getMax();
	    double min = stats.getMin();
	    double mean = stats.getMean();
	    double sd = stats.getStandardDeviation();
	    stats.clear();

	    log.debug("Recording stats - max: " + max + ", mean: " + mean);
	    saveStats(filepath, generation, max, min, mean, sd);
	}

	private BasicNetwork decodeGenome(Genome genome) {
	    return (BasicNetwork) trainer.getCODEC().decode(genome);
	}

	private static void saveStats(Path path, int generation, double max, double min, double mean,
	        double sd) {
	    String line = String.format("%d, %f, %f, %f, %f\n", generation, max, min, mean, sd);

	    final OpenOption[] options = {
	            StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE
	    };
	    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path,
	            Charset.defaultCharset(), options))) {
	        writer.append(line);
	    } catch (IOException e) {
	        log.error("Failed to append to log file", e);
	    }
	}
}
