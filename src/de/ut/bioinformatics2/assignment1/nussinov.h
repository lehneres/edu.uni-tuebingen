/*+++++++++++++++++ class Nussinov  +++++++++++++++++++ */

#ifndef __nussinov_h__
#define __nussinov_h__

// include IO
#include <iostream>

#include<string>

#include <matrix.h>
#include <sequence.h>
#include <scorematrix.h>
#include <algorithm>

/*	This class should implement the Nussinov RNA prediction
 algorithm in its final version.
 Your task is to implement the empty functions.
 */
class Nussinov {
public:

	// new type that saves a base pairing (just the indices)
	struct base_pair_type {
		int base_pos_1;
		int base_pos_2;
	};

	// new type to save a final RNA fold and score
	struct fold_result_type {
		// this vector is just a collection of base pairs
		std::vector<base_pair_type> base_pairs;
		int score;
	};

	// Constructor, initiate object with a sequence
	Nussinov(Sequence seq);

	// fold sequence
	void fold(ScoreMatrix score_matrix, int min_loop_length);

	// return fold results
	std::vector<fold_result_type> getRNAStructures() const;

	// print fold result
	void printRNAStructure() const;

private:
	Sequence _sequence;
	std::vector<fold_result_type> _fold_result;

	// compute the scoring matrix
	Matrix<int> computeNussinovMatrix(ScoreMatrix score_matrix);

	// compute the maximum score for base pair
	int computeMaxScore(ScoreMatrix score_matrix, Matrix<int> nussinov, int i,
			int j);

	// merge to vectors
	std::vector<Nussinov::base_pair_type> mergeBasePairVectors(
			std::vector<Nussinov::base_pair_type> A,
			std::vector<Nussinov::base_pair_type> B);

	// compute the traceback (only one optimal solution)
	std::vector<Nussinov::base_pair_type> tracebackNussinovSimple(
			ScoreMatrix score_matrix, Matrix<int> nussinov,
			int minimum_loop_length, int i, int j);

	// compute the traceback
	void tracebackNussinov(ScoreMatrix score_matrix, Matrix<int> nussinov,
			std::vector<Nussinov::base_pair_type> base_pairs,
			int minimum_loop_length, int i, int j, int score);

	// check if _fold_result already contains a certain structure
	bool foldResultContains(std::vector<Nussinov::base_pair_type>);

};
#endif
