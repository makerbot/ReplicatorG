"""
Boolean geometry sphere.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
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


def addSphereByRadius(faces, radius, vertexes, xmlElement):
	'Add sphere by radius.'
	bottom = -radius.z
	sides = evaluate.getSidesMinimumThreeBasedOnPrecision(max(radius.x, radius.y, radius.z), xmlElement )
	sphereSlices = max(sides / 2, 2)
	equator = euclidean.getComplexPolygonByComplexRadius(complex(radius.x, radius.y), sides)
	polygons = [triangle_mesh.getAddIndexedLoop([complex()], vertexes, bottom)]
	zIncrement = (radius.z + radius.z) / float(sphereSlices)
	z = bottom
	for sphereSlice in xrange(1, sphereSlices):
		z += zIncrement
		zPortion = abs(z) / radius.z
		multipliedPath = euclidean.getComplexPathByMultiplier(math.sqrt(1.0 - zPortion * zPortion), equator)
		polygons.append(triangle_mesh.getAddIndexedLoop(multipliedPath, vertexes, z))
	polygons.append(triangle_mesh.getAddIndexedLoop([complex()], vertexes, radius.z))
	triangle_mesh.addPillarByLoops(faces, polygons)

def getGeometryOutput(radius, xmlElement):
	'Get triangle mesh from attribute dictionary.'
	faces = []
	vertexes = []
	addSphereByRadius(faces, radius, vertexes, xmlElement)
	return {'trianglemesh' : {'vertex' : vertexes, 'face' : faces}}

def processXMLElement(xmlElement):
	'Process the xml element.'
	group.processShape(Sphere, xmlElement)


class Sphere(cube.Cube):
	'A sphere object.'
	def createShape(self):
		'Create the shape.'
		addSphereByRadius(self.faces, self.radius, self.vertexes, self.xmlElement)

	def setToObjectAttributeDictionary(self):
		'Set the shape of this carvable object info.'
		self.radius = evaluate.getVector3ByPrefixes( ['demisize', 'radius'], Vector3(1.0, 1.0, 1.0), self.xmlElement )
		self.radius = evaluate.getVector3ByMultiplierPrefixes( 2.0, ['diameter', 'size'], self.radius, self.xmlElement )
		self.xmlElement.attributeDictionary['radius.x'] = self.radius.x
		self.xmlElement.attributeDictionary['radius.y'] = self.radius.y
		self.xmlElement.attributeDictionary['radius.z'] = self.radius.z
		self.createShape()
