package de.ut.bioinformatics2.assignment3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * implements a in silico digestion with trypsin
 * 
 * @author LehnereS
 * 
 */
public class Cutter {
	
	/**
	 * @param peptide
	 * @return list of all masses
	 * 
	 */
	public static List<Double> byIons(String peptide) {
		final List<Double> masses = new LinkedList<>();
		for (int i = 0; i < peptide.length() - 1; i++) {
			System.out.print("b-ion: ");
			int s = 0;
			double mass = 0;
			while (s <= i) {
				System.out.print(peptide.charAt(s));
				mass += AminoAcid.getAminoAcid(peptide.charAt(s)).getMonoMass();
				s++;
			}
			System.out.println("\t" + mass);
			masses.add(mass);
			System.out.print("y-ion: ");
			mass = 0;
			s = i + 1;
			while (s < peptide.length()) {
				System.out.print(peptide.charAt(s));
				mass += AminoAcid.getAminoAcid(peptide.charAt(s)).getMonoMass();
				s++;
			}
			System.out.println("\t" + mass);
			masses.add(mass);
		}
		return masses;
	}
	
	private static Map<String, String> digest(String name, String sequence) {
		final Map<String, String> parts = new HashMap<>();
		
		int i = 0;
		for (int j = i; j < sequence.length(); j++)
			if (sequence.charAt(j) == 'K' || sequence.charAt(j) == 'R') {
				parts.put(name + "_" + i + "" + j, sequence.substring(i, j));
				i = j;
			}
		
		return parts;
	}
	
	/**
	 * @param args
	 *            [0] fasta file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		final Map<String, String> sequences = new HashMap<>();
		final Map<String, String> seq_Digested = new HashMap<>();
		
		if (args[0] == null) System.err.println("first argument must be a fasta file");
		
		String line = "";
		try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
			String name = null;
			while ((line = br.readLine()) != null)
				// receive header information
				if (line.startsWith(">")) {
					name = line.substring(1);
					sequences.put(name, new String());
				}
				// add sequence information
				else if (!line.startsWith(";") && name != null) sequences.put(name, sequences.get(name) + line);
		}
		
		for (final String key : sequences.keySet())
			seq_Digested.putAll(digest(key, sequences.get(key)));
		
		System.out.println(seq_Digested);
		
		final Set<Double> masses = new HashSet<>();
		
		for (final String key : seq_Digested.keySet()) {
			masses.addAll(byIons(seq_Digested.get(key)));
			if (seq_Digested.get(key).length() > 4) {
				
				double avgMass = 0;
				double monoMass = 0;
				for (final char c : seq_Digested.get(key).toCharArray()) {
					avgMass += AminoAcid.getAminoAcid(c).getAvgMass();
					monoMass += AminoAcid.getAminoAcid(c).getMonoMass();
				}
				System.out.println(seq_Digested.get(key) + "\t" + monoMass + "\tda\t(mono)");
				System.out.println(seq_Digested.get(key) + "\t" + avgMass + "\tda\t(avg)");
			}
		}
		
		double min = Collections.min(masses);
		final double max = Collections.max(masses);
		
		final Map<Double, Integer> count = new TreeMap<>();
		while (min <= max) {
			count.put(min + 1, 0);
			min += 1;
		}
		
		int total = 0;
		
		for (final Double mass : masses)
			for (final double key : count.keySet())
				if (mass <= key + 0.5 && key - 0.5 <= mass) {
					count.put(key, count.get(key) + 1);
					total++;
				}
		
		System.out.println("total of unambigues ion masses: " + total);
	}
	
}
