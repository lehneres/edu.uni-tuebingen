#!/usr/bin/env python

"""
wrapper for the bridgedb commandline tool. Translates list of gene identifiers
from one system to another. Can also be used for whole (ProbeSet) datasets.
"""

import DataStructure
import os
import sys

class BridgeDbClient:
    """
    wrapper class for the bridgedb commandline tool. Translates a list of
    identifiers from one system to an other. Can also be used for whole
    (ProbeSet) datasets.
    """
    def __init__(self):
        self.output_system = "U"
        self.input_system = "EnHs"
        self.map_db = "/share/data/bridgedb/Hs_Derby_20110601.bridge"
        self.__input = "input"
        self.__output = "output"
        self.database = None

    def setInputSystem(self, input_system):
        """
        sets the input system
        """
        self.input_system = input_system

    def setOutputSystem(self, output_system):
        """
        sets the output system
        """
        self.output_system = output_system

    def getInputSystem(self):
        """
        gets the input system
        """
        return self.input_system

    def getOutputSystem(self):
        """
        gets the output system
        """
        return self.output_system

    def setInput(self, id_list):
        """
        sets the input data. can be a list of ids or a dataset of ProbeSets
        """
        try:
            handle = open(self.__input, "w")
        except IOError, err:
            print "I/O error ({0}): {1}".format(err.errorno, err.strerror)
            sys.exit("exiting due to io error in BridgeDbClient-setInput")
        else:
            if isinstance(id_list, DataStructure.DataSet):
                self.database = id_list
                for probe in id_list.probe_set_list.itervalues():
                    for ens in probe.getGeneIds(self.input_system):
                        handle.write(ens + "\n")
            else:
                for gene_id in id_list:
                    handle.write(gene_id + "\n")
            handle.close()

    def getOutput(self):
        """
        parses the output file. returns a list of ids and enriches the dataset
        of ProbeSets if given.
        """
        converted = list()
        try:
            handle = open(self.__output, "r")
        except IOError, err:
            print "I/O error ({0}): {1}".format(err.errorno, err.strerror)
            sys.exit("exiting due to io error in BridgeDbClient-getOutput")
        else:
            if not self.database is None:
                for probe in self.database.probe_set_list.itervalues():
                    for i in range (len(probe.getGeneIds(self.input_system))):
                        line = handle.readline()
                        new_id = line.split("\t")[0]
                        if (len(new_id) > 0):
                            probe.addGeneIds(self.output_system, new_id)
                            converted.append(line.split("\t"))
            else:
                for line in handle:
                    converted.append(line.split("\t"))
            handle.close()

        # delete output file here:
        out_path = os.getcwd() + "/" + self.__output
        os.remove(out_path)

        return converted


    def run(self):
        """
        runs the bridge tool
        """
        call = "/share/opt/noarc/BridgeDB/bridgedb-1.1.0/batchmapper.sh "
        call += " -g " + self.map_db
        call += " -i " + os.getcwd() + "/" + self.__input
        call += " -o " + os.getcwd() + "/" + self.__output
        call += " -is " + self.input_system
        call += " -os " + self.output_system

        os.system(call)

        # delete input file here:
        in_path = os.getcwd() + "/" + self.__input
        os.remove(in_path)


    def remap(self, database, source, target):
        """
        small wrapper for the BridgeDbClient
        """
        
        self.setInputSystem(source)
        self.setOutputSystem(target)
        self.setInput(database)
        self.run()
        self.getOutput()
        
