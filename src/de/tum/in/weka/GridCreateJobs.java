package de.tum.in.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.regex.Pattern;

import weka.core.Utils;

/**
 * class creating jobs for using CVParameterSelection on a compute-cluster based on Sun Gird Engine. start with:
 * GridCreateJobs <outputFolder> <weka options>
 * 
 * @author LehnereS
 */
public class GridCreateJobs {
	
	/**
	 * main class for GridCreateJob
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		GridCreateJobs.writeJobObjects(args);
	}
	
	/**
	 * writing JobObjects, first starting GridEvaluation.createJobs() to get a job list, containing all objects needed
	 * to perform CVParameterSelection (eg. classifiers, instances, evaluation)
	 * 
	 * @param currentArgs
	 * @throws Exception
	 */
	private static void writeJobObjects(final String[] args) throws Exception {
		// reading output folder and splitting args
		final String outputFolder = Utils.getOption("output", args);
		final String queue = Utils.getOption("queue", args);
		String heapSize = Utils.getOption("heap", args);
		final String wekajar = Utils.getOption("wekajar", args);
		final boolean isTest = Utils.getFlag("isTest", args);
		
		if (!Pattern.matches("[1-9][0-9]+[mgMG]", heapSize) && !heapSize.equals(""))
			throw new Exception("wrong heapsize format: -[1-9][0-9]+[mgMG]");
		if (heapSize.equals("")) heapSize = "2000M";
		
		// if outputFolder allready exists deleting it or trying new one
		if (new File(outputFolder).exists()) {
			System.err.print("output folder already exists: override it? (type YES/NO):");
			final String bf = new BufferedReader(new InputStreamReader(System.in)).readLine();
			if (bf.toUpperCase().equals("YES") || bf.toUpperCase().equals("Y")) ObjectWriter.deleteFolder(new File(
					outputFolder));
			else if (bf.toUpperCase().equals("NO") || bf.toUpperCase().equals("N")) {
				System.err.print("please enter new folder name:");
				final String newName = new BufferedReader(new InputStreamReader(System.in)).readLine();
				final String[] currentArgs = new String[args.length];
				System.arraycopy(args, 1, currentArgs, 1, args.length - 1);
				currentArgs[0] = newName;
				GridCreateJobs.writeJobObjects(currentArgs);
				return;
			} else {
				GridCreateJobs.writeJobObjects(args);
				return;
			}
		}
		System.err.print("creating jobs in " + outputFolder + "....");
		// getting jobList from GridEvaluation
		final LinkedList<JobObject> jobList = GridEvaluation.createJobObjects(new CVParameterSelectionGrid(), args);
		System.err.println("done!");
		// writing objects to disk
		System.err.print("writing jobs [objects] in " + outputFolder + "....");
		new ObjectWriter(jobList).write(outputFolder);
		System.err.println("done!");
		// writing jobs
		final JobWriter writer = new JobWriter(jobList);
		System.err.print("writing jobs [clusterjobs] in " + outputFolder + "....");
		writer.write(outputFolder, queue, heapSize, wekajar);
		System.err.println("done!");
		if (isTest) {
			System.err.print("training all classifiers [for testing purpose] in " + outputFolder + "....");
			writer.startAll(outputFolder);
			System.err.println("done!");
		}
	}
}