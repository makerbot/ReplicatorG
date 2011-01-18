"""
Boolean geometry cylinder.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.manipulation_matrix import matrix
from fabmetheus_utilities.geometry.solids import cube
from fabmetheus_utilities.geometry.solids import group
from fabmetheus_utilities.geometry.solids import triangle_mesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math

__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def addCylinderByInradius(faces, inradius, sides, topOverBottom, vertexes, xmlElement):
	'Add cylinder by radius.'
	polygonBottom = euclidean.getComplexPolygonByComplexRadius(complex(inradius.x, inradius.y), sides)
	polygonTop = polygonBottom
	if topOverBottom <= 0.0:
		polygonTop = [complex()]
	elif topOverBottom != 1.0:
		polygonTop = euclidean.getComplexPathByMultiplier(topOverBottom, polygonTop)
	bottomTopPolygon = [
		triangle_mesh.getAddIndexedLoop(polygonBottom, vertexes, -inradius.z),
		triangle_mesh.getAddIndexedLoop(polygonTop, vertexes, inradius.z)]
	triangle_mesh.addPillarByLoops(faces, bottomTopPolygon)

def getGeometryOutput(inradius, sides, topOverBottom, xmlElement):
	'Get cylinder triangle mesh by inradius.'
	faces = []
	vertexes = []
	addCylinderByInradius(faces, inradius, sides, topOverBottom, vertexes, xmlElement)
	return {'trianglemesh' : {'vertex' : vertexes, 'face' : faces}}

def getGeometryOutputByEndStart(endZ, inradiusComplex, sides, start, topOverBottom, xmlElement):
	'Get cylinder triangle mesh by endZ, inradius and start.'
	inradius = Vector3(inradiusComplex.real, inradiusComplex.imag, 0.5 * abs(endZ - start.z))
	cylinderOutput = getGeometryOutput(inradius, sides, topOverBottom, xmlElement)
	vertexes = matrix.getVertexes(cylinderOutput)
	if endZ < start.z:
		for vertex in vertexes:
			vertex.z = -vertex.z
	translation = Vector3(start.x, start.y, inradius.z + min(start.z, endZ))
	euclidean.translateVector3Path(vertexes, translation)
	return cylinderOutput

def processXMLElement(xmlElement):
	'Process the xml element.'
	group.processShape( Cylinder, xmlElement)


class Cylinder( cube.Cube ):
	'A cylinder object.'
	def __init__(self):
		'Add empty lists.'
		cube.Cube.__init__(self)

	def createShape(self):
		'Create the shape.'
		sides = evaluate.getSidesMinimumThreeBasedOnPrecision(max(self.inradius.x, self.inradius.y), self.xmlElement )
		addCylinderByInradius(self.faces, self.inradius, sides, self.topOverBottom, self.vertexes, self.xmlElement)

	def setToObjectAttributeDictionary(self):
		'Set the shape of this carvable object info.'
		self.inradius = evaluate.getVector3ByPrefixes(['demisize', 'inradius', 'radius'], Vector3(1.0, 1.0, 1.0), self.xmlElement)
		self.inradius = evaluate.getVector3ByMultiplierPrefixes(2.0, ['diameter', 'size'], self.inradius, self.xmlElement)
		self.inradius.z = 0.5 * evaluate.getEvaluatedFloatDefault(self.inradius.z + self.inradius.z, 'height', self.xmlElement)
		self.topOverBottom = evaluate.getEvaluatedFloatDefault(1.0, 'topOverBottom', self.xmlElement )
		self.xmlElement.attributeDictionary['height'] = self.inradius.z + self.inradius.z
		self.xmlElement.attributeDictionary['radius.x'] = self.inradius.x
		self.xmlElement.attributeDictionary['radius.y'] = self.inradius.y
		self.xmlElement.attributeDictionary['topOverBottom'] = self.topOverBottom
		self.createShape()
