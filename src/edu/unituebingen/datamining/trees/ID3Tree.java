package edu.unituebingen.datamining.trees;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.unituebingen.datamining.common.Instance;

/**
 * @author LehnereS
 *
 */
public class ID3Tree {
	
	private static double computeEntropy(final Collection<Instance> instances) {
		final double[] probalities = new double[3];
		for (final Instance instance : instances)
			probalities[instance.label != Integer.MIN_VALUE ? instance.label + 1
					: 0]++;

		probalities[0] /= instances.size();
		probalities[1] /= instances.size();
		probalities[2] /= instances.size();
		
		double entropy = 0;
		for (final double prob : probalities)
			entropy += prob > 0 ? -1
					* (prob * Math.log(prob) / Math.log(2)) : 0;
		return entropy;
	}
	
	private static double computeGain(final double entropy, final int size,
			final HashMap<Double, ArrayList<Instance>> allSplits) {
		double weightedEntropy = 0;
		for (final ArrayList<Instance> split : allSplits.values())
			weightedEntropy += split.size() / (double) size
					* computeEntropy(split);
		return entropy - weightedEntropy;
	}
	
	private static HashMap<Double, ArrayList<Instance>>[] computeSplits(
			final List<Instance> instances) {
		@SuppressWarnings("unchecked") final HashMap<Double, ArrayList<Instance>>[] variableSplits = new HashMap[instances
				.get(0).values.length];
		for (int attribute = 0; attribute < instances.get(0).values.length; attribute++) {
			variableSplits[attribute] = new HashMap<>();
			for (final Instance instance : instances) {
				if (!variableSplits[attribute]
						.containsKey(instance.values[attribute]))
					variableSplits[attribute].put(instance.values[attribute],
							new ArrayList<Instance>());
				variableSplits[attribute].get(instance.values[attribute]).add(
						instance);
			}
		}
		return variableSplits;
	}
	
	private static ArrayList<Instance> removeAttribute(
			final ArrayList<Instance> instances, final int attribute) {
		final ArrayList<Instance> newInstances = new ArrayList<>();
		for (final Instance instance : instances) {
			final double[] newValues = new double[instance.values.length - 1];
			System.arraycopy(instance.values, 0, newValues, 0, attribute);
			System.arraycopy(instance.values, attribute + 1, newValues,
					attribute, instance.values.length - 1 - attribute);
			
			final int[] newAttributes = new int[instance.attributes.length - 1];
			System.arraycopy(instance.attributes, 0, newAttributes, 0,
					attribute);
			System.arraycopy(instance.attributes, attribute + 1, newAttributes,
					attribute, instance.attributes.length - 1 - attribute);
			newInstances.add(new Instance(instance.id, newValues,
					newAttributes, instance.label));
		}
		return newInstances;
	}
	
	protected int		id;
	protected Random	rand;
	
	protected ID3Tree[]	children;
	protected boolean	isLeaf;
	
	protected int		splitAttribute	= Integer.MIN_VALUE;
	
	protected int		label			= Integer.MIN_VALUE;
	
	protected double	infogain		= Double.NEGATIVE_INFINITY;
	
	protected double	branchName		= Double.NEGATIVE_INFINITY;
	
	/**
	 * @param instances
	 * @param rand
	 * @param nodeName
	 */
	public ID3Tree(final ArrayList<Instance> instances, final Random rand,
			final double nodeName) {
		this(instances, rand);
		branchName = nodeName;
	}
	
	/**
	 * @param instances
	 */
	public ID3Tree(final List<Instance> instances) {
		rand = new Random(011235);
		id = rand.nextInt();
		buildTree(instances);
	}
	
	/**
	 * @param instances
	 * @param rand
	 */
	public ID3Tree(final List<Instance> instances, final Random rand) {
		this.rand = rand;
		id = rand.nextInt();
		buildTree(instances);
	}
	
	private void buildTree(final List<Instance> instances) {
		
		// count labels
		final int[] labelCount = new int[3];
		for (final Instance instance : instances)
			labelCount[instance.label != Integer.MIN_VALUE ? instance.label + 1
					: 0]++;
		
		// if all instance have label x, current tree is leaf with label x
		if (labelCount[1] == instances.size()) {
			label = 0;
			isLeaf = true;
		} else if (labelCount[2] == instances.size()) {
			label = 1;
			isLeaf = true;
		}
		
		else {
			
			// compute splits and information gain for every attribute
			final double entropy = computeEntropy(instances);
			final HashMap<Double, ArrayList<Instance>>[] allSplits = computeSplits(instances);
			for (int attribute = 0; attribute < allSplits.length; attribute++) {
				final double gain = computeGain(entropy, instances.size(),
						allSplits[attribute]);
				// System.out.println(attribute + "\t" + new
				// DecimalFormat("#.#####").format(gain));
				if (infogain < gain) {
					infogain = gain;
					splitAttribute = attribute;
				}
			}

			final int realSplitAttribute = instances.get(0).attributes[splitAttribute];
			
			// if the remaining dataset is smaller than 2, no further distinction can be made and
			// the current tree is leaf with the majority label of the remaining dataset
			if (allSplits[splitAttribute].keySet().size() < 2) {
				isLeaf = true;
				label = labelCount[2] > labelCount[1] ? 1 : 0;
			} else {
				
				// build subtree for every split dataset without the current split attribute
				children = new ID3Tree[allSplits[splitAttribute].keySet().size()];
				
				int i = 0;
				for (final double v : allSplits[splitAttribute].keySet())
					children[i++] = new ID3Tree(removeAttribute(
							allSplits[splitAttribute].get(v), splitAttribute),
							rand, v);
				
				splitAttribute = realSplitAttribute;
			}
		}
	}
	
	/**
	 * @param instance
	 * @return the label
	 */
	public int classify(final Instance instance) {
		if (label > Integer.MIN_VALUE)
			return label;
		if (isLeaf) return label;
		for (final ID3Tree child : children)
			if (instance.values[splitAttribute] == child.branchName)
				return child.classify(instance);
		// return label;
		// new value for current attribute, choose random child
		return children[new Random().nextInt(children.length)].classify(instance);
	}
	
	/**
	 * @return returns the tree as dot-style representation
	 */
	public String getDotString() {
		return getDotString(true);
	}
	
	private String getDotString(final boolean isRoot) {
		final StringBuilder stringBuilder = new StringBuilder();
		if (isRoot)
			stringBuilder.append("digraph decision_tree {\n");
		if (!isLeaf)
			for (final ID3Tree child : children)
				if (!child.isLeaf) {
					stringBuilder.append(id + " [label= " + splitAttribute
							+ "];\n");
					stringBuilder.append(child.id + " [label= "
							+ child.splitAttribute + "];\n");
					stringBuilder.append(id + " -> " + child.id + " [label="
							+ child.branchName + "];\n");
					stringBuilder.append(child.getDotString(false));
				} else {
					stringBuilder.append(id + " [label= " + splitAttribute
							+ "];\n");
					stringBuilder.append(child.id + " [label=\"LABEL("
							+ child.label + ")\"];\n");
					stringBuilder.append(id + " -> " + child.id + " [label="
							+ child.branchName + "];\n");
				}
		if (isRoot)
			stringBuilder.append("}");
		return stringBuilder.toString();
	}
}