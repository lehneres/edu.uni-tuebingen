package de.ut.bioinfo1.assignment5MS.bioinf1.suffixtrees;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Manuel Ruff
 * @author Sonja Hägele This class represents a node in a suffix tree
 */
public class NaiveSuffixTreeNode {
	
	private String						incomingEdgeLabel;	// the label of the
															// edge coming from
															// the previous node
	private List<NaiveSuffixTreeNode>	children;			// the children
															// attached to this
															// edge
	private int							position	= -1;	// the starting
															// position of the
															// suffix in the
															// text (-1 is a
															// internal node, >
															// 0 is a leaf)
															
	/**
	 * Constructor
	 * 
	 * @param s
	 *            {@link String} (the word to create the suffix tree from)
	 */
	public NaiveSuffixTreeNode(String s) {
		incomingEdgeLabel = s;
		children = new ArrayList<NaiveSuffixTreeNode>();
	}
	
	/**
	 * Extend a node by a children
	 * 
	 * @param s
	 *            {@link String} the new label of the string
	 * @param position
	 *            {@link Integer} the position in the text (-1 internal node,
	 *            otherwise the occurence in the text)
	 */
	public void extend(String s, int position) {
		final NaiveSuffixTreeNode node = new NaiveSuffixTreeNode(s);
		node.setPosition(position);
		children.add(node);
	}
	
	public List<NaiveSuffixTreeNode> getChildren() {
		return children;
	}
	
	public String getIncomingEdgeLabel() {
		return incomingEdgeLabel;
	}
	
	public int getPosition() {
		return position;
	}
	
	// /////////////////////////
	// Getters and Setters //
	// ///////////////////////
	public boolean hasChildren() {
		return !children.isEmpty();
	}
	
	public void setChildren(List<NaiveSuffixTreeNode> children) {
		this.children = children;
	}
	
	public void setIncomingEdgeLabel(String incomingEdgeLabel) {
		this.incomingEdgeLabel = incomingEdgeLabel;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
}
