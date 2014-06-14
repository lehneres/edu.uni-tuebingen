#!/usr/bin/env python

"""
Serves as test script to create a DataStructure from a SOFT file, translate the
contained Ensemble gene IDs to different database systems using BridgeDbClient
and retrieve the corresponding GO terms using goatools.
"""

import DataStructure
import os
import sys
import numpy
from BridgeDbClient import BridgeDbClient

try:
    import cPickle as pickle
except:
    import pickle


def main():
    """
    For testing of the SOFT parser in DataStructure and the remapping
    functionality in BridgeDbClient
    """

    # timing optimization
    from time import time
    timepoint1 = time()

    database = DataStructure.DataSet()
    if os.path.isfile(sys.argv[1] + ".pkl"):
        print "Using pickled data from " + str(sys.argv[1]) + ".pkl"
        database = pickle.load(open(sys.argv[1] + ".pkl", "rb"))
        timepoint2 = time()
        print "unpickling:\t" + str(timepoint2 - timepoint1)
    else:
        print "Parsing data from " + str(sys.argv[1])
        database.parseFile(sys.argv[1])

        timepoint2 = time()
        print "parsing:\t" + str(timepoint2 - timepoint1)

        # mapping from Ensemble (EnHs) to Entrez (L), UniProt (S) and GOTerm (T)
        mapper = BridgeDbClient()
        mapper.remap(database, "EnHs", "L")
        mapper.remap(database, "EnHs", "S")
        mapper.remap(database, "EnHs", "T")

        timepoint3 = time()
        print "remapping:\t" + str(timepoint3 - timepoint2)

        # dumping database object to file
        print "Pickling data to " + str(sys.argv[1]) + ".pkl"
        pickle.dump(database, open(sys.argv[1] + ".pkl", "wb"))

        timepoint4 = time()
        print "pickling:\t" + str(timepoint4 - timepoint3)

    df = database.asMatrix()

    df["geneOntology"] = df["geneOntology"].map(lambda x: x.split(";")[0] if len(x) > 0  else numpy.nan)
    df = df.dropna(subset = ["geneOntology"], how = "any")

    df.set_index("geneOntology", inplace = True)

    df.to_csv("./gsea_go.txt", sep = "\t", index_label = "Name", cols = [elem for elem in df.columns if elem.startswith("GSM")])
    
if __name__ == "__main__":
    main()
