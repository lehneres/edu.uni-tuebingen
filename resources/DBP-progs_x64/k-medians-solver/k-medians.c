/* k-medians.c
 *
 * k-medians clustering with local search.
 *
 * Pauli Miettinen
 * 7.7.2005
 *
 * Last modified
 * 16.5.2007
 */

#include "approx.h"
#include "utils.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define MAX(a, b) ((a > b) ? a : b)
#define MIN(a, b) ((a > b) ? b : a)
#define ALKIO(s, a, b) (s[(((a) > (b)) ? (a) : (b))][(((a) > (b)) ? (b) : (a))])

typedef struct medians {
  unsigned int *centers;  /* k-vector with center numbers */
  unsigned int *codes;    /* n-vector with mappings to centers */
  unsigned int cost;      /* this solutions' cost */
} medians;


unsigned int **
calculate_distance(matrix S, int size, int dim);

medians *
k_median(int points, int k, unsigned int **distances, unsigned int seed,
	 unsigned int verbose);

void
cluster(medians *S, unsigned int **distances, int points, int k,
	unsigned int verbose);

int
approximate(matrix S, int size, int dim, matrix B, int k, matrix O,
	    options *opti)
{
  int i, j, b, ones, cols;
  unsigned int **distances;
  medians *result;

  distances = calculate_distance(S, size, dim);
  if (distances == NULL) 
    return 0;
  
  /* Verbose */
  if (opti->verbose > 0) {
    fprintf(stderr, "Distance matrix:\n");
    for (i = 0; i < dim; i++) {
      for (j = 0; j <= i; j++) {
	fprintf(stderr, "%i ", distances[i][j]);
      }
      fprintf(stderr, "\n");
    }
    fprintf(stderr, "\n");
  }

  result = k_median(dim, k, distances, opti->seed, opti->verbose);
  if (result == NULL) 
    return 0;

  /* convert result->codes to matrix B */
  for (j = 0; j < dim; j++) {
    B[result->codes[j]][j] = 1;
  }

  if (!opti->majority) {
    /* convert result->centers to matrix O */
    for (i = 0; i < size; i++) {
      for (j = 0; j < k; j++) {
	O[i][j] = S[i][result->centers[j]];
      }
    }
  } else {
    /* set the column in matrix O to be coordinate-wise majority
     * of cluster points. */
    for (i = 0; i < size; i++) {
      for (b = 0; b < k; b++) {
	cols = 0;
	ones = 0;
	for (j = 0; j < dim; j++) {
	  cols += B[b][j];
	  ones += S[i][j]*B[b][j];
	}
	O[i][b] = (ones > cols/2) ? 1 : 0;
      }
    }
  }

  return 1;
}

unsigned int **
calculate_distance(matrix S, int size, int dim)
{
  int i, j, d;
  unsigned int **distances;

  /* Allocate distance matrix for S's transpose*/
  distances = (unsigned int **)malloc(dim * sizeof(unsigned int *));
  if (distances == NULL) {
    perror("Error while allocating space for the distance matrix");
    return NULL;
  }

  /* Distance matrix is actually a lower triangle matrix */
  for (i = 0; i < dim; i++) {
    distances[i] = (unsigned int *)malloc((i+1) * sizeof(unsigned int));
    if (distances[i] == NULL) {
      perror("Error while allocating space for the distance matrix");
      return NULL;
    }
  }

  /* Calculate distances in L1-metric */
  for (i = 0; i < dim; i++) {
    for (j = 0; j <= i; j++) {
      if (i == j) {
	distances[i][j] = 0;
      } else {
	distances[i][j] = 0;
	for (d = 0; d < size; d++)
	  distances[i][j] += abs(S[d][i] - S[d][j]);
      }
    }
  }

  return distances;
}
      
medians *
k_median(int points, int k, unsigned int **distances, unsigned int seed,
	 unsigned int verbose)
{
  int i, j;
  medians *S, S_prime, S_temp;
  char *in_centers, improved;
  FILE *randdev;

  /* BEGIN space allocation */
  S = (medians *)malloc(sizeof(medians));
  if (S == NULL) {
    perror("Error while allocating space for initial result");
    return NULL;
  }

  S->centers = (unsigned int *)malloc(k * sizeof(unsigned int));
  if (S->centers == NULL) {
    perror("Error while allocating space for initial centers");
    return NULL;
  }

  S->codes = (unsigned int *)malloc(points * sizeof(unsigned int));
  if (S->codes == NULL) {
    perror("Error while allocating space for initial codes");
    return NULL;
  }

  S_prime.centers = (unsigned int *)malloc(k * sizeof(unsigned int));
  if (S_prime.centers == NULL) {
    perror("Error while allocating space for swap centers");
    return NULL;
  }

  S_prime.codes = (unsigned int *)malloc(points * sizeof(unsigned int));
  if (S_prime.codes == NULL) {
    perror("Error while allocating space for swap codes");
    return NULL;
  }

  S_temp.centers = (unsigned int *)malloc(k * sizeof(unsigned int));
  if (S_temp.centers == NULL) {
    perror("Error while allocating space for temp centers");
    return NULL;
  }

  S_temp.codes = (unsigned int *)malloc(points * sizeof(unsigned int));
  if (S_temp.codes == NULL) {
    perror("Error while allocating space for temp codes");
    return NULL;
  }

  in_centers = (char *)malloc(points * sizeof(char));
  if (in_centers == NULL) {
    perror("Error while allocating space for in_centers");
    return NULL;
  }
  /* END space allocation */

  /* Make initial random quess */
  randdev = init_seed(seed); /* init random device/generator */
  for (i = 0; i < k; i++) {
    S->centers[i] = give_rand(randdev) % points;
    for (j = 0; j < i; j++) { 
      /* a naive method to ensure that we get different centers */
      if (S->centers[j] == S->centers[i]) 
	i--;
    }
  }

  cluster(S, distances, points, k, verbose);

  /* Verbose */
  if (verbose > 0) {
    fprintf(stderr, "Initial quess:\n");
    fprintf(stderr, "  Centers: ");
    for (i = 0; i < k; i++) {
      fprintf(stderr, "%i ", S->centers[i]);
    }
    fprintf(stderr, "\n  Codes: ");
    for (i = 0; i < points; i++) {
      fprintf(stderr, "%i ", S->codes[i]);
    }
    fprintf(stderr, "\n  Cost: %i\n\n", S->cost);
  }

  /* begin local search */
  do {
    /* Find best swap <s,s'> */
    memcpy(S_prime.centers, S->centers, k * sizeof(unsigned int));
    memcpy(S_prime.codes, S->codes, points * sizeof(unsigned int));
    S_prime.cost = S->cost;
    memset(in_centers, 0, points);
    for (i = 0; i < k; i++)
      in_centers[S_prime.centers[i]] = 1;

    /* verbosity */
    if (verbose > 0) {
      fprintf(stderr, "Local search step.\n");
      fprintf(stderr, "  in_centers: ");
      for (i = 0; i < points; i++) 
	fprintf(stderr, "%i ", in_centers[i]);
      fprintf(stderr, "\n  Thus far best cost: %i\n\n", S->cost);
    }
    
    /* loop thru all centers */
    for (i = 0; i < k; i++) {
      /* loop thru all points */
      for (j = 0; j < points; j++) {
	/* if point j is not already some center */
	if (!in_centers[j]) {
	  /* swap center i with point j <==> <i, j> */
	  memcpy(S_temp.centers, S->centers, k * sizeof(unsigned int));
	  S_temp.centers[i] = j;
	  cluster(&S_temp, distances, points, k, verbose);

	  /* if <i, j> is better than the best thus far founded */
	  if (S_temp.cost < S_prime.cost) {
	    /* S_prime <- S_temp */
	    memcpy(S_prime.centers, S_temp.centers, k * sizeof(unsigned int));
	    memcpy(S_prime.codes, S_temp.codes, points * sizeof(unsigned int));
	    S_prime.cost = S_temp.cost;
	  }
	}
      }
    }

    /* If we have got any better solution, change to it */
    if (S_prime.cost < S->cost) {
      improved = 1;
      memcpy(S->centers, S_prime.centers, k * sizeof(unsigned int));
      memcpy(S->codes, S_prime.codes, points * sizeof(unsigned int));
      S->cost = S_prime.cost;
    } else {
      improved = 0;
    }
  } while (improved); /* loop while we got any better solution */
  /* End local search */

  /* Verbose */
  if (verbose > 0) {
    fprintf(stderr, "Final answer:\n");
    fprintf(stderr, "  Centers: ");
    for (i = 0; i < k; i++) {
      fprintf(stderr, "%i ", S->centers[i]);
    }
    fprintf(stderr, "\n  Codes: ");
    for (i = 0; i < points; i++) {
      fprintf(stderr, "%i ", S->codes[i]);
    }
    fprintf(stderr, "\n  Cost: %i\n\n", S->cost);
  }

  return S;
}

void
cluster(medians *S, unsigned int **distances, int points, int k,
	unsigned int verbose)
{
  int i, j, nearest;

  S->cost = 0;

  /* Verbose */
  if (verbose > 1)
    fprintf(stderr, "cluster() function\n");

  /* cluster each point to nearest center */
  for (i = 0; i < points; i++) {
    nearest = 0;
    for (j = 1; j < k; j++) {
      if (ALKIO(distances, i, S->centers[nearest])
	  >= ALKIO(distances, i, S->centers[j]))
	nearest = j;
    }
    S->codes[i] = nearest;
    S->cost += ALKIO(distances, i, S->centers[nearest]);
    
    /* Verbose */
    if (verbose > 1) {
      fprintf(stderr, "  Point %i is clustered to point %i.\n", i, S->centers[nearest]);
      fprintf(stderr, "  Distance from point to center is %i.\n", 
	      ALKIO(distances, i, S->centers[nearest]));
      fprintf(stderr, "  Current cost is %i.\n\n", S->cost);
    }
  }
}

void
approx_help()
{
  printf("-d, --seed=n\n"
	 "\t Seed for initializing random number generator. If 0 (default),\n"
	 "\t uses special random device.\n"
	 "-v, --verbose\n"
	 "\t Verbosity level; more v's means more verbosity.\n\n");
}
