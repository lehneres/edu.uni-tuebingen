package de.tum.in.weka;

/**
 * @author LehnereS
 */
public class testing {
	
	@SuppressWarnings ("javadoc")
	public static void main(final String[] argv) throws Exception {
		for (int i = 0; i < 1; i++) {
			final String[] args =
					{ "./CVParameterGridTest" + i, "-PX", "C 0.1 1 10 100", "-W", "weka.classifiers.functions.SMO",
							"-t", "test" + i + ".arff", "--", "-K",
							"weka.classifiers.functions.supportVector.RBFKernel" };
			GridCreateJobs.main(args);
			final long start = System.currentTimeMillis();
			GridEvalJobs.main(args);
			System.out.println("time for test" + i + ".arff: " + (System.currentTimeMillis() - start));
		}
	}
}
