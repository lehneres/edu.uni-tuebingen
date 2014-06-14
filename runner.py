#!/usr/bin/env python

"""
Serves as test script to create a DataStructure from a SOFT file, translate the
contained Ensemble gene IDs to different database systems using BridgeDbClient
and retrieve the corresponding GO terms using goatools.
"""

from Fisher import FisherTester
import DataStructure
import os
import sys
#import numpy
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

        mapper = BridgeDbClient()
        mapper.remap(database, "EnHs", "L")     # Ensembl to Entrez
        mapper.remap(database, "EnHs", "S")     # Ensembl to UniProt
        mapper.remap(database, "EnHs", "T")     # Ensembl to GO term

        timepoint3 = time()
        print "remapping:\t" + str(timepoint3 - timepoint2)

        # dumping database object to file
        print "Pickling data to " + str(sys.argv[1]) + ".pkl"
        pickle.dump(database, open(sys.argv[1] + ".pkl", "wb"))

        timepoint4 = time()
        print "pickling:\t" + str(timepoint4 - timepoint3)

    print "running Fisher tests"
    gmt_entrez = "c2.cp.kegg.v3.1.entrez.gmt"
    gmt_go = "c5.all.v3.1.entrez.gmt"
    threshold = 0.05
    tester = FisherTester(database)
    print "... GO TERMS:"
    tester.test(gmt_go, threshold)
    print "... KEGG PATHWAYS:"
    tester.test(gmt_entrez, threshold)

    #print "writing CSV file for BiNA"
    #database.writeCsvFile(1)

if __name__ == "__main__":
    main()
