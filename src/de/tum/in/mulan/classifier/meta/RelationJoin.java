package de.tum.in.mulan.classifier.meta;

import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/**
 * @author LehnereS class for comparison of noble to naive learners Instead of relative learning, datasets are simply
 *         joined
 */
@SuppressWarnings ("deprecation")
public class RelationJoin extends AbstractNoble {
	
	private static Instance joinInstance(Instance in_left, Instance in_top) {
		final FastVector<Attribute> atts = new FastVector<>();
		for (int i = 0; i < in_left.numAttributes(); i++)
			atts.addElement(in_left.attribute(i));
		
		for (int i = 0; i < in_top.numAttributes(); i++)
			atts.addElement(in_top.attribute(i));
		final FastVector<String> attVals = new FastVector<>();
		attVals.addElement("0");
		attVals.addElement("1");
		atts.addElement(new Attribute("class", attVals));
		
		final Instances join = new Instances("join", atts, 0);
		join.setClassIndex(join.numAttributes() - 1);
		
		final double[] vals_left = in_left.toDoubleArray();
		final double[] vals_top = in_top.toDoubleArray();
		final double[] vals_final = new double[vals_left.length + vals_top.length + 1];
		for (int k = 0; k < vals_left.length; k++)
			vals_final[k] = vals_left[k];
		for (int k = vals_left.length; k < vals_top.length; k++)
			vals_final[k] = vals_top[k - vals_left.length];
		
		vals_final[vals_final.length - 1] = Utils.missingValue();
		final Instance joined = new DenseInstance(1.0, vals_final);
		joined.setDataset(join);
		return joined;
	}
	
	private static Instances joinInstances(Instances in_left, Instances in_top, Instances in_center) {
		
		final FastVector<Attribute> atts = new FastVector<>();
		
		for (int i = 0; i < in_left.numAttributes(); i++)
			atts.addElement(in_left.attribute(i));
		
		for (int i = 0; i < in_top.numAttributes(); i++)
			atts.addElement(in_top.attribute(i));
		
		final FastVector<String> attVals = new FastVector<>();
		attVals.addElement("0");
		attVals.addElement("1");
		atts.addElement(new Attribute("class", attVals));
		
		final Instances join = new Instances("join", atts, 0);
		
		for (int i = 0; i < in_left.numInstances(); i++) {
			final double[] vals_left = in_left.get(i).toDoubleArray();
			for (int j = 0; j < in_top.numInstances(); j++) {
				final double[] vals_top = in_top.get(j).toDoubleArray();
				final double[] vals_final = new double[vals_left.length + vals_top.length + 1];
				for (int k = 0; k < vals_left.length; k++)
					vals_final[k] = vals_left[k];
				for (int k = vals_left.length; k < vals_top.length; k++)
					vals_final[k] = vals_top[k - vals_left.length];
				
				vals_final[vals_final.length - 1] = in_center.get(i).value(j);
				join.add(new DenseInstance(1.0, vals_final));
			}
		}
		return join;
	}
	
	Classifier	baseClassifier;
	
	/**
	 * @param baseClassifier
	 *            {@link Classifier} instantiating simple BR as learner
	 */
	public RelationJoin(Classifier baseClassifier) {
		this.baseClassifier = baseClassifier;
	}
	
	@Override
	public void build(Instances in_left, Instances in_top, Instances in_center) throws InvalidDataException, Exception {
		final Instances finalInstances = joinInstances(in_left, in_top, in_center);
		finalInstances.setClassIndex(finalInstances.numAttributes() - 1);
		baseClassifier.buildClassifier(finalInstances);
	}
	
	@Override
	public double classifyInstances(Instance in_left, Instance in_top) throws InvalidDataException,
			ModelInitializationException, Exception {
		if (isTranspose()) {
			in_left = in_top;
			in_top = in_left;
		}
		
		final Instance joined = joinInstance(in_left, in_top);
		return baseClassifier.classifyInstance(joined);
	}
	
	@Override
	@SuppressWarnings ("unused")
	public double distributionForInstances(Instance in_left, Instance in_top) {
		joinInstance(in_left, in_top);
		// return baseClassifier.distributionForInstance(joined);
		return Double.NaN;
	}
	
}
