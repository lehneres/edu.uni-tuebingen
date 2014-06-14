package de.tum.in.mulan.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;

import weka.core.Utils;

/**
 * reads result files from experiments and aggregates them in one csv file3
 * 
 * @author LehnereS
 */
@SuppressWarnings ("javadoc")
public class CSVLoader {
	
	static String			folderString	= "results/field/current/treshold";
	static StringBuilder	csv				= new StringBuilder();
	static StringBuilder	header			= new StringBuilder();
	static boolean			readHeader		= true;
	
	@SuppressWarnings ("resource")
	public static void main(final String[] args) throws NumberFormatException, Exception {
		final File folder = new File(CSVLoader.folderString);
		CSVLoader.readFolder(folder);
		final BufferedWriter writer = new BufferedWriter(new FileWriter(new File("results.tsv")));
		writer.write(CSVLoader.header.toString() + "\n");
		writer.write(CSVLoader.csv.toString());
	}
	
	@SuppressWarnings ("resource")
	public static void putData(final File file) throws IOException, ClassNotFoundException, SQLException, Exception {
		final ObjectInputStream oi = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
		final DataStore data = (DataStore) oi.readObject();
		final String[] opt = data.options.clone();
		System.err.println("processing experiment: " + file.getAbsolutePath());
		if (CSVLoader.readHeader) {
			CSVLoader.header.append("path" + "\t");
			CSVLoader.header.append("dataset" + "\t");
			CSVLoader.header.append("mllearner" + "\t");
			CSVLoader.header.append("sllearner" + "\t");
			CSVLoader.header.append("clusterer" + "\t");
			CSVLoader.header.append("log" + "\t");
			CSVLoader.header.append("distancemeasure" + "\t");
			CSVLoader.header.append("clustermethod" + "\t");
			CSVLoader.header.append("sllearner" + "\t");
			CSVLoader.header.append("threshold" + "\t");
			CSVLoader.header.append("nc" + "\t");
			CSVLoader.header.append("group" + "\t");
		}
		CSVLoader.csv.append(file.getAbsolutePath() + "\t");
		CSVLoader.csv.append(TestSuite.dataSets[Integer.valueOf(Utils.getOption("data", opt))] + "\t");
		CSVLoader.csv.append(TestSuite.multiLabelLearner[Integer.valueOf(Utils.getOption("ml", opt))] + "\t");
		CSVLoader.csv.append(TestSuite.singleLabelLearner[Integer.valueOf(Utils.getOption("sl", opt))] + "\t");
		CSVLoader.csv.append(TestSuite.clusterAlgorithms[Integer.valueOf(Utils.getOption("cl", opt))] + "\t");
		CSVLoader.csv.append(Utils.getFlag("log", opt) + "\t");
		final String dm = Utils.getOption("dm", opt);
		if (dm.length() > 0) {
			final int dmint = Integer.valueOf(dm);
			if (dmint > -1) CSVLoader.csv.append(TestSuite.distanceFunctions[dmint] + "\t");
		}
		final String cm = Utils.getOption("cm", opt);
		if (cm.length() > 0 && Integer.valueOf(cm) > -1)
			CSVLoader.csv.append(TestSuite.clusterMethodNames[Integer.valueOf(cm)] + "\t");
		CSVLoader.csv.append(TestSuite.ASEvaluation[Integer.valueOf(Utils.getOption("ae", data.options))] + "\t");
		CSVLoader.csv.append(Utils.getOption("t", opt) + "\t");
		CSVLoader.csv.append(data.currentNC + "\t");
		CSVLoader.csv.append((file.getAbsolutePath().contains("group") ? data.group + "" : -1 + "") + "\t");
		CSVLoader.putMeasures(data);
		oi.close();
		CSVLoader.csv.append("\n");
	}
	
	private static void putMeasures(final DataStore data) {
		if (data.confusionMatrix != null) {
			CSVLoader.csv.append(data.confusionMatrix[0][0] + "\t");
			CSVLoader.csv.append(data.confusionMatrix[0][1] + "\t");
			CSVLoader.csv.append(data.confusionMatrix[1][0] + "\t");
			CSVLoader.csv.append(data.confusionMatrix[1][1] + "\t");
			CSVLoader.csv.append(data.confusionMatrix[0][0] / (data.confusionMatrix[0][0] + data.confusionMatrix[1][0])
					+ "\t");
			CSVLoader.csv
					.append((data.confusionMatrix[0][0] + data.confusionMatrix[1][1])
							/ (data.confusionMatrix[0][0] + data.confusionMatrix[1][1] + data.confusionMatrix[1][0] + data.confusionMatrix[0][1])
							+ "\t");
		} else if (data.measures != null) {
			final MultipleEvaluationLehn eval = new MultipleEvaluationLehn(data.measures);
			if (CSVLoader.readHeader) {
				CSVLoader.header.append("TP" + "\t");
				CSVLoader.header.append("FP" + "\t");
				CSVLoader.header.append("FN" + "\t");
				CSVLoader.header.append("TN" + "\t");
				CSVLoader.header.append("recall" + "\t");
				CSVLoader.header.append("accuracy" + "\t");
			}
			CSVLoader.csv.append("\t\t\t\t\t\t");
			for (final String measure : eval.getMeasures()) {
				if (CSVLoader.readHeader) CSVLoader.header.append(measure + "\t");
				CSVLoader.csv.append(String.format("%.4f", eval.get(measure)) + "\t");
			}
		}
		CSVLoader.readHeader = false;
	}
	
	private static void readFolder(final File file) throws NumberFormatException, Exception {
		if (file.isDirectory()) for (final File file2 : file.listFiles())
			CSVLoader.readFolder(file2);
		else if (file.isFile() && file.getName().contains("data.ob")) CSVLoader.putData(file);
	}
}
