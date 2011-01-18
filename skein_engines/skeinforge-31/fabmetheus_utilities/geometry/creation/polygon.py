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
	sides = evaluate.getEvaluatedFloatDefault(4.0, 'sides', xmlElement)
	sideAngle = 2.0 * math.pi / sides
	radius = complex(1.0, 1.0)
	radius = lineation.getComplexByMultiplierPrefixes(math.cos(0.5 * sideAngle), ['apothem', 'inradius'], radius, xmlElement)
	radius = lineation.getComplexByPrefixes(['demisize', 'radius'], radius, xmlElement)
	radius = lineation.getComplexByMultiplierPrefixes(2.0, ['diameter', 'size'], radius, xmlElement)
	loop = []
	sidesCeiling = int(math.ceil(abs(sides)))
	startEnd = lineation.StartEnd(sidesCeiling, '', xmlElement)
	spiral = lineation.Spiral(0.5 * sideAngle / math.pi, xmlElement)
	for side in xrange(startEnd.start, startEnd.end):
		angle = float(side) * sideAngle
		unitPolar = euclidean.getWiddershinsUnitPolar(angle)
		vertex = spiral.getSpiralPoint(unitPolar, Vector3(unitPolar.real * radius.real, unitPolar.imag * radius.imag))
		loop.append(vertex)
	sideLength = sideAngle * lineation.getAverageRadius(radius)
	lineation.setClosedAttribute(startEnd.revolutions, xmlElement)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, sideAngle, sideLength), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['sides', 'radius'], arguments, xmlElement)
	return getGeometryOutput(xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)
