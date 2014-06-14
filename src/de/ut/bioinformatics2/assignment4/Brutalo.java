package de.ut.bioinformatics2.assignment4;

import java.util.Arrays;

import Jama.Matrix;

@SuppressWarnings ("all")
public class Brutalo {
	
	public static void main(String[] args) {
		//@formatter:off
		final double[][] matrix =
				{
			
				{ -1,  0,  0,  0,  0,  0,  0,  1,  0,  0,  0 },
				{ -1,  0,  0,  0,  0, 1,  0,  0,  0,  0,  0 },
				{  1, -1,  0,  0,  0,  0, -1,  0,  0,  0,  0 },
				{  1,  0,  0,  0,  0,  -1,  0,  0,  0,  0,  0 },
				{  0,  1, -1,  0,  0,  0,  0,  0,  0,  0,  0 },
				{  0,  0,  1, -1,  0,  0,  1,  0,  0,  0,  0 },
				{  0,  0,  0,  0,  0,  1,  0,  0,  0,  0, -1 },
				{  0,  0,  0,  0, -1,  0, -1,  0,  1,  0,  0 },
				{  0,  0,  0,  0,  1,  0,  1,  0,  0, -1,  0 },
				{  0,  0,  0,  1, -1,  0,  0,  0,  0,  0,  0 },
				{  0,  0,  0,  0,  1, -1,  0,  0,  0,  0,  0 } };
		//@formatter:on
		
		final Matrix jama = new Matrix(matrix);
		System.out.println(jama.rank());
		
		for (double r1 = 0; r1 < 3; r1++)
			for (double r2 = 0; r2 < 3; r2++)
				for (double r3 = 0; r3 < 3; r3++)
					for (double r4 = 0; r4 < 3; r4++)
						for (double r5 = 0; r5 < 3; r5++)
							for (double r6 = 0; r6 < 3; r6++)
								for (double r7 = 0; r7 < 3; r7++)
									for (double b1 = 0; b1 < 3; b1++)
										for (double b2 = 0; b2 < 3; b2++)
											for (double b3 = 0; b3 < 3; b3++)
												for (double b4 = 0; b4 < 3; b4++) {
													
													final Matrix res =
															new Matrix(new double[] { r1, r2, r3, r4, r5, r6, r7, b1,
																	b2, b3, b4 }, 1).times(jama);
													
													boolean nxt = false;
													
													for (int i = 0; i < (res.getRowDimension() > res
															.getColumnDimension() ? res.getRowDimension() : res
															.getColumnDimension()); i++)
														if (res.get(0, i) != 0) {
															nxt = true;
															break;
														}
													
													if (!nxt)
														System.out.println(Arrays.deepToString(new Double[] { r1, r2,
																r3, r4, r5, r6, r7, b1, b2, b3, b4 }));
													
												}
		
	}
	
}
