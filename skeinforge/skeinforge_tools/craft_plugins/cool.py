"""
Cool is a script to cool the shape.

Allan Ecker aka The Masked Retriever's has written the "Skeinforge Quicktip: Cool" at:
http://blog.thingiverse.com/2009/07/28/skeinforge-quicktip-cool/

The default 'Activate Cool' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The important value for the cool preferences is "Minimum Layer Time (seconds)" which is the minimum amount of time the extruder will spend on a layer.  If it takes less time to extrude the layer than the minimum layer time, cool adds orbits with the extruder off to give the layer time to cool, so that the next layer is not extruded on a molten base.  The orbits will be around the largest island on that layer.  If the area of the largest island is as large as the square of the "Minimum Orbital Radius" then the orbits will be just within the island.  If the island is smaller, then the orbits will be in a square of the "Minimum Orbital Radius" around the center of the island.

Before the orbits, if there is a file cool_start.gcode, cool will add that to the start of the orbits. After it has added the orbits, it will add the file cool_end.gcode if it exists.  Cool does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  Cool looks for those files in the alterations folder in the .skeinforge folder in the home directory. If it doesn't find the file it then looks in the alterations folder in the skeinforge_tools folder. If it doesn't find anything there it looks in the skeinforge_tools folder.  The cool start and end text idea is from:
http://makerhahn.blogspot.com/2008/10/yay-minimug.html

If the 'Turn Fan On at Beginning' preference is true, cool will turn the fan on at the beginning of the fabrication.  If the 'Turn Fan Off at Ending' preference is true, cool will turn the fan off at the ending of the fabrication.

The following examples cool the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and cool.py.


> python cool.py
This brings up the cool dialog.


> python cool.py Screw Holder Bottom.stl
The cool tool is parsing the file:
Screw Holder Bottom.stl
..
The cool tool has created the file:
.. Screw Holder Bottom_cool.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import cool
>>> cool.main()
This brings up the cool dialog.


>>> cool.writeOutput()
The cool tool is parsing the file:
Screw Holder Bottom.stl
..
The cool tool has created the file:
.. Screw Holder Bottom_cool.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import math
import os
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text, coolRepository = None ):
	"Cool a gcode linear move text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), coolRepository )

def getCraftedTextFromText( gcodeText, coolRepository = None ):
	"Cool a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'cool' ):
		return gcodeText
	if coolRepository == None:
		coolRepository = preferences.getReadRepository( CoolRepository() )
	if not coolRepository.activateCool.value:
		return gcodeText
	return CoolSkein().getCraftedGcode( gcodeText, coolRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return CoolRepository()

def writeOutput( fileName = '' ):
	"Cool a gcode linear move file.  Chain cool the gcode if it is not already cooled. If no fileName is specified, cool the first unmodified gcode file in this folder."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'cool' )


class CoolRepository:
	"A class to handle the cool preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Cooled', self, '' )
		self.activateCool = preferences.BooleanPreference().getFromValue( 'Activate Cool', self, True )
		self.maximumCool = preferences.FloatPreference().getFromValue( 'Maximum Cool (Celcius):', self, 2.0 )
		self.minimumLayerTime = preferences.FloatPreference().getFromValue( 'Minimum Layer Time (seconds):', self, 60.0 )
		self.minimumOrbitalRadius = preferences.FloatPreference().getFromValue( 'Minimum Orbital Radius (millimeters):', self, 10.0 )
		self.turnFanOnAtBeginning = preferences.BooleanPreference().getFromValue( 'Turn Fan On at Beginning', self, True )
		self.turnFanOffAtEnding = preferences.BooleanPreference().getFromValue( 'Turn Fan Off at Ending', self, True )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Cool'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.cool.html' )

	def execute( self ):
		"Cool button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class CoolSkein:
	"A class to cool a skein of extrusions."
	def __init__( self ):
		self.boundaryLayer = None
		self.coolTemperature = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRateMinute = 960.0
		self.highestZ = - 99999999.9
		self.layerTime = 0.0
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.oldTemperature = None

	def addCoolOrbits( self, remainingOrbitTime ):
		"Add the minimum radius cool orbits."
		if len( self.boundaryLayer.loops ) < 1:
			return
		insetBoundaryLoops = intercircle.getInsetLoopsFromLoops( self.perimeterWidth, self.boundaryLayer.loops )
		if len( insetBoundaryLoops ) < 1:
			insetBoundaryLoops = self.boundaryLayer.loops
		largestLoop = euclidean.getLargestLoop( insetBoundaryLoops )
		loopArea = abs( euclidean.getPolygonArea( largestLoop ) )
		if loopArea < self.minimumArea:
			center = 0.5 * ( euclidean.getMaximumFromPoints( largestLoop ) + euclidean.getMinimumFromPoints( largestLoop ) )
			centerXBounded = max( center.real, self.cornerMinimum.real )
			centerXBounded = min( centerXBounded, self.cornerMaximum.real )
			centerYBounded = max( center.imag, self.cornerMinimum.imag )
			centerYBounded = min( centerYBounded, self.cornerMaximum.imag )
			center = complex( centerXBounded, centerYBounded )
			maximumCorner = center + self.halfCorner
			minimumCorner = center - self.halfCorner
			largestLoop = euclidean.getSquareLoop( minimumCorner, maximumCorner )
		pointComplex = euclidean.getXYComplexFromVector3( self.oldLocation )
		if pointComplex != None:
			largestLoop = euclidean.getLoopStartingNearest( self.perimeterWidth, pointComplex, largestLoop )
		intercircle.addOrbitsIfLarge( self.distanceFeedRate, largestLoop, self.orbitalFeedRatePerSecond, remainingOrbitTime, self.highestZ )

	def addGcodeFromFeedRateMovementZ( self, feedRateMinute, point, z ):
		"Add a movement to the output."
		self.distanceFeedRate.addLine( self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( feedRateMinute, point, z ) )

	def addTemperature( self, temperature ):
		"Add a line of temperature."
		self.distanceFeedRate.addLine( 'M104 S' + euclidean.getRoundedToThreePlaces( temperature ) ) 

	def getCraftedGcode( self, gcodeText, coolRepository ):
		"Parse gcode text and store the cool gcode."
		self.coolRepository = coolRepository
		self.coolEndText = preferences.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Cool_End.gcode' )
		self.coolEndLines = gcodec.getTextLines( self.coolEndText )
		self.coolStartText = preferences.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Cool_Start.gcode' )
		self.coolStartLines = gcodec.getTextLines( self.coolStartText )
		self.cornerMaximum = complex( - 999999999.0, - 999999999.0 )
		self.cornerMinimum = complex( 999999999.0, 999999999.0 )
		self.halfCorner = complex( coolRepository.minimumOrbitalRadius.value, coolRepository.minimumOrbitalRadius.value )
		self.lines = gcodec.getTextLines( gcodeText )
		self.minimumArea = 4.0 * coolRepository.minimumOrbitalRadius.value * coolRepository.minimumOrbitalRadius.value
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ lineIndex ]
			self.parseCorner( line )
		margin = 0.2 * self.perimeterWidth
		halfCornerMargin = self.halfCorner + complex( margin, margin )
		self.cornerMaximum -= halfCornerMargin
		self.cornerMinimum += halfCornerMargin
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )
		if coolRepository.turnFanOffAtEnding.value:
			self.distanceFeedRate.addLine( 'M107' )
		return self.distanceFeedRate.output.getvalue()

	def linearMove( self, splitLine ):
		"Add line to time spent on layer."
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		if self.oldLocation != None:
			feedRateSecond = self.feedRateMinute / 60.0
			self.layerTime += location.distance( self.oldLocation ) / feedRateSecond
		self.highestZ = max( location.z, self.highestZ )
		self.oldLocation = location

	def parseCorner( self, line ):
		"Parse a gcode line and use the location to update the bounding corners."
		splitLine = line.split()
		firstWord = gcodec.getFirstWord( splitLine )
		if firstWord == '(<boundaryPoint>':
			locationComplex = gcodec.getLocationFromSplitLine( None, splitLine ).dropAxis( 2 )
			self.cornerMaximum = euclidean.getMaximum( self.cornerMaximum, locationComplex )
			self.cornerMinimum = euclidean.getMinimum( self.cornerMinimum, locationComplex )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
				if self.coolRepository.turnFanOnAtBeginning.value:
					self.distanceFeedRate.addLine( 'M106' )
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> cool </procedureDone>)' )
				return
			elif firstWord == '(<orbitalFeedRatePerSecond>':
				self.orbitalFeedRatePerSecond = float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the cool skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == '(<boundaryPoint>':
			self.boundaryLoop.append( gcodec.getLocationFromSplitLine( None, splitLine ).dropAxis( 2 ) )
		elif firstWord == '(<layer>':
			self.distanceFeedRate.addLine( line )
			self.distanceFeedRate.addLinesSetAbsoluteDistanceMode( self.coolStartLines )
			remainingOrbitTime = self.coolRepository.minimumLayerTime.value - self.layerTime
			if remainingOrbitTime > 0.0 and self.boundaryLayer != None:
				layerCool = self.coolRepository.maximumCool.value * remainingOrbitTime / self.coolRepository.minimumLayerTime.value
				if self.oldTemperature != None and layerCool != 0.0:
					self.coolTemperature = self.oldTemperature - layerCool
					self.addTemperature( self.coolTemperature )
				self.addCoolOrbits( remainingOrbitTime )
			z = float( splitLine[ 1 ] )
			self.boundaryLayer = euclidean.LoopLayer( z )
			self.highestZ = z
			self.layerTime = 0.0
			self.distanceFeedRate.addLinesSetAbsoluteDistanceMode( self.coolEndLines )
			return
		elif firstWord == '(</layer>)':
			if self.coolTemperature != None:
				self.addTemperature( self.oldTemperature )
				self.coolTemperature = None
		elif firstWord == 'M104':
			self.oldTemperature = gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] )
		elif firstWord == '(<surroundingLoop>)':
			self.boundaryLoop = []
			self.boundaryLayer.loops.append( self.boundaryLoop )
		self.distanceFeedRate.addLine( line )


def main():
	"Display the cool dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
