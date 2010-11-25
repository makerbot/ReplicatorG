"""
Boolean geometry group of solids.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.geometry.solids import group
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.xml_simple_reader import XMLSimpleReader
from fabmetheus_utilities import archive
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
import cStringIO
import os


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getXMLFromCarvingFileName(fileName):
	"Get xml text from xml text."
	carving = fabmetheus_interpret.getCarving(fileName)
	if carving == None:
		return ''
	output = cStringIO.StringIO()
	carving.addXML( 0, output )
	return output.getvalue()

def processXMLElement(xmlElement):
	"Process the xml element."
	fileName = evaluate.getEvaluatedValue('file', xmlElement )
	if fileName == None:
		return
	parserFileName = xmlElement.getRoot().parser.fileName
	absoluteFileName = archive.getAbsoluteFolderPath( parserFileName, fileName )
	xmlText = ''
	if fileName.endswith('.xml'):
		xmlText = archive.getFileText( absoluteFileName )
	else:
		xmlText = getXMLFromCarvingFileName( absoluteFileName )
	if xmlText == '':
		print('The file %s could not be found in the folder which the fabmetheus xml file is in.' % fileName )
		return
	if '_importname' in xmlElement.attributeDictionary:
		xmlElement.importName = xmlElement.attributeDictionary['_importname']
	else:
		xmlElement.importName = archive.getUntilDot(fileName)
		xmlElement.attributeDictionary['_importname'] = xmlElement.importName
	XMLSimpleReader( parserFileName, xmlElement, xmlText )
	originalChildren = xmlElement.children[:]
	xmlElement.children = []
	for child in originalChildren:
		for subchild in child.children:
			subchild.setParentAddToChildren(xmlElement)
		for attributeDictionaryKey in child.attributeDictionary.keys():
			if attributeDictionaryKey != 'version':
				xmlElement.attributeDictionary[attributeDictionaryKey] = child.attributeDictionary[attributeDictionaryKey]
	group.processShape( group.Group, xmlElement)
