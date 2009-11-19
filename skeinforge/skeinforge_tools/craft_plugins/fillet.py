"""
Fillet is a script to fillet or bevel the corners on a gcode file.

The default 'Activate Fillet' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

Fillet rounds the corners slightly in a variety of ways.  This is to reduce corner blobbing and sudden extruder acceleration.  The 'Arc Point' method fillets the corners with an arc using the gcode point form.  The 'Arc Radius' method fillets with an arc using the gcode radius form.  The 'Arc Segment' method fillets corners with an arc composed of several segments.  The 'Bevel' method bevels each corner.  The default radio button choice is 'Bevel'.

The 'Corner FeedRate over Operating FeedRate' is the ratio of the feedRate in corners over the operating feedRate.  With a high value the extruder will move quickly in corners, accelerating quickly and leaving a thin extrusion.  With a low value, the extruder will move slowly in corners, accelerating gently and leaving a thick extrusion.  The default value is 1.0.  The 'Fillet Radius over Perimeter Width' ratio determines how much wide the fillet will be, the default is 0.35.  The 'Reversal Slowdown over Perimeter Width' ratio determines how far before a path reversal the extruder will slow down.  Some tools, like nozzle wipe, double back the path of the extruder and this option will add a slowdown point in that path so there won't be a sudden jerk at the end of the path.  The default value is 0.5 and if the value is less than 0.1 a slowdown will not be added.  If 'Use Intermediate FeedRate in Corners' is chosen, the feedRate entering the corner will be the average of the old feedRate and the new feedRate, the default is true.

The following examples fillet the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and fillet.py.


> python fillet.py
This brings up the fillet dialog.


> python fillet.py Screw Holder Bottom.stl
The fillet tool is parsing the file:
Screw Holder Bottom.stl
..
The fillet tool has created the file:
.. Screw Holder Bottom_fillet.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import fillet
>>> fillet.main()
This brings up the fillet dialog.


>>> fillet.writeOutput()
The fillet tool is parsing the file:
Screw Holder Bottom.stl
..
The fillet tool has created the file:
.. Screw Holder Bottom_fillet.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.meta_plugins import polyfile
from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text, filletRepository = None ):
	"Fillet a gcode linear move file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), filletRepository )

def getCraftedTextFromText( gcodeText, filletRepository = None ):
	"Fillet a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'fillet' ):
		return gcodeText
	if filletRepository == None:
		filletRepository = preferences.getReadRepository( FilletRepository() )
	if not filletRepository.activateFillet.value:
		return gcodeText
	if filletRepository.arcPoint.value:
		return ArcPointSkein().getCraftedGcode( filletRepository, gcodeText )
	elif filletRepository.arcRadius.value:
		return ArcRadiusSkein().getCraftedGcode( filletRepository, gcodeText )
	elif filletRepository.arcSegment.value:
		return ArcSegmentSkein().getCraftedGcode( filletRepository, gcodeText )
	elif filletRepository.bevel.value:
		return BevelSkein().getCraftedGcode( filletRepository, gcodeText )
	return gcodeText

def getRepositoryConstructor():
	"Get the repository constructor."
	return FilletRepository()

def writeOutput( fileName = '' ):
	"Fillet a gcode linear move file. Depending on the preferences, either arcPoint, arcRadius, arcSegment, bevel or do nothing."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'fillet' )


class BevelSkein:
	"A class to bevel a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.feedRateMinute = 960.0
		self.filletRadius = 0.2
		self.lineIndex = 0
		self.lines = None
		self.oldFeedRateMinute = None
		self.oldLocation = None
		self.shouldAddLine = True

	def addLinearMovePoint( self, feedRateMinute, point ):
		"Add a gcode linear move, feedRate and newline to the output."
		self.distanceFeedRate.addLine( self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( feedRateMinute, point.dropAxis( 2 ), point.z ) )

	def getCornerFeedRate( self ):
		"Get the corner feedRate, which may be based on the intermediate feedRate."
		feedRateMinute = self.feedRateMinute
		if self.filletRepository.useIntermediateFeedRateInCorners.value:
			if self.oldFeedRateMinute != None:
				feedRateMinute = 0.5 * ( self.oldFeedRateMinute + self.feedRateMinute )
		return feedRateMinute * self.cornerFeedRateOverOperatingFeedRate

	def getCraftedGcode( self, filletRepository, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.cornerFeedRateOverOperatingFeedRate = filletRepository.cornerFeedRateOverOperatingFeedRate.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.filletRepository = filletRepository
		self.parseInitialization( filletRepository )
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getExtruderOffReversalPoint( self, afterSegment, afterSegmentComplex, beforeSegment, beforeSegmentComplex, location ):
		"If the extruder is off and the path is reversing, add intermediate slow points."
		if self.filletRepository.reversalSlowdownDistanceOverPerimeterWidth.value < 0.1:
			return None
		if self.extruderActive:
			return None
		reversalBufferSlowdownDistance = self.reversalSlowdownDistance * 2.0
		afterSegmentComplexLength = abs( afterSegmentComplex )
		if afterSegmentComplexLength < reversalBufferSlowdownDistance:
			return None
		beforeSegmentComplexLength = abs( beforeSegmentComplex )
		if beforeSegmentComplexLength < reversalBufferSlowdownDistance:
			return None
		afterSegmentComplexNormalized = afterSegmentComplex / afterSegmentComplexLength
		beforeSegmentComplexNormalized = beforeSegmentComplex / beforeSegmentComplexLength
		if euclidean.getDotProduct( afterSegmentComplexNormalized, beforeSegmentComplexNormalized ) < 0.95:
			return None
		slowdownFeedRate = self.feedRateMinute * 0.5
		self.shouldAddLine = False
		beforePoint = euclidean.getPointPlusSegmentWithLength( self.reversalSlowdownDistance * abs( beforeSegment ) / beforeSegmentComplexLength, location, beforeSegment )
		self.addLinearMovePoint( self.feedRateMinute, beforePoint )
		self.addLinearMovePoint( slowdownFeedRate, location )
		afterPoint = euclidean.getPointPlusSegmentWithLength( self.reversalSlowdownDistance * abs( afterSegment ) / afterSegmentComplexLength, location, afterSegment )
		self.addLinearMovePoint( slowdownFeedRate, afterPoint )
		return afterPoint

	def getNextLocation( self ):
		"Get the next linear move.  Return none is none is found."
		for afterIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ afterIndex ]
			splitLine = line.split( ' ' )
			if gcodec.getFirstWord( splitLine ) == 'G1':
				nextLocation = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
				return nextLocation
		return None

	def linearMove( self, splitLine ):
		"Bevel a linear move."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		if self.oldLocation != None:
			nextLocation = self.getNextLocation()
			if nextLocation != None:
				location = self.splitPointGetAfter( location, nextLocation )
		self.oldLocation = location
		self.oldFeedRateMinute = self.feedRateMinute

	def parseInitialization( self, filletRepository ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> fillet </procedureDone>)' )
				return
			elif firstWord == '(<perimeterWidth>':
				perimeterWidth = abs( float( splitLine[ 1 ] ) )
				self.curveSection = 0.7 * perimeterWidth
				self.filletRadius = perimeterWidth * filletRepository.filletRadiusOverPerimeterWidth.value
				self.minimumRadius = 0.1 * perimeterWidth
				self.reversalSlowdownDistance = perimeterWidth * filletRepository.reversalSlowdownDistanceOverPerimeterWidth.value
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the bevel gcode."
		self.shouldAddLine = True
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
		if self.shouldAddLine:
			self.distanceFeedRate.addLine( line )

	def splitPointGetAfter( self, location, nextLocation ):
		"Bevel a point and return the end of the bevel.   should get complex for radius"
		if self.filletRadius < 2.0 * self.minimumRadius:
			return location
		afterSegment = nextLocation - location
		afterSegmentComplex = afterSegment.dropAxis( 2 )
		afterSegmentComplexLength = abs( afterSegmentComplex )
		thirdAfterSegmentLength = 0.333 * afterSegmentComplexLength
		if thirdAfterSegmentLength < self.minimumRadius:
			return location
		beforeSegment = self.oldLocation - location
		beforeSegmentComplex = beforeSegment.dropAxis( 2 )
		beforeSegmentComplexLength = abs( beforeSegmentComplex )
		thirdBeforeSegmentLength = 0.333 * beforeSegmentComplexLength
		if thirdBeforeSegmentLength < self.minimumRadius:
			return location
		extruderOffReversalPoint = self.getExtruderOffReversalPoint( afterSegment, afterSegmentComplex, beforeSegment, beforeSegmentComplex, location )
		if extruderOffReversalPoint != None:
			return extruderOffReversalPoint
		bevelRadius = min( thirdAfterSegmentLength, self.filletRadius )
		bevelRadius = min( thirdBeforeSegmentLength, bevelRadius )
		self.shouldAddLine = False
		beforePoint = euclidean.getPointPlusSegmentWithLength( bevelRadius * abs( beforeSegment ) / beforeSegmentComplexLength, location, beforeSegment )
		self.addLinearMovePoint( self.feedRateMinute, beforePoint )
		afterPoint = euclidean.getPointPlusSegmentWithLength( bevelRadius * abs( afterSegment ) / afterSegmentComplexLength, location, afterSegment )
		self.addLinearMovePoint( self.getCornerFeedRate(), afterPoint )
		return afterPoint


class ArcSegmentSkein( BevelSkein ):
	"A class to arc segment a skein of extrusions."
	def addArc( self, afterCenterDifferenceAngle, afterPoint, beforeCenterSegment, beforePoint, center ):
		"Add arc segments to the filleted skein."
		absoluteDifferenceAngle = abs( afterCenterDifferenceAngle )
#		steps = int( math.ceil( absoluteDifferenceAngle * 1.5 ) )
		steps = int( math.ceil( min( absoluteDifferenceAngle * 1.5, absoluteDifferenceAngle * abs( beforeCenterSegment ) / self.curveSection ) ) )
		stepPlaneAngle = euclidean.getPolar( afterCenterDifferenceAngle / steps, 1.0 )
		for step in xrange( 1, steps ):
			beforeCenterSegment = euclidean.getRoundZAxisByPlaneAngle( stepPlaneAngle, beforeCenterSegment )
			arcPoint = center + beforeCenterSegment
			self.addLinearMovePoint( self.getCornerFeedRate(), arcPoint )
		self.addLinearMovePoint( self.getCornerFeedRate(), afterPoint )

	def splitPointGetAfter( self, location, nextLocation ):
		"Fillet a point into arc segments and return the end of the last segment."
		if self.filletRadius < 2.0 * self.minimumRadius:
			return location
		afterSegment = nextLocation - location
		afterSegmentComplex = afterSegment.dropAxis( 2 )
		thirdAfterSegmentLength = 0.333 * abs( afterSegmentComplex )
		if thirdAfterSegmentLength < self.minimumRadius:
			return location
		beforeSegment = self.oldLocation - location
		beforeSegmentComplex = beforeSegment.dropAxis( 2 )
		thirdBeforeSegmentLength = 0.333 * abs( beforeSegmentComplex )
		if thirdBeforeSegmentLength < self.minimumRadius:
			return location
		extruderOffReversalPoint = self.getExtruderOffReversalPoint( afterSegment, afterSegmentComplex, beforeSegment, beforeSegmentComplex, location )
		if extruderOffReversalPoint != None:
			return extruderOffReversalPoint
		bevelRadius = min( thirdAfterSegmentLength, self.filletRadius )
		bevelRadius = min( thirdBeforeSegmentLength, bevelRadius )
		self.shouldAddLine = False
		beforePoint = euclidean.getPointPlusSegmentWithLength( bevelRadius * abs( beforeSegment ) / abs( beforeSegmentComplex ), location, beforeSegment )
		self.addLinearMovePoint( self.feedRateMinute, beforePoint )
		afterPoint = euclidean.getPointPlusSegmentWithLength( bevelRadius * abs( afterSegment ) / abs( afterSegmentComplex ), location, afterSegment )
		afterPointComplex = afterPoint.dropAxis( 2 )
		beforePointComplex = beforePoint.dropAxis( 2 )
		locationComplex = location.dropAxis( 2 )
		midPoint = 0.5 * ( afterPoint + beforePoint )
		midPointComplex = midPoint.dropAxis( 2 )
		midPointMinusLocationComplex = midPointComplex - locationComplex
		midPointLocationLength = abs( midPointMinusLocationComplex )
		if midPointLocationLength < 0.01 * self.filletRadius:
			self.addLinearMovePoint( self.getCornerFeedRate(), afterPoint )
			return afterPoint
		midPointAfterPointLength = abs( midPointComplex - afterPointComplex )
		midPointCenterLength = midPointAfterPointLength * midPointAfterPointLength / midPointLocationLength
		radius = math.sqrt( midPointCenterLength * midPointCenterLength + midPointAfterPointLength * midPointAfterPointLength )
		centerComplex = midPointComplex + midPointMinusLocationComplex * midPointCenterLength / midPointLocationLength
		center = Vector3( centerComplex.real, centerComplex.imag, midPoint.z )
		afterCenterComplex = afterPointComplex - centerComplex
		beforeMinusCenterCenterComplex = beforePointComplex - centerComplex
		beforeCenter = beforePoint - center
		beforeCenterComplex = beforeCenter.dropAxis( 2 )
		subtractComplexMirror = complex( beforeCenterComplex.real , - beforeCenterComplex.imag )
		differenceComplex = subtractComplexMirror * afterCenterComplex
		differenceAngle = math.atan2( differenceComplex.imag, differenceComplex.real )
		self.addArc( differenceAngle, afterPoint, beforeCenter, beforePoint, center )
		return afterPoint


class ArcPointSkein( ArcSegmentSkein ):
	"A class to arc point a skein of extrusions."
	def addArc( self, afterCenterDifferenceAngle, afterPoint, beforeCenterSegment, beforePoint, center ):
		"Add an arc point to the filleted skein."
		afterPointMinusBefore = afterPoint - beforePoint
		centerMinusBefore = center - beforePoint
		firstWord = 'G3'
		if afterCenterDifferenceAngle < 0.0:
			firstWord = 'G2'
		centerMinusBeforeComplex = centerMinusBefore.dropAxis( 2 )
		if abs( centerMinusBeforeComplex ) <= 0.0:
			return
		self.distanceFeedRate.output.write( self.distanceFeedRate.getFirstWordMovement( firstWord, afterPointMinusBefore ) )
		self.distanceFeedRate.output.write( self.getRelativeCenter( centerMinusBeforeComplex ) )
		self.distanceFeedRate.addLine( self.distanceFeedRate.getArcFeedRateString( afterCenterDifferenceAngle, afterPointMinusBefore, centerMinusBefore, self.getCornerFeedRate() ) )

	def getRelativeCenter( self, centerMinusBeforeComplex ):
		"Get the relative center."
		return ' I%s J%s' % ( self.distanceFeedRate.getRounded( centerMinusBeforeComplex.real ), self.distanceFeedRate.getRounded( centerMinusBeforeComplex.imag ) )


class ArcRadiusSkein( ArcPointSkein ):
	"A class to arc radius a skein of extrusions."
	def getRelativeCenter( self, centerMinusBeforeComplex ):
		"Get the relative center."
		radius = abs( centerMinusBeforeComplex )
		return ' R' + ( self.distanceFeedRate.getRounded( radius ) )


class FilletRepository:
	"A class to handle the fillet preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Filleted', self, '' )
		self.activateFillet = preferences.BooleanPreference().getFromValue( 'Activate Fillet', self, True )
		self.filletProcedureChoiceLabel = preferences.LabelDisplay().getFromName( 'Fillet Procedure Choice: ', self )
		filletRadio = []
		self.arcPoint = preferences.Radio().getFromRadio( 'Arc Point', filletRadio, self, False )
		self.arcRadius = preferences.Radio().getFromRadio( 'Arc Radius', filletRadio, self, False )
		self.arcSegment = preferences.Radio().getFromRadio( 'Arc Segment', filletRadio, self, False )
		self.bevel = preferences.Radio().getFromRadio( 'Bevel', filletRadio, self, True )
		self.cornerFeedRateOverOperatingFeedRate = preferences.FloatPreference().getFromValue( 'Corner FeedRate over Operating Feed Rate (ratio):', self, 1.0 )
		self.filletRadiusOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Fillet Radius over Perimeter Width (ratio):', self, 0.35 )
		self.reversalSlowdownDistanceOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Reversal Slowdown Distance over Perimeter Width (ratio):', self, 0.5 )
		self.useIntermediateFeedRateInCorners = preferences.BooleanPreference().getFromValue( 'Use Intermediate FeedRate in Corners', self, True )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Fillet'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.fillet.html' )

	def execute( self ):
		"Fillet button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


def main():
	"Display the fillet dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
