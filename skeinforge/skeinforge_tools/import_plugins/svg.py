"""
The svg.py script is an import translator plugin to get a carving from an svg file.

An import plugin is a script in the import_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an svg file and returns the carving.

This example gets a carving for the svg file Screw Holder Bottom.svg.  This example is run in a terminal in the folder which contains Screw Holder Bottom.svg and svg.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import svg
>>> svg.getCarving()
0.20000000298, 999999999.0, -999999999.0, [8.72782748851e-17, None
..
many more lines of the carving
..
"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addPathData( line, loops ):
	"Add the data from the path line."
	line = line.replace( '"', ' ' )
	splitLine = line.split()
	if splitLine[ 1 ] != 'transform=':
		return
	line = line.lower()
	line = line.replace( 'm', ' ' )
	line = line.replace( 'l', ' ' )
	line = line.replace( '/>', ' ' )
	splitLine = line.split()
	if 'd=' not in splitLine:
		return
	splitLine = splitLine[ splitLine.index( 'd=' ) + 1 : ]
	pathSequence = []
	for word in splitLine:
		if word == 'z':
			loop = []
			for pathSequenceIndex in xrange( 0, len( pathSequence ), 2 ):
				coordinate = complex( pathSequence[ pathSequenceIndex ], pathSequence[ pathSequenceIndex + 1 ] )
				loop.append( coordinate )
			loops.append( loop )
			pathSequence = []
		else:
			pathSequence.append( float( word ) )

def addTextData( line, rotatedBoundaryLayers ):
	"Add the data from the text line."
	if line.find( 'layerThickness' ) != - 1:
		return
	line = line.replace( '>', ' ' )
	line = line.replace( '<', ' ' )
	line = line.replace( ',', ' ' )
	splitLine = line.split()
	if 'Layer' not in splitLine:
		return
	splitLine = splitLine[ splitLine.index( 'Layer' ) + 1 : ]
	if 'z' not in splitLine:
		return
	zIndex = splitLine.index( 'z' )
	rotatedBoundaryLayer = euclidean.RotatedLoopLayer( float( splitLine[ zIndex + 1 ] ) )
	rotatedBoundaryLayers.append( rotatedBoundaryLayer )

def getCarving( fileName = '' ):
	"Get the triangle mesh for the gts file."
	if fileName == '':
		unmodified = gcodec.getFilesWithFileTypeWithoutWords( 'gts' )
		if len( unmodified ) == 0:
			print( "There is no gts file in this folder." )
			return None
		fileName = unmodified[ 0 ]
	carving = SVGCarving()
	carving.parseSVG( gcodec.getFileText( fileName ) )
	return carving

def getValueInQuotes( name, text, value ):
	"Get value in quotes after the name."
	nameAndQuote = name + '="'
	nameIndexStart = text.find( nameAndQuote )
	if nameIndexStart == - 1:
		return value
	valueStartIndex = nameIndexStart + len( nameAndQuote )
	nameIndexEnd = text.find( '"', valueStartIndex )
	if nameIndexEnd == - 1:
		return value
	return float( text[ valueStartIndex : nameIndexEnd ] )


class SVGCarving:
	"An svg carving."
	def __init__( self ):
		"Add empty lists."
		self.maximumZ = - 999999999.0
		self.minimumZ = 999999999.0
		self.layerThickness = None
		self.rotatedBoundaryLayers = []
	
	def __repr__( self ):
		"Get the string representation of this carving."
		return str( self.rotatedBoundaryLayers )

	def getCarveCornerMaximum( self ):
		"Get the corner maximum of the vertices."
		return self.cornerMaximum

	def getCarveCornerMinimum( self ):
		"Get the corner minimum of the vertices."
		return self.cornerMinimum

	def getCarveLayerThickness( self ):
		"Get the layer thickness."
		return self.layerThickness

	def getCarveRotatedBoundaryLayers( self ):
		"Get the rotated boundary layers."
		self.cornerMaximum = Vector3( - 999999999.0, - 999999999.0, self.maximumZ )
		self.cornerMinimum = Vector3( 999999999.0, 999999999.0, self.minimumZ )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			for loop in rotatedBoundaryLayer.loops:
				for point in loop:
					pointVector3 = Vector3( point.real, point.imag, rotatedBoundaryLayer.z )
					self.cornerMaximum = euclidean.getPointMaximum( self.cornerMaximum, pointVector3 )
					self.cornerMinimum = euclidean.getPointMinimum( self.cornerMinimum, pointVector3 )
		return self.rotatedBoundaryLayers

	def parseSVG( self, svgText ):
		"Parse SVG text and store the layers."
		if svgText == '':
			return None
		svgText = svgText.replace( '\t', ' ' )
		svgText = svgText.replace( ';', ' ' )
		self.lines = gcodec.getTextLines( svgText )
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			self.layerThickness = getValueInQuotes( 'layerThickness', line, self.layerThickness )
			self.maximumZ = getValueInQuotes( 'maxZ', line, self.maximumZ )
			self.minimumZ = getValueInQuotes( 'minZ', line, self.minimumZ )
			if line.find( '</metadata>' ) != - 1:
				return
		self.lineIndex = 2

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the inset skein."
		line = self.lines[ lineIndex ].lstrip()
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '<path':
			addPathData( line, self.rotatedBoundaryLayers[ - 1 ].loops )
		elif firstWord == '<text':
			addTextData( line, self.rotatedBoundaryLayers )

	def setCarveBridgeLayerThickness( self, bridgeLayerThickness ):
		"Set the bridge layer thickness.  If the infill is not in the direction of the bridge, the bridge layer thickness should be given as None or not set at all."
		pass

	def setCarveLayerThickness( self, layerThickness ):
		"Set the layer thickness."
		pass

	def setCarveImportRadius( self, importRadius ):
		"Set the import radius."
		pass

	def setCarveIsCorrectMesh( self, isCorrectMesh ):
		"Set the is correct mesh flag."
		pass
