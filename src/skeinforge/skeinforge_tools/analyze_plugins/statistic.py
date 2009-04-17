"""
Statistic is a script to generate statistics a gcode file.

The default 'Activate Statistic' checkbox is on.  When it is on, the functions described below will work when called from the
skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether
or not the 'Activate Statistic' checkbox is on, when statistic is run directly.

When the 'Print Statistics' checkbox is on, the statistics will be printed to the console, the default is on.  When the 'Save
Statistics' checkbox is on, the statistics will be save as a .txt file, the default is off.

To run statistic, in a shell in the folder which statistic is in type:
> python statistic.py

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example generates statistics the gcode file Screw Holder_comb.gcode.  This example is run in a terminal in the folder which contains
Screw Holder_comb.gcode and statistic.py.


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
from skeinforge_tools import polyfile
import cStringIO
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


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
	preferences.readPreferences( statisticPreferences )
	writeStatisticFileGivenText( fileName, gcodec.getFileText( fileName ), statisticPreferences )

def writeOutput( fileName, gcodeText = '' ):
	"Write statistics for a skeinforge gcode file, if 'Write Statistics File for Skeinforge Chain' is selected."
	statisticPreferences = StatisticPreferences()
	preferences.readPreferences( statisticPreferences )
	if gcodeText == '':
		gcodeText = gcodec.getFileText( fileName )
	if statisticPreferences.activateStatistic.value:
		writeStatisticFileGivenText( fileName, gcodeText, statisticPreferences )

def writeStatisticFileGivenText( fileName, gcodeText, statisticPreferences ):
	"Write statistics for a gcode file."
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
		self.archive = []
		self.activateStatistic = preferences.BooleanPreference().getFromValue( 'Activate Statistic', True )
		self.archive.append( self.activateStatistic )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Generate Statistics for', '' )
		self.archive.append( self.fileNameInput )
		self.printStatistics = preferences.BooleanPreference().getFromValue( 'Print Statistics', True )
		self.archive.append( self.printStatistics )
		self.saveStatistics = preferences.BooleanPreference().getFromValue( 'Save Statistics', False )
		self.archive.append( self.saveStatistics )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Generate Statistics'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.statistic.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled, [ '_comment' ] )
		for fileName in fileNames:
			statisticFile( fileName )


class StatisticSkein:
	"A class to get statistics for a gcode skein."
	def __init__( self ):
		self.oldLocation = None
		self.output = cStringIO.StringIO()

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + "\n" )

	def addToPath( self, location ):
		"Add a point to travel and maybe extrusion."
		if self.oldLocation != None:
			travel = location.distance( self.oldLocation )
			if self.feedrateMinute > 0.0:
				self.totalBuildTime += 60.0 * travel / self.feedrateMinute
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

	def getLocationSetFeedrateToSplitLine( self, splitLine ):
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		indexOfF = gcodec.indexOfStartingWithSecond( "F", splitLine )
		if indexOfF > 0:
			self.feedrateMinute = gcodec.getDoubleAfterFirstLetter( splitLine[ indexOfF ] )
		return location

	def helicalMove( self, isCounterclockwise, splitLine ):
		"Get statistics for a helical move."
		if self.oldLocation == None:
			return
		location = self.getLocationSetFeedrateToSplitLine( splitLine )
		location += self.oldLocation
		center = self.oldLocation.copy()
		indexOfR = gcodec.indexOfStartingWithSecond( "R", splitLine )
		if indexOfR > 0:
			radius = gcodec.getDoubleAfterFirstLetter( splitLine[ indexOfR ] )
			halfLocationMinusOld = location - self.oldLocation
			halfLocationMinusOld *= 0.5
			halfLocationMinusOldLength = halfLocationMinusOld.magnitude()
			centerMidpointDistance = math.sqrt( radius * radius - halfLocationMinusOldLength * halfLocationMinusOldLength )
			centerMinusMidpoint = euclidean.getRotatedWiddershinsQuarterAroundZAxis( halfLocationMinusOld )
			centerMinusMidpoint.normalize()
			centerMinusMidpoint *= centerMidpointDistance
			if isCounterclockwise:
				center.setToVec3( halfLocationMinusOld + centerMinusMidpoint )
			else:
				center.setToVec3( halfLocationMinusOld - centerMinusMidpoint )
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
		location = self.getLocationSetFeedrateToSplitLine( splitLine )
		self.addToPath( location )

	def parseGcode( self, gcodeText ):
		"Parse gcode text and store the statistics."
		self.characters = 0
		self.cornerHigh = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.extruderActive = False
		self.extruderSpeed = 0.0
		self.extruderToggled = 0
		self.extrusionDiameter = None
		self.extrusionWidth = 0.4
		self.feedrateMinute = 600.0
		self.layerThickness = 0.4
		self.numberOfLines = 0
		self.procedures = []
		self.totalBuildTime = 0.0
		self.totalDistanceExtruded = 0.0
		self.totalDistanceTraveled = 0.0
		lines = gcodec.getTextLines( gcodeText )
		for line in lines:
			self.parseLine( line )
		averageFeedrate = self.totalDistanceTraveled / self.totalBuildTime
		self.characters += self.numberOfLines
		kilobytes = round( self.characters / 1024.0 )
		halfExtrusionWidth = 0.5 * self.extrusionWidth
		halfExtrusionCorner = Vector3( halfExtrusionWidth, halfExtrusionWidth, halfExtrusionWidth )
		self.cornerHigh += halfExtrusionCorner
		self.cornerLow -= halfExtrusionCorner
		extent = self.cornerHigh - self.cornerLow
		roundedHigh = euclidean.getRoundedPoint( self.cornerHigh )
		roundedLow = euclidean.getRoundedPoint( self.cornerLow )
		roundedExtent = euclidean.getRoundedPoint( extent )
		axisString =  " axis, the extrusion starts at "
		volumeExtruded = 0.0009 * self.extrusionWidth * self.layerThickness * self.totalDistanceExtruded # the 9 comes from a typical fill density of 0.9
		self.addLine( "On the X" + axisString + str( int ( roundedLow.x ) ) + " mm and ends at " + str( int ( roundedHigh.x ) ) + " mm, for a width of " + str( int ( extent.x ) ) + " mm" )
		self.addLine( "On the Y" + axisString + str( int ( roundedLow.y ) ) + " mm and ends at " + str( int ( roundedHigh.y ) ) + " mm, for a depth of " + str( int ( extent.y ) ) + " mm" )
		self.addLine( "On the Z" + axisString + str( int ( roundedLow.z ) ) + " mm and ends at " + str( int ( roundedHigh.z ) ) + " mm, for a height of " + str( int ( extent.z ) ) + " mm" )
		self.addLine( "The average feedrate is "  + str( int( round( averageFeedrate ) ) )  + " mm/s, (" + str( int( round( 60.0 * averageFeedrate ) ) ) + " mm/min)." )
		self.addLine( "The extruder speed is " + str( int( round( self.extruderSpeed ) ) ) )
		self.addLine( "The extruder was extruding "  + str( int( round( 100.0 * self.totalDistanceExtruded / self.totalDistanceTraveled ) ) ) + "% of the time." )
		self.addLine( "The extruder was toggled " + str( self.extruderToggled ) + " times." )
		if self.extrusionDiameter != None:
			self.addLine( "The extrusion diameter is "  + str( self.extrusionDiameter ) + " mm." )
		self.addLine( "The extrusion width is "  + str( self.extrusionWidth ) + " mm." )
		self.addLine( "The following procedures have been performed on the skein:" )
		for procedure in self.procedures:
			self.addLine( procedure )
		self.addLine( "The layer thickness is "  + str( self.layerThickness ) + " mm." )
		self.addLine( "The text has " + str( self.numberOfLines ) + " lines and a size of " + str( kilobytes ) + " KB." )
		self.addLine( "The total build time is " + str( int( round( self.totalBuildTime ) ) ) + " s." )
		self.addLine( "The total distance extruded is " + str( int( round( self.totalDistanceExtruded ) ) ) + " mm." )
		self.addLine( "The total distance traveled is " + str( int( round( self.totalDistanceTraveled ) ) ) + " mm." )
		self.addLine( "The volume extruded is "  + str( int( round( volumeExtruded ) ) ) + " cc." )

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
			self.extrusionDiameter = gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] )
		elif firstWord == '(<extrusionWidth>':
			self.extrusionWidth = gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] )
		elif firstWord == '(<layerThickness>':
			self.layerThickness = gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] )
		elif firstWord == '(<procedureDone>':
			self.procedures.append( splitLine[ 1 ] )


def main():
	"Display the statistics dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( StatisticPreferences() )

if __name__ == "__main__":
	main()
