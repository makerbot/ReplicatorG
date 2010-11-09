"""
Shaft path.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = ShaftDerivation()
		derivation.setToXMLElement(xmlElement)
	shaftPath = getShaftPath(derivation.depthBottom, derivation.depthTop, derivation.radius, derivation.sides)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(shaftPath), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['radius', 'sides'], arguments, xmlElement)
	return getGeometryOutput(None, xmlElement)

def getShaftPath(depthBottom, depthTop, radius, sides):
	'Get shaft with the option of a flat on the top and/or bottom.'
	if radius <= 0.0:
		return []
	sideAngle = 2.0 * math.pi / float(abs(sides))
	startAngle = 0.5 * sideAngle
	endAngle = math.pi - 0.1 * sideAngle
	shaftProfile = []
	while startAngle < endAngle:
		unitPolar = euclidean.getWiddershinsUnitPolar(startAngle)
		shaftProfile.append(unitPolar * radius)
		startAngle += sideAngle
	if abs(sides) % 2 == 1:
		shaftProfile.append(complex(-radius, 0.0))
	horizontalBegin = radius - depthTop
	horizontalEnd = depthBottom - radius
	shaftProfile = euclidean.getHorizontallyBoundedPath(horizontalBegin, horizontalEnd, shaftProfile)
	for shaftPointIndex, shaftPoint in enumerate(shaftProfile):
		shaftProfile[shaftPointIndex] = complex(shaftPoint.imag, shaftPoint.real)
	shaftPath = euclidean.getVector3Path(euclidean.getMirrorPath(shaftProfile))
	if sides > 0:
		shaftPath.reverse()
	return shaftPath

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)


class ShaftDerivation:
	"Class to hold shaft variables."
	def __init__(self):
		'Set defaults.'
		self.depthBottom = None
		self.depthBottomOverRadius = 0.0
		self.depthTop = None
		self.depthTopOverRadius = 0.0
		self.radius = None
		self.sides = 4
		self.radius = 1.0

	def __repr__(self):
		"Get the string representation of this ShaftDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.depthBottomOverRadius = evaluate.getEvaluatedFloatDefault(
			self.depthBottomOverRadius, 'depthBottomOverRadius', xmlElement)
		self.depthTopOverRadius = evaluate.getEvaluatedFloatDefault(
			self.depthTopOverRadius, 'depthOverRadius', xmlElement)
		self.depthTopOverRadius = evaluate.getEvaluatedFloatDefault(
			self.depthTopOverRadius, 'depthTopOverRadius', xmlElement)
		self.radius = evaluate.getEvaluatedFloatDefault(self.radius, 'radius', xmlElement)
		self.sides = evaluate.getEvaluatedIntDefault(self.sides, 'sides', xmlElement)
		if self.depthBottom == None:
			self.depthBottom = self.radius * self.depthBottomOverRadius
		self.depthBottom = evaluate.getEvaluatedFloatDefault(self.depthBottom, 'depthBottom', xmlElement)
		if self.depthTop == None:
			self.depthTop = self.radius * self.depthTopOverRadius
		self.depthTop = evaluate.getEvaluatedFloatDefault(self.depthTop, 'depth', xmlElement)
		self.depthTop = evaluate.getEvaluatedFloatDefault(self.depthTop, 'depthTop', xmlElement)
