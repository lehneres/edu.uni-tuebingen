#!/usr/bin/env python
"""
GeneSet class representing entries in KEGG GMT files as objects with title, URL
pointing to additional information and a list of Entrez gene IDs
"""


class GeneSet:
    def __init__(self, title, url, entrez_ids):
        """
        initialising GeneSet with title string, URL for additional information
        and list of Entrez IDs
        """
        self.title = title
        self.url = url
        self.entrez_ids = entrez_ids

    def __str__(self):
        """
        redefinition of the to-string-method
        """
        return "\n" + self.title + "\n" + self.url + "\n" + str(self.entrez_ids)

    def getTitle(self):
        """
        returns title of GeneSet
        """
        return self.title

    def setTitle(self, new_title):
        """
        sets title of GeneSet
        """
        self.title = new_title

    def getUrl(self):
        """
        gets URL of GeneSet metainfo
        """
        return self.url

    def setUrl(self, new_url):
        """
        sets URL of GeneSet metainfo
        """
        self.url = new_url

    def getEntrezIds(self):
        """
        gets list of Entrez gene IDs
        """
        return self.entrez_ids

    def addEntrezId(self, entrez_id):
        """
        adds Entrez gene ID to GeneSet
        """
        self.entrez_ids.add(entrez_id)
