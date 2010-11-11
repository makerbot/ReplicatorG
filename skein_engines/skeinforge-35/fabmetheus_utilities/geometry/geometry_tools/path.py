"""
Path.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_tools import dictionary
from fabmetheus_utilities.geometry.geometry_tools import vertex
from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import svg_writer
from fabmetheus_utilities import xml_simple_reader
from fabmetheus_utilities import xml_simple_writer


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def convertProcessXMLElementRenameByPaths(geometryOutput, xmlElement):
	"Convert the xml element to a path xml element, add paths and process."
	convertXMLElementRenameByPaths(geometryOutput, xmlElement)
	processXMLElement(xmlElement)

def convertXMLElement(geometryOutput, xmlElement):
	"Convert the xml element to a path xml element."
	vertex.addGeometryList(geometryOutput, xmlElement)

def convertXMLElementRename(geometryOutput, xmlElement):
	"Convert the xml element to a path xml element."
	xmlElement.className = 'path'
	convertXMLElement(geometryOutput, xmlElement)

def convertXMLElementRenameByPaths(geometryOutput, xmlElement):
	"Convert the xml element to a path xml element and add paths."
	xmlElement.className = 'path'
	for geometryOutputChild in geometryOutput:
		pathElement = xml_simple_reader.XMLElement()
		pathElement.setParentAddToChildren(xmlElement)
		convertXMLElementRename( geometryOutputChild, pathElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	evaluate.processArchivable(Path, xmlElement)


class Path(dictionary.Dictionary):
	"A path."
	def __init__(self):
		"Add empty lists."
		dictionary.Dictionary.__init__(self)
		self.matrix4X4 = matrix.Matrix()
		self.oldChainTetragrid = None
		self.transformedPath = None
		self.vertexes = []

	def addXMLInnerSection(self, depth, output):
		"Add the xml section for this object."
		if self.matrix4X4 != None:
			self.matrix4X4.addXML(depth, output)
		xml_simple_writer.addXMLFromVertexes(depth, output, self.vertexes)

	def getFabricationExtension(self):
		"Get fabrication extension."
		return 'svg'

	def getFabricationText(self):
		"Get fabrication text."
		carving = SVGFabricationCarving(self.xmlElement)
		carving.setCarveLayerThickness(evaluate.getSheetThickness(self.xmlElement))
		carving.processSVGElement(self.xmlElement.getRoot().parser.fileName)
		return str(carving)

	def getMatrixChainTetragrid(self):
		"Get the matrix chain tetragrid."
		return self.matrix4X4.getOtherTimesSelf(self.xmlElement.parent.object.getMatrixChainTetragrid()).matrixTetragrid

	def getPaths(self):
		"Get all paths."
		self.transformedPath = None
		if len(self.vertexes) > 0:
			return dictionary.getAllPaths([self.vertexes], self)
		return dictionary.getAllPaths([], self)

	def getTransformedPaths(self):
		"Get all transformed paths."
		if self.xmlElement == None:
			return dictionary.getAllPaths([self.vertexes], self)
		chainTetragrid = self.getMatrixChainTetragrid()
		if self.oldChainTetragrid != chainTetragrid:
			self.oldChainTetragrid = chainTetragrid
			self.transformedPath = None
		if self.transformedPath == None:
			self.transformedPath = matrix.getTransformedVector3s(chainTetragrid, self.vertexes)
		if len(self.transformedPath) > 0:
			return dictionary.getAllTransformedPaths([self.transformedPath], self)
		return dictionary.getAllTransformedPaths([], self)


class SVGFabricationCarving:
	"An slc carving."
	def __init__(self, xmlElement):
		"Add empty lists."
		self.layerThickness = 1.0
		self.rotatedLoopLayers = []
		self.xmlElement = xmlElement

	def __repr__(self):
		"Get the string representation of this carving."
		return self.getCarvedSVG()

	def addXML(self, depth, output):
		"Add xml for this object."
		xml_simple_writer.addXMLFromObjects(depth, self.rotatedLoopLayers, output)

	def getCarveCornerMaximum(self):
		"Get the corner maximum of the vertexes."
		return self.cornerMaximum

	def getCarveCornerMinimum(self):
		"Get the corner minimum of the vertexes."
		return self.cornerMinimum

	def getCarvedSVG(self):
		"Get the carved svg text."
		return svg_writer.getSVGByLoopLayers(False, self.rotatedLoopLayers, self)

	def getCarveLayerThickness(self):
		"Get the layer thickness."
		return self.layerThickness

	def getCarveRotatedBoundaryLayers(self):
		"Get the rotated boundary layers."
		return self.rotatedLoopLayers

	def getFabmetheusXML(self):
		"Return the fabmetheus XML."
		return self.xmlElement.getParser().getOriginalRoot()

	def getInterpretationSuffix(self):
		"Return the suffix for a carving."
		return 'svg'

	def processSVGElement(self, fileName):
		"Parse SVG element and store the layers."
		self.fileName = fileName
		paths = self.xmlElement.object.getPaths()
		if len(paths) < 1:
			return
		firstPath = paths[0]
		if len(firstPath) < 1:
			return
		rotatedLoopLayer = euclidean.RotatedLoopLayer(firstPath[0].z)
		self.rotatedLoopLayers.append(rotatedLoopLayer)
		for path in paths:
			rotatedLoopLayer.loops.append(euclidean.getComplexPath(path))
		self.cornerMaximum = Vector3(-999999999.0, -999999999.0, -999999999.0)
		self.cornerMinimum = Vector3(999999999.0, 999999999.0, 999999999.0)
		svg_writer.setSVGCarvingCorners(self.rotatedLoopLayers, self)
		halfLayerThickness = 0.5 * self.layerThickness
		self.cornerMaximum.z += halfLayerThickness
		self.cornerMinimum.z -= halfLayerThickness

	def setCarveBridgeLayerThickness( self, bridgeLayerThickness ):
		"Set the bridge layer thickness.  If the infill is not in the direction of the bridge, the bridge layer thickness should be given as None or not set at all."
		pass

	def setCarveLayerThickness( self, layerThickness ):
		"Set the layer thickness."
		self.layerThickness = layerThickness

	def setCarveImportRadius( self, importRadius ):
		"Set the import radius."
		pass

	def setCarveIsCorrectMesh( self, isCorrectMesh ):
		"Set the is correct mesh flag."
		pass
