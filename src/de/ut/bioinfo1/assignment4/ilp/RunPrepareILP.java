package de.ut.bioinfo1.assignment4.ilp;

// Name of student(s): Sebastian Lehnerer
//

import java.io.File;
import java.io.FileReader;

import de.ut.bioinfo1.assignment4.io.FastA;

/**
 * RunPrepareILP. ioinformatics I, WS 12/13 WSI-Informatik, Universitaet
 * Tuebingen, 2012, Daniel Huson
 */

public class RunPrepareILP {
	/**
	 * This program reads as input two DNA/RNA sequences xseq and yseq from two
	 * different files. Moreover, it expects two additional arguments, the match
	 * and mismatch score. The program writes out a ILP for computing an
	 * alignment of the two sequences that can be read by lp_solve
	 * 
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings ("resource")
	public static void main(String[] args) throws Exception {
		System.err.println("Author:" + PrepareILP.AUTHOR);
		
		System.err.println("RunPrepareILP: generates an ILP for the pairwise alignment problem ");
		// Check usage:
		if (args.length != 4) {
			System.err.println("Usage: RunPrepareILP infile1 infile2 s m");
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
		
		// Get the match score:
		final int matchScore = Integer.valueOf(args[2]);
		
		// Get the mismatch score:
		final int mismatchScore = Integer.valueOf(args[3]);
		
		System.err.print("Match score: " + matchScore);
		System.err.println(", mismatch score: " + mismatchScore);
		
		final PrepareILP prepareILP = new PrepareILP(seq1, seq2);
		prepareILP.setMatchScore(matchScore);
		prepareILP.setMismatchScore(mismatchScore);
		
		prepareILP.print(System.out);
	}
}
// EOF
