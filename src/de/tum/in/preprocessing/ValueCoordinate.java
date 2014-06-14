package de.tum.in.preprocessing;

import weka.core.Attribute;

/**
 * @author LehnereS
 */
public class ValueCoordinate {
	private final int	instance;
	private final int	feature;
	
	/**
	 * @param instance
	 * @param attribute
	 */
	public ValueCoordinate(final int instance, final Attribute attribute) {
		super();
		this.instance = instance;
		feature = attribute.index();
	}
	
	/**
	 * @param instance
	 * @param feature
	 */
	public ValueCoordinate(final int instance, final int feature) {
		super();
		this.instance = instance;
		this.feature = feature;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ValueCoordinate other = (ValueCoordinate) obj;
		if (feature != other.feature) return false;
		if (instance != other.instance) return false;
		return true;
	}
	
	/**
	 * @return feature
	 */
	public int getFeature() {
		return feature;
	}
	
	/**
	 * @return instance
	 */
	public int getInstance() {
		return instance;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + feature;
		result = prime * result + instance;
		return result;
	}
	
	@Override
	public String toString() {
		return "ValueCoordinate [instance=" + instance + ", feature=" + feature + "]";
	}
}
