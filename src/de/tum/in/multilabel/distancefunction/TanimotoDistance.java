package de.tum.in.multilabel.distancefunction;

import weka.core.Instance;
import weka.core.NormalizableDistance;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.neighboursearch.PerformanceStats;

/**
 * implements tanimoto distance for weka
 * 
 * @author LehnereS
 */
public class TanimotoDistance extends NormalizableDistance implements TechnicalInformationHandler {
	/**
	 * constructor set normalize to false
	 */
	public TanimotoDistance() {
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
		return -1 * Math.log(this.distance(first, second, null)) / Math.log(2);
	}
	
	/**
	 * Calculates the distance between two instances. Offers speed up (if the distance function class in use supports
	 * it) in nearest neighbor search by taking into account the cutOff or maximum distance. Depending on the distance
	 * function class, post processing of the distances by postProcessDistances(double []) may be required if this
	 * function is used.
	 * 
	 * @param first
	 *            the first instance
	 * @param second
	 *            the second instance
	 * @param cutOffValue
	 *            If the distance being calculated becomes larger than cutOffValue then the rest of the calculation is
	 *            discarded.
	 * @param stats
	 *            the performance stats object
	 * @return the distance between the two given instances or Double.POSITIVE_INFINITY if the distance being calculated
	 *         becomes larger than cutOffValue.
	 */
	@Override
	public double distance(final Instance first, final Instance second, final double cutOffValue,
			final PerformanceStats stats) {
		if (first.numAttributes() != second.numAttributes())
			throw new UnsupportedOperationException(
					"can only compute difference of instances with equal number attributes");
		
		final int numAttributes = first.numAttributes();
		double sumCon = 0, sum = 0;
		
		for (int i = 0; i < numAttributes; i++)
			if (first.value(i) == second.value(i)) sumCon++;
			else if (first.value(i) == 1 || second.value(i) == 1) sum++;
		
		return sumCon / sum;
	}
	
	@Override
	public String getRevision() {
		throw new UnsupportedOperationException("not supported");
	}
	
	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;
		
		result = new TechnicalInformation(Type.MISC);
		result.setValue(Field.AUTHOR, "Wikipedia");
		result.setValue(Field.TITLE, "Tanimoto distance (Jaccard index)");
		result.setValue(Field.URL, "http://en.wikipedia.org/wiki/Jaccard_index");
		
		return result;
	}
	
	@Override
	public String globalInfo() {
		return "The Tanimoto index, also known as the Tanimoto similarity coefficient"
				+ " is a statistic used for comparing the similarity and diversity " + "of sample sets.\n\n"
				+ "For more information, see:\n\n" + getTechnicalInformation().toString();
	}
	
	@Override
	protected double updateDistance(final double currDist, final double diff) {
		return Double.NaN; // useless
	}
}