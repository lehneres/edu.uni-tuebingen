package de.ut.bioinformatics2.assignment6;

import java.util.Set;

/**
 * a simple node
 * 
 * @author LehnereS
 * 
 */
public class Node {
	
	/**
	 * a set of neigbours
	 */
	public Set<Node>	neigbours;
	private String		name;
	private String		organism	= "human";
	private int			id;
	
	/**
	 * @param id
	 *            obligatoric id
	 */
	public Node(int id) {
		this.setId(id);
	}

	/**
	 * 
	 * @return the name of the node
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @param name
	 *            set the name of the node
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the organism
	 */
	public String getOrganism() {
		return organism;
	}

	/**
	 * @param organism the organism to set
	 */
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	
	@Override
	public String toString() {
		return name + "(" + neigbours.size() + ")";
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

}
