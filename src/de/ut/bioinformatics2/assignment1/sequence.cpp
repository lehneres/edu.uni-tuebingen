/*+++++++++++++++++ class Sequence +++++++++++++++++++ */
#include <sequence.h>
#include <stdlib.h>

/*###### Constructors ########## */

// standard (empty) constructor
Sequence::Sequence() : _sequence(""), _name("empty sequence") {
};

// initiate object with sequence and name
Sequence::Sequence(std::string seq, std::string name) : _sequence(seq), _name(name) {
};

// copy constructor
Sequence::Sequence(Sequence const &seq_object) : _sequence(seq_object._sequence), _name(seq_object._sequence) {
}

/*###### Access functions ########## */

// returns sequence legth
int Sequence::size() const {
	return _sequence.size();
}

// access character at position 'index'
char& Sequence::operator[](int index) {
	return _sequence[index];
}

// return name of sequence
std::string Sequence::getName() const {
	return _name;
}

// returns sequence
std::string Sequence::getSequence() const {
	return _sequence;
}

// read sequence from fasta file (overwrite existing sequence)
void Sequence::readFasta(char* file_name) {

	// open file
	std::ifstream fasta_file(file_name);
	if (!fasta_file) {
		std::cout << "ERROR: Could not open FASTA file '" << file_name << "' !" << std::endl;
		exit(1);
	}

	// get line containing the sequence name
	std::string row;
	bool eof = !getline(fasta_file, row);
	if (eof || row[0] != '>') {
		std::cout << "ERROR: FASTA file '" << file_name << "' with wrong format !" << std::endl;
		exit(1);
	}
	// save name, therefor reject leading '>'
	_name = row.substr(1, row.size() - 2);

	// get sequence from file by appending line after line, stop if '>' and empty line or eof occurs
	_sequence = "";
	while (! fasta_file.eof()) {
		getline(fasta_file, row);
		// deblank
		row.erase(row.find_last_not_of(" \n\r\t")+1);
		// append row of file to sequence
		_sequence.append(row);
	}
	// close file
	fasta_file.close();
}

// assign operator
Sequence& Sequence::operator=(const Sequence& right) {
	_name = right._name;
	_sequence = right._sequence;
	
	return *this;
}
