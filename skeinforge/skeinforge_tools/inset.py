#! /usr/bin/env python
"""
Inset is a script to inset the carvings of an svg file.

Inset insets the svg slices into gcode extrusion layers.  The 'Extrusion Perimeter Width over Thickness' ratio is the ratio of the
extrusion perimeter width over the layer thickness.  The higher the value the more the perimeter will be inset, the default is 1.8.

The 'Infill Perimeter Overlap' ratio is the amount the infill overlaps the perimeter over the extrusion width.  The higher the value the
more the infill will overlap the perimeter, and the thicker join between the infill and the perimeter.  If the value is too high, the join will
be so thick that the nozzle will run plow through the join below making a mess, the default is 0.05.  There are two choices for the
infill perimeter overlap method of calculation.  If the 'Calculate Overlap from Perimeter and Infill' option is chosen, the overlap will be
calculated from the average of the perimeter width and the infill width, this is the default choice.  If the 'Calculate Overlap from
Perimeter Only' option is chosen, the overlap will be calculated from the perimeter width only.

If the "Start at Home" preference is selected, the G28 gcode will be added at the beginning of the file, the default is off

When inset is generating the code, if there is a file start.txt, it will add that to the very beginning of the gcode. After it has added some
initialization code and just before it adds the extrusion gcode, it will add the file endofthebeginning.txt if it exists. At the very end, it will
add the file end.txt if it exists. Carve does not care if the text file names are capitalized, but some file systems do not handle file name
cases properly, so to be on the safe side you should give them lower case names.  It will first look for the file in the same directory as
inset, if it does not find it it will look in ~/.skeinforge/gcode_scripts.

The following examples inset the files Screw Holder Bottom.gcode & Screw Holder Bottom.stl.  The examples are run in a terminal in
the folder which contains Screw Holder Bottom.stl and inset.py.


> python inset.py
This brings up the dialog, after clicking 'Inset', the following is printed:
File Screw Holder Bottom.stl is being chain insetted.
The insetted file is saved as Screw Holder Bottom_inset.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import inset
>>> inset.main()
File Screw Holder Bottom.stl is being insetted.
The insetted file is saved as Screw Holder Bottom_inset.gcode
It took 3 seconds to inset the file.


>>> inset.writeOutput()
File Screw Holder Bottom.stl is being insetted.
The insetted file is saved as Screw Holder Bottom_inset.gcode
It took 3 seconds to inset the file.

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools import analyze
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools import polyfile
from skeinforge_tools import carve
import cStringIO
import math
import os
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"


def addAlreadyFilledArounds( alreadyFilledArounds, loop, radius ):
	"Add already filled loops around loop to alreadyFilledArounds."
	radius = abs( radius )
	alreadyFilledLoop = []
	slightlyGreaterThanRadius = 1.01 * radius
	muchGreaterThanRadius = 2.5 * radius
	circleNodes = intercircle.getCircleNodesFromLoop( loop, slightlyGreaterThanRadius )
	centers = intercircle.getCentersFromCircleNodes( circleNodes )
	for center in centers:
		alreadyFilledInset = intercircle.getSimplifiedInsetFromClockwiseLoop( center, radius )
		if euclidean.isLarge( alreadyFilledInset, muchGreaterThanRadius ) or euclidean.isWiddershins( alreadyFilledInset ):
			alreadyFilledLoop.append( alreadyFilledInset )
	if len( alreadyFilledLoop ) > 0:
		alreadyFilledArounds.append( alreadyFilledLoop )

def addSegmentOutline( isThick, outlines, pointBegin, pointEnd, width ):
	"Add a diamond or hexagonal outline for a line segment."
	width = abs( width )
	exclusionWidth = 0.6 * width
	slope = 0.2
	if isThick:
		slope = 3.0
		exclusionWidth = 0.8 * width
	segment = pointEnd - pointBegin
	segmentLength = abs( segment )
	if segmentLength == 0.0:
		return
	normalizedSegment = segment / segmentLength
	outline = []
	segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
	pointBeginRotated = segmentYMirror * pointBegin
	pointEndRotated = segmentYMirror * pointEnd
	along = 0.05
	alongLength = along * segmentLength
	if alongLength > 0.1 * exclusionWidth:
		along *= 0.1 * exclusionWidth / alongLength
	alongEnd = 1.0 - along
	remainingToHalf = 0.5 - along
	alongToWidth = exclusionWidth / slope / segmentLength
	pointBeginIntermediate = euclidean.getIntermediateLocation( along, pointBeginRotated, pointEndRotated )
	pointEndIntermediate = euclidean.getIntermediateLocation( alongEnd, pointBeginRotated, pointEndRotated )
	outline.append( pointBeginIntermediate )
	verticalWidth = complex( 0.0, exclusionWidth )
	if alongToWidth > 0.9 * remainingToHalf:
		verticalWidth = complex( 0.0, slope * remainingToHalf * segmentLength )
		middle = ( pointBeginIntermediate + pointEndIntermediate ) * 0.5
		middleDown = middle - verticalWidth
		middleUp = middle + verticalWidth
		outline.append( middleUp )
		outline.append( pointEndIntermediate )
		outline.append( middleDown )
	else:
		alongOutsideBegin = along + alongToWidth
		alongOutsideEnd = alongEnd - alongToWidth
		outsideBeginCenter = euclidean.getIntermediateLocation( alongOutsideBegin, pointBeginRotated, pointEndRotated )
		outsideBeginCenterDown = outsideBeginCenter - verticalWidth
		outsideBeginCenterUp = outsideBeginCenter + verticalWidth
		outsideEndCenter = euclidean.getIntermediateLocation( alongOutsideEnd, pointBeginRotated, pointEndRotated )
		outsideEndCenterDown = outsideEndCenter - verticalWidth
		outsideEndCenterUp = outsideEndCenter + verticalWidth
		outline.append( outsideBeginCenterUp )
		outline.append( outsideEndCenterUp )
		outline.append( pointEndIntermediate )
		outline.append( outsideEndCenterDown )
		outline.append( outsideBeginCenterDown )
	outlines.append( euclidean.getPointsRoundZAxis( normalizedSegment, outline ) )

def getInsetChainGcode( fileName, gcodeText, insetPreferences = None ):
	"Inset the carves of a gcode text.  Chain inset the gcode if it is not already carved."
	if gcodeText == '':
		if fileName[ - len( '.svg' ) : ] == '.svg':
			gcodeText = gcodec.getFileText( fileName )
	if not gcodec.isProcedureDone( gcodeText, '"carve"' ): #"carve" is used instead of carve because quotes have to be used for strings in svg files
		gcodeText = carve.getCarveGcode( fileName )
	return getInsetGcode( gcodeText, insetPreferences )

def getInsetGcode( gcodeText, insetPreferences = None ):
	"Inset the carves of a gcode text."
	if gcodeText == '':
		return ''
	if gcodec.isProcedureDone( gcodeText, 'inset' ):
		return gcodeText
	if insetPreferences == None:
		insetPreferences = InsetPreferences()
		preferences.readPreferences( insetPreferences )
	skein = InsetSkein()
	skein.parseGcode( insetPreferences, gcodeText )
	return skein.output.getvalue()

def getSegmentsFromPoints( aroundLists, loopLists, pointBegin, pointEnd ):
	"Get endpoint segments from the beginning and end of a line segment."
	normalizedSegment = pointEnd - pointBegin
	normalizedSegmentLength = abs( normalizedSegment )
	if normalizedSegmentLength == 0.0:
		return
	normalizedSegment /= normalizedSegmentLength
	segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
	pointBeginRotated = segmentYMirror * pointBegin
	pointEndRotated = segmentYMirror * pointEnd
	rotatedLoopLists = []
	for loopList in loopLists:
		rotatedLoopList = []
		rotatedLoopLists.append( rotatedLoopList )
		for loop in loopList:
			rotatedLoop = euclidean.getPointsRoundZAxis( segmentYMirror, loop )
			rotatedLoopList.append( rotatedLoop )
	xIntersectionIndexList = []
	xIntersectionIndexList.append( euclidean.XIntersectionIndex( - 1, pointBeginRotated.real ) )
	xIntersectionIndexList.append( euclidean.XIntersectionIndex( - 1, pointEndRotated.real ) )
	euclidean.addXIntersectionIndexesFromLoopLists( rotatedLoopLists, xIntersectionIndexList, pointBeginRotated.imag )
	segments = euclidean.getSegmentsFromXIntersectionIndexes( xIntersectionIndexList, pointBeginRotated.imag )
	insideSegments = []
	for segment in segments:
		insideSegment = euclidean.getSegmentFromPoints( normalizedSegment * segment[ 0 ].point, normalizedSegment * segment[ 1 ].point )
		if len( aroundLists ) < 1:
			insideSegments.append( insideSegment )
		elif isSegmentInsideAround( aroundLists, insideSegment ):
			insideSegments.append( insideSegment )
	return insideSegments

def isCloseToLast( paths, point, radius ):
	"Determine if the point is close to the last point of the last path."
	if len( paths ) < 1:
		return False
	lastPath = paths[ - 1 ]
	return abs( lastPath[ - 1 ] - point ) < radius

def isIntersectingItself( loop, width ):
	"Determine if the loop is intersecting itself."
	outlines = []
	for pointIndex in xrange( len( loop ) ):
		pointBegin = loop[ pointIndex ]
		pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
		if euclidean.isLineIntersectingLoops( outlines, pointBegin, pointEnd ):
			return True
		addSegmentOutline( False, outlines, pointBegin, pointEnd, width )
	return False

def isIntersectingWithinList( loop, loopList ):
	"Determine if the loop is intersecting or is within the loop list."
	if euclidean.isLoopIntersectingLoops( loop, loopList ):
		return True
	totalNumberOfIntersections = 0
	for otherLoop in loopList:
		leftPoint = euclidean.getLeftPoint( otherLoop )
		totalNumberOfIntersections += euclidean.getNumberOfIntersectionsToLeft( leftPoint, loop )
	return totalNumberOfIntersections % 2 == 1

def isIntersectingWithinLists( loop, loopLists ):
	"Determine if the loop is intersecting or is within the loop lists."
	for loopList in loopLists:
		if isIntersectingWithinList( loop, loopList ):
			return True
	return False

def isSegmentInsideAround( aroundLists, segment ):
	"Determine if the segment is inside an around."
	midpoint = 0.5 * ( segment[ 0 ].point + segment[ 1 ].point )
	for aroundList in aroundLists:
		if euclidean.isPointInsideLoops( aroundList, midpoint ):
			return True
	return False

def writeOutput( fileName = '' ):
	"Inset the carves of a gcode file.  Chain carve the file if it is a GNU TriangulatedSurface file.  If no fileName is specified, inset the first unmodified gcode file in this folder."
	if fileName == '':
		unmodified = interpret.getGNUTranslatorFilesUnmodified()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	startTime = time.time()
	insetPreferences = InsetPreferences()
	preferences.readPreferences( insetPreferences )
	print( 'File ' + gcodec.getSummarizedFilename( fileName ) + ' is being chain insetted.' )
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_inset.gcode'
	insetGcode = getInsetChainGcode( fileName, '', insetPreferences )
	if insetGcode == '':
		return
	gcodec.writeFileText( suffixFilename, insetGcode )
	print( 'The insetted file is saved as ' + suffixFilename )
	analyze.writeOutput( suffixFilename, insetGcode )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to inset the file.' )


class InsetPreferences:
	"A class to handle the inset preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive = []
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.extrusionPerimeterWidthOverThickness = preferences.FloatPreference().getFromValue( 'Extrusion Perimeter Width over Thickness (ratio):', 1.8 )
		self.archive.append( self.extrusionPerimeterWidthOverThickness )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Insetted', '' )
		self.archive.append( self.fileNameInput )
		self.infillPerimeterOverlap = preferences.FloatPreference().getFromValue( 'Infill Perimeter Overlap (ratio):', 0.05 )
		self.archive.append( self.infillPerimeterOverlap )
		self.infillPerimeterOverlapMethodOfCalculationLabel = preferences.LabelDisplay().getFromName( 'Infill Perimeter Overlap Method of Calculation: ' )
		self.archive.append( self.infillPerimeterOverlapMethodOfCalculationLabel )
		infillRadio = []
		self.perimeterInfillPreference = preferences.Radio().getFromRadio( 'Calculate Overlap from Perimeter and Infill', infillRadio, True )
		self.archive.append( self.perimeterInfillPreference )
		self.perimeterPreference = preferences.Radio().getFromRadio( 'Calculate Overlap from Perimeter Only', infillRadio, False )
		self.archive.append( self.perimeterPreference )
		self.startAtHome = preferences.BooleanPreference().getFromValue( 'Start at Home', False )
		self.archive.append( self.startAtHome )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Inset'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.inset.html' )

	def execute( self ):
		"Inset button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class InsetSkein:
	"A class to inset a skein of extrusions."
	def __init__( self ):
		self.boundary = None
		self.decimalPlacesCarried = 3
		self.extruderActive = False
		self.lineIndex = 0
		self.oldLocation = None
		self.oldOrderedLocation = Vector3()
		self.output = cStringIO.StringIO()
		self.rotatedBoundaryLayers = []
		self.thread = None

	def addFromUpperLowerFile( self, fileName ):
		"Add lines of text from the fileName or the lowercase fileName, if there is no file by the original fileName in the directory."
		fileText = preferences.getFileInGivenPreferencesDirectory( os.path.dirname( __file__ ), fileName )
		if fileText == '':
			return
		fileLines = gcodec.getTextLines( fileText )
		for line in fileLines:
			self.addLine( line )

	def addGcodeFromPerimeterPaths( self, isIntersectingSelf, loop, loopLists, radius, z ):
		"Add the perimeter paths to the output."
		segments = []
		outlines = []
		thickOutlines = []
		allLoopLists = loopLists[ : ] + [ thickOutlines ]
		aroundLists = loopLists
#		if euclidean.isWiddershins( loop ):
#			aroundLists = []
		for pointIndex in xrange( len( loop ) ):
			pointBegin = loop[ pointIndex ]
			pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
			if isIntersectingSelf:
				if euclidean.isLineIntersectingLoops( outlines, pointBegin, pointEnd ):
					segments += getSegmentsFromPoints( [], allLoopLists, pointBegin, pointEnd )
				else:
					segments += getSegmentsFromPoints( [], loopLists, pointBegin, pointEnd )
				addSegmentOutline( False, outlines, pointBegin, pointEnd, self.extrusionWidth )
				addSegmentOutline( True, thickOutlines, pointBegin, pointEnd, self.extrusionWidth )
			else:
				segments += getSegmentsFromPoints( aroundLists, loopLists, pointBegin, pointEnd )
		perimeterPaths = []
		path = []
		muchSmallerThanRadius = 0.1 * radius
		for segment in segments:
			pointBegin = segment[ 0 ].point
			if not isCloseToLast( perimeterPaths, pointBegin, muchSmallerThanRadius ):
				path = [ pointBegin ]
				perimeterPaths.append( path )
			path.append( segment[ 1 ].point )
		if len( perimeterPaths ) > 1:
			firstPath = perimeterPaths[ 0 ]
			lastPath = perimeterPaths[ - 1 ]
			if abs( lastPath[ - 1 ] - firstPath[ 0 ] ) < 0.1 * muchSmallerThanRadius:
				connectedBeginning = lastPath[ : - 1 ] + firstPath
				perimeterPaths[ 0 ] = connectedBeginning
				perimeterPaths.remove( lastPath )
		for perimeterPath in perimeterPaths:
			self.addGcodeFromThreadZ( perimeterPath, z )

	def addGcodeFromRemainingLoop( self, loop, loopLists, radius, z ):
		"Add the remainder of the loop which does not overlap the alreadyFilledArounds loops."
		euclidean.addSurroundingLoopBeginning( loop, self, z )
		isIntersectingSelf = isIntersectingItself( loop, self.extrusionWidth )
		if isIntersectingWithinLists( loop, loopLists ) or isIntersectingSelf:
			self.addGcodeFromPerimeterPaths( isIntersectingSelf, loop, loopLists, radius, z )
		else:
			self.addLine( '(<perimeter>)' ) # Indicate that a perimeter is beginning.
			self.addGcodeFromThreadZ( loop + [ loop[ 0 ] ], z )
			self.addLine( '(</perimeter>)' ) # Indicate that a perimeter is beginning.
		self.addLine( '(</surroundingLoop>)' )

	def addGcodeFromThreadZ( self, thread, z ):
		"Add a thread to the output."
		if len( thread ) > 0:
			self.addGcodeMovementZ( thread[ 0 ], z )
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			print( thread )
			return
		self.addLine( "M101" ) # Turn extruder on.
		for point in thread[ 1 : ]:
			self.addGcodeMovementZ( point, z )
		self.addLine( "M103" ) # Turn extruder off.

	def addGcodeMovementZ( self, point, z ):
		"Add a movement to the output."
		self.addLine( "G1 X%s Y%s Z%s" % ( self.getRounded( point.real ), self.getRounded( point.imag ), self.getRounded( z ) ) )

	def addInitializationToOutput( self ):
		"Add initialization gcode to the output."
		self.addFromUpperLowerFile( 'Start.txt' ) # Add a start file if it exists.
#		self.addLine( '(<creator> skeinforge June 8, 2009 </creator>)' ) # GCode formatted comment
		self.addLine( 'M110' ) # GCode for compatibility with Nophead's code.
		self.addLine( '(<extruderInitialization>)' ) # GCode formatted comment
#		self.addLine( 'G21' ) # Set units to mm.
#		self.addLine( 'G90' ) # Set positioning to absolute.
#		if self.insetPreferences.startAtHome.value:
#			self.addLine( 'G28' ) # Start at home.
#		self.addLine( 'M103' ) # Turn extruder off.
#		self.addLine( 'M105' ) # Custom code for temperature reading.
		self.addFromUpperLowerFile( 'EndOfTheBeginning.txt' ) # Add a second start file if it exists.
		self.addLine( '(<decimalPlacesCarried> ' + str( self.decimalPlacesCarried ) + ' </decimalPlacesCarried>)' ) # Set decimal places carried.
		self.addLine( '(<layerThickness> ' + self.getRounded( self.layerThickness ) + ' </layerThickness>)' ) # Set layer thickness.
		self.addLine( '(<extrusionPerimeterWidth> ' + self.getRounded( self.extrusionPerimeterWidth ) + ' </extrusionPerimeterWidth>)' ) # Set extrusion perimeter width.
		self.addLine( '(<extrusionWidth> ' + self.getRounded( self.extrusionWidth ) + ' </extrusionWidth>)' ) # Set extrusion width.
		self.addLine( '(<fillInset> ' + str( self.fillInset ) + ' </fillInset>)' ) # Set fill inset.
		# Set bridge extrusion width
		self.addLine( '(<infillBridgeWidthOverExtrusionWidth> ' + euclidean.getRoundedToThreePlaces( self.infillBridgeWidthOverExtrusionWidth ) + ' </infillBridgeWidthOverExtrusionWidth>)' )
		self.addLine( '(<procedureDone> carve </procedureDone>)' ) # The skein has been carved.
		self.addLine( '(<procedureDone> inset </procedureDone>)' ) # The skein has been carved.
		self.addLine( '(</extruderInitialization>)' ) # Initialization is finished, extrusion is starting.
		self.addLine( '(<extrusion>)' ) # Initialization is finished, extrusion is starting.

	def addInset( self, rotatedBoundaryLayer ):
		"Add inset to the carve layer."
		alreadyFilledArounds = []
		halfWidth = self.halfExtrusionPerimeterWidth
		self.addLine( '(<layer> %s )' % rotatedBoundaryLayer.z ) # Indicate that a new layer is starting.
		if rotatedBoundaryLayer.rotation != None:
			halfWidth *= self.infillBridgeWidthOverExtrusionWidth
			self.addLine( '(<bridgeDirection> ' + str( rotatedBoundaryLayer.rotation ) + ' </bridgeDirection>)' ) # Indicate the bridge direction.
		for loop in rotatedBoundaryLayer.loops:
			extrudateLoops = intercircle.getInsetLoopsFromLoop( halfWidth, loop )
			for extrudateLoop in extrudateLoops:
				self.addGcodeFromRemainingLoop( extrudateLoop, alreadyFilledArounds, halfWidth, rotatedBoundaryLayer.z )
				addAlreadyFilledArounds( alreadyFilledArounds, extrudateLoop, self.fillInset )
		self.addLine( '(</layer>)' )

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		if len( line ) > 0:
			self.output.write( line + "\n" )

	def addPathData( self, line ):
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
				self.rotatedBoundaryLayer.loops.append( loop )
				pathSequence = []
			else:
				pathSequence.append( float( word ) )

	def addRotatedLoopLayer( self, z ):
		"Add rotated loop layer."
		self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		self.rotatedBoundaryLayers.append( self.rotatedBoundaryLayer )

	def addShutdownToOutput( self ):
		"Add shutdown gcode to the output."
		self.addLine( '(</extrusion>)' ) # GCode formatted comment
#		self.addLine( 'M103' ) # Turn extruder motor off.
#		self.addLine( 'M104 S0' ) # Turn extruder heater off.
		self.addFromUpperLowerFile( 'End.txt' ) # Add an end file if it exists.

	def addTextData( self, line ):
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
		self.addRotatedLoopLayer( float( splitLine[ zIndex + 1 ] ) )

	def getRounded( self, number ):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToDecimalPlacesString( self.decimalPlacesCarried, number )

	def parseGcode( self, insetPreferences, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.insetPreferences = insetPreferences
		gcodeText = gcodeText.replace( '\t', ' ' )
		gcodeText = gcodeText.replace( ';', ' ' )
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		self.addInitializationToOutput()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			self.addInset( rotatedBoundaryLayer )
		self.addShutdownToOutput()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			splitLine = gcodec.getWithoutBracketsEqualTab( line ).split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'infillBridgeWidthOverExtrusionWidth':
				self.infillBridgeWidthOverExtrusionWidth = float( splitLine[ 1 ] )
			elif firstWord == 'decimalPlacesCarried':
				self.decimalPlacesCarried = int( splitLine[ 1 ] )
			elif firstWord == 'layerThickness':
				self.layerThickness = float( splitLine[ 1 ] )
				self.extrusionPerimeterWidth = self.insetPreferences.extrusionPerimeterWidthOverThickness.value * self.layerThickness
				self.halfExtrusionPerimeterWidth = 0.5 * self.extrusionPerimeterWidth
			elif firstWord == 'extrusionWidth':
				self.extrusionWidth = float( splitLine[ 1 ] )
				self.fillInset = self.extrusionPerimeterWidth - self.extrusionPerimeterWidth * self.insetPreferences.infillPerimeterOverlap.value
				if self.insetPreferences.perimeterInfillPreference.value:
					self.fillInset = self.halfExtrusionPerimeterWidth + 0.5 * self.extrusionWidth - self.extrusionWidth * self.insetPreferences.infillPerimeterOverlap.value
			elif firstWord == 'extrusionStart':
				return

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the inset skein."
		line = self.lines[ lineIndex ].lstrip()
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine( None, splitLine )
			self.boundary.append( location.dropAxis( 2 ) )
		elif ( firstWord == '(<bridgeDirection>' or firstWord == '<!--bridgeDirection-->' ):
			secondWordWithoutBrackets = splitLine[ 1 ].replace( '(', '' ).replace( ')', '' )
			self.rotatedBoundaryLayer.rotation = complex( secondWordWithoutBrackets )
		elif firstWord == '(<layer>':
			self.addRotatedLoopLayer( float( splitLine[ 1 ] ) )
		elif firstWord == '<path':
			self.addPathData( line )
		elif firstWord == '(<surroundingLoop>)':
			self.boundary = []
			self.rotatedBoundaryLayer.loops.append( self.boundary )
		elif firstWord == '<text':
			self.addTextData( line )


def main( hashtable = None ):
	"Display the inset dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( InsetPreferences() )

if __name__ == "__main__":
	main()
