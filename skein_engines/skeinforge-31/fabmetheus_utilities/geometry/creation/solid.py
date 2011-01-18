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


def getGeometryOutput(xmlElement):
	"Get triangle mesh from attribute dictionary."
	geometryOutput = []
	paths = evaluate.getPathsByKeys( ['path', 'paths', 'target'], xmlElement )
	for path in paths:
		sideLoop = SideLoop(path)
		geometryOutput += getGeometryOutputByLoop( sideLoop, xmlElement )
	return getUnpackedLoops(geometryOutput)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get triangle mesh from attribute dictionary by arguments."
	return getGeometryOutput(xmlElement)
#
#def getGeometryOutputByLoop( sideLoop, xmlElement ):
#	"Get geometry output by side loop."
#	sideLoop.rotate(xmlElement)
#	return getUnpackedLoops( getGeometryOutputByManipulation( sideLoop, xmlElement ) )
#
#def processXMLElement(xmlElement):
#	"Process the xml element."
#	processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)

#def processXMLElementByGeometry(geometryOutput, xmlElement):
#	"Process the xml element by geometryOutput."
#	firstElement = None
#	if len(geometryOutput) > 0:
#		firstElement = geometryOutput[0]
#	if firstElement.__class__ == list:
#		if len( firstElement ) > 1:
#			path.convertXMLElementRenameByPaths(geometryOutput, xmlElement)
#		else:
#			path.convertXMLElementRename(firstElement, xmlElement)
#	else:
#		path.convertXMLElementRename(geometryOutput, xmlElement)
#	path.processXMLElement(xmlElement)

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
