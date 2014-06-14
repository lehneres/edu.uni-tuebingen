package de.tum.in.mulan.experiments;

import java.io.Serializable;
import java.util.HashMap;

/**
 * stores results from extended evaluation
 * 
 * @author LehnereS
 */
public class DataStore implements Serializable {
	String[]					options;
	int							currentNC, group;
	int[]						labelIndices, featureIndices;
	HashMap<String, Double>[]	measures;
	double[][]					confusionMatrix;
}
