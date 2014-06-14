package de.ut.bioinfo1.assignment9;

import java.util.BitSet;

@SuppressWarnings ("javadoc")
public class ShiftOr {
	
	private static final char[]	epsilon	= new char[] { 'A', 'C', 'T', 'G' };
	
	static int getPosInArray(char c, char[] ep) {
		for (int i = 0; i < ep.length; i++)
			if (ep[i] == c) return i;
		return -1;
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		final String p = "ACAT";
		final String t = "CACACATTA";
		
		final int m = p.length();
		
		final BitSet[] B = new BitSet[m];
		
		for (int i = 0; i < m; i++) {
			B[i] = new BitSet(m);
			B[i].set(0, m);
		}
		
		for (int i = 0; i < p.length(); i++) {
			final int pp = getPosInArray(p.charAt(i), epsilon);
			if (pp == -1) throw new Exception("unkown char");
			B[pp].clear(m - i - 1);
		}
		
		for (int i = 0; i < m; i++)
			System.out.println("B[" + epsilon[i] + "] = " + printBitSet(B[i], m));
		
		BitSet D = new BitSet(m);
		D.set(0, m);
		
		System.out.println("\nD = " + printBitSet(D, m) + "\n");
		
		for (int i = 0; i < t.length(); i++) {
			System.out.println(i + 1 + " Reading " + t.charAt(i));
			
			D = D.get(1, m);
			System.out.print("D (" + printBitSet(D, m) + ") | B[" + t.charAt(i) + "] ("
					+ printBitSet(B[getPosInArray(t.charAt(i), epsilon)], m) + ")");
			D.or(B[getPosInArray(t.charAt(i), epsilon)]);
			System.out.print(" = " + printBitSet(D, m));
			final BitSet test = new BitSet(m);
			test.set(0);
			test.and(D);
			if (test.isEmpty()) System.out.println(" occurence at pos " + (i + 1 - m + 1));
			else System.out.println();
		}
	}
	
	private static String printBitSet(BitSet test, int m) {
		String bit = "";
		for (int i = 0; i < m; i++)
			bit += test.get(i) ? "1" : "0";
		return bit;
	}
	
}
