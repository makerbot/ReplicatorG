"""
Binary 16 byte is an export plugin to convert gcode into 16 byte binary segments.

An export plugin is a script in the export_plugins folder which has the functions getOuput, isArchivable and writeOutput.  It is
meant to be run from the export tool.  To ensure that the plugin works on platforms which do not handle file capitalization
properly, give the plugin a lower case name.

The getOutput function of this script takes a gcode text and returns that text converted into 16 byte segments.  The writeOutput
function of this script takes a gcode text and writes that in a binary format converted into 16 byte segments.

Many of the functions in this script are copied from gcodec in skeinforge_utilities.  They are copied rather than imported so
developers making new plugins do not have to learn about gcodec, the code here is all they need to learn.

This plugin is just a starter to make a real binary converter.

//Record structure
BinArray(0) = AscW(Inst_Code_Letter)
BinArray(1) = cInst_Code

X Data
sInt32_to_Hbytes(iXdim_1)
BinArray(2) = lsb 'short lsb
BinArray(3) = msb 'short msb

Y Data
sInt32_to_Hbytes(iYdim_2)
BinArray(4) = lsb 'short lsb
BinArray(5) = msb 'short msb

Z Data
sInt32_to_Hbytes(iZdim_3)
BinArray(6) = lsb 'short lsb
BinArray(7) = msb 'short msb

I Data
sInt32_to_Hbytes(iIdim_4)
BinArray(8) = lsb 'short lsb
BinArray(9) = msb 'short msb

J Data
sInt32_to_Hbytes(iJdim_5)
BinArray(10) = lsb 'short lsb
BinArray(11) = msb 'short msb

BinArray(12) = FP_Char
sInt32_to_Hbytes(iFP_Num)
BinArray(13) = lsb 'short lsb

BinArray(14) = bActiveFlags

BinArray(15) = AscW("#")End of record filler

Byte 14 is worth a few extra notes, this byte is used to define which of the axes are active, its used to get round the problem of say a
line of code with no mention of z. This would be put into the file as z = 0 as the space for this data is reserved, if we did nothing, this
would instruct the machine to go to z = 0. If we use the active flag to define the z axis as inactive the z = 0 is ignored and the value
set to the last saved value of z, i.e it does not move.  If the z data is actually set to z = 0 then the axis would be set to active and
the move takes place.
"""

from __future__ import absolute_import
import __init__
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools import polyfile
from struct import Struct
import cStringIO
import os
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"

def getIntegerFromCharacterLengthLineOffset( character, offset, splitLine, stepLength ):
	"Get the integer after the first occurence of the character in the split line."
	lineFromCharacter = getStringFromCharacterSplitLine( character, splitLine )
	if lineFromCharacter == None:
		return 0
	floatValue = ( float( lineFromCharacter ) + offset ) / stepLength
	return int( round( floatValue ) )

def getIntegerFlagFromCharacterSplitLine( character, splitLine ):
	"Get the integer flag after the first occurence of the character in the split line."
	lineFromCharacter = getStringFromCharacterSplitLine( character, splitLine )
	if lineFromCharacter == None:
		return 0
	return 1

def getOutput( gcodeText, binary16BytePreferences = None ):
	"""Get the exported version of a gcode file.  This function, isArchivable and writeOutput are the only necessary functions in a skeinforge export plugin.
	If this plugin writes an output than should not be printed, an empty string should be returned."""
	if gcodeText == '':
		return ''
	if binary16BytePreferences == None:
		binary16BytePreferences = Binary16BytePreferences()
		preferences.readPreferences( binary16BytePreferences )
	skein = Binary16ByteSkein()
	skein.parseGcode( gcodeText, binary16BytePreferences )
	return skein.output.getvalue()

def getStringFromCharacterSplitLine( character, splitLine ):
	"Get the string after the first occurence of the character in the split line."
	indexOfCharacter = indexOfStartingWithSecond( character, splitLine )
	if indexOfCharacter < 0:
		return None
	return splitLine[ indexOfCharacter ][ 1 : ]

def getSummarizedFilename( fileName ):
	"Get the fileName basename if the file is in the current working directory, otherwise return the original full name."
	if os.getcwd() == os.path.dirname( fileName ):
		return os.path.basename( fileName )
	return fileName

def getTextLines( text ):
	"Get the all the lines of text of a text."
	return text.replace( '\r', '\n' ).split( '\n' )

def indexOfStartingWithSecond( letter, splitLine ):
	"Get index of the first occurence of the given letter in the split line, starting with the second word.  Return - 1 if letter is not found"
	for wordIndex in xrange( 1, len( splitLine ) ):
		word = splitLine[ wordIndex ]
		firstLetter = word[ 0 ]
		if firstLetter == letter:
			return wordIndex
	return - 1

def isArchivable():
	"Return whether or not this plugin is archivable."
	return True

def isReplacable():
	"Return whether or not the output from this plugin is replacable.  This should be true if the output is text and false if it is binary."
	return False

def writeFileText( fileName, fileText ):
	"Write a text to a file."
	try:
		file = open( fileName, 'wb' )
		file.write( fileText )
		file.close()
	except IOError:
		print( 'The file ' + fileName + ' can not be written to.' )

def writeOutput( fileName = '', gcodeText = '' ):
	"Write the exported version of a gcode file.  This function, getOutput and isArchivable are the only necessary functions in a skeinforge export plugin."
	if fileName == '':
		unmodified = interpret.getGNUTranslatorFilesUnmodified()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	binary16BytePreferences = Binary16BytePreferences()
	preferences.readPreferences( binary16BytePreferences )
	gcodeText = gcodec.getGcodeFileText( fileName, gcodeText )
	skeinOutput = getOutput( gcodeText, binary16BytePreferences )
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_export.' + binary16BytePreferences.fileExtension.value
	writeFileText( suffixFilename, skeinOutput )
	print( 'The converted file is saved as ' + getSummarizedFilename( suffixFilename ) )


class Binary16BytePreferences:
	"A class to handle the export preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive = []
		self.fileExtension = preferences.StringPreference().getFromValue( 'File Extension:', 'bin' )
		self.archive.append( self.fileExtension )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to be Converted to Binary 16 Byte', '' )
		self.archive.append( self.fileNameInput )
		self.feedrateStepLength = preferences.FloatPreference().getFromValue( 'Feedrate Step Length (millimeters/second)', 0.1 )
		self.archive.append( self.feedrateStepLength )
		self.xStepLength = preferences.FloatPreference().getFromValue( 'X Step Length (millimeters)', 0.1 )
		self.archive.append( self.xStepLength )
		self.yStepLength = preferences.FloatPreference().getFromValue( 'Y Step Length (millimeters)', 0.1 )
		self.archive.append( self.yStepLength )
		self.zStepLength = preferences.FloatPreference().getFromValue( 'Z Step Length (millimeters)', 0.01 )
		self.archive.append( self.zStepLength )
		self.xOffset = preferences.FloatPreference().getFromValue( 'X Offset (millimeters)', 0.0 )
		self.archive.append( self.xOffset )
		self.yOffset = preferences.FloatPreference().getFromValue( 'Y Offset (millimeters)', 0.0 )
		self.archive.append( self.yOffset )
		self.zOffset = preferences.FloatPreference().getFromValue( 'Z Offset (millimeters)', 0.0 )
		self.archive.append( self.zOffset )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Convert to Binary 16 Byte'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.export_plugins.binary_16_byte.html' )

	def execute( self ):
		"Convert to binary 16 byte button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, [ '.gcode' ], self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class Binary16ByteSkein:
	"A class to convert gcode into 16 byte binary segments."
	def __init__( self ):
		self.output = cStringIO.StringIO()

	def parseGcode( self, gcodeText, binary16BytePreferences ):
		"Parse gcode text and store the gcode."
		self.binary16BytePreferences = binary16BytePreferences
		lines = getTextLines( gcodeText )
		for line in lines:
			self.parseLine( line )

	def parseLine( self, line ):
		"Parse a gcode line."
		binary16BytePreferences = self.binary16BytePreferences
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if len( firstWord ) < 1:
			return
		firstLetter = firstWord[ 0 ]
		if firstLetter == '(':
			return
		feedrateInteger = getIntegerFromCharacterLengthLineOffset( 'F', 0.0, splitLine, binary16BytePreferences.feedrateStepLength.value )
		iInteger = getIntegerFromCharacterLengthLineOffset( 'I', 0.0, splitLine, binary16BytePreferences.xStepLength.value )
		jInteger = getIntegerFromCharacterLengthLineOffset( 'J', 0.0, splitLine, binary16BytePreferences.yStepLength.value )
		xInteger = getIntegerFromCharacterLengthLineOffset( 'X', binary16BytePreferences.xOffset.value, splitLine, binary16BytePreferences.xStepLength.value )
		yInteger = getIntegerFromCharacterLengthLineOffset( 'Y', binary16BytePreferences.yOffset.value, splitLine, binary16BytePreferences.yStepLength.value )
		zInteger = getIntegerFromCharacterLengthLineOffset( 'Z', binary16BytePreferences.zOffset.value, splitLine, binary16BytePreferences.zStepLength.value )
		sixteenByteStruct = Struct( 'cBhhhhhhBc' )
#		print( 'xInteger' )
#		print( xInteger )
		flagInteger = getIntegerFlagFromCharacterSplitLine( 'X', splitLine )
		flagInteger += 2 * getIntegerFlagFromCharacterSplitLine( 'Y', splitLine )
		flagInteger += 4 * getIntegerFlagFromCharacterSplitLine( 'Z', splitLine )
		flagInteger += 8 * getIntegerFlagFromCharacterSplitLine( 'I', splitLine )
		flagInteger += 16 * getIntegerFlagFromCharacterSplitLine( 'J', splitLine )
		flagInteger += 32 * getIntegerFlagFromCharacterSplitLine( 'F', splitLine )
		packedString = sixteenByteStruct.pack( firstLetter, int( firstWord[ 1 : ] ), xInteger, yInteger, zInteger, iInteger, jInteger, feedrateInteger, flagInteger, '#' )
		self.output.write( packedString )


def main( hashtable = None ):
	"Display the export dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( Binary16BytePreferences() )

if __name__ == "__main__":
	main()
