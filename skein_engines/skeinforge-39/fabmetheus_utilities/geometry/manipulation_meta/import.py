"""
Boolean geometry group of solids.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.geometry.solids import group
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import xml_simple_reader
from fabmetheus_utilities import xml_simple_writer
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
import cStringIO
import os


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GPL 3.0'


def getXMLFromCarvingFileName(fileName):
	'Get xml text from xml text.'
	carving = fabmetheus_interpret.getCarving(fileName)
	if carving == None:
		return ''
	output = xml_simple_writer.getBeginGeometryXMLOutput()
	carving.addXML(0, output)
	return xml_simple_writer.getEndGeometryXMLString(output)

def processXMLElement(xmlElement):
	'Process the xml element.'
	fileName = evaluate.getEvaluatedValue('file', xmlElement )
	if fileName == None:
		return
	parserFileName = xmlElement.getRoot().parser.fileName
	absoluteFileName = archive.getAbsoluteFolderPath(parserFileName, fileName)
	absoluteFileName = os.path.abspath(absoluteFileName)
	if 'models/' not in absoluteFileName:
		print('Warning, models/ was not in the absolute file path, so for security nothing will be done for:')
		print(xmlElement)
		print('For which the absolute file path is:')
		print(absoluteFileName)
		print('The import tool can only read a file which has models/ in the file path.')
		print('To import the file, move the file into a folder called model/ or a subfolder which is inside the model folder tree.')
		return
	xmlText = ''
	if fileName.endswith('.xml'):
		xmlText = archive.getFileText(absoluteFileName)
	else:
		xmlText = getXMLFromCarvingFileName(absoluteFileName)
	print('The import tool is opening the file:')
	print(absoluteFileName)
	if xmlText == '':
		print('The file %s could not be found by processXMLElement in import.' % fileName)
		return
	if '_importName' in xmlElement.attributeDictionary:
		xmlElement.importName = xmlElement.attributeDictionary['_importName']
	else:
		xmlElement.importName = archive.getUntilDot(fileName)
		if evaluate.getEvaluatedBooleanDefault(True, 'basename', xmlElement):
			xmlElement.importName = os.path.basename(xmlElement.importName)
		xmlElement.attributeDictionary['_importName'] = xmlElement.importName
	importXMLElement = xml_simple_reader.XMLElement()
	xml_simple_reader.XMLSimpleReader(parserFileName, importXMLElement, xmlText)
	for child in importXMLElement.children:
		child.copyXMLChildren('', xmlElement)
		euclidean.removeElementsFromDictionary(child.attributeDictionary, ['id', 'name'])
		xmlElement.attributeDictionary.update(child.attributeDictionary)
		if evaluate.getEvaluatedBooleanDefault(False, 'overwriteRoot', xmlElement):
			xmlElement.getRoot().attributeDictionary.update(child.attributeDictionary)
	group.processShape(group.Group, xmlElement)
