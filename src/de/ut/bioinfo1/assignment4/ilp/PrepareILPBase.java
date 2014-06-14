package de.ut.bioinfo1.assignment4.ilp;

/**
 * PrepareILPBase. Bioinformatics I, WS 12/13 Formulates and prints pairwise
 * alignment problem as ILP WSI-Informatik, Universitaet Tuebingen, 2012, Daniel
 * Huson
 */
public class PrepareILPBase {
	/**
	 * Given two nodes, ie positions in the two sequences, returns the name of
	 * the corresponding edge variable
	 * 
	 * @param i
	 *            the position in the first sequence
	 * @param j
	 *            the position in the second sequence
	 * @return the name of the corresponding edge variable
	 * @throws Exception
	 */
	public static String nodesToVariable(int i, int j) throws Exception {
		if (j >= 1000) throw new Exception("j too big: " + j);
		return "x" + (1000 * (i + 1) + j + 1);
	}
	
	private String	seq1;
	private String	seq2;
	private int		matchScore		= 1;
	
	private int		mismatchScore	= -1;
	
	/**
	 * constructor for ILP alignment of two sequences
	 * 
	 * @param seq1
	 *            first sequence
	 * @param seq2
	 *            second sequence
	 */
	public PrepareILPBase(String seq1, String seq2) {
		this.seq1 = seq1;
		this.seq2 = seq2;
	}
	
	/**
	 * get match score
	 * 
	 * @return match score
	 */
	public int getMatchScore() {
		return matchScore;
	}
	
	/**
	 * get mismatch score
	 * 
	 * @return mismatch score
	 */
	public int getMismatchScore() {
		return mismatchScore;
	}
	
	/**
	 * get first sequence
	 * 
	 * @return first sequence
	 */
	public String getSeq1() {
		return seq1;
	}
	
	/**
	 * get second sequence
	 * 
	 * @return second sequence
	 */
	public String getSeq2() {
		return seq2;
	}
	
	/**
	 * set match score
	 * 
	 * @param matchScore
	 */
	public void setMatchScore(int matchScore) {
		this.matchScore = matchScore;
	}
	
	/**
	 * set mismatch score
	 * 
	 * @param mismatchScore
	 */
	public void setMismatchScore(int mismatchScore) {
		this.mismatchScore = mismatchScore;
	}
	
	/**
	 * set first sequence
	 * 
	 * @param seq1
	 */
	public void setSeq1(String seq1) {
		this.seq1 = seq1;
	}
	
	/**
	 * set second sequence
	 * 
	 * @param seq2
	 */
	
	public void setSeq2(String seq2) {
		this.seq2 = seq2;
	}
}
// EOF
