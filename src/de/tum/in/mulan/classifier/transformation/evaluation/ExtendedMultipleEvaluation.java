package de.tum.in.mulan.classifier.transformation.evaluation;

import java.util.ArrayList;
import java.util.HashMap;

import mulan.evaluation.Evaluation;
import mulan.evaluation.measure.Measure;

/**
 * @author LehnereS
 */
public class ExtendedMultipleEvaluation {
	
	ArrayList<Evaluation>	evaluations;
	HashMap<String, Double>	mean, standardDeviation;
	
	/**
	 * @param someEvaluations
	 *            array of mulan evaluations
	 */
	public ExtendedMultipleEvaluation(final Evaluation[] someEvaluations) {
		evaluations = new ArrayList<>();
		for (final Evaluation e : someEvaluations)
			evaluations.add(e);
		calculateStatistics();
	}
	
	/**
	 * @param evaluation
	 *            a mulan evaluation
	 */
	public void addEvaluation(final Evaluation evaluation) {
		if (evaluations == null) evaluations = new ArrayList<>();
		evaluations.add(evaluation);
	}
	
	/**
	 * calculating statistics, code from mulan
	 */
	public void calculateStatistics() {
		final int size = evaluations.size();
		HashMap<String, Double> sums = new HashMap<>();
		
		// calculate sums of measures
		for (int i = 0; i < evaluations.size(); i++)
			for (final Measure m : evaluations.get(i).getMeasures()) {
				double value = Double.NaN;
				try {
					value = m.getValue();
				} catch (final Exception ex) {/* nothing to do here */
				}
				if (sums.containsKey(m.getName())) sums.put(m.getName(), sums.get(m.getName()) + value);
				else sums.put(m.getName(), value);
			}
		mean = new HashMap<>();
		for (final String measureName : sums.keySet())
			mean.put(measureName, sums.get(measureName) / size);
		
		// calculate sums of squared differences from mean
		sums = new HashMap<>();
		
		for (int i = 0; i < evaluations.size(); i++)
			for (final Measure m : evaluations.get(i).getMeasures()) {
				double value = Double.NaN;
				try {
					value = m.getValue();
				} catch (final Exception ex) {/* nothing to do here */
				}
				if (sums.containsKey(m.getName())) sums.put(m.getName(),
						sums.get(m.getName()) + Math.pow(value - mean.get(m.getName()), 2));
				else sums.put(m.getName(), Math.pow(value - mean.get(m.getName()), 2));
			}
		standardDeviation = new HashMap<>();
		for (final String measureName : sums.keySet())
			standardDeviation.put(measureName, Math.sqrt(sums.get(measureName) / size));
	}
	
	/**
	 * @param measureName
	 *            name of the measure
	 * @return mean
	 */
	public double getMean(final String measureName) {
		return mean.get(measureName);
	}
}
