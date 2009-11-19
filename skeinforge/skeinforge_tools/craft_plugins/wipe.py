"""
Wipe is a script to wipe the nozzle.

At the beginning of a layer, depending on the preferences, wipe will move the nozzle with the extruder off to the arrival point, then to the wipe point, then to the departure point, then back to the layer.

The default 'Activate Wipe' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The "Location Arrival X" preference, is the x coordinate of the arrival location.  The "Location Arrival Y" and "Location Arrival Z" preferences are the y & z coordinates of the location.  The equivalent "Location Wipe.." and "Location Departure.." preferences are for the wipe and departure locations.

The "Wipe Period (layers)" preference is the number of layers between wipes.  Wipe will always wipe just before the first layer, afterwards it will wipe every "Wipe Period" layers.  With the default of three, wipe will wipe just before the zeroth layer, the third layer, sixth layer and so on.

The following examples wipe the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and wipe.py.


> python wipe.py
This brings up the wipe dialog.


> python wipe.py Screw Holder Bottom.stl
The wipe tool is parsing the file:
Screw Holder Bottom.stl
..
The wipe tool has created the file:
.. Screw Holder Bottom_wipe.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import wipe
>>> wipe.main()
This brings up the wipe dialog.


>>> wipe.writeOutput()
The wipe tool is parsing the file:
Screw Holder Bottom.stl
..
The wipe tool has created the file:
.. Screw Holder Bottom_wipe.gcode

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


def getCraftedText( fileName, text, wipeRepository = None ):
	"Wipe a gcode linear move text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), wipeRepository )

def getCraftedTextFromText( gcodeText, wipeRepository = None ):
	"Wipe a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'wipe' ):
		return gcodeText
	if wipeRepository == None:
		wipeRepository = preferences.getReadRepository( WipeRepository() )
	if not wipeRepository.activateWipe.value:
		return gcodeText
	return WipeSkein().getCraftedGcode( gcodeText, wipeRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return WipeRepository()

def writeOutput( fileName = '' ):
	"Wipe a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'wipe' )


class WipeRepository:
	"A class to handle the wipe preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Wiped', self, '' )
		self.activateWipe = preferences.BooleanPreference().getFromValue( 'Activate Wipe', self, False )
		self.locationArrivalX = preferences.FloatPreference().getFromValue( 'Location Arrival X (mm):', self, - 70.0 )
		self.locationArrivalY = preferences.FloatPreference().getFromValue( 'Location Arrival Y (mm):', self, - 50.0 )
		self.locationArrivalZ = preferences.FloatPreference().getFromValue( 'Location Arrival Z (mm):', self, 50.0 )
		self.locationDepartureX = preferences.FloatPreference().getFromValue( 'Location Departure X (mm):', self, - 70.0 )
		self.locationDepartureY = preferences.FloatPreference().getFromValue( 'Location Departure Y (mm):', self, - 40.0 )
		self.locationDepartureZ = preferences.FloatPreference().getFromValue( 'Location Departure Z (mm):', self, 50.0 )
		self.locationWipeX = preferences.FloatPreference().getFromValue( 'Location Wipe X (mm):', self, - 70.0 )
		self.locationWipeY = preferences.FloatPreference().getFromValue( 'Location Wipe Y (mm):', self, - 70.0 )
		self.locationWipeZ = preferences.FloatPreference().getFromValue( 'Location Wipe Z (mm):', self, 50.0 )
		self.wipePeriod = preferences.IntPreference().getFromValue( 'Wipe Period (layers):', self, 3 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Wipe'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.wipe.html' )

	def execute( self ):
		"Wipe button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class WipeSkein:
	"A class to wipe a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.highestZ = None
		self.layerIndex = - 1
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.shouldWipe = False
		self.travelFeedRatePerMinute = 957.0

	def addHop( self, begin, end ):
		"Add hop to highest point."
		beginEndDistance = begin.distance( end )
		if beginEndDistance < 3.0 * self.absolutePerimeterWidth:
			return
		alongWay = self.absolutePerimeterWidth / beginEndDistance
		closeToOldLocation = euclidean.getIntermediateLocation( alongWay, begin, end )
		closeToOldLocation.z = self.highestZ
		self.distanceFeedRate.addLine( self.getLinearMoveWithFeedRate( self.travelFeedRatePerMinute, closeToOldLocation ) )
		closeToOldArrival = euclidean.getIntermediateLocation( alongWay, end, begin )
		closeToOldArrival.z = self.highestZ
		self.distanceFeedRate.addLine( self.getLinearMoveWithFeedRate( self.travelFeedRatePerMinute, closeToOldArrival ) )

	def addWipeTravel( self, splitLine ):
		"Add the wipe travel gcode."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.highestZ = max( self.highestZ, location.z )
		if not self.shouldWipe:
			return
		self.shouldWipe = False
		if self.extruderActive:
			self.distanceFeedRate.addLine( 'M103' )
		if self.oldLocation != None:
			self.addHop( self.oldLocation, self.locationArrival )
		self.distanceFeedRate.addLine( self.getLinearMoveWithFeedRate( self.travelFeedRatePerMinute, self.locationArrival ) )
		self.distanceFeedRate.addLine( self.getLinearMoveWithFeedRate( self.travelFeedRatePerMinute, self.locationWipe ) )
		self.distanceFeedRate.addLine( self.getLinearMoveWithFeedRate( self.travelFeedRatePerMinute, self.locationDeparture ) )
		self.addHop( self.locationDeparture, location )
		if self.extruderActive:
			self.distanceFeedRate.addLine( 'M101' )

	def getCraftedGcode( self, gcodeText, wipeRepository ):
		"Parse gcode text and store the wipe gcode."
		self.lines = gcodec.getTextLines( gcodeText )
		self.wipePeriod = wipeRepository.wipePeriod.value
		self.parseInitialization( wipeRepository )
		self.locationArrival = Vector3( wipeRepository.locationArrivalX.value, wipeRepository.locationArrivalY.value, wipeRepository.locationArrivalZ.value )
		self.locationDeparture = Vector3( wipeRepository.locationDepartureX.value, wipeRepository.locationDepartureY.value, wipeRepository.locationDepartureZ.value )
		self.locationWipe = Vector3( wipeRepository.locationWipeX.value, wipeRepository.locationWipeY.value, wipeRepository.locationWipeZ.value )
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getLinearMoveWithFeedRate( self, feedRate, location ):
		"Get a linear move line with the feedRate."
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( feedRate, location.dropAxis( 2 ), location.z )

	def parseInitialization( self, wipeRepository ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> wipe </procedureDone>)' )
				return
			elif firstWord == '(<perimeterWidth>':
				self.absolutePerimeterWidth = abs( float( splitLine[ 1 ] ) )
			elif firstWord == '(<travelFeedRatePerSecond>':
				self.travelFeedRatePerMinute = 60.0 * float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the bevel gcode."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.addWipeTravel( splitLine )
			self.oldLocation = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		elif firstWord == '(<layer>':
			self.layerIndex += 1
			if self.layerIndex % self.wipePeriod == 0:
				self.shouldWipe = True
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
		self.distanceFeedRate.addLine( line )


def main():
	"Display the wipe dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
