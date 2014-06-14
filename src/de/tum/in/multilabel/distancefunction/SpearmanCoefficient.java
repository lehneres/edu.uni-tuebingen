package de.tum.in.multilabel.distancefunction;

import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 * implements SpearmanCoefficient distance for weka
 * 
 * @author LehnereS
 */
public class SpearmanCoefficient extends EuclideanDistance {
	
	/**
	 * little hack as spearman is not normalizable. constructor set normalize to false
	 */
	public SpearmanCoefficient() {
		setDontNormalize(true);
	}
	
	/**
	 * Calculates the distance between two instances.
	 * 
	 * @param first
	 *            the first instance
	 * @param second
	 *            the second instance
	 * @return the distance between the two given instances
	 */
	@Override
	public double distance(final Instance first, final Instance second) {
		return 1 - (1 - 6 * this.distance(first, second, Double.POSITIVE_INFINITY)
				/ (first.numAttributes() * (Math.pow(first.numAttributes(), 2) - 1)));
	}
	
	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed information about the technical
	 * background of this class, e.g., paper reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;
		
		result = new TechnicalInformation(Type.MISC);
		result.setValue(Field.AUTHOR, "Wikipedia");
		result.setValue(Field.TITLE, "SpearmanCoefficient correlation coefficient");
		result.setValue(Field.URL, "http://en.wikipedia.org/wiki/SpearmanCoefficient%27s_rank_correlation_coefficient");
		
		return result;
	}
	
	/**
	 * Returns a string describing this object.
	 * 
	 * @return a description of the evaluator suitable for displaying in the explorer/experimenter gui
	 */
	@Override
	public String globalInfo() {
		return "SpearmanCoefficient's rank correlation coefficient or SpearmanCoefficient's rho,"
				+ "named after Charles SpearmanCoefficient and often denoted by the Greek"
				+ "letter (rho) or as rs, is a non-parametric measure of"
				+ "statistical dependence between two variables. It assesses"
				+ "how well the relationship between two variables can be"
				+ "described using a monotonic function. If there are no"
				+ "repeated data values, a perfect SpearmanCoefficient correlation of +1"
				+ "or −1 occurs when each of the variables is a perfect" + "monotone function of the other.\n\n"
				+ "For more information, see:\n\n" + getTechnicalInformation().toString();
	}
	
	/**
	 * Updates the current distance calculated so far with the new difference between two attributes. The difference
	 * between the attributes was calculated with the difference(int,double,double) method.
	 * 
	 * @param currDist
	 *            the current distance calculated so far
	 * @param diff
	 *            the difference between two new attributes
	 * @return the update distance
	 * @see #difference(int, double, double)
	 */
	@Override
	protected double updateDistance(final double currDist, final double diff) {
		double result;
		
		result = currDist;
		result += diff * diff;
		
		return result;
	}
}
