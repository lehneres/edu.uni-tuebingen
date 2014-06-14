package de.ut.bioinfo1.assignment8MS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.ut.bioinfo1.assignment8.FastA;

/**
 * Computes the shortest superstring out of reads
 */
public class SuperStringSolver {
	
	private static String	AUTHORS	= "Manuel Ruff & Sonja Hägele"; // names of
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		// print authors of this amazing program
		System.err.println("Authors: " + SuperStringSolver.AUTHORS);
		
		// print what this amazing program is doin
		System.err.println("SuperStringSolver: solves the shortest superstring problem with a greedy heuristic\n");
		
		// check usage:
		if (args.length != 1) {
			System.err.println("Usage: SuperStringSolver infile1");
			System.exit(1);
		}
		
		// read in the reads
		final FastA fasta = new FastA();
		fasta.read(new FileReader(new File(args[0])));
		
		// collect all the reads
		final List<String> reads = new ArrayList<String>();
		for (int i = 0; i < fasta.getSize(); i++)
			reads.add(fasta.getSequence(i));
		
		final SuperStringSolver solver = new SuperStringSolver(reads);
		solver.computeShortestSuperstring();
		System.out.println(solver.getSuperString());
		solver.printReadsWithGaps();
		
		// System.out.println(read1.contains(read2));
		// System.out.println(read1.startsWith(read2));
	}
	
	// the
	// authors
	private final List<String>	reads;			// list
												
	// containing
	// all reads
	private String				superString;	// the
												// resulting
												// shortest
												// superstring
												
	/**
	 * Constructor
	 * 
	 * @param reads
	 *            {@link List<String>} containng all the reads
	 */
	public SuperStringSolver(List<String> reads) {
		this.reads = reads;
	}
	
	/**
	 * Compute the shortest superstrning
	 */
	private void computeShortestSuperstring() {
		
		// initialize (globally)
		final List<String> temporaryReads = new ArrayList<String>(reads);
		superString = temporaryReads.get(0);
		temporaryReads.remove(0);
		
		// as long as we got reads
		while (!temporaryReads.isEmpty()) {
			
			// initialize (locally)
			int bestScore = -1; // biggest overlap or biggest score
			int bestIndex = -1; // index of the best read in the array
			boolean leftAligned = false; // aligned the best read right or left
			boolean partOfSequence = false; // is the complete read part of our
											// superstring
			
			// for every read
			for (int j = 0; j < temporaryReads.size(); j++) {
				final String read = temporaryReads.get(j); // get the read
				
				// get the length
				final int readLength = read.length();
				
				// if the complete read is part of our superstring than we are
				// done for this one
				if (superString.contains(read)) {
					bestScore = readLength;
					bestIndex = j;
					partOfSequence = true;
					break;
				}
				
				// check the overlap of both sides
				for (int i = 0; i < readLength; i++) {
					
					int currentScore = -1; // currently the best score (compared
											// to bestScore which is globally)
					
					// left alignment
					if (superString.startsWith(read.substring(i, readLength))) {
						currentScore = readLength - i;
						if (currentScore > bestScore) {
							bestScore = currentScore;
							bestIndex = j;
							leftAligned = true;
							break;
						}
					}
					
					// right alignment
					if (superString.endsWith(read.substring(0, readLength - i))) {
						currentScore = readLength - i;
						if (currentScore > bestScore) {
							bestScore = currentScore;
							bestIndex = j;
							leftAligned = false;
							break;
						}
					}
				}
				
			}
			
			// align them
			if (bestScore != -1 && bestIndex != -1) {
				String bestRead = temporaryReads.get(bestIndex); // get the best
																	// read
				
				// check if it was completely in our superstring
				if (partOfSequence) temporaryReads.remove(bestIndex);
				else {
					// if not we align it left or right
					if (leftAligned) {
						bestRead += superString.substring(bestScore, superString.length());
						superString = bestRead;
					} else superString += bestRead.substring(bestScore, bestRead.length());
					// remove it afterwards (because its now part of our
					// superstring
					temporaryReads.remove(bestIndex);
				}
			}
		}
		
	}
	
	/**
	 * Getter for the superstring
	 * 
	 * @return {@link SuperStringSolver#superString} which is the shortest
	 *         superstring out of all reads
	 */
	public String getSuperString() {
		return superString;
	}
	
	/**
	 * Prints out the reads filled with gaps to see where they appear in our
	 * superstring
	 */
	private void printReadsWithGaps() {
		// for every read
		for (final String read : reads) {
			// get start end end position
			final int startPosition = superString.indexOf(read);
			final int endPosition = startPosition + read.length();
			// print the dashes in front
			for (int i = 0; i < startPosition; i++)
				System.out.print("-");
			// print the read
			System.out.print(read);
			// print the dashes at the end
			for (int j = endPosition; j < superString.length(); j++)
				if (j == superString.length() - 1) System.out.println("-");
				else System.out.print("-");
			// check if a read ends at the end so we need to make a new line
			if (endPosition == superString.length()) System.out.println();
		}
	}
	
}
