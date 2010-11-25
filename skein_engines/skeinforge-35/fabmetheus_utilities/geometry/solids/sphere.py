"""
Boolean geometry sphere.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.solids import cube
from fabmetheus_utilities.geometry.solids import group
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def processXMLElement(xmlElement):
	"Process the xml element."
	group.processShape( Sphere, xmlElement)


class Sphere( cube.Cube ):
	"A sphere object."
	def createShape(self):
		"Create the shape."
		maximumRadius = max( self.radius.x, self.radius.y, self.radius.z )
		numberOfInBetweens = max( int( 0.25 * evaluate.getSidesBasedOnPrecision( maximumRadius, self.xmlElement ) ), 1 )
		numberOfDivisions = numberOfInBetweens + 1
		bottomLeft = complex( - 1.0, - 1.0 )
		topRight = complex(1.0, 1.0)
		extent = topRight - bottomLeft
		elementExtent = extent / numberOfDivisions
		grid = []
		for rowIndex in xrange( numberOfDivisions + 1 ):
			row = []
			grid.append( row )
			for columnIndex in xrange( numberOfDivisions + 1 ):
				point = complex( elementExtent.real * float( columnIndex ), elementExtent.real * float( rowIndex ) ) + bottomLeft
				row.append(point)
		indexedGridBottom = trianglemesh.getAddIndexedGrid( grid, self.vertexes, - 1.0 )
		indexedGridBottomLoop = trianglemesh.getIndexedLoopFromIndexedGrid( indexedGridBottom )
		indexedLoops = [ indexedGridBottomLoop ]
		zList = []
		for zIndex in xrange( 1, numberOfDivisions ):
			z = elementExtent.real * float( zIndex ) + bottomLeft.real
			zList.append(z)
		gridLoop = []
		for vertex in indexedGridBottomLoop:
			gridLoop.append( vertex.dropAxis(2) )
		indexedLoops += trianglemesh.getAddIndexedLoops( gridLoop, self.vertexes, zList )
		indexedGridTop = trianglemesh.getAddIndexedGrid( grid, self.vertexes, 1.0 )
		indexedLoops.append( trianglemesh.getIndexedLoopFromIndexedGrid( indexedGridTop ) )
		trianglemesh.addPillarFromConvexLoopsGrids( self.faces, [ indexedGridBottom, indexedGridTop ], indexedLoops )
		for vertex in self.vertexes:
			vertex.normalize()
			vertex.x *= self.radius.x
			vertex.y *= self.radius.y
			vertex.z *= self.radius.z

	def setToObjectAttributeDictionary(self):
		"Set the shape of this carvable object info."
		self.radius = evaluate.getVector3ByPrefixes( ['demisize', 'radius'], Vector3(1.0, 1.0, 1.0), self.xmlElement )
		self.radius = evaluate.getVector3ByMultiplierPrefixes( 2.0, ['diameter', 'size'], self.radius, self.xmlElement )
		self.xmlElement.attributeDictionary['radius.x'] = self.radius.x
		self.xmlElement.attributeDictionary['radius.y'] = self.radius.y
		self.xmlElement.attributeDictionary['radius.z'] = self.radius.z
		self.createShape()
