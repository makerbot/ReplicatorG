"""
Solid.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_tools import path
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.geometry_utilities import matrix
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


def getGeometryOutput(derivation, elementNode):
	'Get geometry output from paths.'
	if derivation == None:
		derivation = SolidDerivation(elementNode)
	geometryOutput = []
	for path in derivation.target:
		sideLoop = SideLoop(path)
		geometryOutput += getGeometryOutputByLoop(elementNode, sideLoop)
	return geometryOutput

def getGeometryOutputByArguments(arguments, elementNode):
	'Get triangle mesh from attribute dictionary by arguments.'
	return getGeometryOutput(None, elementNode)

def getGeometryOutputByFunction(elementNode, geometryFunction):
	'Get geometry output by manipulationFunction.'
	if elementNode.xmlObject == None:
		print('Warning, there is no object in getGeometryOutputByFunction in solid for:')
		print(elementNode)
		return None
	geometryOutput = elementNode.xmlObject.getGeometryOutput()
	if geometryOutput == None:
		print('Warning, there is no geometryOutput in getGeometryOutputByFunction in solid for:')
		print(elementNode)
		return None
	return geometryFunction(elementNode, geometryOutput, '')

def getGeometryOutputByManipulation(elementNode, geometryOutput):
	'Get geometryOutput manipulated by the plugins in the manipulation shapes & solids folders.'
	xmlProcessor = elementNode.getXMLProcessor()
	matchingPlugins = getSolidMatchingPlugins(elementNode)
	matchingPlugins.sort(evaluate.compareExecutionOrderAscending)
	for matchingPlugin in matchingPlugins:
		prefix = matchingPlugin.__name__.replace('_', '') + '.'
		geometryOutput = matchingPlugin.getManipulatedGeometryOutput(elementNode, geometryOutput, prefix)
	return geometryOutput

def getNewDerivation(elementNode):
	'Get new derivation.'
	return SolidDerivation(elementNode)

def getSolidMatchingPlugins(elementNode):
	'Get solid plugins in the manipulation matrix, shapes & solids folders.'
	xmlProcessor = elementNode.getXMLProcessor()
	matchingPlugins = evaluate.getMatchingPlugins(elementNode, xmlProcessor.manipulationMatrixDictionary)
	return matchingPlugins + evaluate.getMatchingPlugins(elementNode, xmlProcessor.manipulationShapeDictionary)

def processArchiveRemoveSolid(elementNode, geometryOutput):
	'Process the target by the manipulationFunction.'
	solidMatchingPlugins = getSolidMatchingPlugins(elementNode)
	if len(solidMatchingPlugins) < 1:
		elementNode.parentNode.xmlObject.archivableObjects.append(elementNode.xmlObject)
		return
	processElementNodeByGeometry(elementNode, getGeometryOutputByManipulation(elementNode, geometryOutput))
	elementNode.removeFromIDNameParent()
	matrix.getBranchMatrixSetElementNode(elementNode)

def processElementNodeByFunction(elementNode, manipulationFunction):
	'Process the xml element.'
	if 'target' not in elementNode.attributes:
		print('Warning, there was no target in processElementNodeByFunction in solid for:')
		print(elementNode)
		return
	target = evaluate.getEvaluatedLinkValue(elementNode, str(elementNode.attributes['target']).strip())
	if target.__class__.__name__ == 'ElementNode':
		manipulationFunction(elementNode, target)
		return
	path.convertElementNode(elementNode, target)
	manipulationFunction(elementNode, elementNode)

def processElementNodeByFunctions(elementNode, geometryFunction, pathFunction):
	'Process the xml element by the appropriate manipulationFunction.'
	targets = evaluate.getElementNodesByKey(elementNode, 'target')
	for target in targets:
		processTargetByFunctions(geometryFunction, pathFunction, target)

def processElementNodeByGeometry(elementNode, geometryOutput):
	'Process the xml element by geometryOutput.'
	if geometryOutput == None:
		return
	elementNode.getXMLProcessor().convertElementNode(elementNode, geometryOutput)

def processTargetByFunctions(geometryFunction, pathFunction, target):
	'Process the target by the manipulationFunction.'
	if target.xmlObject == None:
		return
	if len(target.xmlObject.getPaths()) > 0:
		lineation.processTargetByFunction(pathFunction, target)
		return
	geometryOutput = getGeometryOutputByFunction(target, geometryFunction)
	lineation.removeChildNodesFromElementObject(target)
	xmlProcessor = target.getXMLProcessor()
	xmlProcessor.convertElementNode(target, geometryOutput)


class SolidDerivation:
	'Class to hold solid variables.'
	def __init__(self, elementNode):
		'Set defaults.'
		self.target = evaluate.getTransformedPathsByKey([], elementNode, 'target')

	def __repr__(self):
		'Get the string representation of this SolidDerivation.'
		return str(self.__dict__)
