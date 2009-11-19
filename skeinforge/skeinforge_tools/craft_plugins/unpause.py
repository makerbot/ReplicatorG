"""
Unpause is a script to speed up a line segment to compensate for the delay of the microprocessor.

The default 'Activate Unpause' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The unpause script is based on the Shane Hathaway's patch to speed up a line segment to compensate for the delay of the microprocessor.  The description is at:
http://shane.willowrise.com/archives/delay-compensation-in-firmware/

The "Delay (milliseconds)" preference is the delay on the microprocessor that will be at least partially compensated for.  The default is 28 milliseconds, which Shane found for the Arduino.  The "Maximum Speed" ratio is the maximum amount that the feed rate will be sped up to, compared to the original feed rate, the default is 1.5.

The equation to set the feed rate is from Shane Hathaway's description at:
http://shane.willowrise.com/archives/delay-compensation-in-firmware/

The following examples unpause the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and unpause.py.


> python unpause.py
This brings up the unpause dialog.


> python unpause.py Screw Holder Bottom.stl
The unpause tool is parsing the file:
Screw Holder Bottom.stl
..
The unpause tool has created the file:
.. Screw Holder Bottom_unpause.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import unpause
>>> unpause.main()
This brings up the unpause dialog.


>>> unpause.writeOutput()
The unpause tool is parsing the file:
Screw Holder Bottom.stl
..
The unpause tool has created the file:
.. Screw Holder Bottom_unpause.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.meta_plugins import polyfile
from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text, unpauseRepository = None ):
	"Unpause a gcode linear move file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), unpauseRepository )

def getCraftedTextFromText( gcodeText, unpauseRepository = None ):
	"Unpause a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'unpause' ):
		return gcodeText
	if unpauseRepository == None:
		unpauseRepository = preferences.getReadRepository( UnpauseRepository() )
	if not unpauseRepository.activateUnpause.value:
		return gcodeText
	return UnpauseSkein().getCraftedGcode( unpauseRepository, gcodeText )

def getRepositoryConstructor():
	"Get the repository constructor."
	return UnpauseRepository()

def getSelectedPlugin( unpauseRepository ):
	"Get the selected plugin."
	for plugin in unpauseRepository.unpausePlugins:
		if plugin.value:
			return plugin
	return None

def writeOutput( fileName = '' ):
	"Unpause a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'unpause' )


class UnpauseRepository:
	"A class to handle the unpause preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Unpaused', self, '' )
		self.activateUnpause = preferences.BooleanPreference().getFromValue( 'Activate Unpause', self, False )
		self.delay = preferences.FloatPreference().getFromValue( 'Delay (milliseconds):', self, 28.0 )
		self.maximumSpeed = preferences.FloatPreference().getFromValue( 'Maximum Speed (ratio):', self, 1.5 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Unpause'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.unpause.html' )

	def execute( self ):
		"Unpause button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class UnpauseSkein:
	"A class to unpause a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.feedRateMinute = 959.0
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None

	def getCraftedGcode( self, unpauseRepository, gcodeText ):
		"Parse gcode text and store the unpause gcode."
		self.delaySecond = unpauseRepository.delay.value * 0.001
		self.maximumSpeed = unpauseRepository.maximumSpeed.value
		self.minimumSpeedUpReciprocal = 1.0 / self.maximumSpeed
		self.unpauseRepository = unpauseRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getUnpausedFeedRateMinute( self, location, splitLine ):
		"Get the feed rate which will compensate for the pause."
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		if self.oldLocation == None:
			return self.feedRateMinute
		distance = location.distance( self.oldLocation )
		if distance <= 0.0:
			return self.feedRateMinute
		specifiedFeedRateSecond = self.feedRateMinute / 60.0
		resultantReciprocal = 1.0 - self.delaySecond / distance * specifiedFeedRateSecond
		if resultantReciprocal < self.minimumSpeedUpReciprocal:
			return self.feedRateMinute * self.maximumSpeed
		return self.feedRateMinute / resultantReciprocal

	def getUnpausedArcMovement( self, line, splitLine ):
		"Get an unpaused arc movement."
		if self.oldLocation == None:
			return line
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		relativeLocation = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		location = self.oldLocation + relativeLocation
		self.oldLocation = location
		halfPlaneLineDistance = 0.5 * abs( relativeLocation.dropAxis( 2 ) )
		radius = gcodec.getDoubleFromCharacterSplitLine( 'R', splitLine )
		if radius == None:
			relativeCenter = complex( gcodec.getDoubleFromCharacterSplitLine( 'I', splitLine ), gcodec.getDoubleFromCharacterSplitLine( 'J', splitLine ) )
			radius = abs( relativeCenter )
		angle = 0.0
		if radius > 0.0:
			angle = math.pi
			if halfPlaneLineDistance < radius:
				angle = 2.0 * math.asin( halfPlaneLineDistance / radius )
			else:
				angle *= halfPlaneLineDistance / radius
		deltaZ = abs( relativeLocation.z )
		arcDistanceZ = complex( abs( angle ) * radius, relativeLocation.z )
		distance = abs( arcDistanceZ )
		if distance <= 0.0:
			return ''
		feedRateMinute = self.distanceFeedRate.getZLimitedFeedRate( deltaZ, distance, self.feedRateMinute )
		indexOfF = gcodec.indexOfStartingWithSecond( "F", splitLine )
		if indexOfF > 0 and feedRateMinute != self.feedRateMinute:
			feedRateStringOriginal = splitLine[ indexOfF ]
			feedRateStringReplacement = 'F' + self.distanceFeedRate.getRounded( feedRateMinute )
			return line.replace( feedRateStringOriginal, feedRateStringReplacement )
		return line

	def getUnpausedLinearMovement( self, splitLine ):
		"Get an unpaused linear movement."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		unpausedFeedRateMinute = self.getUnpausedFeedRateMinute( location, splitLine )
		self.oldLocation = location
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( unpausedFeedRateMinute, location.dropAxis( 2 ), location.z )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> unpause </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			line = self.getUnpausedLinearMovement( splitLine )
		if firstWord == 'G2' or firstWord == 'G3':
			line = self.getUnpausedArcMovement( line, splitLine )
		self.distanceFeedRate.addLine( line )


def main():
	"Display the unpause dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
