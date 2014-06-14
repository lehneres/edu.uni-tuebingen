/*
 * iter_full.c
 *
 * Tries to solve DBP using iterative association algorithm.
 *
 * Pauli Miettinen
 * 14.2.2007
 */

#include "approx.h"
#include "utils.h"
#include "iter.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>

/* A global variable for progress printing */
char progress[] = {'|', '/', '-', '\\'};

smatrix
calculate_assosiation(matrix S, int size, int dim, options *opti);

int
solve_basis(matrix S, int size, int dim, matrix B, int k, smatrix D, 
	    options *opti);

int 
vect_max(int *vect, int size);


int
approximate(matrix S, int size, int dim, matrix B, int k, matrix O, 
	    options *opti)
{
  smatrix D; /* let D be a sparse matrix */

  /* Check the correct number of iterations for the `iter' function */
  if (opti->iterations == 0) opti->iterations = MAX_ITER; /* set as default*/

  /* k must be small enough */
  if (k > sizeof(unsigned long)*CHAR_BIT) {
    fprintf(stderr, "Error: number of basis vectors must be at most %i, "
	    "but is %i! Aborting!\n", MAX_BASIS_SIZE,
	    k);
    return 0;
  }

  if (opti->verbose > 0) {
    fprintf(stderr, "Calculating associations...\n");
  }
  D = calculate_assosiation(S, size, dim, opti);

  if (D == NULL)
    return 0;

  if (opti->verbose > 0) {
    fprintf(stderr, "Solving basis...\n");
  }
  return solve_basis(S, size, dim, B, k, D, opti);

}

smatrix
calculate_assosiation(matrix S, int size, int dim, options *opti)
{
  int i, j, bit;
  double *A; /* We only need a row at a time */
  double sum;
  smatrix D;
  selement *ptr, *tmp;

  A = (double *)malloc(dim * sizeof(double));
  if (A == NULL) {
    perror("Error while allocating space for assosiation matrix");
    return NULL;
  }

  /* Allocate space for sparce matrix D */
  D = (smatrix)malloc(dim * sizeof(selement *));
  if (D == NULL) {
    perror("Error while allocating space for sparce association matrix");
    return NULL;
  }
  /* Set D to be empty matrix, i.e., full of NULLs */
  memset(D, 0, dim * sizeof(selement *));

  for (bit = 0; bit < dim; bit++) {
    if (opti->verbose > 0) {
      fprintf(stderr, "\r  column %i   ", bit+1);
    }
    /* set A to be a zero vector */
    memset(A, 0, dim * sizeof(double));

    /* summarize over the set */
    for (i = 0; i < size; i++) {
      if (S[i][bit]) {
	for (j = 0; j < dim; j++) 
	  A[j] += S[i][j];
      }
    }

    /* 'normalize' sums, A[bit] is always the largest; calculate D */
    sum = A[bit];
    ptr = tmp = NULL;
    for (i = 0; i < dim; i++) {
      A[i] /= sum;
      /* Build sparse matrix D */
      if (A[i] > opti->threshold) {
	tmp = (selement *)malloc(sizeof(selement));
	if (tmp == NULL) {
	  perror("Error while allocating space for sparse matrix element");
	  return NULL;
	}
	tmp->c = i;
	tmp->n = NULL;
	if (D[bit] == NULL) {
	  /* This is the first 1 in this row */
	  D[bit] = ptr = tmp;
	} else {
	  ptr->n=tmp;
	  ptr = tmp;
	}
      }
    }
  }
  if (opti->verbose > 0) {
    fprintf(stderr, "\n");
  }

  return D;
}

int
solve_basis(matrix S, int size, int dim, matrix B, int k, smatrix D, 
	    options *opti)
{
  int i, j, basis, row;
  int best;
  int *best_rowcount, *rowcount;
  int best_covers, covers;
  selement *element;
  FILE *fp;
  matrix decomp;
  imatrix covered;
  unsigned long int error, bestError;


  /* Open basis file */
  if (opti->original_basis != NULL) {
    fp = fopen(opti->original_basis, "w");
    if (fp == NULL) {
      perror("Error when opening file for printing basis");
    }
  } else fp = NULL;


  covered = (imatrix)malloc(size * sizeof(ivector));
  if (covered == NULL) {
    perror("Error while allocating space for covered matrix");
    return 0;
  }
  for (i = 0; i < size; i++) {
    covered[i] = (ivector)malloc(dim * sizeof(unsigned long int));
    if (covered[i] == NULL) {
      perror("Error while allocating space for covered matrix");
      return 0;
    }
    memset(covered[i], 0, dim * sizeof(unsigned long int));
  }

  decomp = (matrix)malloc(size * sizeof(vector));
  if (covered == NULL) {
    perror("Error when allocating space for decomposition matrix");
    return 0;
  }
  for (i = 0; i < size; i++) {
    decomp[i] = (vector)malloc(k * sizeof(char));
    if (decomp[i] == NULL) {
      perror("Error when allocating space for decomposition matrix");
      return 0;
    }
    memset(decomp[i], 0, k);
  }

  /* best is thus far an integer giving the correct row */

  best_rowcount = (int *)malloc(size * sizeof(int));
  if (best_rowcount == NULL) {
    perror("Error while allocating space for 'best_rowcount' vector");
    return 0;
  }

  rowcount = (int *)malloc(size * sizeof(int));
  if (rowcount == NULL) {
    perror("Error while allocating space for 'rowcount' vector");
    return 0;
  }

  /* iterate thru all asked basis vectors */
  for (basis = 0; basis < k; basis++) {
    if (opti->verbose > 0) {
      fprintf(stderr, "\r                                                  "
	      "\r  basis vector %i   ", basis+1);
    }

    best = -1;
    best_covers = 0;
    memset(best_rowcount, 0, size * sizeof(int));
  
    /* iterate thru all rows in D */
    for (row = 0; row < dim; row++) {
      /* find best row */

      if (opti->verbose > 0) fprintf(stderr, "\b%c", progress[row%4]);

      covers = 0;
      memset(rowcount, 0, size * sizeof(int));

      for (i = 0; i < size; i++) {
	element = D[row];
	while (element != NULL) {
	  if (S[i][element->c] == 0) rowcount[i] -= opti->penalty_overcovered
	    * (1 - roundc(covered[i][element->c]));
	  else rowcount[i] += opti->bonus_covered 
	    * (S[i][element->c] - roundc(covered[i][element->c]));
	  element = element->n;
	}
	if (rowcount[i] > 0) covers += rowcount[i];
      }

      if (covers > best_covers) {
	/* we found the best */
	best_covers = covers;
	/* 'best' is this row */
	best = row;
	memcpy(best_rowcount, rowcount, size*sizeof(int));
      }
    }


    if (best > -1) {
      /* We have found the best - if best == -1, this basis vector will be
       * empty.
       */
      for (i = 0; i < size; i++) {
	if (best_rowcount[i] > 0) {
	  /* save to decomp */
	  decomp[i][basis] = 1;
	  element = D[best];
	  while (element != NULL) {
	    covered[i][element->c] = add_k(covered[i][element->c], basis);
	    element = element->n;
	  }
	}
      }
      element = D[best];
      while (element != NULL) {
	B[basis][element->c] = 1;
	element = element->n;
      }
    }
    

    /* And finally, let's print each basis vector as soon as they are ready */
    if (fp != NULL) {
      for (i=0; i < dim; i++) fprintf(fp, "%c ", '0'+B[basis][i]);
      fprintf(fp, "\n");
      fflush(fp);
    }
  }
  if (opti->verbose > 0) {
    fprintf(stderr, "\n");
  }

  if (opti->verbose > 2) {
    fprintf(stderr, "Covered, phase 1:\n");
    for (i = 0; i < size; i++) {
      for (j = 0; j < dim; j++) fprintf(stderr, "%lu ", covered[i][j]);
      fprintf(stderr, "\n");
    }
  }

  error = 0;
  /* Set error = current error; count current error */
  for (i = 0; i < size; i++) {
    for (j = 0; j < dim; j++) {
      error += abs(S[i][j] - roundc(covered[i][j]));
    }
  }

  bestError = 0;
  i = 0;
  if (opti->verbose > 0) {
    fprintf(stderr, "Iterating ...  ");
  }
  do {
    if (opti->verbose > 1) {
      fprintf(stderr, "\r                                            "
	      "\rIterating: %i, difference = %lu", i+1, bestError - error);
    }
    bestError = error;
    error = iterate(S, size, dim, B, k, decomp, covered, opti);
    i++;
  } while (error < bestError && i < opti->iterations);


  if (opti->verbose > 0) {
    fprintf(stderr, " Done! Took %i iterations.\n", i);
  }


  /* This is simply a sanity check */
  if (error > bestError) {
    fprintf(stderr, "Warning: iteration increased the error - something is\n"
	    "wrong here! Old error = %lu vs. new error = %lu\n", bestError,
	    error);
  }

  if (opti->verbose > 2) {
    fprintf(stderr, "Covered, phase 2:\n");
    for (i = 0; i < size; i++) {
      for (j = 0; j < dim; j++) fprintf(stderr, "%lu ", covered[i][j]);
      fprintf(stderr, "\n");
    }
  }


  /* Print decomposition matrix */
  if (opti->decomp_matrix != NULL) 
    print_matrix(opti->decomp_matrix, decomp, size, k);

  if (fp != NULL) fclose(fp);
  
  return 1; 
}


int
vect_max(int *vect, int size)
{
  int i, best;
  best = vect[0];
  for (i = 1; i < size; i++) {
    if (vect[i] > best)
      best = vect[i];
  }
  return best;
}

void
approx_help()
{
  printf("-t, --threshold=f\n"
	 "\t Threshold giving the floating-point number where to discretize.\n"
	 "\t Defaults to 1.0.\n"
	 "-B, --temporary-basis=FILE\n"
	 "\t A file where the basis will be printed during computation.\n"
	 "-p, --bonus-covered=p\n"
	 "\t Bias the object function to give `p' times more points for\n"
	 "\t covering 1s.\n");
  printf("-P, --penalty-overcovered=P\n"
	 "\t Bias the object function to penalize each covered 0 by 'P'.\n"
	 "-D, --decomp-matrix=FILE\n"
	 "\t The file where the decomposition matrix will be printed.\n");
  printf("-i, --iterations=i\n"
	 "\t Maximum number of iterations to make. Optional.\n\n");
  printf("NOTE: the basis size `k' can be at most %i.\n\n", MAX_BASIS_SIZE);
	 
}
