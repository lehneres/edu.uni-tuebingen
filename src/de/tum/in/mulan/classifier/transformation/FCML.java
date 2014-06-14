package de.tum.in.mulan.classifier.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mulan.classifier.MultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.experiment.Stats;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveUseless;

/**
 * Group selecting by feature selection based clustering. for every label a feature selection is made and received score
 * vectors are clustered.
 * 
 * @author LehnereS
 */
public class FCML extends GroupBasedMetaClassifier {
	
	/**
	 * creating new dataset with replaced values. The new values are the logscores evaluated by feature selection
	 * 
	 * @param trainingSet
	 *            {@link mulan.data.MultiLabelInstances}
	 * @param attributeMap
	 *            containing the feature selection result for each label
	 * @return dataset with ranks
	 */
	private static Instances getRankMatrix(final MultiLabelInstances trainingSet,
			final Map<Integer, double[][]> attributeMap) {
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < trainingSet.getDataSet().numAttributes(); i++)
			attributes.add(new Attribute(trainingSet.getDataSet().attribute(i).name()));
		final Instances rankMatrix = new Instances("ranks", attributes, trainingSet.getNumLabels());
		for (final Integer label : attributeMap.keySet()) {
			final Instance tempInstance = new DenseInstance(trainingSet.getDataSet().numAttributes());
			final double[][] ranking = attributeMap.get(label);
			for (int rank = 0; rank < ranking.length; rank++)
				tempInstance.setValue((int) ranking[rank][0], rank);
			rankMatrix.add(tempInstance);
		}
		return rankMatrix;
	}
	
	private static Instances preProcessData(final Instances scores) throws Exception {
		// removing instances which are all-zero
		Instances preProcessed = scores;
		final RemoveUseless removeUseless = new RemoveUseless();
		removeUseless.setInputFormat(scores);
		preProcessed = Filter.useFilter(scores, removeUseless);
		// if (this.useRanks) {
		// final Instances rankedScores = new Instances(scores,
		// scores.numInstances());
		// for (final Instance instance : scores) {
		// final Map<Double, Double> ranks = this
		// .generateRanking(instance);
		// final double[] values = new double[instance.numAttributes()];
		// for (int i = 0; i < values.length; i++) {
		// values[i] = ranks.get(instance.value(i));
		// }
		// rankedScores.add(new DenseInstance(1, values));
		// }
		// preProcessed = rankedScores;
		// }
		return preProcessed;
	}
	
	private Map<Integer, double[][]>	attributeMap;
	private Clusterer					clusterer;
	private boolean						computeThreshold			= false;
	private double						multiplier					= 2;
	private boolean						optimizeClusterNumber		= false;
	private final Set<Integer>			outlierIndices				= new HashSet<>();
	private boolean						removeOutliers				= false;
	private boolean						saveClusters				= false;
	private AttributeSelection			selector;
	private double						threshold					= Double.NEGATIVE_INFINITY;
	private double[]					thresholdForEachCluster;
	
	private final boolean				useLabelInFeatureSelection	= false;
	
	private final boolean				UseLabelsForThreshold		= false;
	
	private boolean						useLogScores				= false;
	
	private boolean						useRanks					= false;
	
	protected FCML(final AttributeSelection selector, final Clusterer clusterer) {
		this.selector = selector;
		this.clusterer = clusterer;
	}
	
	/**
	 * @param mlClassifier
	 *            {@link mulan.classifier.MultiLabelLearner}
	 * @param slClassifier
	 *            {@link weka.classifiers.Classifier}
	 * @param selector
	 *            {@link weka.attributeSelection.AttributeSelection}
	 * @param clusterer
	 *            {@link weka.clusterers.Clusterer}
	 */
	public FCML(final MultiLabelLearner mlClassifier, final Classifier slClassifier, final AttributeSelection selector,
			final Clusterer clusterer) {
		super(slClassifier);
		this.selector = selector;
		this.clusterer = clusterer;
		this.mlClassifier = mlClassifier;
	}
	
	/**
	 * computes threshold for feature selection
	 * 
	 * @param originalData
	 *            {@link MultiLabelInstances}
	 * @param clusterer
	 *            {@link Clusterer}
	 * @param scores
	 *            {@link Instances} with the scores from feature selection
	 * @param working
	 *            {@link Instances} origin instances
	 * @return threshold as n x standard deviation from the mean
	 * @throws Exception
	 */
	private double[] computeThreshold(final MultiLabelInstances originalData,
			@SuppressWarnings ("hiding") final Clusterer clusterer, final Instances scores, final Instances working)
			throws Exception {
		debug("autocalculating threshold...");
		final double[] tresholds = new double[clusterer.numberOfClusters()];
		final Instances[] splitDataSets = new Instances[clusterer.numberOfClusters()];
		for (int i = 0; i < working.numInstances(); i++) {
			final int cluster = clusterer.clusterInstance(working.get(i));
			if (splitDataSets[cluster] == null)
				splitDataSets[cluster] =
						new Instances(scores, originalData.getNumLabels() / clusterer.numberOfClusters());
			splitDataSets[cluster].add(scores.get(i));
		}
		for (int i = 0; i < splitDataSets.length; i++) {
			final Stats stats = new Stats();
			for (int k = 0; k < splitDataSets[i].numInstances(); k++)
				for (int attr = 0; attr < splitDataSets[i].numAttributes(); attr++)
					if (UseLabelsForThreshold
							|| !originalData.getLabelsMetaData().containsLabel(splitDataSets[i].attribute(attr).name())) {
						double value = 0;
						if (useLogScores) value = Math.pow(Math.E, splitDataSets[i].get(k).value(attr));
						else value = splitDataSets[i].get(k).value(attr);
						if (!Double.isNaN(value)) stats.add(value);
					}
			stats.calculateDerived();
			tresholds[i] = stats.mean + multiplier * stats.stdDev;
			debug("treshold for cluster " + i + " is " + tresholds[i]);
		}
		debug("autocalculating threshold...done");
		return tresholds;
	}
	
	@Override
	protected int[][][] findLabelFeatureSets(final MultiLabelInstances trainingSet) throws Exception {
		@SuppressWarnings ("hiding")
		final Map<Integer, double[][]> attributeMap = getAttributeMap(trainingSet);
		Instances working, scores;
		scores = getScoreMatrix(trainingSet, attributeMap);
		if (useRanks) working = FCML.getRankMatrix(trainingSet, attributeMap);
		else working = new Instances(scores);
		working = FCML.preProcessData(working);
		
		debug("learning clusters...");
		clusterer.buildClusterer(working);
		if (removeOutliers) {
			debug("removing outliers...");
			working = this.removeOutliers(working);
			debug("" + "removing outliers...done");
		}
		debug("number of clusters found: " + clusterer.numberOfClusters());
		if (getDebug()) printClusters(working);
		debug("learning clusters...done");
		if (computeThreshold) thresholdForEachCluster = computeThreshold(trainingSet, clusterer, scores, working);
		debug("finding cluster sets...");
		final int[][][] labelFeatureSets = getLabelFeatureSets(trainingSet.getDataSet(), working, scores);
		debug("finding cluster sets...done");
		return labelFeatureSets;
	}
	
	/**
	 * @param dataSet
	 *            {@link MultiLabelInstances}
	 * @return a map containing scores
	 * @throws Exception
	 *             weka exception
	 */
	public Map<Integer, double[][]> getAttributeMap(final MultiLabelInstances dataSet) throws Exception {
		if (attributeMap == null) {
			@SuppressWarnings ("hiding")
			final Map<Integer, double[][]> attributeMap = new HashMap<>();
			int[] indicesToUse;
			indicesToUse = dataSet.getLabelIndices();
			// doing feature selection for every label
			debug("attribute selection...");
			for (final int currentIndex : indicesToUse) {
				debug("attribute selection for attribute #" + currentIndex + "...");
				// setting class to current label for the attribute selection
				final Instances tmpData = dataSet.getDataSet();
				tmpData.setClassIndex(currentIndex);
				// selecting attribute
				selector.SelectAttributes(tmpData);
				debug("attribute selection for attribute #" + currentIndex + "...done");
				// result comes in format:
				// this.selector.rankedAttributes()[rank]{[0] = index
				// , [1] = score}
				attributeMap.put(currentIndex, selector.rankedAttributes());
			}
			debug("attribute selection...done");
			return attributeMap;
		}
		return attributeMap;
	}
	
	/**
	 * @return the clusterer
	 */
	public Clusterer getClusterer() {
		return clusterer;
	}
	
	/**
	 * creates label and feature sets according to a clustering
	 * 
	 * @param originalData
	 *            {@link MultiLabelInstances}
	 * @param cluster
	 *            {@link Instances} clustered data (may differ to scores because of preprocessing}
	 * @param scores
	 *            {@link Instances} scores from feature selection
	 * @return a 3-dimensional array containing the label- and feature sets
	 * @throws Exception
	 *             any exception
	 */
	private int[][][]
			getLabelFeatureSets(final Instances originalData, final Instances cluster, final Instances scores)
					throws Exception {
		int labelFeatureSetSize = clusterer.numberOfClusters();
		if (outlierIndices != null && clusterer instanceof HierarchicalClusterer && !outlierIndices.isEmpty())
			labelFeatureSetSize += 1;
		@SuppressWarnings ("unchecked")
		final Set<Integer>[][] labelFeatureSets = new Set[labelFeatureSetSize][2];
		// each instance is representing a label
		for (int curInstanceIndex = 0; curInstanceIndex < cluster.numInstances(); curInstanceIndex++) {
			final int curClusterID;
			if (outlierIndices != null && clusterer instanceof HierarchicalClusterer
					&& outlierIndices.contains(curInstanceIndex)) curClusterID = labelFeatureSetSize - 1;
			else curClusterID = clusterer.clusterInstance(cluster.instance(curInstanceIndex));
			// initializing labelset
			if (labelFeatureSets[curClusterID][0] == null) labelFeatureSets[curClusterID][0] = new TreeSet<>();
			// initializing featureset
			if (labelFeatureSets[curClusterID][1] == null) labelFeatureSets[curClusterID][1] = new TreeSet<>();
			
			// current instance is added as label to the labelset
			labelFeatureSets[curClusterID][0].add(labelIndices[curInstanceIndex]);
			for (int curAttributeIndex = 0; curAttributeIndex < cluster.numAttributes(); curAttributeIndex++) {
				final Attribute curAttribute = cluster.attribute(curAttributeIndex);
				// revert logscores to original scores, applying threshold.
				// If the score is above threshold the attribute is added to
				// the feature set
				// XXX if all labels in featureSelection() are kept, here
				// only features should be added to the set (as label
				// indices correspond to the instance indices and will be
				// added above)
				if (useLabelInFeatureSelection
						|| !labelIndicesSet.contains(originalData.attribute(curAttribute.name()).index())) {
					double score;
					if (useLogScores) score = Math.pow(Math.E, scores.instance(curInstanceIndex).value(curAttribute));
					else score = scores.instance(curInstanceIndex).value(curAttribute);
					if (score >= (thresholdForEachCluster != null ? thresholdForEachCluster[curClusterID] : threshold)) {
						labelFeatureSets[curClusterID][labelIndicesSet.contains(originalData.attribute(
								curAttribute.name()).index()) ? 0 : 1].add(originalData.attribute(curAttribute.name())
								.index());
						if (labelIndicesSet.contains(originalData.attribute(curAttribute.name()).index()))
							System.err.println("added LABEL " + originalData.attribute(curAttribute.name()).index());
					}
				}
			}
		}
		return compactLabelFeatureSets(labelFeatureSets);
	}
	
	/**
	 * @return multiplier
	 */
	public double getMultiplier() {
		return multiplier;
	}
	
	private Set<Integer> getOutliers(final Instances data) throws Exception {
		@SuppressWarnings ("unchecked")
		final Set<Integer>[] clusters = new HashSet[clusterer.numberOfClusters()];
		final Set<Integer> outliers = new HashSet<>();
		for (int index = 0; index < data.numInstances(); index++) {
			final int cluster = clusterer.clusterInstance(data.get(index));
			if (clusters[cluster] == null) clusters[cluster] = new HashSet<>();
			clusters[cluster].add(index);
		}
		for (final Set<Integer> cluster : clusters)
			if (cluster != null && cluster.size() <= data.numInstances() / 10) outliers.addAll(cluster);
		return outliers;
	}
	
	/**
	 * creating new dataset with replaced values. The new values are the logscores evaluated by feature selection
	 * 
	 * @param trainingSet
	 *            {@link mulan.data.MultiLabelInstances}
	 * @param attributeMap
	 *            containing the feature selection result for each label
	 * @return dataset with scores
	 */
	private Instances getScoreMatrix(final MultiLabelInstances trainingSet,
			@SuppressWarnings ("hiding") final Map<Integer, double[][]> attributeMap) {
		final ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < trainingSet.getDataSet().numAttributes(); i++)
			attributes.add(new Attribute(trainingSet.getDataSet().attribute(i).name()));
		final Instances scoreMatrix = new Instances("scores", attributes, trainingSet.getNumLabels());
		for (final Integer label : attributeMap.keySet()) {
			final Instance tempInstance = new DenseInstance(trainingSet.getDataSet().numAttributes());
			for (final double[] entry : attributeMap.get(label)) {
				double score;
				if (useLogScores) score = Math.log(entry[1]);
				else score = entry[1];
				if (!Double.isInfinite(score)) tempInstance.setValue((int) entry[0], score);
			}
			scoreMatrix.add(tempInstance);
		}
		return scoreMatrix;
	}
	
	/**
	 * @return the selector
	 */
	public AttributeSelection getSelector() {
		return selector;
	}
	
	/**
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}
	
	/**
	 * @return if to compute threshold
	 */
	public boolean isComputeThreshold() {
		return computeThreshold;
	}
	
	/**
	 * @return if to optimize the number of clusters
	 */
	public boolean isOptimizeClusterNumber() {
		return optimizeClusterNumber;
	}
	
	/**
	 * @return the removeOutliers
	 */
	public boolean isRemoveOutliers() {
		return removeOutliers;
	}
	
	/**
	 * @return the saveClusters
	 */
	public boolean isSaveClusters() {
		return saveClusters;
	}
	
	/**
	 * @return the useLogScores
	 */
	public boolean isUseLogScores() {
		return useLogScores;
	}
	
	/**
	 * @return the useRanks
	 */
	public boolean isUseRanks() {
		return useRanks;
	}
	
	private void printClusters(final Instances working) throws Exception {
		final Map<Integer, HashSet<Integer>> clusters = new TreeMap<>();
		for (int i = 0; i < working.numInstances(); i++) {
			final int cluster = clusterer.clusterInstance(working.get(i));
			if (clusters.containsKey(cluster)) clusters.get(cluster).add(i);
			else {
				final HashSet<Integer> set = new HashSet<>();
				set.add(i);
				clusters.put(cluster, set);
			}
		}
		for (final Entry<Integer, HashSet<Integer>> entry : clusters.entrySet())
			System.err.println("cluster no " + entry.getKey() + " : "
					+ de.tum.in.multilabel.Utils.collectionToString(entry.getValue()));
	}
	
	private Instances removeOutliers(final Instances in_data) throws Exception {
		Instances data = in_data;
		Set<Integer> outliers = getOutliers(data);
		while (outliers.size() > 0) {
			debug(outliers.size() + " outliers found");
			data = this.removeOutliers(data, outliers);
			clusterer.buildClusterer(data);
			outliers = getOutliers(data);
		}
		return data;
	}
	
	private Instances removeOutliers(final Instances data, final Set<Integer> outliers) {
		outlierIndices.addAll(outliers);
		final Instances withOutOutliers = new Instances(data, data.numInstances() - outliers.size());
		for (int index = 0; index < data.numInstances(); index++)
			if (!outliers.contains(index)) withOutOutliers.add(data.get(index));
		return withOutOutliers;
	}
	
	/**
	 * @param map
	 *            the attribute map
	 */
	public void setAttributeMap(final Map<Integer, double[][]> map) {
		attributeMap = map;
	}
	
	/**
	 * @param clusterer
	 *            the clusterer to set
	 */
	public void setClusterer(final Clusterer clusterer) {
		this.clusterer = clusterer;
	}
	
	/**
	 * @param autocomputeThreshold
	 *            true to compute threshold
	 */
	public void setComputeThreshold(final boolean autocomputeThreshold) {
		computeThreshold = autocomputeThreshold;
	}
	
	/**
	 * @param multiplier
	 *            if computeThreshold threshold = mean + multiplier * std.dev
	 */
	public void setMultiplier(final double multiplier) {
		this.multiplier = multiplier;
	}
	
	/**
	 * @param optimizeClusterNumber
	 *            if to optimize the number of clusters
	 */
	public void setOptimizeClusterNumber(final boolean optimizeClusterNumber) {
		this.optimizeClusterNumber = optimizeClusterNumber;
	}
	
	/**
	 * @param removeOutliers
	 *            the removeOutliers to set
	 */
	public void setRemoveOutliers(final boolean removeOutliers) {
		this.removeOutliers = removeOutliers;
	}
	
	// DEAD code. Was used to auto find number of clusters. Has not been updated
	// to current implementation
	
	// @SuppressWarnings("unused")
	// private void learnClusters(final MultiLabelInstances originalData,
	// final Instances data) throws Exception {
	// if (this.optimizeClusterNumber) {
	// this.debug("doing cluster optimization...");
	// final int bestNumber = this.learnClustersHelper(originalData, data,
	// 2, originalData.getNumLabels());
	// de.tum.in.multilabel.Utils.setNumberOfClusters(this.clusterer,
	// bestNumber);
	// this.clusterer.buildClusterer(data);
	// this.debug("doing cluster optimization...done");
	// } else {
	// this.clusterer.buildClusterer(data);
	// }
	// }
	//
	// private int learnClustersHelper(final MultiLabelInstances originalData,
	// final Instances data, final int low, final int high)
	// throws Exception {
	// int lowPoint, highPoint;
	// final double quartil = (high - low) / 4;
	// lowPoint = (int) (low + quartil);
	// highPoint = (int) (low + (3 * quartil));
	// Clusterer localClusterer = AbstractClusterer.makeCopy(this.clusterer);
	// de.tum.in.multilabel.Utils
	// .setNumberOfClusters(localClusterer, lowPoint);
	// localClusterer.buildClusterer(data);
	// Instances[] splitDataSets = this.getSplitDataSets(originalData,
	// localClusterer, data);
	// final double scoreLow = this.getAvgDensity(splitDataSets, originalData);
	//
	// localClusterer = AbstractClusterer.makeCopy(this.clusterer);
	// de.tum.in.multilabel.Utils.setNumberOfClusters(localClusterer,
	// highPoint);
	// localClusterer.buildClusterer(data);
	// splitDataSets = this.getSplitDataSets(originalData, localClusterer,
	// data);
	// final double scoreHigh = this
	// .getAvgDensity(splitDataSets, originalData);
	//
	// splitDataSets = null;
	// if (Math.max(scoreHigh, scoreLow) == scoreHigh) {
	// if (((high - low) / 2) > 1) {
	// this.debug("new best: " + highPoint
	// + " continuing, taking high... ");
	// localClusterer = null;
	// return this.learnClustersHelper(originalData, data, high
	// - ((high - low) / 2), high);
	// }
	// this.debug("new best: " + highPoint + " with score " + scoreHigh);
	// return highPoint;
	// }
	// if (((high - low) / 2) > 1) {
	// this.debug("new best: " + lowPoint + " continuing, taking low... ");
	// localClusterer = null;
	// return this.learnClustersHelper(originalData, data, low, high
	// - ((high - low) / 2));
	// }
	// this.debug("new best: " + lowPoint + " with score " + scoreLow);
	// return lowPoint;
	// }
	
	/**
	 * @param saveClusters
	 *            the saveClusters to set
	 */
	public void setSaveClusters(final boolean saveClusters) {
		this.saveClusters = saveClusters;
	}
	
	/**
	 * @param selector
	 *            the selector to set
	 */
	public void setSelector(final AttributeSelection selector) {
		this.selector = selector;
	}
	
	/**
	 * setting a threshold for the feature selection. Standard is Double.MIN_VALUE (selecting all features)
	 * 
	 * @param threshold
	 *            a threshold
	 */
	public void setThreshold(final double threshold) {
		this.threshold = threshold;
	}
	
	/**
	 * @param useLogScores
	 *            the useLogScores to set
	 */
	public void setUseLogScores(final boolean useLogScores) {
		this.useLogScores = useLogScores;
	}
	
	/**
	 * @param useRanks
	 *            the useRanks to set
	 */
	public void setUseRanks(final boolean useRanks) {
		this.useRanks = useRanks;
	}
}
