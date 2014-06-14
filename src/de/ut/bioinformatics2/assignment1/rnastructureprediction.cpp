#include <iostream>
#include <sstream>
#include <stdlib.h>

#include<scorematrix.h>
#include<nussinov.h>

int main(int argc, char **argv)
{
	// read in arguments
	if (argc != 3) {
		std::cout << "ERROR: Wrong number of parameters!" << std::endl;
		exit(1);
	}
	// first argument is the RNA FastaFile -> create sequence object and read in the file
	Sequence rna_sequence = Sequence();
	rna_sequence.readFasta(argv[1]);

	// second argument is the minimum loop length
	int min_loop_length;
	std::istringstream i(argv[2]); 
	i >> min_loop_length; 
	
	// create Nussinov object with given sequence
	Nussinov nussinov = Nussinov(rna_sequence);

	// create alphabet for score matrix
	std::vector< char > alphabet = std::vector< char >(0);
	alphabet.push_back('A');
	alphabet.push_back('C');
	alphabet.push_back('U');
	alphabet.push_back('G');
	
	// size=4 since we use  4 characters {A,U,G,C}, zero is the default value for the score matrix
	ScoreMatrix score_matrix = ScoreMatrix(alphabet, 0);
	score_matrix.set('A','U',1);
	score_matrix.set('C','G',1);
	
	//start nussinov algorithm
	nussinov.fold(score_matrix, min_loop_length);
	
	// print structures
	nussinov.printRNAStructure();

	return 0;
};
