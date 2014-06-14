/*********************************************************************
 *
 * iter_pp.c
 *
 * Pauli Miettinen 15.5.2007
 *
 * A post-processing program to iteratively improve the DBP solution.
 *
 **********************************************************************/

#include "approx.h"
#include "utils.h"
#include "iter.h"
#include <stdio.h>
#include <getopt.h>
#include <stdlib.h>
#include <string.h>

int
improveD(matrix S, int n, int m, matrix B, int k, matrix D, options *opti);

void
print_help();


int
main(int argc, char *argv[])
{
  char *set_file, *basis_file, *orig_decomp_file, *new_decomp_file;
  matrix S, B, D;
  int c, sparse, n, m, k, tmpN, tmpM, tmpK;
  options opti = {0, 0, 0, MAX_ITER, 0, 0, 0, NULL, 0.0, 0, 0, 0, NULL};

  set_file = basis_file = orig_decomp_file = new_decomp_file = NULL;

  sparse = 0;

  while(1) {
    int option_index = 0;
    static struct option long_options[] = {
      {"basis-file", 1, 0, 'b'},
      {"dense-set-file", 1, 0, 's'},
      {"sparse-set-file", 1, 0, 'S'},
      {"help", 0, 0, 'h'},
      {"verbose", 0, 0, 'v'},
      {"decomp-matrix", 1, 0, 'D'},
      {"orig-decomp-matrix", 1, 0, 'O'},
      {"iterations", 1, 0, 'i'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "b:s:S:hvD:O:i:", long_options, 
		    &option_index);

    if (c == -1) break;

    switch (c) {
    case 'b':
      basis_file = optarg;
      break;
    case 's':
      set_file = optarg;
      sparse = 0;
      break;
    case 'S':
      set_file = optarg;
      sparse = 1;
      break;
    case 'h':
      print_help();
      return 0;
    case 'v':
      opti.verbose++;
      break;
    case 'D':
      new_decomp_file = optarg;
      break;
    case 'O':
      orig_decomp_file = optarg;
      break;
    case 'i':
      opti.iterations = atoi(optarg);
    case '?':
      break;
    case ':':
      fprintf(stderr, "Missing argument.\n");
      break;
    default:
      fprintf(stderr, "getop returned character code 0%o\n", c);
    }
  }

  /* Read the dataset */
  if (sparse) S = read_sparse_matrix((set_file==NULL)?"-":set_file, &n, &m);
  else S = read_matrix((set_file==NULL)?"-":set_file, &n, &m);

  if (S == NULL) return 1;

  /* Read the basis file */

  B = read_matrix((basis_file==NULL)?"-":basis_file, &k, &tmpM);

  if (B == NULL) return 1;
  if (tmpM != m) {
    fprintf(stderr, "Data and basis dimensions do not agree: \n"
	    "\tdata has %i columns and basis has %i.\n", m, tmpM);
    return 1;
  }
  if (k > MAX_BASIS_SIZE) {
    fprintf(stderr, "Too many basis vectors: the maximum is %i, but there "
	    "are %i.\n", MAX_BASIS_SIZE, k);
    return 1;
  }

  /* Read the decomp file (we need an initial guess) */

  D=read_matrix((orig_decomp_file==NULL)?"-":orig_decomp_file,&tmpN,&tmpK); 

  if (D == NULL) return 1;
  if (tmpN != n || tmpK != k) {
    fprintf(stderr, "Matrix dimensions must agree:\n"
	    "Data is %i x %i, basis is %i x %i and decomp is %i x %i\n",
	    n, m, k, m, tmpN, tmpK);
    return 1;
  }

  if (improveD(S, n, m, B, k, D, &opti) != 1) return 1;

  /* print new decomp matrix */ 
  print_matrix((new_decomp_file==NULL)?"-":new_decomp_file, D, n, k);

  return 0;
}


int
improveD(matrix S, int n, int m, matrix B, int k, matrix D, options *opti)
{
  int i, j, basis;
  imatrix covered;
  unsigned long int error, bestError;

  covered = (imatrix)malloc(n * sizeof(ivector));
  if (covered == NULL) {
    perror("Error while allocating space for covered matrix");
    return 0;
  }
  for (i = 0; i < n; i++) {
    covered[i] = (ivector)malloc(m * sizeof(unsigned long int));
    if (covered[i] == NULL) {
      perror("Error while allocating space for covered matrix");
      return 0;
    }
    memset(covered[i], 0, m * sizeof(unsigned long int));
  }
  
  /* Initialize covered */

  for (i = 0; i < n; i++) {
    for (j = 0; j < m; j++) {
      if (S[i][j] == 1) {
	for (basis = 0; basis < k; basis++) {
	  if (D[i][basis]*B[basis][j] == 1) 
	    covered[i][j] = add_k(covered[i][j], basis);
	}
      }
    }
  }

  if (opti->verbose > 2) {
    fprintf(stderr, "Covered before update:\n");
    for (i = 0; i < n; i++) {
      for (j = 0; j < m; j++) fprintf(stderr, "%lu ", covered[i][j]);
      fprintf(stderr, "\n");
    }
    fprintf(stderr, "\n");
  }


  /* Set current error */
  error = 0;
  for (i = 0; i < n; i++) {
    for (j = 0; j < m; j++) {
      error += abs(S[i][j] - roundc(covered[i][j]));
    }
  }

  bestError = 0;
  i = 0;
  if (opti->verbose > 0) fprintf(stderr, "Iterating...   ");

  /* Start iterating */
  do {
    if (opti->verbose > 1) {
      fprintf(stderr, "\r                                            "
	      "\rIterating: %i, difference = %lu", i+1, bestError - error);
    }
    bestError = error;
    error = iterate(S, n, m, B, k, D, covered, opti);
    i++;
  } while (error < bestError && i < opti->iterations);

  if (opti->verbose > 0) fprintf(stderr, "Done! Took %i iterations.\n", i);

  /* This is simply a sanity check */
  if (error > bestError) {
    fprintf(stderr, "Warning: iteration increased the error - something is\n"
	    "wrong here! Old error = %lu vs. new error = %lu\n", bestError,
	    error);
  }

  if (opti->verbose > 2) {
    fprintf(stderr, "Covered after update:\n");
    for (i = 0; i < n; i++) {
      for (j = 0; j < m; j++) fprintf(stderr, "%lu ", covered[i][j]);
      fprintf(stderr, "\n");
    }
    fprintf(stderr, "\n");
  }


  return 1;
}

void
print_help()
{
  printf("Iter-postproc v0.1\n\n"
	 "Command-line options:\n");
  printf("-s, --dense-set-file=FILE\n"
	 "\t Dataset's file in dense format. If not given (or is `-'),\n"
	 "\t standard input is read.\n");
  printf("-S, --sparse-set-file=FILE\n"
	 "\t Dataset's file in sparse format. If not given (or is `-'),\n"
	 "\t stadard in input is read.\n");
  printf("-b, --basis-file=FILE\n"
	 "\t Basis vector's file (in dense format). If not given (or is\n"
	 "\t `-'), standard input is read. NOTE: basis file can have at\n"
	 "\t most %i rows!\n", MAX_BASIS_SIZE);
  printf("-O, --orig-decomp-matrix=FILE\n"
	 "\t Original decomposition's file (in dense format). If not given\n"
	 "\t (or is `-'), standard input is read. NOTE: mandatory option!\n");
  printf("-D, --decomp-file=FILE\n"
	 "\t Updated decomposition's file. If not given (or is `-'), \n"
	 "\t standard output is used.\n");
  printf("-i, --iterations=INT\n"
	 "\t Maximum number of iterations. Optional.\n");
  printf("-v, --verbose\n"
	 "\t Verbosity level, more `v's mean more verbosity\n");
  printf("-h, --help\n"
	 "\t Print this help\n\n");
}
