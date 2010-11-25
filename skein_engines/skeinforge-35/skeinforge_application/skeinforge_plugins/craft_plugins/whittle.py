"""
This page is in the table of contents.
Whittle will convert each polygon of a gcode file into a helix which has a vertical step down on each rotation.

==Operation==
The default 'Activate Whittle' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.  If the cutting tool can cut the slab in one cut, the 'Activate Whittle' checkbox should be off, the default is off.

==Settings==
===Maximum Vertical Step'===
Default is 0.1 mm.

Defines the maximum distance that the helix will step down on each rotation.  The number of steps in the helix will be the layer thickness divided by the 'Maximum Vertical Step', rounded up.  The amount the helix will step down is the layer thickness divided by the number of steps.  The thinner the 'Maximum Vertical Step', the more times the cutting tool will circle around on its way to the bottom of the slab.

==Examples==
The following examples whittle the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and whittle.py.


> python whittle.py
This brings up the whittle dialog.


> python whittle.py Screw Holder Bottom.stl
The whittle tool is parsing the file:
Screw Holder Bottom.stl
..
The whittle tool has created the file:
.. Screw Holder Bottom_whittle.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import whittle
>>> whittle.main()
This brings up the whittle dialog.


>>> whittle.writeOutput('Screw Holder Bottom.stl')
The whittle tool is parsing the file:
Screw Holder Bottom.stl
..
The whittle tool has created the file:
.. Screw Holder Bottom_whittle.gcode


"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import archive
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import math
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = "$Date: 2008/28/04 $"
__license__ = 'GPL 3.0'


def getCraftedText( fileName, text = '', whittleRepository = None ):
	"Whittle the preface file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), whittleRepository )

def getCraftedTextFromText( gcodeText, whittleRepository = None ):
	"Whittle the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'whittle'):
		return gcodeText
	if whittleRepository == None:
		whittleRepository = settings.getReadRepository( WhittleRepository() )
	if not whittleRepository.activateWhittle.value:
		return gcodeText
	return WhittleSkein().getCraftedGcode( whittleRepository, gcodeText )

def getNewRepository():
	"Get the repository constructor."
	return WhittleRepository()

def writeOutput(fileName=''):
	"Whittle the carving of a gcode file.  If no fileName is specified, whittle the first unmodified gcode file in this folder."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName == '':
		return
	skeinforge_craft.writeChainTextWithNounMessage( fileName, 'whittle')


class WhittleRepository:
	"A class to handle the whittle settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.whittle.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Whittled', self, '')
		self.activateWhittle = settings.BooleanSetting().getFromValue('Activate Whittle:', self, False )
		self.maximumVerticalStep = settings.FloatSpin().getFromValue( 0.02, 'Maximum Vertical Step (mm):', self, 0.42, 0.1 )
		self.executeTitle = 'Whittle'

	def execute(self):
		"Whittle button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class WhittleSkein:
	"A class to whittle a skein of extrusions."
	def __init__(self):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.layerThickness = 0.3333333333
		self.lineIndex = 0
		self.movementLines = []
		self.oldLocation = None

	def getCraftedGcode( self, whittleRepository, gcodeText ):
		"Parse gcode text and store the whittle gcode."
		self.whittleRepository = whittleRepository
		self.lines = archive.getTextLines(gcodeText)
		self.parseInitialization()
		for line in self.lines[self.lineIndex :]:
			self.parseLine(line)
		return self.distanceFeedRate.output.getvalue()

	def getLinearMove( self, line, splitLine ):
		"Get the linear move."
		location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
		self.movementLines.append(line)
		z = location.z + self.layerDeltas[0]
		self.oldLocation = location
		return self.distanceFeedRate.getLineWithZ( line, splitLine, z )

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex].lstrip()
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine('procedureDone', 'whittle')
				return
			elif firstWord == '(<layerThickness>':
				self.setLayerThinknessVerticalDeltas(splitLine)
				self.distanceFeedRate.addTagBracketedLine('layerStep', self.layerStep )
			self.distanceFeedRate.addLine(line)

	def parseLine(self, line):
		"Parse a gcode line and add it to the whittle skein."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			line = self.getLinearMove( line, splitLine )
		elif firstWord == 'M103':
			self.repeatLines()
		self.distanceFeedRate.addLine(line)

	def repeatLines(self):
		"Repeat the lines at decreasing altitude."
		for layerDelta in self.layerDeltas[1 :]:
			for movementLine in self.movementLines:
				splitLine = movementLine.split()
				location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
				z = location.z + layerDelta
				self.distanceFeedRate.addLine( self.distanceFeedRate.getLineWithZ( movementLine, splitLine, z ) )
		self.movementLines = []

	def setLayerThinknessVerticalDeltas( self, splitLine ):
		"Set the layer thickness and the vertical deltas."
		self.layerThickness = float(splitLine[1])
		numberOfSteps = int( math.ceil( self.layerThickness / self.whittleRepository.maximumVerticalStep.value ) )
		self.layerStep = self.layerThickness / float( numberOfSteps )
		self.layerDeltas = []
		halfDeltaMinusHalfTop = 0.5 * self.layerStep * ( 1.0 - numberOfSteps )
		for layerDeltaIndex in xrange( numberOfSteps - 1, - 1, - 1 ):
			layerDelta = layerDeltaIndex * self.layerStep + halfDeltaMinusHalfTop
			self.layerDeltas.append( layerDelta )


def main():
	"Display the whittle dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
