package de.ut.bioinformatics2.assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author LehnereS
 */
public class PDB {
	
	static HashMap<String, Character>	translate3to1	= new HashMap<String, Character>() {
															/**
		 * 
		 */
															private static final long	serialVersionUID	= 1L;
															
															{
																put("GLY", 'G');
																put("ALA", 'A');
																put("SER", 'S');
																put("THR", 'T');
																put("CYS", 'C');
																put("VAL", 'V');
																put("LEU", 'L');
																put("ILE", 'I');
																put("MET", 'M');
																put("PRO", 'P');
																put("PHE", 'F');
																put("TYR", 'Y');
																put("TRP", 'W');
																put("ASP", 'D');
																put("GLU", 'E');
																put("ASN", 'N');
																put("GLN", 'Q');
																put("HIS", 'H');
																put("LYS", 'K');
																put("ARG", 'R');
															}
														};
	
	static HashMap<Character, String>	translate1to3	= new HashMap<Character, String>() {
															/**
		 * 
		 */
															private static final long	serialVersionUID	= 1L;
															
															{
																put('G', "GLY");
																put('A', "ALA");
																put('S', "SER");
																put('T', "THR");
																put('C', "CYS");
																put('V', "VAL");
																put('L', "LEU");
																put('I', "ILE");
																put('M', "MET");
																put('P', "PRO");
																put('F', "PHE");
																put('Y', "TYR");
																put('W', "TRP");
																put('D', "ASP");
																put('E', "GLU");
																put('N', "ASN");
																put('Q', "GLN");
																put('H', "HIS");
																put('K', "LYS");
																put('R', "ARG");
															}
														};
	
	private static double getCaDistance(Triple<Double, Double, Double> triple, Triple<Double, Double, Double> triple2,
			double precision) {
		return Math.round(Math.sqrt(Math.pow(triple2.val1 - triple.val1, 2) + Math.pow(triple2.val2 - triple.val2, 2)
				+ Math.pow(triple2.val3 - triple.val3, 2))
				* precision)
				/ precision;
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		final Set<PDBObject> pdbObjs = new HashSet<>();
		
		final File input = new File(args[0]);
		if (input.isDirectory()) for (final File file : input.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("pdb");
			}
		}))
			pdbObjs.add(readPDBFile(file));
		else pdbObjs.add(readPDBFile(input));
		
		int alpha = 0;
		int h310 = 0;
		int total = 0;
		
		final Map<Character, Integer> alphaCountsH = new HashMap<>();
		final Map<Character, Integer> a310CountsH = new HashMap<>();
		
		final SortedMap<Double, Integer> alphaDist = new TreeMap<>();
		final SortedMap<Double, Integer> a310Dist = new TreeMap<>();
		
		for (final PDBObject obj : pdbObjs) {
			alpha += obj.getNumberOfRightHandedAlpha();
			h310 += obj.getNumberOfRightHanded310();
			total += obj.getNumberOfResidues();
			
			for (int i = 0; i < obj.getNumberOfResidues(); i++) {
				if (obj.secondarystruc[i] == 1) {
					if (alphaCountsH.containsKey(obj.sequence[i])) alphaCountsH.put(obj.sequence[i],
							alphaCountsH.get(obj.sequence[i]) + 1);
					else alphaCountsH.put(obj.sequence[i], 0);
				} else if (obj.secondarystruc[i] == 5)
					if (a310CountsH.containsKey(obj.sequence[i])) a310CountsH.put(obj.sequence[i],
							a310CountsH.get(obj.sequence[i]) + 1);
					else a310CountsH.put(obj.sequence[i], 0);
				if (obj.secondarystruc[i] == 1 && i + 1 < obj.getNumberOfResidues() && obj.cAlphaPos[i] != null
						&& obj.cAlphaPos[i + 1] != null) {
					final double dist = getCaDistance(obj.cAlphaPos[i], obj.cAlphaPos[i + 1], 10);
					if (alphaDist.containsKey(dist)) alphaDist.put(dist, alphaDist.get(dist) + 1);
					else alphaDist.put(dist, 1);
					
				} else if (obj.secondarystruc[i] == 5 && i + 1 < obj.getNumberOfResidues() & obj.cAlphaPos[i] != null
						&& obj.cAlphaPos[i + 1] != null) {
					final double dist = getCaDistance(obj.cAlphaPos[i], obj.cAlphaPos[i + 1], 10);
					if (a310Dist.containsKey(dist)) a310Dist.put(dist, a310Dist.get(dist) + 1);
					else a310Dist.put(dist, 1);
				}
			}
		}
		
		final SortedMap<Character, Integer> alphaCounts = new TreeMap<>(new ValueComparator<>(alphaCountsH));
		alphaCounts.putAll(alphaCountsH);
		
		final SortedMap<Character, Integer> a310Counts = new TreeMap<>(new ValueComparator<>(a310CountsH));
		a310Counts.putAll(a310CountsH);
		
		System.out.println("precentage of rAlpha: " + new DecimalFormat("#.##").format(alpha / (double) total * 100)
				+ "%");
		System.out
				.println("precentage of r310: " + new DecimalFormat("#.##").format(h310 / (double) total * 100) + "%");
		
		System.out.println("\namino acid count in rAlpha:");
		for (final char c : alphaCounts.keySet())
			System.out.println(translate1to3.get(c) + "\t" + alphaCounts.get(c) + "\t"
					+ new DecimalFormat("#.##").format(alphaCounts.get(c) / (double) total * 100) + "%");
		System.out.println("\namino acid count in r310:");
		for (final char c : a310Counts.keySet())
			System.out.println(translate1to3.get(c) + "\t" + a310Counts.get(c) + "\t"
					+ new DecimalFormat("#.##").format(a310Counts.get(c) / (double) total * 100) + "%");
		
		System.out.println("\nca distances in rAlpha:");
		for (final double d : alphaDist.keySet())
			System.out.println(d + "\t" + alphaDist.get(d));
		System.out.println("\nca distances in r310:");
		for (final double d : a310Dist.keySet())
			System.out.println(d + "\t" + a310Dist.get(d));
	}
	
	/**
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	private static PDBObject readPDBFile(File file) throws FileNotFoundException, IOException, NumberFormatException {
		try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
			
			PDBObject obj = null;
			String line = "";
			
			int offset = 0;
			boolean modelEnd = false;
			
			while ((line = reader.readLine()) != null)
				if (line.startsWith("DBREF")) {
					int size = Integer.valueOf(line.substring(20, 24).trim());
					while ((line = reader.readLine()).startsWith("DBREF"))
						size =
								Integer.valueOf(line.substring(14, 18).trim()) < size ? size
										+ Integer.valueOf(line.substring(20, 24).trim())
										- Integer.valueOf(line.substring(14, 18).trim()) + 1 : Integer.valueOf(line
										.substring(20, 24).trim());
					obj = new PDBObject(size, file.getName());
				} else if (!modelEnd && obj != null && (line.startsWith("ATOM") || line.startsWith("HETATM"))) {
					if (line.substring(12, 16).trim().equals("CA")) {
						int pos = Integer.valueOf(line.substring(22, 26).trim()) + offset;
						if (pos > 0 && pos < obj.getNumberOfResidues()) {
							if (obj.cAlphaPos[pos] != null) {
								offset = pos;
								pos = offset + 1;
							}
							if (pos < obj.getNumberOfResidues()) {
								obj.cAlphaPos[pos] =
										new Triple<>(Double.valueOf(line.substring(30, 38).trim()), Double.valueOf(line
												.substring(38, 46).trim()), Double.valueOf(line.substring(46, 54)
												.trim()));
								obj.sequence[pos] =
										translate3to1.containsKey(line.substring(17, 20).trim()) ? translate3to1
												.get(line.substring(17, 20).trim()) : 'X';
							}
						}
					}
				} else if (obj != null && line.startsWith("HELIX")) for (int i =
						Integer.valueOf(line.substring(21, 25).trim()); i <= Integer.valueOf(line.substring(33, 37)
						.trim()); i++)
					obj.secondarystruc[Math.max(i < obj.getNumberOfResidues() ? i : obj.getNumberOfResidues() - 1, 0)] =
							Integer.valueOf(line.substring(38, 40).trim());
				else if (obj != null && line.startsWith("SHEET")) for (int i =
						Integer.valueOf(line.substring(22, 26).trim()); i <= Integer.valueOf(line.substring(33, 37)
						.trim()); i++)
					obj.secondarystruc[Math.max(i < obj.getNumberOfResidues() ? i : obj.getNumberOfResidues() - 1, 0)] =
							Integer.MAX_VALUE;
				else if (line.startsWith("ENDMDL")) modelEnd = true;
			
			reader.close();
			return obj;
		}
	}
}

class PDBObject {
	
	String								name;
	int[]								secondarystruc;
	Triple<Double, Double, Double>[]	cAlphaPos;
	char[]								sequence;
	
	@SuppressWarnings ("unchecked")
	PDBObject(int size, String name) {
		this.name = name;
		sequence = new char[size];
		secondarystruc = new int[size];
		cAlphaPos = new Triple[size];
	}
	
	int getNumberOfResidues() {
		return secondarystruc.length;
	}
	
	int getNumberOfRightHanded310() {
		return getNumberOfStructureType(5);
	}
	
	int getNumberOfRightHandedAlpha() {
		return getNumberOfStructureType(1);
	}
	
	int getNumberOfStructureType(int type) {
		
		int count = 0;
		
		for (final int struct : secondarystruc)
			if (struct == type) count++;
		
		return count;
	}
}

class Triple<a, b, c> {
	
	a	val1;
	b	val2;
	c	val3;
	
	Triple(a a, b b, c c) {
		this.val1 = a;
		this.val2 = b;
		this.val3 = c;
	}
}

class ValueComparator<K, V extends Comparable<V>> implements Comparator<K> {
	
	Map<K, V>	base;
	
	public ValueComparator(Map<K, V> base) {
		this.base = base;
	}
	
	@Override
	public int compare(K a, K b) {
		return base.get(b).compareTo(base.get(a));
	}
}
