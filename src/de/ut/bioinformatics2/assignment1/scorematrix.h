/*+++++++++++++++++ class ScoreMatrix  +++++++++++++++++++ */

#ifndef __scorematrix_h__
#define __scorematrix_h__


// include IO
#include <iostream>

#include <matrix.h>
#include<map>

/* 	This class saves a score matrix, that saves an integer
	score	for every combination of two characters
 */
class ScoreMatrix {
public:

	// Constructors
	// initiate score matrix with list of keys, every key combination gets score 'default_value'
	ScoreMatrix(std::vector< char > keys, int default_value);
	// initiate score matrix with list of keys and a matrix that corresponds to the score matrix
	ScoreMatrix(std::vector< char > keys, Matrix<int> scores);
	// copy constructor
	ScoreMatrix(ScoreMatrix const &score_matrix_object);
	
	// returns score of a and b
	int get(char a, char b);
	
	// sets score of a and b
	void set(char a, char b, int new_score);
	
	// returns size of score matrix, or number of available keys
	int size() const;
	
	// assign operator
	ScoreMatrix& operator=(const ScoreMatrix& right);
	
private:
	// member variables
	
	// score matrix
	Matrix<int> _matrix;
	// list of availible keys
	std::vector< char > _keys;
	// mapping of keys to matrix
	std::map<char, int>  _index;
	
};
#endif

