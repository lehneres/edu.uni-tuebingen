/*+++++++++++++++++ class Matrix  +++++++++++++++++++ */

#ifndef __matrix_h__
#define __matrix_h__


// include IO
#include <iostream>
#include <sstream>

#include <vector>

/* This class saves a matrix of any type. Elements
     can be assesed with [a,b].
 */
template <class Type> class Matrix {
public:

	// Constructors
	
	// standard (empty) constructor
	Matrix() :_matrix( std::vector< std::vector<Type> >(0) ) { };
	// initiate object with certain size and a default value for every field
	Matrix(int size, Type default_value) : _matrix( std::vector< std::vector<Type> >(size, std::vector<Type>(size, default_value))) {};
	Matrix(Matrix<Type> const &matrix_object) : _matrix(matrix_object._matrix) {};
	
	// access matrix at index position
	// if [] is used a line of the matrix is returned
	// if [][] is used an element is returned
	std::vector<Type>& operator[](int index) {
		return _matrix[index];
	}
	
	// return matrix size
	int size() const {
		return _matrix.size();
	}
	
	// assign operator
	Matrix<Type>& operator=(const Matrix<Type>& right) {
		_matrix = right._matrix;
		return *this;
	}
	
	// convert matrix to string
	std::string toString() const {
		// create string of matrix
		std::string smatrix = "";
		for (int i = 0; i < _matrix.size(); i++) {
			for (int k = 0; k < _matrix[i].size(); k++) {
				smatrix += valueToString<Type>(_matrix[i][k]) + " ";
			}
			smatrix += "\n";
		}
		return smatrix;
	}
	
	//matrix to stream
	friend std::ostream& operator<<(std::ostream& output, const Matrix& m) {
		output << (m.toString()).c_str();
		return output;
	}
	
	
private:
	// member variable = matrixitself
	std::vector< std::vector<Type> > _matrix;
	
	// function to convert any value into a string (needed for stream output)
	template<typename T> std::string valueToString(T x) const {
		std::ostringstream o;
		o << x;
		return o.str();
	}

};

#endif
