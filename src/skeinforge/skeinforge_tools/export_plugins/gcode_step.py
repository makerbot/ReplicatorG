"""
Gcode step is an export plugin to convert gcode from float position to number of steps.

An export plugin is a script in the export_plugins folder which has the functions getOuput, isArchivable and writeOutput.  It is
meant to be run from the export tool.  To ensure that the plugin works on platforms which do not handle file capitalization
properly, give the plugin a lower case name.

If the "Add Feedrate Even When Unchanging" checkbox is true, the feedrate will be added even when it did not change
from the previous line.  If the "Add Space Between Words" checkbox is true, a space will be added between each gcode
word.  If the "Add Z Even When Unchanging" checkbox is true, the z word will be added even when it did not change.  The
defaults for these checkboxes are all true. 

The "Feedrate Step Length" is the length of one feedrate increment.  The "Radius Step Length" is the length of one radius
increment.  The "X Step Length" is the length of one x step.  The "Y Step Length" is the length of one y step.  The "Z Step
Length" is the length of one z step.

The "X Offset " is the distance the x word in a gcode line will be offset.  The "Y Offset " is the distance the y word will be
offset.  The "Z Offset " is the distance the z word will be offset.

The getOutput function of this script takes a gcode text and returns it with the positions converted into number of steps.
The writeOutput function of this script takes a gcode text and writes that with the positions converted into number of steps.
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


def getCharacterIntegerString( character, offset, splitLine, stepLength ):
	"Get a character and integer string."
	floatValue = getFloatFromCharacterSplitLine( character, splitLine )
	if floatValue == None:
		return None
	floatValue += offset
	integerValue = int( round( float( floatValue / stepLength ) ) )
	return character + str( integerValue )

def getFloatFromCharacterSplitLine( character, splitLine ):
	"Get the float after the first occurence of the character in the split line."
	lineFromCharacter = getStringFromCharacterSplitLine( character, splitLine )
	if lineFromCharacter == None:
		return None
	return float( lineFromCharacter )

def getOutput( gcodeText, gcodeStepPreferences = None ):
	"""Get the exported version of a gcode file.  This function, isArchivable and writeOutput are the only necessary functions in a skeinforge export plugin.
	If this plugin writes an output than should not be printed, an empty string should be returned."""
	if gcodeText == '':
		return ''
	if gcodeStepPreferences == None:
		gcodeStepPreferences = GcodeStepPreferences()
		preferences.readPreferences( gcodeStepPreferences )
	skein = GcodeStepSkein()
	skein.parseGcode( gcodeStepPreferences, gcodeText )
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
	return True


class GcodeStepPreferences:
	"A class to handle the export preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive = []
		self.addFeedrateEvenWhenUnchanging = preferences.BooleanPreference().getFromValue( 'Add Feedrate Even When Unchanging', True )
		self.archive.append( self.addFeedrateEvenWhenUnchanging )
		self.addSpaceBetweenWords = preferences.BooleanPreference().getFromValue( 'Add Space Between Words', True )
		self.archive.append( self.addSpaceBetweenWords )
		self.addZEvenWhenUnchanging = preferences.BooleanPreference().getFromValue( 'Add Z Even When Unchanging', True )
		self.archive.append( self.addZEvenWhenUnchanging )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to be Converted to Gcode Step', '' )
		self.archive.append( self.fileNameInput )
		self.feedrateStepLength = preferences.FloatPreference().getFromValue( 'Feedrate Step Length (millimeters/second)', 0.1 )
		self.archive.append( self.feedrateStepLength )
		self.radiusStepLength = preferences.FloatPreference().getFromValue( 'Radius Step Length (millimeters)', 0.1 )
		self.archive.append( self.radiusStepLength )
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
		self.executeTitle = 'Convert to Gcode Step'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.export_plugins.gcode_step.html' )

	def execute( self ):
		"Convert to gcode step button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, [ '.gcode' ], self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class GcodeStepSkein:
	"A class to convert gcode into 16 byte binary segments."
	def __init__( self ):
		self.oldFeedrateString = None
		self.oldZString = None
		self.output = cStringIO.StringIO()

	def addCharacterInteger( self, character, lineStringIO, offset, splitLine, stepLength ):
		"Add a character and integer to line string."
		characterIntegerString = getCharacterIntegerString( character, offset, splitLine, stepLength )
		self.addStringToLine( lineStringIO, characterIntegerString )

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + '\n' )

	def addStringToLine( self, lineStringIO, wordString ):
		"Add a character and integer to line string."
		if wordString == None:
			return
		if self.gcodeStepPreferences.addSpaceBetweenWords.value:
			lineStringIO.write( ' ' )
		lineStringIO.write( wordString )

	def parseGcode( self, gcodeStepPreferences, gcodeText ):
		"Parse gcode text and store the gcode."
		self.gcodeStepPreferences = gcodeStepPreferences
		lines = getTextLines( gcodeText )
		for line in lines:
			self.parseLine( line )

	def parseLine( self, line ):
		"Parse a gcode line."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if len( firstWord ) < 1:
			return
		firstLetter = firstWord[ 0 ]
		if firstLetter == '(':
			return
		if firstWord != 'G1' and firstWord != 'G2' and firstWord != 'G3':
			self.addLine( line )
			return
		lineStringIO = cStringIO.StringIO()
		lineStringIO.write( firstWord )
		self.addCharacterInteger( 'I', lineStringIO, 0.0, splitLine, self.gcodeStepPreferences.xStepLength.value )
		self.addCharacterInteger( 'J', lineStringIO, 0.0, splitLine, self.gcodeStepPreferences.yStepLength.value )
		self.addCharacterInteger( 'R', lineStringIO, 0.0, splitLine, self.gcodeStepPreferences.radiusStepLength.value )
		self.addCharacterInteger( 'X', lineStringIO, self.gcodeStepPreferences.xOffset.value, splitLine, self.gcodeStepPreferences.xStepLength.value )
		self.addCharacterInteger( 'Y', lineStringIO, self.gcodeStepPreferences.yOffset.value, splitLine, self.gcodeStepPreferences.yStepLength.value )
		zString = getCharacterIntegerString( 'Z', self.gcodeStepPreferences.zOffset.value, splitLine, self.gcodeStepPreferences.zStepLength.value )
		feedrateString = getCharacterIntegerString( 'F', 0.0, splitLine, self.gcodeStepPreferences.feedrateStepLength.value )
		if zString != None:
			if zString != self.oldZString or self.gcodeStepPreferences.addZEvenWhenUnchanging.value:
				self.addStringToLine( lineStringIO, zString )
		if feedrateString != None:
			if feedrateString != self.oldFeedrateString or self.gcodeStepPreferences.addFeedrateEvenWhenUnchanging.value:
				self.addStringToLine( lineStringIO, feedrateString )
		self.addLine( lineStringIO.getvalue() )
		self.oldFeedrateString = feedrateString
		self.oldZString = zString


def main( hashtable = None ):
	"Display the export dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( GcodeStepPreferences() )

if __name__ == "__main__":
	main()
