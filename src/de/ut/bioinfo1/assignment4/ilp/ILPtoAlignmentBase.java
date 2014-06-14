package de.ut.bioinfo1.assignment4.ilp;

/**
 * ILPtoAlignment. Bioinformatics I, WS 12/13 Parses the output of lp_solve and
 * produces an alignment ZBIT, Universitaet Tuebingen, 2012, Daniel Huson
 */

public class ILPtoAlignmentBase {
	/**
	 * Given an edge variable name, returns the position in the first sequence
	 * This assumes the variable names were produced by the nodesToVariable
	 * method in PrepareILP.
	 * 
	 * @param edgeVariable
	 *            the edge variable
	 * @return the position (or "node") in the first sequence
	 */
	public static int xNode(String edgeVariable) {
		final int n = Integer.parseInt(edgeVariable.substring(1).trim());
		return n / 1000 - 1;
	}
	
	/**
	 * Given an edge variable name, returns the position in the first sequence
	 * This assumes the variable names were produced by the nodesToVariable
	 * method in PrepareILP.
	 * 
	 * @param edgeVariable
	 *            the edge variable
	 * @return the position (or "node") in the first sequence
	 */
	public static int yNode(String edgeVariable) {
		final int n = Integer.parseInt(edgeVariable.substring(1).trim());
		return n % 1000 - 1;
	}
	
	private String	seq1;
	private String	seq2;
	private String	align1;
	
	private String	align2;
	
	private int		OptimalScore;
	
	/**
	 * construct ILP to alignment converter for two given sequences
	 * 
	 * @param seq1
	 * @param seq2
	 */
	public ILPtoAlignmentBase(String seq1, String seq2) {
		this.seq1 = seq1;
		this.seq2 = seq2;
		align1 = "";
		align2 = "";
	}
	
	/**
	 * @return the aligned sequence one
	 */
	public String getAlign1() {
		return align1;
	}
	
	/**
	 * @return the aligned sequence two
	 */
	public String getAlign2() {
		return align2;
	}
	
	/**
	 * @return the optimal score of the alignment
	 */
	public int getOptimalScore() {
		return OptimalScore;
	}
	
	/**
	 * @return sequence one
	 */
	public String getSeq1() {
		return seq1;
	}
	
	/**
	 * @return sequence two
	 */
	public String getSeq2() {
		return seq2;
	}
	
	/**
	 * sets the aligned sequence one
	 * 
	 * @param align1
	 */
	public void setAlign1(String align1) {
		this.align1 = align1;
	}
	
	/**
	 * sets the aligned sequence two
	 * 
	 * @param align2
	 */
	public void setAlign2(String align2) {
		this.align2 = align2;
	}
	
	/**
	 * sets the optimal score of the alignment
	 * 
	 * @param optimalScore
	 */
	public void setOptimalScore(int optimalScore) {
		OptimalScore = optimalScore;
	}
	
	/**
	 * sets sequence one
	 * 
	 * @param seq1
	 */
	public void setSeq1(String seq1) {
		this.seq1 = seq1;
	}
	
	/**
	 * sets sequence two
	 * 
	 * @param seq2
	 */
	public void setSeq2(String seq2) {
		this.seq2 = seq2;
	}
}
// EOF
