"""
Lash is a script to partially compensate for the backlash of the tool head.

The lash tool is ported from Erik de Bruijn's 3D-to-5D-Gcode php GPL'd script at:
http://objects.reprap.org/wiki/3D-to-5D-Gcode.php

The default 'Activate Lash' checkbox is off.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The 'X Backlash' is the distance the tool head will be lashed in the X direction, the default is 0.2 mm.  The 'Y Backlash' is the distance the tool head will be lashed in the Y direction, the default is 0.2 mm.  These default values are from the settings in Erik's 3D-to-5D-Gcode, I believe the settings are used on his Darwin reprap.

The following examples lash the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and lash.py.


> python lash.py
This brings up the lash dialog.


> python lash.py Screw Holder Bottom.stl
The lash tool is parsing the file:
Screw Holder Bottom.stl
..
The lash tool has created the file:
.. Screw Holder Bottom_lash.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import lash
>>> lash.main()
This brings up the lash dialog.


>>> lash.writeOutput()
The lash tool is parsing the file:
Screw Holder Bottom.stl
..
The lash tool has created the file:
.. Screw Holder Bottom_lash.gcode


"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text, lashRepository = None ):
	"Get a lashed gcode linear move text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), lashRepository )

def getCraftedTextFromText( gcodeText, lashRepository = None ):
	"Get a lashed gcode linear move text from text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'lash' ):
		return gcodeText
	if lashRepository == None:
		lashRepository = preferences.getReadRepository( LashRepository() )
	if not lashRepository.activateLash.value:
		return gcodeText
	return LashSkein().getCraftedGcode( gcodeText, lashRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return LashRepository()

def writeOutput( fileName = '' ):
	"Lash a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'lash' )


class LashRepository:
	"A class to handle the lash preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Lashed', self, '' )
		self.activateLash = preferences.BooleanPreference().getFromValue( 'Activate Lash', self, False )
		self.xBacklash = preferences.FloatPreference().getFromValue( 'X Backlash (mm):', self, 0.2 )
		self.yBacklash = preferences.FloatPreference().getFromValue( 'Y Backlash (mm):', self, 0.3 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Lash'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.lash.html' )

	def execute( self ):
		"Lash button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class LashSkein:
	"A class to lash a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRateMinute = 958.0
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None

	def getCraftedGcode( self, gcodeText, lashRepository ):
		"Parse gcode text and store the lash gcode."
		self.lines = gcodec.getTextLines( gcodeText )
		self.lashRepository = lashRepository
		self.xBacklash = lashRepository.xBacklash.value
		self.yBacklash = lashRepository.yBacklash.value
		self.parseInitialization()
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLash( line )
		return self.distanceFeedRate.output.getvalue()

	def getLashedLine( self, line, location, splitLine ):
		"Get lashed gcode line."
		if self.oldLocation == None:
			return line
		if location.x > self.oldLocation.x:
			line = self.distanceFeedRate.getLineWithX( line, splitLine, location.x + self.xBacklash )
		else:
			line = self.distanceFeedRate.getLineWithX( line, splitLine, location.x - self.xBacklash )
		if location.y > self.oldLocation.y:
			line = self.distanceFeedRate.getLineWithY( line, splitLine, location.y + self.yBacklash )
		else:
			line = self.distanceFeedRate.getLineWithY( line, splitLine, location.y - self.yBacklash )
		return line

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> lash </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )

	def parseLash( self, line ):
		"Parse a gcode line and add it to the lash skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
			line = self.getLashedLine( line, location, splitLine )
			self.oldLocation = location
		self.distanceFeedRate.addLine( line )


def main():
	"Display the lash dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
