package de.tum.in.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * @author LehnereS
 */
public class JobObject implements Serializable {
	
	private Classifier		classifier;
	private int				classifierID	= -1;
	private int				cvId;
	private Instances		data;
	private GridEvaluation	evaluation;
	private boolean			isRef;
	private int				jobID;
	private String			jobName;
	private int				mNumFolds;
	private String[]		options;
	private String			path;
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final Classifier classifierBackup) {
		setJobName(jobName);
		setJobID(jobId);
		setClassifier(classifierBackup);
		setCvId(cvId);
	}
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final Classifier classifier,
			final Instances tempTrain) {
		setJobName(jobName);
		setJobID(jobId);
		setClassifier(classifier);
		setData(tempTrain);
		setCvId(cvId);
	}
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final GridEvaluation eval) {
		setJobName(jobName);
		setJobID(jobId);
		setEvaluation(eval);
		setCvId(cvId);
	}
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final Instances tempTrain) {
		setJobName(jobName);
		setJobID(jobId);
		setData(tempTrain);
		setCvId(cvId);
	}
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final int classifierId,
			final int mNumFolds, final Classifier mClassifier, final Instances tempTrain) {
		setJobName(jobName);
		setJobID(jobId);
		setClassifierID(classifierId);
		setmNumFolds(mNumFolds);
		setClassifier(mClassifier);
		setData(tempTrain);
		setCvId(cvId);
	}
	
	/**
	 * @param jobName
	 * @param cvId
	 * @param jobId
	 * @param classifierId
	 * @param path
	 */
	public JobObject(final String jobName, final int cvId, final int jobId, final int classifierId, final String path) {
		this.jobName = jobName;
		this.cvId = cvId;
		jobID = jobId;
		classifierID = classifierId;
		this.path = path;
		isRef = true;
	}
	
	@SuppressWarnings ("javadoc")
	public JobObject(final String jobName, final int cvId, final int jobId, final String[] options) {
		setJobName(jobName);
		setJobID(jobId);
		setOptions(options);
		setCvId(cvId);
	}
	
	@SuppressWarnings ("javadoc")
	public void execute() throws Exception {
		if (getClassifier() != null)
			getClassifier().buildClassifier(getData().trainCV(getmNumFolds(), getClassifierId(), new Random(1)));
	}
	
	/**
	 * @return the classifier
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings ("resource")
	public Classifier getClassifier() throws FileNotFoundException, IOException, ClassNotFoundException {
		if (isRef) {
			final ObjectInputStream oi =
					new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(path))));
			return ((JobObject) oi.readObject()).getClassifier();
		}
		return classifier;
	}
	
	/**
	 * @return the classifierID
	 */
	public int getClassifierId() {
		return classifierID;
	}
	
	/**
	 * @return the cvId
	 */
	public int getCvId() {
		return cvId;
	}
	
	/**
	 * @return the data
	 */
	public Instances getData() {
		return data;
	}
	
	/**
	 * @return the evaluation
	 */
	public GridEvaluation getEvaluation() {
		return evaluation;
	}
	
	/**
	 * @return the jobID
	 */
	public int getJobId() {
		return jobID;
	}
	
	/**
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
	}
	
	/**
	 * @return the mNumFolds
	 */
	public int getmNumFolds() {
		return mNumFolds;
	}
	
	/**
	 * @return the options
	 */
	public String[] getOptions() {
		return options;
	}
	
	/**
	 * @param classifier
	 *            the classifier to set
	 */
	public void setClassifier(final Classifier classifier) {
		this.classifier = classifier;
	}
	
	/**
	 * @param classifierID
	 *            the classifierID to set
	 */
	public void setClassifierID(final int classifierID) {
		this.classifierID = classifierID;
	}
	
	/**
	 * @param cvId
	 *            the cvId to set
	 */
	public void setCvId(final int cvId) {
		this.cvId = cvId;
	}
	
	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(final Instances data) {
		this.data = data;
	}
	
	/**
	 * @param evaluation
	 *            the evaluation to set
	 */
	public void setEvaluation(final GridEvaluation evaluation) {
		this.evaluation = evaluation;
	}
	
	/**
	 * @param jobID
	 *            the jobID to set
	 */
	public void setJobID(final int jobID) {
		this.jobID = jobID;
	}
	
	/**
	 * @param jobName
	 *            the jobName to set
	 */
	public void setJobName(final String jobName) {
		this.jobName = jobName;
	}
	
	/**
	 * @param mNumFolds
	 *            the mNumFolds to set
	 */
	public void setmNumFolds(final int mNumFolds) {
		this.mNumFolds = mNumFolds;
	}
	
	/**
	 * @param options
	 *            the options to set
	 */
	public void setOptions(final String[] options) {
		this.options = options;
	}
	
}
