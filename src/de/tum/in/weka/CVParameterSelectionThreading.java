package de.tum.in.weka;

/*
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program; if not, write to
 * the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * CVParameterSelection.java Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 */

import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Summarizable;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * origin see javadoc below. This is a adaption for using CVParamenterSelection with Threading using as much threads as
 * possible so for every option tupel and every step in cross-validation
 * 
 * @author LehnereS (lehnerer@in.tum.de)
 */

/**
 * <!-- globalinfo-start --> Class for performing parameter selection by cross-validation for any classifier.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * R. Kohavi (1995). Wrappers for Performance Enhancement and Oblivious Decision Graphs. Department of Computer Science,
 * Stanford University.
 * <p/>
 * <!-- globalinfo-end --> <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;phdthesis{Kohavi1995,
 *    address = {Department of Computer Science, Stanford University},
 *    author = {R. Kohavi},
 *    school = {Stanford University},
 *    title = {Wrappers for Performance Enhancement and Oblivious Decision Graphs},
 *    year = {1995}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end --> <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -X &lt;number of folds&gt;
 *  Number of folds used for cross validation (default 10).
 * </pre>
 * 
 * <pre>
 * -P &lt;classifier parameter&gt;
 *  Classifier parameter options.
 *  eg: "N 1 5 10" Sets an optimisation parameter for the
 *  classifier with name -N, with lower bound 1, upper bound
 *  5, and 10 optimisation steps. The upper bound may be the
 *  character 'A' or 'I' to substitute the number of
 *  attributes or instances in the training data,
 *  respectively. This parameter may be supplied more than
 *  once to optimise over several classifier options
 *  simultaneously.
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.rules.ZeroR)
 * </pre>
 * 
 * <pre>
 * Options specific to classifier weka.classifiers.rules.ZeroR:
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end --> Options after -- are passed to the designated sub-classifier.
 * <p>
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 5928 $
 */
@SuppressWarnings ("deprecation")
public class CVParameterSelectionThreading extends RandomizableSingleClassifierEnhancer implements Drawable,
		Summarizable, TechnicalInformationHandler {
	
	/**
	 * A data structure holding discrete values for a cross-validation search parameter
	 * 
	 * @author LehnereS
	 */
	protected class CVFixedParameter extends CVParameter {
		
		final List<Double>	m_ParamSteps	= new LinkedList<>();
		
		/**
		 * @param param
		 * @throws Exception
		 */
		public CVFixedParameter(final String param) throws Exception {
			super(new String(param));
			// Tokenize the string into it's parts
			final StreamTokenizer st = new StreamTokenizer(new StringReader(param));
			if (st.nextToken() != StreamTokenizer.TT_WORD)
				throw new Exception("CVParameter " + param + ": Character parameter identifier expected");
			m_ParamChar = st.sval.charAt(0);
			while (st.nextToken() == StreamTokenizer.TT_NUMBER)
				m_ParamSteps.add(st.nval);
			if (m_ParamSteps.size() == 0) throw new Exception("no step values for -PX parameter entered!");
		}
	}
	
	/**
	 * A data structure to hold values associated with a single cross-validation search parameter
	 */
	protected class CVParameter implements Serializable, RevisionHandler {
		
		/**
		 * True if the parameter should be added at the end of the argument list
		 */
		boolean			m_AddAtEnd;
		
		/** Lower bound for the CV search */
		final double	m_Lower;
		
		/** The parameter value with the best performance */
		double			m_ParamValue;
		
		/** True if the parameter should be rounded to an integer */
		boolean			m_RoundParam;
		
		/** Number of steps during the search */
		final double	m_Steps;
		
		/** Upper bound for the CV search */
		double			m_Upper;
		
		/** Char used to identify the option of interest */
		protected char	m_ParamChar;
		
		/**
		 * Constructs a CVParameter.
		 * 
		 * @param param
		 *            the parameter definition
		 * @throws Exception
		 *             if construction of CVParameter fails
		 */
		public CVParameter(final String param) throws Exception {
			
			// Tokenize the string into it's parts
			final StreamTokenizer st = new StreamTokenizer(new StringReader(param));
			if (st.nextToken() != StreamTokenizer.TT_WORD)
				throw new Exception("CVParameter " + param + ": Character parameter identifier expected");
			m_ParamChar = st.sval.charAt(0);
			if (st.nextToken() != StreamTokenizer.TT_NUMBER)
				throw new Exception("CVParameter " + param + ": Numeric lower bound expected");
			m_Lower = st.nval;
			if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
				m_Upper = st.nval;
				if (m_Upper < m_Lower)
					throw new Exception("CVParameter " + param + ": Upper bound is less than lower bound");
			} else if (st.ttype == StreamTokenizer.TT_WORD) {
				if (st.sval.toUpperCase().charAt(0) == 'A') m_Upper = m_Lower - 1;
				else if (st.sval.toUpperCase().charAt(0) == 'I') m_Upper = m_Lower - 2;
				else throw new Exception("CVParameter " + param + ": Upper bound must be numeric, or 'A' or 'N'");
			} else throw new Exception("CVParameter " + param + ": Upper bound must be numeric, or 'A' or 'N'");
			if (st.nextToken() != StreamTokenizer.TT_NUMBER)
				throw new Exception("CVParameter " + param + ": Numeric number of steps expected");
			m_Steps = st.nval;
			if (st.nextToken() == StreamTokenizer.TT_WORD)
				if (st.sval.toUpperCase().charAt(0) == 'R') m_RoundParam = true;
		}
		
		/**
		 * gets the current paramter value
		 * 
		 * @return the m_ParamValue
		 */
		public double getM_ParamValue() {
			return m_ParamValue;
		}
		
		/**
		 * Returns the revision string.
		 * 
		 * @return the revision
		 */
		@Override
		public String getRevision() {
			return RevisionUtils.extract("$Revision: 5928 $");
		}
		
		/**
		 * sets the current paramter value
		 * 
		 * @param m_ParamValue
		 *            the value to set
		 * @return paramValue
		 */
		public double setM_ParamValue(final double m_ParamValue) {
			this.m_ParamValue = m_ParamValue;
			return m_ParamValue;
		}
		
		/**
		 * Returns a CVParameter as a string.
		 * 
		 * @return the CVParameter as string
		 */
		@Override
		public String toString() {
			
			String result = m_ParamChar + " " + m_Lower + " ";
			switch ((int) (m_Lower - m_Upper + 0.5)) {
			case 1:
				result += "A";
				break;
			case 2:
				result += "I";
				break;
			default:
				result += m_Upper;
				break;
			}
			result += " " + m_Steps;
			if (m_RoundParam) result += " R";
			return result;
		}
	}
	
	/**
	 * A object containing the options tupel and a evaluation object for evaluating the thread result
	 * 
	 * @author LehnereS
	 */
	protected class EvaluationGroup {
		
		private final Evaluation	eval;
		private final String[]		options;
		
		/**
		 * @param options
		 * @param eval
		 */
		public EvaluationGroup(final String[] options, final Evaluation eval) {
			this.options = options;
			this.eval = eval;
		}
		
		/**
		 * evaluation all threads using a common options tupel
		 */
		@SuppressWarnings ("synthetic-access")
		void evaluateThreadGroup() {
			final double error = eval.errorRate();
			if (CVParameterSelectionThreading.this.m_Debug)
				System.out.println("Cross-validated error rate: " + Utils.doubleToString(error, 6, 4));
			if (m_BestPerformance == -99 || error < m_BestPerformance) {
				System.err.print("new best options for "
						+ CVParameterSelectionThreading.this.m_Classifier.getClass().getName() + ":");
				for (final String option : options)
					System.err.print(" " + option);
				System.err.println(" with error rate: " + new DecimalFormat("#.###").format(error));
				m_BestPerformance = error;
				m_BestClassifierOptions = options;
			}
		}
	}
	
	/**
	 * findParamsByCrossValidationRunnable class containing findParamsByCrossValidation methods is to be submitted to a
	 * threadpool
	 * 
	 * @implements Runnable
	 * @author LehnereS
	 */
	protected class findParamsByCrossValidationRunnable implements Runnable {
		
		private Classifier			currentClassifier;
		private final int			currentFold;
		private final Evaluation	evaluation;
		private Instances			trainData;
		
		/**
		 * @param j
		 * @param trainData
		 *            making deep copy of given trainData set
		 * @param eval
		 *            evaluation object to use
		 * @param currentClassifier
		 *            classifier with set options to use will make deep copy of given classifier
		 */
		public findParamsByCrossValidationRunnable(final int j, final Instances trainData, final Evaluation eval,
				final Classifier currentClassifier) {
			currentFold = j;
			this.trainData = trainData;
			evaluation = eval;
			this.currentClassifier = currentClassifier;
		}
		
		/**
		 * overides run() method in Runnable will be started wenn executing Thread
		 * 
		 * @see java.lang.Runnable
		 */
		@Override
		public void run() {
			final double start = System.currentTimeMillis();
			final SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss ");
			System.err.println(Thread.currentThread().getName() + " started at: " + formatter.format(new Date()));
			try {
				trainData = (Instances) new SerializedObject(trainData).getObject();
				currentClassifier = (Classifier) new SerializedObject(currentClassifier).getObject();
				final Instances train = trainData.trainCV(m_NumFolds, currentFold, new Random(1));
				final Instances test = trainData.testCV(m_NumFolds, currentFold);
				currentClassifier.buildClassifier(train);
				boolean isDone = false;
				do
					if (lock.tryLock()) try {
						evaluation.setPriors(train);
						evaluation.evaluateModel(currentClassifier, test);
					} finally {
						isDone = true;
						lock.unlock();
					}
					else Thread.yield();
				while (!isDone);
			} catch (final Exception e) {
				System.err.print("oops, something went wrong in threading world! ");
				e.printStackTrace();
				System.exit(10);
			}
			System.err.println(Thread.currentThread().getName() + " is finished at: " + formatter.format(new Date())
					+ "and has been running for: "
					+ new DecimalFormat("##.##").format((System.currentTimeMillis() - start) / 1000) + "s");
		}
	}
	
	private static ExecutorService	threadPool;
	
	/** for serialization */
	static final long				serialVersionUID	= -6529603380876641265L;
	
	/**
	 * Main method for testing this class.
	 * 
	 * @param args
	 *            the options
	 */
	public static void main(final String[] args) {
		System.err.println("CVParameterSelectionThreading working on " + Runtime.getRuntime().availableProcessors()
				+ " cpus");
		try {
			final boolean cached = Utils.getFlag("cached", args);
			boolean fixed;
			String threadCount;
			if (!cached) {
				fixed = Utils.getFlag("fixed", args);
				threadCount = Utils.getOption("threads", args);
				if (!fixed || threadCount.equals(""))
					throw new Exception(
							"parameters missing:\n-fixed for fixed threadpool size (+ -threads int for size of threadpool (is multiplied by number of cpus)\n-cached for cached threadpool");
				System.err.println("starting CVParameterSelection with fixed ThreadPool(" + threadCount + ")");
				AbstractClassifier.runClassifier(
						new CVParameterSelectionThreading(fixed, Integer.parseInt(threadCount)), args);
			} else AbstractClassifier.runClassifier(new CVParameterSelectionThreading(false, -1), args);
		} catch (final Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	/** true if used threadpool is type "fixed", false for type "cached" */
	private final boolean				fixed;
	
	/** multiplier for threadpool size (= CPU * threadCount) */
	private final int					threadCount;
	
	/** The set of all classifier options as determined by cross-validation */
	protected String[]					m_BestClassifierOptions;
	
	/** The cross-validated performance of the best options */
	protected double					m_BestPerformance;
	
	/**
	 * The base classifier options (not including those being set by cross-validation)
	 */
	protected String[]					m_ClassifierOptions;
	
	/** The set of parameters to cross-validate over */
	protected FastVector<CVParameter>	m_CVParams	= new FastVector<>();
	
	/**
	 * The set of all options at initialization time. So that getOptions can return this.
	 */
	protected String[]					m_InitOptions;
	
	/** The number of attributes in the data */
	protected int						m_NumAttributes;
	
	/** The number of folds used in cross-validation */
	protected int						m_NumFolds	= 10;
	
	/** The number of instances in a training fold */
	protected int						m_TrainFoldSize;
	
	/** for threading */
	final Lock							lock		= new ReentrantLock();
	
	/**
	 * @param fixed
	 *            true if used threadpool is type "fixed", false for type "cached"
	 * @param threadCount
	 *            multiplier for threadpool size (= CPU * threadCount)
	 */
	public CVParameterSelectionThreading(final boolean fixed, final int threadCount) {
		this.fixed = fixed;
		this.threadCount = threadCount;
	}
	
	/**
	 * Adds a scheme parameter to the list of parameters to be set by cross-validation
	 * 
	 * @param cvParam
	 *            the string representation of a scheme parameter. The format is: <br>
	 *            param_char step#1 step#2 step#3 step#4 ... <br>
	 * @throws Exception
	 *             if the parameter specifier is of the wrong format
	 */
	public void addCVFixedParameter(final String cvParam) throws Exception {
		final CVFixedParameter newCV = new CVFixedParameter(cvParam);
		
		m_CVParams.addElement(newCV);
	}
	
	/**
	 * Adds a scheme parameter to the list of parameters to be set by cross-validation
	 * 
	 * @param cvParam
	 *            the string representation of a scheme parameter. The format is: <br>
	 *            param_char lower_bound upper_bound number_of_steps <br>
	 *            eg to search a parameter -P from 1 to 10 by increments of 1: <br>
	 *            P 1 10 11 <br>
	 * @throws Exception
	 *             if the parameter specifier is of the wrong format
	 */
	public void addCVParameter(final String cvParam) throws Exception {
		
		final CVParameter newCV = new CVParameter(cvParam);
		
		m_CVParams.addElement(newCV);
	}
	
	/**
	 * Generates the classifier.
	 * 
	 * @param instances
	 *            set of instances serving as training data
	 * @throws Exception
	 *             if the classifier has not been generated successfully
	 */
	@Override
	public void buildClassifier(final Instances instances) throws Exception {
		
		// can classifier handle the data?
		getCapabilities().testWithFail(instances);
		
		// remove instances with missing class
		final Instances trainData = new Instances(instances);
		trainData.deleteWithMissingClass();
		
		if (!(m_Classifier instanceof OptionHandler))
			throw new IllegalArgumentException("Base classifier should be OptionHandler.");
		m_InitOptions = ((OptionHandler) m_Classifier).getOptions();
		m_BestPerformance = -99;
		m_NumAttributes = trainData.numAttributes();
		final Random random = new Random(m_Seed);
		trainData.randomize(random);
		m_TrainFoldSize = trainData.trainCV(m_NumFolds, 0).numInstances();
		
		// Check whether there are any parameters to optimize
		if (m_CVParams.size() == 0) {
			m_Classifier.buildClassifier(trainData);
			m_BestClassifierOptions = m_InitOptions;
			return;
		}
		
		if (trainData.classAttribute().isNominal()) trainData.stratify(m_NumFolds);
		m_BestClassifierOptions = null;
		
		// Set up m_ClassifierOptions -- take getOptions() and remove
		// those being optimised.
		m_ClassifierOptions = ((OptionHandler) m_Classifier).getOptions();
		for (int i = 0; i < m_CVParams.size(); i++)
			Utils.getOption(m_CVParams.elementAt(i).m_ParamChar, m_ClassifierOptions);
		findParamsByCrossValidation(0, trainData, random);
		
		final String[] options = m_BestClassifierOptions.clone();
		((OptionHandler) m_Classifier).setOptions(options);
		m_Classifier.buildClassifier(trainData);
	}
	
	/**
	 * Create the options array to pass to the classifier. The parameter values and positions are taken from
	 * m_ClassifierOptions and m_CVParams.
	 * 
	 * @return the options array
	 */
	protected String[] createOptions() {
		
		final String[] options = new String[m_ClassifierOptions.length + 2 * m_CVParams.size()];
		int start = 0, end = options.length;
		
		// Add the cross-validation parameters and their values
		for (int i = 0; i < m_CVParams.size(); i++) {
			final CVParameter cvParam = m_CVParams.elementAt(i);
			double paramValue = cvParam.m_ParamValue;
			if (cvParam.m_RoundParam) paramValue = Math.rint(paramValue);
			if (cvParam.m_AddAtEnd) {
				options[--end] = "" + Utils.doubleToString(paramValue, 4);
				options[--end] = "-" + cvParam.m_ParamChar;
			} else {
				options[start++] = "-" + cvParam.m_ParamChar;
				options[start++] = "" + Utils.doubleToString(paramValue, 4);
			}
		}
		// Add the static parameters
		System.arraycopy(m_ClassifierOptions, 0, options, start, m_ClassifierOptions.length);
		
		return options;
	}
	
	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the explorer/experimenter gui
	 */
	public String CVParametersTipText() {
		return "Sets the scheme parameters which are to be set " + "by cross-validation.\n"
				+ "The format for each string should be:\n" + "param_char lower_bound upper_bound number_of_steps\n"
				+ "eg to search a parameter -P from 1 to 10 by increments of 1:\n" + "    \"P 1 10 10\" ";
	}
	
	/**
	 * Predicts the class distribution for the given test instance.
	 * 
	 * @param instance
	 *            the instance to be classified
	 * @return the predicted class value
	 * @throws Exception
	 *             if an error occurred during the prediction
	 */
	@Override
	public double[] distributionForInstance(final Instance instance) throws Exception {
		
		return m_Classifier.distributionForInstance(instance);
	}
	
	/**
	 * Finds the best parameter combination. (recursive for each parameter being optimised).
	 * 
	 * @param depth
	 *            the index of the parameter to be optimised at this level
	 * @param trainData
	 *            the data the search is based on
	 * @param random
	 *            a random number generator
	 * @throws Exception
	 *             if an error occurs
	 */
	protected void findParamsByCrossValidation(final int depth, final Instances trainData, final Random random)
			throws Exception {
		// getting all option sets
		final LinkedList<String[]> optionSet = getOptionsSet(depth, new LinkedList<String[]>());
		
		// creating fixed or cached threadpool
		CVParameterSelectionThreading.threadPool =
				fixed == true ? Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadCount)
						: Executors.newCachedThreadPool();
		
		// list containing a list for every parameter setting, containing a
		// future object for every
		// thread
		final LinkedList<EvaluationGroup> completeParameterGroupList = new LinkedList<>();
		
		while (!optionSet.isEmpty()) {
			// Set the classifier options
			final String[] options = optionSet.poll();
			// setting the options in classifier
			((OptionHandler) m_Classifier).setOptions(options.clone());
			final Evaluation eval = new Evaluation(trainData);
			// making copy of traindata
			for (int j = 0; j < m_NumFolds; j++)
				// executing Thread in Threadpool
				CVParameterSelectionThreading.threadPool.execute(new findParamsByCrossValidationRunnable(j, trainData,
						eval, m_Classifier));
			completeParameterGroupList.add(new EvaluationGroup(options, eval));
		}
		CVParameterSelectionThreading.threadPool.shutdown();
		// waiting for all threads to be terminated
		while (!CVParameterSelectionThreading.threadPool.isTerminated())
			TimeUnit.MILLISECONDS.sleep(10);
		for (final EvaluationGroup group : completeParameterGroupList)
			group.evaluateThreadGroup();
		System.err.println("**********************************************************************");
		System.err.print("searching parameter by cv finished: best parameters:");
		for (final String mBestClassifierOption : m_BestClassifierOptions)
			System.err.print(" " + mBestClassifierOption);
		System.err.println("\n**********************************************************************");
	}
	
	/**
	 * Returns (a copy of) the best options found for the classifier.
	 * 
	 * @return the best options
	 */
	public String[] getBestClassifierOptions() {
		return m_BestClassifierOptions.clone();
	}
	
	/**
	 * Returns default capabilities of the classifier.
	 * 
	 * @return the capabilities of this classifier
	 */
	@Override
	public Capabilities getCapabilities() {
		final Capabilities result = super.getCapabilities();
		
		result.setMinimumNumberInstances(m_NumFolds);
		
		return result;
	}
	
	/**
	 * Gets the scheme paramter with the given index.
	 * 
	 * @param index
	 *            the index for the parameter
	 * @return the scheme parameter
	 */
	public String getCVParameter(final int index) {
		
		if (m_CVParams.size() <= index) return "";
		return m_CVParams.elementAt(index).toString();
	}
	
	/**
	 * Get method for CVParameters.
	 * 
	 * @return the CVParameters
	 */
	public Object[] getCVParameters() {
		
		final Object[] CVParams = m_CVParams.toArray();
		
		final String params[] = new String[CVParams.length];
		
		for (int i = 0; i < CVParams.length; i++)
			params[i] = CVParams[i].toString();
		
		return params;
		
	}
	
	/**
	 * Gets the number of folds for the cross-validation.
	 * 
	 * @return the number of folds for the cross-validation
	 */
	public int getNumFolds() {
		
		return m_NumFolds;
	}
	
	/**
	 * Gets the current settings of the Classifier.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {
		
		String[] superOptions;
		
		if (m_InitOptions != null) try {
			((OptionHandler) m_Classifier).setOptions(m_InitOptions.clone());
			superOptions = super.getOptions();
			((OptionHandler) m_Classifier).setOptions(m_BestClassifierOptions.clone());
		} catch (final Exception e) {
			throw new RuntimeException("CVParameterSelection: could not set options " + "in getOptions().");
		}
		else superOptions = super.getOptions();
		final String[] options = new String[superOptions.length + m_CVParams.size() * 2 + 2];
		
		int current = 0;
		for (int i = 0; i < m_CVParams.size(); i++) {
			options[current++] = "-P";
			options[current++] = "" + getCVParameter(i);
		}
		options[current++] = "-X";
		options[current++] = "" + getNumFolds();
		
		System.arraycopy(superOptions, 0, options, current, superOptions.length);
		
		return options;
	}
	
	/**
	 * A method going recursive through all options to evaluate and saving options set for later evaluating
	 * 
	 * @param depth
	 *            current option
	 * @param optionTupel
	 *            current option set
	 * @return optionSet
	 */
	protected LinkedList<String[]> getOptionsSet(final int depth, final LinkedList<String[]> optionTupel) {
		if (depth < m_CVParams.size()) {
			if (m_CVParams.elementAt(depth) instanceof CVFixedParameter) {
				final CVFixedParameter cvParam = (CVFixedParameter) m_CVParams.elementAt(depth);
				for (final Double step : cvParam.m_ParamSteps) {
					cvParam.setM_ParamValue(step);
					getOptionsSet(depth + 1, optionTupel);
				}
			} else {
				final CVParameter cvParam = m_CVParams.elementAt(depth);
				double upper;
				switch ((int) (cvParam.m_Lower - cvParam.m_Upper + 0.5)) {
				case 1:
					upper = m_NumAttributes;
					break;
				case 2:
					upper = m_TrainFoldSize;
					break;
				default:
					upper = cvParam.m_Upper;
					break;
				}
				
				final double increment = (upper - cvParam.m_Lower) / (cvParam.m_Steps - 1);
				for (cvParam.setM_ParamValue(cvParam.m_Lower); cvParam.getM_ParamValue() <= upper; cvParam
						.setM_ParamValue(cvParam.getM_ParamValue() + increment))
					getOptionsSet(depth + 1, optionTupel);
			}
		} else optionTupel.add(createOptions());
		return optionTupel;
	}
	
	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 5928 $");
	}
	
	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed information about the technical
	 * background of this class, e.g., paper reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;
		
		result = new TechnicalInformation(Type.PHDTHESIS);
		result.setValue(Field.AUTHOR, "R. Kohavi");
		result.setValue(Field.YEAR, "1995");
		result.setValue(Field.TITLE, "Wrappers for Performance Enhancement and Oblivious Decision Graphs");
		result.setValue(Field.SCHOOL, "Stanford University");
		result.setValue(Field.ADDRESS, "Department of Computer Science, Stanford University");
		
		return result;
	}
	
	/**
	 * Returns a string describing this classifier
	 * 
	 * @return a description of the classifier suitable for displaying in the explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Class for performing parameter selection by cross-validation " + "for any classifier.\n\n"
				+ "For more information, see:\n\n" + getTechnicalInformation().toString();
	}
	
	/**
	 * Returns graph describing the classifier (if possible).
	 * 
	 * @return the graph of the classifier in dotty format
	 * @throws Exception
	 *             if the classifier cannot be graphed
	 */
	@Override
	public String graph() throws Exception {
		
		if (m_Classifier instanceof Drawable) return ((Drawable) m_Classifier).graph();
		throw new Exception("Classifier: " + m_Classifier.getClass().getName() + " "
				+ Utils.joinOptions(m_BestClassifierOptions) + " cannot be graphed");
	}
	
	/**
	 * Returns the type of graph this classifier represents.
	 * 
	 * @return the type of graph this classifier represents
	 */
	@Override
	public int graphType() {
		
		if (m_Classifier instanceof Drawable) return ((Drawable) m_Classifier).graphType();
		return Drawable.NOT_DRAWABLE;
	}
	
	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options.
	 */
	@Override
	public Enumeration<Option> listOptions() {
		
		final Vector<Option> newVector = new Vector<>(2);
		
		newVector.addElement(new Option("\tNumber of folds used for cross validation (default 10).", "X", 1,
				"-X <number of folds>"));
		newVector.addElement(new Option("\tClassifier parameter options.\n"
				+ "\teg: \"N 1 5 10\" Sets an optimisation parameter for the\n"
				+ "\tclassifier with name -N, with lower bound 1, upper bound\n"
				+ "\t5, and 10 optimisation steps. The upper bound may be the\n"
				+ "\tcharacter 'A' or 'I' to substitute the number of\n"
				+ "\tattributes or instances in the training data,\n"
				+ "\trespectively. This parameter may be supplied more than\n"
				+ "\tonce to optimise over several classifier options\n" + "\tsimultaneously.", "P", 1,
				"-P <classifier parameter>"));
		
		final Enumeration<Option> enu = super.listOptions();
		while (enu.hasMoreElements())
			newVector.addElement(enu.nextElement());
		return newVector.elements();
	}
	
	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the explorer/experimenter gui
	 */
	public String numFoldsTipText() {
		return "Get the number of folds used for cross-validation.";
	}
	
	/**
	 * Set method for CVParameters.
	 * 
	 * @param params
	 *            the CVParameters to use
	 * @throws Exception
	 *             if the setting of the CVParameters fails
	 */
	public void setCVParameters(final Object[] params) throws Exception {
		
		final FastVector<CVParameter> backup = m_CVParams;
		m_CVParams = new FastVector<>();
		
		for (final Object param : params)
			try {
				addCVParameter((String) param);
			} catch (final Exception ex) {
				m_CVParams = backup;
				throw ex;
			}
	}
	
	/**
	 * Sets the number of folds for the cross-validation.
	 * 
	 * @param numFolds
	 *            the number of folds for the cross-validation
	 * @throws Exception
	 *             if parameter illegal
	 */
	public void setNumFolds(final int numFolds) throws Exception {
		
		if (numFolds < 0)
			throw new IllegalArgumentException("Stacking: Number of cross-validation " + "folds must be positive.");
		m_NumFolds = numFolds;
	}
	
	/**
	 * Parses a given list of options.
	 * <p/>
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 * -X &lt;number of folds&gt;
	 *  Number of folds used for cross validation (default 10).
	 * </pre>
	 * 
	 * <pre>
	 * -P &lt;classifier parameter&gt;
	 *  Classifier parameter options.
	 *  eg: "N 1 5 10" Sets an optimisation parameter for the
	 *  classifier with name -N, with lower bound 1, upper bound
	 *  5, and 10 optimisation steps. The upper bound may be the
	 *  character 'A' or 'I' to substitute the number of
	 *  attributes or instances in the training data,
	 *  respectively. This parameter may be supplied more than
	 *  once to optimise over several classifier options
	 *  simultaneously.
	 * </pre>
	 * 
	 * <pre>
	 * -S &lt;num&gt;
	 *  Random number seed.
	 *  (default 1)
	 * </pre>
	 * 
	 * <pre>
	 * -D
	 *  If set, classifier is run in debug mode and
	 *  may output additional info to the console
	 * </pre>
	 * 
	 * <pre>
	 * -W
	 *  Full name of base classifier.
	 *  (default: weka.classifiers.rules.ZeroR)
	 * </pre>
	 * 
	 * <pre>
	 * Options specific to classifier weka.classifiers.rules.ZeroR:
	 * </pre>
	 * 
	 * <pre>
	 * -D
	 *  If set, classifier is run in debug mode and
	 *  may output additional info to the console
	 * </pre>
	 * 
	 * <!-- options-end --> Options after -- are passed to the designated sub-classifier.
	 * <p>
	 * 
	 * @param options
	 *            the list of options as an array of strings
	 * @throws Exception
	 *             if an option is not supported
	 */
	@Override
	public void setOptions(final String[] options) throws Exception {
		
		final String foldsString = Utils.getOption('X', options);
		if (foldsString.length() != 0) setNumFolds(Integer.parseInt(foldsString));
		else setNumFolds(10);
		
		boolean nor = false;
		String cvParam;
		m_CVParams = new FastVector<>();
		try {
			do {
				cvParam = Utils.getOption('P', options);
				if (cvParam.length() != 0) addCVParameter(cvParam);
			} while (cvParam.length() != 0);
		} catch (final Exception e) {
			System.err.println("it seems you didnt provide a -P option. Trying -PX instead:");
			nor = true;
		}
		try {
			do {
				cvParam = Utils.getOption("PX", options);
				if (cvParam.length() != 0) addCVFixedParameter(cvParam);
			} while (cvParam.length() != 0);
		} catch (final Exception e) {
			if (nor) {
				System.err.println("it seems you didnt provide a -P nor a -PX option.\n please read doc und restart");
				throw new Exception();
			}
		}
		
		super.setOptions(options);
	}
	
	/**
	 * Returns description of the cross-validated classifier.
	 * 
	 * @return description of the cross-validated classifier as a string
	 */
	@Override
	public String toString() {
		
		if (m_InitOptions == null) return "CVParameterSelection: No model built yet.";
		
		String result =
				"Cross-validated Parameter selection.\n" + "Classifier: " + m_Classifier.getClass().getName() + "\n";
		try {
			for (int i = 0; i < m_CVParams.size(); i++) {
				final CVParameter cvParam = m_CVParams.elementAt(i);
				result +=
						"Cross-validation Parameter: '-" + cvParam.m_ParamChar + "'" + " ranged from "
								+ cvParam.m_Lower + " to ";
				switch ((int) (cvParam.m_Lower - cvParam.m_Upper + 0.5)) {
				case 1:
					result += m_NumAttributes;
					break;
				case 2:
					result += m_TrainFoldSize;
					break;
				default:
					result += cvParam.m_Upper;
					break;
				}
				result += " with " + cvParam.m_Steps + " steps\n";
			}
		} catch (final Exception ex) {
			result += ex.getMessage();
		}
		result +=
				"Classifier Options: " + Utils.joinOptions(m_BestClassifierOptions) + "\n\n" + m_Classifier.toString();
		return result;
	}
	
	/**
	 * A concise description of the model.
	 * 
	 * @return a concise description of the model
	 */
	@Override
	public String toSummaryString() {
		
		final String result = "Selected values: " + Utils.joinOptions(m_BestClassifierOptions);
		return result + '\n';
	}
}
