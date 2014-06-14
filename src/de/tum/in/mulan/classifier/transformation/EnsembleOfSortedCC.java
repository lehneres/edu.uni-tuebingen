package de.tum.in.mulan.classifier.transformation;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemovePercentage;
import de.tum.in.mulan.classifier.transformation.evaluation.ExtendedMultipleEvaluation;
import de.tum.in.mulan.classifier.transformation.evaluation.SortedCCEvaluator;

/**
 * ensembles of SortedClassifierChains. Every model starts with a different random seed and a different subset of data
 * (67% of the full data)
 * 
 * @author LehnereS
 */
public class EnsembleOfSortedCC extends TransformationBasedMultiLabelLearner {
	
	/**
	 * will read options from the command line and starting a crossvalidation
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception
	 *             some exception
	 */
	public static void main(final String[] args) throws Exception {
		System.err.println("reading commandline options...");
		final String[] initalOptions = args.clone();
		if (args.length == 0)
			throw new Exception(
					"-t path/to/arff -xml path/to/xml -B 'options for BayesNet' -C 'options for baseclassifier'");
		final String arffFilename = Utils.getOption('t', args);
		final String xmlFilename = Utils.getOption("xml", args);
		final double numFolds = Double.parseDouble(Utils.getOption("folds", args));
		final MultiLabelInstances dataset = new MultiLabelInstances(arffFilename, xmlFilename);
		final String[] baseClassifierOptions = Utils.getOption('C', args).split(" ");
		final String classifierName = Utils.getOption('W', baseClassifierOptions);
		final Classifier baseClassifier = AbstractClassifier.forName(classifierName, baseClassifierOptions);
		final BayesNet bayesNet = new BayesNet();
		bayesNet.setOptions(Utils.getOption('B', args).split(" "));
		final SortedCC sortedCC = new SortedCC(baseClassifier, bayesNet);
		sortedCC.setSortMethod(SortedCC.parseSortMethod(args));
		sortedCC.setDontUseClassLabel(Utils.getFlag("once", args));
		sortedCC.setDebug(Utils.getFlag('D', args));
		sortedCC.setStacking(Utils.getFlag("sCV", args));
		if (sortedCC.getDoStacking()) {
			final String sCVFolds = Utils.getOption("sCVFolds", args);
			if (sCVFolds.length() > 0) sortedCC.setStackingFolds(Integer.parseInt(sCVFolds));
		}
		final EnsembleOfSortedCC ensemble = new EnsembleOfSortedCC(sortedCC, baseClassifier, 10);
		ensemble.setDebug(sortedCC.getDebug());
		ensemble.useConfidences = true;
		final SortedCCEvaluator eval = new SortedCCEvaluator("EnsembleSortedCC", initalOptions, dataset.getNumLabels());
		ensemble.debug("using dataset: " + arffFilename);
		ensemble.debug("using classifier: " + classifierName);
		ensemble.debug("using sortmethod: " + sortedCC.getSortMethod());
		ensemble.debug("using stacking: " + sortedCC.getDoStacking());
		ensemble.debug("using once: " + sortedCC.getDontUseClassLabel());
		ensemble.debug("using search algorithm: " + sortedCC.getSearchAlgorithmName());
		ensemble.debug("reading commandline options...done");
		ensemble.debug("starting crossvalidation...");
		final ExtendedMultipleEvaluation results = eval.holdOut(ensemble, dataset, numFolds);
		eval.write(results, Utils.getOption("output", args), null);
		System.err.println("crossvalidation done (" + new Date() + ")");
	}
	
	private final int		numOfModels;
	private final double	percentage		= 67;
	private final Random	rand;
	private final long		randomSeed		= 666;
	private boolean			useConfidences	= true;
	protected SortedCC[]	ensemble;
	
	/**
	 * initializes the ensemble by setting aNumOfModels sortedCC classifier with different random seeds
	 * 
	 * @param sortedCC
	 *            a sortedCC classifier
	 * @param classifier
	 *            a weka classifier
	 * @param aNumOfModels
	 *            number of models
	 * @throws Exception
	 *             any exception
	 */
	public EnsembleOfSortedCC(final SortedCC sortedCC, final Classifier classifier, final int aNumOfModels)
			throws Exception {
		super(classifier);
		numOfModels = aNumOfModels;
		ensemble = new SortedCC[aNumOfModels];
		rand = new Random(randomSeed);
		for (int i = 0; i < numOfModels; i++) {
			ensemble[i] = (SortedCC) sortedCC.makeCopy();
			final SearchAlgorithm searchAlgorithm = ensemble[i].getSearchAlgorithm();
			// setting seeds to random number in different types of
			// weka.classifiers.bayes.net.search.SearchAlgorithm {global, local}
			if (searchAlgorithm instanceof weka.classifiers.bayes.net.search.global.SimulatedAnnealing) ((weka.classifiers.bayes.net.search.global.SimulatedAnnealing) searchAlgorithm)
					.setSeed(new Random().nextInt());
			else if (searchAlgorithm instanceof weka.classifiers.bayes.net.search.global.RepeatedHillClimber) ((weka.classifiers.bayes.net.search.global.RepeatedHillClimber) searchAlgorithm)
					.setSeed(new Random().nextInt());
			else if (searchAlgorithm instanceof weka.classifiers.bayes.net.search.local.SimulatedAnnealing) ((weka.classifiers.bayes.net.search.local.SimulatedAnnealing) searchAlgorithm)
					.setSeed(new Random().nextInt());
			else if (searchAlgorithm instanceof weka.classifiers.bayes.net.search.local.RepeatedHillClimber)
				((weka.classifiers.bayes.net.search.local.RepeatedHillClimber) searchAlgorithm).setSeed(new Random()
						.nextInt());
		}
	}
	
	@Override
	protected void buildInternal(final MultiLabelInstances trainingSet) throws Exception {
		final Instances dataSet = new Instances(trainingSet.getDataSet());
		// building each model on 67% of the data
		for (int i = 0; i < numOfModels; i++) {
			dataSet.randomize(rand);
			final RemovePercentage rmvp = new RemovePercentage();
			rmvp.setInvertSelection(true);
			rmvp.setPercentage(percentage);
			rmvp.setInputFormat(dataSet);
			final Instances trainDataSet = Filter.useFilter(dataSet, rmvp);
			final MultiLabelInstances train = new MultiLabelInstances(trainDataSet, trainingSet.getLabelsMetaData());
			ensemble[i].build(train);
		}
	}
	
	@Override
	protected MultiLabelOutput makePredictionInternal(final Instance instance) throws Exception, InvalidDataException {
		final int[] sumVotes = new int[numLabels];
		final double[] sumConf = new double[numLabels];
		Arrays.fill(sumVotes, 0);
		Arrays.fill(sumConf, 0);
		// sums up votes for every model
		for (int i = 0; i < numOfModels; i++) {
			final MultiLabelOutput currentModel = ensemble[i].makePrediction(instance);
			final boolean[] bip = currentModel.getBipartition();
			final double[] conf = currentModel.getConfidences();
			for (int j = 0; j < numLabels; j++) {
				sumVotes[j] += bip[j] ? 1 : 0;
				sumConf[j] += conf[j];
			}
		}
		// confidence is sum of all votes / number of voters
		final double[] confidence = new double[numLabels];
		for (int j = 0; j < numLabels; j++)
			if (useConfidences) confidence[j] = sumConf[j] / numOfModels;
			else confidence[j] = sumVotes[j] / (double) numOfModels;
		final MultiLabelOutput mlo = new MultiLabelOutput(confidence, 0.5);
		return mlo;
	}
}
