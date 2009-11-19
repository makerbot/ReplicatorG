"""
Statistic is a script to generate statistics a gcode file.

The default 'Activate Statistic' checkbox is on.  When it is on, the functions described below will work when called from the skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether or not the 'Activate Statistic' checkbox is on, when statistic is run directly.

When the 'Print Statistics' checkbox is on, the statistics will be printed to the console, the default is on.  When the 'Save Statistics' checkbox is on, the statistics will be save as a .txt file, the default is off.

To run statistic, in a shell in the folder which statistic is in type:
> python statistic.py

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example generates statistics for the gcode file Screw Holder_comb.gcode.  This example is run in a terminal in the folder which contains Screw Holder_comb.gcode and statistic.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import statistic
>>> statistic.main()
This brings up the statistic dialog.


>>> statistic.statisticFile()
The statistics file is saved as Screw Holder_comb_statistic.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import cStringIO
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getRepositoryConstructor():
	"Get the repository constructor."
	return StatisticPreferences()

def getStatisticGcode( gcodeText ):
	"Get statistics for a gcode text."
	skein = StatisticSkein()
	skein.parseGcode( gcodeText )
	return skein.output.getvalue()

def statisticFile( fileName = '' ):
	"Write statistics for a gcode file.  If no fileName is specified, write statistics for the first gcode file in this folder that is not modified."
	if fileName == '':
		unmodified = gcodec.getUnmodifiedGCodeFiles()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	statisticPreferences = StatisticPreferences()
	preferences.getReadRepository( statisticPreferences )
	writeStatisticFileGivenText( fileName, gcodec.getFileText( fileName ), statisticPreferences )

def writeOutput( fileName, gcodeText = '' ):
	"Write statistics for a skeinforge gcode file, if 'Write Statistics File for Skeinforge Chain' is selected."
	statisticPreferences = StatisticPreferences()
	preferences.getReadRepository( statisticPreferences )
	if gcodeText == '':
		gcodeText = gcodec.getFileText( fileName )
	if statisticPreferences.activateStatistic.value:
		writeStatisticFileGivenText( fileName, gcodeText, statisticPreferences )

def writeStatisticFileGivenText( fileName, gcodeText, statisticPreferences ):
	"Write statistics for a gcode file."
	print( '' )
	print( '' )
	print( 'Statistics are being generated for the file ' + gcodec.getSummarizedFilename( fileName ) )
	statisticGcode = getStatisticGcode( gcodeText )
	if statisticPreferences.printStatistics.value:
		print( statisticGcode )
	if statisticPreferences.saveStatistics.value:
		gcodec.writeFileMessageEnd( '.txt', fileName, statisticGcode, 'The statistics file is saved as ' )


class StatisticPreferences:
	"A class to handle the statistics preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.activateStatistic = preferences.BooleanPreference().getFromValue( 'Activate Statistic', self, True )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Generate Statistics for', self, '' )
		self.printStatistics = preferences.BooleanPreference().getFromValue( 'Print Statistics', self, True )
		self.saveStatistics = preferences.BooleanPreference().getFromValue( 'Save Statistics', self, False )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Generate Statistics'
		self.saveCloseTitle = 'Save and Close'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.statistic.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled, [ '_comment' ] )
		for fileName in fileNames:
			statisticFile( fileName )


class StatisticSkein:
	"A class to get statistics for a gcode skein."
	def __init__( self ):
		self.extrusionDiameter = None
		self.oldLocation = None
		self.operatingFeedRatePerSecond = None
		self.output = cStringIO.StringIO()
		self.version = None

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + "\n" )

	def addToPath( self, location ):
		"Add a point to travel and maybe extrusion."
		if self.oldLocation != None:
			travel = location.distance( self.oldLocation )
			if self.feedRateMinute > 0.0:
				self.totalBuildTime += 60.0 * travel / self.feedRateMinute
			self.totalDistanceTraveled += travel
			if self.extruderActive:
				self.totalDistanceExtruded += travel
				self.cornerHigh = euclidean.getPointMaximum( self.cornerHigh, location )
				self.cornerLow = euclidean.getPointMinimum( self.cornerLow, location )
		self.oldLocation = location

	def extruderSet( self, active ):
		"Maybe increment the number of times the extruder was toggled."
		if self.extruderActive != active:
			self.extruderToggled += 1
		self.extruderActive = active

	def getLocationSetFeedRateToSplitLine( self, splitLine ):
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		indexOfF = gcodec.indexOfStartingWithSecond( "F", splitLine )
		if indexOfF > 0:
			self.feedRateMinute = gcodec.getDoubleAfterFirstLetter( splitLine[ indexOfF ] )
		return location

	def helicalMove( self, isCounterclockwise, splitLine ):
		"Get statistics for a helical move."
		if self.oldLocation == None:
			return
		location = self.getLocationSetFeedRateToSplitLine( splitLine )
		location += self.oldLocation
		center = self.oldLocation.copy()
		indexOfR = gcodec.indexOfStartingWithSecond( "R", splitLine )
		if indexOfR > 0:
			radius = gcodec.getDoubleAfterFirstLetter( splitLine[ indexOfR ] )
			halfLocationMinusOld = location - self.oldLocation
			halfLocationMinusOld *= 0.5
			halfLocationMinusOldLength = halfLocationMinusOld.magnitude()
			centerMidpointDistanceSquared = radius * radius - halfLocationMinusOldLength * halfLocationMinusOldLength
			centerMidpointDistance = math.sqrt( max( centerMidpointDistanceSquared, 0.0 ) )
			centerMinusMidpoint = euclidean.getRotatedWiddershinsQuarterAroundZAxis( halfLocationMinusOld )
			centerMinusMidpoint.normalize()
			centerMinusMidpoint *= centerMidpointDistance
			if isCounterclockwise:
				center.setToVector3( halfLocationMinusOld + centerMinusMidpoint )
			else:
				center.setToVector3( halfLocationMinusOld - centerMinusMidpoint )
		else:
			center.x = gcodec.getDoubleForLetter( "I", splitLine )
			center.y = gcodec.getDoubleForLetter( "J", splitLine )
		curveSection = 0.5
		center += self.oldLocation
		afterCenterSegment = location - center
		beforeCenterSegment = self.oldLocation - center
		afterCenterDifferenceAngle = euclidean.getAngleAroundZAxisDifference( afterCenterSegment, beforeCenterSegment )
		absoluteDifferenceAngle = abs( afterCenterDifferenceAngle )
		steps = int( round( 0.5 + max( absoluteDifferenceAngle * 2.4, absoluteDifferenceAngle * beforeCenterSegment.magnitude() / curveSection ) ) )
		stepPlaneAngle = euclidean.getPolar( afterCenterDifferenceAngle / steps, 1.0 )
		zIncrement = ( afterCenterSegment.z - beforeCenterSegment.z ) / float( steps )
		for step in xrange( 1, steps ):
			beforeCenterSegment = euclidean.getRoundZAxisByPlaneAngle( stepPlaneAngle, beforeCenterSegment )
			beforeCenterSegment.z += zIncrement
			arcPoint = center + beforeCenterSegment
			self.addToPath( arcPoint )
		self.addToPath( location )

	def linearMove( self, splitLine ):
		"Get statistics for a linear move."
		location = self.getLocationSetFeedRateToSplitLine( splitLine )
		self.addToPath( location )

	def parseGcode( self, gcodeText ):
		"Parse gcode text and store the statistics."
		self.absolutePerimeterWidth = 0.4
		self.characters = 0
		self.cornerHigh = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.extruderActive = False
		self.extruderSpeed = 0.0
		self.extruderToggled = 0
		self.feedRateMinute = 600.0
		self.layerThickness = 0.4
		self.numberOfLines = 0
		self.procedures = []
		self.totalBuildTime = 0.0
		self.totalDistanceExtruded = 0.0
		self.totalDistanceTraveled = 0.0
		lines = gcodec.getTextLines( gcodeText )
		for line in lines:
			self.parseLine( line )
		averageFeedRate = self.totalDistanceTraveled / self.totalBuildTime
		self.characters += self.numberOfLines
		kilobytes = round( self.characters / 1024.0 )
		halfPerimeterWidth = 0.5 * self.absolutePerimeterWidth
		halfExtrusionCorner = Vector3( halfPerimeterWidth, halfPerimeterWidth, halfPerimeterWidth )
		self.cornerHigh += halfExtrusionCorner
		self.cornerLow -= halfExtrusionCorner
		extent = self.cornerHigh - self.cornerLow
		roundedHigh = euclidean.getRoundedPoint( self.cornerHigh )
		roundedLow = euclidean.getRoundedPoint( self.cornerLow )
		roundedExtent = euclidean.getRoundedPoint( extent )
		axisString =  " axis, the extrusion starts at "
		crossSectionArea = 0.9 * self.absolutePerimeterWidth * self.layerThickness # 0.9 if from the typical fill density
		if self.extrusionDiameter != None:
			crossSectionArea = math.pi / 4.0 * self.extrusionDiameter * self.extrusionDiameter
		volumeExtruded = 0.001 * crossSectionArea * self.totalDistanceExtruded
		self.addLine( "On the X%s%s mm and ends at %s mm, for a width of %s mm." % ( axisString, int( roundedLow.x ), int( roundedHigh.x ), int( extent.x ) ) )
		self.addLine( "On the Y%s%s mm and ends at %s mm, for a depth of %s mm." % ( axisString, int( roundedLow.y ), int( roundedHigh.y ), int( extent.y ) ) )
		self.addLine( "On the Z%s%s mm and ends at %s mm, for a height of %s mm." % ( axisString, int( roundedLow.z ), int( roundedHigh.z ), int( extent.z ) ) )
		self.addLine( " " )
		self.addLine( "The average feedRate is %s mm/s, (%s mm/min)." % ( euclidean.getThreeSignificantFigures( averageFeedRate ), euclidean.getThreeSignificantFigures( 60.0 * averageFeedRate ) ) )
		self.addLine( "The cross section area is %s mm2." % euclidean.getThreeSignificantFigures( crossSectionArea ) )
		if self.extrusionDiameter != None:
			self.addLine( "The extrusion diameter is %s mm." % euclidean.getThreeSignificantFigures( self.extrusionDiameter ) )
		self.addLine( "The extruder speed is %s" % euclidean.getThreeSignificantFigures( self.extruderSpeed ) )
		self.addLine( "The extruder was extruding %s percent of the time." % euclidean.getThreeSignificantFigures( 100.0 * self.totalDistanceExtruded / self.totalDistanceTraveled ) )
		self.addLine( "The extruder was toggled %s times." % self.extruderToggled )
		self.addLine( "The layer thickness is %s mm." % euclidean.getThreeSignificantFigures( self.layerThickness ) )
		if self.operatingFeedRatePerSecond != None:
			flowRate = crossSectionArea * self.operatingFeedRatePerSecond
			self.addLine( "The operating flow rate is %s mm3/s." % euclidean.getThreeSignificantFigures( flowRate ) )
		self.addLine( 'The perimeter extrusion fill density ratio is %s' % euclidean.getThreeSignificantFigures( crossSectionArea / self.absolutePerimeterWidth / self.layerThickness ) )
		self.addLine( "The perimeter width is %s mm." % euclidean.getThreeSignificantFigures( self.absolutePerimeterWidth ) )
		self.addLine( " " )
		self.addLine( "The following procedures have been performed on the skein:" )
		for procedure in self.procedures:
			self.addLine( procedure )
		self.addLine( " " )
		self.addLine( "The text has %s lines and a size of %s KB." % ( self.numberOfLines, kilobytes ) )
		self.addLine( "The total build time is %s s." % int( round( self.totalBuildTime ) ) )
		self.addLine( "The total distance extruded is %s mm." % euclidean.getThreeSignificantFigures( self.totalDistanceExtruded ) )
		self.addLine( "The total distance traveled is %s mm." % euclidean.getThreeSignificantFigures( self.totalDistanceTraveled ) )
		if self.version != None:
			self.addLine( "The version is "  + self.version )
		self.addLine( "The volume extruded is %s cc." % euclidean.getThreeSignificantFigures( volumeExtruded ) )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the statistics."
		self.characters += len( line )
		self.numberOfLines += 1
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == 'G2':
			self.helicalMove( False, splitLine )
		elif firstWord == 'G3':
			self.helicalMove( True, splitLine )
		elif firstWord == 'M101':
			self.extruderSet( True )
		elif firstWord == 'M102':
			self.extruderSet( False )
		elif firstWord == 'M103':
			self.extruderSet( False )
		elif firstWord == 'M108':
			self.extruderSpeed = gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] )
		elif firstWord == '(<extrusionDiameter>':
			self.extrusionDiameter = float( splitLine[ 1 ] )
		elif firstWord == '(<layerThickness>':
			self.layerThickness = float( splitLine[ 1 ] )
		elif firstWord == '(<operatingFeedRatePerSecond>':
			self.operatingFeedRatePerSecond = float( splitLine[ 1 ] )
		elif firstWord == '(<perimeterWidth>':
			self.absolutePerimeterWidth = abs( float( splitLine[ 1 ] ) )
		elif firstWord == '(<procedureDone>':
			self.procedures.append( splitLine[ 1 ] )
		elif firstWord == '(<version>':
			self.version = splitLine[ 1 ]


def main():
	"Display the statistics dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
