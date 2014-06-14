#!/usr/bin/env python
"""
Parser for KEGG GMT files with biologically relevant genesets
based on CSV reader
"""

import csv
import os
import GeneSet

class GeneSetParser:
    """
    parsing a GMT file
    """

    def __init__(self, gmt_filename):
        """
        constructor for reader object, expects path to GMT file
        """
        if os.path.isfile(gmt_filename) and gmt_filename.endswith(".gmt"):
            try:
                handle = open(gmt_filename, 'rb')
            except IOError, err:
                print "I/O error({0}): {1}".format(err.errorno, err.strerror)

            self.reader = csv.reader(handle, delimiter='\t')
        else:
            print "failure to read GMT file: " + gmt_filename + "\n"

    def next(self):
        """
        returns next GeneSet in GMT file
        """
        items = self.reader.next()
        return GeneSet.GeneSet(items[0], items[1], items[2:])

    def all(self):
        """
        returns all GeneSets in GMT file as list
        """
        geneset_list = []
        for row in self.reader:
            geneset = GeneSet.GeneSet(row[0], row[1], row[2:])
            geneset_list.append(geneset)
        return geneset_list


def main():
    """
    for testing the parser
    """
    parser = GeneSetParser("/share/usr/PIBI1213/data/gene_sets/c2.cp.kegg.v3.1.entrez.gmt")
    print parser.next()
    geneset = parser.next()
    print geneset

    genesets = parser.all()
    for gset in genesets:
        print gset

if __name__ == "__main__":
    main()
