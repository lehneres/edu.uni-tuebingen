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
public class NeedlemanWunsch {
	
	// please use the following variables for the algorithm:
	private final int	matchScore		= 1;
	private final int	mismatchScore	= -1;
	private final int	gapPenalty		= 2;
	
	int[][]				matrix;
	char[]				x, y;
	
	static boolean		linear			= false;
	
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
		
		if (args.length > 1) {
			final String lin = args[1];
			linear = lin.equals("linear");
		}
		
		final NeedlemanWunsch nm = new NeedlemanWunsch(x, y);
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
	
	/**
	 * constructor
	 * 
	 * @param x
	 *            first sequence
	 * @param y
	 *            second sequence
	 */
	public NeedlemanWunsch(final String x, final String y) {
		
		matrix = new int[x.length() + 1][y.length() + 1];
		
		this.x = x.toCharArray();
		this.y = y.toCharArray();
		
		// initialize matrix
		
		for (int i = 0; i < matrix.length; i++)
			matrix[i][0] = -1 * i * gapPenalty;
		
		for (int j = 0; j < matrix[0].length; j++)
			matrix[0][j] = -1 * j * gapPenalty;
		
	}
	
	/**
	 * compute and then print the alignment score and alignment for two DNA
	 * sequences
	 */
	public void computeAndPrintAlignment() {
		
		if (linear) {
			computeAndPrintLinear();
			return;
		}
		
		// fill matrix
		for (int i = 1; i < matrix.length; i++)
			for (int j = 1; j < matrix[i].length; j++) {
				final int top = matrix[i - 1][j] - gapPenalty;
				final int left = matrix[i][j - 1] - gapPenalty;
				final int match = matrix[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? matchScore : mismatchScore);
				
				matrix[i][j] = Math.max(Math.max(top, left), match);
			}
		
		printMatrix();
		
		// traceback
		
		String xAl = "", yAl = "";
		int i = matrix.length - 1;
		int j = matrix[i].length - 1;
		
		while (i > 0 && j > 0)
			if (matrix[i][j] == matrix[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? matchScore : mismatchScore)) {
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
	
	/**
	 * 
	 */
	private void computeAndPrintLinear() {
		
		matrix = new int[x.length + 1][2];
		
		for (int i = 0; i < matrix.length; i++)
			matrix[i][0] = -1 * i * gapPenalty;
		
		// fill matrix
		for (int i = 1; i < y.length + 1; i++) {
			matrix[0][1] = -i * gapPenalty;
			for (int j = 1; j < x.length + 1; j++) {
				final int top = matrix[j - 1][1] - gapPenalty;
				final int left = matrix[j][0] - gapPenalty;
				final int match = matrix[j - 1][0] + (x[j - 1] == y[i - 1] ? matchScore : mismatchScore);
				
				matrix[j][1] = Math.max(Math.max(top, left), match);
			}
			// printMatrix();
			matrix = shiftColumn();
		}
		
		System.out.println("highest score " + matrix[matrix.length - 1][0]);
	}
	
	private void printMatrix() {
		for (final int[] element : matrix) {
			for (final int element2 : element)
				System.out.print(element2 + "\t");
			System.out.println("");
		}
		System.out.println("");
	}
	
	private int[][] shiftColumn() {
		final int[][] nMatrix = new int[matrix.length][2];
		
		for (int i = 0; i < matrix.length; i++)
			nMatrix[i][0] = matrix[i][1];
		
		return nMatrix;
	}
}
