/*
 * iter.c
 *
 * Implements the iterative local search to improve DBP results.
 *
 * Pauli Miettinen
 * 15.5.2007
 */

#include "iter.h"
#include "approx.h"
#include "utils.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>


unsigned long int
iterate(matrix S, int size, int dim, matrix B, int k, matrix decomp,
	imatrix covered, options *opti)
{
  unsigned long int error, newError;
  int i, j, ub;
  int *rowcount;

  rowcount = (int *)malloc(size * sizeof(int));
  if (rowcount == NULL) {
    perror("Error while allocating space for 'rowcount' vector in 'iterate'");
    return 0;
  }

  /* At first, compute current error, so that we know what we have to
   * upgrade.
   */

  /* Streamlining, do not do this */
#ifndef STREAMLINE
  error = 0;
  for (i = 0; i < size; i++) {
    for (j = 0; j < dim; j++) {
      error += abs(S[i][j] - roundc(covered[i][j]));
    }
  }
#endif

  /* Iterate over all basis vectors and update their usage if possible */
  if (opti->verbose > 0) fprintf(stderr, "  Updating basis    ");
  for (ub = 0; ub < k; ub++) { /* ub = Updated Basis */
    
    if (opti->verbose > 0) fprintf(stderr, "\b\b%2i", ub+1);
    memset(rowcount, 0, size * sizeof(int));
    
    /* Here we don't use penalties or bonuses, as they are only for
     * finding better basis vectors.
     */

    for (i = 0; i < size; i++) {
      for (j = 0; j < dim; j++) {
	if (S[i][j] == 0 && B[ub][j] == 1) {
	  /* penalty * 1 if we cover 0 that is not yet covered */
	  rowcount[i] -= 1 - roundc(rem_k(covered[i][j],ub));
	}
	else {
	  /* If S[i][j] = 0, then B[ub][j] = 0, and this is 0;
	   * if S[i][j] = 1, then we check does B[i][j] = 1 and is
	   * S[i][j] already covered.
	   */
	  rowcount[i] += B[ub][j]* (S[i][j] - roundc(rem_k(covered[i][j],ub)));
	}
      }
    }
    
    /* Then check if we should update anything */
    /* Streamlining, do not do this! */
#ifndef STREAMLINE
    newError = 0;
    for (i = 0; i < size; i++) {
      for (j = 0; j < dim; j++) {
	if (B[ub][j] == 0) { /* Nothing is changed here */
	  newError += abs(S[i][j] - roundc(covered[i][j]));
	} else {
	  if (rowcount[i] > 0) { /* Error increases if S[i][j] = 0 */
	    newError += 1 - S[i][j];
	  } else { /* We don't use 'ub' for this row, check what happens */ 
	    newError += abs(S[i][j] - roundc(rem_k(covered[i][j],ub)));
	  }
	}
      }
    }
#endif
#ifdef STREAMLINE
    /* To make the next if always true */
    newError = 0;
    error = 1;
#endif
    if (newError < error) {
      /* We can update this basis vector's usage accordingly */
      /* Also, updata error */
      error = newError;
      for (i = 0; i < size; i++) {
	if (rowcount[i] > 0) {
	  /* Update decomp */
	  decomp[i][ub] = 1;
	  for (j = 0; j < dim; j++) {
	    if (B[ub][j] == 1) covered[i][j] = add_k(covered[i][j], ub);
	  }
	}
	else {
	  decomp[i][ub] = 0;
	  for (j = 0; j < dim; j++) {
	    if (B[ub][j] == 1) covered[i][j] = rem_k(covered[i][j], ub);
	  }
	}
      }
    }
  }

  /* Finally, count and return the error. */
  /* This part is obsolete, as 'error' shoud already hold this information */
  newError = 0;
  for (i = 0; i < size; i++) {
    for (j = 0; j < dim; j++) {
      newError += abs(S[i][j] - roundc(covered[i][j]));
    }
  }
#ifndef STREAMLINE
  if (error != newError) {
    fprintf(stderr, "Warning: Something is wrong as errors do not match.\n"
	    "Old error is %lu but new error is %lu!\n", error, newError);
  }
#endif

  return newError;
}
