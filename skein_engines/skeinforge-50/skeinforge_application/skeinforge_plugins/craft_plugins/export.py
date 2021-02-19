"""
This page is in the table of contents.
Export is a craft tool to pick an export plugin, add information to the file name, and delete comments.

The export manual page is at:
http://fabmetheus.crsndoo.com/wiki/index.php/Skeinforge_Export

==Operation==
The default 'Activate Export' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

==Settings==
===Add Descriptive Extension===
Default is off.

When selected, key profile values will be added as an extension to the gcode file.  For example:
test.04hx06w_03fill_2cx2r_33EL.gcode

would mean:

* . (Carve section.)
* 04h = 'Layer Height (mm):' 0.4
* x
* 06w = 0.6 width i.e. 0.4 times 'Edge Width over Height (ratio):' 1.5
* _ (Fill section.)
* 03fill = 'Infill Solidity (ratio):' 0.3
* _ (Multiply section; if there is one column and one row then this section is not shown.)
* 2c = 'Number of Columns (integer):' 2
* x
* 2r = 'Number of Rows (integer):' 2.
* _ (Speed section.)
* 33EL = 'Feed Rate (mm/s):' 33.0 and 'Flow Rate Setting (float):' 33.0.  If either value has a positive value after the decimal place then this is also shown, but if it is zero it is hidden.  Also, if the values differ (which they shouldn't with 5D volumetrics) then each should be displayed separately.  For example, 35.2E30L = 'Feed Rate (mm/s):' 35.2 and 'Flow Rate Setting (float):' 30.0.

===Add Profile Extension===
Default is off.

When selected, the current profile will be added to the file extension.  For example:
test.my_profile_name.gcode

===Add Timestamp Extension===
Default is off.

When selected, the current date and time is added as an extension in format YYYYmmdd_HHMMSS (so it is sortable if one has many files).  For example:
test.my_profile_name.20110613_220113.gcode

===Also Send Output To===
Default is empty.

Defines the output name for sending to a file or pipe.  A common choice is stdout to print the output in the shell screen.  Another common choice is stderr.  With the empty default, nothing will be done.  If the value is anything else, the output will be written to that file name.

===Analyze Gcode===
Default is on.

When selected, the penultimate gcode will be sent to the analyze plugins to be analyzed and viewed.

===Comment Deletion Choices===
These checkboxes determine which comments, if any, will be removed from the exported gcode. Note they are honoured in the order given, so that for example if Delete <keep> Comments is unchecked (the default), then <keep> comments will not be deleted regardless of the subsequent settings.

====Delete <keep> Comments====
Default is unchecked.

If checked, any preface comments starting with <keep> are removed. The code as shipped does not generate any <keep> comments, but user modifications may add some. They can be useful for recording information like the profile or version used to generate the gcode, if other comments are being deleted.

====Delete Trace Comments====
Default is unchecked.

If checked, any preface comments starting with <trace> are removed. The code as shipped does not generate any <trace> comments, but there is a debugging aid which adds some.

====Delete <layer> Comments====
Default is unchecked.

If checked, any comments starting with <layer> are removed. The Dual Extrusion gcode merge logic requires the <layer> comments to be left in, so this box should normally remain unchecked unless you have a single extruder printer and never use Dual Extrusion.

====Delete Settings Comments====
Default is unchecked.

If checked, the block of comments near the start of the gcode reporting the settings used are removed.

====Delete Other Initialisation Comments====
Default is unchecked.

If checked, the block of comments near the start of the gcode between (<extruderInitialization>) and (</extruderInitialization>), apart from any described above, are removed.

====Delete Other Preface Comments====
Default is unchecked.

If checked, any preface comments [before (<alteration>)] not mentioned above are removed. If Alteration is not enabled, all comments are preface comments.

====Delete Other Crafting Comments====
Default is unchecked.

If checked, all comments between (<crafting>) and (</crafting>), other than those described above, are removed. You will usually not want these unless you are debugging the gcode generator.

====Delete Other Tagged Comments====
Default is False.

If checked, all comments starting with '(<', other than those described above, are removed.

====Delete Other Starred Comments====
Default is False.

If checked, all comments starting with '(*', other than those described above, are removed.

====Delete All Other Comments====
Default is False.

If checked, all comments  other than those described above are removed.

===Export Operations===
Export presents the user with a choice of the export plugins in the export_plugins folder.  The chosen plugin will then modify the gcode or translate it into another format.  There is also the "Do Not Change Output" choice, which will not change the output.  An export plugin is a script in the export_plugins folder which has the getOutput function, the globalIsReplaceable variable and if its output is not replaceable, the writeOutput function.

===File Extension===
Default is gcode.

Defines the file extension added to the name of the output file.  The output file will be named as originalname_export.extension so if you are processing XYZ.stl the output will by default be XYZ_export.gcode
 
===Name of Replace File===
Default is replace.csv.

When export is exporting the code, if there is a tab separated file  with the name of the "Name of Replace File" setting, it will replace the string in the first column by its replacement in the second column.  If there is nothing in the second column, the first column string will be deleted, if this leads to an empty line, the line will be deleted.  If there are replacement columns after the second, they will be added as extra lines of text.  There is an example file replace_example.csv to demonstrate the tab separated format, which can be edited in a text editor or a spreadsheet.

Export looks for the alteration file in the alterations folder in the .skeinforge folder in the home directory.  Export does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_plugins folder.

===Save Penultimate Gcode===
Default is off.

When selected, export will save the gcode file with the suffix '_penultimate.gcode' just before it is exported.  This is useful because the code after it is exported could be in a form which the viewers can not display well.

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
__credits__ = 'Gary Hodgson <http://garyhodgson.com/reprap/2011/06/hacking-skeinforge-export-module/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


def getCraftedTextFromText(gcodeText, repository=None):
	'Export a gcode linear move text.'
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'export'):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository(ExportRepository())
	if not repository.activateExport.value:
		return gcodeText
	return ExportSkein().getCraftedGcode(repository, gcodeText)

def getDescriptionCarve(lines):
	'Get the description for carve.'
	descriptionCarve = ''
	layerThicknessString = getSettingString(lines, 'carve', 'Layer Height')
	if layerThicknessString != None:
		descriptionCarve += layerThicknessString.replace('.', '') + 'h'
	edgeWidthString = getSettingString(lines, 'carve', 'Edge Width over Height')
	if edgeWidthString != None:
		descriptionCarve += 'x%sw' % str(float(edgeWidthString) * float(layerThicknessString)).replace('.', '')
	return descriptionCarve

def getDescriptionFill(lines):
	'Get the description for fill.'
	activateFillString = getSettingString(lines, 'fill', 'Activate Fill')
	if activateFillString == None or activateFillString == 'False':
		return ''
	infillSolidityString = getSettingString(lines, 'fill', 'Infill Solidity')
	return '_' + infillSolidityString.replace('.', '') + 'fill'

def getDescriptionMultiply(lines):
	'Get the description for multiply.'
	activateMultiplyString = getSettingString(lines, 'multiply', 'Activate Multiply')
	if activateMultiplyString == None or activateMultiplyString == 'False':
		return ''
	columnsString = getSettingString(lines, 'multiply', 'Number of Columns')
	rowsString = getSettingString(lines, 'multiply', 'Number of Rows')
	if columnsString == '1' and rowsString == '1':
		return ''
	return '_%scx%sr' % (columnsString, rowsString)

def getDescriptionSpeed(lines):
	'Get the description for speed.'
	activateSpeedString = getSettingString(lines, 'speed', 'Activate Speed')
	if activateSpeedString == None or activateSpeedString == 'False':
		return ''
	feedRateString = getSettingString(lines, 'speed', 'Feed Rate')
	flowRateString = getSettingString(lines, 'speed', 'Flow Rate')
	if feedRateString == flowRateString:
		return '_%sEL' % feedRateString.replace('.0', '')
	return '_%sE%sL' % (feedRateString.replace('.0', ''), flowRateString.replace('.0', ''))

def getDescriptiveExtension(gcodeText):
	'Get the descriptive extension.'
	lines = archive.getTextLines(gcodeText)
	return '.' + getDescriptionCarve(lines) + getDescriptionFill(lines) + getDescriptionMultiply(lines) + getDescriptionSpeed(lines)

def getDistanceGcode(exportText):
	'Get gcode lines with distance variable added, this is for if ever there is distance code.'
	lines = archive.getTextLines(exportText)
	oldLocation = None
	for line in lines:
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		firstWord = None
		if len(splitLine) > 0:
			firstWord = splitLine[0]
		if firstWord == 'G1':
			location = gcodec.getLocationFromSplitLine(oldLocation, splitLine)
			if oldLocation != None:
				distance = location.distance(oldLocation)
			oldLocation = location
	return exportText

def getFirstValue(gcodeText, word):
	'Get the value from the first line which starts with the given word.'
	for line in archive.getTextLines(gcodeText):
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if gcodec.getFirstWord(splitLine) == word:
			return splitLine[1]
	return ''

def getNewRepository():
	'Get new repository.'
	return ExportRepository()

def getReplaceableExportGcode(nameOfReplaceFile, replaceableExportGcode):
	'Get text with strings replaced according to replace.csv file.'
	replaceLines = settings.getAlterationLines(nameOfReplaceFile)
	if len(replaceLines) < 1:
		return replaceableExportGcode
	for replaceLine in replaceLines:
		splitLine = replaceLine.replace('\n', '\t').split('\t')
		if len(splitLine) > 0:
			replaceableExportGcode = replaceableExportGcode.replace(splitLine[0], '\n'.join(splitLine[1 :]))
	output = cStringIO.StringIO()
	gcodec.addLinesToCString(output, archive.getTextLines(replaceableExportGcode))
	return output.getvalue()

def getSelectedPluginModule( plugins ):
	'Get the selected plugin module.'
	for plugin in plugins:
		if plugin.value:
			return archive.getModuleWithDirectoryPath( plugin.directoryPath, plugin.name )
	return None

def getSettingString(lines, procedureName, settingNameStart):
	'Get the setting value from the lines, return None if there is no setting starting with that name.'
	settingNameStart = settingNameStart.replace(' ', '_')
	for line in lines:
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		firstWord = None
		if len(splitLine) > 0:
			firstWord = splitLine[0]
		if firstWord == '(<setting>':
			if len(splitLine) > 4:
				if splitLine[1] == procedureName and splitLine[2].startswith(settingNameStart):
					return splitLine[3]
		elif firstWord == '(</settings>)':
			return None
	return None

def sendOutputTo(outputTo, text):
	'Send output to a file or a standard output.'
	if outputTo.endswith('stderr'):
		sys.stderr.write(text)
		sys.stderr.write('\n')
		sys.stderr.flush()
		return
	if outputTo.endswith('stdout'):
		sys.stdout.write(text)
		sys.stdout.write('\n')
		sys.stdout.flush()
		return
	archive.writeFileText(outputTo, text)

def writeOutput(fileName, shouldAnalyze=True):
	'Export a gcode linear move file.'
	if fileName == '':
		return None
	repository = ExportRepository()
	settings.getReadRepository(repository)
	startTime = time.time()
	summarisedFileName = archive.getSummarizedFileName(fileName)
	print('File ' + summarisedFileName + ' is being chain exported.')
	fileNameSuffix = fileName[: fileName.rfind('.')]
	if repository.addExportSuffix.value:
		fileNameSuffix += '_export'
	gcodeText = gcodec.getGcodeFileText(fileName, '')
	procedures = skeinforge_craft.getProcedures('export', gcodeText)
	gcodeText = skeinforge_craft.getChainTextFromProcedures(fileName, procedures[: -1], gcodeText)
	if gcodeText == '':
		return None
	if repository.addProfileExtension.value:
		fileNameSuffix += '.' + getFirstValue(gcodeText, '(<profileName>')
	if repository.addDescriptiveExtension.value:
		fileNameSuffix += getDescriptiveExtension(gcodeText)
	if repository.addTimestampExtension.value:
		fileNameSuffix += '.' + getFirstValue(gcodeText, '(<timeStampPreface>')
	fileNameSuffix += '.' + repository.fileExtension.value
	fileNamePenultimate = fileName[: fileName.rfind('.')] + '_penultimate.gcode'
	filePenultimateWritten = False
	if repository.savePenultimateGcode.value:
		archive.writeFileText(fileNamePenultimate, gcodeText)
		filePenultimateWritten = True
		print('The penultimate file is saved as ' + archive.getSummarizedFileName(fileNamePenultimate))
	exportGcode = getCraftedTextFromText(gcodeText, repository)
	window = None
	if shouldAnalyze and repository.analyzeGcode.value:
		window = skeinforge_analyze.writeOutput(fileName, fileNamePenultimate, fileNameSuffix, filePenultimateWritten, gcodeText)
	replaceableExportGcode = None
	selectedPluginModule = getSelectedPluginModule(repository.exportPlugins)
	if selectedPluginModule == None:
		replaceableExportGcode = exportGcode
	else:
		if selectedPluginModule.globalIsReplaceable:
			replaceableExportGcode = selectedPluginModule.getOutput(exportGcode)
		else:
			selectedPluginModule.writeOutput(fileNameSuffix, exportGcode)
	if replaceableExportGcode != None:
		replaceableExportGcode = getReplaceableExportGcode(repository.nameOfReplaceFile.value, replaceableExportGcode)
		archive.writeFileText( fileNameSuffix, replaceableExportGcode )
		print('The exported file is saved as ' + archive.getSummarizedFileName(fileNameSuffix))
	if repository.alsoSendOutputTo.value != '':
		if replaceableExportGcode == None:
			replaceableExportGcode = selectedPluginModule.getOutput(exportGcode)
		sendOutputTo(repository.alsoSendOutputTo.value, replaceableExportGcode)
	print('It took %s to export the file.' % euclidean.getDurationString(time.time() - startTime))
	return window


class ExportRepository:
	'A class to handle the export settings.'
	def __init__(self):
		'Set the default settings, execute title & settings fileName.'
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.export.html', self)
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Export', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromDocumentationSubName('skeinforge_application.skeinforge_plugins.craft_plugins.export.html')
		self.activateExport = settings.BooleanSetting().getFromValue('Activate Export', self, True)
		self.addDescriptiveExtension = settings.BooleanSetting().getFromValue('Add Descriptive Extension', self, False)
		self.addExportSuffix = settings.BooleanSetting().getFromValue('Add Export Suffix', self, True)
		self.addProfileExtension = settings.BooleanSetting().getFromValue('Add Profile Extension', self, False)
		self.addTimestampExtension = settings.BooleanSetting().getFromValue('Add Timestamp Extension', self, False)
		self.alsoSendOutputTo = settings.StringSetting().getFromValue('Also Send Output To:', self, '')
		self.analyzeGcode = settings.BooleanSetting().getFromValue('Analyze Gcode', self, True)
		self.commentLabel = settings.LabelDisplay().getFromName('Comment Deletion Choices:', self)
		self.deleteKeepComments = settings.BooleanSetting().getFromValue('Delete <keep> Comments', self, False)
		self.deleteTraceComments = settings.BooleanSetting().getFromValue('Delete Trace Comments', self, False)
		self.deleteLayerComments = settings.BooleanSetting().getFromValue('Delete <layer> Comments', self, False)
		self.deleteSettingsComments = settings.BooleanSetting().getFromValue('Delete Settings Comments', self, False)
		self.deleteInitialisationComments = settings.BooleanSetting().getFromValue('Delete Other Initialisation Comments', self, False)
		self.deletePrefaceComments = settings.BooleanSetting().getFromValue('Delete Other Preface Comments', self, False)
		self.deleteCraftingComments = settings.BooleanSetting().getFromValue('Delete Other Crafting Comments', self, False)
		self.deleteTaggedComments = settings.BooleanSetting().getFromValue('Delete Other Tagged Comments', self, False)
		self.deleteStarredComments = settings.BooleanSetting().getFromValue('Delete Other Starred Comments', self, False)
		self.deleteOtherComments = settings.BooleanSetting().getFromValue('Delete All Other Comments', self, False)
		exportPluginsFolderPath = archive.getAbsoluteFrozenFolderPath(archive.getCraftPluginsDirectoryPath('export.py'), 'export_plugins')
		exportStaticDirectoryPath = os.path.join(exportPluginsFolderPath, 'static_plugins')
		exportPluginFileNames = archive.getPluginFileNamesFromDirectoryPath(exportPluginsFolderPath)
		exportStaticPluginFileNames = archive.getPluginFileNamesFromDirectoryPath(exportStaticDirectoryPath)
		self.exportLabel = settings.LabelDisplay().getFromName('Export Operations: ', self)
		self.exportPlugins = []
		exportLatentStringVar = settings.LatentStringVar()
		self.doNotChangeOutput = settings.RadioCapitalized().getFromRadio(exportLatentStringVar, 'Do Not Change Output', self, True)
		self.doNotChangeOutput.directoryPath = None
		allExportPluginFileNames = exportPluginFileNames + exportStaticPluginFileNames
		for exportPluginFileName in allExportPluginFileNames:
			exportPlugin = None
			if exportPluginFileName in exportPluginFileNames:
				path = os.path.join(exportPluginsFolderPath, exportPluginFileName)
				exportPlugin = settings.RadioCapitalizedButton().getFromPath(exportLatentStringVar, exportPluginFileName, path, self, False)
				exportPlugin.directoryPath = exportPluginsFolderPath
			else:
				exportPlugin = settings.RadioCapitalized().getFromRadio(exportLatentStringVar, exportPluginFileName, self, False)
				exportPlugin.directoryPath = exportStaticDirectoryPath
			self.exportPlugins.append(exportPlugin)
		self.fileExtension = settings.StringSetting().getFromValue('File Extension:', self, 'gcode')
		self.nameOfReplaceFile = settings.StringSetting().getFromValue('Name of Replace File:', self, 'replace.csv')
		self.savePenultimateGcode = settings.BooleanSetting().getFromValue('Save Penultimate Gcode', self, False)
		self.executeTitle = 'Export'

	def execute(self):
		'Export button has been clicked.'
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class ExportSkein:
	'A class to export a skein of extrusions.'
	def __init__(self):
		self.crafting = False
		self.decimalPlacesExported = 2
		self.output = cStringIO.StringIO()

	def addLine(self, line):
		'Add a line of text and a newline to the output.'
		if line != '':
			self.output.write(line + '\n')

	def getCraftedGcode( self, repository, gcodeText ):
		'Parse gcode text and store the export gcode.'
		self.repository = repository
		self.inPreface = True
		self.inInitialisation = False
		self.crafting = False
		lines = archive.getTextLines(gcodeText)
		for line in lines:
			self.parseLine(line)
		return self.output.getvalue()

	def getLineWithTruncatedNumber(self, character, line, splitLine):
		'Get a line with the number after the character truncated.'
		numberString = gcodec.getStringFromCharacterSplitLine(character, splitLine)
		if numberString == None:
			return line
		roundedNumberString = euclidean.getRoundedToPlacesString(self.decimalPlacesExported, float(numberString))
		return gcodec.getLineWithValueString(character, line, splitLine, roundedNumberString)

	def parseLine(self, line):
		'Parse a gcode line.'
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == '(<alteration>)':
			self.inPreface = False
		if firstWord == '(<crafting>)':
			self.crafting = True
			if not self.repository.deleteCraftingComments.value:
				self.addLine(line)
			return
		if firstWord == '(</crafting>)':
			self.crafting = False
			if not self.repository.deleteCraftingComments.value:
				self.addLine(line)
			return
		if firstWord == '(<decimalPlacesCarried>':
			self.decimalPlacesExported = int(splitLine[1]) - 1
		if firstWord == '(<keep>':
			if not self.repository.deleteKeepComments.value:
				self.addLine(line)
			return
		if firstWord == '(<layer>' or firstWord.find('(</layer>') == 0:
			if not self.repository.deleteLayerComments.value:
				self.addLine(line)
			return
		if firstWord == '(<trace>':
			if not self.repository.deleteTraceComments.value:
				self.addLine(line)
			return
		if firstWord == '(<settings>)' or firstWord == '(<setting>' or firstWord == '(</settings>)':
			if not self.repository.deleteSettingsComments.value:
				self.addLine(line)
			return
		if firstWord == '(<extruderInitialization>)':
			self.inInitialisation = True
			if not self.repository.deleteInitialisationComments.value:
				self.addLine(line)
			return
		if firstWord == '(</extruderInitialization>)':
			self.inInitialisation = False
			if not self.repository.deleteInitialisationComments.value:
				self.addLine(gcodec.getTagBracketedProcedure('export'))
				self.addLine(line)
			return
		if firstWord[0] == '(':
			if self.inPreface:
				if self.repository.deletePrefaceComments.value: 
					return
			elif self.inInitialisation:
				if self.repository.deleteInitialisationComments.value:
					return
			elif firstWord[1] == '<':
				if self.crafting:
					if self.repository.deleteCraftingComments.value:
						return
				elif self.repository.deleteTaggedComments.value:
					return
			elif firstWord[1] == '*':
				if self.repository.deleteStarredComments.value:
					return
			elif self.repository.deleteOtherComments.value:
				return
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
	'Display the export dialog.'
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor(getNewRepository())

if __name__ == '__main__':
	main()
