package de.tum.in.mulan.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mulan.classifier.lazy.MLkNN;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluator;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.functions.SMO;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Utils;
import de.tum.in.mulan.classifier.transformation.FCML;

/**
 * main testing class for Feature and Label-Selection in Multi-Label Classification. Different working parametes as
 * datasets, feature selection methods, clusterings etc. are stored here.
 * 
 * @author LehnereS
 */
public class TestSuite {
	static int		arrayJobPackageSize	= 1000;
	static String[]	ASEvaluation		= { "weka.attributeSelection.InfoGainAttributeEval",
			"weka.attributeSelection.SignificanceAttributeEval",
			"weka.attributeSelection.SymmetricalUncertAttributeEval", "weka.attributeSelection.CfsSubsetEval",
			"weka.attributeSelection.ReliefFAttributeEval", "weka.attributeSelection.ChiSquaredAttributeEval",
			"weka.attributeSelection.GainRatioAttributeEval" };
	static String[]	ASSearch			= { "weka.attributeSelection.BestFirst",
			"weka.attributeSelection.GreedyStepwise", "weka.attributeSelection.Ranker" };
	static String	baseCommand			= "java -Xmx4G -cp mulan-tum.jar";
	static String[]	clusterAlgorithms	= { "weka.clusterers.EM", "weka.clusterers.SimpleKMeans",
			"weka.clusterers.HierarchicalClusterer" };
	static String[]	clusterMethodNames	= { "SINGLE", "COMPLETE", "MEAN", "AVERAGE" };
	static int[]	clusterMethods		= { 0, 1, 2, 3 };
	static String[]	dataSets			= { "./data/CAL500", "./data/delicious", "./data/enron",
			"./data/mediamill_small", "./data/medical", "./data/yeast", "./data/bibtex", "./data/bookmarks" };
	static String[]	distanceFunctions	= { "weka.core.EuclideanDistance", "weka.core.ManhattanDistance",
			"weka.core.ChebyshevDistance", "weka.core.MinkowskiDistance",
			"de.tum.in.multilabel.distancefunction.SpearmanCoefficient",
			"de.tum.in.multilabel.distancefunction.TanimotoDistance" };
	static int		folds				= 10;
	static String	header				= "#!/bin/bash\n" + "#$-M lehnerer@in.tum.de\n" + "#$-S /bin/bash\n"
												+ "#$-N %NAME%\n" + "#$-o $HOME/%NAME%/ -j y\n" + "#$-t 1-%SIZE%\n"
												+ "#$-l march=x86_64\n" + "#$-l mf=4000M\n" + ". /etc/profile\n"
												+ "module load java\n" + "cd\n";
	static String	header2				= "#!/bin/bash\n" + "#$-M lehnerer@in.tum.de\n" + "#$-S /bin/bash\n"
												+ "#$-N %NAME%\n" + "#$-o $HOME/%NAME%/ -j y\n" + "#$-l march=x86_64\n"
												+ ". /etc/profile\n" + "module load java\n" + "cd\n";
	static String[]	measures			= { "Example-Based Accuracy", "Coverage", "Example-Based F Measure",
			"Example-Based Recall", "Example-Based Precision", "Subset Accuracy", "Micro-averaged F-Measure",
			"Micro-averaged Recall", "Micro-averaged Precision", "Micro-averaged AUC", "Hamming Loss", "OneError",
			"Ranking Loss"				};
	
	static String[]	multiLabelLearner	= { "mulan.classifier.transformation.BinaryRelevance",
			"mulan.classifier.transformation.ClassifierChain", "mulan.classifier.lazy.MLkNN",
			"mulan.classifier.transformation.MultiLabelStacking", "mulan.classifier.transformation.LabelPowerset",
			"mulan.classifier.transformation.EnsembleOfClassifierChains",
			"de.tum.in.mulan.classifier.transformation.SortedCC" };
	static String[]	singleLabelLearner	= { "weka.classifiers.functions.SMO", "weka.classifiers.lazy.IBk",
			"weka.classifiers.trees.RandomForest" };
	static int		StandardChainLength	= 10;
	static double[]	thresholds			= { 0.075, 1, 0.003, 0.003, 0.1, 0.05, 0.005 };
	
	/**
	 * main method, obsolete because of {@link TestFCML}
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception
	 *             any exception
	 */
	public static void main(final String[] args) throws Exception {
		// if (weka.core.Utils.getFlag("thread", args)) {
		// final String fileName = weka.core.Utils.getOption("file", args);
		// final String range = weka.core.Utils.getOption("range", args);
		// final String output = weka.core.Utils.getOption("out", args);
		// TestSuite.runJobsThreaded(TestSuite.reReadJobs(fileName, range),
		// output);
		// } else
		if (weka.core.Utils.getFlag("FCML", args)) TestFCML.main(args);
		else if (weka.core.Utils.getFlag("Noble", args)) TestNoble.main(args);
		else {
			// for (int i = 2; i < dataSets.length; i++) {
			final String data = TestSuite.dataSets[Integer.valueOf(Utils.getOption("data", args))];
			System.out.println(data);
			final MultiLabelInstances mlData = new MultiLabelInstances(data + ".arff", data + ".xml");
			// String ml = Utils.getOption("ml", args);
			// MultiLabelLearnerBase base = null;
			// if (ml.equals("cc")) {
			// base = new ClassifierChain(new SMO());
			// } else if (ml.equals("lp")) {
			// base = new LabelPowerset(new SMO());
			// } else if (ml.equals("ms")) {
			// base = new MultiLabelStacking(new SMO(), new SMO());
			// } else if (ml.equals("ml")) {
			// base = new MLkNN();
			// }
			//
			// StringBuilder res = new StringBuilder();
			// for (int k = 1; k < 9; k++) {
			// try {
			// HOMER homer = new HOMER(base, k,
			// HierarchyBuilder.Method.BalancedClustering);
			// final Evaluator eval = new Evaluator();
			// eval.setStrict(false);
			// res.append(data
			// + "\nk="
			// + k
			// + "\n"
			// + eval.crossValidate(homer, mlData, TestSuite.folds));
			// } catch (StackOverflowError e) {
			//
			// }
			// }
			//
			// System.out.println(ml);
			// System.out.println(res.toString());
			final HierarchicalClusterer clusterer = new HierarchicalClusterer();
			clusterer.setNumClusters(158);
			final AttributeSelection sel = new AttributeSelection();
			sel.setEvaluator(new InfoGainAttributeEval());
			sel.setSearch(new Ranker());
			final FCML ml = new FCML(new MLkNN(), new SMO(), sel, clusterer);
			final Evaluator eval = new Evaluator();
			eval.setStrict(false);
			System.out.println(eval.crossValidate(ml, mlData, TestSuite.folds));
		}
	}
	
	// public static StringBuilder mainThread(final String[] args)
	// throws NumberFormatException, Exception {
	// if (weka.core.Utils.getFlag("FCML", args)) {
	// weka.core.Utils.getFlag("run", args);
	// return TestFCML.runJob(args);
	// }
	// return null;
	// }
	//
	// private static ArrayList<String[]> reReadJobs(final String fileName,
	// final String range) throws IOException {
	// final ArrayList<String[]> jobs = new ArrayList<String[]>();
	// final int[] ranges = Utils.getRanges(range);
	// final BufferedReader reader = new BufferedReader(new FileReader(
	// new File(fileName)));
	// String line;
	// boolean activated = false;
	// while ((line = reader.readLine()) != null)
	// if (activated && line.endsWith(";;")) {
	// line = line.replace(";;", "");
	// final String[] splitted = line.split(" ");
	// int lastPos = 0;
	// for (; lastPos < splitted.length; lastPos++)
	// if (splitted[lastPos].equals("-D")) {
	// break;
	// }
	// final String[] args = Arrays.copyOfRange(splitted, lastPos - 1,
	// splitted.length);
	// jobs.add(args);
	// activated = false;
	// } else if (line.endsWith(")") && !line.endsWith("*)")) {
	// final int jobId = Integer.valueOf(line.replace(")", ""));
	// activated = Arrays.binarySearch(ranges, jobId) > -1;
	// }
	// return jobs;
	// }
	//
	
	@SuppressWarnings ({ "unused", "resource" })
	static void readResults(final String fileName) throws Exception {
		new StringBuffer();
		final StringBuffer stringBuffer = new StringBuffer();
		final ZipFile zipFile = new ZipFile(fileName);
		final Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();
		new TreeSet<Integer>();
		String line;
		while (zipEntryEnum.hasMoreElements()) {
			final ZipEntry entry = zipEntryEnum.nextElement();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
			boolean idLine = false;
			String[] options = null;
			while ((line = reader.readLine()) != null)
				if (idLine) {
					options = line.split("\t");
					idLine = false;
				} else if (line.startsWith("0")) stringBuffer.append(entry.getName() + "\t"
						+ Utils.arrayToString(options).replaceAll(",", "\t") + "\t"
						+ Utils.arrayToString(line.split("\t")).replaceAll(",", "\t") + "\n");
				else if (line.startsWith("#####")) idLine = true;
		}
		final FileWriter writer = new FileWriter(new File(fileName.toLowerCase().replace(".zip", "") + "_results"));
		writer.write(stringBuffer.toString());
		writer.flush();
	}
	
	// static void runJobsThreaded(final ArrayList<String[]> allJobs,
	// final String dir) throws IOException, InterruptedException {
	// final ExecutorService pool = Executors.newFixedThreadPool(Runtime
	// .getRuntime().availableProcessors());
	// int id = 0;
	// for (final String[] job : allJobs) {
	// pool.execute(new Job(job, dir, id++));
	// Thread.sleep(5000);
	// }
	// pool.shutdown();
	// while (!pool.isTerminated()) {
	// Thread.sleep(60000);
	// }
	// }
	
	@SuppressWarnings ("resource")
	static void writeJobs(final String fileName, final StringBuilder sb, final int taskId) throws IOException {
		final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
		sb.append("*)\n;;\nesac");
		// String[] jobs = sb.toString().split("\n");
		// for (int i = 0; i < jobs.length; i++) {
		// BufferedWriter writer2 = new BufferedWriter(new FileWriter(
		// new File(i + "_job")));
		// writer2.write(TestSuite.header2.replaceAll("%NAME%", "Noble")
		// + "\n" + jobs[i]);
		// writer2.flush();
		// writer2.close();
		//
		// }
		if (taskId < TestSuite.arrayJobPackageSize) writer.write(sb.toString().replace("%SIZE%", "" + taskId));
		else {
			writer.write(sb.toString().replace("#$-t 1-%SIZE%", ""));
			final StringBuffer sb2 = new StringBuffer();
			sb2.append("#!/bin/bash\n");
			int curpack = 1;
			do
				sb2.append("qsub -t " + curpack + "-" + (curpack + TestSuite.arrayJobPackageSize - 1) + " " + fileName
						+ "\n");
			while ((curpack = curpack + TestSuite.arrayJobPackageSize) + TestSuite.arrayJobPackageSize < taskId);
			sb2.append("qsub -t " + curpack + "-" + taskId + " " + fileName + "\n");
			final BufferedWriter writer2 = new BufferedWriter(new FileWriter(new File("start" + fileName)));
			writer2.write(sb2.toString());
			writer2.flush();
			writer2.close();
		}
		writer.flush();
		writer.close();
		System.out.println("nr. of jobs: " + taskId);
	}
}
