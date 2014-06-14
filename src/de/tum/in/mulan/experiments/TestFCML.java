package de.tum.in.mulan.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.transformation.ClassifierChain;
import mulan.core.MulanRuntimeException;
import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import mulan.data.Statistics;
import mulan.evaluation.Evaluation;
import mulan.evaluation.Evaluator;
import mulan.evaluation.measure.Measure;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.RankedOutputSearch;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.WrapperSubsetEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import de.tum.in.mulan.classifier.meta.FilteredMLLearner;
import de.tum.in.mulan.classifier.transformation.FCML;

/**
 * Testing class for FCML. Contains routines for running, writing and reading FCML experiments
 * 
 * @author LehnereS
 */
@SuppressWarnings ("unused")
public class TestFCML {
	
	private static final int	folds				= 2;
	static String[]				fcmlOptions			= new String[] { " ", " -ranks ", " -log ", " -log -ranks ",
			" -outlier ", " -log -outlier ", " -outlier -ranks ", " -log -outlier -ranks " };
	static boolean				generateWithTesting	= false;
	
	/**
	 * method used to evaluate FCML of varying number of clusters. Experiments are repeated with increasing nc until nc
	 * = |L| (by 2% steps)
	 * 
	 * @param data
	 *            {@link MultiLabelInstances}
	 * @param classifier
	 *            {@link FCML}
	 * @param targetFolder
	 *            target folder for result files
	 * @param options
	 *            FCML options to be noted in result files
	 * @throws Exception
	 *             any exception
	 */
	public static void extendedEvaluation(final MultiLabelInstances data, final FCML classifier,
			final String targetFolder, final String[] options) throws Exception {
		final int step =
				(int) (data.getNumLabels() * ((double) 2 / 100) > 1 ? Math.round(data.getNumLabels()
						* ((double) 2 / 100)) : 1);
		for (int currentNC = 2; currentNC <= data.getNumLabels(); currentNC = currentNC + step) {
			Instances workingSet = new Instances(data.getDataSet());
			workingSet.randomize(new Random());
			de.tum.in.multilabel.Utils.setNumberOfClusters(classifier.getClusterer(), currentNC);
			final Evaluation[] evaluations = new Evaluation[TestFCML.folds];
			for (int fold = 0; fold < TestFCML.folds; fold++) {
				final Instances train = workingSet.trainCV(TestFCML.folds, fold);
				final Instances test = workingSet.testCV(TestFCML.folds, fold);
				final MultiLabelInstances mlTrain = data.reintegrateModifiedDataSet(train);
				final MultiLabelInstances mlTest = data.reintegrateModifiedDataSet(test);
				final FCML clone = (FCML) classifier.makeCopy();
				clone.build(mlTrain);
				final Evaluator eval = new Evaluator();
				eval.setStrict(false);
				evaluations[fold] = eval.evaluate(clone, mlTest);
			}
			Statistics stats = new Statistics();
			// stats.calculateCoocurrence(data);
			stats.calculatePhi(data);
			stats.calculateStats(data);
			TestFCML.saveEvaluation(evaluations, null, currentNC, 0, options, targetFolder + "/currentNC_" + currentNC);
			final FCML clone = (FCML) classifier.makeCopy();
			clone.build(data);
			System.err.println("starting internal cross validation (groups)");
			for (int i = 0; i < clone.getEnsemble().length; i++) {
				System.err.println("ensemble " + i + " ...");
				try {
					workingSet = clone.getEnsemble()[i].getFilteredData(data).getDataSet();
					workingSet.randomize(new Random());
					for (int fold = 0; fold < TestFCML.folds; fold++) {
						final Instances train = workingSet.trainCV(TestFCML.folds, fold);
						final Instances test = workingSet.testCV(TestFCML.folds, fold);
						final MultiLabelInstances mlTrain = data.reintegrateModifiedDataSet(train);
						final MultiLabelInstances mlTest = data.reintegrateModifiedDataSet(test);
						final MultiLabelLearner learner = classifier.getMlClassifier().makeCopy();
						learner.build(mlTrain);
						final Evaluator eval = new Evaluator();
						eval.setStrict(false);
						evaluations[fold] = eval.evaluate(learner, mlTest);
					}
					stats = new Statistics();
					// stats.calculateCoocurrence(data);
					stats.calculatePhi(data);
					stats.calculateStats(data);
					TestFCML.saveEvaluation(evaluations, clone.getEnsemble()[i], currentNC, i, options, targetFolder
							+ "/currentNC_" + currentNC + "/group" + i);
				} catch (final InvalidDataFormatException e) {
					workingSet = clone.getEnsemble()[i].getFilteredSLData(data);
					final Classifier slClassifier = clone.getBaseClassifier();
					final weka.classifiers.Evaluation eval = new weka.classifiers.Evaluation(workingSet);
					eval.crossValidateModel(slClassifier, workingSet, TestFCML.folds, new Random());
					TestFCML.saveEvaluation(eval, clone.getEnsemble()[i], currentNC, i, options, targetFolder
							+ "/currentNC_" + currentNC + "/group" + i);
				}
				System.err.println("ensemble " + i + " ...done");
			}
		}
	}
	
	/**
	 * generates a job description file for the SUN Grid Engine
	 * 
	 * @param name
	 *            name of the job
	 * @param fileName
	 *            target file
	 * @throws Exception
	 *             any exception
	 */
	@SuppressWarnings ("resource")
	private static void generateJobs(final String name, final String fileName) throws Exception {
		final String command = TestSuite.baseCommand + " de.tum.in.mulan.experiments/TestFCML -run -D ";
		final StringBuilder sb = new StringBuilder();
		sb.append(TestSuite.header.replaceAll("%NAME%", name));
		int taskId = 1;
		// data, ml, sl, ae, as, cl, t, nc, dm, cm, option
		final ArrayList<Integer[]> allJobs = new ArrayList<Integer[]>();
		sb.append("case $SGE_TASK_ID in\n");
		for (int data = 0; data < TestSuite.dataSets.length - 7; data++)
			for (int ml = 1; ml < TestSuite.multiLabelLearner.length - 5; ml++)
				for (int sl = 0; sl < TestSuite.singleLabelLearner.length - 2; sl++)
					for (int ae = 0; ae < TestSuite.ASEvaluation.length - 6; ae++) {
						final int as = ae == 1 ? 1 : 2;
						for (int opt = 0; opt < TestFCML.fcmlOptions.length - 4; opt++)
							if (TestFCML.fcmlOptions[opt].contains("ranks")) for (int cm = 0; cm < TestSuite.clusterMethods.length - 3; cm++)
								allJobs.add(new Integer[] { data, ml, sl, ae, as, 2, -1, -1, 4, cm, opt });
							else for (int cl = 0; cl < TestSuite.clusterAlgorithms.length; cl++)
								if (cl == 0) {
									// allJobs.add(new Integer[] { data, ml,
									// sl, ae, as, cl, -1, -1, -1, -1,
									// opt });
								} else if (cl == 1) {
									// for (int dm = 0; dm <
									// (TestSuite.distanceFunctions.length
									// - 4); dm++) {
									// allJobs.add(new Integer[] {
									// data, ml, sl, ae, as,
									// cl, -1, -1, dm, -1, opt });
									// }
								} else for (int dm = 0; dm < 1; dm++)
									for (int cm = 0; cm < TestSuite.clusterMethods.length - 3; cm++)
										allJobs.add(new Integer[] { data, ml, sl, ae, as, cl, -1, -1, dm, cm, opt });
					}
		final StringBuilder info = new StringBuilder();
		for (final Integer[] job : allJobs) {
			final String options =
					" -data " + job[0] + " -ml " + job[1] + " -sl " + job[2] + " -ae " + job[3] + " -as " + job[4]
							+ " -cl " + job[5] + " -t " + job[6] + " -nc " + job[7] + " -dm " + job[8] + " -cm "
							+ job[9] + TestFCML.fcmlOptions[job[10]] + " -outputFolder results/experiment#"
							+ TestSuite.dataSets[job[0]].replaceAll("\\./data/", "") + "#" + taskId;
			if (TestFCML.generateWithTesting && job[0] == 5) {
				System.err.println("testing " + options + "...");
				TestFCML.runJob(("-test " + options).split(" "));
				System.err.println("testing " + options + "...done");
			}
			info.append(taskId + "\t" + TestFCML.getInfo(options.split(" ")));
			sb.append(taskId++ + ")\n\t" + command + options + ";;\n");
		}
		final BufferedWriter writer = new BufferedWriter(new FileWriter(new File("info")));
		writer.write(info.toString());
		writer.flush();
		writer.close();
		TestSuite.writeJobs(fileName, sb, taskId);
	}
	
	/**
	 * reads and formats command line arguments
	 * 
	 * @param options
	 *            command line arguments
	 * @return a formated string containing options
	 * @throws Exception
	 *             any exception
	 */
	private static String getInfo(final String[] options) throws Exception {
		final String[] opt = options.clone();
		String dm = Utils.getOption("dm", opt);
		if (dm.length() > 0) {
			final int dmint = Integer.valueOf(dm);
			if (dmint > -1)
				dm =
						TestSuite.distanceFunctions[dmint].replaceAll("weka\\.core\\.", "").replaceAll(
								"de\\.tum\\.in\\.multilabel\\.distancefunction.", "");
		}
		String cm = Utils.getOption("cm", opt);
		if (cm.length() > 0 && Integer.valueOf(cm) > -1) cm = TestSuite.clusterMethodNames[Integer.valueOf(cm)];
		return TestSuite.multiLabelLearner[Integer.valueOf(Utils.getOption("ml", opt))].replaceAll(
				"mulan\\.classifier\\..*\\.", "")
				+ "\t"
				+ Utils.getFlag("log", opt)
				+ "\t"
				+ Utils.getFlag("outlier", opt)
				+ "\t"
				+ Utils.getFlag("ranks", opt)
				+ "\t"
				+ TestSuite.singleLabelLearner[Integer.valueOf(Utils.getOption("sl", opt))].replaceAll(
						"weka\\.classifiers\\..*\\.", "")
				+ "\t"
				+ TestSuite.dataSets[Integer.valueOf(Utils.getOption("data", opt))].replaceAll("\\./data/", "")
				+ "\t"
				+ TestSuite.ASEvaluation[Integer.valueOf(Utils.getOption("ae", opt))].replaceAll(
						"weka\\.attributeSelection\\.", "")
				+ "\t"
				+ TestSuite.clusterAlgorithms[Integer.valueOf(Utils.getOption("cl", opt))].replaceAll(
						"weka\\.clusterers\\.", "")
				+ "\t"
				+ dm
				+ "\t"
				+ cm
				+ "\t"
				+ Utils.getOption("nc", opt)
				+ "\t"
				+ Double.valueOf(Utils.getOption("t", opt)) + "\n";
	}
	
	// @formatter:off
    /**
     * main class, possible flags are:
     * -run = job is run as cluster job
     * -jobs = creating a job file for SUN Grid Engine
     * -read = reads job results found in "./results/field/current/FCML.zip"
     * 
     * without flag local routine as specified in main method.
     * 
     * @param args
     *            command line arguments
     * @throws Exception
     *             any exception
     */
    // @formatter:on
	public static void main(final String[] args) throws Exception {
		if (Utils.getFlag("run", args)) {
			System.err
					.println("CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE \n");
			TestFCML.runJob(args);
		} else if (Utils.getFlag("jobs", args)) {
			System.err
					.println("JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE \n");
			TestFCML.generateJobs("FCML", "FCMLJobs");
		} else if (Utils.getFlag("read", args)) {
			System.err
					.println("RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE \n");
			TestSuite.readResults("./results/field/current/FCML.zip");
		} else {
			System.err
					.println("TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE \npress ENTER to start...\n");
			// new BufferedReader(new InputStreamReader(System.in)).readLine();
			// final String data = TestSuite.dataSets[3];
			final String data = "data/CAL500";
			final MultiLabelInstances inputData = new MultiLabelInstances(data + ".arff", data + ".xml");
			// final MultiLabelInstances inputData = new
			// MultiLabelInstances(data,
			// "data/tcast/tcast.xml");
			final HierarchicalClusterer clusterer = new HierarchicalClusterer();
			clusterer.setDistanceFunction(new EuclideanDistance());
			clusterer.setNumClusters(89);
			final AttributeSelection selector = new AttributeSelection();
			selector.setEvaluator(new InfoGainAttributeEval());
			selector.setSearch(new Ranker());
			final FCML FCML = new FCML(new ClassifierChain(new SMO()), new SMO(), selector, clusterer);
			FCML.setDebug(true);
			FCML.setOptimizeClusterNumber(false);
			FCML.setMultiplier(2);
			FCML.setComputeThreshold(true);
			FCML.setUseLogScores(false);
			
			TestFCML.extendedEvaluation(inputData, FCML, "results/experiment#"
					+ data.split("/")[data.split("/").length - 1], new String[] { "test", "test" });
			// readEvaluation("results/experiment#"
			// + data.split("/")[data.split("/").length - 1]);
			// final Evaluator eval = new Evaluator();
			// eval.setStrict(false);
			// System.out.println(eval.crossValidate(FCML, inputData,
			// TestSuite.folds));
		}
	}
	
	@SuppressWarnings ("resource")
	private static void readEvaluationHelper(final File file,
			final HashMap<Integer, HashMap<Integer, DataStore>> results) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		if (file.isDirectory() && (file.getName().contains("currentNC") || file.getName().contains("groups"))) for (final File file2 : file
				.listFiles())
			TestFCML.readEvaluationHelper(file2, results);
		else if (file.isDirectory()) for (final File file2 : file.listFiles())
			TestFCML.readEvaluationHelper(file2, results);
		else if (file.isFile()) {
			final ObjectInputStream oi = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			if (file.getName().contains("data.ob")) {
				final DataStore data = (DataStore) oi.readObject();
				final HashMap<Integer, DataStore> map = TestFCML.retrieveSubHashMap(results, data.currentNC);
				map.put(data.group, data);
			}
			oi.close();
		}
	}
	
	/**
	 * reads a result line and appends formated results to the given {@link Stringbuffer}
	 * 
	 * @param line
	 *            current line
	 * @param currentFile
	 *            {@link StringBuffer}
	 * @throws Exception
	 *             any exception
	 */
	static void readResults(final String line, final StringBuffer currentFile) throws Exception {
		final String[] splittedLine = line.split(",");
		String dm = Utils.getOption("dm", splittedLine);
		if (dm.length() > 0) {
			final int dmint = Integer.valueOf(dm);
			if (dmint > -1)
				dm =
						TestSuite.distanceFunctions[dmint].replaceAll("weka\\.core\\.", "").replaceAll(
								"de\\.tum\\.in\\.multilabel\\.distancefunction", "");
		}
		String cm = Utils.getOption("cm", splittedLine);
		if (cm.length() > 0 && Integer.valueOf(cm) > -1) cm = TestSuite.clusterMethodNames[Integer.valueOf(cm)];
		currentFile.append(TestSuite.multiLabelLearner[Integer.valueOf(Utils.getOption("ml", splittedLine))]
				.replaceAll("mulan\\.classifier\\..*\\.", "")
				+ "\t"
				+ Utils.getFlag("log", splittedLine)
				+ "\t"
				+ Utils.getFlag("outlier", splittedLine)
				+ "\t"
				+ Utils.getFlag("ranks", splittedLine)
				+ "\t"
				+ TestSuite.singleLabelLearner[Integer.valueOf(Utils.getOption("sl", splittedLine))].replaceAll(
						"weka\\.classifiers\\..*\\.", "")
				+ "\t"
				+ TestSuite.dataSets[Integer.valueOf(Utils.getOption("data", splittedLine))]
						.replaceAll("\\./data/", "")
				+ "\t"
				+ TestSuite.ASEvaluation[Integer.valueOf(Utils.getOption("ae", splittedLine))].replaceAll(
						"weka\\.attributeSelection\\.", "")
				+ "\t"
				+ TestSuite.clusterAlgorithms[Integer.valueOf(Utils.getOption("cl", splittedLine))].replaceAll(
						"weka\\.clusterers\\.", "")
				+ "\t"
				+ dm
				+ "\t"
				+ cm
				+ "\t"
				+ Utils.getOption("nc", splittedLine)
				+ "\t"
				+ Double.valueOf(Utils.getOption("t", splittedLine))
				+ "\t");
	}
	
	private static <K, V> HashMap<K, V> retrieveSubHashMap(final HashMap<K, HashMap<K, V>> meta, final K key) {
		HashMap<K, V> map;
		if (meta.containsKey(key)) map = meta.get(key);
		else {
			map = new HashMap<K, V>();
			meta.put(key, map);
		}
		return map;
	}
	
	/**
	 * reads command line options and starts a extendend evaluation. used for running experiments on cluster
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception
	 */
	static void runJob(final String[] args) throws Exception {
		final String[] options = args.clone();
		final boolean test = Utils.getFlag("test", args);
		final boolean ranks = Utils.getFlag("ranks", args);
		final boolean log = Utils.getFlag("log", args);
		final boolean outlier = Utils.getFlag("outlier", args);
		final boolean debug = Utils.getFlag("D", args);
		final int data = Integer.valueOf(Utils.getOption("data", args));
		final int sl = Integer.valueOf(Utils.getOption("sl", args));
		final int ml = Integer.valueOf(Utils.getOption("ml", args));
		final int cl = Integer.valueOf(Utils.getOption("cl", args));
		final int nc = Integer.valueOf(Utils.getOption("nc", args));
		final String cmtmp = Utils.getOption("cm", args);
		int cm = Integer.valueOf(cmtmp) > -1 ? cm = Integer.valueOf(cmtmp) : -1;
		final int dm = Integer.valueOf(Utils.getOption("dm", args));
		final int ae = Integer.valueOf(Utils.getOption("ae", args));
		final int as = Integer.valueOf(Utils.getOption("as", args));
		final String ttmp = Utils.getOption("t", args);
		final String target = Utils.getOption("outputFolder", args);
		final double t = Double.valueOf(ttmp) > -1 ? Double.valueOf(ttmp) : Double.NEGATIVE_INFINITY;
		if (debug)
			System.err.println(Thread.currentThread().getName() + "@" + new Date() + ": " + "this is "
					+ TestFCML.getInfo(options));
		final Classifier slClassifier = AbstractClassifier.forName(TestSuite.singleLabelLearner[sl], null);
		final String mlLearnerName = TestSuite.multiLabelLearner[ml];
		final MultiLabelLearner mlClassifier =
				de.tum.in.multilabel.Utils.getMLLearnerFromName(slClassifier, mlLearnerName);
		final MultiLabelInstances inputData =
				new MultiLabelInstances(TestSuite.dataSets[data] + ".arff", TestSuite.dataSets[data] + ".xml");
		final Clusterer clusterer = (Clusterer) Class.forName(TestSuite.clusterAlgorithms[cl]).newInstance();
		if (clusterer instanceof EM) {
			((EM) clusterer).setNumClusters(-1);
			((EM) clusterer).setDebug(false);
		} else if (clusterer instanceof HierarchicalClusterer) {
			((HierarchicalClusterer) clusterer).setDebug(false);
			((HierarchicalClusterer) clusterer).setNumClusters(nc);
			((HierarchicalClusterer) clusterer).setLinkType(new SelectedTag(cm, HierarchicalClusterer.TAGS_LINK_TYPE));
			final DistanceFunction distanceFunction =
					(DistanceFunction) Class.forName(TestSuite.distanceFunctions[dm]).newInstance();
			((HierarchicalClusterer) clusterer).setDistanceFunction(distanceFunction);
		} else if (clusterer instanceof SimpleKMeans) {
			final DistanceFunction distanceFunction =
					(DistanceFunction) Class.forName(TestSuite.distanceFunctions[dm]).newInstance();
			((SimpleKMeans) clusterer).setDistanceFunction(distanceFunction);
			((SimpleKMeans) clusterer).setNumClusters(nc);
		}
		final ASEvaluation evaluator =
				(ASEvaluation) Utils.forName(ASEvaluation.class, TestSuite.ASEvaluation[ae], null);
		if (evaluator instanceof WrapperSubsetEval) {
			((WrapperSubsetEval) evaluator).setClassifier(slClassifier);
			((WrapperSubsetEval) evaluator).setFolds(TestFCML.folds);
		}
		final ASSearch searcher = (ASSearch) Utils.forName(ASSearch.class, TestSuite.ASSearch[as], null);
		((RankedOutputSearch) searcher).setGenerateRanking(true);
		final AttributeSelection selector = new AttributeSelection();
		selector.setEvaluator(evaluator);
		selector.setSearch(searcher);
		final FCML FCML = new FCML(mlClassifier, slClassifier, selector, clusterer);
		FCML.setThreshold(t);
		FCML.setDebug(debug);
		FCML.setRemoveOutliers(outlier);
		FCML.setUseLogScores(log);
		FCML.setUseRanks(ranks);
		FCML.setComputeThreshold(true);
		FCML.setMultiplier(1);
		Utils.checkForRemainingOptions(args);
		if (test) {
			System.err.println(t);
			System.err.println("test succeeded");
			return;
		}
		TestFCML.extendedEvaluation(inputData, FCML, target, options);
	}
	
	@SuppressWarnings ({ "unchecked", "resource" })
	private static void saveEvaluation(final Object evaluation, final FilteredMLLearner learner, final int currentNC,
			final int group, final String[] options, final String targetFolder) throws Exception {
		new File(targetFolder).mkdirs();
		final DataStore store = new DataStore();
		final String filename = targetFolder + "/data.ob";
		if (evaluation instanceof Evaluation[]) {
			final Evaluation[] eval = (Evaluation[]) evaluation;
			store.measures = new HashMap[eval.length];
			for (int i = 0; i < eval.length; i++) {
				store.measures[i] = new HashMap<String, Double>();
				for (final Measure me : eval[i].getMeasures())
					try {
						store.measures[i].put(me.getName(), me.getValue());
					} catch (final MulanRuntimeException e) {
						store.measures[i].put(me.getName(), null);
					}
			}
		} else if (evaluation instanceof weka.classifiers.Evaluation) {
			final weka.classifiers.Evaluation eval = (weka.classifiers.Evaluation) evaluation;
			store.confusionMatrix = eval.confusionMatrix();
		}
		if (learner != null) {
			store.labelIndices = learner.getLabelIndices();
			store.featureIndices = learner.getFeatureIndices();
		}
		store.currentNC = currentNC;
		store.group = group;
		store.options = options;
		final ObjectOutputStream writer =
				new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File(filename))));
		writer.writeObject(store);
		writer.flush();
		writer.close();
	}
}
