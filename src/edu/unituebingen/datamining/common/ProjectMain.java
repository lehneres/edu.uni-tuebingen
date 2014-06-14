package edu.unituebingen.datamining.common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import au.com.bytecode.opencsv.CSVReader;
import edu.unituebingen.datamining.trees.ID3Tree;

/**
 *
 * @author LehnereS
 *
 */
public class ProjectMain {

	/**
	 * main method for the decision tree project. trains a classifier, saves it to a dotfile and
	 * evaluates it
	 *
	 * @param args
	 *            arg1: feature instances arg2: labels
	 * @throws IOException
	 */
	@SuppressWarnings("resource") public static void main(final String[] args) throws IOException {
		final CSVReader dataset = new CSVReader(new FileReader(args[0]));
		final CSVReader classes = new CSVReader(new FileReader(args[1]));
		final List<Instance> instances = readInstances(dataset.readAll(), classes.readAll());
		final ID3Tree tree = new ID3Tree(instances);
		final String dotTree = tree.getDotString();
		final FileWriter file = new FileWriter(new File("dottree.dot"));
		file.write(dotTree);
		file.flush();
		file.close();
		final int[][] matrix = Evaluation.evaluate(instances, tree);
		System.out.println(Arrays.deepToString(matrix));
		Evaluation.xEvaluate(instances, 10, new Random(12345));
	}

	private static List<Instance> readInstances(final List<String[]> dataset, final List<String[]> classes) {
		final List<Instance> instances = new LinkedList<>();
		for (int i = 0; i < dataset.size(); i++) {
			final String[] entry = dataset.get(i);
			final double[] values = new double[entry.length];
			final int[] attributes = new int[entry.length];
			for (int j = 0; j < entry.length; j++) {
				values[j] = new Double(entry[j]);
				attributes[j] = j;
			}
			if (classes != null)
				instances.add(new Instance(values, attributes, new Double(classes.get(i)[0]).intValue()));
			else instances.add(new Instance(values, attributes));
		}
		return instances;
	}
}