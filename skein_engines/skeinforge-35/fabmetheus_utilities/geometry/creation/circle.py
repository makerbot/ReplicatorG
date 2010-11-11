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
		derivation = CircleDerivation()
		derivation.setToXMLElement(xmlElement)
	loop = []
	angleTotal = math.radians(derivation.start)
	sidesCeiling = int(math.ceil(abs(derivation.sides) * derivation.extent / 360.0))
	sideAngle = math.radians(derivation.extent) / sidesCeiling
	spiral = lineation.Spiral(derivation.spiral, 0.5 * sideAngle / math.pi)
	for side in xrange(sidesCeiling + (derivation.extent != 360.0)):
		unitPolar = euclidean.getWiddershinsUnitPolar(angleTotal)
		vertex = spiral.getSpiralPoint(unitPolar, Vector3(unitPolar.real * derivation.radius.real, unitPolar.imag * derivation.radius.imag))
		angleTotal += sideAngle
		loop.append(vertex)
	sideLength = sideAngle * lineation.getRadiusAverage(derivation.radius)
	lineation.setClosedAttribute(derivation.revolutions, xmlElement)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, sideAngle, sideLength), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['radius', 'start', 'end', 'revolutions'], arguments, xmlElement)
	return getGeometryOutput(None, xmlElement)

def getWrappedFloat(floatValue, modulo):
	"Get wrapped float."
	if floatValue >= modulo:
		return modulo
	if floatValue >= 0:
		return floatValue
	return floatValue % modulo

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)


class CircleDerivation:
	"Class to hold circle variables."
	def __init__(self):
		'Set defaults.'
		self.radius = complex(1.0, 1.0)
		self.revolutions = 1.0
		self.sides = None
		self.spiral = None
		self.start = 0.0

	def __repr__(self):
		"Get the string representation of this CircleDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.radius = lineation.getRadiusComplex(self.radius, xmlElement)
		if self.sides == None:
			radiusMaximum = max(self.radius.real, self.radius.imag)
			self.sides = evaluate.getSidesMinimumThreeBasedOnPrecisionSides(radiusMaximum, xmlElement)
		self.start = evaluate.getEvaluatedFloatDefault(self.start, 'start', xmlElement)
		self.start = getWrappedFloat(self.start, 360.0)
		self.extent = evaluate.getEvaluatedFloatDefault(360.0 - self.start, 'extent', xmlElement)
		self.end = evaluate.getEvaluatedFloatDefault(self.start + self.extent, 'end', xmlElement)
		self.end = getWrappedFloat(self.end, 360.0)
		self.revolutions = evaluate.getEvaluatedFloatDefault(self.revolutions, 'revolutions', xmlElement)
		if self.revolutions > 1:
			self.end += 360.0 * (self.revolutions - 1)
		self.extent = self.end - self.start
		self.spiral = evaluate.getVector3ByPrefix(self.spiral, 'spiral', xmlElement)
