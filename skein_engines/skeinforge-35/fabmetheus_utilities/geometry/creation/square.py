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


def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = SquareDerivation()
		derivation.setToXMLElement(xmlElement)
	topRight = complex(derivation.topDemiwidth, derivation.demiheight)
	topLeft = complex(-derivation.topDemiwidth, derivation.demiheight)
	bottomLeft = complex(-derivation.bottomDemiwidth, -derivation.demiheight)
	bottomRight = complex(derivation.bottomDemiwidth, -derivation.demiheight)
	if derivation.interiorAngle != 90.0:
		interiorPlaneAngle = euclidean.getWiddershinsUnitPolar(math.radians(derivation.interiorAngle - 90.0))
		topRight = (topRight - bottomRight) * interiorPlaneAngle + bottomRight
		topLeft = (topLeft - bottomLeft) * interiorPlaneAngle + bottomLeft
	lineation.setClosedAttribute(derivation.revolutions, xmlElement)
	complexLoop = [topRight, topLeft, bottomLeft, bottomRight]
	originalLoop = complexLoop[:]
	for revolution in xrange(1, derivation.revolutions):
		complexLoop += originalLoop
	spiral = lineation.Spiral(derivation.spiral, 0.25)
	loop = []
	loopCentroid = euclidean.getLoopCentroid(originalLoop)
	for point in complexLoop:
		unitPolar = euclidean.getNormalized(point - loopCentroid)
		loop.append(spiral.getSpiralPoint(unitPolar, Vector3(point.real, point.imag)))
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, 0.5 * math.pi), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	if len(arguments) < 1:
		return getGeometryOutput(None, xmlElement)
	inradius = 0.5 * euclidean.getFloatFromValue(arguments[0])
	xmlElement.attributeDictionary['inradius.x'] = str(inradius)
	if len(arguments) > 1:
		inradius = 0.5 * euclidean.getFloatFromValue(arguments[1])
	xmlElement.attributeDictionary['inradius.y'] = str(inradius)
	return getGeometryOutput(None, xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)


class SquareDerivation:
	"Class to hold square variables."
	def __init__(self):
		'Set defaults.'
		self.inradius = complex(1.0, 1.0)
		self.interiorAngle = 90.0
		self.revolutions = 1
		self.spiral = None

	def __repr__(self):
		"Get the string representation of this SquareDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.inradius = lineation.getComplexByPrefixes(['demisize', 'inradius'], self.inradius, xmlElement)
		self.inradius = lineation.getComplexByMultiplierPrefix(2.0, 'size', self.inradius, xmlElement)
		self.demiwidth = lineation.getFloatByPrefixBeginEnd('demiwidth', 'width', self.inradius.real, xmlElement)
		self.demiheight = lineation.getFloatByPrefixBeginEnd('demiheight', 'height', self.inradius.imag, xmlElement)
		self.bottomDemiwidth = lineation.getFloatByPrefixBeginEnd('bottomdemiwidth', 'bottomwidth', self.demiwidth, xmlElement)
		self.topDemiwidth = lineation.getFloatByPrefixBeginEnd('topdemiwidth', 'topwidth', self.demiwidth, xmlElement)
		self.interiorAngle = evaluate.getEvaluatedFloatDefault(self.interiorAngle, 'interiorangle', xmlElement)
		self.revolutions = evaluate.getEvaluatedIntDefault(self.revolutions, 'revolutions', xmlElement)
		self.spiral = evaluate.getVector3ByPrefix(self.spiral, 'spiral', xmlElement)
