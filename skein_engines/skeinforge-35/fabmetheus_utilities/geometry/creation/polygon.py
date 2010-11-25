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


def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = PolygonDerivation()
		derivation.setToXMLElement(xmlElement)
	loop = []
	spiral = lineation.Spiral(derivation.spiral, 0.5 * derivation.sideAngle / math.pi)
	for side in xrange(derivation.start, derivation.end):
		angle = float(side) * derivation.sideAngle
		unitPolar = euclidean.getWiddershinsUnitPolar(angle)
		vertex = spiral.getSpiralPoint(unitPolar, Vector3(unitPolar.real * derivation.radius.real, unitPolar.imag * derivation.radius.imag))
		loop.append(vertex)
	sideLength = derivation.sideAngle * lineation.getRadiusAverage(derivation.radius)
	lineation.setClosedAttribute(derivation.revolutions, xmlElement)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, derivation.sideAngle, sideLength), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['sides', 'radius'], arguments, xmlElement)
	return getGeometryOutput(None, xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)


class PolygonDerivation:
	"Class to hold polygon variables."
	def __init__(self):
		'Set defaults.'
		self.radius = complex(1.0, 1.0)
		self.revolutions = 1
		self.sides = 4.0
		self.spiral = None
		self.start = 0

	def __repr__(self):
		"Get the string representation of this PolygonDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.sides = evaluate.getEvaluatedFloatDefault(self.sides, 'sides', xmlElement)
		self.sideAngle = 2.0 * math.pi / self.sides
		self.radius = lineation.getComplexByMultiplierPrefixes(math.cos(0.5 * self.sideAngle), ['apothem', 'inradius'], self.radius, xmlElement)
		self.radius = lineation.getComplexByPrefixes(['demisize', 'radius'], self.radius, xmlElement)
		self.radius = lineation.getComplexByMultiplierPrefixes(2.0, ['diameter', 'size'], self.radius, xmlElement)
		self.sidesCeiling = int(math.ceil(abs(self.sides)))
		self.start = evaluate.getEvaluatedIntDefault(self.start, 'start', xmlElement)
		self.start = lineation.getWrappedInteger(self.start, 360.0)
		self.extent = evaluate.getEvaluatedIntDefault(self.sidesCeiling - self.start, 'extent', xmlElement)
		self.end = evaluate.getEvaluatedIntDefault(self.start + self.extent, 'end', xmlElement)
		self.end = lineation.getWrappedInteger(self.end, self.sidesCeiling)
		self.revolutions = evaluate.getEvaluatedIntDefault(self.revolutions, 'revolutions', xmlElement)
		if self.revolutions > 1:
			self.end += self.sidesCeiling * (self.revolutions - 1)
		self.spiral = evaluate.getVector3ByPrefix(self.spiral, 'spiral', xmlElement)
