package de.tum.in.mulan.classifier.transformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mulan.classifier.MultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;

/**
 * feature selection based group selecting with naive group merging
 * 
 * @author LehnereS
 */
public class MLA extends GroupBasedMetaClassifier {
	
	private Map<Integer, Set<Integer>>	attributeMap;
	private final AttributeSelection	selector;
	
	protected MLA(final ASEvaluation evaluation, final ASSearch searcher) {
		selector = new AttributeSelection();
		selector.setEvaluator(evaluation);
		selector.setSearch(searcher);
	}
	
	/**
	 * @param mlClassifier
	 *            mulan.classifier.MultiLabelLearner
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 * @param evaluation
	 *            weka.attributeSelection.ASEvaluation
	 * @param searcher
	 *            weka.attributeSelection.ASSearch
	 */
	public MLA(final MultiLabelLearner mlClassifier, final Classifier slClassifier, final ASEvaluation evaluation,
			final ASSearch searcher) {
		super(slClassifier);
		selector = new AttributeSelection();
		selector.setEvaluator(evaluation);
		selector.setSearch(searcher);
		this.mlClassifier = mlClassifier;
	}
	
	private Set<Integer> backtrace(final HashMap<Set<Integer>, Set<Integer>> mergedAttributeMap,
			final Set<Integer> labelSet, final Set<Integer> featureSet) {
		// checking four cases:
		for (final Set<Integer> currentLabelSet : mergedAttributeMap.keySet())
			// if currentLabelSet contains all labels in labelSet
			if (currentLabelSet.containsAll(labelSet)) {
				debug("backtrace: found redundant labelset");
				// adding all features from currentLabelSet to the featureSet of
				// labelSet
				featureSet.addAll(mergedAttributeMap.get(currentLabelSet));
				// removing currentLabelSet entry
				mergedAttributeMap.remove(currentLabelSet);
				// can stop here, other keys in mergedAttributeMap will be
				// worked later
				return currentLabelSet;
			} // if labelSet contains all labels in currentLabelSet
			else if (labelSet.containsAll(currentLabelSet)) {
				debug("backtrace: found redundant labelset");
				// adding all features from currentLabelSet to the featureSet of
				// labelSet
				featureSet.addAll(mergedAttributeMap.get(currentLabelSet));
				// removing currentLabelSet entry
				mergedAttributeMap.remove(currentLabelSet);
				// can stop here, other keys in mergedAttributeMap will be
				// worked later
				return labelSet;
			} // if featureSet of currentLabelSet contains all features of
				// labelSet
			else if (mergedAttributeMap.get(currentLabelSet).containsAll(featureSet)) {
				debug("backtrace: found redundant feature set");
				// adding all features from currentLabelSet to featureSet
				featureSet.addAll(mergedAttributeMap.get(currentLabelSet));
				// removing currentLabelSet entry;
				mergedAttributeMap.remove(currentLabelSet);
				// adding all all labels from labelSet to currentLabelSet
				currentLabelSet.addAll(labelSet);
				// can stop here, other keys in mergedAttributeMap will be
				// worked later
				return currentLabelSet;
			} // if featureSet of labelSet contains all features of
				// currentLabelSet
			else if (featureSet.containsAll(mergedAttributeMap.get(currentLabelSet))) {
				debug("backtrace: found redundant feature set");
				// removing currentLabelSet entry;
				mergedAttributeMap.remove(currentLabelSet);
				// adding all all labels from labelSet to currentLabelSet
				currentLabelSet.addAll(labelSet);
				// can stop here, other keys in mergedAttributeMap will be
				// worked later
				return currentLabelSet;
			}
		return labelSet;
	}
	
	private Map<Integer, Set<Integer>> featureSelection(final MultiLabelInstances dataSet) throws Exception {
		attributeMap = new TreeMap<>();
		// attribute selection for each label
		for (final int currentLabelIndex : dataSet.getLabelIndices()) {
			dataSet.getDataSet().setClassIndex(currentLabelIndex);
			debug("attribute selection for label #" + currentLabelIndex + "...");
			selector.SelectAttributes(dataSet.getDataSet());
			debug("attribute selection for label #" + currentLabelIndex + "...done");
			final Set<Integer> selectedAttributes =
			// converting int[] to List<Integer> to TreeSet<Integer>
					new TreeSet<>(de.tum.in.multilabel.Utils.arrayAsList(selector.selectedAttributes()));
			selectedAttributes.remove(currentLabelIndex);
			attributeMap.put(currentLabelIndex, selectedAttributes);
		}
		return attributeMap;
	}
	
	@Override
	protected int[][][] findLabelFeatureSets(final MultiLabelInstances trainingSet) throws Exception {
		debug("searching attribute map...");
		@SuppressWarnings ("hiding")
		final Map<Integer, Set<Integer>> attributeMap = featureSelection(trainingSet);
		debug("searching attribute map...done");
		debug("merging attribute map...");
		final int[][][] labelFeatureSets = mergeAttributeMap(trainingSet, attributeMap);
		debug("merging attribute map...done");
		return labelFeatureSets;
	}
	
	private int[][][] mergeAttributeMap(final MultiLabelInstances dataSet,
			@SuppressWarnings ("hiding") final Map<Integer, Set<Integer>> attributeMap) {
		final HashMap<Set<Integer>, Set<Integer>> mergedAttributeMap = new HashMap<>();
		// for every labelIndex merging attribute sets
		for (final Integer labelIndex : dataSet.getLabelIndices()) {
			final Set<Integer> labelSet = new TreeSet<>();
			labelSet.add(labelIndex);
			final Set<Integer> featureSet = new TreeSet<>(attributeMap.get(labelIndex));
			// recursive helper, will run until no label is left in the
			// featureSet
			mergeFeatureSetsHelper(dataSet, labelSet, featureSet);
			mergedAttributeMap.put(
			// backtracking to avoid redundant sets
					backtrace(mergedAttributeMap, labelSet, featureSet), featureSet);
		}
		final int[][][] labelFeatureSets = new int[mergedAttributeMap.keySet().size()][2][];
		int m = 0;
		for (final Set<Integer> labelSet : mergedAttributeMap.keySet()) {
			// converting Set<Integer> to int[]
			labelFeatureSets[m][0] =
					de.tum.in.multilabel.Utils.convertIntegerArray(labelSet.toArray(new Integer[labelSet.size()]));
			// converting Set<Integer> to int[]
			labelFeatureSets[m][1] =
					de.tum.in.multilabel.Utils.convertIntegerArray(mergedAttributeMap.get(labelSet).toArray(
							new Integer[mergedAttributeMap.get(labelSet).size()]));
			m++;
		}
		return labelFeatureSets;
	}
	
	private Set<Integer> mergeFeatureSetsHelper(final MultiLabelInstances dataSet, final Set<Integer> labelSet,
			final Set<Integer> featureSet) {
		// searching for label indices
		for (final Integer featureIndex : new TreeSet<>(featureSet))
			// skip if already in labelSet
			if (!labelSet.contains(featureIndex) && labelIndicesSet.contains(featureIndex)) {
				labelSet.add(featureIndex);
				// recursive solve feature set
				featureSet.addAll(mergeFeatureSetsHelper(dataSet, labelSet,
						new HashSet<>(attributeMap.get(featureIndex))));
			}
		// XXX removing labels from feature set: should not be necessary
		featureSet.removeAll(new HashSet<>(de.tum.in.multilabel.Utils.arrayAsList(dataSet.getLabelIndices())));
		return featureSet;
	}
}
