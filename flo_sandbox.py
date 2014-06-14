# transform z scores to p values
import numpy
from scipy import special

def getPValue(self, zScore):
    return 2*special.ndtr(numpy.absolute(zScore)*-1)

## this snippet to be put into runner.py...

#vectorized_function = numpy.vectorize(getPValue)
#matrix_p = vectorized_function([database.asMatrix()])
## test matrix
##matrix_p = vectorized_function([1, 0, 2])
##print matrix_p