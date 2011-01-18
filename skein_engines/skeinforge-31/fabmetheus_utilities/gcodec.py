"""
Gcodec is a collection of utilities to decode and encode gcode.

To run gcodec, install python 2.x on your machine, which is avaliable from http://www.python.org/download/

Then in the folder which gcodec is in, type 'python' in a shell to run the python interpreter.  Finally type 'from gcodec import *' to import this program.

Below is an example of gcodec use.  This example is run in a terminal in the folder which contains gcodec and Screw Holder Bottom_export.gcode.

>>> from gcodec import *
>>> getFileText('Screw Holder Bottom_export.gcode')
'G90\nG21\nM103\nM105\nM106\nM110 S60.0\nM111 S30.0\nM108 S210.0\nM104 S235.0\nG1 X0.37 Y-4.07 Z1.9 F60.0\nM101\n
..
many lines of text
..

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import cStringIO
import os
import sys
import traceback


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def addLineAndNewlineIfNecessary(line, output):
	'Add the line and if the line does not end with a newline add a newline.'
	output.write(line)
	if len(line) < 1:
		return
	if not line.endswith('\n'):
		output.write('\n')

def addXMLLine(line, xmlLines):
	'Get the all the xml lines of a text.'
	strippedLine = line.strip()
	if strippedLine[ : len('<!--') ] == '<!--':
		endIndex = line.find('-->')
		if endIndex != - 1:
			endIndex += len('-->')
			commentLine = line[: endIndex]
			remainderLine = line[endIndex :].strip()
			if len(remainderLine) > 0:
				xmlLines.append(commentLine)
				xmlLines.append(remainderLine)
				return
	xmlLines.append(line)

def createInitFile():
	'Create the __init__.py file.'
	fileText = '__all__ = ' + str(getPythonFileNamesExceptInit())
	writeFileText('__init__.py', fileText)

def getAbsoluteFolderPath(filePath, folderName=''):
	'Get the absolute folder path.'
	absoluteFolderPath = os.path.dirname(os.path.abspath(filePath))
	if folderName == '':
		return absoluteFolderPath
	return os.path.join(absoluteFolderPath, folderName)

def getDoubleAfterFirstLetter(word):
	'Get the double value of the word after the first letter.'
	return float(word[1 :])

def getDoubleForLetter(letter, splitLine):
	'Get the double value of the word after the first occurence of the letter in the split line.'
	return getDoubleAfterFirstLetter(splitLine[indexOfStartingWithSecond(letter, splitLine)])

def getDoubleFromCharacterSplitLine(character, splitLine):
	'Get the double value of the string after the first occurence of the character in the split line.'
	indexOfCharacter = indexOfStartingWithSecond(character, splitLine)
	if indexOfCharacter < 0:
		return None
	floatString = splitLine[indexOfCharacter][1 :]
	try:
		return float(floatString)
	except ValueError:
		return None

def getDoubleFromCharacterSplitLineValue(character, splitLine, value):
	'Get the double value of the string after the first occurence of the character in the split line, if it does not exist return the value.'
	splitLineFloat = getDoubleFromCharacterSplitLine(character, splitLine)
	if splitLineFloat == None:
		return value
	return splitLineFloat

def getFeedRateMinute(feedRateMinute, splitLine):
	'Get the feed rate per minute if the split line has a feed rate.'
	indexOfF = indexOfStartingWithSecond('F', splitLine)
	if indexOfF > 0:
		return getDoubleAfterFirstLetter( splitLine[indexOfF] )
	return feedRateMinute

def getFilePathWithUnderscoredBasename(fileName, suffix):
	'Get the file path with all spaces in the basename replaced with underscores.'
	suffixFileName = getUntilDot(fileName) + suffix
	suffixDirectoryName = os.path.dirname(suffixFileName)
	suffixReplacedBaseName = os.path.basename(suffixFileName).replace(' ', '_')
	return os.path.join(suffixDirectoryName, suffixReplacedBaseName)

def getFilesWithFileTypesWithoutWords(fileTypes, words = [], fileInDirectory=''):
	'Get files which have a given file type, but with do not contain a word in a list.'
	filesWithFileTypes = []
	for fileType in fileTypes:
		filesWithFileTypes += getFilesWithFileTypeWithoutWords(fileType, words, fileInDirectory)
	filesWithFileTypes.sort()
	return filesWithFileTypes

def getFilesWithFileTypeWithoutWords(fileType, words = [], fileInDirectory=''):
	'Get files which have a given file type, but with do not contain a word in a list.'
	filesWithFileType = []
	directoryName = os.getcwd()
	if fileInDirectory != '':
		directoryName = os.path.dirname(fileInDirectory)
	directory = os.listdir(directoryName)
	for fileName in directory:
		joinedFileName = fileName
		if fileInDirectory != '':
			joinedFileName = os.path.join(directoryName, fileName)
		if isFileWithFileTypeWithoutWords(fileType, joinedFileName, words):
			filesWithFileType.append(joinedFileName)
	filesWithFileType.sort()
	return filesWithFileType

def getFileText(fileName, readMode = 'r', printWarning=True):
	'Get the entire text of a file.'
	try:
		file = open(fileName, readMode)
		fileText = file.read()
		file.close()
		return fileText
	except IOError:
		if printWarning:
			print('The file ' + fileName + ' does not exist.')
		return ''

def getFileTextInFileDirectory(fileInDirectory, fileName, readMode='r'):
	'Get the entire text of a file in the directory of the file in directory.'
	absoluteFilePathInFileDirectory = os.path.join(os.path.dirname(fileInDirectory), fileName)
	return getFileText(absoluteFilePathInFileDirectory, readMode)

def getFirstWord(splitLine):
	'Get the first word of a split line.'
	if len(splitLine) > 0:
		return splitLine[0]
	return ''

def getFirstWordFromLine(line):
	'Get the first word of a line.'
	return getFirstWord(line.split())

def getGcodeFileText(fileName, gcodeText):
	'Get the gcode text from a file if it the gcode text is empty and if the file is a gcode file.'
	if gcodeText != '':
		return gcodeText
	if fileName.endswith('.gcode'):
		return getFileText(fileName)
	return ''

def getLocationFromSplitLine(oldLocation, splitLine):
	'Get the location from the split line.'
	if oldLocation == None:
		oldLocation = Vector3()
	return Vector3(
		getDoubleFromCharacterSplitLineValue('X', splitLine, oldLocation.x),
		getDoubleFromCharacterSplitLineValue('Y', splitLine, oldLocation.y),
		getDoubleFromCharacterSplitLineValue('Z', splitLine, oldLocation.z))

def getModuleWithDirectoryPath(directoryPath, fileName):
	'Get the module from the fileName and folder name.'
	if fileName == '':
		print('The file name in getModule in gcodec was empty.')
		return None
	originalSystemPath = sys.path[:]
	try:
		sys.path.insert(0, directoryPath)
		folderPluginsModule = __import__(fileName)
		sys.path = originalSystemPath
		return folderPluginsModule
	except:
		sys.path = originalSystemPath
		print('')
		print('Exception traceback in getModuleWithDirectoryPath in gcodec:')
		traceback.print_exc(file=sys.stdout)
		print('')
		print('That error means; could not import a module with the fileName ' + fileName)
		print('and an absolute directory name of ' + directoryPath)
		print('')
	return None

def getModuleWithPath(path):
	'Get the module from the path.'
	return getModuleWithDirectoryPath(os.path.dirname(path), os.path.basename(path))

def getPluginFileNamesFromDirectoryPath(directoryPath):
	'Get the file names of the python plugins in the directory path.'
	fileInDirectory = os.path.join(directoryPath, '__init__.py')
	fullPluginFileNames = getPythonFileNamesExceptInit(fileInDirectory)
	pluginFileNames = []
	for fullPluginFileName in fullPluginFileNames:
		pluginBasename = os.path.basename(fullPluginFileName)
		pluginBasename = getUntilDot(pluginBasename)
		pluginFileNames.append(pluginBasename)
	return pluginFileNames

def getPythonDirectoryNames(directoryName):
	'Get the python directories.'
	pythonDirectoryNames = []
	directory = os.listdir(directoryName)
	for fileName in directory:
		subdirectoryName = os.path.join(directoryName, fileName)
		if os.path.isdir(subdirectoryName):
			if os.path.isfile(os.path.join(subdirectoryName, '__init__.py')):
				pythonDirectoryNames.append(subdirectoryName)
	return pythonDirectoryNames

def getPythonDirectoryNamesRecursively(directoryName=''):
	'Get the python directories recursively.'
	recursivePythonDirectoryNames = []
	if directoryName == '':
		directoryName = os.getcwd()
	if os.path.isfile(os.path.join(directoryName, '__init__.py')):
		recursivePythonDirectoryNames.append(directoryName)
		pythonDirectoryNames = getPythonDirectoryNames(directoryName)
		for pythonDirectoryName in pythonDirectoryNames:
			recursivePythonDirectoryNames += getPythonDirectoryNamesRecursively(pythonDirectoryName)
	else:
		return []
	return recursivePythonDirectoryNames

def getPythonFileNamesExceptInit(fileInDirectory=''):
	'Get the python fileNames of the directory which the fileInDirectory is in, except for the __init__.py file.'
	pythonFileNamesExceptInit = getFilesWithFileTypeWithoutWords('py', ['__init__.py'], fileInDirectory)
	pythonFileNamesExceptInit.sort()
	return pythonFileNamesExceptInit

def getPythonFileNamesExceptInitRecursively(directoryName=''):
	'Get the python fileNames of the directory recursively, except for the __init__.py files.'
	pythonDirectoryNames = getPythonDirectoryNamesRecursively(directoryName)
	pythonFileNamesExceptInitRecursively = []
	for pythonDirectoryName in pythonDirectoryNames:
		pythonFileNamesExceptInitRecursively += getPythonFileNamesExceptInit(os.path.join(pythonDirectoryName, '__init__.py'))
	pythonFileNamesExceptInitRecursively.sort()
	return pythonFileNamesExceptInitRecursively

def getSplitLineBeforeBracketSemicolon(line):
	'Get the split line before a bracket or semicolon.'
	semicolonIndex = line.find(';')
	if semicolonIndex >= 0:
		line = line[ : semicolonIndex ]
	bracketIndex = line.find('(')
	if bracketIndex > 0:
		return line[: bracketIndex].split()
	return line.split()

def getStartsWithByList(word, wordPrefixes):
	'Determine if the word starts with a prefix in a list.'
	for wordPrefix in wordPrefixes:
		if word.startswith(wordPrefix):
			return True
	return False

def getStringFromCharacterSplitLine(character, splitLine):
	'Get the string after the first occurence of the character in the split line.'
	indexOfCharacter = indexOfStartingWithSecond(character, splitLine)
	if indexOfCharacter < 0:
		return None
	return splitLine[indexOfCharacter][1 :]

def getSummarizedFileName(fileName):
	'Get the fileName basename if the file is in the current working directory, otherwise return the original full name.'
	if os.getcwd() == os.path.dirname(fileName):
		return os.path.basename(fileName)
	return fileName

def getTextIfEmpty(fileName, text):
	'Get the text from a file if it the text is empty.'
	if text != '':
		return text
	return getFileText(fileName)

def getTextLines(text):
	'Get the all the lines of text of a text.'
	return text.replace('\r', '\n').replace('\n\n', '\n').split('\n')

def getUnmodifiedGCodeFiles(fileInDirectory=''):
	'Get gcode files which are not modified.'
	#transform may be needed in future but probably won't
	words = ' carve clip comb comment cool fill fillet hop inset oozebane raft stretch tower wipe'.replace(' ', ' _').split()
	return getFilesWithFileTypeWithoutWords('gcode', words, fileInDirectory)

def getUntilDot(text):
	'Get the text until the last dot, if any.'
	dotIndex = text.rfind('.')
	if dotIndex < 0:
		return text
	return text[: dotIndex]

def getVersionFileName():
	'Get the file name of the version date.'
	return os.path.join(os.path.dirname(os.path.abspath(__file__)), 'version.txt')

def getWithoutBracketsEqualTab(line):
	'Get a string without the greater than sign, the bracket and less than sign, the equal sign or the tab.'
	line = line.replace('=', ' ')
	line = line.replace('(<', '')
	line = line.replace('>', '')
	return line.replace('\t', '')

def getXMLTagSplitLines(combinedLine):
	'Get the xml lines split at a tag.'
	characterIndex = 0
	lastWord = None
	splitIndexes = []
	tagEnd = False
	while characterIndex < len(combinedLine):
		character = combinedLine[characterIndex]
		if character == '"' or character == "'":
			lastWord = character
		elif combinedLine[characterIndex : characterIndex + len('<!--')] == '<!--':
			lastWord = '-->'
		elif combinedLine[characterIndex : characterIndex + len('<![CDATA[')] == '<![CDATA[':
			lastWord = ']]>'
		if lastWord != None:
			characterIndex = combinedLine.find(lastWord, characterIndex + 1)
			if characterIndex == -1:
				return [combinedLine]
			character = None
			lastWord = None
		if character == '>':
			tagEnd = True
		elif character == '<':
			if tagEnd:
				if combinedLine[characterIndex : characterIndex + 2] != '</':
					splitIndexes.append(characterIndex)
		characterIndex += 1
	if len(splitIndexes) < 1:
		return [combinedLine]
	xmlTagSplitLines = []
	lastSplitIndex = 0
	for splitIndex in splitIndexes:
		xmlTagSplitLines.append(combinedLine[lastSplitIndex : splitIndex])
		lastSplitIndex = splitIndex
	xmlTagSplitLines.append(combinedLine[lastSplitIndex :])
	return xmlTagSplitLines

def getXMLLines(text):
	'Get the all the xml lines of a text.'
	accumulatedOutput = None
	textLines = getTextLines(text)
	combinedLines = []
	lastWord = '>'
	for textLine in textLines:
		strippedLine = textLine.strip()
		firstCharacter = None
		lastCharacter = None
		if len( strippedLine ) > 1:
			firstCharacter = strippedLine[0]
			lastCharacter = strippedLine[-1]
		if firstCharacter == '<' and lastCharacter != '>' and accumulatedOutput == None:
			accumulatedOutput = cStringIO.StringIO()
			accumulatedOutput.write( textLine )
			if strippedLine[ : len('<!--') ] == '<!--':
				lastWord = '-->'
		else:
			if accumulatedOutput == None:
				addXMLLine( textLine, combinedLines )
			else:
				accumulatedOutput.write('\n' + textLine )
				if strippedLine[ - len( lastWord ) : ] == lastWord:
					addXMLLine( accumulatedOutput.getvalue(), combinedLines )
					accumulatedOutput = None
					lastWord = '>'
	xmlLines = []
	for combinedLine in combinedLines:
		xmlLines += getXMLTagSplitLines(combinedLine)
	return xmlLines

def indexOfStartingWithSecond(letter, splitLine):
	'Get index of the first occurence of the given letter in the split line, starting with the second word.  Return - 1 if letter is not found'
	for wordIndex in xrange( 1, len(splitLine) ):
		word = splitLine[ wordIndex ]
		firstLetter = word[0]
		if firstLetter == letter:
			return wordIndex
	return - 1

def isFileWithFileTypeWithoutWords(fileType, fileName, words):
	'Determine if file has a given file type, but with does not contain a word in a list.'
	fileName = os.path.basename(fileName)
	fileTypeDot = '.' + fileType
	if not fileName.endswith(fileTypeDot):
		return False
	for word in words:
		if fileName.find(word) >= 0:
			return False
	return True

def isProcedureDone(gcodeText, procedure):
	'Determine if the procedure has been done on the gcode text.'
	if gcodeText == '':
		return False
	lines = getTextLines(gcodeText)
	for line in lines:
		withoutBracketsEqualTabQuotes = getWithoutBracketsEqualTab(line).replace('"', '').replace("'", '')
		splitLine = getWithoutBracketsEqualTab( withoutBracketsEqualTabQuotes ).split()
		firstWord = getFirstWord(splitLine)
		if firstWord == 'procedureDone':
			if splitLine[1].find(procedure) != -1:
				return True
		elif firstWord == 'extrusionStart':
			return False
		procedureIndex = line.find(procedure)
		if procedureIndex != -1:
			if 'procedureDone' in splitLine:
				nextIndex = splitLine.index('procedureDone') + 1
				if nextIndex < len(splitLine):
					if splitLine[nextIndex] == procedure:
						return True
	return False

def isProcedureDoneOrFileIsEmpty(gcodeText, procedure):
	'Determine if the procedure has been done on the gcode text or the file is empty.'
	if gcodeText == '':
		return True
	return isProcedureDone(gcodeText, procedure)

def isThereAFirstWord(firstWord, lines, startIndex):
	'Parse gcode until the first word if there is one.'
	for lineIndex in xrange(startIndex, len(lines)):
		line = lines[lineIndex]
		splitLine = getSplitLineBeforeBracketSemicolon(line)
		if firstWord == getFirstWord(splitLine):
			return True
	return False

def makeDirectory(directory):
	'Make a directory if it does not already exist.'
	if os.path.isdir(directory):
		return
	try:
		os.makedirs(directory)
	except OSError:
		print('Skeinforge can not make the directory %s so give it read/write permission for that directory and the containing directory.' % directory)

def writeFileMessageEnd(end, fileName, fileText, message):
	'Write to a fileName with a suffix and print a message.'
	suffixFileName = getUntilDot(fileName) + end
	writeFileText(suffixFileName, fileText)
	print( message + getSummarizedFileName(suffixFileName) )

def writeFileText(fileName, fileText, writeMode='w+'):
	'Write a text to a file.'
	try:
		file = open(fileName, writeMode)
		file.write(fileText)
		file.close()
	except IOError:
		print('The file ' + fileName + ' can not be written to.')


class BoundingRectangle:
	'A class to get the corners of a gcode text.'
	def getFromGcodeLines(self, lines, radius):
		'Parse gcode text and get the minimum and maximum corners.'
		self.cornerMaximum = complex(-999999999.0, -999999999.0)
		self.cornerMinimum = complex(999999999.0, 999999999.0)
		self.oldLocation = None
		self.cornerRadius = complex(radius, radius)
		for line in lines:
			self.parseCorner(line)
		return self

	def isPointInside(self, point):
		'Determine if the point is inside the bounding rectangle.'
		return point.imag >= self.cornerMinimum.imag and point.imag <= self.cornerMaximum.imag and point.real >= self.cornerMinimum.real and point.real <= self.cornerMaximum.real

	def parseCorner(self, line):
		'Parse a gcode line and use the location to update the bounding corners.'
		splitLine = getSplitLineBeforeBracketSemicolon(line)
		firstWord = getFirstWord(splitLine)
		if firstWord == '(<boundaryPoint>':
			locationComplex = getLocationFromSplitLine(None, splitLine).dropAxis(2)
			self.cornerMaximum = euclidean.getMaximum(self.cornerMaximum, locationComplex)
			self.cornerMinimum = euclidean.getMinimum(self.cornerMinimum, locationComplex)
		elif firstWord == 'G1':
			location = getLocationFromSplitLine(self.oldLocation, splitLine)
			locationComplex = location.dropAxis(2)
			self.cornerMaximum = euclidean.getMaximum(self.cornerMaximum, locationComplex + self.cornerRadius)
			self.cornerMinimum = euclidean.getMinimum(self.cornerMinimum, locationComplex - self.cornerRadius)
			self.oldLocation = location


class DistanceFeedRate:
	'A class to limit the z feed rate and round values.'
	def __init__(self):
		self.absoluteDistanceMode = True
		self.decimalPlacesCarried = 3
		self.extrusionDistanceFormat = ''
		self.maximumZDrillFeedRatePerSecond = None
		self.maximumZFeedRatePerSecond = None
		self.maximumZTravelFeedRatePerSecond = None
		self.oldAddedLocation = None
		self.output = cStringIO.StringIO()

	def addGcodeFromFeedRateThreadZ(self, feedRateMinute, thread, z):
		'Add a thread to the output.'
		if len(thread) > 0:
			self.addGcodeMovementZWithFeedRate(feedRateMinute, thread[0], z)
		else:
			print('zero length vertex positions array which was skipped over, this should never happen.')
		if len(thread) < 2:
			print('thread of only one point in addGcodeFromFeedRateThreadZ in gcodec, this should never happen.')
			print(thread)
			return
		self.addLine('M101') # Turn extruder on.
		for point in thread[1 :]:
			self.addGcodeMovementZWithFeedRate(feedRateMinute, point, z)
		self.addLine('M103') # Turn extruder off.

	def addGcodeFromLoop(self, loop, z):
		'Add the gcode loop.'
		euclidean.addSurroundingLoopBeginning(self, loop, z)
		self.addPerimeterBlock(loop, z)
		self.addLine('(</boundaryPerimeter>)')
		self.addLine('(</surroundingLoop>)')

	def addGcodeFromThreadZ(self, thread, z):
		'Add a thread to the output.'
		if len(thread) > 0:
			self.addGcodeMovementZ(thread[0], z)
		else:
			print('zero length vertex positions array which was skipped over, this should never happen.')
		if len(thread) < 2:
			print('thread of only one point in addGcodeFromThreadZ in gcodec, this should never happen.')
			print(thread)
			return
		self.addLine('M101') # Turn extruder on.
		for point in thread[1 :]:
			self.addGcodeMovementZ(point, z)
		self.addLine('M103') # Turn extruder off.

	def addGcodeMovementZ(self, point, z):
		'Add a movement to the output.'
		self.addLine(self.getLinearGcodeMovement(point, z))

	def addGcodeMovementZWithFeedRate(self, feedRateMinute, point, z):
		'Add a movement to the output.'
		self.addLine(self.getLinearGcodeMovementWithFeedRate(feedRateMinute, point, z))

	def addLine(self, line):
		'Add a line of text and a newline to the output.'
		splitLine = getSplitLineBeforeBracketSemicolon(line)
		firstWord = getFirstWord(splitLine)
		if firstWord == 'G90':
			self.absoluteDistanceMode = True
		elif firstWord == 'G91':
			self.absoluteDistanceMode = False
		elif firstWord == 'G1':
			feedRateMinute = getFeedRateMinute(None, splitLine)
			if self.absoluteDistanceMode:
				location = getLocationFromSplitLine(self.oldAddedLocation, splitLine)
				line = self.getLineWithZLimitedFeedRate(feedRateMinute, line, location, splitLine)
				self.oldAddedLocation = location
			else:
				if self.oldAddedLocation == None:
					print('Warning: There was no absolute location when the G91 command was parsed, so the absolute location will be set to the origin.')
					self.oldAddedLocation = Vector3()
				self.oldAddedLocation += getLocationFromSplitLine(None, splitLine)
		elif firstWord == 'G92':
			self.oldAddedLocation = getLocationFromSplitLine(self.oldAddedLocation, splitLine)
		elif firstWord == 'M101':
			self.maximumZFeedRatePerSecond = self.maximumZDrillFeedRatePerSecond
		elif firstWord == 'M103':
			self.maximumZFeedRatePerSecond = self.maximumZTravelFeedRatePerSecond
		if len(line) > 0:
			self.output.write(line + '\n')

	def addLines(self, lines):
		'Add lines of text to the output.'
		for line in lines:
			self.addLine(line)

	def addLinesSetAbsoluteDistanceMode(self, lines):
		'Add lines of text to the output.'
		self.addLines(lines)
		self.absoluteDistanceMode = True

	def addParameter(self, firstWord, parameter):
		'Add the parameter.'
		self.addLine(firstWord + ' S' + euclidean.getRoundedToThreePlaces(parameter))

	def addPerimeterBlock(self, loop, z):
		'Add the perimeter gcode block for the loop.'
		if len(loop) < 2:
			return
		if euclidean.isWiddershins(loop): # Indicate that a perimeter is beginning.
			self.addLine('(<perimeter> outer )')
		else:
			self.addLine('(<perimeter> inner )')
		self.addGcodeFromThreadZ(loop + [loop[0]], z)
		self.addLine('(</perimeter>)') # Indicate that a perimeter is beginning.

	def addTagBracketedLine(self, tagName, value):
		'Add a begin tag, balue and end tag.'
		self.addLine(self.getTagBracketedLine(tagName, value))

	def getBoundaryLine(self, location):
		'Get boundary gcode line.'
		return '(<boundaryPoint> X%s Y%s Z%s </boundaryPoint>)' % (self.getRounded(location.x), self.getRounded(location.y), self.getRounded(location.z))

	def getFirstWordMovement(self, firstWord, location):
		'Get the start of the arc line.'
		return '%s X%s Y%s Z%s' % (firstWord, self.getRounded(location.x), self.getRounded(location.y), self.getRounded(location.z))

	def getLinearGcodeMovement(self, point, z):
		'Get a linear gcode movement.'
		return 'G1 X%s Y%s Z%s' % ( self.getRounded( point.real ), self.getRounded( point.imag ), self.getRounded(z) )

	def getLinearGcodeMovementWithFeedRate(self, feedRateMinute, point, z):
		'Get a z limited gcode movement.'
		addedLocation = Vector3(point.real, point.imag, z)
		if addedLocation == self.oldAddedLocation:
			return ''
		linearGcodeMovement = self.getLinearGcodeMovement(point, z)
		if feedRateMinute == None:
			return linearGcodeMovement
		return linearGcodeMovement + ' F' + self.getRounded(feedRateMinute)

	def getLinearGcodeMovementWithZLimitedFeedRate(self, feedRateMinute, location):
		'Get a z limited gcode movement.'
		if location == self.oldAddedLocation:
			return ''
		distance = 0.0
		extrusionDistanceString = ''
		if self.oldAddedLocation != None:
			distance = abs(location - self.oldAddedLocation)
		linearGcodeMovement = self.getLinearGcodeMovement(location.dropAxis(2), location.z)
		if feedRateMinute == None:
			return linearGcodeMovement
		if self.oldAddedLocation != None:
			deltaZ = abs(location.z - self.oldAddedLocation.z)
			feedRateMinute = self.getZLimitedFeedRate(deltaZ, distance, feedRateMinute)
		return linearGcodeMovement + ' F' + self.getRounded(feedRateMinute)

	def getLineWithFeedRate(self, feedRateMinute, line, splitLine):
		'Get the line with a feed rate.'
		roundedFeedRateString = 'F' + self.getRounded(feedRateMinute)
		indexOfF = indexOfStartingWithSecond('F', splitLine)
		if indexOfF < 0:
			return line + ' ' + roundedFeedRateString
		word = splitLine[indexOfF]
		return line.replace(word, roundedFeedRateString)

	def getLineWithX(self, line, splitLine, x):
		'Get the line with an x.'
		roundedXString = 'X' + self.getRounded(x)
		indexOfX = indexOfStartingWithSecond('X', splitLine)
		if indexOfX == - 1:
			return line + ' ' + roundedXString
		word = splitLine[indexOfX]
		return line.replace(word, roundedXString)

	def getLineWithY(self, line, splitLine, y):
		'Get the line with a y.'
		roundedYString = 'Y' + self.getRounded(y)
		indexOfY = indexOfStartingWithSecond('Y', splitLine)
		if indexOfY == - 1:
			return line + ' ' + roundedYString
		word = splitLine[indexOfY]
		return line.replace(word, roundedYString)

	def getLineWithZ(self, line, splitLine, z):
		'Get the line with a z.'
		roundedZString = 'Z' + self.getRounded(z)
		indexOfZ = indexOfStartingWithSecond('Z', splitLine)
		if indexOfZ == - 1:
			return line + ' ' + roundedZString
		word = splitLine[indexOfZ]
		return line.replace(word, roundedZString)

	def getLineWithZLimitedFeedRate(self, feedRateMinute, line, location, splitLine):
		'Get a replaced limited gcode movement line.'
		if location == self.oldAddedLocation:
			return ''
		if feedRateMinute == None:
			return line
		if self.oldAddedLocation != None:
			deltaZ = abs(location.z - self.oldAddedLocation.z)
			distance = abs(location - self.oldAddedLocation)
			feedRateMinute = self.getZLimitedFeedRate(deltaZ, distance, feedRateMinute)
		return self.getLineWithFeedRate(feedRateMinute, line, splitLine)

	def getRounded(self, number):
		'Get number rounded to the number of carried decimal places as a string.'
		return euclidean.getRoundedToDecimalPlacesString(self.decimalPlacesCarried, number)

	def getTagBracketedLine(self, tagName, value):
		'Add a begin tag, balue and end tag.'
		return '(<%s> %s </%s>)' % (tagName, value, tagName)

	def getZLimitedFeedRate(self, deltaZ, distance, feedRateMinute):
		'Get the z limited feed rate.'
		if self.maximumZFeedRatePerSecond == None:
			return feedRateMinute
		zFeedRateSecond = feedRateMinute * deltaZ / distance / 60.0
		if zFeedRateSecond > self.maximumZFeedRatePerSecond:
			feedRateMinute *= self.maximumZFeedRatePerSecond / zFeedRateSecond
		return feedRateMinute

	def parseSplitLine(self, firstWord, splitLine):
		'Parse gcode split line and store the parameters.'
		firstWord = getWithoutBracketsEqualTab(firstWord)
		if firstWord == 'decimalPlacesCarried':
			self.decimalPlacesCarried = int(splitLine[1])
		elif firstWord == 'maximumZDrillFeedRatePerSecond':
			self.maximumZDrillFeedRatePerSecond = float(splitLine[1])
			self.maximumZFeedRatePerSecond = self.maximumZDrillFeedRatePerSecond
		elif firstWord == 'maximumZTravelFeedRatePerSecond':
			self.maximumZTravelFeedRatePerSecond = float(splitLine[1])
