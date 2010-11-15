"""
This page is in the table of contents.
Multiply is a script to multiply the shape into an array of copies arranged in a table.

The multiply manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Multiply

Besides using the multiply tool, another way of printing many copies of the model is to duplicate the model in Art of Illusion, however many times you want, with the appropriate offsets.  Then you can either use the Join Objects script in the scripts submenu to create a combined shape or you can export the whole scene as an xml file, which skeinforge can then slice.

==Operation==
The default 'Activate Multiply' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
===Center===
Default is the origin.

The center of the shape will be moved to the "Center X" and "Center Y" coordinates.

====Center X====
====Center Y====

===Number of Cells===
====Number of Columns====
Default is one.

Defines the number of columns in the array table.

====Number of Rows====
Default is one.

Defines the number of rows in the table.

===Separation over Perimeter Width===
Default is fifteen.

Defines the ratio of separation between the shape copies over the extrusion width.

==Examples==
The following examples multiply the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and multiply.py.


> python multiply.py
This brings up the multiply dialog.


> python multiply.py Screw Holder Bottom.stl
The multiply tool is parsing the file:
Screw Holder Bottom.stl
..
The multiply tool has created the file:
.. Screw Holder Bottom_multiply.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import multiply
>>> multiply.main()
This brings up the multiply dialog.


>>> multiply.writeOutput('Screw Holder Bottom.stl')
The multiply tool is parsing the file:
Screw Holder Bottom.stl
..
The multiply tool has created the file:
.. Screw Holder Bottom_multiply.gcode

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import math
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getCraftedText( fileName, text = '', multiplyRepository = None ):
	"Multiply the fill file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), multiplyRepository )

def getCraftedTextFromText( gcodeText, multiplyRepository = None ):
	"Multiply the fill text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'multiply'):
		return gcodeText
	if multiplyRepository == None:
		multiplyRepository = settings.getReadRepository( MultiplyRepository() )
	if not multiplyRepository.activateMultiply.value:
		return gcodeText
	return MultiplySkein().getCraftedGcode( gcodeText, multiplyRepository )

def getNewRepository():
	"Get the repository constructor."
	return MultiplyRepository()

def writeOutput(fileName=''):
	"Multiply a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName != '':
		skeinforge_craft.writeChainTextWithNounMessage( fileName, 'multiply')


class MultiplyRepository:
	"A class to handle the multiply settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.multiply.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Multiply', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Multiply')
		self.activateMultiply = settings.BooleanSetting().getFromValue('Activate Multiply:', self, False )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Center -', self )
		self.centerX = settings.FloatSpin().getFromValue( - 100.0, 'Center X (mm):', self, 100.0, 0.0 )
		self.centerY = settings.FloatSpin().getFromValue( -100.0, 'Center Y (mm):', self, 100.0, 0.0 )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Number of Cells -', self )
		self.numberOfColumns = settings.IntSpin().getFromValue( 1, 'Number of Columns (integer):', self, 10, 1 )
		self.numberOfRows = settings.IntSpin().getFromValue( 1, 'Number of Rows (integer):', self, 10, 1 )
		settings.LabelSeparator().getFromRepository(self)
		self.separationOverPerimeterWidth = settings.FloatSpin().getFromValue( 5.0, 'Separation over Perimeter Width (ratio):', self, 25.0, 15.0 )
		self.executeTitle = 'Multiply'

	def execute(self):
		"Multiply button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class MultiplySkein:
	"A class to multiply a skein of extrusions."
	def __init__(self):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.isExtrusionActive = False
		self.layerCount = settings.LayerCount()
		self.layerIndex = 0
		self.layerLines = []
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.rowIndex = 0
		self.shouldAccumulate = True

	def addElement( self, offset ):
		"Add moved element to the output."
		for line in self.layerLines:
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				movedLocation = self.getMovedLocationSetOldLocation( offset, splitLine )
				line = self.distanceFeedRate.getLinearGcodeMovement( movedLocation.dropAxis(2), movedLocation.z )
			elif firstWord == '(<boundaryPoint>':
				movedLocation = self.getMovedLocationSetOldLocation( offset, splitLine )
				line = self.distanceFeedRate.getBoundaryLine( movedLocation )
			self.distanceFeedRate.addLine(line)

	def addLayer(self):
		"Add multiplied layer to the output."
		self.layerCount.printProgressIncrement('multiply')
		self.addRemoveThroughLayer()
		offset = self.centerOffset - self.arrayCenter - self.shapeCenter
		for rowIndex in xrange( self.multiplyRepository.numberOfRows.value ):
			yRowOffset = float( rowIndex ) * self.extentPlusSeparation.imag
			if self.layerIndex % 2 == 1:
				yRowOffset = self.arrayExtent.imag - yRowOffset
			for columnIndex in xrange( self.multiplyRepository.numberOfColumns.value ):
				xColumnOffset = float( columnIndex ) * self.extentPlusSeparation.real
				if self.rowIndex % 2 == 1:
					xColumnOffset = self.arrayExtent.real - xColumnOffset
				elementOffset = complex( offset.real + xColumnOffset, offset.imag + yRowOffset )
				self.addElement( elementOffset )
			self.rowIndex += 1
		if len( self.layerLines ) > 1:
			self.layerIndex += 1
		self.layerLines = []

	def addRemoveThroughLayer(self):
		'Parse gcode initialization and store the parameters.'
		for layerLineIndex in xrange( len( self.layerLines ) ):
			line = self.layerLines[ layerLineIndex ]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.addLine(line)
			if firstWord == '(<layer>':
				self.layerLines = self.layerLines[ layerLineIndex + 1 : ]
				return

	def getCraftedGcode( self, gcodeText, multiplyRepository ):
		"Parse gcode text and store the multiply gcode."
		self.centerOffset = complex( multiplyRepository.centerX.value, multiplyRepository.centerY.value )
		self.multiplyRepository = multiplyRepository
		self.numberOfColumns = multiplyRepository.numberOfColumns.value
		self.numberOfRows = multiplyRepository.numberOfRows.value
		self.lines = archive.getTextLines(gcodeText)
		self.parseInitialization()
		self.setCorners()
		for line in self.lines[self.lineIndex :]:
			self.parseLine(line)
		return self.distanceFeedRate.output.getvalue()

	def getMovedLocationSetOldLocation( self, offset, splitLine ):
		"Get the moved location and set the old location."
		location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
		self.oldLocation = location
		return Vector3( location.x + offset.real, location.y + offset.imag, location.z )

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine('(<procedureDone> multiply </procedureDone>)')
				self.distanceFeedRate.addLine(line)
				self.lineIndex += 1
				return
			elif firstWord == '(<perimeterWidth>':
				self.absolutePerimeterWidth = abs(float(splitLine[1]))
			self.distanceFeedRate.addLine(line)

	def parseLine(self, line):
		"Parse a gcode line and add it to the multiply skein."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == '(</layer>)':
			self.addLayer()
			self.distanceFeedRate.addLine(line)
			return
		elif firstWord == '(</extrusion>)':
			self.shouldAccumulate = False
		if self.shouldAccumulate:
			self.layerLines.append(line)
			return
		self.distanceFeedRate.addLine(line)

	def setCorners(self):
		"Set maximum and minimum corners and z."
		cornerHighComplex = complex(-987654321.0, -987654321.0)
		cornerLowComplex = -cornerHighComplex
		for line in self.lines[self.lineIndex :]:
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
				if self.isExtrusionActive:
					locationComplex = location.dropAxis()
					cornerHighComplex = euclidean.getMaximum(locationComplex,  cornerHighComplex)
					cornerLowComplex = euclidean.getMinimum(locationComplex,  cornerLowComplex)
				self.oldLocation = location
			elif firstWord == 'M101':
				self.isExtrusionActive = True
			elif firstWord == 'M103':
				self.isExtrusionActive = False
		self.extent = cornerHighComplex - cornerLowComplex
		self.shapeCenter = 0.5 * ( cornerHighComplex + cornerLowComplex )
		self.separation = self.multiplyRepository.separationOverPerimeterWidth.value * self.absolutePerimeterWidth
		self.extentPlusSeparation = self.extent + complex( self.separation, self.separation )
		columnsMinusOne = self.numberOfColumns - 1
		rowsMinusOne = self.numberOfRows - 1
		self.arrayExtent = complex( self.extentPlusSeparation.real * columnsMinusOne, self.extentPlusSeparation.imag * rowsMinusOne )
		self.arrayCenter = 0.5 * self.arrayExtent


def main():
	"Display the multiply dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
