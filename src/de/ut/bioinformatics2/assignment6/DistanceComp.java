package de.ut.bioinformatics2.assignment6;

import java.util.Comparator;

/**
 * comparator for node distances
 * 
 * @author LehnereS
 * 
 */
public class DistanceComp implements Comparator<Node> {
	
	int[]		distances;

	/**
	 * @param distances
	 *            the distances
	 */
	public DistanceComp(int[] distances) {
		this.distances = distances;
	}
	
	@Override
	public int compare(Node o1, Node o2) {
		return distances[o1.getId()] - distances[o2.getId()];
	}
}