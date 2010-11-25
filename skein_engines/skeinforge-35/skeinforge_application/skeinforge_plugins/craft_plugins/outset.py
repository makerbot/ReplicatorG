"""
This page is in the table of contents.
Outset outsets the perimeters of the slices of a gcode file.  The outside perimeters will be outset by half the perimeter width, and the inside perimeters will be inset by half the perimeter width.  Outset is needed for subtractive machining, like cutting or milling.

==Operation==
The default 'Activate Outset' checkbox is on.  When it is on, the gcode will be outset, when it is off, the gcode will not be changed.

==Examples==
The following examples outset the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and outset.py.


> python outset.py
This brings up the outset dialog.


> python outset.py Screw Holder Bottom.stl
The outset tool is parsing the file:
Screw Holder Bottom.stl
..
The outset tool has created the file:
.. Screw Holder Bottom_outset.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import outset
>>> outset.main()
This brings up the outset dialog.


>>> outset.writeOutput('Screw Holder Bottom.stl')
The outset tool is parsing the file:
Screw Holder Bottom.stl
..
The outset tool has created the file:
.. Screw Holder Bottom_outset.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = "$Date: 2008/28/04 $"
__license__ = 'GPL 3.0'


def getCraftedText( fileName, text = '', repository=None):
	"Outset the preface file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), repository )

def getCraftedTextFromText(gcodeText, repository=None):
	"Outset the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'outset'):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository( OutsetRepository() )
	if not repository.activateOutset.value:
		return gcodeText
	return OutsetSkein().getCraftedGcode(gcodeText, repository)

def getNewRepository():
	"Get the repository constructor."
	return OutsetRepository()

def writeOutput(fileName=''):
	"Outset the carving of a gcode file.  If no fileName is specified, outset the first unmodified gcode file in this folder."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage( fileName, 'outset')


class OutsetRepository:
	"A class to handle the outset settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.outset.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Outset', self, '')
		self.activateOutset = settings.BooleanSetting().getFromValue('Activate Outset:', self, True )
		self.executeTitle = 'Outset'

	def execute(self):
		"Outset button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class OutsetSkein:
	"A class to outset a skein of extrusions."
	def __init__(self):
		self.boundary = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.layerCount = settings.LayerCount()
		self.lineIndex = 0
		self.rotatedBoundaryLayer = None

	def addGcodeFromRemainingLoop( self, loop, radius, z ):
		"Add the remainder of the loop."
		boundary = intercircle.getLargestInsetLoopFromLoopRegardless( loop, radius )
		euclidean.addSurroundingLoopBeginning( self.distanceFeedRate, boundary, z )
		self.distanceFeedRate.addPerimeterBlock(loop, z)
		self.distanceFeedRate.addLine('(</boundaryPerimeter>)')
		self.distanceFeedRate.addLine('(</surroundingLoop>)')

	def addOutset( self, rotatedBoundaryLayer ):
		"Add outset to the layer."
		extrudateLoops = intercircle.getInsetLoopsFromLoops( - self.absoluteHalfPerimeterWidth, rotatedBoundaryLayer.loops )
		sortedLoops = trianglemesh.getLoopsInOrderOfArea( trianglemesh.compareAreaAscending, extrudateLoops )
		for sortedLoop in sortedLoops:
			self.addGcodeFromRemainingLoop( sortedLoop, self.absoluteHalfPerimeterWidth, rotatedBoundaryLayer.z )

	def getCraftedGcode(self, gcodeText, repository):
		"Parse gcode text and store the bevel gcode."
		self.repository = repository
		self.lines = archive.getTextLines(gcodeText)
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len(self.lines) ):
			self.parseLine( lineIndex )
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex].lstrip()
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine('procedureDone', 'outset')
			elif firstWord == '(<perimeterWidth>':
				self.absoluteHalfPerimeterWidth = 0.5 * abs(float(splitLine[1]))
			elif firstWord == '(<layer>':
				self.lineIndex -= 1
				return
			self.distanceFeedRate.addLine(line)

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the outset skein."
		line = self.lines[lineIndex].lstrip()
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine(None, splitLine)
			self.boundary.append( location.dropAxis(2) )
		elif firstWord == '(<layer>':
			self.layerCount.printProgressIncrement('outset')
			self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer(float(splitLine[1]))
			self.distanceFeedRate.addLine(line)
		elif firstWord == '(</layer>)':
			self.addOutset( self.rotatedBoundaryLayer )
			self.rotatedBoundaryLayer = None
		elif firstWord == '(<surroundingLoop>)':
			self.boundary = []
			self.rotatedBoundaryLayer.loops.append( self.boundary )
		if self.rotatedBoundaryLayer == None:
			self.distanceFeedRate.addLine(line)


def main():
	"Display the outset dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
