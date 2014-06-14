package edu.unituebingen.datamining.common;

import java.util.Random;

/**
 * @author LehnereS
 *
 *         Instance of an dataset
 *
 */
public class Instance {
	
	/**
	 * the id of this instance
	 */
	public int		id;
	/**
	 * the values of this instance
	 */
	public int[]	attributes;
	/**
	 * the values of this instance
	 */
	public double[]	values;
	/**
	 * the label for this instance. If not set label = Integer.MIN_VALUE
	 */
	public int		label;
	
	/**
	 * @param values
	 *            the values for this instance
	 * @param attributes
	 *            attribute ids
	 */
	public Instance(final double[] values, final int[] attributes) {
		this(new Random().nextInt(), values, attributes, Integer.MIN_VALUE);
	}
	
	/**
	 * @param values
	 *            the values for this instance
	 * @param attributes
	 *            attribute ids
	 * @param label
	 *            the label of this instance
	 */
	public Instance(final double[] values, final int[] attributes, final int label) {
		this(new Random().nextInt(), values, attributes, label);
	}

	/**
	 * @param id
	 *            the id of this instance
	 * @param values
	 *            the values for this instance
	 * @param attributes
	 *            attribute ids
	 * @param label
	 *            the label of this instance
	 */
	public Instance(final int id, final double[] values, final int[] attributes, final int label) {
		this.id = id;
		this.values = values;
		this.attributes = attributes;
		this.label = label;
	}

}
