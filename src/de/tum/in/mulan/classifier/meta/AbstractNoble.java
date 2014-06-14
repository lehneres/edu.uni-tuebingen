package de.tum.in.mulan.classifier.meta;

import java.io.FileReader;
import java.io.Serializable;
import java.util.Date;

import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.Utils;
import de.tum.in.mulan.classifier.transformation.Noble;
import de.tum.in.mulan.classifier.transformation.evaluation.NobleEvaluator;

/**
 * @author LehnereS abstract class for Noble and RelationJoin
 */
@SuppressWarnings ("deprecation")
public abstract class AbstractNoble implements Serializable {
	
	private static boolean	debug	= true;
	
	/**
	 * Writes the debug message string to the console output if debug for the learner is enabled.
	 * 
	 * @param msg
	 *            the debug message
	 */
	public static void debug(final String msg) {
		if (!debug) return;
		System.err.println(Thread.currentThread().getName() + "@" + new Date() + ": " + msg);
	}
	
	/**
	 * reads command line options and start a crossvalidation
	 * 
	 * @param args
	 *            command line arguments
	 */
	@SuppressWarnings ("resource")
	public static void main(final String[] args) {
		AbstractNoble.debug("reading options and data...");
		final long start = System.currentTimeMillis();
		try {
			final String[] initalOptions = args.clone();
			final String id = Utils.getOption("id", args);
			final String leftArff = Utils.getOption("left", args);
			final String centerArff = Utils.getOption("center", args);
			final String topArff = Utils.getOption("top", args);
			final String k = Utils.getOption("k", args);
			final String workingDir = Utils.getOption("dir", args);
			final String t = Utils.getOption('t', args);
			final String threshold = Utils.getOption("threshold", args);
			final Classifier baseClassifier = AbstractClassifier.forName(Utils.getOption('S', args), null);
			final String classifierName = Utils.getOption('A', args);
			final String foldsStr = Utils.getOption("folds", args);
			final String DBPdir = Utils.getOption("dbp", args);
			final boolean keep = Utils.getFlag("keep", args);
			// we always use bipartition, not an option
			final boolean useBipartion = true;
			final boolean transp = Utils.getFlag("transpose", args);
			int folds;
			if (foldsStr.equals("n")) folds = Integer.MAX_VALUE;
			else folds = Integer.valueOf(foldsStr);
			AbstractNoble.debug("doing " + foldsStr + " fold-cross-evaluation...");
			final NobleEvaluator eval = new NobleEvaluator(initalOptions, useBipartion);
			final Instances left = new Instances(new FileReader(leftArff));
			final Instances top = new Instances(new FileReader(topArff));
			final Instances center = new Instances(new FileReader(centerArff));
			AbstractNoble.debug("done");
			AbstractNoble.debug("starting evaluation..." + transp);
			AbstractNoble classifier;
			if (classifierName.equals("Rel")) classifier = new RelationJoin(baseClassifier);
			else {
				classifier = new Noble(baseClassifier);
				((Noble) classifier).setId(Integer.valueOf(id));
				if (!threshold.equals("")) ((Noble) classifier).setThreshold(Double.parseDouble(threshold));
				if (Utils.getFlag("opt", args)) {
					eval.setOpt(true);
					
					final double[] tar = new double[9];
					
					final int[] kar = new int[Math.min(63, center.numAttributes()) - 1];
					
					final int[] kartransp = new int[Math.min(63, center.numInstances()) - 1];
					
					for (int i = 0; i < 9; i++)
						tar[i] = (i + 1.0) / 10.0;
					
					for (int i = 0; i < kar.length; i++)
						kar[i] = i + 2;
					
					for (int i = 0; i < kartransp.length; i++)
						kartransp[i] = i + 2;
					
					eval.setKArray(kar);
					eval.setKArraytrans(kartransp);
					
					eval.setTArray(tar);
					
				} else {
					((Noble) classifier).setK(Integer.valueOf(k));
					((Noble) classifier).setT(Double.valueOf(t));
					((Noble) classifier).setTranspose(transp);
				}
				((Noble) classifier).setWorkingDir(workingDir);
				((Noble) classifier).setdBPdir(DBPdir);
				((Noble) classifier).setKeep(keep);
			}
			
			eval.nobleCV(classifier, left, top, center, folds);
			eval.write(Utils.getOption("output", args), workingDir, start);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static Instances transpose(final Instances inst) {
		final FastVector<Attribute> atts = new FastVector<>();
		
		for (int i = 0; i < inst.numInstances(); i++) {
			final FastVector<String> attVals = new FastVector<>();
			
			attVals.addElement("0");
			attVals.addElement("1");
			
			atts.addElement(new Attribute("att" + i, attVals));
		}
		
		final Instances transp = new Instances("transposed", atts, 0);
		for (int i = 0; i < inst.numAttributes(); i++) {
			final double[] vals = inst.attributeToDoubleArray(i);
			transp.add(new DenseInstance(1.0, vals));
		}
		
		return transp;
	}
	
	private boolean			transpose;
	private int				id;
	
	private final boolean	replaceMissing	= true;
	
	/**
	 * builds the classifier by given left, top and center matrix
	 * 
	 * @param in_left
	 *            the left matrix
	 * @param in_top
	 *            the top matrix
	 * @param in_center
	 *            the center matrix
	 * @throws Exception
	 * @throws InvalidDataException
	 */
	public abstract void build(final Instances in_left, final Instances in_top, final Instances in_center)
			throws InvalidDataException, Exception;
	
	/**
	 * method returning boolean-class for two given instances
	 * 
	 * @param in_left
	 *            the instance from the main table
	 * @param in_top
	 *            the instance from the second table
	 * @return true if instances "fit"
	 * @throws InvalidDataException
	 *             if the data is corrupt
	 * @throws ModelInitializationException
	 *             if the model could not be initialized
	 * @throws Exception
	 *             any exception
	 */
	public abstract double classifyInstances(final Instance in_left, final Instance in_top)
			throws InvalidDataException, ModelInitializationException, Exception;
	
	/**
	 * returning a double probability for two given instances
	 * 
	 * @param in_left
	 *            the instance from the left matrix
	 * @param in_top
	 *            the instance from the top matrix
	 * @return probability
	 * @throws Exception
	 *             any exception
	 */
	public abstract double distributionForInstances(final Instance in_left, final Instance in_top) throws Exception;
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @return if missing values are replaced
	 */
	public boolean isReplaceMissing() {
		return replaceMissing;
	}
	
	/**
	 * @return true if left and top are interchanged
	 */
	public boolean isTranspose() {
		return transpose;
	}
	
	/**
	 * @return a (deep) copy of the classifier
	 * @throws Exception
	 *             any exception
	 */
	public AbstractNoble makeCopy() throws Exception {
		return (AbstractNoble) new SerializedObject(this).getObject();
	}
	
	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final int id) {
		this.id = id;
	}
	
	/**
	 * @param transp
	 *            if to transpose the input matrizes
	 */
	public void setTranspose(final boolean transp) {
		transpose = transp;
	}
	
}
