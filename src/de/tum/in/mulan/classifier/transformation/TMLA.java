package de.tum.in.mulan.classifier.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mulan.classifier.MultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import de.tum.in.multilabel.distancefunction.TanimotoDistance;

/**
 * group selecting by feature selection based clustering using Tanimoto-Distance
 * 
 * @author LehnereS
 */
public class TMLA extends GroupBasedMetaClassifier {
	
	private static Instances getSelectedMatrix(final MultiLabelInstances trainingSet,
			final Map<Integer, boolean[]> attributeMap) {
		// creating new dataset with replaced values. The new values are the
		// binary values evaluated
		// by feature selection
		final ArrayList<Attribute> attributes = new ArrayList<>();
		final LinkedList<String> values = new LinkedList<>();
		values.add("0");
		values.add("1");
		for (int i = 0; i < trainingSet.getDataSet().numAttributes(); i++)
			attributes.add(new Attribute(trainingSet.getDataSet().attribute(i).name(), values));
		final Instances scores = new Instances("scores", attributes, trainingSet.getNumLabels());
		for (final Integer label : attributeMap.keySet()) {
			final Instance tempInstance = new DenseInstance(trainingSet.getDataSet().numAttributes());
			tempInstance.setDataset(scores);
			for (int index = 0; index < attributeMap.get(label).length; index++)
				tempInstance.setValue(index, attributeMap.get(label)[index] ? "1" : "0");
			scores.add(tempInstance);
		}
		return scores;
	}
	
	private final HierarchicalClusterer	clusterer;
	
	private final AttributeSelection	selector;
	
	protected TMLA(final ASSearch searcher, final HierarchicalClusterer clusterer) {
		selector = new AttributeSelection();
		selector.setEvaluator(new CfsSubsetEval());
		selector.setSearch(searcher);
		if (!(this.clusterer.getDistanceFunction() instanceof TanimotoDistance))
			throw new UnsupportedOperationException("only Tanimoto Distance is allowd for TMLA!");
		this.clusterer = clusterer;
	}
	
	/**
	 * @param mlClassifier
	 *            mulan.classifier.MultiLabelLearner
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 * @param searcher
	 *            weka.attributeSelection.ASSearch
	 * @param clusterer
	 *            weka.clusterers.HierarchicalClusterer
	 */
	public TMLA(final MultiLabelLearner mlClassifier, final Classifier slClassifier, final ASSearch searcher,
			final HierarchicalClusterer clusterer) {
		super(slClassifier);
		selector = new AttributeSelection();
		selector.setEvaluator(new CfsSubsetEval());
		selector.setSearch(searcher);
		if (!(clusterer.getDistanceFunction() instanceof TanimotoDistance))
			throw new UnsupportedOperationException("only Tanimoto Distance is allowd for TMLA!");
		this.clusterer = clusterer;
		this.mlClassifier = mlClassifier;
	}
	
	private Map<Integer, boolean[]> featureSelection(final MultiLabelInstances dataSet) throws Exception {
		final Map<Integer, boolean[]> attributeMap = new HashMap<>();
		// doing feature selection for every label
		for (final int currentLabelIndex : dataSet.getLabelIndices()) {
			final Instances tmpData = dataSet.getDataSet();
			debug("attribute selection for label #" + currentLabelIndex + "...");
			tmpData.setClassIndex(currentLabelIndex);
			selector.SelectAttributes(tmpData);
			debug("attribute selection for label #" + currentLabelIndex + "...done");
			// if selected index is set to true
			final boolean[] selected = new boolean[tmpData.numAttributes()];
			Arrays.fill(selected, false);
			for (final int index : selector.selectedAttributes())
				// all selected indices true but the currentLabelIndex
				selected[index] = true; // index != currentLabelIndex;
			attributeMap.put(currentLabelIndex, selected);
		}
		return attributeMap;
	}
	
	@Override
	protected int[][][] findLabelFeatureSets(final MultiLabelInstances trainingSet) throws Exception {
		final Map<Integer, boolean[]> attributeMap = featureSelection(trainingSet);
		final Instances selectedMatrix = TMLA.getSelectedMatrix(trainingSet, attributeMap);
		debug("learning clusters...");
		clusterer.buildClusterer(selectedMatrix);
		debug("number of clusters found: " + clusterer.numberOfClusters());
		debug("learning clusters...done");
		debug("finding cluster sets...");
		final int[][][] labelFeatureSets = getLabelFeatureSets(selectedMatrix);
		debug("finding cluster sets...done");
		return labelFeatureSets;
	}
	
	private int[][][] getLabelFeatureSets(final Instances selectedMatrix) throws Exception {
		@SuppressWarnings ("unchecked")
		final Set<Integer>[][] labelFeatureSets = new Set[clusterer.numberOfClusters()][2];
		// each instance is representing a label
		for (int currentInstanceIndex = 0; currentInstanceIndex < selectedMatrix.numInstances(); currentInstanceIndex++) {
			final int currentClusterID = clusterer.clusterInstance(selectedMatrix.instance(currentInstanceIndex));
			// initializing labelset
			if (labelFeatureSets[currentClusterID][0] == null) labelFeatureSets[currentClusterID][0] = new TreeSet<>();
			// initializing featureset
			if (labelFeatureSets[currentClusterID][1] == null) labelFeatureSets[currentClusterID][1] = new TreeSet<>();
			for (int curAttributeIndex = 0; curAttributeIndex < selectedMatrix.numAttributes(); curAttributeIndex++) {
				final Attribute curAttribute = selectedMatrix.attribute(curAttributeIndex);
				// if curAttribute is selected within this instance
				if (selectedMatrix.instance(currentInstanceIndex).stringValue(curAttribute).equals("1"))
					if (labelIndicesSet.contains(curAttribute.index())) labelFeatureSets[currentClusterID][0]
							.add(selectedMatrix.attribute(curAttribute.name()).index());
					else labelFeatureSets[currentClusterID][1].add(selectedMatrix.attribute(curAttribute.name())
							.index());
			}
		}
		return compactLabelFeatureSets(labelFeatureSets);
	}
}
