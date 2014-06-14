package de.tum.in.weka;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;

import weka.core.Utils;

/**
 * @author LehnereS
 */
public class GridEvalJobs {
	
	@SuppressWarnings ({ "javadoc", "resource" })
	public static void main(final String[] args) throws Exception {
		final String inputFolder = Utils.getOption("input", args);
		System.err.println("reading finished jobs: ");
		final LinkedList<JobObject> jobList = new JobReader().readJobObjects(inputFolder);
		System.err.println("reading finished jobs: done");
		System.err.print("creating logfile (" + inputFolder + "/logfile.log)...");
		final File logfile = new File(inputFolder + "/logfile.log");
		if (logfile.exists()) logfile.renameTo(new File(inputFolder + "/logfile.log.old"));
		else logfile.createNewFile();
		final PrintWriter out = new PrintWriter(logfile);
		System.err.println("done");
		System.err.println("starting evaluation");
		out.println(new GridEvaluation().evalJobObjects(args, jobList));
		out.close();
		System.err.println("evaluation done: " + logfile);
	}
}