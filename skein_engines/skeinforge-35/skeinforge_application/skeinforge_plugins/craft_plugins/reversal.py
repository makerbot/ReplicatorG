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


def getCraftedText( fileName, text, reversalRepository = None ):
	"Reversal a gcode linear move file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), reversalRepository )

def getCraftedTextFromText( gcodeText, reversalRepository = None ):
	"Reversal a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'reversal'):
		return gcodeText
	if reversalRepository == None:
		reversalRepository = settings.getReadRepository( ReversalRepository() )
	if not reversalRepository.activateReversal.value:
		return gcodeText
	return ReversalSkein().getCraftedGcode( gcodeText, reversalRepository )

def getNewRepository():
	"Get the repository constructor."
	return ReversalRepository()

def writeOutput(fileName=''):
	"Reversal a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage( fileName, 'reversal')


class ReversalRepository:
	"A class to handle the reversal settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.reversal.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Reversal', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Reversal')
		self.activateReversal = settings.BooleanSetting().getFromValue('Activate Reversal', self, False )
		self.reversalRPM = settings.FloatSpin().getFromValue( 2, 'Reversal speed (RPM):', self, 50, 35 )
		self.reversalTime = settings.FloatSpin().getFromValue( 10, 'Reversal time (milliseconds):', self, 1000, 75 )
		self.reversalThreshold = settings.FloatSpin().getFromValue( 0.1, 'Reversal threshold (mm):', self, 5.0, 1.0 )
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
                self.reversalActive = False;

	def getCraftedGcode( self, gcodeText, reversalRepository ):
		"Parse gcode text and store the reversal gcode."
		self.lines = archive.getTextLines(gcodeText)
		self.reversalRepository = reversalRepository
		self.parseInitialization( reversalRepository )
		for self.lineIndex in xrange( self.lineIndex, len(self.lines) ):
			line = self.lines[self.lineIndex]
			self.parseLine(line)
		return self.distanceFeedRate.output.getvalue()


	def getDistanceToThreadBeginning(self):
		"Get the distance from the current location to the beginning of the next thread. Needs self.oldLocation."
		line = self.lines[self.lineIndex]
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		lastThreadLocation = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
		totalDistance = 0.0
		for afterIndex in xrange( self.lineIndex + 1, len(self.lines) ):
			line = self.lines[ afterIndex ]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine( lastThreadLocation, splitLine )
				totalDistance += location.distance( lastThreadLocation )
				lastThreadLocation = location
			elif firstWord == 'M101':
				return totalDistance
		return None

	def parseInitialization( self, reversalRepository ):
		'Parse gcode initialization and store the parameters.'
		self.reversalRPM = self.reversalRepository.reversalRPM.value
		self.reversalTime = self.reversalRepository.reversalTime.value
		self.reversalThreshold = self.reversalRepository.reversalThreshold.value
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

        # This is the main loop
	def parseLine(self, line):
		"Parse a gcode line and add it to the bevel gcode."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			self.oldLocation = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
		elif firstWord == 'M101':
                        if self.reversalActive:
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M101")
                                self.distanceFeedRate.addLine("G04 P" + str(self.reversalTime))
                                self.distanceFeedRate.addLine("M108 R" + str(self.flowrate))
                                return
		elif firstWord == 'M103':
			distance = self.getDistanceToThreadBeginning()
                        self.reversalActive = (distance != None) and (distance > self.reversalThreshold);
                        if self.reversalActive:
                                self.distanceFeedRate.addLine("M108 R" + str(self.reversalRPM))
                                self.distanceFeedRate.addLine("M102")
                                self.distanceFeedRate.addLine("G04 P" + str(self.reversalTime))
		self.distanceFeedRate.addLine(line)



def main():
	"Display the reversal dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
