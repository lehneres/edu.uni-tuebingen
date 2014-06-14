package de.tum.in.mulan.classifier.transformation.evaluation;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mulan.classifier.InvalidDataException;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Reorder;
import de.tum.in.mulan.classifier.meta.AbstractNoble;
import de.tum.in.mulan.classifier.transformation.Noble;

/**
 * @author LehnereS
 */
@SuppressWarnings ("deprecation")
public class NobleEvaluator {
	
	protected class EvalThreaded implements Runnable {
		public double					acc;
		public int						k;
		public double					t;
		private final Instances			centertest;
		private final Instances			centertrain;
		private final Noble				classifier;
		private final int				folds;
		private final int				id;
		
		private final Instances			left;
		private final NobleEvaluator	noble;
		private final Instances			top;
		
		boolean							transpose;
		
		public EvalThreaded(final NobleEvaluator noble, final double t, final int k, final Noble classifier,
				final Instances left, final Instances top, final Instances centertrain, final Instances centertest,
				final int folds, final boolean transpose, final int id) {
			this.classifier = classifier;
			this.left = left;
			this.top = top;
			this.centertrain = centertrain;
			this.centertest = centertest;
			this.folds = folds;
			this.transpose = transpose;
			this.k = k;
			this.t = t;
			this.noble = noble;
			acc = -1;
			this.id = id;
		}
		
		@Override
		public void run() {
			try {
				System.err.println("Testing k = " + k + " and t = " + t + " and transp = " + transpose + " ...");
				final Noble innerclassifier = (Noble) classifier.makeCopy();
				innerclassifier.setId(id);
				innerclassifier.setK(k);
				innerclassifier.setT(t);
				innerclassifier.setTranspose(transpose);
				
				noble.nobleCV(innerclassifier, left, top, centertrain, centertest, folds);
				acc = NobleEvaluator.evalACC(noble.getThresholdInstance(), noble.getResult());
				
				System.err.println("Got k = " + k + " and t = " + t + " and transp = " + transpose + " => Acc: " + acc);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private static double	threshold	= .5;
	
	/**
	 * computing the accuracy
	 * 
	 * @param instance
	 *            the instance representing the threshold
	 * @param measures
	 *            the measures
	 * @return (TP+TN)/(TP+TN+FP+FN)
	 */
	static double evalACC(final Instance instance, final Instances measures) {
		return (instance.value(measures.attribute(ThresholdCurve.TRUE_POS_NAME).index()) + instance.value(measures
				.attribute(ThresholdCurve.TRUE_NEG_NAME).index()))
				/ (instance.value(measures.attribute(ThresholdCurve.TRUE_POS_NAME).index())
						+ instance.value(measures.attribute(ThresholdCurve.TRUE_NEG_NAME).index())
						+ instance.value(measures.attribute(ThresholdCurve.FALSE_NEG_NAME).index()) + instance
							.value(measures.attribute(ThresholdCurve.FALSE_POS_NAME).index()));
	}
	
	private static Instances getFilteredFromLeft(final Instances toFilter, final int[] indices) throws Exception {
		final Instances result = new Instances(toFilter, indices.length);
		
		for (final int index : indices) {
			final Instance tmp = new DenseInstance(1.0, toFilter.get(index).toDoubleArray());
			if (!toFilter.checkInstance(tmp)) throw new Exception("instance not compatible");
			result.add(tmp);
		}
		return result;
	}
	
	private static Instances getFilteredFromTop(final Instances toFilter, final int[] indices) throws Exception {
		final Remove remove = new Remove();
		remove.setAttributeIndicesArray(indices);
		remove.setInvertSelection(true);
		remove.setInputFormat(toFilter);
		return Filter.useFilter(toFilter, remove);
		
	}
	
	/**
	 * @param file
	 *            a file object
	 * @return true if the file is locked
	 * @throws IOException
	 *             some io exception
	 */
	@SuppressWarnings ("resource")
	public static boolean isLocked(final File file) throws IOException {
		try {
			final FileLock lock = new RandomAccessFile(file, "rw").getChannel().tryLock();
			lock.release(); // ignore ClosedChannelException
		} catch (final java.nio.channels.OverlappingFileLockException e) {
			return true;
		}
		return false;
	}
	
	private static Instances reinitialize(final Instances dataset) throws Exception {
		final ArffSaver saver = new ArffSaver();
		saver.setInstances(dataset);
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		saver.setDestination(output);
		// final String tmpFile = "/tmp/re" + System.currentTimeMillis() +
		// ".arff";
		// AbstractNoble.debug(tmpFile);
		// saver.setFile(new File(tmpFile));
		saver.writeBatch();
		final ArffLoader loader = new ArffLoader();
		loader.setSource(new ByteArrayInputStream(output.toByteArray()));
		
		// DataSource loader = new DataSource(tmpFile);
		
		return loader.getDataSet();
	}
	
	/**
	 * Returns the indices of the test set for one fold of a cross-validation on the dataset.
	 * 
	 * @param numFolds
	 *            the number of folds in the cross-validation. Must be greater than 1.
	 * @param numFold
	 *            0 for the first fold, 1 for the second, ...
	 * @param instances
	 *            the complete dataset
	 * @return the training set
	 * @throws IllegalArgumentException
	 *             if the number of folds is less than 2 or greater than the number of instances.
	 */
	// @ requires 2 <= numFolds && numFolds < numInstances();
	// @ requires 0 <= numFold && numFold < numFolds;
	public static int[] testCVIndices(final int numFolds, final int numFold, final Instances instances) {
		int numInstForFold, first, offset;
		if (numFolds < 2) throw new IllegalArgumentException("Number of folds must be at least 2!");
		if (numFolds > instances.numInstances())
			throw new IllegalArgumentException("Can't have more folds than instances!");
		numInstForFold = instances.numInstances() / numFolds;
		if (numFold < instances.numInstances() % numFolds) {
			numInstForFold++;
			offset = numFold;
		} else offset = instances.numInstances() % numFolds;
		final int[] indices = new int[numInstForFold];
		first = numFold * (instances.numInstances() / numFolds) + offset;
		int pointer = 0;
		for (int i = first; i < first + numInstForFold; i++)
			indices[pointer++] = i;
		return indices;
	}
	
	/**
	 * Returns the indices of the training set for one fold of a cross-validation on the dataset.
	 * 
	 * @param numFolds
	 *            the number of folds in the cross-validation. Must be greater than 1.
	 * @param numFold
	 *            0 for the first fold, 1 for the second, ...
	 * @param instances
	 *            the complete dataset
	 * @return the training set
	 * @throws IllegalArgumentException
	 *             if the number of folds is less than 2 or greater than the number of instances.
	 */
	// @ requires 2 <= numFolds && numFolds < numInstances();
	// @ requires 0 <= numFold && numFold < numFolds;
	public static int[] trainCVIndices(final int numFolds, final int numFold, final Instances instances) {
		int numInstForFold, first, offset;
		if (numFolds < 2) throw new IllegalArgumentException("Number of folds must be at least 2!");
		if (numFolds > instances.numInstances())
			throw new IllegalArgumentException("Can't have more folds than instances!");
		numInstForFold = instances.numInstances() / numFolds;
		if (numFold < instances.numInstances() % numFolds) {
			numInstForFold++;
			offset = numFold;
		} else offset = instances.numInstances() % numFolds;
		final int[] indices = new int[instances.numInstances() - numInstForFold];
		first = numFold * (instances.numInstances() / numFolds) + offset;
		int pointer = 0;
		for (int i = 0; i < first; i++)
			indices[pointer++] = i;
		for (int i = first + numInstForFold; i < instances.numInstances(); i++)
			indices[pointer++] = i;
		
		return indices;
	}
	
	private final String[]	classifierOptions;
	private final boolean	holdOut	= false;
	private int[]			karray;
	private int[]			karraytransp;
	private boolean			opt		= false;
	private Instances		result;
	private final long		seed	= 666;
	private double[]		tarray;
	private final boolean	useBipartion;
	
	/**
	 * saving initialOptions for output
	 * 
	 * @param initalOptions
	 *            initial options
	 * @param useBipartion
	 *            if to use bipartitions
	 */
	public NobleEvaluator(final String[] initalOptions, final boolean useBipartion) {
		classifierOptions = initalOptions;
		this.useBipartion = useBipartion;
	}
	
	/**
	 * @return the result
	 */
	public Instances getResult() {
		return result;
	}
	
	Instance getThresholdInstance() {
		Instance myInstance = null;
		final int thresholdIndex = getResult().attribute(ThresholdCurve.THRESHOLD_NAME).index();
		for (final Instance instance : getResult())
			if (instance.value(thresholdIndex) >= NobleEvaluator.threshold) {
				myInstance = instance;
				break;
			}
		if (myInstance == null) {
			AbstractNoble.debug("no threshold greater than " + "0.5 found, using highest threshold.");
			double currentThreshold = 0;
			for (final Instance instance : getResult())
				if (instance.value(thresholdIndex) >= currentThreshold) {
					currentThreshold = instance.value(thresholdIndex);
					myInstance = instance;
				}
		}
		return myInstance;
	}
	
	private NobleEvaluator makeCopy() {
		@SuppressWarnings ("hiding")
		final NobleEvaluator result = new NobleEvaluator(classifierOptions, useBipartion);
		
		result.setResult(getResult());
		result.opt = opt;
		result.tarray = tarray;
		result.karray = karray;
		result.karraytransp = karraytransp;
		
		return result;
	}
	
	/**
	 * evaluator for noble classifier
	 * 
	 * @param classifier
	 *            the classifier
	 * @param left
	 *            left matrix
	 * @param top
	 *            top matrix
	 * @param centerTrain
	 *            center training matrix
	 * @param centerTest
	 *            center testing matrix
	 * @param folds
	 *            the number of folds for cross-validation
	 * @throws InvalidDataException
	 *             if data is corrupt
	 * @throws Exception
	 *             any exception
	 */
	public void nobleCV(AbstractNoble classifier, Instances left, Instances top, Instances centerTrain,
			Instances centerTest, final int folds) throws InvalidDataException, Exception {
		if (opt) classifier = optPar((Noble) classifier, left, top, centerTrain, centerTest, 2);
		
		final AbstractNoble backupClassifier = classifier;
		
		int allPrediction = 0, overall = 0;
		// vector to save the predictions
		final FastVector<NominalPrediction> predictions = new FastVector<>();
		// calculating folds, if folds less than 0 the number of
		// instances is taken (= leave-one-out)
		final int leftFolds = folds < 0 ? left.numInstances() : folds;
		final int topFolds = folds < 0 ? top.numInstances() : folds;
		
		final Instances[] randomized = randomize(left, top, centerTrain, centerTest);
		
		left = randomized[0];
		top = randomized[1];
		centerTrain = randomized[2];
		centerTest = randomized[3];
		
		// doing the left folds
		for (int leftFold = 0; leftFold < (holdOut ? 1 : leftFolds); leftFold++) {
			
			final int[] leftTrainingSetIndices = NobleEvaluator.trainCVIndices(leftFolds, leftFold, left);
			
			final Instances leftTrainingSet = NobleEvaluator.getFilteredFromLeft(left, leftTrainingSetIndices);
			
			final Instances leftTrainingCenter =
					NobleEvaluator.getFilteredFromLeft(centerTrain, leftTrainingSetIndices);
			
			final int[] leftTestingSetIndices = NobleEvaluator.testCVIndices(leftFolds, leftFold, left);
			
			final Instances leftTestingSet = NobleEvaluator.getFilteredFromLeft(left, leftTestingSetIndices);
			
			final Instances leftTestingCenter =
					NobleEvaluator.getFilteredFromLeft(centerTest == null ? centerTrain : centerTest,
							leftTestingSetIndices);
			
			// doing the top folds
			for (int topFold = 0; topFold < (holdOut ? 1 : topFolds); topFold++) {
				
				final int[] topTrainingSetIndices = NobleEvaluator.trainCVIndices(topFolds, topFold, top);
				
				final Instances topTrainingSet = NobleEvaluator.getFilteredFromLeft(top, topTrainingSetIndices);
				
				// FIXME made a little shortcut here
				final Instances trainingCenter =
						NobleEvaluator.getFilteredFromTop(leftTrainingCenter, topTrainingSetIndices);
				
				final Instances workingTrainingLeft = NobleEvaluator.reinitialize(leftTrainingSet);
				final Instances workingTrainingTop = NobleEvaluator.reinitialize(topTrainingSet);
				final Instances workingTrainingCenter = NobleEvaluator.reinitialize(trainingCenter);
				
				AbstractNoble.debug("building " + (leftFold + 1) + "/" + (topFold + 1) + " of " + leftFolds + "/"
						+ topFolds + "...");
				AbstractNoble.debug("Training Left: " + workingTrainingLeft.numInstances() + " x "
						+ workingTrainingLeft.numAttributes());
				AbstractNoble.debug("Training Top: " + workingTrainingTop.numInstances() + " x "
						+ workingTrainingTop.numAttributes());
				AbstractNoble.debug("Training Center: " + workingTrainingCenter.numInstances() + " x "
						+ workingTrainingCenter.numAttributes());
				
				classifier = backupClassifier.makeCopy();
				classifier.build(workingTrainingLeft, workingTrainingTop, workingTrainingCenter);
				
				AbstractNoble.debug("building " + (leftFold + 1) + "/" + (topFold + 1) + " of " + leftFolds + "/"
						+ topFolds + "...done");
				AbstractNoble.debug("predicting " + (leftFold + 1) + "/" + (topFold + 1) + " of " + leftFolds + "/"
						+ topFolds + "...");
				
				final int[] topTestingSetIndices = NobleEvaluator.testCVIndices(topFolds, topFold, top);
				
				final Instances topTestingSet = NobleEvaluator.getFilteredFromLeft(top, topTestingSetIndices);
				
				// removing the other attributes
				final Instances testingCenter =
						NobleEvaluator.getFilteredFromTop(leftTestingCenter, topTestingSetIndices);
				
				final Instances workingTestingLeft = NobleEvaluator.reinitialize(leftTestingSet);
				final Instances workingTestingTop = NobleEvaluator.reinitialize(topTestingSet);
				final Instances workingTestingCenter = NobleEvaluator.reinitialize(testingCenter);
				
				AbstractNoble.debug("Testing Left: " + workingTestingLeft.numInstances() + " x "
						+ workingTestingLeft.numAttributes());
				
				AbstractNoble.debug("Testing Top: " + workingTestingTop.numInstances() + " x "
						+ workingTestingTop.numAttributes());
				AbstractNoble.debug("Testing Center: " + workingTestingCenter.numInstances() + " x "
						+ workingTestingCenter.numAttributes());
				
				int predictionCount = 0;
				
				for (int leftInstanceIndex = 0; leftInstanceIndex < leftTestingSet.numInstances(); leftInstanceIndex++)
					for (int topInstanceIndex = 0; topInstanceIndex < topTestingSet.numInstances(); topInstanceIndex++) {
						// System.out.println(leftInstanceIndex + " " +
						// topInstanceIndex);
						overall++;
						if (!testingCenter.get(leftInstanceIndex).isMissing(topInstanceIndex)) {
							predictionCount++;
							final double prediction;
							if (useBipartion) prediction =
									classifier.classifyInstances(workingTestingLeft.get(leftInstanceIndex),
											workingTestingTop.get(topInstanceIndex));
							else prediction =
									classifier.distributionForInstances(workingTestingLeft.get(leftInstanceIndex),
											workingTestingTop.get(topInstanceIndex));
							final double[] distribution = new double[2];
							distribution[0] = 1 - prediction;
							distribution[1] = prediction;
							predictions.add(new NominalPrediction(workingTestingCenter.get(leftInstanceIndex).value(
									topInstanceIndex), distribution));
						}
					}
				AbstractNoble.debug(leftTestingSet.numInstances() + " " + topTestingSet.numInstances());
				AbstractNoble.debug("(" + predictionCount + ") done");
				allPrediction += predictionCount;
				
			}
		}
		AbstractNoble.debug("done " + allPrediction + "|" + overall + " predictions");
		setResult(new ThresholdCurve().getCurve(predictions));
	}
	
	/**
	 * evaluator for noble classifier
	 * 
	 * @param classifier
	 *            the classifier
	 * @param left
	 *            left matrix
	 * @param top
	 *            top matrix
	 * @param center
	 *            center matrix
	 * @param folds
	 *            the number of folds for cross-validation
	 * @throws InvalidDataException
	 *             if data is corrupt
	 * @throws Exception
	 *             any exception
	 */
	public void nobleCV(final AbstractNoble classifier, final Instances left, final Instances top,
			final Instances center, final int folds) throws InvalidDataException, Exception {
		this.nobleCV(classifier, left, top, center, null, folds);
	}
	
	/**
	 * @param classifier
	 * @param left
	 * @param top
	 * @param centertrain
	 * @param centertest
	 * @param folds
	 * @return classifier with optimal t and k parameter
	 * @throws Exception
	 */
	public Noble optPar(final Noble classifier, final Instances left, final Instances top, final Instances centertrain,
			final Instances centertest, final int folds) throws Exception {
		
		opt = false;
		
		int bestk = -1;
		double bestt = -1.0;
		double bestacc = -1.0;
		
		final Vector<EvalThreaded> threads = new Vector<>();
		final ExecutorService execSvc = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		int count = 0;
		
		for (final boolean transpose : new boolean[] { true, false }) {
			final int[] workkarr = transpose ? karraytransp : karray;
			for (final double t : tarray)
				for (final int k : workkarr) {
					
					final EvalThreaded threadob =
							new EvalThreaded(makeCopy(), t, k, classifier, left, top, centertrain, centertest, folds,
									transpose, count);
					execSvc.execute(threadob);
					count++;
					threads.add(threadob);
					
				}
		}
		System.err.println(count + " threads startet");
		execSvc.awaitTermination(1, TimeUnit.DAYS);
		for (final EvalThreaded t : threads) {
			if (t == null) continue;
			
			if (t.acc > bestacc || t.acc == bestacc && t.k < bestk) {
				bestk = t.k;
				bestt = t.t;
				bestacc = t.acc;
			}
		}
		classifier.setK(bestk);
		classifier.setT(bestt);
		
		opt = true;
		System.err.println("\n\n\n\n#####################################################################\n");
		System.err.println("Found optimal k = " + bestk + "\n Found optimal t = " + bestt);
		System.err.println("Found best acc = " + bestacc);
		System.err.println("\n#####################################################################\n\n\n\n");
		
		return classifier;
	}
	
	private Instances[] randomize(final Instances left, final Instances top, final Instances centerTrain,
			final Instances centerTest) throws Exception {
		// start with left, instances must be in same order as instances in
		// center
		List<Integer> indices = new ArrayList<>(left.numInstances());
		for (int i = 0; i < left.numInstances(); i++)
			indices.add(i);
		Collections.shuffle(indices, new Random(seed));
		int[] randomIndices =
				de.tum.in.multilabel.Utils.convertIntegerArray(indices.toArray(new Integer[indices.size()]));
		final Instances newLeft = new Instances(left, left.numInstances());
		Instances newCenterTrain = new Instances(centerTrain, centerTrain.numInstances());
		Instances newCenterTest = null;
		if (centerTest != null) newCenterTest = new Instances(centerTest, centerTest.numInstances());
		for (final int randomIndex : randomIndices) {
			newLeft.add(left.get(randomIndex));
			newCenterTrain.add(centerTrain.get(randomIndex));
			if (centerTest != null && newCenterTest != null) newCenterTest.add(centerTest.get(randomIndex));
		}
		
		// okay and now top and center, instances must be in same order as
		// attributes in top
		indices = new ArrayList<>(top.numInstances());
		for (int i = 0; i < top.numInstances(); i++)
			indices.add(i);
		Collections.shuffle(indices, new Random(seed));
		randomIndices = de.tum.in.multilabel.Utils.convertIntegerArray(indices.toArray(new Integer[indices.size()]));
		final Instances newTop = new Instances(top, top.numInstances());
		for (final int randomIndice : randomIndices)
			newTop.add(top.get(randomIndice));
		final Reorder reorder = new Reorder();
		reorder.setAttributeIndicesArray(randomIndices);
		reorder.setInputFormat(newCenterTrain);
		newCenterTrain = Filter.useFilter(newCenterTrain, reorder);
		if (newCenterTest != null) {
			reorder.setInputFormat(newCenterTest);
			newCenterTest = Filter.useFilter(newCenterTest, reorder);
		}
		return new Instances[] { newLeft, newTop, newCenterTrain, newCenterTest };
	}
	
	/**
	 * @param karray
	 */
	public void setKArray(final int[] karray) {
		this.karray = karray;
	}
	
	/**
	 * @param karray
	 */
	public void setKArraytrans(final int[] karray) {
		karraytransp = karray;
	}
	
	/**
	 * @param opt
	 */
	public void setOpt(final boolean opt) {
		this.opt = opt;
	}
	
	/**
	 * @param result
	 */
	public void setResult(final Instances result) {
		this.result = result;
	}
	
	/**
	 * @param tarray
	 */
	public void setTArray(final double[] tarray) {
		this.tarray = tarray;
	}
	
	/**
	 * @param fileName
	 *            file name
	 * @param workingDir
	 *            working dir
	 * @param start
	 * @throws Exception
	 *             any exception
	 */
	@SuppressWarnings ("resource")
	public void write(final String fileName, final String workingDir, long start) throws Exception {
		final String id = Utils.getOption("id", classifierOptions);
		// this.writeROC(id, workingDir);
		// this.writeRecall(id, workingDir);
		final StringBuilder sb = new StringBuilder();
		sb.append(id + "\t");
		sb.append("data=" + Utils.getOption("left", classifierOptions) + "\t");
		sb.append("baseClassifierType=" + Utils.getOption("S", classifierOptions) + "\t");
		sb.append("k=" + Utils.getOption("k", classifierOptions) + "\t");
		sb.append("t=" + Utils.getOption("t", classifierOptions) + "\t");
		final Instance myInstance = getThresholdInstance();
		sb.append("useBipartion=" + useBipartion + "\t");
		sb.append("theshold=" + myInstance.value(getResult().attribute(ThresholdCurve.THRESHOLD_NAME).index()) + "\t");
		sb.append("TP=" + myInstance.value(getResult().attribute(ThresholdCurve.TRUE_POS_NAME).index()) + "\t");
		sb.append("TN=" + myInstance.value(getResult().attribute(ThresholdCurve.TRUE_NEG_NAME).index()) + "\t");
		sb.append("FP=" + myInstance.value(getResult().attribute(ThresholdCurve.FALSE_POS_NAME).index()) + "\t");
		sb.append("FN=" + myInstance.value(getResult().attribute(ThresholdCurve.FALSE_NEG_NAME).index()) + "\t");
		sb.append("FMEASURE=" + myInstance.value(getResult().attribute(ThresholdCurve.FMEASURE_NAME).index()) + "\t");
		sb.append("TPRATE=" + myInstance.value(getResult().attribute(ThresholdCurve.TP_RATE_NAME).index()) + "\t");
		sb.append("FPRATE=" + myInstance.value(getResult().attribute(ThresholdCurve.FP_RATE_NAME).index()) + "\t");
		sb.append("PRECISION=" + myInstance.value(getResult().attribute(ThresholdCurve.PRECISION_NAME).index()) + "\t");
		sb.append("RECALL=" + myInstance.value(getResult().attribute(ThresholdCurve.RECALL_NAME).index()) + "\t");
		sb.append("FALLOUT=" + myInstance.value(getResult().attribute(ThresholdCurve.FALLOUT_NAME).index()) + "\t");
		sb.append("ACC=" + NobleEvaluator.evalACC(myInstance, getResult()) + "\t");
		sb.append("time=" + (System.currentTimeMillis() - start) + "\t");
		final File file = new File(fileName);
		if (!file.exists()) file.createNewFile();
		if (NobleEvaluator.isLocked(file)) do
			Thread.sleep(100);
		while (NobleEvaluator.isLocked(file));
		final FileLock lock = new RandomAccessFile(file, "rw").getChannel().lock();
		final BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		writer.append(sb.toString() + "\n");
		writer.flush();
		writer.close();
		lock.release();
	}
}
