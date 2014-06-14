/******************************************************************************
 * main.c
 *
 * Pauli Miettinen
 * 14.1.2005
 *
 * Last modified
 * 16.5.2007
 *
 * Implements the k-medians algorithm from Miettinen, P. The Discrete Basis
 * Problem. Report C-2006-010, Dept. of Computer Science, University of 
 * Helsinki (Master's thesis). The algorithm is essentially the one-swap
 * local search algorithm for k-medians described in Arya et al. Local 
 * search heuristics for k-median and facility location problems. SIAM J.
 * Comput., Vol. 33, No. 3. (2004), pp. 544-562.
 *
 * You may freely modify and/or distribute this code as long as the original
 * author is mentioned. May contain bugs. To be used at one's peril. 
 */

/* Macros etc. */
#define DEFAULT_BASE_SIZE 10

/* Standard libraries */
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>

/* Own libraries */
#include "approx.h"
#include "utils.h"

/* Procedures */
/*
matrix read_set(const char *file, int *s, int *d);
int print_set(const char *file, matrix S, int s, int d);
void free_matrix(matrix M, int n);
*/
void print_help();

int main(int argc, char *argv[]) {
  int s, d, k, i, j, c, sparse;
  matrix S, B, O;
  char *set_file, *base_file, *o_file;
  options opti = {0,     /* error_max */
		  10,    /* cut_size */
		  0,     /* noisy_vectors */
		  0,     /* iterations */
		  1,     /* remove_covered */
		  0,     /* seed */
		  0,     /* verbose */
		  NULL,  /* original_basis */
		  1.0,   /* threshold */
		  0,     /* majority */
		  1,     /* bonus_covered */
		  1,     /* penalty_overcovered */
		  NULL}; /* decomp matrix */


  /* Default values for options */
  k = DEFAULT_BASE_SIZE;
  set_file = base_file = o_file = NULL;
  sparse = 0; 

  /* Parse options for approximate too*/
  while (1) {
    int option_index = 0;
    static struct option long_options[] = {
      {"basis-size", 1, 0, 'k'},
      {"basis-file", 1, 0, 'b'},
      {"decomp-file", 1, 0, 'o'},
      {"dense-set-file", 1, 0, 's'},
      {"sparse-set-file", 1, 0, 'S'},
      {"help", 0, 0, 'h'},
      {"seed", 1, 0, 'd'},
      {"verbose", 0, 0, 'v'},
      {"majority", 0, 0, 'm'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "k:b:o:s:hd:S:vm",
		    long_options, &option_index);
    if (c == -1) 
      break;
    
    switch (c) {
    case 'k':
      k = atoi(optarg);
      break;
      
    case 'b':
      base_file = optarg;
      break;

    case 'o':
      o_file = optarg;
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

      /* start parsing approx options */

    case 'd':
      opti.seed = atoi(optarg);
      break;

    case 'v':
      opti.verbose++;
      break;

    case 'm':
      opti.majority = 1;
      break;

    case '?':
      break;

    case ':':
      fprintf(stderr, "Missing argument.\n");
      break;

    default:
      fprintf(stderr, "getop returned character code 0%o\n", c);
    }
  }

  if (sparse) {
    /* Read sparse matrix */
    S = read_sparse_matrix((set_file==NULL)?"-":set_file, &s, &d);
  } else {
    S = read_matrix((set_file==NULL)?"-":set_file, &s, &d);
  }
  if (S == NULL) return 1;
  
  /* Allocate space for basis here, so that we don't need to do it while
     approximating. */
  if ((B=malloc(k * sizeof(vector))) == NULL) {
    perror("Error while allocating space for basis");
    free_matrix(S, s);
    return 1;
  }
  for (i=0; i<k; i++) {
    if ((B[i]=malloc(d * sizeof(char))) == NULL) {
      perror("Error while allocating space for basis");
      free_matrix(S, s);
      free_matrix(B, i);
      return 1;
    }
  }

  for (i=0; i<k; i++)
    for (j=0; j<d; j++) B[i][j] = 0;

  /* Allocate space for help matrix here, as for basis. */
  if ((O=malloc(s * sizeof(vector))) == NULL) {
    perror("Error while allocating space for help matrix");
    free_matrix(S, s);
    free_matrix(B, k);
    return 1;
  }
  for (i=0; i<s; i++) {
    if ((O[i]=malloc(k * sizeof(char))) == NULL) {
      perror("Error while allocating space for basis");
      free_matrix(S, s);
      free_matrix(B, k);
      free_matrix(O, i);
      return 1;
    }
  }
  for (i=0; i<s; i++) 
    for (j=0; j<k; j++) O[i][j] = 0;

  /* Approximate */

  if (approximate(S, s, d, B, k, O, &opti) != 1) {
    free_matrix(O, s);
    free_matrix(B, k);
    free_matrix(S, s);
    return 2;
  }

  /* Print basis and help matrix */

  c = print_matrix((base_file==NULL)?"-":base_file, B, k, d);
  c = print_matrix((o_file==NULL)?"-":o_file, O, s, k);

  /* Epilogue */

  free_matrix(O, s);
  free_matrix(B, k);
  free_matrix(S, s);
  
  return 0;
}


void print_help() {
  printf("DBP Solver v1.0\n\n");
  printf("Arguments:\n");
  printf("-k, --basis-size=n\n");
  printf("\t Number of basis vectors to find.\n");
  printf("-s, --dense-set-file=FILE\n");
  printf("\t (Optional) The file where the input set in dense format is.\n" 
	 "\t If not given, the standard input is used.\n");
  printf("-S, --sparse-set-file=FILE\n");
  printf("\t (Optional) The file where the input set in sparse format is.\n"
	 "\t If not given, dense format is assumed.\n");
  printf("-b, --basis-file=FILE\n");
  printf("\t (Optional) The file where the created basis should be\n");
  printf("\t written. If not given, the standard output is used.\n");
  printf("-o --decomp-file file\n");
  printf("\t (Optional) The file where decomposition matrix should be written.\n");
  printf("\t If not given the standard output is used.\n");
  printf("-h, --help\n");
  printf("\t This text.\n");
  approx_help();
}
