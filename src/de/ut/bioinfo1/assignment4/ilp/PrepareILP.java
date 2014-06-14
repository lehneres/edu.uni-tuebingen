package de.ut.bioinfo1.assignment4.ilp;

/**
 * PrepareILP. Bioinformatics I, WS 12/13 * Formulates and prints pairwise
 * alignment problem as ILP
 */

import java.io.PrintStream;

/**
 * PrepareILP. Bioinformatics I, WS 12/13 Formulates and prints pairwise
 * alignment problem as ILP
 */

public class PrepareILP extends PrepareILPBase {
	@SuppressWarnings ("javadoc")
	public final static String	AUTHOR	= "Sebastian Lehnerer";
	
	/**
	 * constructor for ILP alignment of two sequences
	 * 
	 * @param xseq
	 *            first sequence
	 * @param yseq
	 *            second sequence
	 */
	public PrepareILP(String xseq, String yseq) {
		super(xseq, yseq);
	}
	
	/**
	 * print ILP
	 * 
	 * @param out
	 * @throws Exception
	 * @throws java.io.IOException
	 */
	public void print(PrintStream out) throws Exception {
		// In the following, use out.print(...) for all printing
		
		// 1. Write out objective function:======================
		
		// The first line of the .ilp file must contain the objective
		// function
		// Write out objective function:
		out.print("max: ");
		// variable x(i+xseq.length+j):
		for (int i = 0; i < getSeq1().length(); i++)
			for (int j = 0; j < getSeq2().length(); j++) {
				// for each pair of positions, determine a factor f
				// for the corresponding edge variable using the
				// match and mismatch scores, depending on the characters
				// at positions i and j in sequences x and y.
				// the name of the edge variable is obtained using the
				// method nodesToVariable(i,j)
				// print +/- factor*variable
				
				final int factor = getSeq1().charAt(i) == getSeq2().charAt(j) ? getMatchScore() : getMismatchScore();
				
				out.print((factor >= 0 ? "+" : "-") + Math.abs(factor) + "*" + nodesToVariable(i, j));
			}
		// end line with ;
		out.print(";\n");
		
		// 2. Write out constraints -x<=0 and x<=1: ==============
		// The solver assumes that x>=0, hence to ensure 0<=x <=1
		// we only have to list the second inequality.
		// e.g. x222<1;
		// Observe that lp_solve interprets < as <=,
		// so print < whenever you mean <=
		for (int i = 0; i < getSeq1().length(); i++)
			for (int j = 0; j < getSeq2().length(); j++)
				out.println(nodesToVariable(i, j) + "<1;");
		
		// 3. Write out critical-mixed-cycle constraints: ================
		// To obtain a valid trace, we must ensure that critical mixed
		// cycles cannot arise.
		// Such a cycle is given by two positions i<j in x
		// and k< m in y
		//
		// x: -------i-------j-------
		//
		// y: --------k---------l----
		//
		// A critical-mixed-cycle arises when we join i to l and
		// join j to k
		// That is, only one of the two variables for (i,l) and (j,k),
		// respectively, may be 1
		
		for (int i = 0; i < getSeq1().length(); i++)
			for (int k = 0; k < getSeq2().length(); k++)
				for (int j = i; j < getSeq1().length(); j++)
					for (int l = k; l < getSeq2().length(); l++)
						if (!(i == j && l == k))
							out.println(nodesToVariable(i, l) + "+" + nodesToVariable(j, k) + "<1;");
		
		// 4. Make integer program
		// We list all variable names behind the int statement to tell
		// lp_solve that all variables must obtain an integer value
		out.print("int");
		for (int i = 0; i < getSeq1().length(); i++)
			for (int j = 0; j < getSeq2().length(); j++) {
				if (i + j > 0) out.print(",");
				out.print(" " + nodesToVariable(i, j));
			}
		out.print(";\n");
	}
}
