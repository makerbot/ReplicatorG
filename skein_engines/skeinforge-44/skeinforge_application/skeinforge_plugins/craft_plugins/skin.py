"""
This page is in the table of contents.
Skin is a script to smooth the surface skin of an object by replacing the perimeter surface with a surface printed at half the carve
height.  This gives the impression that the object was carved at a much thinner height giving a high-quality finish, but still prints 
in a relatively short time.  The latest process has some similarities with a description at:
http://adventuresin3-dprinting.blogspot.com/2011/05/skinning.html

The skin manual page is at:
http://fabmetheus.crsndoo.com/wiki/index.php/Skeinforge_Skin


==Operation==
The default 'Activate Skin' checkbox is off.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
===Hop When Extruding Infill===
Default is off.

When selected, the extruder will hop before and after extruding the lower infill in order to avoid the regular thickness threads.

====Layer From====
Default is one.

Defines which layer of the print the skinning process starts from. It is not wise to set this to zero, skinning the bottom layer is likely to cause the bottom perimeter not to adhere well to the print surface.

====Tips====
Due to the very small Z-axis moves skinning can generate as it prints the perimeter, it can cause the Z-axis speed to be limited by the Limit plug-in, if you have it enabled. This can cause some printers to pause excessively during each layer change. To overcome this, ensure that the Z-axis max speed in the Limit tool is set to an appropriate value for your printer, e.g. 10mm/s

Since Skin prints two half-height perimeter layers for each layer, printing the perimeter last causes the print head to travel down from the current print height. Depending on the shape of your extruder nozzle, you may get higher quality prints if you print the perimeters first, so the print head always travels up.  This is set via the Thread Sequence Choice setting in the Fill tool.


==Examples==
The following examples skin the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and skin.py.

> python skin.py
This brings up the skin dialog.

> python skin.py Screw Holder Bottom.stl
The skin tool is parsing the file:
Screw Holder Bottom.stl
..
The skin tool has created the file:
.. Screw Holder Bottom_skin.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.geometry.solids import triangle_mesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import sys


__author__ = 'Enrique Perez (perez_enrique aht yahoo.com) & James Blackwell (jim_blag ahht hotmail.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


def getCraftedText(fileName, gcodeText, repository=None):
	'Skin a gcode linear move text.'
	return getCraftedTextFromText(archive.getTextIfEmpty(fileName, gcodeText), repository)

def getCraftedTextFromText(gcodeText, repository=None):
	'Skin a gcode linear move text.'
	if gcodec.isProcedureDoneOrFileIsEmpty(gcodeText, 'skin'):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository(SkinRepository())
	if not repository.activateSkin.value:
		return gcodeText
	return SkinSkein().getCraftedGcode(gcodeText, repository)

def getNewRepository():
	'Get new repository.'
	return SkinRepository()

def writeOutput(fileName, shouldAnalyze=True):
	'Skin a gcode linear move file.  Chain skin the gcode if it is not already skinned.'
	skeinforge_craft.writeChainTextWithNounMessage(fileName, 'skin', shouldAnalyze)


class SkinRepository:
	'A class to handle the skin settings.'
	def __init__(self):
		'Set the default settings, execute title & settings fileName.'
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.skin.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Skin', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://fabmetheus.crsndoo.com/wiki/index.php/Skeinforge_Skin')
		self.activateSkin = settings.BooleanSetting().getFromValue('Activate Skin', self, False)
		self.halfWidthPerimeter = settings.BooleanSetting().getFromValue('Half Width Perimeter', self, True)
		self.hopWhenExtrudingInfill = settings.BooleanSetting().getFromValue('Hop When Extruding Infill', self, False)
		self.layersFrom = settings.IntSpin().getSingleIncrementFromValue(0, 'Layers From (index):', self, 912345678, 1)
		self.executeTitle = 'Skin'

	def execute(self):
		'Skin button has been clicked.'
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class SkinSkein:
	'A class to skin a skein of extrusions.'
	def __init__(self):
		'Initialize.'
		self.clipOverPerimeterWidth = 0.0
 		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRateMinute = 959.0
		self.infill = None
		self.infillBoundaries = None
		self.infillBoundary = None
		self.layerCount = settings.LayerCount()
		self.layerIndex = -1
		self.lineIndex = 0
		self.lines = None
		self.maximumZFeedRateMinute = 60.0
		self.oldFlowRate = None
		self.oldLocation = None
		self.perimeter = None
		self.travelFeedRateMinute = 957.0

	def addFlowRateLine(self, flowRate):
		'Add a flow rate line.'
		self.distanceFeedRate.addLine('M108 S' + euclidean.getFourSignificantFigures(flowRate))

	def addPerimeterLoop(self, thread, z):
		'Add the perimeter loop to the gcode.'
		self.distanceFeedRate.addGcodeFromFeedRateThreadZ(self.feedRateMinute, thread, self.travelFeedRateMinute, z)

	def addSkinnedInfill(self):
		'Add skinned infill.'
		if self.infillBoundaries == None:
			return
		offsetY = 0.5 * self.skinInfillWidth
		upperZ = self.oldLocation.z
		lowerZ = upperZ - self.halfLayerThickness
		self.addFlowRateLine(0.25 * self.oldFlowRate)
		self.addSkinnedInfillBoundary(self.infillBoundaries, 0.0, upperZ, lowerZ)
		self.addSkinnedInfillBoundary(self.infillBoundaries, offsetY, upperZ, upperZ)
		self.addFlowRateLine(self.oldFlowRate)
		self.infillBoundaries = None

	def addSkinnedInfillBoundary(self, infillBoundaries, offsetY, upperZ, z):
		'Add skinned infill boundary.'
		aroundInset = 0.25 * self.skinInfillInset
		arounds = []
		aroundWidth = 0.25 * self.skinInfillInset
		endpoints = []
		pixelTable = {}
		rotatedLoops = []
		for infillBoundary in infillBoundaries:
			infillBoundaryRotated = euclidean.getRotatedComplexes(self.reverseRotation, infillBoundary)
			if offsetY != 0.0:
				for infillPointRotatedIndex, infillPointRotated in enumerate(infillBoundaryRotated):
					infillBoundaryRotated[infillPointRotatedIndex] = complex(infillPointRotated.real, infillPointRotated.imag - offsetY)
			rotatedLoops.append(infillBoundaryRotated)
		infillDictionary = triangle_mesh.getInfillDictionary(
			aroundInset, arounds, aroundWidth, self.skinInfillInset, self.skinInfillWidth, pixelTable, rotatedLoops)
		for infillDictionaryKey in infillDictionary.keys():
			xIntersections = infillDictionary[infillDictionaryKey]
			xIntersections.sort()
			for segment in euclidean.getSegmentsFromXIntersections(xIntersections, infillDictionaryKey * self.skinInfillWidth):
				for endpoint in segment:
					endpoint.point = complex(endpoint.point.real, endpoint.point.imag + offsetY)
					endpoints.append(endpoint)
		infillPaths = euclidean.getPathsFromEndpoints(endpoints, 5.0 * self.skinInfillWidth, pixelTable, aroundWidth)
		for infillPath in infillPaths:
			infillRotated = euclidean.getRotatedComplexes(self.rotation, infillPath)
			if upperZ > z and self.repository.hopWhenExtrudingInfill.value:
				self.distanceFeedRate.addGcodeMovementZWithFeedRate(self.maximumZFeedRateMinute, infillRotated[0], upperZ)
			self.distanceFeedRate.addGcodeFromFeedRateThreadZ(self.feedRateMinute, infillRotated, self.travelFeedRateMinute, z)
			lastPointRotated = infillRotated[-1]
			self.oldLocation = Vector3(lastPointRotated.real, lastPointRotated.imag, upperZ)
			if upperZ > z and self.repository.hopWhenExtrudingInfill.value:
				self.distanceFeedRate.addGcodeMovementZWithFeedRate(self.maximumZFeedRateMinute, lastPointRotated, upperZ)

	def addSkinnedPerimeter(self):
		'Add skinned perimeter.'
		if self.perimeter == None:
			return
		perimeterThread = self.perimeter[: -1]
		lowerZ = self.oldLocation.z - self.halfLayerThickness
		innerPerimeter = intercircle.getLargestInsetLoopFromLoop(perimeterThread, self.quarterPerimeterWidth)
		outerPerimeter = intercircle.getLargestInsetLoopFromLoop(perimeterThread, -self.quarterPerimeterWidth)
		innerPerimeter = self.getClippedSimplifiedLoopPathByLoop(innerPerimeter)
		outerPerimeter = self.getClippedSimplifiedLoopPathByLoop(outerPerimeter)
		if len(innerPerimeter) < 4 or len(outerPerimeter) < 4 or not self.repository.halfWidthPerimeter.value:
			self.addFlowRateLine(0.5 * self.oldFlowRate)
			self.addPerimeterLoop(self.perimeter, lowerZ)
			self.addPerimeterLoop(self.perimeter, self.oldLocation.z)
		else:
			self.addFlowRateLine(0.25 * self.oldFlowRate)
			self.addPerimeterLoop(innerPerimeter, lowerZ)
			self.addPerimeterLoop(outerPerimeter, lowerZ)
			self.addPerimeterLoop(innerPerimeter, self.oldLocation.z)
			self.addPerimeterLoop(outerPerimeter, self.oldLocation.z)
		self.addFlowRateLine(self.oldFlowRate)
		self.perimeter = None

	def getClippedSimplifiedLoopPathByLoop(self, loop):
		'Get clipped and simplified loop path from a loop.'
		if len(loop) == 0:
			return []
		loopPath = loop + [loop[0]]
		return euclidean.getClippedSimplifiedLoopPath(self.clipLength, loopPath, self.halfPerimeterWidth)

	def getCraftedGcode( self, gcodeText, repository ):
		'Parse gcode text and store the skin gcode.'
		self.lines = archive.getTextLines(gcodeText)
		self.repository = repository
		self.layersFromBottom = self.repository.layersFrom.value
		self.parseInitialization()
		self.skinInfillInset = 0.5 * (self.infillWidth + self.skinInfillWidth) * (1.0 - self.infillPerimeterOverlap)
		self.clipLength = 0.5 * self.clipOverPerimeterWidth * self.perimeterWidth
		self.parseBoundaries()
		for self.lineIndex in xrange(self.lineIndex, len(self.lines)):
			line = self.lines[self.lineIndex]
			self.parseLine(line)
		return gcodec.getGcodeWithoutDuplication('M108', self.distanceFeedRate.output.getvalue())

	def parseBoundaries(self):
		'Parse the boundaries and add them to the boundary layers.'
		self.boundaryLayers = []
		self.layerIndexTop = -1
		boundaryLoop = None
		boundaryLayer = None
		for line in self.lines[self.lineIndex :]:
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == '(</boundaryPerimeter>)':
				boundaryLoop = None
			elif firstWord == '(<boundaryPoint>':
				location = gcodec.getLocationFromSplitLine(None, splitLine)
				if boundaryLoop == None:
					boundaryLoop = []
					boundaryLayer.loops.append(boundaryLoop)
				boundaryLoop.append(location.dropAxis())
			elif firstWord == '(<layer>':
				boundaryLayer = euclidean.LoopLayer(float(splitLine[1]))
				self.boundaryLayers.append(boundaryLayer)
				self.layerIndexTop += 1
		for boundaryLayerIndex, boundaryLayer in enumerate(self.boundaryLayers):
			if len(boundaryLayer.loops) > 0:
				self.layersFromBottom += boundaryLayerIndex
				return

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(<clipOverPerimeterWidth>':
				self.clipOverPerimeterWidth = float(splitLine[1])
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedProcedure('skin')
				return
			elif firstWord == '(<infillPerimeterOverlap>':
				self.infillPerimeterOverlap = float(splitLine[1])
			elif firstWord == '(<infillWidth>':
				self.infillWidth = float(splitLine[1])
				self.skinInfillWidth = 0.5 * self.infillWidth
			elif firstWord == '(<layerThickness>':
				self.halfLayerThickness = 0.5 * float(splitLine[1])
			elif firstWord == '(<maximumZFeedRatePerSecond>':
				self.maximumZFeedRateMinute = 60.0 * float(splitLine[1])
			elif firstWord == '(<operatingFlowRate>':
				self.oldFlowRate = float(splitLine[1])
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float(splitLine[1])
				self.halfPerimeterWidth = 0.5 * self.perimeterWidth
				self.quarterPerimeterWidth = 0.25 * self.perimeterWidth
			elif firstWord == '(<travelFeedRatePerSecond>':
				self.travelFeedRateMinute = 60.0 * float(splitLine[1])
			self.distanceFeedRate.addLine(line)

	def parseLine(self, line):
		'Parse a gcode line and add it to the skin skein.'
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			self.feedRateMinute = gcodec.getFeedRateMinute(self.feedRateMinute, splitLine)
			location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
			self.oldLocation = location
			if self.infillBoundaries != None:
				return
			if self.perimeter != None:
				self.perimeter.append(location.dropAxis())
				return
		elif firstWord == '(<infill>)':
			if self.layerIndex >= self.layersFromBottom and self.layerIndex == self.layerIndexTop:
				self.infillBoundaries = []
		elif firstWord == '(</infill>)':
			self.addSkinnedInfill()
		elif firstWord == '(<infillBoundary>)':
			if self.infillBoundaries != None:
				self.infillBoundary = []
				self.infillBoundaries.append(self.infillBoundary)
		elif firstWord == '(<infillPoint>':
			if self.infillBoundaries != None:
				location = gcodec.getLocationFromSplitLine(None, splitLine)
				self.infillBoundary.append(location.dropAxis())
		elif firstWord == '(<layer>':
			self.layerCount.printProgressIncrement('skin')
			self.layerIndex += 1
		elif firstWord == 'M101' or firstWord == 'M103':
			if self.infillBoundaries != None or self.perimeter != None:
				return
		elif firstWord == 'M108':
			self.oldFlowRate = gcodec.getDoubleAfterFirstLetter(splitLine[1])
		elif firstWord == '(<perimeter>':
			if self.layerIndex >= self.layersFromBottom:
				self.perimeter = []
		elif firstWord == '(<rotation>':
			self.rotation = gcodec.getRotationBySplitLine(splitLine)
			self.reverseRotation = complex(self.rotation.real, -self.rotation.imag)
		elif firstWord == '(</perimeter>)':
			self.addSkinnedPerimeter()
		self.distanceFeedRate.addLine(line)


def main():
	'Display the skin dialog.'
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor(getNewRepository())

if __name__ == '__main__':
	main()
