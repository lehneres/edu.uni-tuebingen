package de.tum.in.weka;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

import weka.classifiers.Classifier;

/**
 * class writing all objects structure: <ouputfolder>/<name>/job-cvId-jobId-classifierId
 * 
 * @author LehnereS
 */
public class ObjectWriter {
	
	/**
	 * deleting recursively given folder
	 * 
	 * @param jobFolder
	 */
	static boolean deleteFolder(final File jobFolder) {
		if (jobFolder.isDirectory()) {
			final String[] entries = jobFolder.list();
			for (final String entrie : entries)
				ObjectWriter.deleteFolder(new File(jobFolder.getPath(), entrie));
			return jobFolder.delete();
		}
		return jobFolder.delete();
	}
	
	/**
	 * writing finished job
	 * 
	 * @param job
	 * @param outputFolder
	 *            s
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings ({ "resource", "cast" })
	public static void writeFinishedJob(final JobObject job, final String outputFolder) throws FileNotFoundException,
			IOException, ClassNotFoundException {
		final String containerName = job.getJobName();
		final int cvId = job.getCvId();
		final int jobId = job.getJobId();
		final int classifierId = job.getClassifierId();
		try {
			if (job.getJobName() != "datasource") {
				// deleting job data in order to get smaller files
				job.setData(null);
				// writing new job, compressing in order to get smaller files
				final ObjectOutputStream o =
						new ObjectOutputStream(new GZIPOutputStream(
								new FileOutputStream(new File(outputFolder + "/" + containerName + "/" + "job-" + cvId
										+ "-" + jobId + "-" + classifierId + "-finished"))));
				o.writeObject(job);
				o.close();
			}
		} catch (final FileNotFoundException e) {
			System.err.println("file to write could not be opend/created!");
			System.exit(1);
		} catch (final IOException e) {
			System.err.println(job.getJobName() + " " + job.getJobId() + " " + job.getClassifierId()
					+ " is classifier: " + (job.getClassifier() instanceof Classifier) + " is evaluation: "
					+ (job.getEvaluation() instanceof GridEvaluation) + " object could not be written!");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private final LinkedList<JobObject>	jobsToWrite;
	
	@SuppressWarnings ("javadoc")
	public ObjectWriter(final LinkedList<JobObject> jobList) {
		jobsToWrite = jobList;
	}
	
	/**
	 * method writing given <jobsToWrite> to <outputfolder>
	 * 
	 * @param outputFolder
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings ({ "resource", "cast" })
	public void write(final String outputFolder) throws FileNotFoundException, IOException, ClassNotFoundException {
		
		// iterating through the jobsToWrite List and writing down all
		for (final JobObject job : jobsToWrite) {
			final String jobName = job.getJobName();
			final int cvId = job.getCvId();
			final int jobId = job.getJobId();
			final int classifierId = job.getClassifierId();
			try {
				// if jobId == -2 the current job is a datasource and will be
				// stored in
				// <outputfolder>/datasource
				if (jobId == -2) {
					// creating folder structure
					new File(outputFolder + "/datasource").mkdirs();
					final ObjectOutputStream o =
							new ObjectOutputStream(new FileOutputStream(new File(outputFolder + "/datasource/data-"
									+ jobName + "-" + cvId)));
					o.writeObject(job);
					o.close();
				} else {
					// creating folder structure
					new File(outputFolder + "/" + jobName).mkdirs();
					// deleting job data in order to get smaller files
					job.setData(null);
					final ObjectOutputStream o =
							new ObjectOutputStream(new FileOutputStream(new File(outputFolder + "/" + jobName + "/job-"
									+ cvId + "-" + jobId + "-" + classifierId)));
					o.writeObject(job);
					o.close();
				}
				
			} catch (final FileNotFoundException e) {
				System.err.println("file to write could not be opend/created!");
				System.exit(1);
			} catch (final IOException e) {
				System.err.println(job.getJobName() + " " + job.getJobId() + " " + job.getClassifierId()
						+ " is classifier: " + (job.getClassifier() instanceof Classifier) + " is evaluation: "
						+ (job.getEvaluation() instanceof GridEvaluation) + " object could not be written!");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
