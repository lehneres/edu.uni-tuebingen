/*+++++++++++++++++ class ScoreMatrix +++++++++++++++++++ */
#include <scorematrix.h>

/*###### Constructors ########## */

// initiate score matrix with list of keys, every key combination gets score 'default_value'
ScoreMatrix::ScoreMatrix(std::vector<char> keys, int default_value) :
		_keys(keys), _matrix(Matrix<int>(keys.size(), default_value)) {
	for (int i = 0; i < keys.size(); i++) {
		_index[keys[i]] = i;
	}
}

// initiate score matrix with list of keys and a matrix that corresponds to the score matrix
ScoreMatrix::ScoreMatrix(std::vector<char> keys, Matrix<int> scores) :
		_keys(keys), _matrix(scores) {
	for (int i = 0; i < keys.size(); i++) {
		_index[keys[i]] = i;
	}
}

// copy constructor
ScoreMatrix::ScoreMatrix(ScoreMatrix const &score_matrix_object) :
		_keys(score_matrix_object._keys), _matrix(score_matrix_object._matrix), _index(
				score_matrix_object._index) {
}

/*###### Access functions ########## */

// returns score of a and b
int ScoreMatrix::get(char a, char b) {
	if (_index.find(a) == _index.end()) {
		std::cout << "ERROR: Character " << a
				<< " is not a valid character for the scoring matrix!"
				<< std::endl;
	}
	if (_index.find(b) == _index.end()) {
		std::cout << "ERROR: Character " << b
				<< " is not a valid character for the scoring matrix!"
				<< std::endl;
	}
	int i1 = _index[a];
	int i2 = _index[b];
	return _matrix[i1][i2];
}

// sets score of a and b
void ScoreMatrix::set(char a, char b, int new_score) {
	int i1 = _index[a];
	int i2 = _index[b];
	// set value on both sides of the matrix to gurantee to find the value
	_matrix[i1][i2] = new_score;
	_matrix[i2][i1] = new_score;
}

// returns size of score matrix, or number of available keys
int ScoreMatrix::size() const {
	return _keys.size();
}

// assign operator
ScoreMatrix& ScoreMatrix::operator=(const ScoreMatrix& right) {
	_matrix = right._matrix;
	_keys = right._keys;
	_index = right._index;
	return *this;
}
