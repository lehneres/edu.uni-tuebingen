/*
 * comparer.c
 * 
 * Creates a set from basis and help matrix and compares it to the original
 * set. Prints out some statistical information.
 *
 * Usage:
 * comparer [-s|-S] set_file -o o_file -b basis_file [-t]
 *  
 * Pauli Miettinen
 * 20.1.2005
 *
 * Last modified
 * 16.5.2007
 *
 * You are free to distribute and/or modify these files as long as the
 * original author is mentioned. May contain bugs. To be used at one's peril.
 */

#include "utils.h"
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>

struct error_struct {
  int total_error;
  int set_total;
  int cover_total;
  int uncovered_total;
  int overcovered_total;
};

/* Procedures */

matrix
matrix_mult(matrix A, int a_size, int a_dim, matrix B, int b_dim);

struct error_struct
compare(matrix A, matrix B, int size, int dim);

void
print_help();

int
main (int argc, char *argv[]) 
{
  int c, set_dim, set_size, basis_dim, basis_size;
  int o_dim, o_size, statistic, sparse;
  matrix S, B, O, result;
  char *set_file, *basis_file, *o_file;
  struct error_struct error;

  /* Default (= wrong) values for options */
  set_file = basis_file = o_file = NULL;
  statistic = 0; /* defaults not to print statistical mode */
  sparse = 0;
  
  /* Parse command-line options */
  while (1) {
    int option_index = 0;
    static struct option long_options[] = {
      {"dense-set-file", 1, 0, 's'},
      {"sparse-set-file", 1, 0, 'S'},
      {"basis-file", 1, 0, 'b'},
      {"decomp-file", 1, 0, 'D'},
      {"statistic", 0, 0, 't'},
      {"help", 0, 0, 'h'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "s:S:D:b:th", long_options, &option_index);
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
      o_file = optarg;
      break;
    case 't':
      statistic = 1;
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

  /* We need all of the options! */
  if (set_file == NULL || basis_file == NULL || o_file == NULL) {
    fprintf(stderr, "Error: all file names must be given.\n");
    return 1;
  }
  
  if (sparse) {
    S = read_sparse_matrix(set_file, &set_size, &set_dim);
  } else {
    S = read_matrix(set_file, &set_size, &set_dim);
  }
  if (S == NULL) 
    return 1;
  B = read_matrix(basis_file, &basis_size, &basis_dim);
  if (B == NULL) {
    free_matrix(S, set_size);
    return 1;
  }
  if (set_dim != basis_dim) {
    fprintf(stderr, "Error: Set and basis must have same dimension!\n");
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    return 1;
  }
  O = read_matrix(o_file, &o_size, &o_dim);
  if (O == NULL) {
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    return 1;
  }
  if (set_size != o_size) {
    fprintf(stderr, "Error: Set and decomp must have same size!\n");
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    free_matrix(O, o_size);
    return 1;
  }
  if (basis_size != o_dim) {
    fprintf(stderr, "Error: Basis' size must be equal to decomp's dimension!\n");
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    free_matrix(O, o_size);
    return 1;
  }

  result = matrix_mult(O, o_size, o_dim, B, basis_dim);
  if (result == NULL) {
    free_matrix(S, set_size);
    free_matrix(B, basis_size);
    free_matrix(O, o_size);
    return 1;
  }

  error = compare(S, result, set_size, set_dim);

  if (statistic) {
    printf("\"%s\" \"%s\" \"%s\" ", set_file, basis_file, o_file);
    printf("%i %i %i ", set_size, set_dim, basis_size);
    printf("%i %f ", error.total_error, 
	   ((float)error.total_error / (float)(error.set_total)) * 100.0);
    printf("%i %i ", error.set_total, error.cover_total);
    printf("%i %i\n", error.uncovered_total, error.overcovered_total);
  } else {
    printf("\nSet file: %s\nBasis file: %s\nDecomp file: %s\n\n",
	   set_file, basis_file, o_file);
    printf("Set has %i %i-dimensional vectors.\n", set_size, set_dim);
    printf("Basis has %i vectors.\n", basis_size);
    printf("Total error is %i, which is %.2f %% of set's ones.\n",
	   error.total_error, 
	   ((float)error.total_error / (float)(error.set_total)) * 100.0);
    printf("Set has %i 1's and cover has %i 1's.\n", error.set_total,
	   error.cover_total);
    printf("Cover did not cover %i of set's 1's and covered %i of set's 0's.\n",
	   error.uncovered_total, error.overcovered_total);
  }
  free_matrix(S, set_size);
  free_matrix(B, basis_size);
  free_matrix(O, o_size);
  free_matrix(result, set_size);

  return 0;
}


matrix
matrix_mult(matrix A, int a_size, int a_dim, matrix B, int b_dim) 
{
  int i, j, k, b_size, c_size, c_dim;
  matrix C;

  b_size = a_dim;
  c_size = a_size;
  c_dim = b_dim;

  /* Allocate space for resulting matrix */
  if ((C = malloc(c_size * sizeof(char *))) == NULL) {
    perror("Error while allocating space for multiplied matrix");
    return NULL;
  }

  for (i=0; i<c_size; i++) {
    if ((C[i] = malloc(c_dim * sizeof(char))) == NULL) {
      perror("Error while allocating space for multiplied matrix");
      free_matrix(C, i);
      return NULL;
    }
  }

  for (i=0; i<c_size; i++)
    for (j=0; j<c_dim; j++) C[i][j] = 0;

  /* Multiply (using Boolean matrix multiplication) matrixes A and B */

  for (i=0; i<c_size; i++) {
    for (j=0; j<c_dim; j++) {
      for (k=0; k<a_dim; k++) C[i][j] |= A[i][k]*B[k][j];
    }
  }

  return C;
}

struct error_struct
compare(matrix A, matrix B, int size, int dim)
{
  int i, j;
  struct error_struct error = {0, 0, 0, 0, 0};

  for (i=0; i<size; i++) {
    for (j=0; j<dim; j++) {
      error.total_error += ((A[i][j] - B[i][j] < 0) 
			    ? (B[i][j] - A[i][j]) 
			    : (A[i][j] - B[i][j]));
      error.set_total += A[i][j];
      error.cover_total += B[i][j];
      if (A[i][j] > B[i][j]) 
	error.uncovered_total++;
      else if (B[i][j] > A[i][j])
	error.overcovered_total++;
    }
  }

  return error;
}

void
print_help()
{
  printf("DBP result comparer v1.1\n\n");
  printf("Arguments:\n");
  printf("-s, --dense-set-file=FILE\n"
	 "\t The file where the original set in dense format should be readed.\n");
  printf("-S, --sparse-set-file=FILE\n"
	 "\t The file where the original set in sparse format should be readed.\n"
	 "\t If not given, the dense format is assumed.\n");
  printf("-b, --basis-file=FILE\n"
	 "\t The file where the basis should be readed.\n");
  printf("-D, --decomp-file=FILE\n"
	 "\t The file where the decomposition should be readed.\n");
  printf("-t, --statistic\n"
	 "\t Formats output in spreadsheet format.\n");
  printf("-h, --help\n\t This text\n\n");
}
