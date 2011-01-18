"""
This page is in the table of contents.
The csv.py script is an import translator plugin to get a carving from an csv file.

An import plugin is a script in the interpret_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an csv file and returns the carving.

This example gets a triangle mesh for the csv file boolean.csv.  This example is run in a terminal in the folder which contains boolean.csv and csv.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import csv
>>> csv.getCarving().getCarveRotatedBoundaryLayers()
[-1.159765625, None, [[(-18.925000000000001-2.4550000000000001j), (-18.754999999999981-2.4550000000000001j)
..
many more lines of the carving
..


An example of an csv boolean geometry format file follows below.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import archive
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import xml_simple_reader
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getCarving(fileName=''):
	"Get the carving for the csv file."
	csvText = archive.getFileText(fileName)
	if csvText == '':
		return None
	csvParser = CSVSimpleParser( fileName, None, csvText )
	lowerClassName = csvParser.getRoot().className.lower()
	pluginModule = archive.getModuleWithDirectoryPath( getPluginsDirectoryPath(), lowerClassName )
	if pluginModule == None:
		return None
	return pluginModule.getCarvingFromParser( csvParser )

def getLineDictionary(line):
	"Get the line dictionary."
	lineDictionary = {}
	splitLine = line.split('\t')
	for splitLineIndex in xrange( len(splitLine) ):
		word = splitLine[ splitLineIndex ]
		if word != '':
			lineDictionary[ splitLineIndex ] = word
	return lineDictionary

def getPluginsDirectoryPath():
	"Get the plugins directory path."
	return archive.getAbsoluteFrozenFolderPath( __file__, 'xml_plugins')


class CSVElement( xml_simple_reader.XMLElement ):
	"A csv element."
	def continueParsingObject( self, line, lineStripped ):
		"Parse replaced line."
		splitLineStripped = lineStripped.split('\t')
		key = splitLineStripped[0]
		value = splitLineStripped[1]
		self.attributeDictionary[key] = value
		self.addToIDDictionaryIFIDExists()

	def continueParsingTable( self, line, lineStripped ):
		"Parse replaced line."
		if self.headingDictionary == None:
			self.headingDictionary = getLineDictionary(line)
			return
		csvElement = self
		oldAttributeDictionaryLength = len( self.attributeDictionary )
		if oldAttributeDictionaryLength > 0:
			csvElement = CSVElement()
		csvElement.parent = self.parent
		csvElement.className = self.className
		lineDictionary = getLineDictionary(line)
		for columnIndex in lineDictionary.keys():
			if columnIndex in self.headingDictionary:
				key = self.headingDictionary[ columnIndex ]
				value = lineDictionary[ columnIndex ]
				csvElement.attributeDictionary[key] = value
		csvElement.addToIDDictionaryIFIDExists()
		if len( csvElement.attributeDictionary ) == 0 or oldAttributeDictionaryLength == 0 or self.parent == None:
			return
		self.parent.children.append( csvElement )

	def getElementFromObject( self, leadingTabCount, lineStripped, oldElement ):
		"Parse replaced line."
		splitLine = lineStripped.split('\t')
		self.className = splitLine[1]
		if leadingTabCount == 0:
			return self
		self.parent = oldElement
		while leadingTabCount <= self.parent.getNumberOfParents():
			self.parent = self.parent.parent
		self.parent.children.append(self)
		return self

	def getElementFromTable( self, leadingTabCount, lineStripped, oldElement ):
		"Parse replaced line."
		self.headingDictionary = None
		return self.getElementFromObject( leadingTabCount, lineStripped, oldElement )

	def getNumberOfParents(self):
		"Get the number of parents."
		if self.parent == None:
			return 0
		return self.parent.getNumberOfParents() + 1


class CSVSimpleParser( xml_simple_reader.XMLSimpleReader ):
	"A simple csv parser."
	def __init__( self, parent, csvText ):
		"Add empty lists."
		self.continueFunction = None
		self.extraLeadingTabCount = None
		self.lines = archive.getTextLines( csvText )
		self.oldCSVElement = None
		self.root = None
		for line in self.lines:
			self.parseLine(line)

	def getNewCSVElement( self, leadingTabCount, lineStripped ):
		"Get a new csv element."
		if self.root != None and self.extraLeadingTabCount == None:
			self.extraLeadingTabCount = 1 - leadingTabCount
		if self.extraLeadingTabCount != None:
			leadingTabCount += self.extraLeadingTabCount
		if lineStripped[ : len('_table') ] == '_table' or lineStripped[ : len('_t') ] == '_t':
			self.oldCSVElement = CSVElement().getElementFromTable( leadingTabCount, lineStripped, self.oldCSVElement )
			self.continueFunction = self.oldCSVElement.continueParsingTable
			return
		self.oldCSVElement = CSVElement().getElementFromObject( leadingTabCount, lineStripped, self.oldCSVElement )
		self.continueFunction = self.oldCSVElement.continueParsingObject

	def parseLine(self, line):
		"Parse a gcode line and add it to the inset skein."
		lineStripped = line.lstrip()
		if len( lineStripped ) < 1:
			return
		leadingPart = line[ : line.find( lineStripped ) ]
		leadingTabCount = leadingPart.count('\t')
		if lineStripped[ : len('_') ] == '_':
			self.getNewCSVElement( leadingTabCount, lineStripped )
			if self.root == None:
				self.root = self.oldCSVElement
				self.root.parser = self
			return
		if self.continueFunction != None:
			self.continueFunction( line, lineStripped )


def main():
	"Display the inset dialog."
	if len(sys.argv) > 1:
		getCarving(' '.join(sys.argv[1 :]))

if __name__ == "__main__":
	main()
