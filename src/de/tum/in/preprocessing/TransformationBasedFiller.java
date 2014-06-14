package de.tum.in.preprocessing;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;

/**
 * @author LehnereS
 */
public abstract class TransformationBasedFiller implements Filler {
	
	protected final Classifier	baseClassifier;
	
	/**
     * 
     */
	public TransformationBasedFiller() {
		this(new SMO());
	}
	
	/**
	 * @param baseClassifier
	 */
	public TransformationBasedFiller(final Classifier baseClassifier) {
		this.baseClassifier = baseClassifier;
	}
	
	/**
	 * @return baseClassifier
	 */
	public Classifier getBaseClassifier() {
		return baseClassifier;
	}
	
	protected Classifier getCopyOfBaseClassifier() {
		Classifier c;
		try {
			c = AbstractClassifier.makeCopy(baseClassifier);
			return c;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getName() {
		return getTransformationName() + "(" + baseClassifier.getClass().getSimpleName() + ")";
	}
	
	protected abstract String getTransformationName();
}
