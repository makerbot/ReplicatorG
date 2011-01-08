"""
Square path.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getGeometryOutput(xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	inradius = lineation.getComplexByPrefixes(['demisize', 'inradius'], complex(1.0, 1.0), xmlElement)
	inradius = lineation.getComplexByMultiplierPrefix(2.0, 'size', inradius, xmlElement)
	demiwidth = lineation.getFloatByPrefixBeginEnd('demiwidth', 'width', inradius.real, xmlElement)
	demiheight = lineation.getFloatByPrefixBeginEnd('demiheight', 'height', inradius.imag, xmlElement)
	bottomDemiwidth = lineation.getFloatByPrefixBeginEnd('bottomdemiwidth', 'bottomwidth', demiwidth, xmlElement)
	topDemiwidth = lineation.getFloatByPrefixBeginEnd('topdemiwidth', 'topwidth', demiwidth, xmlElement)
	interiorAngle = evaluate.getEvaluatedFloatDefault(90.0, 'interiorangle', xmlElement)
	topRight = complex(topDemiwidth, demiheight)
	topLeft = complex(-topDemiwidth, demiheight)
	bottomLeft = complex(-bottomDemiwidth, -demiheight)
	bottomRight = complex(bottomDemiwidth, -demiheight)
	if interiorAngle != 90.0:
		interiorPlaneAngle = euclidean.getWiddershinsUnitPolar(math.radians(interiorAngle - 90.0))
		topRight = (topRight - bottomRight) * interiorPlaneAngle + bottomRight
		topLeft = (topLeft - bottomLeft) * interiorPlaneAngle + bottomLeft
	revolutions = evaluate.getEvaluatedIntOne('revolutions', xmlElement)
	lineation.setClosedAttribute(revolutions, xmlElement)
	complexLoop = [topRight, topLeft, bottomLeft, bottomRight]
	originalLoop = complexLoop[:]
	for revolution in xrange(1, revolutions):
		complexLoop += originalLoop
	spiral = lineation.Spiral(0.25, xmlElement)
	loop = []
	loopCentroid = euclidean.getLoopCentroid(originalLoop)
	for point in complexLoop:
		unitPolar = euclidean.getNormalized(point - loopCentroid)
		loop.append(spiral.getSpiralPoint(unitPolar, Vector3(point.real, point.imag)))
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, 0.5 * math.pi), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	if len(arguments) < 1:
		return getGeometryOutput(xmlElement)
	inradius = 0.5 * euclidean.getFloatFromValue(arguments[0])
	xmlElement.attributeDictionary['inradius.x'] = str(inradius)
	if len(arguments) > 1:
		inradius = 0.5 * euclidean.getFloatFromValue(arguments[1])
	xmlElement.attributeDictionary['inradius.y'] = str(inradius)
	return getGeometryOutput(xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)
