package de.ut.bioinfo1.assignment1;

@SuppressWarnings ("javadoc")
public class Assignement1 {
	
	static int		count	= 0;
	
	static String	n		= "CSAFSAFGSAFSADD";
	static String	m		= "ASDSADSAFSAFSAD";
	
	public static void main(String[] args) {
		
		int i = 1;
		
		while (i < 10) {
			count = 0;
			System.out.println(recNeedlemann(i, i) + " after " + count + " steps for length " + i++);
		}
	}
	
	private static int recNeedlemann(int i, int j) {
		
		count++;
		
		if (i == 0) return j;
		else if (j == 0) return i;
		else {
			final int a = recNeedlemann(i - 1, j) + 1;
			final int b = recNeedlemann(i, j - 1) + 1;
			final int c = recNeedlemann(i - 1, j - 1) + score(i, j);
			return Math.min(a, Math.min(b, c));
		}
	}
	
	private static int score(int i, int j) {
		return n.charAt(i - 1) == m.charAt(j - 1) ? 0 : 1;
	}
}