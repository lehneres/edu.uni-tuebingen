#ifndef ITER_H
#define ITER_H
#include "utils.h"
#include "approx.h"
#include <limits.h>


/* Some handy macros to handle new "covered" matrix in "solve_basis()" */

#define roundc(A) ((A) == 0 ? 0 : 1)
#define rem_k(A,k) ((A) & ~(1 << k))
#define add_k(A,k) ((A) | (1 << k))

/* Maximum iterations, could as well be an input argument */
#define MAX_ITER 1000

/* Streamline */
#define STREAMLINE

/* Maximum number of basis vectors */
#define MAX_BASIS_SIZE ((int)sizeof(unsigned long) * CHAR_BIT)

unsigned long int
iterate(matrix S, int size, int dim, matrix B, int k, matrix decomp,
	imatrix covered, options *opti);
#endif
