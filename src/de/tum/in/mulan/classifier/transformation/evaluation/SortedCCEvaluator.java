package de.tum.in.mulan.classifier.transformation.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.Evaluator;
import mulan.evaluation.measure.AveragePrecision;
import mulan.evaluation.measure.Coverage;
import mulan.evaluation.measure.ErrorSetSize;
import mulan.evaluation.measure.ExampleBasedAccuracy;
import mulan.evaluation.measure.ExampleBasedFMeasure;
import mulan.evaluation.measure.ExampleBasedPrecision;
import mulan.evaluation.measure.ExampleBasedRecall;
import mulan.evaluation.measure.HammingLoss;
import mulan.evaluation.measure.IsError;
import mulan.evaluation.measure.MacroAUC;
import mulan.evaluation.measure.MacroFMeasure;
import mulan.evaluation.measure.MacroPrecision;
import mulan.evaluation.measure.MacroRecall;
import mulan.evaluation.measure.MeanAveragePrecision;
import mulan.evaluation.measure.Measure;
import mulan.evaluation.measure.MicroAUC;
import mulan.evaluation.measure.MicroFMeasure;
import mulan.evaluation.measure.MicroPrecision;
import mulan.evaluation.measure.MicroRecall;
import mulan.evaluation.measure.OneError;
import mulan.evaluation.measure.RankingLoss;
import weka.core.Instances;
import weka.core.Utils;
import de.tum.in.mulan.classifier.transformation.SortedCC;

/**
 * @author LehnereS
 */
public class SortedCCEvaluator {
	
	private final List<Measure>	measures;
	private final int			seed	= 666;
	String[]					classifierOptions;
	String						method;
	
	/**
	 * @param method
	 *            name of the method
	 * @param initalOptions
	 *            initial options of the classifier
	 * @param numOfLabels
	 *            number of labels
	 */
	public SortedCCEvaluator(final String method, final String[] initalOptions, final int numOfLabels) {
		this.method = method;
		classifierOptions = initalOptions;
		measures = new ArrayList<>();
		measures.add(new HammingLoss());
		measures.add(new ExampleBasedPrecision(false));
		measures.add(new ExampleBasedRecall(false));
		measures.add(new ExampleBasedFMeasure(false));
		measures.add(new ExampleBasedAccuracy(false));
		measures.add(new MicroPrecision(numOfLabels));
		measures.add(new MicroRecall(numOfLabels));
		measures.add(new MicroFMeasure(numOfLabels));
		measures.add(new MacroPrecision(numOfLabels, false));
		measures.add(new MacroRecall(numOfLabels, false));
		measures.add(new MacroFMeasure(numOfLabels, false));
		measures.add(new OneError());
		measures.add(new AveragePrecision());
		measures.add(new IsError());
		measures.add(new ErrorSetSize());
		measures.add(new Coverage());
		measures.add(new RankingLoss());
		measures.add(new MeanAveragePrecision(numOfLabels));
		measures.add(new MicroAUC(numOfLabels));
		measures.add(new MacroAUC(numOfLabels));
	}
	
	/**
	 * @param classifier
	 *            the classifier extending TransformationBasedMultiLabelLearner
	 * @param data
	 *            the multilabel data
	 * @param in_numFolds
	 *            the number of folds
	 * @return ExtendendMulitpleEvaluation
	 * @throws Exception
	 *             any exception
	 */
	public ExtendedMultipleEvaluation crossValidate(final TransformationBasedMultiLabelLearner classifier,
			final MultiLabelInstances data, final int in_numFolds) throws Exception {
		final Instances workingSet = new Instances(data.getDataSet());
		int numFolds;
		if (in_numFolds < 0) numFolds = workingSet.numInstances();
		else numFolds = in_numFolds;
		final Evaluation[] evaluation = new Evaluation[numFolds];
		final Random random = new Random(seed);
		workingSet.randomize(random);
		for (int i = 0; i < numFolds; i++) {
			System.err.println("\ndoing fold " + i + "/" + numFolds + "...");
			final Instances train = workingSet.trainCV(numFolds, i, random);
			final Instances test = workingSet.testCV(numFolds, i);
			final MultiLabelInstances mlTrain = new MultiLabelInstances(train, data.getLabelsMetaData());
			final MultiLabelInstances mlTest = new MultiLabelInstances(test, data.getLabelsMetaData());
			final MultiLabelLearner clone = classifier.makeCopy();
			System.err.println("building classifier...");
			clone.build(mlTrain);
			System.err.println("building classifier...done\nevaluating...");
			final Evaluator eval = new Evaluator();
			eval.setStrict(false);
			evaluation[i] = eval.evaluate(clone, mlTest, measures);
			System.err.println("evaluating...done\ndoing fold " + i + "/" + numFolds + "...done");
		}
		return new ExtendedMultipleEvaluation(evaluation);
	}
	
	/**
	 * @param classifier
	 *            the classifier extending TransformationBasedMultiLabelLearner
	 * @param data
	 *            the multilabel data
	 * @param in_percentage
	 *            the number of folds
	 * @return ExtendendMulitpleEvaluation
	 * @throws Exception
	 *             any exception
	 */
	public ExtendedMultipleEvaluation holdOut(final TransformationBasedMultiLabelLearner classifier,
			final MultiLabelInstances data, final double in_percentage) throws Exception {
		final Instances workingSet = new Instances(data.getDataSet());
		double percentage;
		if (in_percentage < 0) percentage = 0.33;
		else percentage = in_percentage;
		final Evaluation[] evaluation = new Evaluation[1];
		final Random random = new Random(seed);
		workingSet.randomize(random);
		final Instances train = workingSet.trainCV((int) (1 / percentage), 1, random);
		final Instances test = workingSet.testCV((int) (1 / percentage), 1);
		final MultiLabelInstances mlTrain = new MultiLabelInstances(train, data.getLabelsMetaData());
		final MultiLabelInstances mlTest = new MultiLabelInstances(test, data.getLabelsMetaData());
		// final MultiLabelLearner clone = classifier.makeCopy();
		final MultiLabelLearner clone = classifier;
		System.err.println("building classifier...");
		classifier.build(mlTrain);
		System.err.println("building classifier...done\nevaluating...");
		final Evaluator eval = new Evaluator();
		eval.setStrict(false);
		evaluation[0] = eval.evaluate(clone, mlTest, measures);
		return new ExtendedMultipleEvaluation(evaluation);
	}
	
	/**
	 * @param results
	 *            some results
	 * @param fileName
	 *            output file name
	 * @param labelIndices
	 *            the label indices
	 * @throws Exception
	 *             any exception
	 */
	public void write(final ExtendedMultipleEvaluation results, final String fileName, final int[] labelIndices)
			throws Exception {
		final String id = Utils.getOption("id", classifierOptions);
		final StringBuilder sb = new StringBuilder();
		sb.append(id + "\t");
		sb.append("method=" + method + "\t");
		sb.append("data=" + Utils.getOption("t", classifierOptions) + "\t");
		sb.append("bayseNet=" + Utils.getOption("B", classifierOptions) + "\t");
		sb.append("baseClassifierType=" + Utils.getOption("C", classifierOptions) + "\t");
		sb.append("sCV=" + Utils.getFlag("sCV", classifierOptions) + "\t");
		sb.append("once=" + Utils.getFlag("once", classifierOptions) + "\t");
		sb.append("sortMethod=" + SortedCC.parseSortMethod(classifierOptions) + "\t");
		for (final Measure m : results.evaluations.get(0).getMeasures()) {
			final String measureName = m.getName();
			sb.append(measureName);
			sb.append("=");
			sb.append(String.format("%.4f", results.getMean(measureName)));
			sb.append("\u00B1");
			sb.append(String.format("%.4f", results.standardDeviation.get(measureName)));
			sb.append("\t");
		}
		if (labelIndices != null) {
			sb.append("label order=");
			for (final int i : labelIndices)
				sb.append(i + " ");
		}
		System.out.println(sb.toString());
		// final File file = new File(fileName);
		// if (!file.exists()) {
		// file.createNewFile();
		// }
		// do {
		// Thread.sleep(100);
		// } while (NobleEvaluator.isLocked(file));
		// final FileLock lock = new RandomAccessFile(file, "rw").getChannel()
		// .lock();
		// final BufferedWriter writer = new BufferedWriter(new FileWriter(file,
		// true));
		// writer.append(sb.toString() + "\n");
		// writer.flush();
		// writer.close();
		// lock.release();
	}
}