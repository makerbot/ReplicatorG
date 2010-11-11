"""
Solid.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_tools import path
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getGeometryOutput(derivation, xmlElement):
	"Get geometry output from paths."
	if derivation == None:
		derivation = SolidDerivation()
		derivation.setToXMLElement(xmlElement)
	geometryOutput = []
	for path in derivation.target:
		sideLoop = SideLoop(path)
		geometryOutput += getGeometryOutputByLoop( sideLoop, xmlElement )
	return getUnpackedLoops(geometryOutput)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get triangle mesh from attribute dictionary by arguments."
	return getGeometryOutput(None, xmlElement)
#
#def getGeometryOutputByLoop( sideLoop, xmlElement ):
#	"Get geometry output by side loop."
#	sideLoop.rotate(xmlElement)
#	return getUnpackedLoops( getGeometryOutputByManipulation( sideLoop, xmlElement ) )
#
#def processXMLElement(xmlElement):
#	"Process the xml element."
#	processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)

def getGeometryOutputByManipulation(geometryOutput, xmlElement):
	"Get geometryOutput manipulated by the plugins in the manipulation shapes & solids folders."
	xmlProcessor = xmlElement.getXMLProcessor()
	matchingPlugins = evaluate.getFromCreationEvaluatorPlugins(xmlProcessor.manipulationEvaluatorDictionary, xmlElement)
	matchingPlugins += evaluate.getMatchingPlugins(xmlProcessor.manipulationShapeDictionary, xmlElement)
	matchingPlugins.sort(evaluate.compareExecutionOrderAscending)
	for matchingPlugin in matchingPlugins:
		geometryOutput = matchingPlugin.getManipulatedGeometryOutput(geometryOutput, xmlElement)
	return geometryOutput

def processXMLElementByFunction(manipulationFunction, xmlElement):
	"Process the xml element."
	if 'target' not in xmlElement.attributeDictionary:
		print('Warning, there was no target in processXMLElementByFunction in solid for:')
		print(xmlElement)
		return
	target = evaluate.getEvaluatedLinkValue(str(xmlElement.attributeDictionary['target']).strip(), xmlElement)
	if target.__class__.__name__ == 'XMLElement':
		manipulationFunction(target, xmlElement)
		return
	lineation.processXMLElementByGeometry(target, xmlElement)
	manipulationFunction(xmlElement, xmlElement)

def processXMLElementByGeometry(geometryOutput, xmlElement):
	"Process the xml element by geometryOutput."
	if geometryOutput == None:
		return
	xmlProcessor = xmlElement.getXMLProcessor()
	xmlProcessor.convertXMLElement(geometryOutput, xmlElement)
	xmlProcessor.processXMLElement(xmlElement)


class SolidDerivation:
	"Class to hold solid variables."
	def __init__(self):
		'Set defaults.'
		self.target = []

	def __repr__(self):
		"Get the string representation of this SolidDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		if len(self.target) < 1:
			self.target = evaluate.getTransformedPathsByKey('target', xmlElement)
