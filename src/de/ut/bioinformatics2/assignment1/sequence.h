/*+++++++++++++++++ class Sequence  +++++++++++++++++++ */

#ifndef __sequence_h__
#define __sequence_h__

// include IO
#include <iostream>
#include <fstream>

#include <string>

/* This class saves any kind of sequence. It can be
   accessed like an array and sequences can be read from
   a FASTA input file.
 */
class Sequence {
public:

	// Constructors
	Sequence();
	Sequence(std::string seq, std::string name="unkown sequence");
	Sequence(Sequence const &seq_object);
	
	// access string at index position
	char& operator[](int index);
	
	// read sequence from fasta file (will overwrite exisiting sequence)
	void readFasta(char*);
	
	// return sequence length
	int size() const;
	
	// returns sequence name
	std::string getName() const;
	
	// returns sequence
	std::string getSequence() const;
	
	// assign operator
	Sequence& operator=(const Sequence& right);
	
private:
	// member variable = sequence itself
	std::string _sequence;
	std::string _name;
};
#endif
