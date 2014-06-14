package de.tum.in.weka;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * @author LehnereS
 */
public class JobWriter {
	
	private final LinkedList<JobObject>	jobsToWrite;
	
	/**
	 * @param objectList
	 */
	public JobWriter(final LinkedList<JobObject> objectList) {
		jobsToWrite = objectList;
	}
	
	/**
	 * @param outputFolder
	 * @throws Exception
	 */
	public void startAll(final String outputFolder) throws Exception {
		// Runtime.getRuntime().exec("sh " + outputFolder + "/startAll.sh");
		
		// if jobs are performed directly: (for testing purpose only)
		for (final JobObject job : jobsToWrite) {
			final String jobName = job.getJobName();
			final int cvId = job.getCvId();
			final int jobId = job.getJobId();
			final int classifierId = job.getClassifierId();
			if (classifierId >= 0) {
				final String[] tmp =
						{ outputFolder + "/" + jobName + "/" + "job-" + cvId + "-" + jobId + "-" + classifierId };
				runJob.main(tmp);
			}
		}
	}
	
	/**
	 * @param outputFolder
	 * @param queue
	 * @param heapSize
	 * @param wekajar
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings ("resource")
	public void write(final String outputFolder, final String queue, final String heapSize, final String wekajar)
			throws IOException, ClassNotFoundException, FileNotFoundException {
		new File(outputFolder).mkdir();
		final File startAllFile = new File(outputFolder + "/startAll");
		if (startAllFile.exists()) startAllFile.delete();
		final PrintWriter startAll = new PrintWriter(startAllFile);
		startAll.println("#!/bin/bash");
		for (final JobObject job : jobsToWrite)
			if (job.getClassifierId() >= 0) {
				new File(outputFolder + "/" + job.getJobName()).mkdirs();
				final PrintWriter jobStream =
						new PrintWriter(new File(outputFolder + "/" + job.getJobName() + "/" + "clusterjob-"
								+ job.getCvId() + "-" + job.getJobId() + "-" + job.getClassifierId()));
				jobStream.print("#$-cwd\njava " + (!wekajar.equals("") ? "-cp .:" + wekajar : "") + " -Xmx" + heapSize
						+ " runJob " + outputFolder + "/" + job.getJobName() + "/" + "job-" + job.getCvId() + "-"
						+ job.getJobId() + "-" + job.getClassifierId());
				jobStream.close();
				startAll.println("qsub " + (!queue.equals("") ? "-l " + queue + "=true " : "") + outputFolder + "/"
						+ job.getJobName() + "/" + "clusterjob-" + job.getCvId() + "-" + job.getJobId() + "-"
						+ job.getClassifierId());
			}
		startAll.close();
	}
}