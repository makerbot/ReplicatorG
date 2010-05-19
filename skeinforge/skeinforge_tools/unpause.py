"""
Unpause is a script to speed up a line segment to compensate for the delay of the microprocessor.

The default 'Activate Unpause' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions
will not be called.

The unpause script is based on the Shane Hathaway's patch to speed up a line segment to compensate for the delay of the
microprocessor.  The description is at:
http://shane.willowrise.com/archives/delay-compensation-in-firmware/

The "Delay (milliseconds)" preference is the delay on the microprocessor that will be at least partially compensated for.  The
default is 28 milliseconds, which Shane found for the Arduino.  The "Maximum Speed" ratio is the maximum amount that the
feedrate will be sped up to, compared to the original feedrate, the default is 1.5.

To run unpause, in a shell type:
> python unpause.py

The following examples unpause the files Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains
Screw Holder Bottom.stl & unpause.py.  The function writeOutput checks to see if the text has been unpaused, if not they call
getFilletChainGcode in fillet.py to fillet the text; once they have the filleted text, then it unpauses.


> python unpause.py
This brings up the dialog, after clicking 'Unpause', the following is printed:
File Screw Holder Bottom.stl is being chain unpaused.
The unpaused file is saved as Screw Holder Bottom_unpause.gcode


>python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import unpause
>>> unpause.main()
This brings up the unpause dialog.


>>> unpause.writeOutput()
Screw Holder Bottom.stl
File Screw Holder Bottom.stl is being chain unpaused.
The unpaused file is saved as Screw Holder Bottom_unpause.gcode

The equation to set the feedrate is from Shane Hathaway's description at:
http://shane.willowrise.com/archives/delay-compensation-in-firmware/
"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import analyze
from skeinforge_tools import fillet
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools import polyfile
import cStringIO
import os
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getUnpauseChainGcode( fileName, gcodeText, unpausePreferences = None ):
	"Unpause a gcode linear move text.  Chain unpause the gcode if it is not already unpaused."
	gcodeText = gcodec.getGcodeFileText( fileName, gcodeText )
	if not gcodec.isProcedureDone( gcodeText, 'fillet' ):
		gcodeText = fillet.getFilletChainGcode( fileName, gcodeText )
	return getUnpauseGcode( gcodeText, unpausePreferences )

def getUnpauseGcode( gcodeText, unpausePreferences = None ):
	"Unpause a gcode linear move text."
	if gcodeText == '':
		return ''
	if gcodec.isProcedureDone( gcodeText, 'unpause' ):
		return gcodeText
	if unpausePreferences == None:
		unpausePreferences = UnpausePreferences()
		preferences.readPreferences( unpausePreferences )
	if not unpausePreferences.activateUnpause.value:
		return gcodeText
	skein = UnpauseSkein()
	skein.parseGcode( unpausePreferences, gcodeText )
	return skein.output.getvalue()

def getSelectedPlugin( unpausePreferences ):
	"Get the selected plugin."
	for plugin in unpausePreferences.unpausePlugins:
		if plugin.value:
			return plugin
	return None

def writeOutput( fileName = '' ):
	"Unpause a gcode linear move file.  Chain unpause the gcode if it is not already unpaused.  If no fileName is specified, unpause the first unmodified gcode file in this folder."
	if fileName == '':
		unmodified = interpret.getGNUTranslatorFilesUnmodified()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	unpausePreferences = UnpausePreferences()
	preferences.readPreferences( unpausePreferences )
	startTime = time.time()
	print( 'File ' + gcodec.getSummarizedFilename( fileName ) + ' is being chain unpaused.' )
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_unpause.gcode'
	unpauseGcode = getUnpauseChainGcode( fileName, '', unpausePreferences )
	if unpauseGcode == '':
		return
	gcodec.writeFileText( suffixFilename, unpauseGcode )
	print( 'The unpaused file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	analyze.writeOutput( suffixFilename, unpauseGcode )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to unpause the file.' )


class UnpausePreferences:
	"A class to handle the unpause preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive = []
		self.activateUnpause = preferences.BooleanPreference().getFromValue( 'Activate Unpause', False )
		self.archive.append( self.activateUnpause )
		self.delay = preferences.FloatPreference().getFromValue( 'Delay (milliseconds):', 28.0 )
		self.archive.append( self.delay )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Unpaused', '' )
		self.archive.append( self.fileNameInput )
		self.maximumSpeed = preferences.FloatPreference().getFromValue( 'Maximum Speed (ratio):', 1.5 )
		self.archive.append( self.maximumSpeed )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Unpause'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.unpause.html' )

	def execute( self ):
		"Unpause button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class UnpauseSkein:
	"A class to unpause a skein of extrusions."
	def __init__( self ):
		self.decimalPlacesCarried = 3
		self.extruderActive = False
		self.feedrateMinute = 959.0
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.output = cStringIO.StringIO()

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + '\n' )

	def getLinearMoveWithFeedrate( self, feedrate, location ):
		"Get a linear move line with the feedrate."
		return 'G1 X%s Y%s Z%s F%s' % ( self.getRounded( location.x ), self.getRounded( location.y ), self.getRounded( location.z ), self.getRounded( feedrate ) )

	def getRounded( self, number ):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToDecimalPlacesString( self.decimalPlacesCarried, number )

	def getUnpausedFeedrateMinute( self, location, splitLine ):
		"Get the feedrate which will compensate for the pause."
		self.feedrateMinute = gcodec.getFeedrateMinute( self.feedrateMinute, splitLine )
		if self.oldLocation == None:
			return self.feedrateMinute
		distance = location.distance( self.oldLocation )
		if distance <= 0.0:
			return self.feedrateMinute
		specifiedFeedrateSecond = self.feedrateMinute / 60.0
		resultantReciprocal = 1.0 - self.delaySecond / distance * specifiedFeedrateSecond
		if resultantReciprocal < self.minimumSpeedUpReciprocal:
			return self.feedrateMinute * self.maximumSpeed
		return self.feedrateMinute / resultantReciprocal

	def getUnpausedLine( self, splitLine ):
		"Bevel a linear move."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		unpausedFeedrateMinute = self.getUnpausedFeedrateMinute( location, splitLine )
		self.oldLocation = location
		return self.getLinearMoveWithFeedrate( unpausedFeedrateMinute, location )

	def parseGcode( self, unpausePreferences, gcodeText ):
		"Parse gcode text and store the unpause gcode."
		self.delaySecond = unpausePreferences.delay.value * 0.001
		self.maximumSpeed = unpausePreferences.maximumSpeed.value
		self.minimumSpeedUpReciprocal = 1.0 / self.maximumSpeed
		self.unpausePreferences = unpausePreferences
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == '(<decimalPlacesCarried>':
				self.decimalPlacesCarried = int( splitLine[ 1 ] )
			elif firstWord == '(</extruderInitialization>)':
				self.addLine( '(<procedureDone> unpause </procedureDone>)' )
				return
			self.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			line = self.getUnpausedLine( splitLine )
		self.addLine( line )


def main( hashtable = None ):
	"Display the unpause dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( UnpausePreferences() )

if __name__ == "__main__":
	main()
