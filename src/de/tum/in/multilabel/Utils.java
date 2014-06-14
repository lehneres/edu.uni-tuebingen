package de.tum.in.multilabel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.channels.FileLock;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import mulan.classifier.MultiLabelLearner;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.NumberOfClustersRequestable;

/**
 * containing usefully code snippets
 * 
 * @author LehnereS
 */
public class Utils {
	
	private static final int	StandardChainLength	= 10;
	
	/**
	 * @param array
	 *            an int array
	 * @return array as list
	 */
	public static List<Integer> arrayAsList(final int[] array) {
		return Utils.arrayAsList(Utils.convertIntegerArray(array));
	}
	
	/**
	 * @param <T>
	 *            any object array
	 * @param array
	 *            an int array
	 * @return array as list
	 */
	public static <T> List<T> arrayAsList(final T[] array) {
		final List<T> list = new LinkedList<>();
		for (final T i : array)
			list.add(i);
		return list;
	}
	
	/**
	 * @param arrays
	 *            int[] arrays
	 * @return merged int[] array
	 */
	public static int[] arrayMerge(final int[] ... arrays) {
		final Integer[][] objectArrays = new Integer[arrays.length][];
		for (int i = 0; i < arrays.length; i++)
			objectArrays[i] = Utils.convertIntegerArray(arrays[i]);
		final int[] unsorted = Utils.convertIntegerArray(Utils.arrayMerge(objectArrays));
		// Arrays.sort(unsorted);
		return unsorted;
	}
	
	/**
	 * @param <T>
	 *            any object array
	 * @param arrays
	 *            any number of arrays
	 * @return merged array
	 */
	@SafeVarargs
	public static <T> T[] arrayMerge(final T[] ... arrays) {
		int count = 0;
		for (final T[] array : arrays)
			count += array.length;
		final T[] mergedArray = (T[]) Array.newInstance(arrays[0][0].getClass(), count);
		int start = 0;
		for (final T[] array : arrays) {
			System.arraycopy(array, 0, mergedArray, start, array.length);
			start += array.length;
		}
		return mergedArray;
	}
	
	/**
	 * @param <T>
	 *            any object array
	 * @param arrays
	 *            any number of arrays
	 * @return merged array with unique elements
	 */
	@SafeVarargs
	public static <T> T[] arrayMergeUnique(final T[] ... arrays) {
		final T[] tmp = Utils.arrayMerge(arrays);
		final TreeSet<T> tmpset = new TreeSet<>();
		for (final T i : tmp)
			tmpset.add(i);
		return (T[]) tmpset.toArray();
	}
	
	/**
	 * returns the given set in a string representation.
	 * 
	 * @param <E>
	 *            any object, should implement .toString()
	 * @param set
	 *            the set to return in a string representation
	 * @return the set as string
	 */
	public static <E> String collectionToString(final Collection<E> set) {
		String result = "";
		for (final E i : set)
			result += i + " ";
		return result.substring(0, result.length() - 1);
	}
	
	/**
	 * converts a int array into a Integer array
	 * 
	 * @param array
	 *            the Integer array
	 * @return a Integer array
	 */
	public static Integer[] convertIntegerArray(final int[] array) {
		final Integer[] res = new Integer[array.length];
		for (int i = 0; i < array.length; i++)
			res[i] = array[i];
		return res;
	}
	
	/**
	 * converts a Integer array into a int array
	 * 
	 * @param array
	 *            the Integer array
	 * @return a int array
	 */
	public static int[] convertIntegerArray(final Integer[] array) {
		final int[] res = new int[array.length];
		for (int i = 0; i < array.length; i++)
			res[i] = array[i];
		return res;
	}
	
	/**
	 * divides double array by given delimiter
	 * 
	 * @param doubles
	 *            first double array
	 * @param delimiter
	 *            second double array
	 * @return double array divided by delimiter
	 */
	public static double[] div(final double[] doubles, final double delimiter) {
		final double[] div = new double[doubles.length];
		for (int i = 0; i < doubles.length; i++)
			div[i] = doubles[i] / delimiter;
		return div;
	}
	
	/**
	 * @param slClassifier
	 *            weka.classifiers.Classifier
	 * @param mlLearnerName
	 *            mulan.classifier.MultiLabelLearner
	 * @return mulan.classifier.MultiLabelLearner
	 * @throws Exception
	 *             any exception
	 */
	public static MultiLabelLearner getMLLearnerFromName(final Classifier slClassifier, final String mlLearnerName)
			throws Exception {
		MultiLabelLearner mlClassifier;
		if (mlLearnerName.equals("mulan.classifier.transformation.EnsembleOfClassifierChains")) mlClassifier =
				(MultiLabelLearner) Class.forName(mlLearnerName)
						.getConstructor(Classifier.class, Integer.TYPE, Boolean.TYPE, Boolean.TYPE)
						.newInstance(AbstractClassifier.makeCopy(slClassifier), Utils.StandardChainLength, true, false);
		else if (mlLearnerName.equals("mulan.classifier.transformation.MultiLabelStacking")) mlClassifier =
				(MultiLabelLearner) Class
						.forName(mlLearnerName)
						.getConstructor(Classifier.class, Classifier.class)
						.newInstance(AbstractClassifier.makeCopy(slClassifier),
								AbstractClassifier.makeCopy(slClassifier));
		else if (mlLearnerName.equals("mulan.classifier.lazy.MLkNN")) mlClassifier =
				(MultiLabelLearner) Class.forName(mlLearnerName).newInstance();
		else mlClassifier =
				(MultiLabelLearner) Class.forName(mlLearnerName).getConstructor(Classifier.class)
						.newInstance(AbstractClassifier.makeCopy(slClassifier));
		return mlClassifier;
	}
	
	/**
	 * @param in_input
	 *            input
	 * @return output
	 */
	public static int[] getRanges(final String in_input) {
		final Vector<Integer> ranges = new Vector<>(10);
		String input = in_input;
		while (!input.equals("")) {
			String range = input.trim();
			final int commaLoc = input.indexOf(',');
			if (commaLoc != -1) {
				range = input.substring(0, commaLoc).trim();
				input = input.substring(commaLoc + 1).trim();
			} else input = "";
			if (!range.equals("")) if (range.contains("-")) {
				final String[] tmpRange = range.split("-");
				final int start = Integer.valueOf(tmpRange[0]);
				final int end = Integer.valueOf(tmpRange[1]);
				for (int i = start; i <= end; i++)
					ranges.addElement(i);
			} else ranges.addElement(Integer.valueOf(range));
		}
		final Integer[] rangeArray = ranges.toArray(new Integer[ranges.size()]);
		final int[] finalArray = new int[rangeArray.length];
		for (int i = 0; i < finalArray.length; i++)
			finalArray[i] = rangeArray[i];
		
		return finalArray;
	}
	
	/**
	 * @param file
	 *            a file object
	 * @return true if the file is locked, false if not
	 * @throws IOException
	 *             some IO exception
	 */
	@SuppressWarnings ("resource")
	public static boolean isLocked(final File file) throws IOException {
		try {
			final FileLock lock = new RandomAccessFile(file, "rw").getChannel().tryLock();
			lock.release();
		} catch (final java.nio.channels.OverlappingFileLockException e) {
			return true;
		}
		return false;
	}
	
	/**
	 * compacts a given indices array (eg. 145,146,147 will become 145-147)
	 * 
	 * @param ranges
	 *            indices
	 * @return compacted indices as string
	 */
	public static String[] rangesCompact(final int[] ranges) {
		Arrays.sort(ranges);
		int old = -1;
		boolean slash = false;
		final StringBuffer failedBuffer = new StringBuffer();
		for (final int i : ranges) {
			if (i - 1 == old) {
				if (!slash) {
					// System.out.print("-");
					failedBuffer.append("-");
					slash = true;
				}
			} else {
				if (slash) {
					// System.out.print(old);
					failedBuffer.append(old);
					slash = false;
				}
				// System.out.print("," + i);
				failedBuffer.append("," + i);
			}
			old = i;
		}
		if (slash) failedBuffer.append(ranges[ranges.length - 1]);
		final String[] rangesStr = failedBuffer.toString().split(",");
		return rangesStr;
	}
	
	/**
	 * sets the given number of clusters to the cluster object
	 * 
	 * @param clusterer
	 *            {@link weka.clusterers.Clusterer}
	 * @param numberOfClusters
	 *            a number of clusters
	 * @throws Exception
	 *             if number of clusters cant be set
	 */
	public static void setNumberOfClusters(final Clusterer clusterer, final int numberOfClusters) throws Exception {
		if (clusterer instanceof HierarchicalClusterer) ((HierarchicalClusterer) clusterer)
				.setNumClusters(numberOfClusters);
		else if (clusterer instanceof NumberOfClustersRequestable) ((NumberOfClustersRequestable) clusterer)
				.setNumClusters(numberOfClusters);
		else throw new Exception("number of clusters could not be set");
	}
	
	/**
	 * sums two double arrays
	 * 
	 * @param double1
	 *            first double array
	 * @param double2
	 *            second double array
	 * @return sum of double arrays
	 * @throws UnexpectedException
	 *             if double array do not have same length
	 */
	public static double[] sum(final double[] double1, final double[] double2) throws UnexpectedException {
		if (double1.length != double2.length) throw new UnexpectedException("double[] not equal length");
		final double[] sum = new double[double1.length];
		for (int i = 0; i < double1.length; i++)
			sum[i] = double1[i] + double2[i];
		return sum;
	}
	
	/**
	 * @param obj
	 *            any object
	 * @return object as byte array
	 */
	@SuppressWarnings ("resource")
	public static byte[] toByteArray(final Object obj) {
		byte[] bytes = null;
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();
			bos.close();
			bytes = bos.toByteArray();
		} catch (final IOException ex) {
			// TODO: Handle the exception
		}
		return bytes;
	}
	
	/**
	 * @param bytes
	 *            byte array
	 * @return byte array as object
	 */
	public static Object toObject(final byte[] bytes) {
		Object obj = null;
		try {
			final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			final ObjectInputStream ois = new ObjectInputStream(bis);
			obj = ois.readObject();
		} catch (final IOException ex) {
			// TODO: Handle the exception
		} catch (final ClassNotFoundException ex) {
			// TODO: Handle the exception
		}
		return obj;
	}
}
