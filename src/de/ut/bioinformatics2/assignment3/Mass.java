/**
 * from PRIDE
 * https://code.google.com/p/pride-toolsuite/source/browse/pride-utilities/trunk/src/main/java/uk/ac/ebi/pride
 * /mol/Mass.java?r=608
 */

package de.ut.bioinformatics2.assignment3;

/**
 * Mass provides a set of interfaces to get monoisotopic mass and average mass.
 * 
 * User: rwang Date: 10-Aug-2010 Time: 09:31:57
 */
@SuppressWarnings ("all")
public interface Mass {
	
	public double getAvgMass();
	
	public double getMonoMass();
}