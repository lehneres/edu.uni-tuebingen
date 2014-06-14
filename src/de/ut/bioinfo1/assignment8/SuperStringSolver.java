/**
 * 
 */
package de.ut.bioinfo1.assignment8;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import de.ut.bioinfo1.assignment5MS.bioinf1.io.FastA;

/**
 * @author LehnereS
 */
public class SuperStringSolver {
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings ("resource")
	public static void main(String[] args) throws FileNotFoundException, IOException {
		final FastA sequences = new FastA();
		sequences.read(new FileReader(new File(args[0])));
		
		final int start = 0;
		// new Random().nextInt(sequences.getSize());
		
		HashSet<String> reads = new HashSet<String>();
		for (int i = 0; i < sequences.getSize(); i++)
			if (i != start) reads.add(sequences.getSequence(i));
		
		String result = sequences.getSequence(start);
		
		while (!reads.isEmpty()) {
			
			int leftmax = Integer.MIN_VALUE, rightmax = Integer.MIN_VALUE;
			String leftseq = null, rightseq = null;
			String skip = null;
			
			for (final String read : reads) {
				
				if (result.contains(read)) skip = read;
				
				final int leftscore = overlap(read, result);
				final int rightscore = overlap(result, read);
				
				if (leftscore > leftmax) {
					leftmax = leftscore;
					leftseq = read;
				}
				
				if (rightscore > rightmax) {
					rightmax = rightscore;
					rightseq = read;
				}
			}
			
			if (skip != null) reads.remove(skip);
			else if (leftmax > rightmax) {
				result = merge(leftseq, result);
				reads.remove(leftseq);
			} else {
				result = merge(result, rightseq);
				reads.remove(rightseq);
			}
		}
		
		reads = new HashSet<String>();
		for (int i = 0; i < sequences.getSize(); i++)
			reads.add(sequences.getSequence(i));
		
		System.err.println(result);
		printSSP(reads, result);
	}
	
	private static String merge(String sequence, String sequence2) {
		return sequence.concat(sequence2.substring(overlap(sequence, sequence2)));
	}
	
	private static int overlap(String sequence1, String sequence2) {
		for (int i = sequence2.length(); i > 0; i--)
			if (sequence1.endsWith(sequence2.substring(0, i))) return i;
		return 0;
	}
	
	private static void printSSP(HashSet<String> reads, String string) {
		for (final String read : reads) {
			final int start = string.indexOf(read);
			if (start == -1) {
				System.err.println("not found!");
				System.exit(666);
			}
			final int end = start + read.length();
			for (int i = 0; i < start; i++)
				System.out.print("-");
			System.out.print(read);
			for (int j = end; j < string.length(); j++)
				if (j == string.length() - 1) System.out.println("-");
				else System.out.print("-");
			if (end == string.length()) System.out.println();
		}
	}
	
}