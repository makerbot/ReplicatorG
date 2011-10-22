#! /usr/bin/env python
"""
This page is in the table of contents.
Bookend adds the start and end files to the gcode.

The bookend manual page is at:
http://fabmetheus.crsndoo.com/wiki/index.php/Skeinforge_Bookend

==Operation==
The default 'Activate Bookend' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
Bookend looks for alteration files in the alterations folder in the .skeinforge folder in the home directory.  Bookend does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_plugins folder.

===Name of End File===
Default is 'end.gcode'.

If there is a file with the name of the "Name of End File" setting, it will be added to the very end of the gcode.

===Name of Start File===
Default is 'start.gcode'.

If there is a file with the name of the "Name of Start File" setting, it will be added to the very beginning of the gcode.

==Examples==
The following examples add the bookend information to the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and bookend.py.

> python bookend.py
This brings up the bookend dialog.

> python bookend.py Screw Holder Bottom.stl
The bookend tool is parsing the file:
Screw Holder Bottom.stl
..
The bookend tool has created the file:
.. Screw Holder Bottom_bookend.gcode

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
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


def getCraftedText(fileName, text='', repository=None):
	'Bookend a gcode linear move text.'
	return getCraftedTextFromText(archive.getTextIfEmpty(fileName, text), repository)

def getCraftedTextFromText(gcodeText, repository=None):
	'Bookend a gcode linear move text.'
	if gcodec.isProcedureDoneOrFileIsEmpty(gcodeText, 'bookend'):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository(BookendRepository())
	if not repository.activateBookend.value:
		return gcodeText
	return BookendSkein().getCraftedGcode(gcodeText, repository)

def getNewRepository():
	'Get new repository.'
	return BookendRepository()

def writeOutput(fileName, shouldAnalyze=True):
	'Bookend a gcode linear move file.  Chain bookend the gcode if the bookend procedure has not been done.'
	skeinforge_craft.writeChainTextWithNounMessage(fileName, 'bookend', shouldAnalyze)


class BookendRepository:
	"A class to handle the bookend settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.bookend.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName(fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Bookend', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://fabmetheus.crsndoo.com/wiki/index.php/Skeinforge_Bookend')
		self.activateBookend = settings.BooleanSetting().getFromValue('Activate Bookend', self, True)
		self.nameOfEndFile = settings.StringSetting().getFromValue('Name of End File:', self, 'end.gcode')
		self.nameOfStartFile = settings.StringSetting().getFromValue('Name of Start File:', self, 'start.gcode')
		self.executeTitle = 'Bookend'

	def execute(self):
		'Bookend button has been clicked.'
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class BookendSkein:
	"A class to bookend a skein of extrusions."
	def __init__(self):
		'Initialize.'
 		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0

	def addFromUpperLowerFile(self, fileName):
		"Add lines of text from the fileName or the lowercase fileName, if there is no file by the original fileName in the directory."
		self.distanceFeedRate.addLinesSetAbsoluteDistanceMode(settings.getAlterationFileLines(fileName))

	def getCraftedGcode(self, gcodeText, repository):
		"Parse gcode text and store the bevel gcode."
		self.lines = archive.getTextLines(gcodeText)
		self.addFromUpperLowerFile(repository.nameOfStartFile.value) # Add a start file if it exists.
		self.parseInitialization()
		for self.lineIndex in xrange(self.lineIndex, len(self.lines)):
			line = self.lines[self.lineIndex]
			self.distanceFeedRate.addLine(line)
		self.addFromUpperLowerFile(repository.nameOfEndFile.value) # Add an end file if it exists.
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedProcedure('bookend')
				return
			self.distanceFeedRate.addLine(line)
		


def main():
	"Display the bookend dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor(getNewRepository())

if __name__ == "__main__":
	main()
