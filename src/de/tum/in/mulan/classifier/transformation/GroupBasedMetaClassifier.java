/**
 * 
 */
package de.tum.in.mulan.classifier.transformation;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.transformation.ClassifierChain;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Utils;
import de.tum.in.mulan.classifier.meta.FilteredMLLearner;

/**
 * group based meta classifier. Contains a ensemble of models, which each model learned on a different cluster
 * 
 * @author LehnereS
 */
public abstract class GroupBasedMetaClassifier extends TransformationBasedMultiLabelLearner {
	
	protected static String getLabelFeatureSetsAsString(final int[][][] labelFeatureSets) {
		final StringBuilder string = new StringBuilder();
		string.append("\nfound label feature sets:\n");
		for (final int[][] labelFeatureSet : labelFeatureSets) {
			string.append("labelset:\t{ ");
			string.append(Utils.arrayToString(labelFeatureSet[0]));
			string.append("}\t->\tfeature set:\t{ ");
			string.append(Utils.arrayToString(labelFeatureSet[1]));
			string.append("}\n");
		}
		return string.toString();
	}
	
	private double				avgCardinality, avgDensity, avgLabels;
	private FilteredMLLearner[]	ensemble;
	private int[][][]			labelFeatureSets;
	protected Set<Integer>		labelIndicesSet;
	protected MultiLabelLearner	mlClassifier	= new ClassifierChain(baseClassifier);
	private double[]			avgPhi;
	
	private int					avgLabelCombCount;
	
	/**
     */
	public GroupBasedMetaClassifier() {
		super(new J48());
	}
	
	// /**
	// * computes statistics on filtered datasets
	// *
	// * @param data
	// * {@link MultiLabelInstances}
	// * @throws Exception
	// * weka exception
	// */
	// public void computeStatistics(final MultiLabelInstances data)
	// throws Exception {
	// avgCardinality = 0;
	// avgDensity = 0;
	// avgLabels = 0;
	// avgLabelCombCount = 0;
	// for (int i = 0; i < ensemble.length; i++) {
	// ensemble[i].computeStatsOnFilteredData(data);
	// debug("stats for ensemble #" + i + " "
	// + ensemble[i].getCardinality() + "\t"
	// + ensemble[i].getDensity());
	// avgCardinality += ensemble[i].getCardinality();
	// avgDensity += ensemble[i].getDensity();
	// avgLabels += ensemble[i].getNumLabels();
	// if (avgPhi == null) {
	// avgPhi = ensemble[i].getPhi();
	// } else {
	// avgPhi = de.tum.in.multilabel.Utils.sum(avgPhi,
	// ensemble[i].getPhi());
	// }
	// avgLabelCombCount += ensemble[i].getLabelCombCount();
	// }
	// avgCardinality /= ensemble.length;
	// avgDensity /= ensemble.length;
	// avgLabels /= ensemble.length;
	// avgLabelCombCount /= ensemble.length;
	// // avgPhi = de.tum.in.multilabel.Utils.div(avgPhi, ensemble.length);
	// }
	
	/**
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 */
	public GroupBasedMetaClassifier(final Classifier slClassifier) {
		super(slClassifier);
	}
	
	@Override
	protected void buildInternal(final MultiLabelInstances trainingSet) throws Exception {
		// trainingSet = checkAndFillMissingLabels(trainingSet);
		// saving all labels in a HashSet for fast index check
		labelIndicesSet = new HashSet<>(
		// converting int[] to List<Integer> to HashSet<Integer>
				de.tum.in.multilabel.Utils.arrayAsList(de.tum.in.multilabel.Utils.convertIntegerArray(labelIndices)));
		labelFeatureSets = findLabelFeatureSets(trainingSet);
		// checking for remaining labels and adding found to the labelFeatureSet
		labelFeatureSets = checkForRemainingLables(labelFeatureSets);
		// XXX enable again
		debug(GroupBasedMetaClassifier.getLabelFeatureSetsAsString(labelFeatureSets));
		ensemble = new FilteredMLLearner[labelFeatureSets.length];
		// learning model for each group using FilteredMLClassifier
		debug("building classifiers...");
		for (int i = 0; i < labelFeatureSets.length; i++) {
			// this.debug("building classifier #" + i + "...");
			ensemble[i] = new FilteredMLLearner(mlClassifier.makeCopy(), AbstractClassifier.makeCopy(baseClassifier));
			ensemble[i].setLabelIndices(labelFeatureSets[i][0]);
			ensemble[i].setFeaturesIndices(labelFeatureSets[i][1]);
			ensemble[i].setDebug(getDebug());
			ensemble[i].build(trainingSet);
			// this.debug("building classifier #" + i + "...done");
		}
		debug("building classifiers...done");
		// if (getDebug()) {
		// computeStatistics(trainingSet);
		// printStatistics();
		// }
		
	}
	
	/**
	 * checks groups for remaining labels and adds them as an extra group. All features will be used for this group
	 * 
	 * @param labelFeatureSets
	 * @return
	 */
	private int[][][] checkForRemainingLables(@SuppressWarnings ("hiding") final int[][][] labelFeatureSets) {
		final Set<Integer> remainingLabels = new HashSet<>(labelIndicesSet);
		// removing all labels covered in groups
		for (final int[][] labelFeatureSet : labelFeatureSets)
			remainingLabels.removeAll(de.tum.in.multilabel.Utils.arrayAsList(labelFeatureSet[0]));
		// if non labels remain returning the labelFeatureSets untouched
		if (!remainingLabels.isEmpty()) {
			// else adding the remaining labels with all features
			debug("remaining labels found");
			final int[][][] completedLabelFeatureSets = new int[labelFeatureSets.length + 1][2][];
			System.arraycopy(labelFeatureSets, 0, completedLabelFeatureSets, 0, labelFeatureSets.length);
			completedLabelFeatureSets[labelFeatureSets.length][0] =
			// converting Set<Integer> to int[]
					de.tum.in.multilabel.Utils.convertIntegerArray(remainingLabels.toArray(new Integer[remainingLabels
							.size()]));
			completedLabelFeatureSets[labelFeatureSets.length][1] = featureIndices;
			return completedLabelFeatureSets;
		}
		return labelFeatureSets;
	}
	
	protected int[][][] compactLabelFeatureSets(@SuppressWarnings ("hiding") final Set<Integer>[][] labelFeatureSets) {
		final int[][][] finalLabelFeatureSets = new int[labelFeatureSets.length][2][];
		int offset = 0; // if a cluster doens't contain any labels we dismiss it
		// and offset will be +1
		for (int currentClusterID = 0; currentClusterID < labelFeatureSets.length; currentClusterID++)
			if (labelFeatureSets[currentClusterID][0] != null && !labelFeatureSets[currentClusterID][0].isEmpty()) {
				// converting Set<Integer> to int[]
				finalLabelFeatureSets[currentClusterID - offset][0] =
						de.tum.in.multilabel.Utils.convertIntegerArray(labelFeatureSets[currentClusterID][0]
								.toArray(new Integer[labelFeatureSets[currentClusterID][0].size()]));
				// if a cluster contains only labels all features will be added
				if (labelFeatureSets[currentClusterID][1].isEmpty()) {
					debug("empty feature set found");
					finalLabelFeatureSets[currentClusterID - offset][1] = featureIndices;
				} else // converting Set<Integer> to int[]
				finalLabelFeatureSets[currentClusterID - offset][1] =
						de.tum.in.multilabel.Utils.convertIntegerArray(labelFeatureSets[currentClusterID][1]
								.toArray(new Integer[labelFeatureSets[currentClusterID][1].size()]));
			} else offset++;
		// returning only clusters with labels (allClusters - offset)
		return Arrays.copyOfRange(finalLabelFeatureSets, 0, finalLabelFeatureSets.length - offset);
	}
	
	/**
	 * Writes the debug message string to the console output if debug for the learner is enabled.
	 * 
	 * @param msg
	 *            the debug message
	 */
	@Override
	protected void debug(final String msg) {
		if (!getDebug()) return;
		System.err.println(Thread.currentThread().getName() + "@" + new Date() + ": " + msg);
	}
	
	/**
	 * to be implemented by subclasses. Finds labelFeatureSets in the given dataset
	 * 
	 * @param trainingSet
	 *            mulan.data.MultiLabelInstances
	 * @return int[cluster]{[0] = label indices, [1] = feature indices}
	 * @throws Exception
	 *             weka exception
	 */
	protected abstract int[][][] findLabelFeatureSets(final MultiLabelInstances trainingSet) throws Exception;
	
	/**
	 * @return average cardinality
	 */
	public double getAvgCardinality() {
		return avgCardinality;
	}
	
	/**
	 * @return average distinct label combinations
	 */
	public double getAvgCombCount() {
		return avgLabelCombCount;
	}
	
	/**
	 * @return average density
	 */
	public double getAvgDensity() {
		return avgDensity;
	}
	
	/**
	 * @return averages labels per group
	 */
	public double getAvgLabels() {
		return avgLabels;
	}
	
	/**
	 * @return average phi correlation
	 */
	public double[] getAvgPhi() {
		return avgPhi;
	}
	
	/**
	 * @return the ensemble of filteredMLLearner
	 */
	public FilteredMLLearner[] getEnsemble() {
		return ensemble;
	}
	
	/**
	 * @return mulan.classifier.MultiLabelLearner
	 */
	public MultiLabelLearner getMlClassifier() {
		return mlClassifier;
	}
	
	/**
	 * making the prediction for given instance over all group models. If labels are contained in more then one group
	 * the vote with the farthermost distance to the threshold (0.5) is taken
	 */
	@Override
	protected MultiLabelOutput makePredictionInternal(final Instance instance) throws Exception, InvalidDataException {
		final boolean[] bipartions = new boolean[numLabels];
		final double[] confidences = new double[numLabels];
		// Double.NaN = no vote is given
		Arrays.fill(confidences, Double.NaN);
		// predicting for each group
		for (int i = 0; i < labelFeatureSets.length; i++) {
			final MultiLabelOutput prediction = ensemble[i].makePrediction(instance);
			final boolean[] bipartition = prediction.getBipartition();
			final double[] confidence = prediction.getConfidences();
			// matching votes to the correct label indices
			for (int currentLabel = 0; currentLabel < labelFeatureSets[i][0].length; currentLabel++) {
				final int index = // index of the current label in the complete
						// label indices
						Arrays.binarySearch(labelIndices, labelFeatureSets[i][0][currentLabel]);
				// if a vote for this label has already been given
				if (!Double.isNaN(confidences[index])) {
					// taking the vote with the farthest distance to the
					// threshold (0.5)
					confidences[index] =
							Math.abs(0.5 - confidences[index]) > Math.abs(0.5 - confidences[currentLabel])
									? confidences[index] : confidence[currentLabel];
					// taking the vote with the farthermost distance to the
					// threshold (0.5)
					bipartions[index] =
							Math.abs(0.5 - confidences[index]) > Math.abs(0.5 - confidences[currentLabel])
									? bipartions[index] : bipartions[currentLabel];
				} else {
					bipartions[index] = bipartition[currentLabel];
					confidences[index] = confidence[currentLabel];
				}
			}
		}
		return new MultiLabelOutput(bipartions, confidences);
	}
	
	/**
	 * prints statistics on ensemble
	 */
	public void printStatistics() {
		System.out.println(Thread.currentThread().getName() + "@" + new Date() + ": " + "average cardinality : "
				+ Utils.roundDouble(avgCardinality, 3));
		System.out.println(Thread.currentThread().getName() + "@" + new Date() + ": " + "average density : "
				+ Utils.roundDouble(avgDensity, 3));
		System.out.println(Thread.currentThread().getName() + "@" + new Date() + ": " + "average labels : "
				+ Utils.roundDouble(avgLabels, 3));
	}
	
	/**
	 * @param mlClassifier
	 *            {@link MultiLabelLearner}
	 */
	public void setMlClassifier(final MultiLabelLearner mlClassifier) {
		this.mlClassifier = mlClassifier;
	}
}
