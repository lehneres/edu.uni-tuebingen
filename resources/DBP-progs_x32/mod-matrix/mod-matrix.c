/* mod-matrix.c
 *
 * Modifies input matrix, i.e., outputs it in sparse/dense format or
 * transposes it.
 *
 * Pauli Miettinen
 * 16.5.2007
 *
 * You are free to distribute and/or modify these files as long as the
 * original author is mentioned. May contain bugs. To be used at one's peril.
 */

#include "utils.h"
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

matrix
transpose_matrix(matrix M, int *n, int *m);

void
print_help();

int
main(int argc, char *argv[])
{
  int c, n, m, transpose, sparse_input, sparse_output;
  static int verbose;
  char *input, *output;
  matrix M, T;

  transpose = sparse_input = sparse_output = 0;
  input = output = NULL;
  M = T = NULL;
  verbose = 0;

  /* Parse command-line options */
  while (1) {
    int option_index = 0;
    static struct option long_options[] = {
      {"dense-input", optional_argument, NULL, 'i'},
      {"sparse-input", optional_argument, NULL, 'I'},
      {"dense-output", optional_argument, NULL, 'o'},
      {"sparse-output", optional_argument, NULL, 'O'},
      {"transpose", no_argument, NULL, 't'},
      {"verbose", no_argument, &verbose, 1},
      {"help", no_argument, NULL, 'h'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "i::I::o::O::th", long_options,
		    &option_index);
    if (c == -1)
      break;

    switch(c) {
    case 0:
      /* verbose sets flag, do nothing */
      break;

    case 'i':
      input = optarg;
      sparse_input = 0;
      break;

    case 'I':
      input = optarg;
      sparse_input = 1;
      break;

    case 'o':
      output = optarg;
      sparse_output = 0;
      break;

    case 'O':
      output = optarg;
      sparse_output = 1;
      break;

    case 't':
      transpose = 1;
      break;

    case 'h':
      print_help();
      return 0;

    case '?':
      break;

    case ':':
      fprintf(stderr, "Missing argument.\n");
      break;

    default:
      fprintf(stderr, "getop returned character code 0%o\n", c);
    }
  }

  if (input == NULL) input = "-";
  if (output == NULL) output = "-";
  if (verbose) {
    fprintf(stderr, "%s input = %s, %s output = %s, %s transpose\n",
	    (sparse_input) ? "sparse" : "dense", input,
	    (sparse_output) ? "sparse" : "dense", output,
	    (transpose) ? "do" : "do not");
  }
  if (sparse_input) {
    M = read_sparse_matrix(input, &n, &m);
  } else {
    M = read_matrix(input, &n, &m);
  }
  if (M == NULL) return 1;

  if (transpose) {
    T = transpose_matrix(M, &n, &m);
    if (T == NULL) return 1;
  }

  if (sparse_output) {
    c = print_sparse_matrix(output, (transpose) ? T : M, n, m);
  } else {
    c = print_matrix(output, (transpose) ? T : M, n, m);
  }

  return 0;
}

matrix
transpose_matrix(matrix M, int *n, int *m) 
{
  int i, j, r, c;
  matrix T;
  
  /* flip n and m */
  r = *m;
  c = *n;
  *m = c;
  *n = r;
  
  /* Create new matrix */
  T = (matrix)malloc(r*sizeof(vector));
  if (T == NULL) {
    perror("Error when allocating space for transpose");
    return NULL;
  }
  for (i=0; i<r; i++) {
    T[i] = (vector)malloc(c*sizeof(char));
    if (T[i] == NULL) {
      perror("Error when allocating space for transpose");
      return NULL;
    }
  }

  for (j=0; j<c; j++) {
    for (i=0; i<r; i++) {
      T[i][j] = M[j][i];
    }
  }

  return T;
}

void
print_help() {
  printf("mod-matrix v0.1\n"
	 "Arguments:\n"
	 "-i, --dense-input[=FILE]\n"
	 "\t Read input in dense format (default). If FILE is given, then\n"
	 "\t it is used for reading the input. Otherwise stdin is used.\n"
	 "-I, --sparse-input[=FILE]\n"
	 "\t Read input in sparse format. Otherwise similar to the previous.\n"
	 "-o, --dense-output[=FILE]\n"
	 "\t Print output in dense format (default). If FILE is given, then\n"
	 "\t output is printed in that file. Otherwise stdout is used.\n");
  printf("-O, --sparse-output[=FILE]\n"
	 "\t Print output in sparse format. Otherwise similar to the previous\n"	 
	 "-t, --transpose\n"
	 "\t Transpose the matrix.\n"
	 "--verbose\n\t Prints some additional information.\n"
	 "-h, --help\n\t This text.\n\n");
  printf("NOTE: When using short options with arguments, argument must be\n"
	 "written immediately after (i.e., without spaces) the option.!\n\n");
}
