package edu.unituebingen.datamining.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import edu.unituebingen.datamining.trees.ID3Tree;

/**
 * @author LehnereS
 *
 *         class containing simple evaluation schemes for my ID3Tree implementation
 *
 */
public class Evaluation {

	/**
	 * evaluates a tree with a testset
	 *
	 * @param instances
	 *            the testset
	 * @param tree
	 *            the tree
	 * @return confusion matrix
	 */
	public static int[][] evaluate(final List<Instance> instances, final ID3Tree tree) {
		final int[][] matrix = new int[2][2];
		for (final Instance instance : instances) {
			final int predictedLabel = tree.classify(instance);
			if (predictedLabel > Integer.MIN_VALUE && instance.label > Integer.MIN_VALUE)
				matrix[predictedLabel][instance.label]++;
			else System.err.println("could not predict label (" + predictedLabel + "," + instance.label + ")");
		}
		return matrix;
	}

	/**
	 * prints the accuracy for a confusion matrix
	 *
	 * @param matrix
	 *            the confusion matrix
	 */
	public static void printMeasures(final int[][] matrix) {
		final int TP = matrix[1][1];
		final int FP = matrix[1][0];
		final int TN = matrix[0][0];
		final int FN = matrix[0][1];
		System.out
				.println("accuracy: "
						+ (TP + TN)
						/ (double) (TP + TN + FP + FN));
		System.out
				.println("Recall: "
						+ TP
						/ (double) (TP + FN));
		System.out
				.println("precision: "
						+ TP
						/ (double) (TP + FP));
	}

	/**
	 * performs a crossvalidation for my ID3Tree implementation
	 *
	 * @param instances
	 *            dataset
	 * @param folds
	 *            number of folds
	 * @param random
	 *            a random number generator for shuffeling the instances
	 */
	public static void xEvaluate(final List<Instance> instances, final int folds, final Random random) {
		final int[][] matrix = new int[2][2];
		Collections.shuffle(instances, random);

		final int testSetSize = instances.size() / folds;
		for (int curFold = 0; curFold < folds; curFold++) {
			final List<Instance> trainingSet = new LinkedList<>(instances);
			final List<Instance> testSet = new LinkedList<>(
					trainingSet.subList(curFold * testSetSize, curFold
							* testSetSize + testSetSize));
			trainingSet.removeAll(testSet);
			final ID3Tree tree = new ID3Tree(trainingSet);
			final int[][] curMatrix = evaluate(testSet, tree);
			matrix[0][0] += curMatrix[0][0];
			matrix[0][1] += curMatrix[0][1];
			matrix[1][0] += curMatrix[1][0];
			matrix[1][1] += curMatrix[1][1];
		}
		printMeasures(matrix);
	}
}