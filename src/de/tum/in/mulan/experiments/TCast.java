package de.tum.in.mulan.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.Vector;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.CalibratedLabelRanking;
import mulan.classifier.transformation.ClassifierChain;
import mulan.classifier.transformation.EnsembleOfClassifierChains;
import mulan.classifier.transformation.IncludeLabelsClassifier;
import mulan.classifier.transformation.LabelPowerset;
import mulan.classifier.transformation.MultiLabelStacking;
import mulan.data.LabelsMetaDataImpl;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.MultipleEvaluation;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.GridSearch;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.converters.ArffSaver;
import weka.filters.AllFilter;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import de.tum.in.mulan.classifier.transformation.MLCMAD;
import de.tum.in.mulan.evaluation.MissingCapableEvaluator;
import de.tum.in.preprocessing.EnsembleOfClassifierChainsFiller;

@SuppressWarnings ({ "all" })
public class TCast {
	
	private static class PredictionCouple implements Serializable {
		
		static final long			serialVersionUID	= 666;
		public int					fold;
		public String				name;
		public AttributeStats		stats;
		public NominalPrediction	real;
		public NominalPrediction	second;
		
		public PredictionCouple(NominalPrediction real, NominalPrediction second, String name, AttributeStats stats,
				int fold) {
			this.real = real;
			this.second = second;
			this.name = name;
			this.stats = stats;
			this.fold = fold;
			
		}
		
		public int getFold() {
			return fold;
		}
		
		public String getName() {
			return name;
		}
		
		public NominalPrediction getReal() {
			return real;
		}
		
		public NominalPrediction getSecond() {
			return second;
		}
		
		public AttributeStats getStats() {
			return stats;
		}
		
	}
	
	private static final int				CC		= 1;
	private static final int				BR2		= 2;
	private static final int				ECC		= 3;
	private static final int				CLR		= 4;
	private static final int				LP		= 5;
	private static final int				MLKNN	= 6;
	private static final int				INCLAB	= 7;
	
	private static final int				BR		= 8;
	
	private static Vector<Vector<String>>	groupvec;
	
	private static final int				RF		= 1;
	
	public static Vector<Attribute> addDelAtt(Vector<Attribute> deletes, MultiLabelInstances mli, Capabilities cap) {
		for (final int i : mli.getLabelIndices()) {// = 0; i <
													// mli.getDataSet().numAttributes();
													// i++) {
			
			if (deletes.contains(mli.getDataSet().attribute(i))) continue;
			
			final AttributeStats as = mli.getDataSet().attributeStats(i);
			final Instances cp = new Instances(mli.getDataSet());
			cp.setClassIndex(i);
			
			// for (int k = 0; k < cp.numInstances(); k++) {
			// found0 = found0 || cp.instance(k).value(i)==0;
			// found1 = found1 || cp.instance(k).value(i)==1;
			
			// if (found1 && found0) {
			// break;
			// }
			
			// }
			
			if (!cap.test(cp) || // {// mli.getDataSet().attribute(i), true) ){
					// as.missingCount > mli.getDataSet().numInstances() - 2 ||
					as.distinctCount < 2 // ||
			// (biodegrules &&
			// (mli.getDataSet().numInstances()- as.missingCount < 35 ||
			// ((double)as.nominalCounts[1])/((double)(as.nominalCounts[0]+as.nominalCounts[1]))
			// < 0.15))
			) {
				System.err.println(as.distinctCount);
				
				System.err.println("Cannot use label " + mli.getDataSet().attribute(i).name() + " - missing: "
						+ as.missingCount + " - distinct: " + as.distinctCount);
				
				if (cap.getFailReason() != null) System.err.println(cap.getFailReason().getMessage());
				
				deletes.add(mli.getDataSet().attribute(i));
				
			}
			
		}
		return deletes;
		
	}
	
	public static MultiLabelInstances clean(MultiLabelInstances mli, int numfolds, Capabilities cap) throws Exception {
		
		Vector<Attribute> deletes = new Vector<>();
		
		// deletes = addDelAtt(deletes, mli, cap);
		
		int iterates = 1;
		
		System.err.println("folds: " + numfolds + " inst: " + mli.getDataSet().numInstances());
		
		if (numfolds < mli.getDataSet().numInstances()) iterates = 50;
		
		for (int j = 0; j < iterates; j++) {
			
			final Instances inst = mli.getDataSet();
			if (numfolds < mli.getDataSet().numInstances()) inst.randomize(new Random());
			
			for (int i = 0; i < numfolds; i++) {
				
				final Instances train = inst.trainCV(numfolds, i);
				final MultiLabelInstances trainMli = new MultiLabelInstances(train, mli.getLabelsMetaData());
				
				deletes = addDelAtt(deletes, trainMli, cap);
				
			}
		}
		
		final Instances resInst = mli.getDataSet();
		final LabelsMetaDataImpl lmd = (LabelsMetaDataImpl) mli.getLabelsMetaData();
		
		final Remove remove = new Remove();
		final int[] delarr = new int[deletes.size()];
		System.err.println("Before all, we have " + resInst.numAttributes() + " attributes and " + lmd.getNumLabels()
				+ " labels in meta data and want to remove " + deletes.size() + ".");
		int count = 0;
		for (final Attribute att : deletes) {
			delarr[count] = att.index();
			// }
			lmd.removeLabelNode(att.name());
			count++;
		}
		
		System.err.println("Before filtering, we have " + resInst.numAttributes() + " attributes and "
				+ lmd.getNumLabels() + " labels in meta data and want to remove " + deletes.size() + ".");
		
		remove.setAttributeIndicesArray(delarr);
		remove.setInvertSelection(false);
		remove.setInputFormat(resInst);
		final Instances instNew = Filter.useFilter(resInst, remove);
		
		System.err.println("After removing, we have " + instNew.numAttributes() + " attributes and "
				+ lmd.getNumLabels() + " labels in meta data and wanted to remove " + deletes.size() + ".");
		
		final MultiLabelInstances result = new MultiLabelInstances(instNew, lmd);
		
		System.err.println("After all, we have " + instNew.numAttributes() + " attributes and " + lmd.getNumLabels()
				+ " labels in meta data and wanted to remove " + deletes.size() + ".");
		
		return result;
	}
	
	public static String getModelPath(int fold, String folder) {
		return folder + "/fold_" + fold;
	}
	
	@SuppressWarnings ({ "rawtypes", "unused", "resource", "unchecked" })
	public static void main(String[] args) throws Exception {
		
		final boolean biodegfilter = Utils.getFlag("biodeg", args);
		
		final String groupFile = Utils.getOption("groups", args);
		
		groupvec = new Vector<Vector<String>>();
		
		final BufferedReader br = new BufferedReader(new FileReader(new File(groupFile)));
		
		String line = br.readLine();
		while (line != null) {
			final Vector<String> group = new Vector<String>();
			
			final String rules = line.split("=")[1];
			
			final String[] bts = rules.split(",");
			
			for (final String bt : bts)
				group.add(bt);
			
			groupvec.add(group);
			
			line = br.readLine();
			System.err.println("group: " + group);
		}
		
		String resfolder = Utils.getOption("results", args);
		
		final String modelfolder = Utils.getOption("models", args);
		
		if (Utils.getFlag("readstats", args)) {
			
			readStats(new File(resfolder), biodegfilter);
			
			return;
		}
		
		String dsbase = Utils.getOption("ds", args);
		
		if (dsbase.isEmpty()) dsbase = "/home/wicker/workspace/multilabel/data/all_ml";
		
		final boolean impute = !Utils.getFlag("noimpute", args);
		
		final boolean clean = !Utils.getFlag("noclean", args);
		
		int basecl = -1;
		int mlcl = -1;
		
		basecl = Integer.parseInt(Utils.getOption("basecl", args));
		mlcl = Integer.parseInt(Utils.getOption("mlcl", args));
		
		MultiLabelInstances mli = new MultiLabelInstances(dsbase + ".arff", dsbase + ".xml");
		
		int numfolds = Integer.parseInt(Utils.getOption("numfolds", args));
		
		if (numfolds < 0) numfolds = mli.getDataSet().numInstances();
		
		System.err.println("======================> Using " + numfolds + " folds");
		
		if (clean) {
			
			Capabilities cap = null;
			
			switch (basecl) {
			case RF:
				final RandomForest baserf = new RandomForest();
				baserf.setNumFeatures(mli.getDataSet().numAttributes() / 10);
				cap = baserf.getCapabilities();// new Capabilities(baserf);
				break;
			default:
				
				final GridSearch gs = new GridSearch();
				cap = gs.getCapabilities();// new Capabilities(gs);
				
			}
			
			mli = clean(mli, numfolds, cap);
		}
		
		final FastVector[] predictions = new FastVector[mli.getLabelsMetaData().getNumLabels()];
		
		final FastVector<NominalPrediction> allpredictions = new FastVector<NominalPrediction>();
		
		final Vector<PredictionCouple> couples = new Vector<>();
		
		final Vector<Vector<PredictionCouple>> allCouples = new Vector<>();
		
		final Instances inst = mli.getDataSet();
		
		if (numfolds < mli.getDataSet().numInstances()) inst.randomize(new Random());
		
		final MissingCapableEvaluator evaluator = new MissingCapableEvaluator();
		
		final MultipleEvaluation meval = new MultipleEvaluation();
		
		int countin = 0;
		
		while (countin < mli.getNumLabels()) {
			allCouples.add(new Vector<PredictionCouple>());
			countin++;
		}
		
		for (int i = 0; i < numfolds; i++) {
			
			System.err.println("Currently in fold " + i);
			
			final Instances train = inst.trainCV(numfolds, i);
			final Instances test = inst.testCV(numfolds, i);
			
			final MultiLabelInstances trainMli = new MultiLabelInstances(train, mli.getLabelsMetaData());
			
			final MultiLabelInstances testMli = new MultiLabelInstances(test, mli.getLabelsMetaData());
			
			MultiLabelLearner mlc;
			
			// Save model + test instance
			
			if (!new File(getModelPath(i, modelfolder)).exists()) {
				
				mlc = train(trainMli, impute, basecl, mlcl);
				
				if (mlc == null) continue;
				
				final FileOutputStream fos = new FileOutputStream(getModelPath(i, modelfolder));
				final ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(mlc);
				out.close();
				
			} else {
				System.err.println("Reading " + getModelPath(i, modelfolder) + "...");
				final FileInputStream fis = new FileInputStream(getModelPath(i, modelfolder));
				
				final ObjectInputStream in = new ObjectInputStream(fis);
				mlc = (MultiLabelLearner) in.readObject();
				in.close();
				System.err.println("Reading " + getModelPath(i, modelfolder) + "... Done!");
			}
			
			for (int j = 0; j < testMli.getDataSet().numInstances(); j++) {
				final Instance testInst = testMli.getDataSet().instance(j);
				
				final MultiLabelOutput mlo = mlc.makePrediction(testInst);
				
				for (int k = 0; k < predictions.length; k++) {
					double pred = -1.0;
					if (mlcl > 0) pred = mlo.getConfidences()[k];
					else {
						// note: cannot use getConfidences here as MLC-BMaD does
						// not
						// return confidences but only a bipartition
						final boolean predbool = mlo.getBipartition()[k];
						pred = predbool ? 1.0 : 0.0;
						
					}
					System.err.println("Evaluating label " + testInst.attribute(testMli.getLabelIndices()[k]).name());
					final double[] predarr = new double[] { 1 - pred, pred };
					if (testInst.isMissing(testMli.getLabelIndices()[k])
							|| !testInst.attribute(testMli.getLabelIndices()[k]).name().startsWith("REAL_")
							&& biodegfilter) {
						System.err.println("Will not use " + testInst.attribute(testMli.getLabelIndices()[k]).name());
						continue;
						
					}
					
					if (predictions[k] == null) predictions[k] = new FastVector();
					
					System.err.println(testInst.value(testMli.getLabelIndices()[k]) + " <========> " + predarr[0] + " "
							+ predarr[1]);
					
					if (biodegfilter)
						for (int l = 0; l < predictions.length; l++)
							if (!testInst.attribute(testMli.getLabelIndices()[l]).name().contains("REAL_")
									&& testInst.attribute(testMli.getLabelIndices()[k]).name().contains("REAL_")
									&& testInst.attribute(testMli.getLabelIndices()[k]).name().replaceAll("REAL_", "")
											.equals(testInst.attribute(testMli.getLabelIndices()[l]).name())) {
								final double predneg = mlo.getConfidences()[l];
								
								final double[] predarneg = new double[] { 1 - predneg, predneg };
								
								final NominalPrediction negp =
										new NominalPrediction(testInst.value(testMli.getLabelIndices()[l]), predarneg);
								final NominalPrediction posp =
										new NominalPrediction(testInst.value(testMli.getLabelIndices()[k]), predarr);
								
								final String name =
										testInst.attribute(testMli.getLabelIndices()[k]).name().replaceAll("REAL_", "");
								
								final AttributeStats stats =
										trainMli.getDataSet().attributeStats(testMli.getLabelIndices()[k]);
								
								final PredictionCouple pc = new PredictionCouple(posp, negp, name, stats, i);
								
								couples.add(pc);
								allCouples.get(k).add(pc);
								
								System.err.println(testInst.attribute(testMli.getLabelIndices()[k]).name() + " "
										+ testInst.attribute(testMli.getLabelIndices()[l]).name() + " "
										+ testInst.value(testMli.getLabelIndices()[k]) + " <========> " + predarr[0]
										+ " " + predarr[1] + " " + predarneg[0] + " " + predarneg[1]);
								
								break;
							}
					
					predictions[k].addElement(new NominalPrediction(testInst.value(testMli.getLabelIndices()[k]),
							predarr));
					
					allpredictions.addElement(new NominalPrediction(testInst.value(testMli.getLabelIndices()[k]),
							predarr));
					
				}
				
			}
			
			final Evaluation eval = evaluator.evaluate(mlc, testMli);
			
			eval.getMeasures();
			
			meval.addEvaluation(eval);
			
		}
		
		meval.calculateStatistics();
		
		if (resfolder.isEmpty()) resfolder = "/home/wicker/multilabel/results_biodeg/";
		
		ObjectOutputStream outputStream = null;
		try {
			
			// for (Measure mes : measures) {
			// try {
			// System.err.println(mes.getName() + " " +
			// meval.getMean(mes.getName()));
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			
			// }
			
			System.err.println(meval.toCSV());
			
			// outputStream =
			// new ObjectOutputStream(new FileOutputStream(resfolder + "all"));
			
			// outputStream.writeObject(meval);
			
			final ThresholdCurve tc = new ThresholdCurve();
			
			int count = 0;
			for (final FastVector singleres : predictions) {
				
				System.err.println("Writing " + resfolder + "label_" + count + ", which is "
						+ mli.getLabelsMetaData().getLabelNames().toArray(new String[] {})[count]);
				try {
					System.err.println("ROC: " + ThresholdCurve.getROCArea(tc.getCurve(singleres)));
					
				} catch (final Exception e) {
					e.printStackTrace();
				}
				
				outputStream = new ObjectOutputStream(new FileOutputStream(resfolder + "label_" + count));
				
				outputStream.writeObject(singleres);
				count++;
			}
			count = 0;
			for (final Vector<PredictionCouple> singlecoup : allCouples) {
				
				outputStream = new ObjectOutputStream(new FileOutputStream(resfolder + "couple_label_" + count));
				
				outputStream.writeObject(singlecoup);
				count++;
				
			}
			
			outputStream = new ObjectOutputStream(new FileOutputStream(resfolder + "couple_" + count));
			
			outputStream.writeObject(couples);
			count++;
			
			System.err.println("Total ROC: " + ThresholdCurve.getROCArea(tc.getCurve(allpredictions)));
			
		} catch (final FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (final IOException ex) {
			ex.printStackTrace();
		} finally {
			
			try {
				if (outputStream != null) {
					outputStream.flush();
					outputStream.close();
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings ("resource")
	public static void readStats(File statfile, boolean biodegfilter) throws Exception {
		
		System.err.println("Reading " + statfile.getName() + "...");
		
		if (biodegfilter) {
			
			// File mapper = new File(statFile.toString() + "labelmapper" );
			
			// BufferedReader bre = new BufferedReader(new FileReader(mapper));
			
			// String line = bre.readLine();
			
			// while (line != null) {
			// // name:label
			// String file = line.split(":")[0];
			// String label = line.split(":")[1];
			
			// if (label.equals) {
			
			// }
			
			// line = bre.readLine();
			// }
			
			for (final String file : statfile.list())
				if (!file.contains("label") && file.contains("couple")) {
					System.err.println("Using " + file);
					
					final ObjectInputStream in =
							new ObjectInputStream(new FileInputStream(statfile.toString() + "/" + file));
					final Vector<PredictionCouple> cp = (Vector<PredictionCouple>) in.readObject();
					System.err.println("Number predictions: " + cp.size());
					in.close();
					
					if (cp.size() == 0) continue;
					
					final FastVector<NominalPrediction> max = new FastVector<>();
					final FastVector<NominalPrediction> avg = new FastVector<>();
					final FastVector<NominalPrediction> min = new FastVector<>();
					final FastVector<NominalPrediction> frac = new FastVector<>();
					final FastVector<NominalPrediction> count = new FastVector<>();
					final FastVector<NominalPrediction> frac2 = new FastVector<>();
					final FastVector<NominalPrediction> diff = new FastVector<>();
					final FastVector<NominalPrediction> def = new FastVector<>();
					
					final FastVector<NominalPrediction> maxormin = new FastVector<>();
					final FastVector<NominalPrediction> groups = new FastVector<>();
					final FastVector<NominalPrediction> signal = new FastVector<>();
					final FastVector<NominalPrediction> groupsignal = new FastVector<>();
					
					final FastVector<NominalPrediction> cheat = new FastVector<>();
					
					for (final PredictionCouple cpi : cp) {
						
						final NominalPrediction real = cpi.getReal();
						
						final NominalPrediction second = cpi.getSecond();
						
						final AttributeStats stats = cpi.getStats();
						
						final double[] predarpos = real.distribution();
						final double[] predarneg = second.distribution();
						
						final double[] predmax = new double[2];
						final double[] predavg = new double[2];
						final double[] predmin = new double[2];
						final double[] predfrac = new double[2];
						final double[] predcount = new double[2];
						final double[] predfrac2 = new double[2];
						final double[] preddiff = new double[2];
						final double[] preddef = new double[2];
						
						final double[] predmaxormin = new double[2];
						final double[] predgroups = new double[2];
						final double[] predsig = new double[2];
						final double[] predgroupsig = new double[2];
						
						final double[] predcheat = new double[2];
						
						System.err.println(predarpos[0] + " " + predarpos[1] + " " + predarneg[0] + " " + predarneg[1]
								+ " =====================> " + real.actual());
						
						// 0: default
						
						preddef[1] = predarpos[1];
						preddef[0] = predarpos[0];
						
						// 1: max
						
						predmax[1] = Math.max(predarpos[1], predarneg[0]);
						predmax[0] = 1.0 - predmax[1];
						
						// 2: avg
						
						predavg[0] = (predarpos[0] + predarneg[1]) / 2.0;
						predavg[1] = (predarpos[1] + predarneg[0]) / 2.0;
						
						// 3: min
						
						predmin[1] = Math.min(predarpos[1], predarneg[0]);
						predmin[0] = 1.0 - predmin[1];
						
						// 4: fraction
						
						predfrac[1] = predarpos[1] + predarneg[0] / predarpos[0] + predarneg[1];
						predfrac[0] = 1.0 - predfrac[1];
						
						// 5: count
						
						predcount[1] = predarpos[1] >= 0.5 ? predarneg[0] >= 0.5 ? 1.0 : 0.5 : 0.0;
						predcount[0] = 1 - predcount[1];
						
						// 6: fraction 2
						
						predfrac2[1] = predarpos[1] * predarneg[0];
						predfrac2[0] = 1 - predfrac2[1];
						
						// 7: difference
						
						preddiff[1] = Math.abs(predarpos[1] - predarneg[0]);
						preddiff[0] = 1 - preddiff[1];
						
						// 8: maxormin
						
						if ((double) Math.abs(stats.nominalCounts[0] - stats.nominalCounts[1])
								/ (double) (stats.nominalCounts[0] + stats.nominalCounts[1]) > 0.66) {
							
							if (stats.nominalCounts[0] < stats.nominalCounts[1]) predmaxormin[1] = predmax[1];
							else predmaxormin[1] = predmin[1];
							
							predmaxormin[0] = 1 - predmaxormin[1];
						} else predmaxormin[1] = predavg[1];
						// if (real.actual() == 1.0) { \
						// //if (stats.nominalCounts[0]<stats.nominalCounts[1])
						// { \
						// \
						// predmaxormin[1] =
						// Math.max(Math.max(Math.max(Math.max(Math.max(predmaxormin[1]
						// ,preddef[1]),predfrac2[1]),predmin[1]),predavg[1]),predmax[1]);
						// \
						// }else { \
						// predmaxormin[1] =
						// Math.min(Math.min(Math.min(Math.min(Math.min(predmaxormin[1]
						// ,preddef[1]),predfrac2[1]),predmin[1]),predavg[1]),predmax[1]);
						// \
						// } \
						// \
						// predmaxormin[0] = 1.0-predmaxormin[1];
						predmaxormin[0] = 1 - predmaxormin[1];
						
						// 9 groups
						
						double groupprob = predarpos[1];
						int groupcount = 1;
						
						for (final Vector<String> group : groupvec) {
							if (!group.contains(cpi.getName())) continue;
							
							groupprob = 0.0;
							groupcount = 0;
							
							for (final PredictionCouple innercpi : cp) {
								if (!(group.contains(innercpi.getName()) && innercpi.getFold() == cpi.getFold()))
									continue;
								
								groupprob += innercpi.getReal().distribution()[1];
								groupcount++;
							} // end of for ()
							
							break;
						}
						System.err.println(groupprob + " <^^^^^^^^^^^^^^^^^^^> " + groupcount);
						groupprob /= groupcount;
						
						predgroups[1] = groupprob;
						predgroups[0] = 1.0 - groupprob;
						
						// 10 strongest signal
						
						if (Math.abs(predarpos[1] - 0.5) > Math.abs(predarneg[0] - 0.5)) predsig[1] = predarpos[1];
						else predsig[1] = predarneg[0];
						
						predsig[0] = 1.0 - predsig[1];
						
						// 11 groupsig
						
						predgroupsig[1] = predsig[1];
						predgroupsig[0] = predsig[0];
						
						groupprob = predsig[1];
						groupcount = 1;
						
						if (Math.abs(predsig[1] - 0.5) < 0.3) {
							
							for (final Vector<String> group : groupvec) {
								if (!group.contains(cpi.getName())) continue;
								
								groupprob = 0.0;
								groupcount = 0;
								
								for (final PredictionCouple innercpi : cp) {
									if (!(group.contains(innercpi.getName()) && innercpi.getFold() == cpi.getFold()))
										continue;
									
									if (Math.abs(predarpos[1] - 0.5) > Math.abs(predarneg[0] - 0.5)) groupprob +=
											predarpos[1];
									else groupprob += predarneg[0];
									
									groupcount++;
								} // end of for ()
								
								break;
							}
							
							predgroupsig[1] = groupprob / groupcount;
							predgroupsig[0] = 1 - predgroupsig[1];
						} // end of if ()
						
						// -1: cheat
						
						if (real.actual() == 1.0) predcheat[1] =
								Math.max(Math.max(Math.max(
										Math.max(Math.max(predmaxormin[1], preddef[1]), predfrac2[1]), predmin[1]),
										predavg[1]), predmax[1]);
						else predcheat[1] =
								Math.min(Math.min(Math.min(
										Math.min(Math.min(predmaxormin[1], preddef[1]), predfrac2[1]), predmin[1]),
										predavg[1]), predmax[1]);
						
						predcheat[0] = 1.0 - predcheat[1];
						
						System.err.println(predmax[0] + " <=max=> " + predmax[1]);
						System.err.println(predavg[0] + " <=avg=> " + predavg[1]);
						System.err.println(predmin[0] + " <=min=> " + predmin[1]);
						System.err.println(predfrac[0] + " <=frac=> " + predfrac[1]);
						System.err.println(predcount[0] + " <=count=> " + predcount[1]);
						System.err.println(predfrac2[0] + " <=frac2=> " + predfrac2[1]);
						System.err.println(preddiff[0] + " <=diff=> " + preddiff[1]);
						System.err.println(preddef[0] + " <=def=> " + preddef[1]);
						System.err.println(predmaxormin[0] + " <=maxormin=> " + predmaxormin[1]);
						System.err.println(predgroups[0] + " <=group=> " + predgroups[1]);
						System.err.println(predsig[0] + " <=signal=> " + predsig[1]);
						System.err.println(predsig[0] + " <=groupsignal=> " + predsig[1]);
						
						System.err.println(predcheat[0] + " <=cheat=> " + predcheat[1]);
						
						max.add(new NominalPrediction(real.actual(), predmax));
						avg.add(new NominalPrediction(real.actual(), predavg));
						min.add(new NominalPrediction(real.actual(), predmin));
						frac.add(new NominalPrediction(real.actual(), predfrac));
						count.add(new NominalPrediction(real.actual(), predcount));
						frac2.add(new NominalPrediction(real.actual(), predfrac2));
						diff.add(new NominalPrediction(real.actual(), preddiff));
						def.add(new NominalPrediction(real.actual(), preddef));
						maxormin.add(new NominalPrediction(real.actual(), predmaxormin));
						groups.add(new NominalPrediction(real.actual(), predgroups));
						signal.add(new NominalPrediction(real.actual(), predsig));
						groupsignal.add(new NominalPrediction(real.actual(), predgroupsig));
						
						cheat.add(new NominalPrediction(real.actual(), predcheat));
						
					}
					
					System.err.println("=============================== ROC start ===================================");
					
					ThresholdCurve tc = new ThresholdCurve();
					
					Instances resultinst = tc.getCurve(def);
					
					System.err.println("ROC def: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(max);
					
					System.err.println("ROC max: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(avg);
					
					System.err.println("ROC avg: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(min);
					
					System.err.println("ROC min: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(frac);
					
					System.err.println("ROC frac: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(count);
					
					System.err.println("ROC count: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(frac2);
					
					System.err.println("ROC frac2: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(diff);
					
					System.err.println("ROC diff: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(maxormin);
					
					System.err.println("ROC minormax: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(groups);
					
					System.err.println("ROC groups: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(signal);
					
					System.err.println("ROC sig: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(groupsignal);
					
					System.err.println("ROC groupsig: " + ThresholdCurve.getROCArea(resultinst));
					
					tc = new ThresholdCurve();
					
					resultinst = tc.getCurve(cheat);
					
					System.err.println("ROC cheat: " + ThresholdCurve.getROCArea(resultinst));
					
					System.err.println("=============================== ROC end ===================================");
				}
			
			// ObjectInputStream in = new ObjectInputStream(new
			// FileInputStream(statfile.getName()));
			// FastVector predictions = (FastVector)in.readObject();
			// in.close();
			
			return;
		}
		
		final ObjectInputStream in = new ObjectInputStream(new FileInputStream(statfile.getName()));
		final FastVector<NominalPrediction> predictions = (FastVector<NominalPrediction>) in.readObject();
		in.close();
		
		final ThresholdCurve tc = new ThresholdCurve();
		
		final Instances resultinst = tc.getCurve(predictions);
		
		System.err.println("ROC: " + ThresholdCurve.getROCArea(resultinst));
		
		final ArffSaver saver = new ArffSaver();
		saver.setInstances(resultinst);
		saver.setFile(new File(statfile.getName() + ".arff"));
		saver.writeBatch();
		
	}
	
	public static MultiLabelLearner train(MultiLabelInstances mli, boolean impute, int baselearner, int mllearner)
			throws Exception {
		
		boolean notimputed = true;
		
		Classifier base;
		
		switch (baselearner) {
		case RF:
			
			System.err.println("Using random forest");
			
			final RandomForest baserf = new RandomForest();
			
			baserf.setNumFeatures(mli.getDataSet().numAttributes() / 10);
			base = baserf;
			break;
		
		default:
			System.err.println("Using default base classifier");
			final GridSearch gs = new GridSearch();
			
			gs.setEvaluation(new SelectedTag(GridSearch.EVALUATION_ACC, GridSearch.TAGS_EVALUATION));
			
			final SMO smo = new SMO();
			smo.setBuildLogisticModels(true);
			smo.setKernel(new RBFKernel());
			
			gs.setFilter(new AllFilter());
			gs.setClassifier(smo);
			gs.setYProperty("classifier.confidenceFactor");
			// use all cores
			gs.setNumExecutionSlots(20);// Runtime.getRuntime().availableProcessors());
			gs.setLogFile(new File("gs1.log"));
			
			gs.setXBase(10.0);
			gs.setXMin(-2);
			gs.setXMax(2);
			gs.setXStep(1);
			
			gs.setXProperty("classifier.c");
			gs.setXExpression("pow(BASE,I)");
			
			gs.setYBase(10.0);
			gs.setYMin(-2);
			gs.setYMax(2);
			gs.setYStep(1);
			
			gs.setYProperty("classifier.kernel.gamma");
			gs.setYExpression("pow(BASE,I)");
			
			final FilteredClassifier fc = new FilteredClassifier();
			
			final AttributeSelection as = new AttributeSelection();
			
			// Ranker rank = new Ranker();
			
			// rank.setNumToSelect(75);
			
			final ReliefFAttributeEval relief = new ReliefFAttributeEval();
			
			final BestFirst bf = new BestFirst();
			
			bf.setSearchTermination(20);
			
			as.setEvaluator(relief);
			
			as.setSearch(bf);
			
			fc.setClassifier(gs);
			
			if (impute) {
				System.err.println("Filling misssing values...");
				
				MultiLabelInstances mliFilled;
				
				final EnsembleOfClassifierChainsFiller filler =
						new EnsembleOfClassifierChainsFiller(AbstractClassifier.makeCopy(fc), 10);
				mliFilled = filler.fillMissing(mli);
				notimputed = false;
				mli = mliFilled;
				
				System.err.println("Filling misssing values done!");
				
			}
			
			gs.setNumExecutionSlots(Runtime.getRuntime().availableProcessors());
			
			fc.setClassifier(gs);
			
			base = fc;
			
			break;
		}
		
		if (impute && notimputed) {
			System.err.println("Filling misssing values....");
			
			MultiLabelInstances mliFilled;
			
			final EnsembleOfClassifierChainsFiller filler =
					new EnsembleOfClassifierChainsFiller(AbstractClassifier.makeCopy(base), 10);
			mliFilled = filler.fillMissing(mli);
			
			mli = mliFilled;
			
			System.err.println("Filling misssing values done!");
			
		} else {
			final Instances data = mli.getDataSet();
			
			final ReplaceMissingValues rmv = new ReplaceMissingValues();
			
			rmv.setInputFormat(data);
			
			final Instances newData = Filter.useFilter(data, rmv);
			
			mli.reintegrateModifiedDataSet(newData);
			
		}
		
		Vector<Attribute> rems = new Vector<>();
		rems = addDelAtt(rems, mli, base.getCapabilities());
		System.err.println(rems.size() + " attributes make problem!");
		
		System.err.println("Learning model on data set... ");
		System.err.println("NumLabels:    " + mli.getNumLabels());
		System.err.println("NumInstances: " + mli.getNumInstances());
		System.err.println("Cardinality:  " + mli.getCardinality());
		
		try {
			
			switch (mllearner) {
			case LP:
				final LabelPowerset lp = new LabelPowerset(base);
				lp.build(mli);
				return lp;
				
			case MLKNN:
				final MLkNN mlk = new MLkNN();
				mlk.build(mli);
				return mlk;
				
			case INCLAB:
				final IncludeLabelsClassifier il = new IncludeLabelsClassifier(base);
				il.build(mli);
				return il;
				
			case ECC:
				final EnsembleOfClassifierChains ecc = new EnsembleOfClassifierChains(base, 10, true, true);
				// ClassifierChain cc = new ClassifierChain(base);
				
				ecc.build(mli);
				return ecc;
			case CC:
				// EnsembleOfClassifierChains cc = new
				// EnsembleOfClassifierChains(base, 10, true,true);
				final ClassifierChain cc = new ClassifierChain(base);
				cc.build(mli);
				return cc;
			case BR2:
				
				System.err.println("Using BR2");
				
				final MultiLabelStacking br2 = new MultiLabelStacking(base, base);
				
				br2.build(mli);
				
				return br2;
				
			case BR:
				
				System.err.println("Using BR");
				
				final BinaryRelevance br = new BinaryRelevance(base);
				
				br.build(mli);
				
				return br;
				
			case CLR:
				
				System.err.println("Using CLR");
				
				final CalibratedLabelRanking clr = new CalibratedLabelRanking(base);
				
				clr.build(mli);
				
				return clr;
				
			default:
				
				final MLCMAD mlcbmad = new MLCMAD(AbstractClassifier.makeCopy(base));
				mlcbmad.setThreading(true);
				mlcbmad.setDebug(true);
				
				mlcbmad.setExternalDecompCommand("resources/doDBP.sh $IN $OUT1 $TPAR $KPAR $OUT2");
				mlcbmad.setOptPar(true);
				// mlcbmad.setK(4);
				// mlcbmad.setT(0.5);
				
				mlcbmad.build(mli);
				
				return mlcbmad;
				
			}
			
		} catch (final WekaException e) {
			return null;
		}
		
		// BinaryRelevance br = new
		// BinaryRelevance(AbstractClassifier.makeCopy(fc));
		
		// br.build(mliFilled);
		
		// return br;
		
		// HOMER homer = new HOMER(mlcbmad, 10,
		// HierarchyBuilder.Method.BalancedClustering);
		
		// homer.build(mliFilled);
		
		// return homer;
		
	}
	
}