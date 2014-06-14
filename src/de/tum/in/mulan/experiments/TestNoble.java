package de.tum.in.mulan.experiments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import de.tum.in.mulan.classifier.meta.AbstractNoble;

@SuppressWarnings ("javadoc")
public class TestNoble {
	
	static String[][]	dataSets	= {
			{ "kinase", "data/mr/kinase/kinase.left.arff", "data/mr/kinase/kinase.top.arff",
			"data/mr/kinase/labels.kinase.arff" },
			{ "gene", "data/mr/gene/left.regulators.arff", "data/mr/gene/top.tfbs.arff",
			"data/mr/gene/labels.gene.arff" },
			// { "nci",
			// "data/mr/nci/nci.left.arff",
			// "data/mr/nci/nci.top.arff",
			// "data/mr/nci/labels.nci.disc.arff"
			// },
			{ "nci_new", "data/mr/nci_new/nci.left.arff", "data/mr/nci_new/nci.top.arff",
			"data/mr/nci_new/labels.nci.disc.arff" } };
	static String		dbpPath		= "resources/DBP-progs_x64";
	static String		outputPath	= "nobleResults";
	
	private static void generateJobs(final String name, final String fileName) throws IOException {
		
		final ArrayList<String[]> jobs = new ArrayList<>();
		for (final String[] dataset : TestNoble.dataSets) {
			final ArffLoader loader = new ArffLoader();
			loader.setFile(new File(dataset[3]));
			final Instances center = loader.getDataSet();
			final double[] tar = new double[9];
			final int[] kar = new int[Math.min(63, center.numAttributes()) - 1];
			final int[] kartransp = new int[Math.min(63, center.numInstances()) - 1];
			for (int i = 0; i < 9; i++)
				tar[i] = (i + 1.0) / 10.0;
			for (int i = 0; i < kar.length; i++)
				kar[i] = i + 2;
			for (int i = 0; i < kartransp.length; i++)
				kartransp[i] = i + 2;
			
			for (final boolean transpose : new boolean[] { true, false }) {
				final int[] workkarr = transpose ? kartransp : kar;
				for (final double t : tar)
					for (final int k : workkarr) {
						final String[] job =
								new String[] { dataset[0], dataset[1], dataset[2], dataset[3], k + "", t + "",
										transpose + "" };
						jobs.add(job);
					}
			}
		}
		
		final String command = TestSuite.baseCommand + " de.tum.in.mulan.classifier.transformation.Noble -D";
		final StringBuilder sb = new StringBuilder();
		sb.append(TestSuite.header.replaceAll("%NAME%", name));
		sb.append("case $SGE_TASK_ID in\n");
		int taskid = 1;
		for (final String[] job : jobs)
			sb.append(taskid + ")\n\t" + command + " -id " + taskid++ + " -left " + job[1] + " -top " + job[2]
					+ " -center " + job[3] + " -k " + job[4] + " -t " + job[5] + " -S " + "weka.classifiers.trees.J48"
					+ " -folds " + TestSuite.folds + " -dbp " + TestNoble.dbpPath
					+ (new Boolean(job[6]) ? " -transpose" : "") + " -output " + outputPath + " -dir " + ". ;;\n");
		TestSuite.writeJobs(fileName, sb, taskid);
	}
	
	public static void main(final String[] args) throws NumberFormatException, Exception {
		if (Utils.getFlag("run", args)) {
			System.err
					.println("CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE CLUSTER MODE \n");
			AbstractNoble.main(args);
		} else if (Utils.getFlag("jobs", args)) {
			System.err
					.println("JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE JOB-GENERATING MODE \n");
			TestNoble.generateJobs("Noble", "NobleJobs");
		} else if (Utils.getFlag("read", args)) System.err
				.println("RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE RESULT MODE \n");
		// TestSuite.readResults("./results/field/aktuell/Noble.zip",
		// "readFCMLResults", TestSuite.folds);
		else System.err
				.println("TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE TESTING MODE \npress ENTER to start...\n");
	}
}
