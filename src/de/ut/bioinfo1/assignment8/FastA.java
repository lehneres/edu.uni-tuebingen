/**
 * Simple FastA class Daniel Huson, 10.2012
 */
package de.ut.bioinfo1.assignment8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Vector;

/**
 * Simple FastA class
 */
public class FastA {
	private int						size		= 0;
	private final Vector<String>	headers		= new Vector<String>();
	private final Vector<String>	sequences	= new Vector<String>();
	
	/**
	 * add a header and sequence
	 * 
	 * @param header
	 * @param sequence
	 */
	public void add(String header, String sequence) {
		set(getSize(), header, sequence);
	}
	
	/**
	 * clear the data
	 */
	public void clear() {
		headers.clear();
		sequences.clear();
		size = 0;
	}
	
	/**
	 * gets the first header
	 * 
	 * @return first header
	 */
	public String getFirstHeader() {
		return headers.firstElement();
	}
	
	/**
	 * gets the first sequence
	 * 
	 * @return first sequence
	 */
	public String getFirstSequence() {
		return sequences.firstElement();
	}
	
	/**
	 * gets the header
	 * 
	 * @param i
	 *            the index
	 * @return the header
	 */
	public String getHeader(int i) {
		return headers.get(i);
	}
	
	/**
	 * gets the sequence
	 * 
	 * @param i
	 *            the index
	 * @return the sequence
	 */
	public String getSequence(int i) {
		return sequences.get(i);
	}
	
	/**
	 * get the size of sequences
	 * 
	 * @return size of sequences
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * read header and sequence in fastA format
	 * 
	 * @param r
	 * @throws java.io.IOException
	 */
	public void read(Reader r) throws IOException {
		clear();
		
		final BufferedReader br = new BufferedReader(r);
		
		String header = "";
		StringBuffer sequence = new StringBuffer();
		
		String aLine;
		while ((aLine = br.readLine()) != null) {
			aLine = aLine.trim();
			if (aLine.length() > 0) if (aLine.charAt(0) == '>') // new fastA
																// header
				{
					if (header.length() > 0) add(header, sequence.toString());
					header = aLine;
					sequence = new StringBuffer();
				} else sequence.append(aLine);
		}
		if (header.length() > 0) add(header, sequence.toString());
	}
	
	/**
	 * sets the header and sequence
	 * 
	 * @param i
	 *            the index
	 * @param header
	 * @param sequence
	 */
	public void set(int i, String header, String sequence) {
		if (i < getSize()) {
			headers.set(i, header);
			sequences.set(i, sequence);
		} else {
			setSize(i + 1);
			headers.add(i, header);
			sequences.add(i, sequence);
		}
	}
	
	/**
	 * sets the size of sequences
	 * 
	 * @param n
	 *            the size
	 */
	public void setSize(int n) {
		if (n > size) {
			headers.setSize(n);
			sequences.setSize(n);
		}
		size = n;
	}
	
	/**
	 * write header and sequence in fastA format
	 * 
	 * @param w
	 * @throws IOException
	 */
	public void write(Writer w) throws IOException {
		for (int i = 0; i < getSize(); i++)
			if (getHeader(i) != null) {
				w.write(getHeader(i) + "\n");
				for (int c = 0; c < getSequence(i).length(); c += 80) {
					final int d = Math.min(getSequence(i).length(), c + 80);
					w.write(getSequence(i).substring(c, d) + "\n");
				}
			}
	}
}
