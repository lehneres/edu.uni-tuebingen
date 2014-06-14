package de.ut.bioinformatics2.assignment6;

/**
 * a simple edge
 * 
 * @author LehnereS
 * 
 */
public class Edge {
	
	private Node	A;
	
	private Node	B;
	
	/**
	 * @param a2
	 *            node 1
	 * @param b2
	 *            node 2
	 */
	public Edge(Node a2, Node b2) {
		setA(a2);
		setB(b2);
	}
	
	/**
	 * @return node 1
	 */
	public Node getA() {
		return A;
	}
	
	/**
	 * @return node 2
	 */
	public Node getB() {
		return B;
	}
	
	/**
	 * @param a
	 *            a node to set
	 */
	public void setA(Node a) {
		A = a;
	}
	
	/**
	 * @param b
	 *            a node to set
	 */
	public void setB(Node b) {
		B = b;
	}
	
}
