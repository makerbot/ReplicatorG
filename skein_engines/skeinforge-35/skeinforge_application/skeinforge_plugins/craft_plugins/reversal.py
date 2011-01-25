from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import math
import sys


__author__ = 'Marius Kintel (marius@kintel.net)'
__date__ = '$Date: 2011/01/20 $'
__license__ = 'GPL 3.0'


def getCraftedText(fileName, text, reversalRepository = None):
	"Reversal a gcode linear move file or text."
	return getCraftedTextFromText(archive.getTextIfEmpty(fileName, text), reversalRepository)

def getCraftedTextFromText(gcodeText, reversalRepository = None):
	"Reversal a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty(gcodeText, 'reversal'):
		return gcodeText
	if reversalRepository == None:
		reversalRepository = settings.getReadRepository(ReversalRepository())
	if not reversalRepository.activateReversal.value:
		return gcodeText
	return ReversalSkein().getCraftedGcode(gcodeText, reversalRepository)

def getNewRepository():
	"Get the repository constructor."
	return ReversalRepository()

def writeOutput(fileName=''):
	"Reversal a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage(fileName, 'reversal')


class ReversalRepository:
	"A class to handle the reversal settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.reversal.html', self)
		self.fileNameInput = settings.FileNameInput().getFromFileName(fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Reversal', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Reversal')
		self.activateReversal = settings.BooleanSetting().getFromValue('Activate Reversal', self, False)
		self.reversalRPM = settings.FloatSpin().getFromValue(2, 'Reversal speed (RPM):', self, 50, 35)
		self.reversalTime = settings.FloatSpin().getFromValue(10, 'Reversal time (milliseconds):', self, 1000, 75)
		self.pushbackTime = settings.FloatSpin().getFromValue(10, 'Push-back time (milliseconds):', self, 1000, 75)
		self.reversalThreshold = settings.FloatSpin().getFromValue(0.1, 'Reversal threshold (mm):', self, 5.0, 1.0)
		self.activateEarlyReversal = settings.BooleanSetting().getFromValue('Activate early reversal and push-back', self, True)
		self.executeTitle = 'Reversal'

	def execute(self):
		"Reversal button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class ReversalSkein:
	"A class to reversal a skein of extrusions."
	def __init__(self):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.flowrate = 1.99
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.newLocation = None
                self.reversalActive = False
                self.reversalWorthy = False
                self.feedRateMinute = 0
                self.extruderOn = False
                self.didReverse = False

	def getCraftedGcode(self, gcodeText, reversalRepository):
		"Parse gcode text and store the reversal gcode."
		self.lines = archive.getTextLines(gcodeText)
		self.reversalRepository = reversalRepository
		self.parseInitialization(reversalRepository)
		for self.lineIndex in xrange(self.lineIndex, len(self.lines)):
			line = self.lines[self.lineIndex]
			self.parseLine(line)
		return self.distanceFeedRate.output.getvalue()

	def getTimeToCommand(self, command, threshold):
		"""Get the time in milliseconds to the next command matching 
                the given command string.
                Returns None if the time is larger than the given threshold."""
		line = self.lines[self.lineIndex]
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		lastThreadLocation = gcodec.getLocationFromSplitLine(self.newLocation, splitLine)
		totalTime = 0.0
		for afterIndex in xrange(self.lineIndex + 1, len(self.lines)):
			line = self.lines[afterIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine(lastThreadLocation, splitLine)
				totalTime += location.distance(lastThreadLocation) / self.feedRateMinute * 60000
				lastThreadLocation = location
				if totalTime >= threshold:
					return None
			elif firstWord == command:
				return totalTime
		return None

	def isNextStopReversalWorthy(self, threshold):
		"""Returns True if we should reverse on next stop, False otherwise. 
                The given threshold defines the off distance under which we don't reverse."""
		extruderOnReached = False
		line = self.lines[self.lineIndex]
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		lastThreadLocation = gcodec.getLocationFromSplitLine(self.newLocation, splitLine)
		threadEndReached = False
		totalDistance = 0.0
		for afterIndex in xrange(self.lineIndex + 1, len(self.lines)):
			line = self.lines[afterIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine(lastThreadLocation, splitLine)
				if threadEndReached:
					totalDistance += location.distance(lastThreadLocation)
					if totalDistance >= threshold:
						return True
				lastThreadLocation = location
			elif firstWord == 'M101':
				extruderOnReached = True
                                if threadEndReached: return False
			elif firstWord == 'M103':
				threadEndReached = True
		return False

	def parseInitialization(self, reversalRepository):
		'Parse gcode initialization and store the parameters.'
		self.reversalRPM = self.reversalRepository.reversalRPM.value
		self.reversalTime = self.reversalRepository.reversalTime.value
		self.pushbackTime = self.reversalRepository.pushbackTime.value
		self.reversalThreshold = self.reversalRepository.reversalThreshold.value
                self.activateEarlyReversal = self.reversalRepository.activateEarlyReversal.value

		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine('(<procedureDone> reversal </procedureDone>)')
				return
			elif firstWord == '(<operatingFlowRate>':
				self.flowrate = float(splitLine[1])
			self.distanceFeedRate.addLine(line)

	def getLinearMoveWithFeedRate(self, feedRate, location):
		"Get a linear move line with the feed rate."
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate(feedRate, location.dropAxis(2), location.z)

        def detectAndMoveToReversalEvent(self, command, thresholdTime):
                """Checks if we've reached the threshold where a reversal event would occur.
                Moves us to the exact point where reversal or pushback should happen.
                Returns True if reversal/pushback should be initiated, False to ignore"""

                # Time to split off at the end of this movement
                # The split off part of the movement, as
                # well as any other movement between here
                # and the end of the thread will incorporate
                # a reversal
                timeTo = self.getTimeToCommand(command, thresholdTime)
                if timeTo != None:
                        segment = self.newLocation - self.oldLocation
                        segmentLength = segment.magnitude()
                        segmentTime = segment.magnitude() / self.feedRateMinute * 60000
#                       # If this is the only segment left
#                       if timeToOn == 0.0:
#                               print "Case B: timeToOn=" + str(timeToOn) + ", segmentTime=" + str(segmentTime)
#                       # We have more segments
#                       else:
#                               print "Case C: timeToOn=" + str(timeToOn) + ", segmentTime=" + str(segmentTime)
                        splitTime = thresholdTime - timeTo
                        reversalPosition = self.oldLocation + segment * (segmentTime - splitTime) / segmentTime
#                       print "  oldPos: " + str(self.newLocation)
#                       print "  newPos: " + str(reversalPosition)
                        self.distanceFeedRate.addLine(self.getLinearMoveWithFeedRate(self.feedRateMinute, reversalPosition))
                        return True
                return False

        # This is the main loop
	def parseLine(self, line):
		"Parse a gcode line and add it to the bevel gcode."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			self.oldLocation = self.newLocation
			self.newLocation = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
                        self.feedRateMinute = gcodec.getFeedRateMinute(self.feedRateMinute, splitLine)
                        if self.activateEarlyReversal:
                                # If we did reverse and we're not already pushing back
                                if not self.extruderOn and self.didReverse and not self.reversalActive:
                                        if self.detectAndMoveToReversalEvent('M101', self.pushbackTime):
                                                self.distanceFeedRate.addLine("M101")
                                                self.reversalActive = True

                                # If we're going to reverse on next stop and we're not already reversing
                                if self.extruderOn and self.reversalWorthy and not self.reversalActive:
                                        assert(not self.didReverse)
                                        if self.detectAndMoveToReversalEvent('M103', self.reversalTime):
                                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                                self.distanceFeedRate.addLine("M102")
                                                self.reversalActive = True
                                                self.didReverse = True

		elif firstWord == 'M101':
                        self.extruderOn = True
                        self.reversalWorthy = self.isNextStopReversalWorthy(self.reversalThreshold)
                        if self.didReverse and not self.activateEarlyReversal:
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M101")
                                self.distanceFeedRate.addLine("G04 P" + str(self.reversalTime))
                                self.reversalActive = True
                        if self.reversalActive:
                                self.distanceFeedRate.addLine("M108 R" + str(self.flowrate))
                                self.reversalActive = False
                                self.didReverse = False
                                return
                        
		elif firstWord == 'M103':
                        self.extruderOn = False
                        self.reversalActive = False
                        if self.reversalWorthy and not self.activateEarlyReversal and not self.didReverse:
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M102")
                                self.distanceFeedRate.addLine("G04 P" + str(self.pushbackTime))
                                self.didReverse = True

                # If someone else inserted a reverse command, detect this (e.g. end.gcode)
		elif firstWord == 'M102':
                        self.extruderOn = True
                        self.didReverse = True
                        self.reversalActive = True
                elif firstWord == 'M108':
                        indexOfR = gcodec.getIndexOfStartingWithSecond('R', splitLine)
                        if indexOfR > 0: 
                                self.flowrate = gcodec.getDoubleAfterFirstLetter(splitLine[indexOfR])
		self.distanceFeedRate.addLine(line)

def main():
	"Display the reversal dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor(getNewRepository())

if __name__ == "__main__":
	main()
