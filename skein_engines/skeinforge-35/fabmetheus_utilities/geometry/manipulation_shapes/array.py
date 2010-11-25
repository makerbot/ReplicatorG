"""
Boolean geometry array.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.geometry.geometry_tools import vertex
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import euclidean


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


globalExecutionOrder = 200


def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	"Get array path."
	arrayPaths = evaluate.getTransformedPathsByKey([prefix + 'path', prefix + 'paths'], xmlElement)
	manipulatedByPaths = []
	for arrayPath in arrayPaths:
		for arrayPoint in arrayPath:
			manipulatedByPath = []
			for point in loop:
				manipulatedByPath.append(point + arrayPoint)
			manipulatedByPaths.append(manipulatedByPath)
	manipulatedByVertexes = []
	vertexes = getVertexesByKey(prefix + 'vertexes', xmlElement)
	for vertex in vertexes:
		manipulatedByVertex = []
		for point in loop:
			manipulatedByVertex.append(point + vertex)
		manipulatedByVertexes.append(manipulatedByVertex)
	manipulatedPaths = manipulatedByPaths + manipulatedByVertexes
	if len(manipulatedPaths) == 0:
		print('Warning, in getManipulatedPaths in array there are no paths or vertexes for:')
		print(xmlElement)
		return [loop]
	return manipulatedPaths

def getVertexesByKey(key, xmlElement):
	"Get the vertexes by key."
	return euclidean.getConcatenatedList(evaluate.getTransformedPathsByKey(key, xmlElement))

def processXMLElement(xmlElement):
	"Process the xml element."
	target = evaluate.getXMLElementByKey('target', xmlElement)
	if target == None:
		print('Warning, array could not get target for:')
		print(xmlElement)
		return
	vertexes = getVertexesByKey('vertexes', xmlElement)
	if len(vertexes) == 0:
		print('Warning, array could not get vertexes for:')
		print(xmlElement)
		return
	arrayDictionary = xmlElement.attributeDictionary.copy()
	targetMatrixCopy = matrix.getFromObjectOrXMLElement(target)
	matrix.setAttributeDictionaryToMatrix(target.attributeDictionary, targetMatrixCopy)
	xmlElement.className = 'group'
	for vector3Index in xrange(len(vertexes)):
		vector3 = vertexes[vector3Index]
		vector3Matrix = matrix.Matrix(targetMatrixCopy.matrixTetragrid)
		lastChild = target.getCopy(xmlElement.getIDSuffix(vector3Index), xmlElement)
		euclidean.overwriteDictionary(xmlElement.attributeDictionary, ['id'], ['visible'], lastChild.attributeDictionary)
		vertexElement = vertex.getUnboundVertexElement(vector3)
		matrix.setXMLElementDictionaryToOtherElementDictionary(vertexElement, vector3Matrix, 'matrix.', lastChild)
	xmlElement.getXMLProcessor().processXMLElement(xmlElement)
