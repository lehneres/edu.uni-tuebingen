package de.ut.bioinfo1.assignment5MS.bioinf1.suffixtrees;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ut.bioinfo1.assignment5MS.bioinf1.io.FastA;

/**
 * @author Manuel Ruff
 * @author Sonja Hägele Naive implementation of a suffix tree
 */
public class NaiveSuffixTree {
	
	private static String	AUTHORS	= "Manuel Ruff & Sonja Hägele"; // names of
																	// the
																	// authors
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		// print authors of this amazing program
		System.err.println("Authors: " + NaiveSuffixTree.AUTHORS);
		
		// print what this amazing program is doin
		System.err
				.println("NaiveSuffixTree: generates an naive suffix tree for a word and searches for querries in it\n");
		
		// check usage:
		if (args.length != 2) {
			System.err.println("Usage: NaiveSuffixTree infile1 infile2");
			System.exit(1);
		}
		
		// read in the word from which the suffixtree is constructed and the
		// querries
		final FastA fasta1 = new FastA();
		fasta1.read(new FileReader(new File(args[0])));
		final String word = fasta1.getFirstSequence();
		System.err.println("Word: " + word.toLowerCase() + "\n");
		
		final FastA fasta2 = new FastA();
		fasta2.read(new FileReader(new File(args[1])));
		
		// compute the tree and print it if u want
		final NaiveSuffixTree tree = new NaiveSuffixTree(word);
		// tree.printTree(tree.root, 0);
		
		// for every querry to be searched print it with occurences in the text
		// separated by kommas
		System.err.println("Querries: " + fasta2.getSize());
		for (int i = 0; i < fasta2.getSize(); i++) {
			final String querry = fasta2.getSequence(i);
			final List<Integer> results = tree.searchQuerry(querry);
			Collections.sort(results);
			System.err.print(querry + ": ");
			if (results.isEmpty()) System.err.println("not found");
			else for (int j = 0; j < results.size(); j++)
				if (j == results.size() - 1) System.err.println(results.get(j));
				else System.err.print(results.get(j) + ",");
			
		}
	}
	
	private final NaiveSuffixTreeNode	root	= new NaiveSuffixTreeNode("");	// root
																				// node
																				// of
																				// the
																				// suffix
																				// tree
																				
	/**
	 * Constructor
	 * 
	 * @param s
	 *            {@link String}
	 */
	public NaiveSuffixTree(String s) {
		// check correctness of input
		if (!s.endsWith("$")) s += "$";
		
		// insert all suffixes in the tree (in lower case)
		for (int i = 0; i < s.length(); i++)
			searchAndInsertSuffix(s.substring(i).toLowerCase(), root, i);
	}
	
	/**
	 * Determines the Longest Common Prefix
	 * 
	 * @param s
	 *            {@link String}
	 * @param t
	 *            {@link String}
	 * @return {@link Integer}
	 */
	public int determineLCP(String s, String t) {
		int lcp = 0;
		// check for every char if they are equal
		for (int i = 0; i < Math.min(s.length(), t.length()); i++)
			if (s.charAt(i) == t.charAt(i)) lcp++;
			else break;
		return lcp;
	}
	
	public NaiveSuffixTreeNode getRoot() {
		return root;
	}
	
	/**
	 * Prints out the resulting suffix tree in a really simple way (just for
	 * debugging)
	 * 
	 * @param n
	 *            {@link NaiveSuffixTreeNode} (should always be the root node)
	 * @param depth
	 *            {@link Integer} (should always be 0)
	 */
	public void printTree(NaiveSuffixTreeNode n, int depth) {
		String minus = "";
		for (int i = 0; i < depth; i++)
			minus += "-";
		
		if (depth == 0) System.out.println("root" + "(" + n.getPosition() + ")");
		else System.out.println("|" + minus + " " + n.getIncomingEdgeLabel() + "(" + n.getPosition() + ")");
		
		if (n.hasChildren()) {
			depth++;
			for (final NaiveSuffixTreeNode currentNode : n.getChildren())
				printTree(currentNode, depth);
		}
	}
	
	/**
	 * search and insert a string below a suffix tree node
	 * 
	 * @param s
	 *            {@link String}
	 * @param node
	 *            {@link NaiveSuffixTreeNode}
	 */
	public void searchAndInsertSuffix(String s, NaiveSuffixTreeNode node, int position) {
		boolean inserted = false; // check if the string is inserted after
									// looking through all children
		
		if (node.hasChildren())
		// visit all children
			for (final NaiveSuffixTreeNode currentNode : node.getChildren()) {
				final int lcp = determineLCP(s, currentNode.getIncomingEdgeLabel()); // compute
																						// the
																						// longest
																						// common
																						// prefix
				
				// if there is no lcp skip to the dip
				if (lcp == 0) continue;
				
				// if the lcp equals the length of the current node
				if (lcp == currentNode.getIncomingEdgeLabel().length()) {
					// visit the children of the current node with the leftover
					// string
					searchAndInsertSuffix(s.substring(lcp), currentNode, position);
					inserted = true;
					break;
				}
				
				// if the lcp is smaller as the current node
				if (lcp < currentNode.getIncomingEdgeLabel().length()) {
					
					// initialize and get the former values
					final String oldLabel = currentNode.getIncomingEdgeLabel();
					final String newLabel = oldLabel.substring(0, lcp);
					final String leftOverLabel = oldLabel.substring(lcp);
					final int oldPosition = currentNode.getPosition();
					final List<NaiveSuffixTreeNode> formerChildren = currentNode.getChildren();
					final List<NaiveSuffixTreeNode> newChildren = new ArrayList<NaiveSuffixTreeNode>();
					final NaiveSuffixTreeNode leftNode = new NaiveSuffixTreeNode(leftOverLabel);
					final NaiveSuffixTreeNode rightNode = new NaiveSuffixTreeNode(s.substring(lcp));
					
					// modify the current node
					currentNode.setIncomingEdgeLabel(newLabel);
					currentNode.setPosition(-1);
					
					// modify the new childs
					leftNode.setPosition(oldPosition);
					leftNode.setChildren(formerChildren);
					rightNode.setPosition(position);
					newChildren.add(leftNode);
					newChildren.add(rightNode);
					
					// add them
					currentNode.setChildren(newChildren);
					
					// mark it as inserted
					inserted = true;
					break;
				}
			}
		
		// if there werent any insertions than add the suffix to the node
		if (!inserted) node.extend(s, position);
	}
	
	/**
	 * Helper function to search a querry in the tree
	 * 
	 * @param s
	 *            {@link String} (querry to be searched)
	 * @param node
	 *            {@link NaiveSuffixTreeNode} (mainly the root node)
	 * @param results
	 *            {@link List<Integer>} (the results are stored here, mainly an
	 *            empy list)
	 * @return {@link List<Integer} of occurences in the text/tree
	 */
	public List<Integer> searchHelper(String s, NaiveSuffixTreeNode node, List<Integer> results) {
		
		// if there are children we can look into it
		if (node.hasChildren())
		// so for every child
			for (final NaiveSuffixTreeNode currentNode : node.getChildren())
				// if we already found the complete string we just search for
				// the children
				if (s.equalsIgnoreCase("")) {
					if (!currentNode.hasChildren()) results.add(currentNode.getPosition());
					else searchHelper(s, currentNode, results);
				} else {
					// else we look through the children if there is a match
					final int lcp = determineLCP(s, currentNode.getIncomingEdgeLabel()); // compute
																							// the
																							// longest
																							// common
																							// prefix
					
					if (lcp == 0) continue;
					
					if (lcp == currentNode.getIncomingEdgeLabel().length())
						searchHelper(s.substring(lcp), currentNode, results);
					
					if (lcp == s.length() && currentNode.getPosition() != -1) results.add(currentNode.getPosition());
				}
		
		return results;
	}
	
	/**
	 * Searches a querry in the tree
	 * 
	 * @param s
	 *            {@link String}
	 * @return {@link List<Integer>} of occurences in the text/tree
	 */
	public List<Integer> searchQuerry(String s) {
		final NaiveSuffixTreeNode node = root;
		return searchHelper(s, node, new ArrayList<Integer>());
	}
	
}
