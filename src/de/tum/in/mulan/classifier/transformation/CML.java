package de.tum.in.mulan.classifier.transformation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;

import mulan.classifier.MultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.core.matrix.Matrix;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

/**
 * classifier using groups defined upon clustering the attributes
 * 
 * @author LehnereS
 */
public class CML extends GroupBasedMetaClassifier {
	
	private static Instances transpose(final Instances dataSet) throws Exception {
		// loading dataSet in CSV format into a ByteArrayOutputStream
		final CSVSaver csvSaver = new CSVSaver();
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		csvSaver.setMissingValue("NaN");
		csvSaver.setDestination(output);
		csvSaver.setInstances(dataSet);
		csvSaver.writeBatch();
		// reading csv data from ByteArrayOutputStream encapsulated by a
		// BufferedReader for better
		// handling
		final BufferedReader inputStream =
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.toByteArray())));
		final double[][] matrix = new double[dataSet.numInstances()][dataSet.numAttributes()];
		// skipping first line as it contains attribute names
		inputStream.readLine();
		// reading line by line from csv format and filling matrix
		for (int row = 0; row < dataSet.numInstances(); row++) {
			final String[] splitted = inputStream.readLine().split(",");
			matrix[row] = new double[dataSet.numAttributes()];
			for (int i = 0; i < dataSet.numAttributes(); i++)
				matrix[row][i] = Double.valueOf(splitted[i]);
		}
		// transposing matrix
		String matlabMatrixString = new Matrix(matrix).transpose().toMatlab();
		// recreating csv format
		final StringBuilder tmpMatrix = new StringBuilder();
		// adding new attribute names
		for (int i = 0; i < matrix.length; i++)
			tmpMatrix.append("inst" + i + ",");
		// deleting last ",", appending newline
		tmpMatrix.deleteCharAt(tmpMatrix.length() - 1);
		tmpMatrix.append("\n");
		// deleting "[","]" from matlab matrix format
		matlabMatrixString = matlabMatrixString.replaceAll("\\[", "");
		matlabMatrixString = matlabMatrixString.replaceAll("\\]", "");
		// converting line by line from matlab matrix format to csv format
		final String[] matlabRows = matlabMatrixString.split(";");
		for (int i = 0; i < dataSet.numAttributes(); i++) {
			for (final String row : matlabRows[i].split(" "))
				if (row.length() > 1) tmpMatrix.append(row + ",");
			tmpMatrix.deleteCharAt(tmpMatrix.length() - 1);
			tmpMatrix.append("\n");
		}
		// loading transposed matrix in a weka csv loader and returning
		// transposed dataset
		final CSVLoader csvLoader = new CSVLoader();
		csvLoader.setMissingValue("NaN");
		csvLoader.setSource(new ByteArrayInputStream(tmpMatrix.toString().getBytes()));
		return csvLoader.getDataSet();
	}
	
	private final Clusterer	clusterer;
	
	/**
	 * @param clusterer
	 *            weka.clusterers.clusterer
	 */
	protected CML(final Clusterer clusterer) {
		this.clusterer = clusterer;
	}
	
	/**
	 * @param mlClassifier
	 *            mulan.classifier.MultiLabelLearner
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 * @param clusterer
	 *            weka.clusterers.clusterer
	 */
	public CML(final MultiLabelLearner mlClassifier, final Classifier slClassifier, final Clusterer clusterer) {
		super(slClassifier);
		this.mlClassifier = mlClassifier;
		this.clusterer = clusterer;
	}
	
	@Override
	protected int[][][] findLabelFeatureSets(final MultiLabelInstances trainingSet) throws Exception {
		debug("normalizing & transposing dataset...");
		final Normalize normalizer = new Normalize();
		normalizer.setInputFormat(trainingSet.getDataSet());
		normalizer.setScale(1);
		normalizer.setTranslation(0);
		final Instances transposed = CML.transpose(Filter.useFilter(trainingSet.getDataSet(), normalizer));
		debug("normalizing & transposing dataset...done");
		debug("learning clusters...");
		clusterer.buildClusterer(transposed);
		debug("number of clusters found: " + clusterer.numberOfClusters());
		debug("learning clusters...done");
		debug("finding cluster sets...");
		final int[][][] labelFeatureSets = generateLabelFeatureSets(transposed);
		debug("finding cluster sets...done");
		return labelFeatureSets;
	}
	
	private int[][][] generateLabelFeatureSets(final Instances data) throws Exception {
		@SuppressWarnings ("unchecked")
		final Set<Integer>[][] labelFeatureSets = new Set[clusterer.numberOfClusters()][2];
		// each instance is representing a attribute
		for (int curInstanceIndex = 0; curInstanceIndex < data.numInstances(); curInstanceIndex++) {
			final int curClusterID = clusterer.clusterInstance(data.instance(curInstanceIndex));
			// initializing labelset
			if (labelFeatureSets[curClusterID][0] == null) labelFeatureSets[curClusterID][0] = new TreeSet<>();
			// initializing featureset
			if (labelFeatureSets[curClusterID][1] == null) labelFeatureSets[curClusterID][1] = new TreeSet<>();
			// if current attribute is a label adding to labelset otherwise
			// adding to featureset
			if (labelIndicesSet.contains(curInstanceIndex)) labelFeatureSets[curClusterID][0].add(curInstanceIndex);
			else labelFeatureSets[curClusterID][1].add(curInstanceIndex);
		}
		
		return compactLabelFeatureSets(labelFeatureSets);
	}
}
