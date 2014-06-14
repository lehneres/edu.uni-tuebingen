package de.ut.bioinfo1.assignment4.ilp;

// Name of student(s): Sebastian Lehnerer
//

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.ut.bioinfo1.assignment4.io.FastA;

/**
 * RunILPtoAlignment. Bioinformatics I, WS 12/13 WSI-Informatik, Universitaet
 * Tuebingen, 2012, Daniel Huson
 */

public class RunILPtoAlignment {
	/**
	 * This program reads as input two DNA/RNA sequences seq1 and seq2 from two
	 * different files, and the output of lp_solve. The program extracts the
	 * trace from the output of lp_solve and reports it. Additionally, it prints
	 * the alignment
	 * 
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings ("resource")
	public static void main(String[] args) throws Exception {
		System.err.println("Author:" + ILPtoAlignment.AUTHOR);
		
		System.err.println("RunILPtoAlignment: produces an alignment from the solution of an ILP");
		// Check usage:
		if (args.length != 3) {
			System.err.println("Usage: RunILPtoAlignment infile1 infile2 crunch");
			System.exit(1);
		}
		
		// Read the two sequences:
		final FastA sf1 = new FastA();
		sf1.read(new FileReader(new File(args[0])));
		final String seq1 = sf1.getFirstSequence();
		System.err.println("Read " + args[0] + ": " + seq1.length());
		
		final FastA sf2 = new FastA();
		sf2.read(new FileReader(new File(args[1])));
		final String seq2 = sf2.getFirstSequence();
		System.err.println("Read " + args[1] + ": " + seq2.length());
		
		final ILPtoAlignment ilp2align = new ILPtoAlignment(seq1, seq2);
		
		// parse the crunch file
		ilp2align.parseCrunchFile(new BufferedReader(new FileReader(new File(args[2]))));
		
		// print sequences:
		System.out.println("Seq1  : " + ilp2align.getSeq1());
		System.out.println("Seq2  : " + ilp2align.getSeq2() + "\n");
		
		// print alignment:
		System.out.println("Align1: " + ilp2align.getAlign1());
		System.out.println("Align2: " + ilp2align.getAlign2());
		System.out.println("score : " + ilp2align.getOptimalScore());
	}
	
}
// EOF
