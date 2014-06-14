package de.tum.in.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * @author LehnereS
 */
public class runJob {
	
	@SuppressWarnings ("javadoc")
	public static void main(final String[] args) throws Exception {
		final runJob workingJob = new runJob(args[0]);
		workingJob.execute();
		ObjectWriter.writeFinishedJob(workingJob.currentJob, workingJob.inputfolder);
	}
	
	private final JobObject	currentJob;
	
	private final String	inputfolder;
	
	@SuppressWarnings ({ "javadoc", "resource" })
	public runJob(final String fileName) throws IOException, ClassNotFoundException {
		// splitting input string
		// final String[] fileNameArray = fileName.split("/");
		inputfolder = new File(fileName).getParentFile().getParent();
		// fileNameArray[0] != "." ? fileNameArray[1] : fileNameArray[0];
		ObjectInputStream o = new ObjectInputStream(new FileInputStream(fileName));
		currentJob = (JobObject) o.readObject();
		o.close();
		o =
				new ObjectInputStream(new FileInputStream(new File(inputfolder + "/datasource/data-"
						+ currentJob.getJobName() + "-" + currentJob.getCvId())));
		currentJob.setData(((JobObject) o.readObject()).getData());
		o.close();
		new File(fileName).delete();
	}
	
	/**
	 * @throws Exception
	 */
	private void execute() throws Exception {
		currentJob.execute();
	}
}