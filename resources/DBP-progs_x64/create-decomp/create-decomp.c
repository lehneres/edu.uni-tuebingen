/******************************************************************************
 * create_decomp.c
 *
 * Pauli Miettinen
 * 1.2.2005
 *
 * Last modified
 * 16.5.2007
 *
 * To create optimal decompostion matrix when data and basis matrices are
 * given. Running time is exponential w.r.t. the number of basis vectors.
 *
 * Version 0.2 uses -p and -P for setting penalties for covered 1s and 0s 
 * and long options.
 *
 * You are free to distribute and/or modify these files as long as the
 * original author is mentioned. May contain bugs. To be used at one's peril.
 *
 *****************************************************************************/

/* Libraries */
#include "utils.h"
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <math.h>


/* Function prototypes */

void
next_combination(vector V, int length);

matrix
find_O(matrix S, int s_size, int s_dimension, matrix B, int b_size,
       int penalty_1, int penalty_0);

void
print_help();

/* Functions */

int
main(int argc, char *argv[]) 
{
  int c, set_size, set_dimension, basis_size, basis_dimension;
  int penalty_1, penalty_0, sparse;
  matrix S, B, O;
  char *set_file, *basis_file, *O_file;

  /* Default values */
  set_file = basis_file = O_file = NULL;
  penalty_1 = penalty_0 = 1;
  sparse = 0;

  /* Read command-line arguments */
  while (1) {
    int option_index = 0;
    static struct option long_options[] = {
      {"dense-set-file", 1, 0, 's'},
      {"sparse-set-file", 1, 0, 'S'},
      {"basis-file", 1, 0, 'b'},
      {"decomp-file", 1, 0, 'D'},
      {"penalty-for-1", 1, 0, 'p'},
      {"penalty-for-0", 1, 0, 'P'},
      {"help", 0, 0, 'h'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "s:S:D:b:p:P:h", long_options, &option_index);
    if (c == -1) 
      break;

    switch (c) {
    case 's':
      set_file = optarg;
      sparse = 0;
      break;
    case 'S':
      set_file = optarg;
      sparse = 1;
      break;
    case 'b':
      basis_file = optarg;
      break;
    case 'D':
      O_file = optarg;
      break;
    case 'p':
      penalty_1 = atoi(optarg);
      break;
    case 'P':
      penalty_0 = atoi(optarg);
      break;
    case 'h':
      print_help();
      return 0;
    case '?':
      break;
    case ':':
      return 1;
    default:
      fprintf(stderr, "Error: getopt returned unknown character code 0%o\n", 
	      c);
      return 1;
    }
  }

  /* Check if some argument is not given */
  if (set_file == NULL)
    set_file = "-";
  if (basis_file == NULL)
    basis_file = "-";
  if (O_file == NULL)
    O_file = "-";
  
  /* Read set & basis */
  if (sparse) {
    S = read_sparse_matrix(set_file, &set_size, &set_dimension);
  } else {
    S = read_matrix(set_file, &set_size, &set_dimension);
  }
  if (S == NULL)
    return 1;

  B = read_matrix(basis_file, &basis_size, &basis_dimension);
  if (B == NULL) {
    free_matrix(S, set_size);
    return 1;
  }

  if (set_dimension != basis_dimension) {
    fprintf(stderr, "Set and basis must have same dimension!\n");
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    return 1;
  }
   
  /* Find O */
  O = find_O(S, set_size, set_dimension, B, basis_size, penalty_1, penalty_0);
  if (O == NULL) {
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    return 1;
  }

  print_matrix(O_file, O, set_size, basis_size);

  free_matrix(S, set_size);
  free_matrix(B, basis_size);
  free_matrix(O, set_size);
  

  return 0;
}

matrix
find_O(matrix S, int s_size, int dim, matrix B, int b_size, int penalty_1,
       int penalty_0) 
{
  int i, j, error, best_error, vect;
  matrix O;
  double num_of_comb, iter;
  vector curr_combination, best_combination, vect_union;

  /* Alloc space for O-matrix */
  O = malloc(s_size * sizeof(vector));
  if (O == NULL) {
    perror("Error while allocating space for decomp matrix");
    return NULL;
  }
  for (i = 0; i < s_size; i++) {
    O[i] = malloc(b_size * sizeof(char));
    if (O[i] == NULL) {
      perror("Error while allocating space for decomp matrix");
      free_matrix(O, i);
      return NULL;
    }
  }

  /* Alloc space for combination vectors and vec_union*/
  curr_combination = malloc(b_size * sizeof(char));
  if (curr_combination == NULL) {
    perror("Error while allocating space for combination vectors");
    free_matrix(O, s_size);
    return NULL;
  }

  best_combination = malloc(b_size * sizeof(char));
  if (best_combination == NULL) {
    perror("Error while allocating space for combination vectors");
    free_matrix(O, s_size);
    free(curr_combination);
    return NULL;
  }

  vect_union = malloc(dim * sizeof(char));
  if (vect_union == NULL) {
    perror("Error while allocating space for vectors union");
    free_matrix(O, s_size);
    free(curr_combination);
    free(best_combination);
    return NULL;
  }

  /* Number of different combinations */
  num_of_comb = pow(2, b_size);
  if (num_of_comb == 0 || num_of_comb == HUGE_VAL) {
    perror("Error when calculating number of combinations");
    free_matrix(O, s_size);
    free(curr_combination);
    free(best_combination);
    free(vect_union);
    return NULL;
  }

  /* Start to iterate over set's vectors */
  for (vect = 0; vect < s_size; vect++) {

    /* Initialize best_error and curr_combination */
    best_error = dim; /* We cannot have any bigger error */
    for (i = 0; i < b_size; i++)
      curr_combination[i] = 0;

    /* Iterate over all (sigh) possible combinations OR until BEST_ERROR = 0*/
    for (iter = 0; iter < num_of_comb && best_error > 0; iter++) {
    
      /* Initialize vect_union to zero vector */
      for (i = 0; i < dim; i++)
	vect_union[i] = 0;

      /* Calculate union of this combination's vectors */
      for (i = 0; i < b_size; i++) {
	if (curr_combination[i]) {
	  for (j = 0; j < dim; j++)
	    vect_union[j] |= B[i][j];
	}
      }

      /* Compare VECT_UNION to set's VECT vector and count error */
      error = 0;
      for (i = 0; i < dim; i++) {
	if (vect_union[i] == 1 && S[vect][i] == 0) error += penalty_0;
	else if (vect_union[i] == 0 && S[vect][i] == 1) error += penalty_1;
      }

      if (error < best_error) {
	best_error = error;
	for (i = 0; i < b_size; i++) 
	  best_combination[i] = curr_combination[i];
      }

      /* Count next combination */
      next_combination(curr_combination, b_size);

    } /* End of loop over different combinations */

    /* Now we know best combination for this vector. */
    /* Let's copy it to O-matrix */
    for (i = 0; i < b_size; i++) 
      O[vect][i] = best_combination[i];

  } /* End of loop over set's vectors */

  /* We now know decomp matrix so let's clean up and return 0 */

  free(vect_union);
  free(best_combination);
  free(curr_combination);

  return O;
}

void
next_combination(vector V, int length)
{
  int i;
  char muistibitti;

  muistibitti = 1;
  for (i=0; i < length && muistibitti; i++) {
    if (muistibitti == 1) {
      if (V[i] == 1) {
	V[i] = 0;
	muistibitti = 1;
      } else {
	V[i] = 1;
	muistibitti = 0;
      }
    }
  }
}

void
print_help()
{
  fprintf(stderr, 
	  "create-decomp v1.0\n\n"
	  "Usage:\n"
	  "-s, --dense-set-file=FILE\n"
	  "\t The file where to read the data set in dense format.\n"
	  "-S, --sparse-set-file=FILE\n"
	  "\t The file where to read the data set in sparse format.\n"
	  "-b, --basis-file=FILE\n"
	  "\t The file where to read the basis (in dense format).\n"
	  "-D, --decomp-file=FILE\n"
	  "\t The file where to write the decomposition.\n"
	  "-p, --penalty-for-1=n\n"
	  "\t The penalty for not covering 1s in data.\n"
	  "-P, --penalty-for-0=n\n"
	  "\t The penalty for covering 0s in data.\n\n");
}
