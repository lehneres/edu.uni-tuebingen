#!/usr/bin/env python
from scipy import stats
import numpy
import GeneSetParser
#import GeneSet
#import sys
#from operator import itemgetter

class FisherTester:
    """
    Tests Affymetrix DataSet using Fisher test with p threshold, calculating
    z-scores and p-values on the fly
    """
    def __init__(self, dataset):
        self.database = dataset

    def test(self, gmt_file, threshold, db_type = "L"):
        """
        parsing GMT file into list of GeneSets, calculating z scores from
        DataSet, transforming z score matrix into p value matrix, filtering all
        significant p values, generating contingency matrices and applying
        Fisher's exact test.
        """
        parser = GeneSetParser.GeneSetParser(gmt_file)
        genesets = parser.all()
        # generate z-score matrix and reduce to tissue-specific expression
        # values using scipy
        matrix = self.database.asMatrix()
        # to retrieve list of samples/tissues
        tissue_list = [elem for elem in matrix.columns if elem.startswith("GSM")]

        ## alternative 1
        #matrix = matrix[tissue_list]
        #matrix_z = stats.zscore(matrix, axis = 1)
        ## alternatively 2
        matrix_z = self.database.getZmatrix()

        print "writing z-scores to CSV file for R heatmap generation"
        #numpy.savetxt("matrix_z.csv", matrix_z, delimiter = ",")    # when using scipy
        matrix_z.to_csv("matrix_z.csv") # when using our zscore method

        # generate p-value matrix
        vectorized_function = numpy.vectorize(self.database.getPValue)
        matrix_p = vectorized_function(matrix_z)
        ## just for debugging
        # numpy.savetxt("matrix_p", matrix_p, delimiter = ",")

        # create boolean matrix of significant (True) and insignificant (False)
        # p values
        vectorized_filter = numpy.vectorize(self.isSignificant)
        matrix_p_significant = vectorized_filter(matrix_p, threshold)
        ## just for debugging
        # numpy.savetxt("matrix_p_sig", matrix_p_significant, delimiter = ",")

        ## the actual counting routine that generates the contingency matrices
        gene_id_list = self.getIds(db_type, self.database.asMatrix())
        # for debugging
        print len(gene_id_list)
        print len(tissue_list)
        
        fisher_result_dict = dict()
         # iterate over tissue indices (dimension j)
        for tissue_idx in range(len(tissue_list)):
            tissue_name = tissue_list[tissue_idx]
            # iterate over all genesets from GMT file (dimension i)
            temp_list = list()
            for geneset in genesets:
                title = geneset.getTitle()
                # iterate over probe indices (counts)
                counts = [0, 0, 0, 0]

                for probe_idx in range(matrix_p_significant.shape[0]):
                    # print sorted(set(gene_id_list[probe_idx]))
                    # print sorted(set(geneset.getEntrezIds()))
                    contained_in_geneset = self.doesIntersect(  \
                            gene_id_list[probe_idx], geneset.getEntrezIds())
                    # case a
                    if matrix_p[probe_idx][tissue_idx] < 0.05 \
                       and contained_in_geneset:
                        counts[0] = counts[0] + 1
                    # case b
                    elif (not matrix_p[probe_idx][tissue_idx] < 0.05) \
                         and contained_in_geneset:
                        counts[1] = counts[1] + 1
                    # case c
                    elif matrix_p[probe_idx][tissue_idx] < 0.05 \
                         and (not contained_in_geneset):
                        counts[2] = counts[2] + 1
                    # case d
                    elif (not matrix_p[probe_idx][tissue_idx] < 0.05) \
                         and (not contained_in_geneset):
                        counts[3] = counts[3] + 1
                    else:
                        raise Exception("unmatched case in contingency matrix")

                ## print contingency matrix for debugging
                #print str(counts) + "\t" + tissue_list[tissue_idx] \
                                  #+ "\t" + title

                # call fisher test
                p_val = stats.fisher_exact([ counts[0:2], counts[2:4] ], alternative = 'less')[1]

                # print title, p_val
                temp_list.append((title , p_val))
            # create dictionary of lists of tuples
            fisher_result_dict[ tissue_name ] = temp_list

        topList = self.topOverAll(fisher_result_dict)
        for ind in range(30):
            print topList[ind]

        # geordnete Liste drucken
        sorted_dic = self.sortByPvalue(fisher_result_dict)
        for key in sorted_dic.keys():
            top_l = sorted_dic[key]
            for i in range(10):
                print key, top_l[i]


    def topOverAll(self,in_dic):

        lst_temp = list()

        for key in in_dic.keys():

            for entr in in_dic[key]:
                lst_temp.append((key, entr[0].ljust(50)[:50], entr[1]))

        lst_temp.sort(key = lambda tup: tup[2])
        return lst_temp


    def sortByPvalue(self, fisher_dic):
        """
        takes Fisher test output (dictionary of dictionaries) and returns list
        ordered by last dictionary value (p value)
        """
        for key in fisher_dic.keys():
            p_list = fisher_dic[key]
            p_list.sort(key = lambda tup: tup[1], reverse = False)
            fisher_dic[key] = p_list

        return fisher_dic


    def isSignificant(self, p_value, threshold):
        """
        very simple filter function for p values, considers p_value below
        threshold as significant, returns boolean
        to be vectorized and applied to entire p value matrix
        """
        return p_value < threshold


    def doesIntersect(self, list1, list2):
        """
        returns True if any element from list1 is also contained in list2
        """
        intersection = list(set(list1) & set(list2))
        if len(intersection) > 0:
            return True
        else:
            return False


    def getIds(self, db_type, matrix_df):
        """
        returns a list of lists that contain gene ids of
        the GeneDatabank type defined by the string db_type.
        The list corresponds with the rows of the
        dataframe object matrix_df
        """

        type_ids = []

        for single_id in matrix_df.index:
            probe = self.database.probe_set_list[single_id]
            type_ids.append(list(probe.getGeneIds(db_type)))

        return type_ids
