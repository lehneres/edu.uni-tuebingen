package de.ut.bioinfo1.assignment9;

import java.util.Arrays;

@SuppressWarnings ("javadoc")
public class Horspool {
	
	private static final char[]	epsilon	= new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ' };
	
	static int getPosInArray(char c, char[] ep) {
		for (int i = 0; i < ep.length; i++)
			if (ep[i] == c) return i;
		return -1;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String t = "CAAAATTTCATTATCAT".toUpperCase();
		final String p = "ATTAT".toUpperCase();
		
		final int m = p.length();
		
		final int[] d = new int[epsilon.length];
		Arrays.fill(d, m);
		
		System.out.print("m=" + m + ", d = ");
		
		for (int j = 0; j < m - 1; j++) {
			d[getPosInArray(p.charAt(j), epsilon)] = m - (j + 1);
			System.out.print(p.charAt(j) + "(" + (m - (j + 1)) + ") ");
		}
		
		System.out.println();
		
		int step = 1;
		
		int pos = 0;
		while (pos <= t.length() - m) {
			System.out.println(step++ + ") " + t.substring(0, pos) + " < " + t.substring(pos, pos + m) + " > "
					+ t.substring(pos + m, t.length()));
			int j = m - 1;
			System.out.print(t.charAt(pos + j) + (t.charAt(pos + j) == p.charAt(j) ? "==" : "!=") + p.charAt(j));
			while (j > -1 && t.charAt(pos + j) == p.charAt(j)) {
				j--;
				if (j > -1)
					System.out.print(", " + t.charAt(pos + j) + (t.charAt(pos + j) == p.charAt(j) ? "==" : "!=")
							+ p.charAt(j));
			}
			System.out
					.println(", d[" + t.charAt(pos + m - 1) + "]=" + d[getPosInArray(t.charAt(pos + m - 1), epsilon)]);
			pos += d[getPosInArray(t.charAt(pos + m - 1), epsilon)];
			if (j == -1) System.out.println("occurance at pos " + (pos + 1));
		}
		
	}
	
}
