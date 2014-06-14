#!/usr/bin/env python

"""
Defines classes for GEO SOFT datasets and probesets containing Affymetrix
microarray data. Unique SOFT IDs, expression values per sample, and lists of
Ensemble gene IDs are parsed with parseFile() and stored in a list of ProbeSet
objects.
"""

import re
import sys
import pandas
from pandas import DataFrame
from collections import defaultdict
import numpy
from scipy import special

class DataSet:
    """
    Contains list of ProbeSets and methods to parse them from a SOFT file
    """

    def __init__(self):
        self.probe_set_list = dict()

    def addProbeSet(self, id_value, probe_set):
        """ setter for a ProbeSet """
        self.probe_set_list[id_value] = probe_set

    def getProbeSet(self, id_value):
        """ getter for a ProbeSet """
        return self.probe_set_list[id_value]

    def printMe(self):
        """ Print all contents of DataSet instance """
        for gene_id in self.probe_set_list:
            print str(self.probe_set_list[gene_id])

    def getGoTerm(self, go_id):
        """ translate GO ID to GO term"""
        from goatools.obo_parser import GODag
        return GODag("/share/usr/PIBI1213/pibi3/goatools/gene_ontology.1_2.obo"\
        ).query_term(go_id)





    def parseFile(self, file_name):
        """ Parses a SOFT formatted file without using biopython """

        # open file:
        try:
            handle = open(file_name)
        except IOError, err:
            print "I/O error ({0}): {1}".format(err.errorno, err.strerror)
            sys.exit("exiting due to io-error...")

        # initialize:
        else:
            is_sample = False
            is_affy_sample = False
            is_affy_sample_table = False
            is_platform_gpl5175 = False
            is_platform_gpl5175_table = False
            row_count = 0
            sample = str()

        # loop through lines in file:
            for line in handle:

                # begin of platform table:
                if line.startswith("^PLATFORM = GPL5175"):
                    is_platform_gpl5175 = True
                if is_platform_gpl5175 and \
                   line.startswith("!platform_table_begin"):
                    is_platform_gpl5175_table = True
                elif is_platform_gpl5175_table:
                    if line.startswith("ID"):
                        continue
                    probe_line = line.split("\t")
                    try:
                        soft = probe_line[0]
                        ncbi = probe_line[1].split(",")
                        ensemble_ids = re.findall(r'ENSG\d+', probe_line[10])
                    except IndexError:
                        pass
                    self.addProbeSet(soft, ProbeSet(soft, ncbi, ensemble_ids))
                # end of table
                if "!platform_table_end" in line:
                    is_platform_gpl5175_table = False
                    is_platform_gpl5175 = False

                # begin of sample table:
                # first line contains sample ID
                if line.startswith("^SAMPLE = "):
                    match = re.search(r'= (.*)$', line)
                    sample = match.group(1)
                    is_sample = True
                # then within ~25 lines the Affymetrix platform ID
                if is_sample and line.startswith("!Sample_platform_id = GPL5175"):
                    is_affy_sample = True
                # then within another ~25 lines the number of rows in the table
                if is_affy_sample and line.startswith("!Sample_data_row_count ="):
                    match = re.search(r'= (\d+)$', line)
                    row_count = int(match.group(1))
                    # results in 156429 lines (17381 probe sets * 9 samples)
                    # print str(row_count) + "\t" + sample
                # soon followed by the actual table
                if is_affy_sample and line.startswith("!sample_table_begin"):
                    is_affy_sample_table = True
                # where we use only the specified number of rows and skip header
                elif is_affy_sample_table:
                    if row_count > 0 and (not "ID_REF" in line):
                        match = re.search(r'^(\d{7})\t(.*)$', line)
                        probe_set_id = match.group(1)
                        expression = match.group(2)
                        row_count = row_count - 1
                        # the magic line where everything comes together; should
                        # result in 156429 lines (17381 probe sets * 9 samples)
                        self.getProbeSet(probe_set_id).addExpressionValue(sample,
                            expression)
                        # print probeSetList[probe_set_id]
                        # print str(row_count) + "\t" + sample + "\t" + \
                        #   probe_set_id + "\t" + expression
                if "!sample_table_end" in line:
                    is_affy_sample_table = False
                    is_affy_sample = False
                    is_sample = False
        handle.close()

        print "sweeping untested genesets from dataframe"
        self.sweepZeroExpression()


    def sweepZeroExpression(self):
        """ delete all Probes without Expression entry"""

        k_list = self.probe_set_list.keys()
        for key in k_list:
            if not self.probe_set_list[key].expression:
                del self.probe_set_list[key]



    def asTable(self, idx=-1):
        """ generate and return a DataFrame object of the current database"""

        # ToDo: auto-generate header list from file
        header_list = ["soft_id",
                       "geneOntology",
                       "uniprot",
                       "ensemble human",
                       "entrez gene",
                       "ncbi",
                       "GSM433776",
                       "GSM433777",
                       "GSM433778",
                       "GSM433779",
                       "GSM433780",
                       "GSM433781",
                       "GSM433782",
                       "GSM433783",
                       "GSM433784"]

        probe_content = []
        df_index = []

        # get the index for the new dataframe (softids)
        for probe in self.probe_set_list.itervalues():
            probe_content.append(probe.getAsDict(idx))
            df_index.append(probe.soft_id)

        # create dataframe
        dframe = DataFrame(probe_content, index = df_index, columns = header_list)



        return dframe



    def writeCsvFile(self, idx = -1):
        """
        write the current database to a csv file
        """
        dframe = self.asTable(idx)
        dframe.to_csv("./data_out.csv")


    def asMatrix(self):
        """
        get a Matrix of the Xpression data
        only use rows where an expression is
        2fold higher/lower than the average
        """
        table_df = self.asTable()

        sample_list = [elem for elem in table_df.columns if elem.startswith("GSM")]

        # create table filter_df from table_df
        # use only rows from sample_list:
        filter_df = table_df.copy()[sample_list]

        # calculate how many times Xpr is higher/lower than mean:
        # (the given xpr-values are already log2(xpr) )
        for col in sample_list:
            filter_df["fold_" + col] = numpy.absolute(table_df.mean(1) - table_df[col])

        fold_list = ["fold_" + elem for elem in sample_list]

        # select only the fold entries:
        filter_df = filter_df[fold_list]
        # keep only probes that have at least 2 times xpr of mean:
        filter_df = filter_df[filter_df.max(1) > 1]
        # retrun:
        # only rows that are also in filter_df(soft_id_index)
        # and delete the fold entries
        matrix = pandas.merge(table_df, filter_df, left_index = True, right_index = True)

        return matrix


    def getZmatrix(self):
        """
        return a matrix of Z-scores for every
        Xpression value entry
        """
        matrix = self.asMatrix()
        sample_list = [elem for elem in matrix.columns if elem.startswith("GSM")]
        matrix = matrix[sample_list]

        # get z-scores:
        temp = list()
        for col in matrix.columns:
            temp.append( (col, numpy.abs( (matrix[col] - matrix.mean(1)) / matrix.std(1) )))
        z_matrix = DataFrame.from_items(temp)
        return z_matrix


    def getPValue(self, z_score):
        """
        calculates p-value from z-score
        """
        return 2 * special.ndtr(numpy.absolute(z_score) * -1)


class ProbeSet:
    """
    Contains DataSet-specific unique SOFT_IDs, the associated gene IDs per
    database and the Affymetrix microarray expression values per sample (tissue)
    """

    def __init__(self, soft, ncbi, ensemble):
        """ Constructor for ProbeSet"""
        self.soft_id = soft
        self.gene_ids = defaultdict(set)

        self.gene_ids["ncbi"] = set(ncbi)
        self.gene_ids["EnHs"] = set(ensemble)
        self.expression = dict()


    def addExpressionValue(self, tissue, value):
        """ extends uniquily the dict of expression values
        """
        self.expression[tissue] = float(value)


    def getExpressionValue(self, tissue):
        """ getter for the expression value
        for a specific sample
        """
        return self.expression[tissue]


    def addGeneIds(self, database, gene_id):
        """ extend the dict for corresponding gene ids for
        this soft_id
        """
        self.gene_ids[database].add(gene_id)


    def getAsDict(self, idx=-1):
        """
        get DataFrame as dict with all IDs up to index (default: all)
        """

        if idx < 0:
            go_id   = ";".join(self.gene_ids['T'   ])
            uni_id  = ";".join(self.gene_ids['S'   ])
            entr_id = ";".join(self.gene_ids['L'   ])
            ens_id  = ";".join(self.gene_ids['EnHs'])
            ncbi_id = ";".join(self.gene_ids[''    ])
        else:
            go_id   = ";".join(list(self.gene_ids['T'   ])[:idx])
            uni_id  = ";".join(list(self.gene_ids['S'   ])[:idx])
            entr_id = ";".join(list(self.gene_ids['L'   ])[:idx])
            ens_id  = ";".join(list(self.gene_ids['EnHs'])[:idx])
            ncbi_id = ";".join(list(self.gene_ids[''    ])[:idx])

        soft = {"soft_id": self.soft_id ,
                "geneOntology": go_id,
                "uniprot": uni_id,
                "ensemble human": ens_id,
                "entrez gene": entr_id,
                "ncbi": ncbi_id}

        soft.update(dict(self.expression))

        return soft


    def getGeneIds(self, database):
        """ return the list of UNIQUE geneids of a probe
        for a given database"""
        return self.gene_ids[database]

    def __str__(self):
        """ redefinition of the to-string-method"""
        return self.soft_id + " " + str(self.expression) + "\n" \
             + self.soft_id + " " + str(self.gene_ids)
