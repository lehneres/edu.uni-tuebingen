package de.ut.bioinfo1.assignment6;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.ut.bioinfo1.assignment5MS.bioinf1.io.FastA;

/**
 * naive suffix tree algorithm
 * 
 * @author LehnereS
 */
public class NaiveSuffixTree {
	
	/**
	 * subclass for tree nodes
	 * 
	 * @author LehnereS
	 */
	public class NaiveSuffixNode {
		
		String					substring;
		int						nodeNr		= -1;
		List<NaiveSuffixNode>	children	= new LinkedList<NaiveSuffixNode>();
		
	}
	
	static Set<Integer> findString(NaiveSuffixNode root, String s) {
		final Set<Integer> result = new HashSet<Integer>();
		
		for (final NaiveSuffixNode node : root.children) {
			if (s.length() == 0) {
				if (node.children.isEmpty()) result.add(node.nodeNr);
				else result.addAll(findString(node, s));
				continue;
			}
			final int ind = maxInd(s, node.substring);
			if (ind == 0) continue;
			if (node.children.isEmpty() && s.substring(ind).length() == 0) {
				result.add(node.nodeNr);
				continue;
			}
			if (node.substring.length() > s.length()) break;
			result.addAll(findString(node, s.substring(ind)));
		}
		
		return result;
	}
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings ("resource")
	public static void main(String[] args) throws FileNotFoundException, IOException {
		final FastA inputSeq = new FastA();
		inputSeq.read(new FileReader(new File(args[0])));
		final String input = inputSeq.getFirstSequence() + "$";
		
		final FastA querys = new FastA();
		querys.read(new FileReader(new File(args[1])));
		
		final NaiveSuffixTree tree = new NaiveSuffixTree();
		tree.buildTree(input);
		printTree(tree.root, 0);
		
		System.err.println("no of queries: " + querys.getSize());
		for (int i = 0; i < querys.getSize(); i++) {
			final String query = querys.getSequence(i);
			final Set<Integer> results = findString(tree.root, query);
			System.err.print(query + ": ");
			if (results.isEmpty()) System.err.println("not found");
			else System.out.println(Arrays.toString(results.toArray()));
		}
		
	}
	
	/**
	 * @param s
	 *            string s
	 * @param t
	 *            string t
	 * @return find the index of the maximum equal prefix of two strings
	 */
	public static int maxInd(String s, String t) {
		int ind = 0;
		for (int i = 0; i < Math.min(s.length(), t.length()); i++)
			if (s.charAt(i) == t.charAt(i)) ind++;
			else break;
		return ind;
	}
	
	static void printTree(NaiveSuffixNode n, int depth) {
		String minus = "";
		for (int i = 0; i < depth; i++)
			minus += "-";
		
		if (depth == 0) System.out.println("root");
		else System.out.println("|" + minus + " " + n.substring + " " + (n.nodeNr > -1 ? n.nodeNr : ""));
		
		if (!n.children.isEmpty()) {
			depth++;
			for (final NaiveSuffixNode currentNode : n.children)
				printTree(currentNode, depth);
		}
	}
	
	NaiveSuffixNode	root	= new NaiveSuffixNode();
	
	void buildTree(String s) {
		
		for (int i = 0; i < s.length(); i++) {
			System.out.println("inserting " + s.substring(i) + "(" + i + ")");
			insertString(root, s.substring(i), i);
			printTree(root, 0);
		}
		
	}
	
	/**
	 * appends a string to a given node
	 * 
	 * @param root
	 * @param substring
	 * @param i
	 */
	public void insertString(@SuppressWarnings ("hiding") NaiveSuffixNode root, String substring, int i) {
		if (root.children.isEmpty()) {
			final NaiveSuffixNode node = new NaiveSuffixNode();
			node.substring = substring;
			node.nodeNr = i;
			root.children.add(node);
		} else {
			
			boolean found = false;
			
			for (final NaiveSuffixNode node : root.children) {
				final int ind = maxInd(node.substring, substring);
				
				if (ind == 0) continue;
				if (node.substring.startsWith(substring)) {
					found = true;
					break;
				}
				
				if (ind == node.substring.length()) {
					insertString(node, substring.substring(node.substring.length()), i);
					found = true;
					break;
				}
				
				final NaiveSuffixNode splitNode = new NaiveSuffixNode();
				splitNode.substring = node.substring.substring(0, ind);
				
				final NaiveSuffixNode newNode = new NaiveSuffixNode();
				newNode.nodeNr = i;
				newNode.substring = substring.substring(ind);
				
				node.substring = node.substring.substring(ind);
				
				root.children.remove(node);
				root.children.add(splitNode);
				splitNode.children.add(newNode);
				splitNode.children.add(node);
				found = true;
				break;
			}
			
			if (!found) {
				final NaiveSuffixNode node = new NaiveSuffixNode();
				node.substring = substring;
				node.nodeNr = i;
				root.children.add(node);
			}
		}
	}
	
}