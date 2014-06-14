package de.tum.in.mulan.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import de.tum.in.mulan.experiments.Cube.Experiment.Step;

/**
 * cube for analyzing results
 * 
 * @author LehnereS
 */
public class Cube {
	
	enum aseval {
		InfoGainAttributeEval, ReliefFAttributeEval, CfsSubsetEval
	}
	
	enum clusterer {
		EM, SimpleKMeans, HierarchicalClusterer
	}
	
	enum clustermethod {
		SINGLE, COMPLETE, MEAN, NULL
	}
	
	enum dataset {
		CAL500, enron, medical, yeast
	}
	
	enum distancemeasure {
		MinkowskiDistance, SpearmanCoefficient, EuclideanDistance, ManhattanDistance, ChebyshevDistance, NULL
	}
	
	class Experiment {
		
		class Step {
			
			double		exampleAccuracy, coverage, exampleFMeasure, exampleRecall, examplePrecision, subsetAcc,
					microFMeasure, microRecall, microPrecision, microAUC, hammingLoss, oneError, rankingLoss,
					numClusters, avgLabels, avgDensity, avgCardinality;
			
			double[]	measures	= new double[16];
			Experiment	experiment;
			
			public Step(final Experiment experiment, final double exampleAccuracy, final double coverage,
					final double exampleFMeasure, final double exampleRecall, final double examplePrecision,
					final double subsetAcc, final double microFMeasure, final double microRecall,
					final double microPrecision, final double microAUC, final double hammingLoss,
					final double oneError, final double rankingLoss, final double avgLabels, final double avgDensity,
					final double avgCardinality, final double numClusters) {
				this.exampleAccuracy = exampleAccuracy;
				this.coverage = coverage;
				this.exampleFMeasure = exampleFMeasure;
				this.exampleRecall = exampleRecall;
				this.examplePrecision = examplePrecision;
				this.subsetAcc = subsetAcc;
				this.microFMeasure = microFMeasure;
				this.microRecall = microRecall;
				this.microPrecision = microPrecision;
				this.microAUC = microAUC;
				this.hammingLoss = hammingLoss;
				this.oneError = oneError;
				this.rankingLoss = rankingLoss;
				this.avgLabels = avgLabels;
				this.avgDensity = avgDensity;
				this.avgCardinality = avgCardinality;
				this.experiment = experiment;
				this.numClusters = numClusters;
				measures[0] = exampleAccuracy;
				measures[1] = coverage;
				measures[2] = exampleFMeasure;
				measures[3] = exampleRecall;
				measures[4] = examplePrecision;
				measures[5] = subsetAcc;
				measures[6] = microFMeasure;
				measures[7] = microRecall;
				measures[8] = microPrecision;
				measures[9] = microAUC;
				measures[10] = hammingLoss;
				measures[11] = oneError;
				measures[12] = rankingLoss;
				measures[13] = avgLabels;
				measures[14] = avgDensity;
				measures[15] = avgCardinality;
			}
			
			double getMeasure(final int i) {
				return measures[i];
			}
			
			@Override
			public String toString() {
				return experiment.toString() + " no clusters: " + numClusters;
			}
		}
		
		private final String	name;
		
		mllearner				ml;
		
		sllearner				sl;
		aseval					as;
		clusterer				cl;
		clustermethod			cm;
		dataset					data;
		distancemeasure			dm;
		HashMap<Double, Step>	steps	= new HashMap<>();
		boolean					log, ranks, outlier;
		
		public Experiment(final String name, final mllearner ml, final sllearner sl, final aseval as,
				final clusterer cl, final clustermethod cm, final dataset data, final distancemeasure dm,
				final double exampleAccuracy, final double coverage, final double exampleFMeasure,
				final double exampleRecall, final double examplePrecision, final double subsetAcc,
				final double microFMeasure, final double microRecall, final double microPrecision,
				final double microAUC, final double hammingLoss, final double oneError, final double rankingLoss,
				final double numClusters, final double avgLabels, final double avgDensity, final double avgCardinality,
				final boolean log, final boolean ranks, final boolean outliner) {
			this.name = name;
			this.ml = ml;
			this.sl = sl;
			this.as = as;
			this.cl = cl;
			this.cm = cm;
			this.data = data;
			this.dm = dm;
			this.ranks = ranks;
			this.log = log;
			outlier = outliner;
			steps.put(numClusters, new Step(this, exampleAccuracy, coverage, exampleFMeasure, exampleRecall,
					examplePrecision, subsetAcc, microFMeasure, microRecall, microPrecision, microAUC, hammingLoss,
					oneError, rankingLoss, avgLabels, avgDensity, avgCardinality, numClusters));
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Experiment) return name.equals(((Experiment) obj).name);
			// return (this.ml == ((Experiment) obj).ml)
			// && (this.sl == ((Experiment) obj).sl)
			// && (this.as == ((Experiment) obj).as)
			// && (this.cl == ((Experiment) obj).cl)
			// && (this.cm == ((Experiment) obj).cm)
			// && (this.dm == ((Experiment) obj).dm)
			// && (this.data == ((Experiment) obj).data)
			// && (this.log == ((Experiment) obj).log)
			// && (this.ranks == ((Experiment) obj).ranks)
			// && (this.outlier == ((Experiment) obj).outlier);
			return false;
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public String toString() {
			return name + "\t " + ml + "\t" + sl + "\t" + as + "\t" + cl + "\t" + cm + "\t" + dm + "\t" + log + "\t"
					+ ranks + "\t" + outlier + "\t" + data;
		}
	}
	
	enum mllearner {
		ClassifierChain, MultiLabelStacking, MLkNN
	}
	
	enum sllearner {
		SMO, IBK, RF
	}
	
	private static Step getBest(final Experiment exp, final int measure) {
		Step best = null;
		for (final Double key : exp.steps.keySet())
			if (best == null || best.getMeasure(measure) <= exp.steps.get(key).getMeasure(measure))
				best = exp.steps.get(key);
		return best;
	}
	
	private static void getBestN(final Cube cube, final int n, final int measure) {
		
		for (final dataset data : dataset.values()) {
			System.out.println(data);
			// for (mllearner learner : mllearner.values()) {
			final mllearner learner = mllearner.ClassifierChain;
			final SortedSet<Step> sortedEntries = new TreeSet<>(new Comparator<Step>() {
				@Override
				public int compare(final Step e1, final Step e2) {
					return Double.valueOf(e1.getMeasure(measure)).compareTo(Double.valueOf(e2.getMeasure(measure)));
				}
			});
			for (final Experiment exp : cube.experiments)
				if (exp.ml == learner && exp.data == data && exp.dm != distancemeasure.MinkowskiDistance) {
					final Step bestStep = Cube.getBest(exp, measure);
					sortedEntries.add(bestStep);
				}
			if (!sortedEntries.isEmpty()) for (int i = 0; i < n; i++) {
				System.out.println("place " + (i + 1) + " " + sortedEntries.first());
				sortedEntries.remove(sortedEntries.first());
			}
			System.out.println();
			// }
		}
	}
	
	/**
	 * @param args
	 *            command line arguments
	 * @throws IOException
	 *             read exception
	 */
	public static void main(final String[] args) throws IOException {
		final Cube cube = new Cube();
		cube.readFile("./results/field/archiv/010911/fcml_results");
		System.out.println("all read");
		Cube.getBestN(cube, 10, 0);
		// for (Experiment experiment : cube.experiments) {
		// if (experiment.ml.equals(mllearner.ClassifierChain)
		// && experiment.cl.equals(clusterer.HierarchicalClusterer)
		// && experiment.dm.equals(distancemeasure.ChebyshevDistance)
		// && experiment.cm.equals(clustermethod.COMPLETE)
		// && (experiment.log == false)) {
		// System.out.println(experiment);
		// computeStats(experiment);
		// }
		// }
	}
	
	LinkedList<Experiment>	experiments	= new LinkedList<>();
	
	private void add(final Experiment experiment) {
		final int index = experiments.indexOf(experiment);
		if (index != -1) {
			final Experiment origExp = experiments.get(index);
			origExp.steps.putAll(experiment.steps);
		} else experiments.add(experiment);
	}
	
	@SuppressWarnings ("resource")
	void readFile(final String filename) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
		reader.readLine();
		String line = reader.readLine();
		// XXX
		
		int id = 0;
		
		while (line != null) {
			final String[] exp = line.split("\t");
			// XXX
			final Experiment expi =
					new Experiment("" + id++, mllearner.valueOf(exp[1]), sllearner.valueOf(exp[5]),
							aseval.valueOf(exp[7]), clusterer.valueOf(exp[8]), clustermethod.valueOf(exp[10]),
							dataset.valueOf(exp[6]), distancemeasure.valueOf(exp[9]), Double.valueOf(exp[13]),
							Double.valueOf(exp[14]), Double.valueOf(exp[15]), Double.valueOf(exp[16]),
							Double.valueOf(exp[17]), Double.valueOf(exp[18]), Double.valueOf(exp[19]),
							Double.valueOf(exp[20]), Double.valueOf(exp[21]), Double.valueOf(exp[22]),
							Double.valueOf(exp[23]), Double.valueOf(exp[24]), Double.valueOf(exp[25]),
							Double.valueOf(exp[26]), Double.valueOf(exp[27]), Double.valueOf(exp[28]),
							Double.valueOf(exp[29]), Boolean.valueOf(exp[2]), Boolean.valueOf(exp[3]),
							Boolean.valueOf(exp[4]));
			add(expi);
			line = reader.readLine();
		}
	}
}
