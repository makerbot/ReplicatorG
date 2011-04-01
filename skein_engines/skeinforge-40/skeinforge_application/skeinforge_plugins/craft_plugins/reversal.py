"""
Reversal is a script to rapidly revert the extruder before the end of
a thread and push back before the beginning. This is an alternative
approach to oozebane for battling filament ooze and will work for
stepper extruders when printing in 3D mode.

==Operation==

The default 'Activate Reversal' checkbox is on.  When it is on, the
functions described below will work, when it is off, the functions
will not be called.

==Settings==

===Reversal speed (RPM)===

The speed of the reversal. The meaning of RPM is dependent of how your
bot is configured, but is ususally defined to be the RPM of the gear
actually pushing the filament.

===Reversal time (milliseconds)==

For how long we are going to reverse.

===Push-back time (milliseconds)===

For how long we are going to push back. This is usually the same as
the reversal time, but could be increased due to small amounts of ooze
forming at the start of a push-back because heater barrel pressure
dropped.

===Reversal threshold (mm)===

Ignore extrusion on-times or off-times smaller than this threshold.

Since there is a small time delay from the act of push the filament
into the heater barrel to the extruded filament exits it, we don't
want to waste time doing retracts which doesn't benefit us at the
right time and place.

===Activate early reversal and push-back===

If this is activated, the reversal will be done while moving on the
last segment of the thread. If it's off, the machine will stop at the
end of the thread while reversing. Correspondingly for push-back.

"""
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


DEBUG = False

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

        FILAMENT_READY = 0
        FILAMENT_REVERSED = 1

	def __init__(self):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.flowrate = 1.99
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.newLocation = None
                self.reversalActive = False
                self.pushbackActive = False
                self.reversalWorthy = False
                self.pushbackWorthy = False
                self.feedRateMinute = 0
                self.extruderOn = False
                self.filamentState = ReversalSkein.FILAMENT_READY

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

                # Cases:
                # 1) The current movement doesn't bring us within threshold
                #    -> Return None
                # 2) The current movement is >= threshold and the requested command is next
                #    -> A split will happen here -> Return time to split
                # 3) The current movement brings us within the threshold and more movements follow
                #    -> A split will happen here -> Return time to split
                # 4) The current movement is < threshold
                #    -> We need more time to reverse properly -> Return missing time (negative)

		line = self.lines[self.lineIndex]
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
                # location of the end of the current movement command
		lastThreadLocation = gcodec.getLocationFromSplitLine(self.newLocation, splitLine)
                currentDistance = lastThreadLocation.distance(self.oldLocation)
		currentTime = currentDistance / self.feedRateMinute * 60000
		totalTime = 0.0
                totalDistance = 0.0
		for afterIndex in xrange(self.lineIndex + 1, len(self.lines)):
			line = self.lines[afterIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
                                # Time to the end of next movement command
				location = gcodec.getLocationFromSplitLine(lastThreadLocation, splitLine)
                                distance = location.distance(lastThreadLocation)
                                totalDistance += distance
				totalTime += distance / self.feedRateMinute * 60000
				lastThreadLocation = location
                                # Case 1)
				if totalTime >= threshold:
					return None
                        # We reached the wanted command before our threshold -> we must split the
                        # current command
			elif firstWord == command:
                                if DEBUG: self.distanceFeedRate.addLine(
                                        "( totalDistance=" +
                                        str(totalDistance+currentDistance) + " )")
                                return totalTime + currentTime - threshold
		return None

	def isNextMovementReversalWorthy(self, threshold, starttoken, endtoken):
		"""
                Returns True if we should activate reversal on the next movement,
                False otherwise.

                The given threshold defines the movement distance
                under which we don't reverse.

                starttoken,endtoken is the movement delimiter (M101,M103 for thread, 
                M103,M101 for stop)
                """
		line = self.lines[self.lineIndex]
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		lastThreadLocation = gcodec.getLocationFromSplitLine(self.newLocation, splitLine)
		startTokenReached = False
		endTokenReached = False
		totalDistance = 0.0
		for afterIndex in xrange(self.lineIndex + 1, len(self.lines)):
			line = self.lines[afterIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine(lastThreadLocation, splitLine)
				if startTokenReached:
					totalDistance += location.distance(lastThreadLocation)
					if totalDistance >= threshold:
                                                if DEBUG: self.distanceFeedRate.addLine("( Next movement worthy: " + str(totalDistance) + " )")
						return True
				lastThreadLocation = location
			elif firstWord == endtoken:
				endTokenReached = True
                                if startTokenReached: return False
			elif firstWord == starttoken:
				startTokenReached = True
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
                timeToSplit = self.getTimeToCommand(command, thresholdTime)
                if timeToSplit != None:
                        segment = self.newLocation - self.oldLocation
                        segmentLength = segment.magnitude()
                        segmentTime = segment.magnitude() / self.feedRateMinute * 60000
                        if DEBUG: 
                                self.distanceFeedRate.addLine(
                                        "(oldLocation="+str(self.oldLocation)+")")
                                self.distanceFeedRate.addLine(
                                        "(newLocation="+str(self.newLocation)+")")
                                self.distanceFeedRate.addLine(
                                        "("+
                                        " thresholdTime="+str(thresholdTime)+
                                        " segmentLength="+str(segmentLength)+
                                        " segmentTime="+str(segmentTime)+
                                        " timeToSplit="+str(timeToSplit)+
                                        ")")
                        if timeToSplit >= 0:
                                reversalPosition = self.oldLocation + segment * timeToSplit / segmentTime
                                self.reversalFeedrate = self.feedRateMinute
                                line = self.getLinearMoveWithFeedRate(self.reversalFeedrate, reversalPosition)
                                self.distanceFeedRate.addLine(line)
                        else:
                                # We don't have time to perform the full reversal
                                # -> do as much as we can, take notes
                                reversalPosition = self.newLocation
                                self.reversalFeedrate = self.feedRateMinute * (thresholdTime + timeToSplit)/thresholdTime
                        if DEBUG: 
                                self.distanceFeedRate.addLine(
                                        "("+
                                        " reversalPosition="+str(reversalPosition)+
                                        " reversalFeedrate="+str(self.reversalFeedrate)+
                                        ")")
                        return True
                return False

        def printState(self):
                        if self.filamentState == ReversalSkein.FILAMENT_REVERSED: state = "REVERSED"
                        elif self.filamentState == ReversalSkein.FILAMENT_READY: state = "READY"
                        self.distanceFeedRate.addLine("( state:"+
                                                      " reversalActive="+str(self.reversalActive)+
                                                      " pushbackActive="+str(self.pushbackActive)+
                                                      " filamentState="+state+
                                                      " )")

        # This is the main loop
	def parseLine(self, line):
		"""Parse a gcode line and add it to the reversal gcode.
                Overview:
                o Keep track of gcode state:
                  current flowrate (self.flowrate), 
                  current position (self.newLocation/oldLocation)
                  current feedrate (self.feedRateMinute)
                  current logical extruder state (self.extruderOn)
                o Keep track of reversal state: (self.didReverse, self.reversalActive)
                o If early reversal is NOT active, it will insert a pause command at the end
                  of each thread (G4). This will cause the extruder to move without XYZ.
                  This is not recommended as it might cause some blobbing
                o If early reversal is active
                
                """
                if DEBUG: self.distanceFeedRate.addLine("( line: " + line + " )")
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]

		if firstWord == 'M108':
                        # If we didn't already rpmify the M108 commands, do this now
                        line = line.replace( 'M108 S', 'M108 R' )
                        splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
                        # Keep track of current flowrate
                        indexOfR = gcodec.getIndexOfStartingWithSecond('R', splitLine)
                        if indexOfR > 0:
                                self.flowrate = gcodec.getDoubleAfterFirstLetter(splitLine[indexOfR])
                # Pick up all movement commands
		elif firstWord == 'G1':
                        # Location at the start of this movement
			self.oldLocation = self.newLocation
                        # Location at the end start of this movement
			self.newLocation = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
                        self.feedRateMinute = gcodec.getFeedRateMinute(self.feedRateMinute, splitLine)
                        if self.activateEarlyReversal:
                                # If we did reverse and we're not already pushing back
                                if (not self.extruderOn and 
                                    self.filamentState == ReversalSkein.FILAMENT_REVERSED and 
                                    self.pushbackWorthy and 
                                    not self.pushbackActive):
                                        # If this movement crosses the push-back point, this will
                                        # move us to where pushback starts and return true.
                                        if self.detectAndMoveToReversalEvent('M101', self.pushbackTime):
                                                if DEBUG: self.distanceFeedRate.addLine("( activating pushback: )")
                                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                                self.distanceFeedRate.addLine("M101")
                                                self.pushbackActive = True
                                                self.filamentState = ReversalSkein.FILAMENT_READY

                                # If we're going to reverse on next stop and we're not already reversing
                                if (self.extruderOn and 
                                    self.filamentState == ReversalSkein.FILAMENT_READY and
                                    self.reversalWorthy and 
                                    not self.reversalActive):
                                        # If this movement crosses the reversal point, this will
                                        # move us to where reversal starts and return true.
                                        if self.detectAndMoveToReversalEvent('M103', self.reversalTime):
                                                if DEBUG: self.distanceFeedRate.addLine("( activating reverse: )")
                                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                                self.distanceFeedRate.addLine("M102")
                                                self.reversalActive = True
                                                self.filamentState = ReversalSkein.FILAMENT_REVERSED
                        # Note: We don't return here since the current G1 command will
                        # keep moving to the end of the current movement thread with 
                        # reversal/push-back potentially active.

                # Detect extruder ON commands
		elif firstWord == 'M101':
                        self.extruderOn = True
                        self.reversalWorthy = self.isNextMovementReversalWorthy(self.reversalThreshold, 'M103', 'M101')
                        if (not self.activateEarlyReversal and 
                            self.pushbackWorthy and 
                            self.filamentState == ReversalSkein.FILAMENT_REVERSED):
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M101")
                                self.distanceFeedRate.addLine("G04 P" + str(self.reversalTime))
                                self.pushbackActive = True
                        if self.pushbackActive:
                                self.distanceFeedRate.addLine("M108 R" + str(self.flowrate))
                                self.pushbackActive = False
                                self.filamentState = ReversalSkein.FILAMENT_READY
                                if DEBUG: self.printState()
                                return # Motor is already on - no need to emit the M101

                # Detect extruder OFF commands
		elif firstWord == 'M103':
                        self.extruderOn = False
                        self.pushbackWorthy = self.isNextMovementReversalWorthy(self.reversalThreshold, 'M101', 'M103')
                        if (not self.activateEarlyReversal and 
                            self.reversalWorthy and 
                            self.filamentState == ReversalSkein.FILAMENT_READY):
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M102")
                                self.distanceFeedRate.addLine("G04 P" + str(self.pushbackTime))
                                self.filamentState = ReversalSkein.FILAMENT_REVERSED
                        if self.reversalActive:
                                self.reversalActive = False
                                self.filamentState = ReversalSkein.FILAMENT_REVERSED
                                # For safety, reset feedrate here. It's a known issue
                                # that some other plugins keep track of flowrate state
                                # and removed redundant settings, so we need to keep
                                # the original state whenever we're done with an operation
                                self.distanceFeedRate.addLine("M108 R" + str(self.flowrate))

                # If someone else inserted a reverse command, detect this and update state
                # (e.g. end.gcode for Makerbots does this for retracting the filament)
		elif firstWord == 'M102':
                        self.extruderOn = True
                        self.filamentState = ReversalSkein.FILAMENT_REVERSED
                        self.reversalActive = True


                if DEBUG: 
                        if firstWord == 'M101' or firstWord == 'M102' or firstWord == 'M103':
                                self.printState()
                # The reversal may change the feedrate for the remaining reversal movements
                if (self.reversalActive or self.pushbackActive) and firstWord == 'G1':
                        line = self.distanceFeedRate.getLineWithFeedRate(self.reversalFeedrate, line, splitLine)
                self.distanceFeedRate.addLine(line)

def main():
	"Display the reversal dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor(getNewRepository())

if __name__ == "__main__":
	main()
