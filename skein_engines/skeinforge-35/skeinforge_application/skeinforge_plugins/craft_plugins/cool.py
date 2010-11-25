"""
This page is in the table of contents.
Cool is a script to cool the shape.

The cool manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Cool

Allan Ecker aka The Masked Retriever's has written the "Skeinforge Quicktip: Cool" at:
http://blog.thingiverse.com/2009/07/28/skeinforge-quicktip-cool/

==Operation==
The default 'Activate Cool' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
===Cool Type===
Default is 'Orbit', because many extruders do not operate properly at very slow flow rates.

====Orbit====
When selected, cool will add orbits with the extruder off to give the layer time to cool, so that the next layer is not extruded on a molten base.  The orbits will be around the largest island on that layer.

====Slow Down====
When selected, cool will slow down the extruder so that it will take the minimum layer time to extrude the layer.

===Maximum Cool===
Default is 2 Celcius.

If it takes less time to extrude the layer than the minimum layer time, then cool will lower the temperature by the 'Maximum Cool' setting times the layer time over the minimum layer time.

===Minimum Layer Time===
Default is 60 seconds.

Defines the minimum amount of time the extruder will spend on a layer, this is an important setting.

===Minimum Orbital Radius===
Default is 10 millimeters.

When the orbit cool type is selected, if the area of the largest island is as large as the square of the "Minimum Orbital Radius" then the orbits will be just within the island.  If the island is smaller, then the orbits will be in a square of the "Minimum Orbital Radius" around the center of the island.

===Turn Fan On at Beginning===
Default is on.

When selected, cool will turn the fan on at the beginning of the fabrication.

===Turn Fan On at Ending===
Default is on.

When selected, cool will turn the fan off at the ending of the fabrication.

==Alterations==
Cool looks for alteration files in the alterations folder in the .skeinforge folder in the home directory.  Cool does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_plugins folder. If it doesn't find anything there it looks in the craft_plugins folder.  The cool start and end text idea is from:
http://makerhahn.blogspot.com/2008/10/yay-minimug.html

===cool_start.gcode===
Cool will add cool_start.gcode to the start of the orbits if it exists.

===cool_end.gcode===
After it has added the orbits, it will add the file cool_end.gcode if it exists.

==Examples==
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


>>> cool.writeOutput('Screw Holder Bottom.stl')
The cool tool is parsing the file:
Screw Holder Bottom.stl
..
The cool tool has created the file:
.. Screw Holder Bottom_cool.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import os
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getCraftedText( fileName, text, coolRepository = None ):
	"Cool a gcode linear move text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), coolRepository )

def getCraftedTextFromText( gcodeText, coolRepository = None ):
	"Cool a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'cool'):
		return gcodeText
	if coolRepository == None:
		coolRepository = settings.getReadRepository( CoolRepository() )
	if not coolRepository.activateCool.value:
		return gcodeText
	return CoolSkein().getCraftedGcode( gcodeText, coolRepository )

def getNewRepository():
	"Get the repository constructor."
	return CoolRepository()

def writeOutput(fileName=''):
	"Cool a gcode linear move file.  Chain cool the gcode if it is not already cooled. If no fileName is specified, cool the first unmodified gcode file in this folder."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage( fileName, 'cool')


class CoolRepository:
	"A class to handle the cool settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.cool.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Cool', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Cool')
		self.activateCool = settings.BooleanSetting().getFromValue('Activate Cool', self, True )
		self.coolType = settings.MenuButtonDisplay().getFromName('Cool Type:', self )
		self.orbit = settings.MenuRadio().getFromMenuButtonDisplay( self.coolType, 'Orbit', self, True )
		self.slowDown = settings.MenuRadio().getFromMenuButtonDisplay( self.coolType, 'Slow Down', self, False )
		self.maximumCool = settings.FloatSpin().getFromValue( 0.0, 'Maximum Cool (Celcius):', self, 10.0, 2.0 )
		self.minimumLayerTime = settings.FloatSpin().getFromValue( 0.0, 'Minimum Layer Time (seconds):', self, 120.0, 60.0 )
		self.minimumOrbitalRadius = settings.FloatSpin().getFromValue( 0.0, 'Minimum Orbital Radius (millimeters):', self, 20.0, 10.0 )
		self.turnFanOnAtBeginning = settings.BooleanSetting().getFromValue('Turn Fan On at Beginning', self, True )
		self.turnFanOffAtEnding = settings.BooleanSetting().getFromValue('Turn Fan Off at Ending', self, True )
		self.executeTitle = 'Cool'

	def execute(self):
		"Cool button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class CoolSkein:
	"A class to cool a skein of extrusions."
	def __init__(self):
		self.boundaryLayer = None
		self.coolTemperature = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRateMinute = 960.0
		self.highestZ = 1.0
		self.layerCount = settings.LayerCount()
		self.lineIndex = 0
		self.lines = None
		self.multiplier = 1.0
		self.oldFlowRate = None
		self.oldFlowRateString = None
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
		loopArea = abs( euclidean.getAreaLoop( largestLoop ) )
		if loopArea < self.minimumArea:
			center = 0.5 * ( euclidean.getMaximumByPathComplex( largestLoop ) + euclidean.getMinimumByPathComplex( largestLoop ) )
			centerXBounded = max( center.real, self.boundingRectangle.cornerMinimum.real )
			centerXBounded = min( centerXBounded, self.boundingRectangle.cornerMaximum.real )
			centerYBounded = max( center.imag, self.boundingRectangle.cornerMinimum.imag )
			centerYBounded = min( centerYBounded, self.boundingRectangle.cornerMaximum.imag )
			center = complex( centerXBounded, centerYBounded )
			maximumCorner = center + self.halfCorner
			minimumCorner = center - self.halfCorner
			largestLoop = euclidean.getSquareLoopWiddershins( minimumCorner, maximumCorner )
		pointComplex = euclidean.getXYComplexFromVector3( self.oldLocation )
		if pointComplex != None:
			largestLoop = euclidean.getLoopStartingNearest( self.perimeterWidth, pointComplex, largestLoop )
		intercircle.addOrbitsIfLarge( self.distanceFeedRate, largestLoop, self.orbitalFeedRatePerSecond, remainingOrbitTime, self.highestZ )

	def addCoolTemperature( self, remainingOrbitTime ):
		"Parse a gcode line and add it to the cool skein."
		layerCool = self.coolRepository.maximumCool.value * remainingOrbitTime / self.coolRepository.minimumLayerTime.value
		if self.oldTemperature != None and layerCool != 0.0:
			self.coolTemperature = self.oldTemperature - layerCool
			self.addTemperature( self.coolTemperature )

	def addFlowRateLineIfNecessary( self, flowRate ):
		"Add a line of flow rate if different."
		flowRateString = euclidean.getFourSignificantFigures( flowRate )
		if flowRateString == self.oldFlowRateString:
			return
		if flowRateString != None:
			self.distanceFeedRate.addLine('M108 S' + flowRateString )
		self.oldFlowRateString = flowRateString

	def addFlowRateMultipliedLineIfNecessary( self, flowRate ):
		"Add a multipled line of flow rate if different."
		if flowRate != None:
			self.addFlowRateLineIfNecessary( self.multiplier * flowRate )

	def addGcodeFromFeedRateMovementZ(self, feedRateMinute, point, z):
		"Add a movement to the output."
		self.distanceFeedRate.addLine( self.distanceFeedRate.getLinearGcodeMovementWithFeedRate(feedRateMinute, point, z) )

	def addOrbitsIfNecessary( self, remainingOrbitTime ):
		"Parse a gcode line and add it to the cool skein."
		if remainingOrbitTime > 0.0 and self.boundaryLayer != None:
			self.addCoolOrbits( remainingOrbitTime )

	def addTemperature( self, temperature ):
		"Add a line of temperature."
		self.distanceFeedRate.addLine('M104 S' + euclidean.getRoundedToThreePlaces( temperature ) ) 

	def getCoolMove(self, line, location, splitLine):
		"Add line to time spent on layer."
		self.feedRateMinute = gcodec.getFeedRateMinute(self.feedRateMinute, splitLine)
		self.highestZ = max(location.z, self.highestZ)
		self.addFlowRateMultipliedLineIfNecessary(self.oldFlowRate)
		return self.distanceFeedRate.getLineWithFeedRate(self.multiplier * self.feedRateMinute, line, splitLine)

	def getCraftedGcode( self, gcodeText, coolRepository ):
		"Parse gcode text and store the cool gcode."
		self.coolRepository = coolRepository
		self.coolEndText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Cool_End.gcode')
		self.coolEndLines = archive.getTextLines( self.coolEndText )
		self.coolStartText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Cool_Start.gcode')
		self.coolStartLines = archive.getTextLines( self.coolStartText )
		self.halfCorner = complex( coolRepository.minimumOrbitalRadius.value, coolRepository.minimumOrbitalRadius.value )
		self.lines = archive.getTextLines(gcodeText)
		self.minimumArea = 4.0 * coolRepository.minimumOrbitalRadius.value * coolRepository.minimumOrbitalRadius.value
		self.parseInitialization()
		self.boundingRectangle = gcodec.BoundingRectangle().getFromGcodeLines( self.lines[self.lineIndex :], 0.5 * self.perimeterWidth )
		margin = 0.2 * self.perimeterWidth
		halfCornerMargin = self.halfCorner + complex( margin, margin )
		self.boundingRectangle.cornerMaximum -= halfCornerMargin
		self.boundingRectangle.cornerMinimum += halfCornerMargin
		for self.lineIndex in xrange( self.lineIndex, len(self.lines) ):
			line = self.lines[self.lineIndex]
			self.parseLine(line)
		if coolRepository.turnFanOffAtEnding.value:
			self.distanceFeedRate.addLine('M107')
		return self.distanceFeedRate.output.getvalue()

	def getLayerTime(self):
		"Get the time the extruder spends on the layer."
		feedRateMinute = self.feedRateMinute
		layerTime = 0.0
		lastThreadLocation = self.oldLocation
		for lineIndex in xrange( self.lineIndex, len(self.lines) ):
			line = self.lines[lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine( lastThreadLocation, splitLine )
				feedRateMinute = gcodec.getFeedRateMinute( feedRateMinute, splitLine )
				if lastThreadLocation != None:
					feedRateSecond = feedRateMinute / 60.0
					layerTime += location.distance( lastThreadLocation ) / feedRateSecond
				lastThreadLocation = location
			elif firstWord == '(</layer>)':
				return layerTime
		return layerTime

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == 'M108':
				self.setOperatingFlowString(splitLine)
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float(splitLine[1])
				if self.coolRepository.turnFanOnAtBeginning.value:
					self.distanceFeedRate.addLine('M106')
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine('(<procedureDone> cool </procedureDone>)')
				return
			elif firstWord == '(<orbitalFeedRatePerSecond>':
				self.orbitalFeedRatePerSecond = float(splitLine[1])
			self.distanceFeedRate.addLine(line)

	def parseLine(self, line):
		"Parse a gcode line and add it to the cool skein."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
			line = self.getCoolMove(line, location, splitLine)
			self.oldLocation = location
		elif firstWord == '(<boundaryPoint>':
			self.boundaryLoop.append(gcodec.getLocationFromSplitLine(None, splitLine).dropAxis(2))
		elif firstWord == '(<layer>':
			self.layerCount.printProgressIncrement('cool')
			self.distanceFeedRate.addLine(line)
			self.distanceFeedRate.addLinesSetAbsoluteDistanceMode(self.coolStartLines)
			layerTime = self.getLayerTime()
			remainingOrbitTime = max(self.coolRepository.minimumLayerTime.value - layerTime, 0.0)
			self.addCoolTemperature(remainingOrbitTime)
			if self.coolRepository.orbit.value:
				self.addOrbitsIfNecessary(remainingOrbitTime)
			else:
				self.setMultiplier(layerTime)
			z = float(splitLine[1])
			self.boundaryLayer = euclidean.LoopLayer(z)
			self.highestZ = max(z, self.highestZ)
			self.distanceFeedRate.addLinesSetAbsoluteDistanceMode(self.coolEndLines)
			return
		elif firstWord == '(</layer>)':
			self.multiplier = 1.0
			if self.coolTemperature != None:
				self.addTemperature(self.oldTemperature)
				self.coolTemperature = None
			self.addFlowRateLineIfNecessary(self.oldFlowRate)
		elif firstWord == 'M104':
			self.oldTemperature = gcodec.getDoubleAfterFirstLetter(splitLine[1])
		elif firstWord == 'M108':
			self.setOperatingFlowString(splitLine)
			self.addFlowRateMultipliedLineIfNecessary(self.oldFlowRate)
			return
		elif firstWord == '(<surroundingLoop>)':
			self.boundaryLoop = []
			self.boundaryLayer.loops.append(self.boundaryLoop)
		self.distanceFeedRate.addLine(line)

	def setMultiplier( self, layerTime ):
		"Set the feed and flow rate multiplier."
		self.multiplier = min( 1.0, layerTime / self.coolRepository.minimumLayerTime.value )

	def setOperatingFlowString( self, splitLine ):
		"Set the operating flow string from the split line."
		self.oldFlowRate = float( splitLine[1][1 :] )


def main():
	"Display the cool dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
