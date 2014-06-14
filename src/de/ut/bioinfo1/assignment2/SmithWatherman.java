package de.ut.bioinfo1.assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Implementation of the Needleman-Wunsch algorithm for DNA Bioinformatics I,
 * Prof. Daniel Huson, WS 2012/13
 * 
 * @author Sebastian Lehnerer (sebastian.lehnerer@student.uni-tuebingen.de)
 */
public class SmithWatherman {
	
	/**
	 * Run the algorithm and print the alignment. Command line syntax should be:
	 * filename1 filename2 For assignment 2, add a third parameter "linear" to
	 * turn on your linear space algorithm
	 * 
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(final String[] args) throws FileNotFoundException, IOException {
		
		if (args.length == 0) {
			System.err.println("input file missing");
			return;
		}
		
		final String x = readFASTASequence(args[0], "X");
		final String y = readFASTASequence(args[0], "Y");
		
		final SmithWatherman nm = new SmithWatherman(x, y);
		nm.computeAndPrintAlignment();
		
	}
	
	@SuppressWarnings ({ "resource", "null" })
	private static String readFASTASequence(String file, String id) throws FileNotFoundException, IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		String line;
		do {
			line = reader.readLine();
			if (line.startsWith(">" + id)) {
				final String seq = reader.readLine();
				reader.close();
				return seq;
			}
		} while (line != null);
		reader.close();
		return null;
	}
	
	// please use the following variables for the algorithm:
	private final int	matchScore		= 1;
	
	private final int	mismatchScore	= -1;
	private final int	gapPenalty		= 2;
	
	int[][]				matrix;
	
	char[]				x, y;
	
	/**
	 * constructor
	 * 
	 * @param x
	 *            first sequence
	 * @param y
	 *            second sequence
	 */
	public SmithWatherman(final String x, final String y) {
		
		matrix = new int[x.length() + 1][y.length() + 1];
		
		this.x = x.toCharArray();
		this.y = y.toCharArray();
		
		// initialize matrix
		
		for (int i = 0; i < matrix.length; i++)
			matrix[i][0] = 0;
		
		for (int j = 0; j < matrix[0].length; j++)
			matrix[0][j] = 0;
		
	}
	
	/**
	 * compute and then print the alignment score and alignment for two DNA
	 * sequences
	 */
	public void computeAndPrintAlignment() {
		
		int max = Integer.MIN_VALUE;
		int maxI = 0, maxJ = 0;
		
		// fill matrix
		for (int i = 1; i < matrix.length; i++)
			for (int j = 1; j < matrix[i].length; j++) {
				final int top = matrix[i - 1][j] - gapPenalty;
				final int left = matrix[i][j - 1] - gapPenalty;
				final int match = matrix[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? matchScore : mismatchScore);
				
				matrix[i][j] = Math.max(Math.max(Math.max(top, left), match), 0);
				
				if (matrix[i][j] > max) {
					maxI = i;
					maxJ = j;
					max = matrix[i][j];
				}
			}
		
		printMatrix();
		
		// traceback
		
		String xAl = "", yAl = "";
		int i = maxI;
		int j = maxJ;
		
		for (int d = matrix.length - 1 - i; d > 0; d--) {
			xAl = x[d] + xAl;
			yAl = "-" + yAl;
		}
		
		for (int d = matrix[i].length - 1 - i; d > 0; d--) {
			yAl = y[d] + yAl;
			xAl = "-" + xAl;
		}
		
		while (i > 0 && j > 0)
			if (matrix[i][j] == 0) break;
			else if (matrix[i][j] == matrix[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? matchScore : mismatchScore)) {
				xAl = x[--i] + xAl;
				yAl = y[--j] + yAl;
			} else if (matrix[i][j] == matrix[i - 1][j] - gapPenalty) {
				xAl = x[--i] + xAl;
				yAl = "-" + yAl;
			} else if (matrix[i][j] == matrix[i][j - 1] - gapPenalty) {
				xAl = "-" + xAl;
				yAl = y[--j] + yAl;
			}
		
		while (i > 0) {
			xAl = x[--i] + xAl;
			yAl = "-" + yAl;
		}
		
		while (j > 0) {
			xAl = "-" + xAl;
			yAl = y[--j] + yAl;
		}
		
		System.out.println(xAl);
		System.out.println(yAl);
	}
	
	private void printMatrix() {
		for (final int[] element : matrix) {
			for (final int element2 : element)
				System.out.print(element2 + "\t");
			System.out.println("");
		}
		System.out.println("");
	}
}
