package de.ut.bioinfo1.assignment5MS.bioinf1.suffixtrees;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.ut.bioinfo1.assignment5MS.bioinf1.helper.MUM;
import de.ut.bioinfo1.assignment5MS.bioinf1.io.FastA;

/**
 * @author Manuel Ruff
 * @author Sonja Hägele Computes all Maximum Unique Matches
 */
public class NaiveMUMMER {
	
	private static String	AUTHORS	= "Manuel Ruff & Sonja Hägele"; // names of
																	// the
																	// authors
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// print authors of this amazing program
		System.err.println("Authors: " + NaiveMUMMER.AUTHORS);
		
		// print what this amazing program is doin
		System.err.println("NaiveMUMMER: generates an naive suffix tree and searches for MUMs\n");
		
		// check usage:
		if (args.length != 3) {
			System.err.println("Usage: NaiveMUMMER infile1 infile2 k");
			System.exit(1);
		}
		
		// read in the words s and t from the fasta files
		final FastA fasta1 = new FastA();
		fasta1.read(new FileReader(new File(args[0])));
		final String s = fasta1.getFirstSequence();
		
		final FastA fasta2 = new FastA();
		fasta2.read(new FileReader(new File(args[1])));
		final String t = fasta2.getFirstSequence();
		
		// print 'em
		System.err.println("s: " + s.toLowerCase());
		System.err.println("t: " + t.toLowerCase());
		
		// create a NaiveMUMMER instance to do the math
		final NaiveMUMMER mummer = new NaiveMUMMER(s, t, Integer.parseInt(args[2]));
		// compute the MUMs
		final List<MUM> results = mummer.searchMUMs();
		
		// mummer.tree.printTree(mummer.tree.getRoot(), 0);
		
		// print 'em
		System.err.println("MUMs:");
		for (final MUM mum : results)
			System.err.println(mum.getSequence() + " " + mum.getLength() + ", " + mum.getPositionS() + ", "
					+ mum.getPositionT());
	}
	
	private final NaiveSuffixTree	tree;	// the suffix tree
	private final String			s;		// the first string
	private final String			t;		// the second string
											
	private final int				k;		// minimum length of the MUMs
											
	/**
	 * Constructor
	 * 
	 * @param s
	 *            {@link String}
	 * @param t
	 *            {@link String}
	 * @param k
	 *            {@link Integer}
	 */
	public NaiveMUMMER(String s, String t, int k) {
		// basic stuff
		this.s = s;
		this.t = t;
		// check if user is too stupid to put the right input
		if (s.endsWith("$")) s = s.substring(0, s.length() - 1);
		if (t.endsWith("$")) t = t.substring(0, t.length() - 1);
		String word = s + "%" + t;
		word += "$";
		// create the suffix tree
		tree = new NaiveSuffixTree(word.toLowerCase());
		this.k = k;
	}
	
	/**
	 * Computes all Maximal Unique Matches
	 * 
	 * @return {@link List<MUM>}
	 */
	public List<MUM> searchMUMs() {
		return searchMUMsHelper(tree.getRoot().getIncomingEdgeLabel(), tree.getRoot(), new ArrayList<MUM>());
	}
	
	/**
	 * Helper for computing all Maximal Unqiue Matches
	 * 
	 * @param edgeLabels
	 *            {@link String} (concatination of all edgelabels until this
	 *            point)
	 * @param node
	 *            {@link NaiveSuffixTreeNode} (current node to look at)
	 * @param results
	 *            {@link List<MUM>} (current list of the result which contains
	 *            all MUMs found so far)
	 * @return {@link List<MUM>} all MUMs
	 */
	public List<MUM> searchMUMsHelper(String edgeLabels, NaiveSuffixTreeNode node, List<MUM> results) {
		
		// variable for left maximality
		boolean leftMaximal = false;
		
		// if the current node has childen
		if (node.hasChildren()) // check if there are precisely two of them
			if (node.getChildren().size() == 2) {
				// get them!
				final NaiveSuffixTreeNode firstChild = node.getChildren().get(0);
				final NaiveSuffixTreeNode secondChild = node.getChildren().get(1);
				// check if they are both leafs
				if (!firstChild.hasChildren() && !secondChild.hasChildren()) {
					// check if the concatination of the labels is >= k and if
					// one of the children contains "%" so they are unique in
					// botch sequences
					if ((edgeLabels + node.getIncomingEdgeLabel()).length() >= k
							&& firstChild.getIncomingEdgeLabel().contains("%")
							^ secondChild.getIncomingEdgeLabel().contains("%")) {
						// compute the positions in s and t
						final int positionS = firstChild.getPosition();
						final int positionT = secondChild.getPosition() - s.length() - 1;
						// check for left maximality
						if (positionS == 0 || positionT == 0) leftMaximal = true;
						if (positionS > 0 && positionT > 0)
							if (s.charAt(positionS - 1) != t.charAt(positionT - 1)) leftMaximal = true;
						
						// insert
						if (leftMaximal) {
							final MUM mum = new MUM(edgeLabels, edgeLabels.length(), positionS, positionT);
							results.add(mum);
						}
					}
				} else // look for the children when they are not leafs
				for (final NaiveSuffixTreeNode currentNode : node.getChildren())
					searchMUMsHelper(edgeLabels + currentNode.getIncomingEdgeLabel(), currentNode, results);
			} else
			// if there are more than 2 children look at them!
			for (final NaiveSuffixTreeNode currentNode : node.getChildren())
				searchMUMsHelper(edgeLabels + currentNode.getIncomingEdgeLabel(), currentNode, results);
		
		// gimme!
		return results;
	}
	
}
