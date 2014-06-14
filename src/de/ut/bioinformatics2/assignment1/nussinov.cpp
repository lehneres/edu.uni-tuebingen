/*+++++++++++++++++ class Nussinov  +++++++++++++++++++ */
#include<nussinov.h>
#include<climits>
#include <stdio.h>
#include <string.h>

/*###### Constructors ########## */

// initiate object with a sequence
Nussinov::Nussinov(Sequence seq) :
		_sequence(seq), _fold_result(std::vector<fold_result_type>(0)) {
}
;

/*###### Access functions ########## */

// fold sequence
void Nussinov::fold(ScoreMatrix score_matrix, int minimum_loop_length) {

	Matrix<int> nussinov = computeNussinovMatrix(score_matrix);

	std::cout << "nussinov matrix" << std::endl << nussinov.toString()
			<< std::endl;

	std::vector<Nussinov::base_pair_type> optimal = tracebackNussinovSimple(
			score_matrix, nussinov, minimum_loop_length, 0,
			nussinov.size() - 1);

	if (_fold_result.size() == 0) {
		fold_result_type tmp = { optimal, nussinov[0][nussinov.size() - 1] };
		_fold_result.push_back(tmp);
	}
}

// return fold results
std::vector<Nussinov::fold_result_type> Nussinov::getRNAStructures() const {
	return _fold_result;
}

void Nussinov::printRNAStructure() const {
	std::cout << "RNA secondary structures of sequence: "
			<< _sequence.getSequence() << std::endl;
	for (int i = 0; i < _fold_result.size(); i++) {
		std::cout << "Optimal RNA structure #" << (i + 1) << std::endl;
		std::cout << "	Score: " << _fold_result[i].score << std::endl;
		std::cout << "bpseq: " << std::endl;

		for (int k = 0; k < _sequence.size(); k++) {
			char c = _sequence.getSequence()[k];
			int t = 0;
			for (int j = 0; j < _fold_result[i].base_pairs.size(); j++) {
				if (k == _fold_result[i].base_pairs[j].base_pos_1) {
					t = _fold_result[i].base_pairs[j].base_pos_2;
					break;
				} else if (k == _fold_result[i].base_pairs[j].base_pos_2) {
					t = _fold_result[i].base_pairs[j].base_pos_1;
					break;
				}
			}
			std::cout << k + 1 << " " << c << " " << (t > 0 ? t + 1 : t)
					<< std::endl;
		}

		std::cout << std::endl << "bracket dot: #" << (i + 1) << std::endl;

		for (int k = 0; k < _sequence.size(); k++) {
			char c = '.';
			for (int j = 0; j < _fold_result[i].base_pairs.size(); j++) {
				if (k == _fold_result[i].base_pairs[j].base_pos_1) {
					c = '(';
					break;
				} else if (k == _fold_result[i].base_pairs[j].base_pos_2) {
					c = ')';
					break;
				}
			}
			std::cout << c;
		}
		std::cout << std::endl;
	}
}

/*###### Private functions ########## */

Matrix<int> Nussinov::computeNussinovMatrix(ScoreMatrix score_matrix) {
	Matrix<int> matrix = Matrix<int>(_sequence.size(), 0);

// initalize matrix
//	for (int i = 0; i < matrix.size(); i++) {
//		matrix[i][i] = 0;
//		if (i > 0) {
//			matrix[i][i - 1] = 0;
//		}
//	}

// compute scores
	for (int l = 1; l < matrix.size(); l++) {
		for (int j = l; j < matrix.size(); j++) {
			matrix[j - l][j] = computeMaxScore(score_matrix, matrix, j - l, j);
		}
	}

	return matrix;
}

int Nussinov::computeMaxScore(ScoreMatrix score_matrix, Matrix<int> nussinov,
		int i, int j) {

	/*	std::cout << "size " << nussinov.size() << std::endl;
	 std::cout << "i " << i << std::endl;
	 std::cout << "j " << j << std::endl;*/

	int tmp_max = std::max(nussinov[i + 1][j], nussinov[i][j - 1]);

	tmp_max = std::max(tmp_max,
			nussinov[i + 1][j - 1]
					+ score_matrix.get(_sequence[i], _sequence[j]));
	for (int k = i + 1; k < j; k++) {
		tmp_max = std::max(tmp_max, nussinov[i][k] + nussinov[k + 1][j]);
	}
	return tmp_max;

}

std::vector<Nussinov::base_pair_type> Nussinov::tracebackNussinovSimple(
		ScoreMatrix score_matrix, Matrix<int> nussinov, int minimum_loop_length,
		int i, int j) {

	std::vector<base_pair_type> base_pairs = std::vector<base_pair_type>(0);

	if (i < j) {

		if (nussinov[i][j] == nussinov[i + 1][j]
				&& nussinov[i][j + 1] != nussinov[i][j]) {
			base_pairs = mergeBasePairVectors(
					tracebackNussinovSimple(score_matrix, nussinov,
							minimum_loop_length, i + 1, j), base_pairs);
		}

		else if (nussinov[i][j] == nussinov[i][j - 1]
				&& nussinov[i - 1][j] != nussinov[i][j]) {
			base_pairs = mergeBasePairVectors(
					tracebackNussinovSimple(score_matrix, nussinov,
							minimum_loop_length, i, j - 1), base_pairs);
		}

		else if (nussinov[i][j]
				== nussinov[i + 1][j - 1]
						+ score_matrix.get(_sequence[i], _sequence[j])) {

			if (score_matrix.get(_sequence[i], _sequence[j]) > 0
					&& j - i > minimum_loop_length) {
				base_pair_type base_pair = { i, j };
				base_pairs.push_back(base_pair);
			}

			base_pairs = mergeBasePairVectors(
					tracebackNussinovSimple(score_matrix, nussinov,
							minimum_loop_length, i + 1, j - 1), base_pairs);
		}

		else {
			for (int k = i + 1; k < j; k++) {
				if (nussinov[i][j] == (nussinov[i][k] + nussinov[k + 1][j])) {
					base_pairs = mergeBasePairVectors(
							tracebackNussinovSimple(score_matrix, nussinov,
									minimum_loop_length, i, k), base_pairs);
					base_pairs = mergeBasePairVectors(
							tracebackNussinovSimple(score_matrix, nussinov,
									minimum_loop_length, k + 1, j), base_pairs);
				}
			}
		}
	}

	return base_pairs;
}

void Nussinov::tracebackNussinov(ScoreMatrix score_matrix, Matrix<int> nussinov,
		std::vector<base_pair_type> base_pairs, int minimum_loop_length, int i,
		int j, int score) {

	if (nussinov[i][j] == 0) {

		if (!foldResultContains(base_pairs)) {
			fold_result_type test_fold = { base_pairs, score };

			_fold_result.push_back(test_fold);
		}

	} else if (i + minimum_loop_length < j) {

		if (nussinov[i][j] == nussinov[i + 1][j]
				&& nussinov[i][j + 1] != nussinov[i][j]) {
			tracebackNussinov(score_matrix, nussinov, base_pairs,
					minimum_loop_length, i + 1, j, score);
		}

		if (nussinov[i][j] == nussinov[i][j - 1]
				&& nussinov[i - 1][j] != nussinov[i][j]) {
			tracebackNussinov(score_matrix, nussinov, base_pairs,
					minimum_loop_length, i, j - 1, score);
		}

		if (nussinov[i][j]
				== nussinov[i + 1][j - 1]
						+ score_matrix.get(_sequence[i], _sequence[j])) {

			if (score_matrix.get(_sequence[i], _sequence[j]) > 0) {
				base_pair_type base_pair = { i, j };
				base_pairs.push_back(base_pair);
			}

			tracebackNussinov(score_matrix, nussinov, base_pairs,
					minimum_loop_length, i + 1, j - 1,
					score + score_matrix.get(_sequence[i], _sequence[j]));
		}

		for (int k = i + 1; k < j; k++) {
			if (nussinov[i][j] == (nussinov[i][k] + nussinov[k + 1][j])) {
				tracebackNussinov(score_matrix, nussinov, base_pairs,
						minimum_loop_length, i, k, score);
				tracebackNussinov(score_matrix, nussinov, base_pairs,
						minimum_loop_length, k + 1, j, score);
			}
		}
	}
}

bool Nussinov::foldResultContains(
		std::vector<Nussinov::base_pair_type> base_pairs) {

	std::string cur = "";
	for (int k = 0; k < _sequence.size(); k++) {
		char c = '.';
		for (int i = 0; i < base_pairs.size(); i++) {
			if (k == base_pairs[i].base_pos_1) {
				c = '(';
				break;
			} else if (k == base_pairs[i].base_pos_2) {
				c = ')';
				break;
			}
		}
		cur += c;
	}

	for (int i = 0; i < _fold_result.size(); i++) {
		std::string ref = "";
		for (int k = 0; k < _sequence.size(); k++) {
			char c = '.';
			for (int j = 0; j < _fold_result[i].base_pairs.size(); j++) {
				if (k == _fold_result[i].base_pairs[j].base_pos_1) {
					c = '(';
					break;
				} else if (k == _fold_result[i].base_pairs[j].base_pos_2) {
					c = ')';
					break;
				}
			}
			ref += c;
		}
		if (ref.compare(cur) == 0)
			return true;
	}

	return false;
}

std::vector<Nussinov::base_pair_type> Nussinov::mergeBasePairVectors(
		std::vector<Nussinov::base_pair_type> A,
		std::vector<Nussinov::base_pair_type> B) {
	std::vector<Nussinov::base_pair_type> new_vec = std::vector<
			Nussinov::base_pair_type>(0);
	new_vec.reserve(A.size() + B.size());
	new_vec.insert(new_vec.end(), A.begin(), A.end());
	new_vec.insert(new_vec.end(), B.begin(), B.end());

	return new_vec;
}

