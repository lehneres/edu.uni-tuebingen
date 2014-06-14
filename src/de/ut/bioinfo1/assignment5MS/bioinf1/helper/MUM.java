package de.ut.bioinfo1.assignment5MS.bioinf1.helper;

/**
 * This represents a Maximal Unique Match
 * 
 * @author Manuel Ruff
 * @author Sonja Hägele
 */
public class MUM {
	
	private final String	sequence;	// the maximal unqiue match
	private final int		length;	// the length of the sequence
	private final int		positionS;	// the position in s
	private final int		positionT;	// the position in t
										
	/**
	 * Constructor
	 * 
	 * @param sequence
	 *            {@link String} maximal unique match
	 * @param length
	 *            {@link Integer} length of the mum
	 * @param positionS
	 *            {@link Integer} position in s
	 * @param positionT
	 *            {@link Integer} position in t
	 */
	public MUM(String sequence, int length, int positionS, int positionT) {
		this.sequence = sequence;
		this.length = length;
		this.positionS = positionS;
		this.positionT = positionT;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getPositionS() {
		return positionS;
	}
	
	public int getPositionT() {
		return positionT;
	}
	
	// /////////////////////////
	// Getters and Setters //
	// ///////////////////////
	public String getSequence() {
		return sequence;
	}
	
}
