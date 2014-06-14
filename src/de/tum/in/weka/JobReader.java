package de.tum.in.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.LinkedList;

/**
 * @author LehnereS
 */
public class JobReader {
	
	/**
	 * @param inputFolder
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings ({ "javadoc", "resource" })
	public LinkedList<JobObject> readJobObjects(final String inputFolder) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		final LinkedList<JobObject> finishedJobs = new LinkedList<>();
		final File input = new File(inputFolder);
		if (input.isDirectory())
			for (final String entry : input.list()) {
				System.err.println("reading " + inputFolder + "/" + entry);
				if (new File(inputFolder + "/" + entry).isDirectory()) finishedJobs.addAll(readJobObjects(inputFolder
						+ "/" + entry));
				else if (entry.contains("finished")) {
					final String jobname = inputFolder.split("/")[inputFolder.split("/").length - 1];
					final String[] jobDetails = entry.split("-");
					System.err.println("creating new job (" + jobname + ", "
							+ Integer.parseInt(jobDetails[jobDetails.length - 4]) + ", "
							+ Integer.parseInt(jobDetails[jobDetails.length - 3]) + ", "
							+ Integer.parseInt(jobDetails[jobDetails.length - 2]) + ")");
					finishedJobs.add(new JobObject(jobname, Integer.parseInt(jobDetails[jobDetails.length - 4]),
							Integer.parseInt(jobDetails[jobDetails.length - 3]), Integer
									.parseInt(jobDetails[jobDetails.length - 2]), new File(input.getPath(), entry)
									.getPath()));
				} else if (!entry.contains("cluster") && entry.contains("job-") || entry.contains("data"))
					try {
						final ObjectInputStream oi =
								new ObjectInputStream(new FileInputStream(new File(input.getPath(), entry)));
						System.err.print("creating new object...");
						finishedJobs.add((JobObject) oi.readObject());
						oi.close();
						System.err.println("done!");
					} catch (final StreamCorruptedException e) {
						System.err.println(e.getMessage());
						System.exit(4);
					}
			}
		return finishedJobs;
	}
}
