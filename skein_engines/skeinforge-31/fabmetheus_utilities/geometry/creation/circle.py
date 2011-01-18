"""
Polygon path.

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
	radius = lineation.getRadiusComplex(complex(1.0, 1.0), xmlElement)
	sides = evaluate.getSidesMinimumThreeBasedOnPrecisionSides(max(radius.real, radius.imag), xmlElement)
	loop = []
	start = evaluate.getEvaluatedFloatZero('start', xmlElement)
	start = getWrappedFloat(start, 360.0)
	extent = evaluate.getEvaluatedFloatDefault(360.0 - start, 'extent', xmlElement)
	end = evaluate.getEvaluatedFloatDefault(start + extent, 'end', xmlElement)
	end = getWrappedFloat(end, 360.0)
	revolutions = evaluate.getEvaluatedFloatOne('revolutions', xmlElement)
	if revolutions > 1:
		end += 360.0 * (revolutions - 1)
	angleTotal = math.radians(start)
	extent = end - start
	sidesCeiling = int(math.ceil(abs(sides) * extent / 360.0))
	sideAngle = math.radians(extent) / sidesCeiling
	spiral = lineation.Spiral(0.5 * sideAngle / math.pi, xmlElement)
	for side in xrange(sidesCeiling + (extent != 360.0)):
		unitPolar = euclidean.getWiddershinsUnitPolar(angleTotal)
		vertex = spiral.getSpiralPoint(unitPolar, Vector3(unitPolar.real * radius.real, unitPolar.imag * radius.imag))
		angleTotal += sideAngle
		loop.append(vertex)
	sideLength = sideAngle * lineation.getAverageRadius(radius)
	lineation.setClosedAttribute(revolutions, xmlElement)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, sideAngle, sideLength), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['radius', 'start', 'end', 'revolutions'], arguments, xmlElement)
	return getGeometryOutput(xmlElement)

def getWrappedFloat(floatValue, modulo):
	"Get wrapped float."
	if floatValue >= modulo:
		return modulo
	if floatValue >= 0:
		return floatValue
	return floatValue % modulo

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)
