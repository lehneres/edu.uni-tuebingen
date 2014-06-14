/*
 * Mulan-TUM Copyright (C) 2009-2011 Joerg Wicker (joerg.wicker@in.tum.de) This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.tum.in.mulan.classifier.transformation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import mulan.classifier.MultiLabelOutput;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.MultiLabelStacking;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.LabelNode;
import mulan.data.LabelNodeImpl;
import mulan.data.LabelsMetaDataImpl;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.meta.GridSearch;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.SparseToNonSparse;
import de.tum.in.mulan.evaluation.MissingCapableEvaluator;

/**
 * Describe class <code>MLCMAD</code> here.
 * 
 * @author <a href="mailto:wicker@uni-mainz.de">Joerg Wicker</a>
 * @version 1.0
 */

public class MLCMAD extends TransformationBasedMultiLabelLearner {
	
	private class Trainer implements Runnable {
		private final MultiLabelInstances	mli;
		private final MLCMAD				mad;
		private MultipleEvaluation			results;
		@SuppressWarnings ("hiding")
		private final int					k;
		@SuppressWarnings ("hiding")
		private final double				t;
		
		/**
		 * @param numFolds
		 */
		@SuppressWarnings ("javadoc")
		public Trainer(MLCMAD mad, MultiLabelInstances mli, int k, double t, int numFolds) {
			this.mad = mad;
			this.mli = mli;
			this.t = t;
			this.k = k;
		}
		
		public int getK() {
			return k;
		}
		
		public MultipleEvaluation getResults() {
			return results;
		}
		
		public double getT() {
			return t;
		}
		
		@Override
		public void run() {
			final MissingCapableEvaluator eval = new MissingCapableEvaluator();
			
			final int numFolds = 5;
			try {
				
				results = eval.crossValidate(mad, mli, Math.min(numFolds, mli.getDataSet().numInstances()));
				
			} catch (final Exception e) {
				System.err
						.println("Parameter Setting k = " + k + " and t = " + t + " did not work, error message was:");
				e.printStackTrace();
				
			}
			
		}
		
	}
	
	@SuppressWarnings ("javadoc")
	public static void main(String[] args) throws Exception {
		final String datasetbase = Utils.getOption("dataset", args);
		
		final MultiLabelInstances dataset = new MultiLabelInstances(datasetbase + ".arff", datasetbase + ".xml");
		final weka.classifiers.functions.SMO smo1 = new weka.classifiers.functions.SMO();
		
		smo1.setBuildLogisticModels(true);
		
		final MLCMAD mlcmad = new MLCMAD(smo1);
		mlcmad.setDebug(true);
		for (double t = 0.9; t >= 0.1; t -= 0.1)
			for (int k = dataset.getLabelIndices().length; k >= 2; k--) {
				mlcmad.setExternalDecompCommand("resources/doDBP.sh $IN $OUT1 " + t + " " + k + " $OUT2");
				final Evaluator eval = new Evaluator();
				final MultipleEvaluation res = eval.crossValidate(mlcmad, dataset, 3);
				System.out.println("\n======\nt=" + t + "\nk=" + k + "\n" + res.toString());
			}
	}
	
	private double			error;
	
	private boolean			optpar;
	private BinaryRelevance	basebr;
	@SuppressWarnings ("hiding")
	private Classifier		baseClassifier;
	private String			externalDecompCommand	= "";
	private Instances		featuresAndDecomp;
	private boolean			threading;
	private int[]			labelsdecomp;
	
	private Instances		uppermatrix;
	
	private int				k;
	private double			t;
	
	/**
	 * An empty constructor
	 */
	public MLCMAD() {
		// empty
	}
	
	/**
	 * A constructor with 2 arguments
	 * 
	 * @param baseClassifier
	 *            the classifier used in the base-level
	 * @throws Exception
	 */
	public MLCMAD(Classifier baseClassifier) throws Exception {
		super(baseClassifier);
		this.baseClassifier = baseClassifier;
	}
	
	/**
	 * Builds the classifier.
	 * 
	 * @param trainingSet
	 * @throws Exception
	 */
	@SuppressWarnings ({ "hiding", "resource", "unused" })
	@Override
	protected void buildInternal(MultiLabelInstances trainingSet) throws Exception {
		
		Instances train = new Instances(trainingSet.getDataSet());
		
		if (trainingSet.getDataSet().numInstances() < 2) {
			optpar = false;
			t = 0.8;
			k = Math.min(trainingSet.getNumLabels(), 28);
		}
		
		if (optpar) {
			int topk = -1;
			double topt = -1.0;
			double topacc = -1.0;
			System.err.println("Optimizing parameters...");
			final Vector<Trainer> threads = new Vector<>();
			final ExecutorService execSvc = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			final int numFolds = 5;
			
			for (int k = 2; k <= Math.min(64, trainingSet.getNumLabels()); k++)
				for (double t = 0.1; t < 0.99; t += 0.1) {
					System.err.println("Trying k = " + k + " t = " + t);
					final MLCMAD mlcbmad = (MLCMAD) makeCopy();
					mlcbmad.setOptPar(false);
					mlcbmad.setK(k);
					mlcbmad.setT(t);
					if (threading) {
						final Trainer tr = new Trainer(mlcbmad, trainingSet, k, t, numFolds);
						
						execSvc.execute(tr);
						
						threads.add(tr);
					} else {
						final MissingCapableEvaluator eval = new MissingCapableEvaluator();
						MultipleEvaluation results;
						
						try {
							
							results =
									eval.crossValidate(mlcbmad, trainingSet,
											Math.min(numFolds, trainingSet.getDataSet().numInstances()));
							final double acc = results.getMean("Example-Based Accuracy");
							System.err.println("Accuracy is: " + acc);
							if (acc > topacc) {
								System.err.println("New top Accuracy!!! " + topacc + " => " + acc);
								topacc = acc;
								topt = t;
								topk = k;
							}
							
						} catch (final Exception e) {
							System.err.println("Parameter Setting k = " + k + " and t = " + t
									+ " did not work, error message was:");
							e.printStackTrace();
							
						}
					}
				}
			if (threading) {
				execSvc.awaitTermination(25, TimeUnit.MINUTES);
				
				double acc = -1.0;
				double t = -1.0;
				int k = -1;
				for (final Trainer tr : threads) {
					acc = tr.getResults().getMean("Example-Based Accuracy");
					t = tr.getT();
					k = tr.getK();
				}
				
				System.err.println("Accuracy is: " + acc);
				if (acc > topacc) {
					System.err.println("New top Accuracy!!! " + topacc + " => " + acc);
					topacc = acc;
					topt = t;
					topk = k;
				}
				
			}
			
			System.err.println("Optimizing parameters done! k = " + topk + " and t = " + topt);
			setK(topk);
			setT(topt);
		}
		
		System.err.println("Learning model...");
		System.err.println("Parameter Setting k = " + k + " and t = " + t + " ...");
		
		// remove the features, so we make a matrix decomposition only of
		// the labels
		
		final Remove rem0 = new Remove();
		final int[] features0 = trainingSet.getFeatureIndices();
		rem0.setAttributeIndicesArray(features0);
		rem0.setInputFormat(train);
		train = Filter.useFilter(train, rem0);
		
		Instances decompData;
		
		// lets do the decomposition
		final Random ran = new Random();
		
		// first save the arff in non sparse form
		
		final SparseToNonSparse spfilter = new SparseToNonSparse();
		spfilter.setInputFormat(train);
		final Instances out = Filter.useFilter(train, spfilter);
		
		final ArffSaver saver = new ArffSaver();
		saver.setInstances(out);
		
		// generate the temporary file names
		final String filenamein = "tmpin" + System.currentTimeMillis() + "" + ran.nextInt();
		final String filenameout1 = "tmpout1" + System.currentTimeMillis() + "" + ran.nextInt() + ".arff";
		
		final String filenameout2 = "tmpout2" + System.currentTimeMillis() + "" + ran.nextInt() + ".arff";
		
		// save labels
		
		saver.setFile(new File(filenamein));
		saver.writeBatch();
		
		// we need a newline at the end of the file (weka doesn't do that)
		
		final BufferedWriter bw = new BufferedWriter(new FileWriter(filenamein, true));
		bw.write("\n");
		bw.close();
		
		// execute external program for the decomposition
		
		final Process p =
				Runtime.getRuntime().exec(
						externalDecompCommand.replaceAll("\\$IN", filenamein).replaceAll("\\$OUT1", filenameout1)
								.replaceAll("\\$OUT2", filenameout2).replaceAll("\\$KPAR", k + "")
								.replaceAll("\\$TPAR", t + ""));
		p.waitFor();
		p.destroy();
		// and import the 2 matrixes
		
		final ArffLoader arff = new ArffLoader();
		arff.setSource(new File(filenameout1));
		decompData = arff.getDataSet();
		arff.setSource(new File(filenameout2));
		uppermatrix = arff.getDataSet();
		
		// generate a MultiLabelInstances object consisting of features and new
		// labels of decomposition
		
		// /////////////////////////////////////////////////////////////////////
		// FIXME calculate error here
		
		// MultiLabelOutput errorout = basebr.makePrediction(instance);
		
		// boolean[] errorbipartition = new
		// boolean[uppermatrix.numAttributes()];
		
		for (int i = 0; i < uppermatrix.numAttributes(); i++)
			for (int j = 0; j < decompData.numInstances(); j++) {
				boolean product = false;
				
				final int index1up = uppermatrix.attribute(i).value(0).equals("0") ? 1 : 0;
				
				for (int k = 0; k < decompData.numAttributes(); k++) {
					final int index1dec = decompData.attribute(k).value(0).equals("0") ? 1 : 0;
					
					product =
							product || decompData.instance(j).value(k) == index1dec
									&& uppermatrix.instance(k).value(j) == index1up;
				}
				final int index1mult = out.attribute(i).value(0).equals("0") ? 1 : 0;
				final boolean real = out.instance(j).value(i) == index1mult;
				
				final double[] pred = new double[2];
				
				pred[0] = product ? 0 : 1;
				pred[1] = product ? 1 : 0;
				
				final double realdoub = real ? 1.0 : 0.0;
				
				new NominalPrediction(realdoub, pred);
			}
		
		// get indices
		
		final int[] features = trainingSet.getFeatureIndices();
		
		final int[] decompindices = new int[decompData.numAttributes()];
		
		int countf = 0;
		for (int i = features.length; i < decompData.numAttributes() + features.length; i++) {
			decompindices[countf] = i;
			countf++;
		}
		labelsdecomp = decompindices;
		
		// get features from training set
		
		final Instances copied = new Instances(trainingSet.getDataSet());
		
		final Remove rem = new Remove();
		
		rem.setAttributeIndicesArray(features);
		rem.setInvertSelection(true);
		rem.setInputFormat(copied);
		
		final Instances onlyFeatures = Filter.useFilter(copied, rem);
		
		// merge features with matrix decomposition
		
		if (onlyFeatures.numInstances() != decompData.numInstances()) {
			// sthg went wrong when decomposing
			System.err.println("filenamein: "
					+ filenamein
					+ "\n"
					+ "filenameout1: "
					+ filenameout1
					+ "\n"
					+ "filenameout2: "
					+ filenameout2
					+ "\n"
					+ "command: "
					+ externalDecompCommand.replaceAll("\\$IN", filenamein).replaceAll("\\$OUT1", filenameout1)
							.replaceAll("\\$OUT2", filenameout2).replaceAll("\\$KPAR", k + "")
							.replaceAll("\\$TPAR", t + ""));
			throw new Exception("Problem when decomposing");
		}
		
		// and delete the temp files
		
		File f = new File(filenamein);
		f.delete();
		f = new File(filenameout1);
		f.delete();
		
		f = new File(filenameout2);
		f.delete();
		
		featuresAndDecomp = Instances.mergeInstances(onlyFeatures, decompData);
		
		final Instances trainset = featuresAndDecomp;
		
		final LabelsMetaDataImpl trainlmd = new LabelsMetaDataImpl();
		for (final int lab : labelsdecomp) {
			final LabelNode lni = new LabelNodeImpl(trainset.attribute(lab).name());
			trainlmd.addRootNode(lni);
		}
		
		final MultiLabelInstances trainMulti = new MultiLabelInstances(trainset, trainlmd);
		
		if (baseClassifier instanceof GridSearch && trainset.numInstances() < 10)
			baseClassifier = ((GridSearch) baseClassifier).getClassifier();
		
		// build br for decomposed label prediction
		
		basebr = new BinaryRelevance(baseClassifier);
		
		basebr.build(trainMulti);
		
		System.err.println("Model trained... all done.");
		
	}
	
	@SuppressWarnings ("javadoc")
	public double getDecompositionError() {
		
		return error;
	}
	
	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed information about the technical
	 * background of this class, e.g., paper reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	@Override
	public TechnicalInformation getTechnicalInformation() {
		// FIXME replace null output
		
		return null;
	}
	
	/**
	 * Returns a string describing classifier.
	 * 
	 * @return a description suitable for displaying in a future explorer/experimenter gui
	 */
	public String globalInfo() {
		return "";
	}
	
	@Override
	protected MultiLabelOutput makePredictionInternal(Instance instance) throws Exception {
		
		final MultiLabelOutput baseout = basebr.makePrediction(instance);
		
		final boolean[] bipartition = new boolean[uppermatrix.numAttributes()];
		
		for (int i = 0; i < bipartition.length; i++) {
			final int index1 = uppermatrix.attribute(i).value(0).equals("0") ? 1 : 0;
			for (int j = 0; j < baseout.getBipartition().length; j++) {
				final double matval = uppermatrix.instance(j).value(i);
				bipartition[i] = bipartition[i] || baseout.getBipartition()[j] && matval == index1;
			}
			
		}
		final MultiLabelOutput mlo = new MultiLabelOutput(bipartition);
		// debug("Output is: " + mlo);
		return mlo;
	}
	
	@SuppressWarnings ({ "javadoc", "resource" })
	public void saveObject(String filename) {
		try {
			final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
			out.writeObject(this);
		} catch (final IOException ex) {
			Logger.getLogger(MultiLabelStacking.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	/**
	 * @param com
	 */
	public void setExternalDecompCommand(String com) {
		// lets say $IN is input for programm, $OUT1, $OUT2 is output
		// $TPAR is t and $KPAR is k
		externalDecompCommand = com;
	}
	
	@SuppressWarnings ("javadoc")
	public void setK(int k) {
		this.k = k;
	}
	
	// public MultiLabelLearner makeCopy(){
	// MLCMAD result = null;
	// try {
	
	// result = new MLCMAD(this.baseClassifier);
	// result.setK(this.k);
	// result.setT(this.t);
	// result.setOptPar(this.optpar);
	// result.setExternalDecompCommand(this.externalDecompCommand);
	// } catch (Exception e) {
	// //return null
	// }
	
	// return result;
	// }
	
	@SuppressWarnings ("javadoc")
	public void setOptPar(boolean opt) {
		optpar = opt;
	}
	
	@SuppressWarnings ("javadoc")
	public void setT(double t) {
		this.t = t;
	}
	
	@SuppressWarnings ("javadoc")
	public void setThreading(boolean threading) {
		this.threading = threading;
	}
	
}
