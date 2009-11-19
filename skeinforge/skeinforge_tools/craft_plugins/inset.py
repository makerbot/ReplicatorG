#! /usr/bin/env python
"""
Inset is a script to inset a gcode file.

Inset will inset the outside outlines by half the perimeter width, and the inside outlines will be outset by half the perimeter width.

If "Add Custom Code for Temperature Reading" is selected, the M105 custom code for temperature reading will be added at the beginning of the file, the default is on.  If the "Turn Extruder Heater Off at Shut Down" preference is selected, the M104 S0 gcode line will be added to the end of the file to turn the extruder heater off by setting the extruder heater temperature to 0, this is the default choice.

The 'Bridge Width Multiplier' is the ratio of the extrusion width of a bridge layer over the extrusion width of the typical non bridge layers, the default is 1.0.

The 'Overlap Removal Width over Perimeter Width' is the ratio of the overlap removal width over the perimeter width, the default is 0.6.  Any part of the extrusion that comes within the overlap removal width of another is removed.  This is to prevent the extruder from depositing two extrusions right beside each other.  If the 'Overlap Removal Width over Perimeter Width' is less than 0.2, the overlap will not be removed.

The following examples inset the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and inset.py.

> python inset.py
This brings up the inset dialog.


> python inset.py Screw Holder Bottom.stl
The inset tool is parsing the file:
Screw Holder Bottom.stl
..
The inset tool has created the file:
.. Screw Holder Bottom_inset.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import inset
>>> inset.main()
This brings up the inset dialog.


>>> inset.writeOutput()
The inset tool is parsing the file:
Screw Holder Bottom.stl
..
The inset tool has created the file:
.. Screw Holder Bottom_inset.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.meta_plugins import polyfile
import math
import os
import sys


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
		if intercircle.isLarge( alreadyFilledInset, muchGreaterThanRadius ) or euclidean.isWiddershins( alreadyFilledInset ):
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

def getCraftedText( fileName, text = '', insetRepository = None ):
	"Inset the preface file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), insetRepository )

def getCraftedTextFromText( gcodeText, insetRepository = None ):
	"Inset the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'inset' ):
		return gcodeText
	if insetRepository == None:
		insetRepository = preferences.getReadRepository( InsetRepository() )
	return InsetSkein().getCraftedGcode( insetRepository, gcodeText )

def getRepositoryConstructor():
	"Get the repository constructor."
	return InsetRepository()

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
	euclidean.addXIntersectionIndexesFromLoopListsY( rotatedLoopLists, xIntersectionIndexList, pointBeginRotated.imag )
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
		totalNumberOfIntersections += euclidean.getNumberOfIntersectionsToLeft( loop, leftPoint )
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
	"Inset the carving of a gcode file.  If no fileName is specified, inset the first unmodified gcode file in this folder."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'inset' )


class InsetRepository:
	"A class to handle the inset preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Inset', self, '' )
		self.addCustomCodeForTemperatureReading = preferences.BooleanPreference().getFromValue( 'Add Custom Code for Temperature Reading', self, True )
		self.bridgeWidthMultiplier = preferences.FloatPreference().getFromValue( 'Bridge Width Multiplier (ratio):', self, 1.0 )
		self.overlapRemovalWidthOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Overlap Removal Width over Perimeter Width (ratio):', self, 0.6 )
		self.turnExtruderHeaterOffAtShutDown = preferences.BooleanPreference().getFromValue( 'Turn Extruder Heater Off at Shut Down', self, True )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Inset'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.inset.html' )

	def execute( self ):
		"Inset button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class InsetSkein:
	"A class to inset a skein of extrusions."
	def __init__( self ):
		self.boundary = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.lineIndex = 0
		self.rotatedBoundaryLayers = []
		self.shutdownLines = None

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
				addSegmentOutline( False, outlines, pointBegin, pointEnd, self.overlapRemovalWidth )
				addSegmentOutline( True, thickOutlines, pointBegin, pointEnd, self.overlapRemovalWidth )
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
			self.distanceFeedRate.addGcodeFromThreadZ( perimeterPath, z )

	def addGcodeFromRemainingLoop( self, loop, loopLists, radius, z ):
		"Add the remainder of the loop which does not overlap the alreadyFilledArounds loops."
		boundary = intercircle.getLargestInsetLoopFromLoopNoMatterWhat( loop, - radius )
		euclidean.addSurroundingLoopBeginning( boundary, self, z )
		self.addGcodePerimeterBlockFromRemainingLoop( loop, loopLists, radius, z )
		self.distanceFeedRate.addLine( '(</surroundingLoop>)' )

	def addGcodePerimeterBlockFromRemainingLoop( self, loop, loopLists, radius, z ):
		"Add the perimter block remainder of the loop which does not overlap the alreadyFilledArounds loops."
		if self.insetRepository.overlapRemovalWidthOverPerimeterWidth.value < 0.2:
			self.distanceFeedRate.addPerimeterBlock( loop, z )
			return
		isIntersectingSelf = isIntersectingItself( loop, self.overlapRemovalWidth )
		if isIntersectingWithinLists( loop, loopLists ) or isIntersectingSelf:
			self.addGcodeFromPerimeterPaths( isIntersectingSelf, loop, loopLists, radius, z )
		else:
			self.distanceFeedRate.addPerimeterBlock( loop, z )

	def addInitializationToOutput( self ):
		"Add initialization gcode to the output."
		if self.insetRepository.addCustomCodeForTemperatureReading.value:
			self.distanceFeedRate.addLine( 'M105' ) # Custom code for temperature reading.

	def addInset( self, rotatedBoundaryLayer ):
		"Add inset to the layer."
		alreadyFilledArounds = []
		halfWidth = self.halfPerimeterWidth
		self.distanceFeedRate.addLine( '(<layer> %s )' % rotatedBoundaryLayer.z ) # Indicate that a new layer is starting.
		if rotatedBoundaryLayer.rotation != None:
			halfWidth *= self.insetRepository.bridgeWidthMultiplier.value
			self.distanceFeedRate.addTagBracketedLine( 'bridgeRotation', rotatedBoundaryLayer.rotation ) # Indicate the bridge rotation.
		extrudateLoops = intercircle.getInsetLoopsFromLoops( halfWidth, rotatedBoundaryLayer.loops )
		for extrudateLoop in extrudateLoops:
			self.addGcodeFromRemainingLoop( extrudateLoop, alreadyFilledArounds, halfWidth, rotatedBoundaryLayer.z )
			addAlreadyFilledArounds( alreadyFilledArounds, extrudateLoop, self.overlapRemovalWidth )
		self.distanceFeedRate.addLine( '(</layer>)' )

	def addRotatedLoopLayer( self, z ):
		"Add rotated loop layer."
		self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		self.rotatedBoundaryLayers.append( self.rotatedBoundaryLayer )

	def addShutdownToOutput( self ):
		"Add shutdown gcode to the output."
		if len( self.shutdownLines ) > 0:
			self.distanceFeedRate.addLine( self.shutdownLines[ 0 ] )
		if self.insetRepository.turnExtruderHeaterOffAtShutDown.value:
			self.distanceFeedRate.addLine( 'M104 S0' ) # Turn extruder heater off.
		self.distanceFeedRate.addLines( self.shutdownLines[ 1 : ] )

	def getCraftedGcode( self, insetRepository, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.insetRepository = insetRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			self.addInset( rotatedBoundaryLayer )
		self.addShutdownToOutput()
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(<decimalPlacesCarried>':
				self.addInitializationToOutput()
				self.distanceFeedRate.addTagBracketedLine( 'bridgeWidthMultiplier', self.distanceFeedRate.getRounded( self.insetRepository.bridgeWidthMultiplier.value ) )
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'inset' )
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
				self.halfPerimeterWidth = 0.5 * self.perimeterWidth
				self.overlapRemovalWidth = self.perimeterWidth * self.insetRepository.overlapRemovalWidthOverPerimeterWidth.value
			elif firstWord == '(<layer>':
				self.lineIndex -= 1
				return
			self.distanceFeedRate.addLine( line )

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
		elif ( firstWord == '(<bridgeRotation>' or firstWord == '<!--bridgeRotation-->' ):
			secondWordWithoutBrackets = splitLine[ 1 ].replace( '(', '' ).replace( ')', '' )
			self.rotatedBoundaryLayer.rotation = complex( secondWordWithoutBrackets )
		elif firstWord == '(<layer>':
			self.addRotatedLoopLayer( float( splitLine[ 1 ] ) )
		elif firstWord == '(<surroundingLoop>)':
			self.boundary = []
			self.rotatedBoundaryLayer.loops.append( self.boundary )
		elif firstWord == '(</extrusion>)':
			self.shutdownLines = []
		if self.shutdownLines != None:
			self.shutdownLines.append( line )


def main():
	"Display the inset dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
