"""
Svg_codec is a class and collection of utilities to read from and write to an svg file.

Svg_codec uses the svg_template.tmpl file in the same folder as svg_codec, to output an svg file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import triangle_mesh
import cStringIO
import os


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/02/05 $"
__license__ = "GPL 3.0"


def getCarving( fileName ):
	"Get a carving for the file using an import plugin."
	importPluginFilenames = interpret.getImportPluginFilenames()
	for importPluginFilename in importPluginFilenames:
		fileTypeDot = '.' + importPluginFilename
		if fileName[ - len( fileTypeDot ) : ].lower() == fileTypeDot:
			importPluginsDirectoryPath = gcodec.getAbsoluteFolderPath( os.path.dirname( __file__ ), 'import_plugins' )
			pluginModule = gcodec.getModuleWithDirectoryPath( importPluginsDirectoryPath, importPluginFilename )
			if pluginModule != None:
				return pluginModule.getCarving( fileName )
	print( 'Could not find plugin to handle ' + fileName )
	return None

def getParameterFromJavascript( lines, parameterName, parameterValue ):
	"Get a parameter from lines of javascript."
	for line in lines:
		strippedLine = line.replace( ';', ' ' ).lstrip()
		splitLine = strippedLine.split()
		firstWord = gcodec.getFirstWord( splitLine )
		if firstWord == parameterName:
			return float( splitLine[ 2 ] )
	return parameterValue

def getReplacedInQuotes( original, replacement, text ):
	"Replace what follows in quotes after the word."
	wordAndQuote = original + '="'
	originalIndexStart = text.find( wordAndQuote )
	if originalIndexStart == - 1:
		return text
	originalIndexEnd = text.find( '"', originalIndexStart + len( wordAndQuote ) )
	if originalIndexEnd == - 1:
		return text
	wordAndBothQuotes = text[ originalIndexStart : originalIndexEnd + 1 ]
	return text.replace( wordAndBothQuotes, wordAndQuote + replacement + '"' )

def getReplacedTagString( replacementTagString, tagID, text ):
	"Get text with the tag string replaced."
	idString = 'id="' + tagID + '"'
	idStringIndexStart = text.find( idString )
	if idStringIndexStart == - 1:
		return text
	tagBeginIndex = text.rfind( '<', 0, idStringIndexStart )
	tagEndIndex = text.find( '>', idStringIndexStart )
	if tagBeginIndex == - 1 or tagEndIndex == - 1:
		return text
	originalTagString = text[ tagBeginIndex : tagEndIndex + 1 ]
	return text.replace( originalTagString, replacementTagString )

def getReplacedWordAndInQuotes( original, replacement, text ):
	"Replace the word in the text and replace what follows in quotes after the word."
	text = text.replace( 'replaceWith' + original, replacement )
	return getReplacedInQuotes( original, replacement, text )


class SVGCodecSkein:
	"A base class to get an svg skein from a carving."
	def __init__( self ):
		self.margin = 20
		self.output = cStringIO.StringIO()
		self.textHeight = 22.5
		self.unitScale = 3.7

	def addInitializationToOutputSVG( self, procedureName ):
		"Add initialization gcode to the output."
		endOfSVGHeaderIndex = self.svgTemplateLines.index( '//End of svg header' )
		self.addLines( self.svgTemplateLines[ : endOfSVGHeaderIndex ] )
		self.addLine( '\tdecimalPlacesCarried = ' + str( self.decimalPlacesCarried ) ) # Set decimal places carried.
		self.addLine( '\tlayerThickness = ' + self.getRounded( self.layerThickness ) ) # Set layer thickness.
		self.addLine( '\tperimeterWidth = ' + self.getRounded( self.perimeterWidth ) ) # Set perimeter width.
		self.addLine( '\tprocedureDone = "%s"' % procedureName ) # The skein has been carved.
		self.addLine( '\textrusionStart = 1' ) # Initialization is finished, extrusion is starting.
		beginningOfPathSectionIndex = self.svgTemplateLines.index( '<!--Beginning of path section-->' )
		self.addLines( self.svgTemplateLines[ endOfSVGHeaderIndex + 1 : beginningOfPathSectionIndex ] )

	def addLayerStart( self, layerIndex, z ):
		"Add the start lines for the layer."
#		y = (1 * i + 1) * ( margin + sliceDimY * unitScale) + i * txtHeight
		layerTranslateY = layerIndex * self.textHeight + ( layerIndex + 1 ) * ( self.extent.y * self.unitScale + self.margin )
		zRounded = self.getRounded( z )
		self.addLine( '\t\t<g id="z %s" transform="translate(%s, %s)">' % ( zRounded, self.getRounded( self.margin ), self.getRounded( layerTranslateY ) ) )
		self.addLine( '\t\t\t<text y="15" fill="#000" stroke="none">Layer %s, z %s</text>' % ( layerIndex, zRounded ) )
#		<g id="z 0.1" transform="translate(20, 242)">
#			<text y="15" fill="#000" stroke="none">Layer 1, z 0.1</text>
#	unit scale (mm=3.7, in=96)
#	
#	g transform
#		x = margin
#		y = (layer + 1) * ( margin + (slice height * unit scale)) + (layer * 20)
#
#	text
#		y = text height
#
#	path transform
#		scale = (unit scale) (-1 * unitscale)
#		translate = (-1 * minX) (-1 * minY)

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + "\n" )

	def addLines( self, lines ):
		"Add lines of text to the output."
		for line in lines:
			self.addLine( line )

	def addShutdownToOutput( self ):
		"Add shutdown svg to the output."
		endOfPathSectionIndex = self.svgTemplateLines.index( '<!--End of path section-->' )
		self.addLines( self.svgTemplateLines[ endOfPathSectionIndex + 1 : ] )

	def getReplacedSVGTemplateLines( self, fileName, rotatedBoundaryLayers ):
		"Get the lines of text from the svg_template.tmpl file."
#( layers.length + 1 ) * (margin + sliceDimY * unitScale + txtHeight) + margin + txtHeight + margin + 110
		svgTemplateText = gcodec.getFileTextInFileDirectory( __file__, 'svg_template.tmpl' )
		originalTextLines = gcodec.getTextLines( svgTemplateText )
		self.margin = getParameterFromJavascript( originalTextLines, 'margin', self.margin )
		self.textHeight = getParameterFromJavascript( originalTextLines, 'textHeight', self.textHeight )
		javascriptControlsWidth = getParameterFromJavascript( originalTextLines, 'javascripControlBoxX', 510.0 )
		noJavascriptControlsHeight = getParameterFromJavascript( originalTextLines, 'noJavascriptControlBoxY', 110.0 )
		controlTop = len( rotatedBoundaryLayers ) * ( self.margin + self.extent.y * self.unitScale + self.textHeight ) + 2.0 * self.margin + self.textHeight
#	width = margin + (sliceDimX * unitScale) + margin;
		svgTemplateText = getReplacedInQuotes( 'height', self.getRounded( controlTop + noJavascriptControlsHeight + self.margin ), svgTemplateText )
		width = 2.0 * self.margin + max( self.extent.y * self.unitScale, javascriptControlsWidth )
		svgTemplateText = getReplacedInQuotes( 'width', self.getRounded( width ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'layerThickness', self.getRounded( self.layerThickness ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'maxX', self.getRounded( self.cornerMaximum.x ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'minX', self.getRounded( self.cornerMinimum.x ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'dimX', self.getRounded( self.extent.x ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'maxY', self.getRounded( self.cornerMaximum.y ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'minY', self.getRounded( self.cornerMinimum.y ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'dimY', self.getRounded( self.extent.y ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'maxZ', self.getRounded( self.cornerMaximum.z ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'minZ', self.getRounded( self.cornerMinimum.z ), svgTemplateText )
		svgTemplateText = getReplacedWordAndInQuotes( 'dimZ', self.getRounded( self.extent.z ), svgTemplateText )
		summarizedFilename = gcodec.getSummarizedFilename( fileName ) + ' SVG Slice File'
		svgTemplateText = getReplacedWordAndInQuotes( 'Title', summarizedFilename, svgTemplateText )
		noJavascriptControlsTagString = '<g id="noJavascriptControls" fill="#000" transform="translate(%s, %s)">' % ( self.getRounded( self.margin ), self.getRounded( controlTop ) )
		svgTemplateText = getReplacedTagString( noJavascriptControlsTagString, 'noJavascriptControls', svgTemplateText )
#	<g id="noJavascriptControls" fill="#000" transform="translate(20, 1400)">
		return gcodec.getTextLines( svgTemplateText )

	def getRounded( self, number ):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToDecimalPlacesString( self.decimalPlacesCarried, number )

	def getRoundedComplexString( self, point ):
		"Get the rounded complex string."
		return self.getRounded( point.real ) + ' ' + self.getRounded( point.imag )

	def getSVGLoopString( self, loop ):
		"Get the svg loop string."
		if len( loop ) < 1:
			return ''
		return self.getSVGPathString( loop ) + ' z'

	def getSVGPathString( self, path ):
		"Get the svg path string."
		if len( path ) < 1:
			return ''
		svgLoopString = ''
		oldRoundedComplexString = self.getRoundedComplexString( path[ - 1 ] )
		for pointIndex in xrange( len( path ) ):
			point = path[ pointIndex ]
			stringBeginning = 'M '
			if len( svgLoopString ) > 0:
				stringBeginning = ' L '
			roundedComplexString = self.getRoundedComplexString( point )
			if roundedComplexString != oldRoundedComplexString:
				svgLoopString += stringBeginning + roundedComplexString
			oldRoundedComplexString = roundedComplexString
		return svgLoopString
