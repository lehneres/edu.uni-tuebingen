/******************************************************************************
 * main.c
 *
 * Pauli Miettinen
 * 14.1.2005
 *
 * Last modified: 15.5.2007
 *
 * You are free to distribute and/or modify these files as long as the
 * original author is mentioned. May contain bugs. To be used at one's peril.
 */

/* Macros etc. */
#define DEFAULT_BASE_SIZE 10

/* Standard libraries */
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <string.h>

/* Own libraries */
#include "approx.h"
#include "utils.h"

/* Procedures */
/*
matrix read_set(const char *file, int *s, int *d);
int print_set(const char *file, matrix S, int s, int d);
void free_matrix(matrix M, int n);
*/
void 
print_help();

int 
main(int argc, char *argv[]) {
  int s, d, k, i, c, sparse;
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
      {"dense-set-file", 1, 0, 's'},
      {"sparse-set-file", 1, 0, 'S'},
      {"help", 0, 0, 'h'},
      {"seed", 1, 0, 'd'},
      {"verbose", 0, 0, 'v'},
      {"temporary-basis", 1, 0, 'B'},
      {"threshold", 1, 0, 't'},
      {"bonus-covered", 1, 0, 'p'},
      {"penalty-overcovered", 1, 0, 'P'},
      {"decomp-matrix", 1, 0, 'D'},
      {"iterations", 1, 0, 'i'},
      {0, 0, 0, 0}
    };

    c = getopt_long(argc, argv, "k:b:s:hd:S:B:vt:p:P:D:i:",
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

    case 'B':
      opti.original_basis = optarg;
      break;

    case 't':
      opti.threshold = atof(optarg);
      break;

    case 'p':
      opti.bonus_covered = atoi(optarg);
      break;

    case 'P':
      opti.penalty_overcovered = atoi(optarg);
      break;

    case 'D':
      opti.decomp_matrix = optarg;
      break;

    case 'i':
      opti.iterations = atoi(optarg);
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
    /* we read sparse matrices */
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
    memset(B[i], 0, d);
  }

  O =NULL; /* Compatibility option... */

  /* Approximate */

  if (approximate(S, s, d, B, k, O, &opti) != 1) {
    free_matrix(B, k);
    free_matrix(S, s);
    return 2;
  }

  /* Print basis and help matrix */

  c = print_matrix((base_file==NULL)?"-":base_file, B, k, d);

  /* Epilogue */
  free_matrix(B, k);
  free_matrix(S, s);
  
  return 0;
}


void 
print_help() {
  printf("DBP Solver v0.5\n\n");
  printf("Arguments:\n");
  printf("-k, --basis-size=n\n");
  printf("\t Number of basis vectors to find.\n");
  printf("-s, --dense-set-file=FILE\n");
  printf("\t (Optional) The file where the input set in dense format should\n"
	 "\t be readed. If not given, the standard input is used.\n");
  printf("-S, --sparse-set-file=FILE\n"
	 "\t (Optional) The file where the input set in sparse format should\n"
	 "\t be readed. If not given, dense format is assumed.\n");
  printf("-b, --basis-file=FILE\n");
  printf("\t (Optional) The file where the created basis should be\n"
	 "\t written. If not given, the standard output is used.\n");
  printf("-v, --verbose\n");
  printf("\t The verbosity level; more v's means more verbosity.\n");
  printf("-d, --seed=n\n"
	 "\t Seed for the RNG. If not given, /dev/urand is used.\n"); 
  printf("-h, --help\n");
  printf("\t This text.\n");
  approx_help();
}
