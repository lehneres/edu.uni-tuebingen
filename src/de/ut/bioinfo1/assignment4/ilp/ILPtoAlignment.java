package de.ut.bioinfo1.assignment4.ilp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * ILPtoAlignment. Bioinformatics I, WS 12/13 Parses the output of lp_solve and
 * produces an alignment ZBIT, Universitaet Tuebingen, 2012, Daniel Huson
 */

public class ILPtoAlignment extends ILPtoAlignmentBase {
	@SuppressWarnings ("javadoc")
	public static final String	AUTHOR	= "Sebastian Lehnerer";
	
	/**
	 * construct ILP to alignment converter for two given sequences
	 * 
	 * @param seq1
	 * @param seq2
	 */
	public ILPtoAlignment(String seq1, String seq2) {
		super(seq1, seq2);
	}
	
	/**
	 * parses the file produced by LP_SOLVE and sets the alignment from it
	 * 
	 * @param r
	 * @throws IOException
	 */
	public void parseCrunchFile(BufferedReader r) throws IOException {
		String align1 = "";
		String align2 = "";
		int score = 0;
		// parse the crunch file and set the alignment:
		
		String line = "";
		final HashSet<Integer> seq1 = new HashSet<Integer>();
		final HashSet<Integer> seq2 = new HashSet<Integer>();
		
		while ((line = r.readLine()) != null)
			if (line.startsWith("Value of objective function:")) score =
					new Double(line.split(" ")[line.split(" ").length - 1]).intValue();
			else if (line.startsWith("x") && line.substring(line.length() - 1).equals("1")) {
				seq1.add(xNode(line.substring(0, 5)));
				seq2.add(yNode(line.substring(0, 5)));
			}
		
		int i1 = 0, i2 = 0;
		do
			if (seq1.contains(i1) && seq2.contains(i2)) {
				align1 += getSeq1().charAt(i1++);
				align2 += getSeq2().charAt(i2++);
			} else if (seq1.contains(i1)) {
				align1 += "-";
				align2 += getSeq2().charAt(i2++);
			} else if (seq2.contains(i2)) {
				align1 += getSeq1().charAt(i1++);
				align2 += "-";
			} else {
				while (i1 < getSeq1().length()) {
					align1 += getSeq1().charAt(i1++);
					align2 += "-";
				}
				while (i2 < getSeq2().length()) {
					align1 += "-";
					align2 += getSeq2().charAt(i2++);
				}
			}
		while (i1 < getSeq1().length() || i2 < getSeq2().length());
		
		// set the alignment:
		setOptimalScore(score);
		setAlign1(align1);
		setAlign2(align2);
	}
}
// EOF
