package de.tum.in.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

@SuppressWarnings ("javadoc")
public abstract class AbstractClassifierChainFiller extends TransformationBasedFiller {
	
	protected static int deleteAtts(int l, final int[] atts, final Instances i) {
		Arrays.sort(atts);
		for (int j = atts.length - 1; j >= 0; j--) {
			i.deleteAttributeAt(atts[j]);
			if (l > atts[j]) l--;
		}
		return l;
	}
	
	public AbstractClassifierChainFiller() {
		super();
	}
	
	public AbstractClassifierChainFiller(final Classifier baseClassifier) {
		super(baseClassifier);
	}
	
	/**
	 * Selects all instances of the dataset which (don't) have a missing value in the class attribute.
	 * 
	 * @param missing
	 *            instances with (true) or without (false) missing value
	 * @param l
	 *            index of the attribute of interest
	 * @param instances
	 *            dataset
	 * @return Instances with instances according the rules.
	 */
	protected Instances filter(final boolean missing, final int l, final Instances instances) {
		int n = 0;
		for (final Instance i : instances)
			if (i.isMissing(l) == missing) n++;
		final Instances result = new Instances(instances, n);
		for (final Instance i : instances)
			if (i.isMissing(l) == missing) result.add(i);
		return result;
	}
	
	/**
	 * Selects the indices of instances from the dataset which (don't) have a missing value in the class attribute.
	 * 
	 * @param missing
	 *            instances with (true) or without (false) missing value
	 * @param l
	 *            index of the attribute of interest
	 * @param instances
	 *            dataset
	 * @return Instances with instances according the rules.
	 */
	protected int[] filterIndices(final boolean missing, final int l, final Instances instances) {
		int n = 0;
		for (final Instance i : instances)
			if (i.isMissing(l) == missing) n++;
		final int[] result = new int[n];
		int r = 0;
		for (int i = 0; i < instances.numInstances(); i++)
			if (instances.get(i).isMissing(l) == missing) result[r++] = i;
		return result;
	}
	
	protected int[] getRandomOrder(final int[] labelAttributes) {
		final List<Integer> list = new ArrayList<>();
		for (final int i : labelAttributes)
			list.add(i);
		Collections.shuffle(list);
		final int[] result = new int[list.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = list.get(i);
		return result;
	}
}
