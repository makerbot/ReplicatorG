"""
Boolean geometry cylinder.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.solids import cube
from fabmetheus_utilities.geometry.solids import group
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math

__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def processXMLElement(xmlElement):
	"Process the xml element."
	group.processShape( Cylinder, xmlElement)


class Cylinder( cube.Cube ):
	"A cylinder object."
	def __init__(self):
		"Add empty lists."
		self.radiusZ = None
		cube.Cube.__init__(self)

	def createShape(self):
		"Create the shape."
		polygonBottom = []
		polygonTop = []
		sides = evaluate.getSidesMinimumThreeBasedOnPrecision(max(self.inradius.x, self.inradius.y), self.xmlElement )
		sideAngle = 2.0 * math.pi / sides
		for side in xrange(int(sides)):
			angle = float(side) * sideAngle
			unitComplex = euclidean.getWiddershinsUnitPolar(angle)
			pointBottom = complex(unitComplex.real * self.inradius.x, unitComplex.imag * self.inradius.y)
			polygonBottom.append(pointBottom)
			if self.topOverBottom > 0.0:
				polygonTop.append(pointBottom * self.topOverBottom)
		if self.topOverBottom <= 0.0:
			polygonTop.append(complex())
		bottomTopPolygon = [
			trianglemesh.getAddIndexedLoop(polygonBottom, self.vertexes, - self.inradius.z),
			trianglemesh.getAddIndexedLoop(polygonTop, self.vertexes, self.inradius.z)]
		trianglemesh.addPillarByLoops(self.faces, bottomTopPolygon)

	def setToObjectAttributeDictionary(self):
		"Set the shape of this carvable object info."
		self.inradius = evaluate.getVector3ByPrefixes(['demisize', 'inradius', 'radius'], Vector3(1.0, 1.0, 1.0), self.xmlElement)
		self.inradius = evaluate.getVector3ByMultiplierPrefixes(2.0, ['diameter', 'size'], self.inradius, self.xmlElement)
		self.inradius.z = 0.5 * evaluate.getEvaluatedFloatDefault(self.inradius.z + self.inradius.z, 'height', self.xmlElement)
		self.topOverBottom = evaluate.getEvaluatedFloatDefault(1.0, 'topoverbottom', self.xmlElement )
		self.xmlElement.attributeDictionary['height'] = self.inradius.z + self.inradius.z
		self.xmlElement.attributeDictionary['radius.x'] = self.inradius.x
		self.xmlElement.attributeDictionary['radius.y'] = self.inradius.y
		self.xmlElement.attributeDictionary['topoverbottom'] = self.topOverBottom
		self.createShape()
