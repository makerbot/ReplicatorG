"""
Svg_writer is a class and collection of utilities to read from and write to an svg file.

Svg_writer uses the layer_template.svg file in the templates folder in the same folder as svg_writer, to output an svg file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities.xml_simple_reader import XMLElement
from fabmetheus_utilities.xml_simple_reader import XMLSimpleReader
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
import cStringIO
import math
import os


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getCarving(fileName):
	"Get a carving for the file using an import plugin."
	pluginModule = fabmetheus_interpret.getInterpretPlugin(fileName)
	if pluginModule == None:
		return None
	return pluginModule.getCarving(fileName)

def getSliceDictionary(xmlElement):
	"Get the metadata slice attribute dictionary."
	for metadataElement in xmlElement.getChildrenWithClassName('metadata'):
		for child in metadataElement.children:
			if child.className.lower() == 'slice:layers':
				return child.attributeDictionary
	return {}

def getSVGByLoopLayers(addLayerTemplateToSVG, rotatedLoopLayers, svgCarving):
	"Get the svg text."
	if len(rotatedLoopLayers) < 1:
		return ''
	decimalPlacesCarried = max(0, 2 - int(math.floor(math.log10(svgCarving.layerThickness))))
	svgWriter = SVGWriter(addLayerTemplateToSVG, svgCarving, decimalPlacesCarried)
	return svgWriter.getReplacedSVGTemplate(svgCarving.fileName, 'basic', rotatedLoopLayers, svgCarving.getFabmetheusXML())

def getTruncatedRotatedBoundaryLayers( repository, rotatedBoundaryLayers ):
	"Get the truncated rotated boundary layers."
	return rotatedBoundaryLayers[ repository.layersFrom.value : repository.layersTo.value ]

def setSVGCarvingCorners(rotatedLoopLayers, svgCarving):
	"Parse SVG text and store the layers."
	for rotatedBoundaryLayer in rotatedLoopLayers:
		for loop in rotatedBoundaryLayer.loops:
			for point in loop:
				pointVector3 = Vector3(point.real, point.imag, rotatedBoundaryLayer.z)
				svgCarving.cornerMaximum = euclidean.getPointMaximum(svgCarving.cornerMaximum, pointVector3)
				svgCarving.cornerMinimum = euclidean.getPointMinimum(svgCarving.cornerMinimum, pointVector3)


class SVGWriter:
	"A base class to get an svg skein from a carving."
	def __init__(self, addLayerTemplateToSVG, carving, decimalPlacesCarried, perimeterWidth = None):
		self.addLayerTemplateToSVG = addLayerTemplateToSVG
		self.carving = carving
		self.decimalPlacesCarried = decimalPlacesCarried
		self.margin = 20
		self.perimeterWidth = perimeterWidth
		self.textHeight = 22.5
		self.unitScale = 3.7

	def addLayerBegin(self, layerIndex, rotatedBoundaryLayer):
		"Add the start lines for the layer."
		zRounded = self.getRounded( rotatedBoundaryLayer.z )
		self.graphicsCopy = self.graphicsXMLElement.getCopy(zRounded, self.graphicsXMLElement.parent)
		if self.addLayerTemplateToSVG:
			marginRounded = self.getRounded(self.margin)
			layerTranslateY = layerIndex * self.textHeight + (layerIndex + 1) * (self.extent.y * self.unitScale + self.margin)
			translateYRounded = self.getRounded(layerTranslateY)
			self.graphicsCopy.attributeDictionary['transform'] = 'translate(%s, %s)' % (marginRounded, translateYRounded)
			self.graphicsCopy.getFirstChildWithClassName('text').text = 'Layer %s, z:%s' % (layerIndex, zRounded)
		self.pathXMLElement = self.graphicsCopy.getFirstChildWithClassName('path')
		self.pathDictionary = self.pathXMLElement.attributeDictionary

	def addOriginalAsComment(self, xmlElement):
		"Add original xmlElement as a comment."
		if xmlElement == None:
			return
		commentElement = XMLElement()
		commentElement.className = 'comment'
		xmlElementOutput = cStringIO.StringIO()
		xmlElement.addXML(0, xmlElementOutput)
		textLines = archive.getTextLines(xmlElementOutput.getvalue())
		commentElementOutput = cStringIO.StringIO()
		isComment = False
		for textLine in textLines:
			lineStripped = textLine.strip()
			if lineStripped[: len('<!--')] == '<!--':
				isComment = True
			if not isComment:
				if len(textLine) > 0:
					commentElementOutput.write(textLine + '\n')
			if '-->' in lineStripped:
				isComment = False
		commentElement.text = '<!-- Original XML Text:\n%s-->\n' % commentElementOutput.getvalue()
		commentElement.setParentAddToChildren(self.svgElement)

	def addRotatedLoopLayerToOutput( self, layerIndex, rotatedBoundaryLayer ):
		"Add rotated boundary layer to the output."
		self.addLayerBegin( layerIndex, rotatedBoundaryLayer )
		if rotatedBoundaryLayer.rotation != None:
			self.graphicsCopy.attributeDictionary['bridgeRotation'] = str( rotatedBoundaryLayer.rotation )
		if self.addLayerTemplateToSVG:
			self.pathDictionary['transform'] = self.getTransformString()
		else:
			del self.pathDictionary['transform']
		self.pathDictionary['d'] = self.getSVGStringForLoops(rotatedBoundaryLayer.loops)

	def addRotatedLoopLayersToOutput( self, rotatedBoundaryLayers ):
		"Add rotated boundary layers to the output."
		for rotatedBoundaryLayerIndex, rotatedBoundaryLayer in enumerate( rotatedBoundaryLayers ):
			self.addRotatedLoopLayerToOutput( rotatedBoundaryLayerIndex, rotatedBoundaryLayer )

	def getReplacedSVGTemplate(self, fileName, procedureName, rotatedBoundaryLayers, xmlElement=None):
		"Get the lines of text from the layer_template.svg file."
#		( layers.length + 1 ) * (margin + sliceDimY * unitScale + txtHeight) + margin + txtHeight + margin + 110
		cornerMaximum = self.carving.getCarveCornerMaximum()
		cornerMinimum = self.carving.getCarveCornerMinimum()
		self.extent = cornerMaximum - cornerMinimum
		svgTemplateText = archive.getFileTextInFileDirectory( __file__, os.path.join('templates', 'layer_template.svg') )
		self.xmlParser = XMLSimpleReader( fileName, None, svgTemplateText )
		self.svgElement = self.xmlParser.getRoot()
		if not self.addLayerTemplateToSVG:
			self.svgElement.getXMLElementByID('layerTextTemplate').removeFromIDNameParent()
			del self.svgElement.getXMLElementByID('sliceElementTemplate').attributeDictionary['transform']
		svgElementDictionary = self.svgElement.attributeDictionary
		self.graphicsXMLElement = self.svgElement.getXMLElementByID('sliceElementTemplate')
		self.graphicsXMLElement.attributeDictionary['id'] = 'z:'
		self.addRotatedLoopLayersToOutput( rotatedBoundaryLayers )
		self.sliceDictionary = getSliceDictionary( self.svgElement )
		self.setMetadataNoscriptElement('layerThickness', self.carving.getCarveLayerThickness() )
		self.setMetadataNoscriptElement('maxX', cornerMaximum.x )
		self.setMetadataNoscriptElement('minX', cornerMinimum.x )
		self.setMetadataNoscriptElement('maxY', cornerMaximum.y )
		self.setMetadataNoscriptElement('minY', cornerMinimum.y )
		self.setMetadataNoscriptElement('maxZ', cornerMaximum.z )
		self.setMetadataNoscriptElement('minZ', cornerMinimum.z )
		self.margin = float( self.sliceDictionary['margin'] )
		self.textHeight = float( self.sliceDictionary['textHeight'] )
		javascriptControlBoxWidth = float( self.sliceDictionary['javascriptControlBoxWidth'] )
		noJavascriptControlBoxHeight = float( self.sliceDictionary['noJavascriptControlBoxHeight'] )
		controlTop = len( rotatedBoundaryLayers ) * ( self.margin + self.extent.y * self.unitScale + self.textHeight ) + 2.0 * self.margin + self.textHeight
		self.svgElement.getFirstChildWithClassName('title').text = os.path.basename(fileName) + ' - Slice Layers'
		svgElementDictionary['height'] = '%spx' % self.getRounded( controlTop + noJavascriptControlBoxHeight + self.margin )
#		width = margin + (sliceDimX * unitScale) + margin;
		width = 2.0 * self.margin + max( self.extent.x * self.unitScale, javascriptControlBoxWidth )
		svgElementDictionary['width'] = '%spx' % self.getRounded( width )
		self.sliceDictionary['decimalPlacesCarried'] = str( self.decimalPlacesCarried )
		if self.perimeterWidth != None:
			self.sliceDictionary['perimeterWidth'] = self.getRounded( self.perimeterWidth )
		self.sliceDictionary['yAxisPointingUpward'] = 'true'
		self.sliceDictionary['procedureDone'] = procedureName
		noJavascriptDictionary = self.svgElement.getXMLElementByID('noJavascriptControls').attributeDictionary
		noJavascriptDictionary['transform'] = 'translate(%s, %s)' % ( self.getRounded(self.margin), self.getRounded( controlTop ) )
		self.svgElement.getXMLElementByID('dimXNoJavascript').text = self.getRounded( self.extent.x )
		self.svgElement.getXMLElementByID('dimYNoJavascript').text = self.getRounded( self.extent.y )
		self.svgElement.getXMLElementByID('dimZNoJavascript').text = self.getRounded( self.extent.z )
		if not self.addLayerTemplateToSVG:
			self.svgElement.getFirstChildWithClassName('script').removeFromIDNameParent()
			self.svgElement.getXMLElementByID('beginningOfControlSection').removeFromIDNameParent()
			self.svgElement.getXMLElementByID('noJavascriptControls').removeFromIDNameParent()
		self.graphicsXMLElement.removeFromIDNameParent()
		self.addOriginalAsComment(xmlElement)
		output = cStringIO.StringIO()
		output.write(self.xmlParser.beforeRoot)
		self.svgElement.addXML(0, output)
		return output.getvalue()

	def getRounded(self, number):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToPlacesString(self.decimalPlacesCarried, number)

	def getRoundedComplexString(self, point):
		"Get the rounded complex string."
		return self.getRounded( point.real ) + ' ' + self.getRounded( point.imag )

	def getSVGStringForLoop( self, loop ):
		"Get the svg loop string."
		if len(loop) < 1:
			return ''
		return self.getSVGStringForPath(loop) + ' z'

	def getSVGStringForLoops( self, loops ):
		"Get the svg loops string."
		loopString = ''
		if len(loops) > 0:
			loopString += self.getSVGStringForLoop( loops[0] )
		for loop in loops[1 :]:
			loopString += ' ' + self.getSVGStringForLoop(loop)
		return loopString

	def getSVGStringForPath( self, path ):
		"Get the svg path string."
		svgLoopString = ''
		for point in path:
			stringBeginning = 'M '
			if len( svgLoopString ) > 0:
				stringBeginning = ' L '
			svgLoopString += stringBeginning + self.getRoundedComplexString(point)
		return svgLoopString

	def getTransformString(self):
		"Get the svg transform string."
		cornerMinimumXString = self.getRounded(-self.carving.getCarveCornerMinimum().x)
		cornerMinimumYString = self.getRounded(-self.carving.getCarveCornerMinimum().y)
		return 'scale(%s, %s) translate(%s, %s)' % (self.unitScale, - self.unitScale, cornerMinimumXString, cornerMinimumYString)

	def setMetadataNoscriptElement(self, prefix, value):
		"Set the metadata value and the NoJavascript text."
		valueString = self.getRounded(value)
		self.sliceDictionary[prefix] = valueString
		self.svgElement.getXMLElementByID(prefix + 'NoJavascript').text = valueString
