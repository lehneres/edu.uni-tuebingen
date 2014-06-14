package de.tum.in.mulan.classifier.transformation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import mulan.classifier.MultiLabelOutput;
import mulan.classifier.transformation.ClassifierChain;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.MultiLabelInstances;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.search.SearchAlgorithm;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import de.tum.in.mulan.classifier.transformation.evaluation.ExtendedMultipleEvaluation;
import de.tum.in.mulan.classifier.transformation.evaluation.SortedCCEvaluator;

/**
 * classifier chains with sorted label order based on bayes nets.
 * 
 * @author LehnereS
 */
public class SortedCC extends TransformationBasedMultiLabelLearner {
	
	/**
	 * Comparator to sort a bayes net
	 * 
	 * @author LehnereS
	 */
	public class BayesNetComparator implements Comparator<Integer> {
		
		@SuppressWarnings ("hiding")
		SortMethod	internalSortmethod;
		BayesNet	net;
		ParentSet[]	parents;
		
		/**
		 * @param bayesNet
		 *            weka bayes net
		 * @param method
		 *            the sorting method
		 */
		public BayesNetComparator(final BayesNet bayesNet, final SortMethod method) {
			net = bayesNet;
			parents = bayesNet.getParentSets();
			internalSortmethod = method;
		}
		
		@SuppressWarnings ("incomplete-switch")
		@Override
		public int compare(final Integer arg0, final Integer arg1) {
			switch (internalSortmethod) {
			case childUp:
				int childrenUp1 = 0,
				childrenUp2 = 0;
				for (final ParentSet parentSet : parents) {
					if (parentSet.contains(arg0)) childrenUp1++;
					if (parentSet.contains(arg1)) childrenUp2++;
				}
				if (childrenUp2 < childrenUp1) return 1;
				else if (childrenUp2 > childrenUp1) return -1;
				break;
			case childDown:
				int childrenDown1 = 0,
				childrenDown2 = 0;
				for (final ParentSet parentSet : parents) {
					if (parentSet.contains(arg0)) childrenDown1++;
					if (parentSet.contains(arg1)) childrenDown2++;
				}
				if (childrenDown2 < childrenDown1) return -1;
				else if (childrenDown2 > childrenDown1) return 1;
				break;
			case parentUp:
				if (net.getNrOfParents(arg0) < net.getNrOfParents(arg1)) return -1;
				else if (net.getNrOfParents(arg0) > net.getNrOfParents(arg1)) return 1;
				break;
			case parentDown:
				if (net.getNrOfParents(arg0) < net.getNrOfParents(arg1)) return 1;
				else if (net.getNrOfParents(arg0) > net.getNrOfParents(arg1)) return -1;
				break;
			case random:
				return new Random().nextInt() < 5 ? -1 : 1;
			}
			return 0;
		}
	}
	
	/**
	 * different sort methods for the bayes net nodes
	 * 
	 * @author LehnereS
	 */
	public enum SortMethod {
		/** node with the most children is first */
		childDown,
		/**
		 * node with the most children is first and delete those being processed
		 */
		childDownD,
		/** node with the fewest children is first */
		childUp,
		/**
		 * node with the fewest children is first and delete those being processed
		 */
		childUpD,
		/** node with the most parents is first */
		parentDown,
		/** node with the most parents is first and delete those being processed */
		parentDownD,
		/** node with the fewest parents is first */
		parentUp,
		/**
		 * node with the fewest parents is first and delete those being processed
		 */
		parentUpD,
		/** random order */
		random
	}
	
	private static String featureToString(final int[] featureIndices, final MultiLabelInstances multiLabelInstances) {
		final StringBuilder builder = new StringBuilder();
		for (final int i : featureIndices)
			builder.append(" [" + multiLabelInstances.getDataSet().attribute(i).name() + "]");
		return builder.toString();
	}
	
	private static int[] getConsensusLabelOrder(final int[][] sortedNodes) {
		final int[] count = new int[sortedNodes.length];
		Arrays.fill(count, 0);
		for (int pos = 0; pos < sortedNodes.length; pos++)
			for (int model = 0; model < sortedNodes.length; model++)
				count[sortedNodes[model][pos]] += pos;
		final int[] order = new int[count.length];
		for (int k = 0; k < count.length; k++) {
			int min = Integer.MAX_VALUE;
			int minIndex = 0;
			for (int i = 0; i < count.length; i++)
				if (count[i] != -1 && count[i] < min) {
					min = count[i];
					minIndex = i;
				}
			order[k] = minIndex;
			count[minIndex] = -1;
		}
		return order;
	}
	
	/**
	 * reads command line options and starts a crossvalidation
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception
	 *             some exceptions
	 */
	public static void main(final String[] args) throws Exception {
		System.err.println("reading commandline options...");
		final String[] initalOptions = args.clone();
		if (args.length == 0)
			throw new Exception(
					"-t path/to/arff -xml path/to/xml -B 'options for BayesNet' -C 'options for baseclassifier'");
		final String arffFilename = Utils.getOption('t', args);
		final String xmlFilename = Utils.getOption("xml", args);
		final double numFolds = Double.parseDouble(Utils.getOption("folds", args));
		final MultiLabelInstances data = new MultiLabelInstances(arffFilename, xmlFilename);
		final String[] baseClassifierOptions = Utils.getOption('C', args).split(" ");
		final String classifierName = Utils.getOption('W', baseClassifierOptions);
		final Classifier baseClassifier = AbstractClassifier.forName(classifierName, baseClassifierOptions);
		final BayesNet bayesNet = new BayesNet();
		bayesNet.setOptions(Utils.getOption('B', args).split(" "));
		// bayesNet.setUseADTree(false);
		final SortedCC sortedCC = new SortedCC(baseClassifier, bayesNet);
		sortedCC.setSortMethod(SortedCC.parseSortMethod(args));
		sortedCC.setDontUseClassLabel(Utils.getFlag("once", args));
		sortedCC.setDebug(Utils.getFlag('D', args));
		sortedCC.setStacking(Utils.getFlag("sCV", args));
		if (sortedCC.getDoStacking()) {
			final String sCVFolds = Utils.getOption("sCVFolds", args);
			if (sCVFolds.length() > 0) sortedCC.setStackingFolds(Integer.parseInt(sCVFolds));
		}
		final SortedCCEvaluator eval = new SortedCCEvaluator("SortedCC", initalOptions, data.getNumLabels());
		sortedCC.debug("using dataset: " + arffFilename);
		sortedCC.debug("using classifier: " + classifierName);
		sortedCC.debug("using sortmethod: " + sortedCC.getSortMethod());
		sortedCC.debug("using stacking: " + sortedCC.getDoStacking());
		sortedCC.debug("using once: " + sortedCC.getDontUseClassLabel());
		sortedCC.debug("using search algorithm: " + sortedCC.getSearchAlgorithmName());
		sortedCC.debug("reading commandline options...done");
		sortedCC.debug("starting crossvalidation...");
		final ExtendedMultipleEvaluation results = eval.holdOut(sortedCC, data, numFolds);
		eval.write(results, Utils.getOption("output", args), sortedCC.ordering);
		sortedCC.debug("crossvalidation...done");
	}
	
	/**
	 * @param args
	 *            command line arguments
	 * @return the enumeration object representing the selected sort method
	 * @throws Exception
	 *             weka...
	 */
	public static SortMethod parseSortMethod(final String[] args) throws Exception {
		if (Utils.getFlag("childDown", args)) return SortMethod.childDown;
		else if (Utils.getFlag("childUp", args)) return SortMethod.childUp;
		else if (Utils.getFlag("parentUp", args)) return SortMethod.parentUp;
		else if (Utils.getFlag("parentDown", args)) return SortMethod.parentDown;
		else if (Utils.getFlag("childDownD", args)) return SortMethod.childDownD;
		else if (Utils.getFlag("childUpD", args)) return SortMethod.childUpD;
		else if (Utils.getFlag("parentUpD", args)) return SortMethod.parentUpD;
		else if (Utils.getFlag("parentDownD", args)) return SortMethod.parentDownD;
		else if (Utils.getFlag("random", args)) return SortMethod.random;
		return SortMethod.childUpD;
	}
	
	private static Instances removeAttributes(final Instances instances, final int[] featureIndices) throws Exception {
		final Remove remove = new Remove();
		remove.setAttributeIndicesArray(featureIndices);
		remove.setInputFormat(instances);
		final Instances result = Filter.useFilter(instances, remove);
		return result;
	}
	
	private static int[] revertToOriginalIndex(final int[] consensusOrder, final Instances labelInstances,
			final Instances instances, final int[] labelIndices) {
		final int[] originalOrder = new int[labelIndices.length];
		for (int i = 0; i < consensusOrder.length; i++)
			for (int j = instances.numAttributes() - 1; j >= 0; j--)
				if (labelInstances.attribute(consensusOrder[i]).name().equals(instances.attribute(j).name())
						&& Arrays.binarySearch(labelIndices, j) > -1) {
					originalOrder[i] = j;
					break;
				}
		return originalOrder;
	}
	
	private final Classifier	backupClassifier;
	private final BayesNet		bayesNet;
	private boolean				dontUseClassLabel;
	private boolean				doStacking;
	
	private ClassifierChain		internalCC;
	
	private SortMethod			internalSortmethod;
	
	private int[]				ordering;
	
	private int					stackingFolds	= 10;
	
	/**
	 * constructor setting base classifier and gives bayes net
	 * 
	 * @param classifier
	 *            a weka classifier
	 * @param bayesNet
	 *            a weka bayes net
	 */
	public SortedCC(final Classifier classifier, final BayesNet bayesNet) {
		super(classifier);
		this.bayesNet = bayesNet;
		backupClassifier = classifier;
	}
	
	@Override
	protected void buildInternal(final MultiLabelInstances train) throws Exception {
		debug("setting label order from bayes net...");
		final int[] newOrder = setLabelOrderFromBayesNet(train);
		debug("setting label order from bayes net...done");
		internalCC = new mulan.classifier.transformation.ClassifierChain(baseClassifier, newOrder);
		internalCC.build(train);
		// if (this.chain == null) {
		// this.chain = new int[this.numLabels];
		// for (int i = 0; i < this.numLabels; i++) {
		// this.chain[i] = Arrays.binarySearch(train.getLabelIndices(),
		// this.labelIndices[i]);
		// }
		// }
		// this.numLabels = train.getNumLabels();
		// this.ensemble = new FilteredClassifier[this.numLabels];
		// Instances trainDataset = train.getDataSet();
		// for (int i = 0; i < this.numLabels; i++) {
		// this.debug("Bulding model " + (i + 1) + "/" + this.numLabels
		// + " for label "
		// + train.getDataSet().attribute(this.labelIndices[i]).name());
		// this.ensemble[i] = new FilteredClassifier();
		// this.ensemble[i].setClassifier(AbstractClassifier
		// .makeCopy(this.baseClassifier));
		// // Indices of attributes to remove first removes numLabels
		// // attributes
		// // the numLabels - 1 attributes and so on.
		// // The loop starts from the last attribute.
		// final int[] indicesToRemove = new int[this.numLabels - 1 - i];
		// for (int counter = 0; counter < (this.numLabels - i - 1); counter++)
		// {
		// indicesToRemove[counter] = this.labelIndices[this.numLabels - 1
		// - counter];
		// }
		// this.debug("removing "
		// + this.featureToString(indicesToRemove, train));
		// final Remove remove = new Remove();
		// remove.setAttributeIndicesArray(indicesToRemove);
		// remove.setInputFormat(trainDataset);
		// remove.setInvertSelection(false);
		// this.ensemble[i].setFilter(remove);
		// trainDataset.setClassIndex(this.labelIndices[i]);
		// this.ensemble[i].buildClassifier(trainDataset);
		// if (this.doStacking) {
		// trainDataset = this.alterDataSetWithStackingCV(trainDataset, i,
		// new Random(this.randomSeed));
		// }
		// }
	}
	
	private int[] getChildrenCount(final boolean[] done) {
		final int[] children = new int[done.length];
		for (int node = 0; node < bayesNet.getNrOfNodes(); node++)
			if (!done[node])
				for (int parent = 0; parent < bayesNet.getNrOfParents(node); parent++)
					if (!done[bayesNet.getParentSet(node).getParent(parent)])
						children[bayesNet.getParentSet(node).getParent(parent)]++;
		for (int i = 0; i < done.length; i++)
			if (done[i]) children[i] = -1;
		return children;
	}
	
	/**
	 * @return doneUseClassLabel
	 */
	public boolean getDontUseClassLabel() {
		return dontUseClassLabel;
	}
	
	/**
	 * @return doStacking
	 */
	public boolean getDoStacking() {
		return doStacking;
	}
	
	private int[] getNodeOrderFromNet() {
		final Integer[] order = new Integer[bayesNet.getNrOfNodes()];
		int[] result = new int[order.length];
		for (int i = 0; i < order.length; i++)
			order[i] = i;
		debug("sorting using " + internalSortmethod + " sorting method...");
		result = sortLabels(order);
		debug("sorting using " + internalSortmethod + " sorting method...done");
		return result;
	}
	
	private int[] getParentCount(final boolean[] done) {
		final int[] parents = new int[done.length];
		for (int node = 0; node < bayesNet.getNrOfNodes(); node++) {
			int parentCount = bayesNet.getNrOfParents(node);
			for (int parent = 0; parent < bayesNet.getNrOfParents(node); parent++)
				if (done[bayesNet.getParentSet(node).getParent(parent)]) parentCount--;
			parents[node] = parentCount;
		}
		for (int i = 0; i < done.length; i++)
			if (done[i]) parents[i] = -1;
		return parents;
	}
	
	/**
	 * @return {@link SearchAlgorithm}
	 */
	public SearchAlgorithm getSearchAlgorithm() {
		return bayesNet.getSearchAlgorithm();
	}
	
	/**
	 * @return name of {@link SearchAlgorithm}
	 */
	public String getSearchAlgorithmName() {
		return bayesNet.getSearchAlgorithm().getClass().getName();
	}
	
	/**
	 * @return {@link SortMethod}
	 */
	public SortMethod getSortMethod() {
		return internalSortmethod;
	}
	
	@Override
	protected MultiLabelOutput makePredictionInternal(final Instance instance) throws Exception {
		return internalCC.makePrediction(instance);
		
		// final boolean[] bipartition = new boolean[this.numLabels];
		// final double[] confidences = new double[this.numLabels];
		// this.debug("predicting for instance " + instance);
		// final Instance tempInstance = DataUtils.createInstance(instance,
		// instance.weight(), instance.toDoubleArray());
		// for (int counter = 0; counter < this.numLabels; counter++) {
		// double distribution[] = new double[2];
		// try {
		// distribution = this.ensemble[counter]
		// .distributionForInstance(tempInstance);
		// } catch (final Exception e) {
		// System.out.println(e);
		// return null;
		// }
		// final int maxIndex = distribution[0] > distribution[1] ? 0 : 1;
		// System.out.println("predicting : " + this.chain[counter]);
		// // Ensure correct predictions both for class values {0,1} and {1,0}
		// final Attribute classAttribute = this.ensemble[counter].getFilter()
		// .getOutputFormat().classAttribute();
		// this.debug("predicting for label: "
		// + instance.dataset()
		// .attribute(this.labelIndices[this.chain[counter]])
		// .name());
		// bipartition[this.chain[counter]] = classAttribute.value(maxIndex)
		// .equals("1") ? true : false;
		//
		// // The confidence of the label being equal to 1
		// confidences[this.chain[counter]] = distribution[classAttribute
		// .indexOfValue("1")];
		// tempInstance.setValue(this.labelIndices[this.chain[counter]],
		// maxIndex);
		//
		// }
		//
		// final MultiLabelOutput mlo = new MultiLabelOutput(bipartition,
		// confidences);
		// // if (this.getDebug()) {
		// // System.err.print("-: ");
		// // for (final double d : mlo.getConfidences())
		// // System.err.print(" | " + new DecimalFormat("0.000").format(d) +
		// // " | ");
		// // System.err.println(" :-");
		// // }
		// return mlo;
	}
	
	private String printChildCount(final int[] sortedNodesByLabel) {
		String msg = " [ ";
		for (final int i : sortedNodesByLabel) {
			int childs = 0;
			for (final ParentSet parentSet : bayesNet.getParentSets())
				if (parentSet.contains(i)) childs++;
			msg += i + "(=" + bayesNet.getNodeName(i) + "):" + childs + " | ";
		}
		return msg + " ]";
	}
	
	private String printParentCount(final int[] sortedNodesByLabel) {
		String msg = " [ ";
		for (final int i : sortedNodesByLabel)
			msg += i + "(=" + bayesNet.getNodeName(i) + "):" + bayesNet.getNrOfParents(i) + " | ";
		return msg + " ]";
	}
	
	/**
	 * @param flag
	 *            dont use class label (once)
	 */
	public void setDontUseClassLabel(final boolean flag) {
		dontUseClassLabel = flag;
	}
	
	int[] setLabelOrderFromBayesNet(final MultiLabelInstances multiLabelInstances) throws IllegalArgumentException,
			Exception {
		debug("deleting non label attributes...");
		Instances labelInstances = multiLabelInstances.getDataSet();
		labelInstances =
				SortedCC.removeAttributes(new Instances(labelInstances), multiLabelInstances.getFeatureIndices());
		debug("deleting non label attributes...done");
		debug("starting determining label order from bayes net...");
		final int[][] sortedNodesByLabel = new int[labelInstances.numAttributes()][];
		int[] consensusOrder;
		if (dontUseClassLabel) {
			debug("using direct net structure");
			debug("building bayes net...");
			if (getSortMethod() == SortMethod.random) {
				final Integer[] rndlabelIndices = new Integer[multiLabelInstances.getNumLabels()];
				for (int i = 0; i < rndlabelIndices.length; i++)
					rndlabelIndices[i] = i;
				Arrays.sort(rndlabelIndices, new BayesNetComparator(bayesNet, SortMethod.random));
				consensusOrder = de.tum.in.multilabel.Utils.convertIntegerArray(rndlabelIndices);
			} else {
				bayesNet.m_Instances = labelInstances;
				bayesNet.initStructure();
				bayesNet.buildStructure();
				bayesNet.estimateCPTs();
				debug("building bayes net...done");
				// final BufferedWriter writer =
				// new BufferedWriter(new FileWriter(new File("tmpnet.xml")));
				// writer.write(this.bayesNet.toXMLBIF03());
				// writer.flush();
				// writer.close();
				consensusOrder = getNodeOrderFromNet();
			}
		} else {
			debug("using consensus label order");
			for (int i = 0; i < labelInstances.numAttributes(); i++) {
				debug("building bayes net for label [" + labelInstances.attribute(i).name() + "]...");
				if (getSortMethod() == SortMethod.random) {
					final Integer[] rndlabelIndices = new Integer[multiLabelInstances.getNumLabels()];
					for (int k = 0; k < rndlabelIndices.length; k++)
						rndlabelIndices[k] = k;
					Arrays.sort(rndlabelIndices, new BayesNetComparator(bayesNet, SortMethod.random));
					sortedNodesByLabel[i] = de.tum.in.multilabel.Utils.convertIntegerArray(rndlabelIndices);
				} else {
					labelInstances.setClassIndex(i);
					bayesNet.buildClassifier(labelInstances);
					debug("building bayes net for label [" + labelInstances.attribute(i).name() + "]...done");
					sortedNodesByLabel[i] = getNodeOrderFromNet();
				}
			}
			consensusOrder = SortedCC.getConsensusLabelOrder(sortedNodesByLabel);
		}
		ordering =
				SortedCC.revertToOriginalIndex(consensusOrder, labelInstances, multiLabelInstances.getDataSet(),
						multiLabelInstances.getLabelIndices());
		debug("starting evaluating label order from bayes net...done");
		debug("found order: " + SortedCC.featureToString(consensusOrder, multiLabelInstances));
		return consensusOrder;
	}
	
	/**
	 * @param method
	 *            {@link SortMethod}
	 */
	public void setSortMethod(final SortMethod method) {
		internalSortmethod = method;
	}
	
	/**
	 * @param flag
	 *            set stacking
	 */
	public void setStacking(final boolean flag) {
		doStacking = flag;
	}
	
	/**
	 * @param folds
	 *            set stacking folds
	 */
	public void setStackingFolds(final int folds) {
		stackingFolds = folds;
	}
	
	private int[] sortLabels(final Integer[] order) {
		final boolean[] done = new boolean[order.length];
		final int[] sorted = new int[order.length];
		switch (internalSortmethod) {
		case childUpD:
			for (int position = 0; position < order.length; position++) {
				int curMin = Integer.MAX_VALUE;
				int curMinPos = -1;
				final int[] count = getChildrenCount(done);
				for (int node = 0; node < count.length; node++)
					if (count[node] >= 0 && count[node] < curMin) {
						curMin = count[node];
						curMinPos = node;
					}
				sorted[position] = curMinPos;
				done[curMinPos] = true;
			}
			break;
		case childDownD:
			for (int position = 0; position < order.length; position++) {
				int curMax = Integer.MIN_VALUE;
				int curMaxPos = -1;
				final int[] count = getChildrenCount(done);
				for (int node = 0; node < count.length; node++)
					if (count[node] >= 0 && count[node] > curMax) {
						curMax = count[node];
						curMaxPos = node;
					}
				sorted[position] = curMaxPos;
				done[curMaxPos] = true;
			}
			break;
		case parentUpD:
			for (int position = 0; position < order.length; position++) {
				int curMin = Integer.MAX_VALUE;
				int curMinPos = -1;
				final int[] count = getParentCount(done);
				for (int node = 0; node < order.length; node++)
					if (count[node] >= 0 && count[node] < curMin) {
						curMin = count[node];
						curMinPos = node;
					}
				sorted[position] = curMinPos;
				done[curMinPos] = true;
			}
			break;
		case parentDownD:
			for (int position = 0; position < order.length; position++) {
				int curMax = Integer.MIN_VALUE;
				int curMaxPos = -1;
				final int[] count = getParentCount(done);
				for (int node = 0; node < count.length; node++)
					if (count[node] >= 0 && count[node] > curMax) {
						curMax = count[node];
						curMaxPos = node;
					}
				sorted[position] = curMaxPos;
				done[curMaxPos] = true;
			}
			break;
		default:
			Arrays.sort(order, new BayesNetComparator(bayesNet, internalSortmethod));
			for (final Integer i : order)
				sorted[i] = order[i];
		}
		debug("child counts: " + printChildCount(sorted));
		debug("parent counts: " + printParentCount(sorted));
		return sorted;
	}
}