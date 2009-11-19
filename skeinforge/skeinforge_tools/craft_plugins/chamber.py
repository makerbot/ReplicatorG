"""
Chamber is a script to set the chamber and bed temperature.

The default 'Activate Chamber' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The 'Temperature of Bed' preference sets the temperature of the bed by sending an M109 command, the default is 60.0.  The 'Temperature of Chamber' preference sets the temperature of the chamber by sending an M109 command, the default is 30.0.

Kulitorum has made a heated bed.  It is a 5mm Alu sheet with a pattern laid out in kapton tape.  The wire is a 0.6mm2 Konstantin wire and it's held in place by small pieces of kapton tape.  The description and picture is at:
http://gallery.kulitorum.com/main.php?g2_itemId=283

The following examples chamber the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and chamber.py.


> python chamber.py
This brings up the chamber dialog.


> python chamber.py Screw Holder Bottom.stl
The chamber tool is parsing the file:
Screw Holder Bottom.stl
..
The chamber tool has created the file:
Screw Holder Bottom_chamber.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import chamber
>>> chamber.main()
This brings up the chamber dialog.


>>> chamber.writeOutput( 'Screw Holder Bottom.stl' )
Screw Holder Bottom.stl
The chamber tool is parsing the file:
Screw Holder Bottom.stl
..
The chamber tool has created the file:
Screw Holder Bottom_chamber.gcode

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.meta_plugins import polyfile
from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', chamberRepository = None ):
	"Chamber the file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), chamberRepository )

def getCraftedTextFromText( gcodeText, chamberRepository = None ):
	"Chamber a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'chamber' ):
		return gcodeText
	if chamberRepository == None:
		chamberRepository = preferences.getReadRepository( ChamberRepository() )
	if not chamberRepository.activateChamber.value:
		return gcodeText
	return ChamberSkein().getCraftedGcode( gcodeText, chamberRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return ChamberRepository()

def writeOutput( fileName = '' ):
	"Chamber a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName == '':
		return
	consecution.writeChainTextWithNounMessage( fileName, 'chamber' )


class ChamberRepository:
	"A class to handle the chamber preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Chamber', self, '' )
		self.activateChamber = preferences.BooleanPreference().getFromValue( 'Activate Chamber:', self, True )
		self.temperatureBed = preferences.FloatPreference().getFromValue( 'Temperature of Bed (Celcius):', self, 60.0 )
		self.temperatureChamber = preferences.FloatPreference().getFromValue( 'Temperature of Chamber (Celcius):', self, 30.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Chamber'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.chamber.html' )

	def execute( self ):
		"Chamber button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )



class ChamberSkein:
	"A class to chamber a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0
		self.lines = None

	def addParameter( self, firstWord, parameter ):
		"Add the parameter if it is at least minus three hundred."
		self.distanceFeedRate.addLine( firstWord + ' S' + euclidean.getRoundedToThreePlaces( parameter ) ) # Set bed temperature.

	def getCraftedGcode( self, gcodeText, chamberRepository ):
		"Parse gcode text and store the chamber gcode."
		self.chamberRepository = chamberRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> chamber </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the chamber skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<extrusion>)':
			self.distanceFeedRate.addLine( line )
			self.addParameter( 'M109', self.chamberRepository.temperatureBed.value ) # Set bed temperature.
			self.addParameter( 'M110', self.chamberRepository.temperatureChamber.value ) # Set chamber temperature.
			return
		self.distanceFeedRate.addLine( line )


def main():
	"Display the chamber dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
