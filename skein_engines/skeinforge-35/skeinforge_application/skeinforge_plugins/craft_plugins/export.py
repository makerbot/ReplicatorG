"""
This page is in the table of contents.
Export is a script to pick an export plugin and optionally print the output to a file.

The export manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Export

==Operation==
The default 'Activate Export' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
===Also Send Output To===
Default is empty.

Defines the output name for sending to a file or pipe.  A common choice is sys.stdout to print the output in the shell screen.  Another common choice is sys.stderr.  With the empty default, nothing will be done.

===Delete Comments===
Default is on.

When selected, export will delete the comments.  The comments are not necessary to run a fabricator.

===Export Operations===
Export presents the user with a choice of the export plugins in the export_plugins folder.  The chosen plugin will then modify the gcode or translate it into another format.  There is also the "Do Not Change Output" choice, which will not change the output.  An export plugin is a script in the export_plugins folder which has the getOutput function, the globalIsReplaceable variable and if it's output is not replaceable, the writeOutput function.

===File Extension===
Default is gcode.

Defines the file extension added to the name of the output file.

===Save Penultimate Gcode===
Default is off.

When selected, export will save the gcode with the suffix '_penultimate.gcode' just before it is exported.  This is useful because the code after it is exported could be in a form which the viewers can not display.

==Alterations==
Export looks for alteration files in the alterations folder in the .skeinforge folder in the home directory.  Export does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_plugins folder. If it doesn't find anything there it looks in the skeinforge_plugins folder.

===replace.csv===
When export is exporting the code, if there is a tab separated file replace.csv, it will replace the string in the first column by its replacement in the second column.  There is an example file replace_example.csv to demonstrate the tab separated format, which can be edited in a text editor or a spreadsheet.

==Examples==
The following examples export the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and export.py.


> python export.py
This brings up the export dialog.


> python export.py Screw Holder Bottom.stl
The export tool is parsing the file:
Screw Holder Bottom.stl
..
The export tool has created the file:
.. Screw Holder Bottom_export.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import export
>>> export.main()
This brings up the export dialog.


>>> export.writeOutput('Screw Holder Bottom.stl')
The export tool is parsing the file:
Screw Holder Bottom.stl
..
The export tool has created the file:
.. Screw Holder Bottom_export.gcode

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
from skeinforge_application.skeinforge_utilities import skeinforge_analyze
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import cStringIO
import os
import sys
import time


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getCraftedTextFromText( gcodeText, exportRepository = None ):
	"Export a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'export'):
		return gcodeText
	if exportRepository == None:
		exportRepository = settings.getReadRepository( ExportRepository() )
	if not exportRepository.activateExport.value:
		return gcodeText
	return ExportSkein().getCraftedGcode( exportRepository, gcodeText )

def getDistanceGcode( exportText ):
	"Get gcode lines with distance variable added."
	lines = archive.getTextLines( exportText )
	oldLocation = None
	for line in lines:
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		firstWord = None
		if len(splitLine) > 0:
			firstWord = splitLine[0]
		if firstWord == 'G1':
			location = gcodec.getLocationFromSplitLine(oldLocation, splitLine)
			if oldLocation != None:
				distance = location.distance( oldLocation )
				print( distance )
			oldLocation = location
	return exportText

def getNewRepository():
	"Get the repository constructor."
	return ExportRepository()

def getReplaced( exportText ):
	"Get text with strings replaced according to replace.csv file."
	replaceText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Replace.csv')
	if replaceText == '':
		return exportText
	lines = archive.getTextLines( replaceText )
	for line in lines:
                splitLine = line.replace('\\n', '\t').split('\t')
                if len(splitLine) > 1:
                        exportText = exportText.replace(splitLine[0], '\n'.join(splitLine[1 :]))
	return exportText

def getSelectedPluginModule( plugins ):
	"Get the selected plugin module."
	for plugin in plugins:
		if plugin.value:
			return archive.getModuleWithDirectoryPath( plugin.directoryPath, plugin.name )
	return None

def writeOutput(fileName=''):
	"Export a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName == '':
		return
	exportRepository = ExportRepository()
	settings.getReadRepository( exportRepository )
	startTime = time.time()
	print('File ' + archive.getSummarizedFileName(fileName) + ' is being chain exported.')
	suffixFileName = fileName[ : fileName.rfind('.') ] + '.' + exportRepository.fileExtension.value
	gcodeText = gcodec.getGcodeFileText( fileName, '')
	procedures = skeinforge_craft.getProcedures('export', gcodeText )
	gcodeText = skeinforge_craft.getChainTextFromProcedures( fileName, procedures[ : - 1 ], gcodeText )
	if gcodeText == '':
		return
	skeinforge_analyze.writeOutput( fileName, suffixFileName, gcodeText )
	if exportRepository.savePenultimateGcode.value:
		penultimateFileName = fileName[ : fileName.rfind('.') ] + '_penultimate.gcode'
		archive.writeFileText( penultimateFileName, gcodeText )
		print('The penultimate file is saved as ' + archive.getSummarizedFileName( penultimateFileName ) )
	exportChainGcode = getCraftedTextFromText( gcodeText, exportRepository )
	replaceableExportChainGcode = None
	selectedPluginModule = getSelectedPluginModule( exportRepository.exportPlugins )
	if selectedPluginModule == None:
		replaceableExportChainGcode = exportChainGcode
	else:
		if selectedPluginModule.globalIsReplaceable:
			replaceableExportChainGcode = selectedPluginModule.getOutput( exportChainGcode )
		else:
			selectedPluginModule.writeOutput( suffixFileName, exportChainGcode )
	if replaceableExportChainGcode != None:
		replaceableExportChainGcode = getReplaced( replaceableExportChainGcode )
		archive.writeFileText( suffixFileName, replaceableExportChainGcode )
		print('The exported file is saved as ' + archive.getSummarizedFileName(suffixFileName) )
	if exportRepository.alsoSendOutputTo.value != '':
		if replaceableExportChainGcode == None:
			replaceableExportChainGcode = selectedPluginModule.getOutput( exportChainGcode )
		exec('print >> ' + exportRepository.alsoSendOutputTo.value + ', replaceableExportChainGcode')
	print('It took %s to export the file.' % euclidean.getDurationString( time.time() - startTime ) )


class ExportRepository:
	"A class to handle the export settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.export.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Export', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Export')
		self.activateExport = settings.BooleanSetting().getFromValue('Activate Export', self, True )
		self.alsoSendOutputTo = settings.StringSetting().getFromValue('Also Send Output To:', self, '')
		self.deleteComments = settings.BooleanSetting().getFromValue('Delete Comments', self, True )
		exportPluginsFolderPath = archive.getAbsoluteFrozenFolderPath( __file__, 'export_plugins')
		exportStaticDirectoryPath = os.path.join( exportPluginsFolderPath, 'static_plugins')
		exportPluginFileNames = archive.getPluginFileNamesFromDirectoryPath( exportPluginsFolderPath )
		exportStaticPluginFileNames = archive.getPluginFileNamesFromDirectoryPath( exportStaticDirectoryPath )
		self.exportLabel = settings.LabelDisplay().getFromName('Export Operations: ', self )
		self.exportPlugins = []
		exportLatentStringVar = settings.LatentStringVar()
		self.doNotChangeOutput = settings.RadioCapitalized().getFromRadio( exportLatentStringVar, 'Do Not Change Output', self, True )
		self.doNotChangeOutput.directoryPath = None
		allExportPluginFileNames = exportPluginFileNames + exportStaticPluginFileNames
		for exportPluginFileName in allExportPluginFileNames:
			exportPlugin = None
			if exportPluginFileName in exportPluginFileNames:
				path = os.path.join( exportPluginsFolderPath, exportPluginFileName )
				exportPlugin = settings.RadioCapitalizedButton().getFromPath( exportLatentStringVar, exportPluginFileName, path, self, False )
				exportPlugin.directoryPath = exportPluginsFolderPath
			else:
				exportPlugin = settings.RadioCapitalized().getFromRadio( exportLatentStringVar, exportPluginFileName, self, False )
				exportPlugin.directoryPath = exportStaticDirectoryPath
			self.exportPlugins.append( exportPlugin )
		self.fileExtension = settings.StringSetting().getFromValue('File Extension:', self, 'gcode')
		self.savePenultimateGcode = settings.BooleanSetting().getFromValue('Save Penultimate Gcode', self, False )
		self.executeTitle = 'Export'

	def execute(self):
		"Export button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class ExportSkein:
	"A class to export a skein of extrusions."
	def __init__(self):
		self.decimalPlacesExported = 2
		self.output = cStringIO.StringIO()

	def addLine(self, line):
		"Add a line of text and a newline to the output."
		if line != '':
			self.output.write( line + '\n')

	def getCraftedGcode( self, exportRepository, gcodeText ):
		"Parse gcode text and store the export gcode."
		lines = archive.getTextLines(gcodeText)
		for line in lines:
			self.parseLine( exportRepository, line )
		return self.output.getvalue()

	def getLineWithTruncatedNumber(self, character, line, splitLine):
		'Get a line with the number after the character truncated.'
		numberString = gcodec.getStringFromCharacterSplitLine(character, splitLine)
		if numberString == None:
			return line
		roundedNumberString = euclidean.getRoundedToPlacesString(self.decimalPlacesExported, float(numberString))
		return gcodec.getLineWithValueString(character, line, splitLine, roundedNumberString)

	def parseLine(self, exportRepository, line):
		"Parse a gcode line."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == '(<decimalPlacesCarried>':
			self.decimalPlacesExported = max( 1, int(splitLine[1]) - 1 )
		if firstWord[0] == '(' and exportRepository.deleteComments.value:
			return
		if firstWord == '(</extruderInitialization>)':
			self.addLine('(<procedureDone> export </procedureDone>)')
		if firstWord != 'G1' and firstWord != 'G2' and firstWord != 'G3' :
			self.addLine(line)
			return
		line = self.getLineWithTruncatedNumber('X', line, splitLine)
		line = self.getLineWithTruncatedNumber('Y', line, splitLine)
		line = self.getLineWithTruncatedNumber('Z', line, splitLine)
		line = self.getLineWithTruncatedNumber('I', line, splitLine)
		line = self.getLineWithTruncatedNumber('J', line, splitLine)
		line = self.getLineWithTruncatedNumber('R', line, splitLine)
		self.addLine(line)


def main():
	"Display the export dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
