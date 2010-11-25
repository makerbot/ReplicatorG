"""
This page is in the table of contents.
The xml.py script is an import translator plugin to get a carving from an Art of Illusion xml file.

An import plugin is a script in the interpret_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an xml file and returns the carving.

This example gets a triangle mesh for the xml file boolean.xml.  This example is run in a terminal in the folder which contains boolean.xml and xml.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import xml
>>> xml.getCarving().getCarveRotatedBoundaryLayers()
[-1.159765625, None, [[(-18.925000000000001-2.4550000000000001j), (-18.754999999999981-2.4550000000000001j)
..
many more lines of the carving
..


An xml file can be exported from Art of Illusion by going to the "File" menu, then going into the "Export" menu item, then picking the XML choice.  This will bring up the XML file chooser window, choose a place to save the file then click "OK".  Leave the "compressFile" checkbox unchecked.  All the objects from the scene will be exported, this plugin will ignore the light and camera.  If you want to fabricate more than one object at a time, you can have multiple objects in the Art of Illusion scene and they will all be carved, then fabricated together.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import booleansolid
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import settings
from fabmetheus_utilities import xml_simple_writer


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


class BooleanGeometry:
	"A shape scene."
	def __init__(self):
		"Add empty lists."
		self.archivableObjects = []
		self.belowLoops = []
		self.bridgeLayerThickness = None
		self.importRadius = 0.3
		self.layerThickness = 0.4
		self.rotatedBoundaryLayers = []

	def __repr__(self):
		"Get the string representation of this carving."
		xmlElement = None
		if len(self.archivableObjects) > 0:
			xmlElement = self.archivableObjects[0].xmlElement
		output = xml_simple_writer.getBeginGeometryXMLOutput(xmlElement)
		self.addXML( 1, output )
		return xml_simple_writer.getEndGeometryXMLString(output)

	def addXML(self, depth, output):
		"Add xml for this object."
		xml_simple_writer.addXMLFromObjects( depth, self.archivableObjects, output )

	def getCarveCornerMaximum(self):
		"Get the corner maximum of the vertexes."
		return self.cornerMaximum

	def getCarveCornerMinimum(self):
		"Get the corner minimum of the vertexes."
		return self.cornerMinimum

	def getCarveLayerThickness(self):
		"Get the layer thickness."
		return self.layerThickness

	def getCarveRotatedBoundaryLayers(self):
		"Get the rotated boundary layers."
		vertexes = []
		for visibleObject in evaluate.getVisibleObjects(self.archivableObjects):
			vertexes += visibleObject.getTransformedVertexes()
		if len(vertexes) < 1:
			return []
		self.cornerMaximum = Vector3(-999999999.0, - 999999999.0, - 9999999999.9)
		self.cornerMinimum = Vector3(999999999.0, 999999999.0, 9999999999.9)
		for vertex in vertexes:
			self.cornerMaximum.z = max(self.cornerMaximum.z, vertex.z)
			self.cornerMinimum.z = min(self.cornerMinimum.z, vertex.z)
		trianglemesh.initializeZoneIntervalTable(self, vertexes)
		halfHeight = 0.5 * self.layerThickness
		self.setActualMinimumZ(halfHeight)
		z = self.cornerMinimum.z + halfHeight
		while z < self.cornerMaximum.z:
			z = self.getZAddExtruderPaths(z)
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			for loop in rotatedBoundaryLayer.loops:
				for point in loop:
					pointVector3 = Vector3(point.real, point.imag, rotatedBoundaryLayer.z)
					self.cornerMaximum = euclidean.getPointMaximum(self.cornerMaximum, pointVector3)
					self.cornerMinimum = euclidean.getPointMinimum(self.cornerMinimum, pointVector3)
		for rotatedBoundaryLayerIndex in xrange(len(self.rotatedBoundaryLayers) -1, -1, -1):
			rotatedBoundaryLayer = self.rotatedBoundaryLayers[rotatedBoundaryLayerIndex]
			if len(rotatedBoundaryLayer.loops) > 0:
				return self.rotatedBoundaryLayers[: rotatedBoundaryLayerIndex + 1]
		return []

	def getEmptyZExtruderPaths( self, shouldPrintWarning, z ):
		"Get extruder loops."
		z = trianglemesh.getEmptyZ(self, z)
		rotatedBoundaryLayer = euclidean.RotatedLoopLayer(z)
		visibleObjectLoopsList = booleansolid.getVisibleObjectLoopsList( self.importRadius, evaluate.getVisibleObjects(self.archivableObjects), z )
		rotatedBoundaryLayer.loops = euclidean.getConcatenatedList( visibleObjectLoopsList )
		if euclidean.isLoopListIntersecting(rotatedBoundaryLayer.loops):
			rotatedBoundaryLayer.loops = booleansolid.getLoopsUnified(self.importRadius, visibleObjectLoopsList)
			if shouldPrintWarning:
				print('Warning, the triangle mesh slice intersects itself in getExtruderPaths in boolean_geometry.')
				print( "Something will still be printed, but there is no guarantee that it will be the correct shape." )
				print('Once the gcode is saved, you should check over the layer with a z of:')
				print(z)
		return rotatedBoundaryLayer

	def getFabmetheusXML(self):
		"Return the fabmetheus XML."
		if len(self.archivableObjects) > 0:
			return self.archivableObjects[0].xmlElement.getParser().getOriginalRoot()
		return None

	def getInterpretationSuffix(self):
		"Return the suffix for a boolean carving."
		return 'xml'

	def getMatrixChainTetragrid(self):
		"Get the matrix chain tetragrid."
		return None

	def getZAddExtruderPaths( self, z ):
		"Get next z and add extruder loops."
		settings.printProgress(len(self.rotatedBoundaryLayers), 'slice')
		rotatedBoundaryLayer = self.getEmptyZExtruderPaths(True, z)
		self.rotatedBoundaryLayers.append( rotatedBoundaryLayer )
		if self.bridgeLayerThickness == None:
			return z + self.layerThickness
		allExtrudateLoops = []
		for loop in rotatedBoundaryLayer.loops:
			allExtrudateLoops += trianglemesh.getBridgeLoops( self.layerThickness, loop )
		rotatedBoundaryLayer.rotation = trianglemesh.getBridgeDirection( self.belowLoops, allExtrudateLoops, self.layerThickness )
		self.belowLoops = allExtrudateLoops
		if rotatedBoundaryLayer.rotation == None:
			return z + self.layerThickness
		return z + self.bridgeLayerThickness

	def setActualMinimumZ(self, halfHeight):
		"Get the actual minimum z at the lowest rotated boundary layer."
		halfHeightOverMyriad = 0.0001 * halfHeight
		halfHeightOverThousand = 0.001 * halfHeight
		while self.cornerMinimum.z < self.cornerMaximum.z:
			if len(self.getEmptyZExtruderPaths(False, self.cornerMinimum.z + halfHeightOverMyriad).loops) > 0:
				increment = - halfHeight
				while abs(increment) > halfHeightOverThousand:
					self.cornerMinimum.z += increment
					increment = 0.5 * abs(increment)
					if len( self.getEmptyZExtruderPaths(False, self.cornerMinimum.z).loops ) > 0:
						increment = - increment
				if abs(self.cornerMinimum.z) < halfHeight:
					self.cornerMinimum.z = 0.0
				return
			self.cornerMinimum.z += self.layerThickness

	def setCarveBridgeLayerThickness( self, bridgeLayerThickness ):
		"Set the bridge layer thickness.  If the infill is not in the direction of the bridge, the bridge layer thickness should be given as None or not set at all."
		self.bridgeLayerThickness = bridgeLayerThickness

	def setCarveLayerThickness( self, layerThickness ):
		"Set the layer thickness."
		self.layerThickness = layerThickness

	def setCarveImportRadius( self, importRadius ):
		"Set the import radius."
		self.importRadius = importRadius

	def setCarveIsCorrectMesh( self, isCorrectMesh ):
		"Set the is correct mesh flag."
		self.isCorrectMesh = isCorrectMesh
