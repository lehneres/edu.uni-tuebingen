package de.tum.in.multilabel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

@SuppressWarnings ("javadoc")
public class Job implements Runnable {
	
	String		dir;
	int			ID;
	String[]	options;
	
	/**
	 * @param args
	 *            the options
	 */
	public Job(final String[] args, final String dir, final int ID) {
		options = args;
		this.ID = ID;
		this.dir = dir;
	}
	
	@SuppressWarnings ("resource")
	@Override
	public void run() {
		try {
			final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir + ID)));
			// writer.write(TestSuite.mainThread(this.options).toString());
			writer.flush();
			writer.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
}
