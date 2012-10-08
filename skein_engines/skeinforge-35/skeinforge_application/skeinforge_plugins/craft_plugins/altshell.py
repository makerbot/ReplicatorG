"""
Dan Newman
10 April 2012
dan dot newman at mtbaldy dot us

Altshell is a script to cause the outside perimeter of an object to be printed
with the valve closed.  The purpose is to allow downstream processing to
identify gcode for the outside shells of an object by spotting segments to be
printed with a closed valve state.

To install the altshell script, move altshell.py to the directory

    skein_engines/skeinforge-VERSION/skeinforge_application/skeinforge_plugins/craft_plugins/

Then edit the file

    skein_engines/skeinforge-VERSION/skeinforge_application/skeinforge_plugins/profile_plugins/extrusion.py

and add the altshell script to the tool chain sequence by inserting 'altshell'
into the plugin sequence in getCraftSequence().   Place 'altshell' before the
'outline' and 'skirt' plugins as they may inject themselves into the perimeter
without marking themselves as not being part of the perimeter.

==Operation==
The default 'Activate Altshell' checkbox is off, enable it if you would like an outline printed.

==Settings==

===Use M320/M321 Commands===

Use M320/M321 to enable  / disable acceleration if checked.
When unchecked, uses Open/Close Valve (M126/M127).
Newer firmwares require M320/M321.
"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_application.skeinforge_utilities import skeinforge_profile
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import archive
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
import sys

__author__ = "Len Trigg (lenbok@gmail.com)"
__date__ = "$Date: 2010/11/20 $"
__license__ = "GPL 3.0"

def getCraftedText( fileName, text='', repository=None ):
	"Alternate shell text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), repository )

def getCraftedTextFromText( gcodeText, repository=None ):
	"Alternate shell text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'altshell' ):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository( AltshellRepository() )
	if not repository.activateAltshell.value:
		return gcodeText
	return AltshellSkein().getCraftedGcode( gcodeText, repository )

def getNewRepository():
	"Get the repository constructor."
	return AltshellRepository()

def writeOutput( fileName = ''):
	"Alternate shell file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName == '':
		return
	skeinforge_craft.writeChainTextWithNounMessage( fileName, 'altshell')

class AltshellRepository:
	"A class to handle the altshell settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge_tools.craft_plugins.altshell.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Altshell', self, '' )
		self.activateAltshell = settings.BooleanSetting().getFromValue( 'Activate Altshell', self, False)
		self.useM320M321 = settings.BooleanSetting().getFromValue( 'Use M320/M321', self, False)
		self.executeTitle = 'Altshell'

	def execute( self ):
		"Altshell button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, fabmetheus_interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )

class AltshellSkein:
	"A class to print the outermost shell with the valve closed."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.state            = 0

	def getCraftedGcode( self, gcodeText, repository ):
		"Parse gcode text and add the altshell gcode."
		self.repository = repository
		self.lines = archive.getTextLines( gcodeText )
		for line in self.lines:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def parseLine( self, line ):
		""""
		Parse a gcode line and add it to the altshell skein.

		We want to place the close valve (M321/M127) command after the first M101 command.
		So doing ensures that any outline or skirt is printed before the valve is
		closed.
		"""

		splitLine = line.split()
		if len( splitLine ) < 1:
			return

		firstWord = splitLine[ 0 ]

		if line == '(<perimeter> outer )' or line == '(<perimeter> inner )':
			self.state = 1
	
		elif firstWord == '(</perimeter>)':
			if self.state == 3:
				# Open valve command
				if self.repository.useM320M321.value:
					self.distanceFeedRate.addLine( 'M320' )
				else:
					self.distanceFeedRate.addLine( 'M126' )
				self.state = 0

		elif firstWord == 'M101':
			if self.state == 1:
				# Found first M101 for outer perimeter
				self.state = 2

		self.distanceFeedRate.addLine( line )
		if self.state == 2:
			# Close valve command
			if self.repository.useM320M321.value:
				self.distanceFeedRate.addLine( 'M321' )
			else:
				self.distanceFeedRate.addLine( 'M127' )
			self.state = 3

def main():
	"Display the altshell dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
