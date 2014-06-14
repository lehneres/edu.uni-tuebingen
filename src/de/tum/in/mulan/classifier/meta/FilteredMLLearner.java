package de.tum.in.mulan.classifier.meta;

import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelLearnerBase;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import de.tum.in.multilabel.Utils;

/**
 * meta multi-label learner, removes specified label and features attributes and trains a classifier on the resulting
 * dataset
 * 
 * @author LehnereS
 */
public class FilteredMLLearner extends MultiLabelLearnerBase {
	
	private double					density, cardinality;
	private double[]				phi;
	private Remove					filter;
	private int[]					filteredLabelIndices, filteredFeatureIndices;
	private final MultiLabelLearner	mlClassifier;
	private final Classifier		slClassifier;
	private int						labelCombCount;
	
	/**
	 * @param mlClassifier
	 *            mulan.classifier.MultiLabelLearner
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 */
	public FilteredMLLearner(final MultiLabelLearner mlClassifier, final Classifier slClassifier) {
		this.mlClassifier = mlClassifier;
		this.slClassifier = slClassifier;
	}
	
	// /**
	// * computes statistics on filtered dataset
	// *
	// * @param data
	// * {@link MultiLabelInstances}
	// * @throws Exception
	// * weka exception
	// */
	// public void computeStatsOnFilteredData(final MultiLabelInstances data)
	// throws Exception {
	// try {
	// final Statistics stats = new Statistics();
	// stats.calculateStats(getFilteredData(data));
	// stats.calculatePhi(getFilteredData(data));
	// density = stats.density();
	// cardinality = stats.cardinality();
	// phi = stats.getPhiHistogram();
	// labelCombCount = stats.labelCombCount().keySet().size();
	// } catch (final Exception e) {
	// final Instances filtered = Filter.useFilter(data.getDataSet(),
	// filter);
	// double labelCardinality = 0;
	// for (int i = 0; i < filtered.numInstances(); i++) {
	// for (int j = 0; j < 1; j++) {
	// if (filtered.instance(i)
	// .stringValue(filtered.numAttributes() - 1)
	// .equals("1")) {
	// labelCardinality++;
	// }
	// }
	// }
	// cardinality = labelCardinality /= filtered.numInstances();
	// density = labelCardinality;
	// }
	// }
	
	@Override
	protected void buildInternal(final MultiLabelInstances trainingSet) throws Exception {
		// keeps only indices in filtered{Feature, Label}Indices
		filter = new Remove();
		final int[] groupIndices = Utils.arrayMerge(filteredFeatureIndices, filteredLabelIndices);
		filter.setAttributeIndicesArray(groupIndices);
		filter.setInvertSelection(true);
		filter.setInputFormat(trainingSet.getDataSet());
		final Instances filteredData = Filter.useFilter(trainingSet.getDataSet(), filter);
		numLabels = filteredLabelIndices.length;
		// switching case multi-/single-label classifier
		if (numLabels > 1) {
			// multi-label
			final MultiLabelInstances filteredMLData = trainingSet.reintegrateModifiedDataSet(filteredData);
			labelIndices = filteredMLData.getLabelIndices();
			featureIndices = filteredMLData.getFeatureIndices();
			mlClassifier.build(filteredMLData);
		} else {
			// single-label, class attribute must be set
			final Attribute classAttribute = filteredData.attribute(filteredData.numAttributes() - 1);
			filteredData.setClass(classAttribute);
			slClassifier.buildClassifier(filteredData);
		}
	}
	
	/**
	 * @return the label cardinality in the filtered dataset
	 */
	public double getCardinality() {
		return cardinality;
	}
	
	/**
	 * @return the label density in the filtered dataset
	 */
	public double getDensity() {
		return density;
	}
	
	/**
	 * @return feature indices used for filtering
	 */
	public int[] getFeatureIndices() {
		return filteredFeatureIndices;
	}
	
	/**
	 * @param data
	 *            {@link MultiLabelInstances}
	 * @return filtered {@link MultiLabelInstances}
	 * @throws Exception
	 *             if filteredData only has one label
	 */
	
	public MultiLabelInstances getFilteredData(final MultiLabelInstances data) throws Exception {
		final Instances filtered = Filter.useFilter(data.getDataSet(), filter);
		return data.reintegrateModifiedDataSet(filtered);
	}
	
	/**
	 * @param data
	 *            {@link MultiLabelInstances}
	 * @return filtered {@link MultiLabelInstances}
	 * @throws Exception
	 *             weka exception
	 */
	
	public Instances getFilteredSLData(final MultiLabelInstances data) throws Exception {
		final Instances filteredData = Filter.useFilter(data.getDataSet(), filter);
		final Attribute classAttribute = filteredData.attribute(filteredData.numAttributes() - 1);
		filteredData.setClass(classAttribute);
		return filteredData;
	}
	
	/**
	 * @return the count of distinct label combinations
	 */
	public int getLabelCombCount() {
		return labelCombCount;
	}
	
	/**
	 * @return label indices used for filtering
	 */
	public int[] getLabelIndices() {
		return filteredLabelIndices;
	}
	
	/**
	 * @return number of labels
	 */
	public int getNumLabels() {
		return numLabels;
	}
	
	/**
	 * @return the phi histogramm of the filtered dataset
	 */
	public double[] getPhi() {
		return phi;
	}
	
	@Override
	public TechnicalInformation getTechnicalInformation() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	protected MultiLabelOutput makePredictionInternal(final Instance instance) throws Exception, InvalidDataException {
		// keeps only indices in filtered{Feature, Label}Indices
		filter.input(instance);
		filter.batchFinished();
		final Instance filteredInstance = filter.output();
		// switching case multi-/single-label classifier
		if (numLabels > 1)
		// multi-label
			return mlClassifier.makePrediction(filteredInstance);
		// single-label, class attribute must be set
		final Attribute classAttribute =
				filteredInstance.dataset().attribute(filteredInstance.dataset().numAttributes() - 1);
		filteredInstance.dataset().setClass(classAttribute);
		// dummy bipartition/confidences arrays in order to create a
		// MultiLabelOutput object
		final boolean[] bipartition = new boolean[1];
		final double[] confidences = new double[1];
		// should be always be 2 since we have binary classification problems
		// {0,1}, could be more if
		// we have a multi-class problem or be less if the class is always 0|1
		double distribution[] = new double[2];
		distribution = slClassifier.distributionForInstance(filteredInstance);
		// maxIndex = max distribution
		int maxIndex = 0;
		for (int i = 0; i < distribution.length; i++)
			if (distribution[i] > distribution[maxIndex]) maxIndex = i;
		// to ensure right order of the classes {0,1},{1,0}
		bipartition[0] = classAttribute.value(maxIndex).equals("1") ? true : false;
		confidences[0] = distribution[classAttribute.indexOfValue("1")];
		return new MultiLabelOutput(bipartition, confidences);
	}
	
	/**
	 * @param indices
	 *            the feature attributes which will be kept in the training data set
	 */
	public void setFeaturesIndices(final int[] indices) {
		filteredFeatureIndices = indices;
	}
	
	/**
	 * @param indices
	 *            the label attributes which will be kept in the training data set
	 */
	public void setLabelIndices(final int[] indices) {
		filteredLabelIndices = indices;
	}
}
