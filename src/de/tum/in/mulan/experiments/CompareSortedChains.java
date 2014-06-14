package de.tum.in.mulan.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.ClassifierChain;
import mulan.classifier.transformation.EnsembleOfClassifierChains;
import mulan.classifier.transformation.LabelPowerset;
import mulan.classifier.transformation.MultiLabelStacking;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.Evaluator;
import mulan.evaluation.measure.Measure;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.search.ci.ICSSearchAlgorithm;
import weka.classifiers.bayes.net.search.local.HillClimber;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Range;
import weka.core.Utils;
import weka.experiment.PairedCorrectedTTester;
import weka.experiment.PairedTTester;
import weka.experiment.ResultMatrix;
import weka.experiment.ResultMatrixLatex;
import weka.experiment.ResultMatrixPlainText;
import de.tum.in.mulan.classifier.transformation.SortedCC;

@SuppressWarnings ({ "javadoc", "deprecation" })
public class CompareSortedChains implements Serializable {
	
	public class DataStore implements Serializable {
		
		private final static long		serialVersionUID	= 666L;
		// private final static long serialVersionUID =
		// 2960133632497472150L;//-2402755040176616622L;//-3462546304225076739L;//-2402755040176616622L;//2960133632497472150L;//666L;//-2402755040176616622L;
		public int						run;
		public String					dataset;
		public int						id;
		public String					learnerstr;
		public TreeMap<String, Double>	measures;
		
		public DataStore() {
			measures = new TreeMap<>();
		}
		
	}
	
	public class DataStoreNew implements Serializable {
		
		private final static long		serialVersionUID	= 666L;
		
		public int						run;
		public String					dataset;
		public int						id;
		public String					learnerstr;
		public TreeMap<String, Double>	measures;
		
		public DataStoreNew() {
			measures = new TreeMap<>();
		}
		
		public DataStoreNew(final DataStore ds) {
			measures = ds.measures;
			run = ds.run;
			dataset = ds.dataset;
			id = ds.id;
			learnerstr = ds.learnerstr;
			
		}
		
	}
	
	public class EvalThreaded implements Runnable {
		
		public DataStoreNew			ds;
		
		private Evaluator			eval;
		public Evaluation			evaluation;
		private MultiLabelInstances	dataset;
		private MultiLabelLearner	mlc;
		public int					repetition;
		public String				learnerstr;
		public String				dsstr;
		public int					id;
		private final String		workingdir;
		private final boolean		save;
		
		public EvalThreaded(final MultiLabelLearner mlc, final MultiLabelInstances dataset, final int repetition,
				final String dsstr, final String learnerstr, final int id, final String workingdir, final boolean save) {
			eval = new Evaluator();
			eval.setStrict(false);
			this.dataset = dataset;
			this.mlc = mlc;
			this.repetition = repetition;
			this.dsstr = dsstr;
			this.learnerstr = learnerstr;
			this.id = id;
			ds = new DataStoreNew();
			this.workingdir = workingdir;
			this.save = save;
		}
		
		@SuppressWarnings ("resource")
		@Override
		public void run() {
			try {
				
				System.err.println("started thread: " + id);
				System.err.println("learner: " + learnerstr);
				System.err.println("ds: " + dsstr);
				System.err.println("rep: " + repetition);
				
				// FIXME skip all sortedcc
				// if (this.learnerstr.contains("SortedCC")) {
				// return;
				// }
				
				// FIXME skip all bibtex
				if (dsstr.contains("Bibtex")) {
					System.err.println("we don't like Bibtex, so let's skip it...");
					return;
				}
				
				if (new File(workingdir + id + ".ob").exists() && !learnerstr.equals(CompareSortedChains.runanyway)) {
					System.err.println(workingdir + id + ".ob" + " exists and no SortedCC, so no need to run...");
					return;
				}
				
				final Random rand = new Random();
				
				final Instances randData = new Instances(dataset.getDataSet());
				randData.randomize(rand);
				
				final Instances train = randData.trainCV(3, 1);
				final MultiLabelInstances multiTrain = new MultiLabelInstances(train, dataset.getLabelsMetaData());
				final Instances test = randData.testCV(3, 1);
				final MultiLabelInstances multiTest = new MultiLabelInstances(test, dataset.getLabelsMetaData());
				mlc.build(multiTrain);
				evaluation = eval.evaluate(mlc, multiTest);
				
				ds.run = repetition;
				ds.dataset = dsstr;
				ds.id = id;
				ds.learnerstr = learnerstr;
				
				for (final Measure meas : evaluation.getMeasures())
					ds.measures.put(meas.getName(), meas.getValue());
				
				if (save) {
					System.err.println("writing object...");
					
					System.err.println("writing " + workingdir + ds.id + ".ob");
					final ObjectOutputStream oos =
							new ObjectOutputStream(new FileOutputStream(workingdir + ds.id + ".ob"));
					oos.writeObject(ds);
					oos.close();
					
					System.err.println("writing objects... done");
				}
				
				eval = null;
				
				dataset = null;
				mlc = null;
				
				System.err.println("done thread " + id);
				
			} catch (final Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private final static long	serialVersionUID	= -3462546304225076739L;	// -3462546304225076739L;
	public static String		runanyway			= null;
	
	@SuppressWarnings ("unused")
	public static void main(final String[] args) throws Exception {
		new CompareSortedChains(args);
	}
	
	@SuppressWarnings ({ "resource" })
	public CompareSortedChains(final String[] args) throws Exception {
		final Vector<DataStoreNew> stores = new Vector<>();
		
		boolean plaintext = true;
		final boolean latex = Utils.getFlag("latex", args);
		plaintext = !latex;
		
		final String baseds = "data/";
		
		final boolean run = !Utils.getFlag("norun", args);
		final boolean save = !Utils.getFlag("nosave", args);
		final boolean read = Utils.getFlag("read", args);
		final boolean calc = Utils.getFlag("calc", args);
		int testid = -1;
		
		final String runover = Utils.getOption("runanyway", args);
		
		if (runover != null && !runover.equals("")) CompareSortedChains.runanyway = runover;
		
		String workingdir = Utils.getOption("workingdir", args);
		
		if (workingdir == null || workingdir.equals("")) workingdir = "bayeswdir_hill/";
		
		final int repetitions = 100;
		
		int numthreads = -1;
		
		int rangesta = -1;
		int rangeend = -1;
		
		final String rangestr = Utils.getOption("range", args);
		
		if (rangestr != null && !rangestr.equals("")) {
			final String[] rangesar = rangestr.split("-");
			rangesta = Integer.parseInt(rangesar[0]);
			rangeend = Integer.parseInt(rangesar[1]);
		}
		
		final String numthreadsstr = Utils.getOption("threads", args);
		
		if (numthreadsstr != null && !numthreadsstr.equals("")) numthreads = Integer.parseInt(numthreadsstr);
		
		final String idstr = Utils.getOption("id", args);
		
		if (idstr != null && !idstr.equals("")) testid = Integer.parseInt(idstr) - 1;
		
		numthreads = numthreads < 0 ? Runtime.getRuntime().availableProcessors() : numthreads;
		System.err.println("numthreads: " + numthreads);
		
		final BayesNet bayesNet = new BayesNet();
		final Classifier baseClassifier = new RandomForest();
		
		final TreeMap<String, MultiLabelInstances> mlds = new TreeMap<>();
		
		mlds.put("Llog", new MultiLabelInstances(baseds + "llog.arff", baseds + "llog.xml"));
		mlds.put("Bibtex", new MultiLabelInstances(baseds + "bibtex.arff", baseds + "bibtex.xml"));
		mlds.put("Enron", new MultiLabelInstances(baseds + "enron.arff", baseds + "enron.xml"));
		mlds.put("Medical", new MultiLabelInstances(baseds + "medical.arff", baseds + "medical.xml"));
		mlds.put("Yeast", new MultiLabelInstances(baseds + "yeast.arff", baseds + "yeast.xml"));
		mlds.put("tmc2007-500", new MultiLabelInstances(baseds + "tmc2007.arff", baseds + "tmc2007.xml"));
		mlds.put("SLASHDOT-F", new MultiLabelInstances(baseds + "SLASHDOT-F.arff", baseds + "SLASHDOT-F.xml"));
		
		final TreeMap<String, MultiLabelLearner> mlclassifiers = new TreeMap<>();
		
		SortedCC sccor = null;
		if (Utils.getFlag("csi", args)) {
			final ICSSearchAlgorithm sa = new ICSSearchAlgorithm();
			bayesNet.setSearchAlgorithm(sa);
			bayesNet.setUseADTree(true);
			
			sccor = new SortedCC(baseClassifier, bayesNet);
			
		} else {
			final HillClimber sa = new HillClimber();
			sa.setMaxNrOfParents(100);
			
			bayesNet.setSearchAlgorithm(sa);
			bayesNet.setUseADTree(true);
			// once
			sccor = new SortedCC(baseClassifier, bayesNet);
			
			sccor.setDontUseClassLabel(false);
			sa.setInitAsNaiveBayes(false);
		}
		
		SortedCC scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.childDown);
		
		mlclassifiers.put("zSortedCC-childDown", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.childUp);
		
		mlclassifiers.put("zSortedCC-childup", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.parentUp);
		
		mlclassifiers.put("zSortedCC-parentup", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.parentDown);
		
		mlclassifiers.put("zSortedCC-parentdown", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.childDownD);
		
		mlclassifiers.put("zSortedCC-childDownD", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.childUpD);
		
		mlclassifiers.put("zSortedCC-childupD", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.parentUpD);
		
		mlclassifiers.put("zSortedCC-parentupD", scc);
		
		scc = (SortedCC) sccor.makeCopy();
		
		scc.setSortMethod(SortedCC.SortMethod.parentDownD);
		
		mlclassifiers.put("zSortedCC-parentdownD", scc);
		
		mlclassifiers.put("BR", new BinaryRelevance(baseClassifier));
		
		mlclassifiers.put("Stacking", new MultiLabelStacking(baseClassifier, baseClassifier));
		mlclassifiers.put("LP", new LabelPowerset(baseClassifier));
		mlclassifiers.put("ECC", new EnsembleOfClassifierChains(baseClassifier, 5, true, false));
		mlclassifiers.put("CC", new ClassifierChain(baseClassifier));
		mlclassifiers.put("MLkNN", new MLkNN());
		
		final String[] measures =
				{ "Example-Based Accuracy", "Coverage", "Example-Based F Measure", "Example-Based Recall",
						"Example-Based Precision", "Subset Accuracy", "Micro-averaged F-Measure",
						"Micro-averaged Recall", "Micro-averaged Precision", "Micro-averaged AUC", "Hamming Loss",
						"OneError", "Ranking Loss" };
		
		if (run) {
			
			final ExecutorService execSvc = Executors.newFixedThreadPool(numthreads);
			
			int id = 0;
			for (final String dataset : mlds.keySet())
				for (final String classif : mlclassifiers.keySet())
					for (int repetition = 0; repetition < repetitions; repetition++) {
						if ((id == testid || testid < 0) && (rangesta < 0 || id > rangesta)
								&& (rangeend < 0 || id < rangeend)) {
							
							MultiLabelLearner mlc = mlclassifiers.get(classif).makeCopy();
							if (mlc instanceof ClassifierChain) {
								
								final ArrayList<Integer> chainlist = new ArrayList<>();
								for (int i = 0; i < mlds.get(dataset).getLabelIndices().length; i++)
									chainlist.add(i);
								
								Collections.shuffle(chainlist);
								
								final int[] chain = new int[chainlist.size()];
								
								int count = 0;
								
								for (final Integer i : chainlist) {
									chain[count] = i;
									count++;
								}
								
								mlc = new ClassifierChain(baseClassifier, chain);
							}
							
							final EvalThreaded threadob =
									new EvalThreaded(mlc, mlds.get(dataset), repetition, dataset, classif, id,
											workingdir, save);
							
							execSvc.execute(threadob);
							if (calc) stores.add(threadob.ds);
							
						}
						id++;
					}
			System.err.println("added threads: " + id);
			execSvc.shutdown();
			System.err.println("waiting for threads...");
			execSvc.awaitTermination(30, TimeUnit.DAYS);
			System.err.println("waiting for threads...done");
			
		}
		if (calc) {
			if (read) {
				final File folder = new File(workingdir);
				final String[] list = folder.list();
				Arrays.sort(list);
				for (final String file : list) {
					// if ((new File("new_"+workingdir + file)).exists()) {
					// System.err.println("skipping...");
					// continue;
					// }
					System.err.println("reading: " + file);
					
					final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(workingdir + file));
					final Object o = ois.readObject();
					final DataStoreNew ds = (DataStoreNew) o;
					
					if (ds.dataset.equals("Bibtex")) continue;
					
					stores.add(ds);
					ois.close();
					
					// DataStoreNew dsn= new DataStoreNew(ds);
					
					// ObjectOutputStream oos = new ObjectOutputStream(new
					// FileOutputStream("new_"+workingdir + ds.id+ ".ob"));
					// oos.writeObject(dsn);
					// oos.close();
					
				}
			}
			
			// System.exit(0);
			
			for (final String key : mlclassifiers.keySet())
				if (key.contains("SortedCC")) {
					System.err.println("calculating for... " + key);
					
					// create instances for sigtestoutput
					
					// col 1: run
					// col 2: method
					// col 3: data set
					// col 4-n: measures
					
					final FastVector<Attribute> attributes = new FastVector<>();
					
					// //run
					FastVector<String> attributeVals = new FastVector<>();
					attributes.addElement(new Attribute("run"));
					
					// method
					attributeVals = new FastVector<>();
					for (final String method : mlclassifiers.keySet()) {
						if (method.contains("SortedCC") && !method.equals(key)) continue;
						
						attributeVals.addElement(method);
					}
					attributes.addElement(new Attribute("methods", attributeVals));
					
					// dataset
					attributeVals = new FastVector<>();
					for (final String dataset : mlds.keySet()) {
						if (dataset.equals("Bibtex")) continue;
						
						attributeVals.addElement(dataset);
					}
					attributes.addElement(new Attribute("dataset", attributeVals));
					
					// measures
					for (final String measure : measures)
						attributes.addElement(new Attribute(measure));
					
					final Instances inst = new Instances("eval", attributes, 0);
					
					for (final DataStoreNew store : stores) {
						if (store.learnerstr.contains("SortedCC") && !store.learnerstr.equals(key)
								|| store.dataset.equals("Bibtex")) continue;
						
						final double[] vals = new double[inst.numAttributes()];
						vals[0] = store.run;
						vals[1] = inst.attribute("methods").indexOfValue(store.learnerstr);
						vals[2] = inst.attribute("dataset").indexOfValue(store.dataset);
						
						for (final String measure : store.measures.keySet())
							if (inst.attribute(measure) != null)
								vals[inst.attribute(measure).index()] = store.measures.get(measure);
						inst.add(new DenseInstance(1.0, vals));
					}
					
					final PairedTTester tester = new PairedCorrectedTTester();
					tester.setInstances(inst);
					// tester.setSortColumn(-1);
					tester.setRunColumn(inst.attribute("run").index());
					// tester.setFoldColumn(-1);
					tester.setDatasetKeyColumns(new Range("" + (inst.attribute("dataset").index() + 1)));
					tester.setResultsetKeyColumns(new Range("" + (inst.attribute("methods").index() + 1)));
					// + ","
					// + (result.attribute("Key_Scheme_options").index() + 1)
					// + ","
					// + (result.attribute("Key_Scheme_version_ID").index() +
					// 1)))
					if (plaintext) tester.setResultMatrix(new ResultMatrixPlainText());
					
					if (latex) tester.setResultMatrix(new ResultMatrixLatex());
					
					// tester.setResultMatrix(new ResultMatrixGnuPlot());
					// tester.setDisplayedResultsets(null);
					tester.setSignificanceLevel(0.05);
					tester.setShowStdDevs(true);
					
					// System.out.println(inst);
					
					for (final String measure : measures) {
						
						// fill result matrix (but discarding the output)
						
						tester.multiResultsetFull(inst.attribute("methods").indexOfValue(key), inst.attribute(measure)
								.index());
						
						// tester.multiResultsetSummary(inst.attribute("accuracy").index()).toString();
						
						// output results for reach dataset
						// System.out.println("\nResult:");
						final ResultMatrix matrix = tester.getResultMatrix();
						System.out.println("#####################################################################"
								+ key + " - " + measure
								+ "##############################################################");
						System.out.println(matrix.toStringMatrix());
						// System.out.println("######################");
						// tester.multiResultsetRanking(inst.attribute("accuracy").index()).toString();
						// matrix = tester.getResultMatrix();
						// System.out.println(matrix.toStringRanking());
						// System.out.println("######################");
						// tester.multiResultsetSummary(inst.attribute("accuracy").index()).toString();
						// matrix = tester.getResultMatrix();
						// System.out.println(matrix.toStringSummary());
						// System.out.println("######################");
						// System.out.println(matrix.toStringKey());
						// System.out.println("######################");
						
					}
					
				}
		}
	}
}
