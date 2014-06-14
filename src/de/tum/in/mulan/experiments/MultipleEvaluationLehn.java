package de.tum.in.mulan.experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import mulan.evaluation.MultipleEvaluation;

/**
 * copy of {@link MultipleEvaluation} to gain direct access to memebers and change evaluation object to {@link HashMap}
 * 
 * @author LehnereS
 */
@SuppressWarnings ("javadoc")
public class MultipleEvaluationLehn {
	
	private final ArrayList<HashMap<String, Double>>	evaluations;
	
	private HashMap<String, Double>						mean;
	private HashMap<String, Double>						standardDeviation;
	
	/**
	 * Constructs a new object
	 */
	public MultipleEvaluationLehn() {
		evaluations = new ArrayList<>();
	}
	
	/**
	 * Constructs a new object with given array of evaluations and calculates statistics
	 * 
	 * @param someEvaluations
	 */
	public MultipleEvaluationLehn(final HashMap<String, Double>[] someEvaluations) {
		evaluations = new ArrayList<>();
		for (final HashMap<String, Double> e : someEvaluations)
			evaluations.add(e);
		calculateStatistics();
	}
	
	/**
	 * Adds an evaluation results to the list of evaluations
	 * 
	 * @param evaluation
	 *            an evaluation result
	 */
	public void addEvaluation(final HashMap<String, Double> evaluation) {
		evaluations.add(evaluation);
	}
	
	/**
	 * Computes mean and standard deviation of all evaluation measures
	 */
	public void calculateStatistics() {
		final int size = evaluations.size();
		HashMap<String, Double> sums = new HashMap<>();
		
		// calculate sums of measures
		for (int i = 0; i < evaluations.size(); i++)
			for (final String m : evaluations.get(i).keySet()) {
				double value = Double.NaN;
				try {
					value = evaluations.get(i).get(m);
				} catch (final Exception ex) {
					// empty
				}
				if (sums.containsKey(m)) sums.put(m, sums.get(m) + value);
				else sums.put(m, value);
			}
		mean = new HashMap<>();
		for (final String measureName : sums.keySet())
			mean.put(measureName, sums.get(measureName) / size);
		
		// calculate sums of squared differences from mean
		sums = new HashMap<>();
		
		for (int i = 0; i < evaluations.size(); i++)
			for (final String m : evaluations.get(i).keySet()) {
				double value = Double.NaN;
				try {
					value = evaluations.get(i).get(m);
				} catch (final Exception ex) {
					// empty
				}
				if (sums.containsKey(m)) sums.put(m, sums.get(m) + Math.pow(value - mean.get(m), 2));
				else sums.put(m, Math.pow(value - mean.get(m), 2));
			}
		standardDeviation = new HashMap<>();
		for (final String measureName : sums.keySet())
			standardDeviation.put(measureName, Math.sqrt(sums.get(measureName) / size));
	}
	
	public Double get(final String measure) {
		return mean.get(measure);
	}
	
	/**
	 * Returns the mean value of a measure
	 * 
	 * @param measureName
	 *            the name of the measure
	 * @return the mean value of the measure
	 */
	public double getMean(final String measureName) {
		return mean.get(measureName);
	}
	
	public Set<String> getMeasures() {
		return evaluations.get(0).keySet();
	}
	
	/**
	 * Returns a CSV string representation of the results
	 * 
	 * @return a CSV string representation of the results
	 */
	public String toCSV() {
		final StringBuilder sb = new StringBuilder();
		for (final String m : evaluations.get(0).keySet()) {
			final String measureName = m;
			sb.append(String.format("%.4f", mean.get(measureName)));
			sb.append("\u00B1");
			sb.append(String.format("%.4f", standardDeviation.get(measureName)));
			sb.append(";");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final String m : evaluations.get(0).keySet()) {
			final String measureName = m;
			sb.append(measureName);
			sb.append(": ");
			sb.append(String.format("%.4f", mean.get(measureName)));
			sb.append("\u00B1");
			sb.append(String.format("%.4f", standardDeviation.get(measureName)));
			sb.append("\n");
		}
		return sb.toString();
	}
}
