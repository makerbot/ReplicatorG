"""
Lift is a script to change the altitude of a gcode file.

Lift will change the altitude of the cutting tool when it is on so that it will cut through the slab at the correct altitude.  It will also lift the gcode when the tool is off so that the cutting tool will clear the top of the slab.

The default 'Activate Lift' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The 'Cutting Lift over Layer Step' is the ratio of the amount the cutting tool will be lifted over the layer step.  If whittle is off the layer step will be the layer thickness, if it is on, it will be the layer step from the whittle gcode.  If the cutting tool is like an end mill, where the cutting happens until the end of the tool, then the 'Cutting Lift over Layer Step' should be minus 0.5, so that the end mill cuts to the bottom of the slab.  If the cutting tool is like a laser, where the cutting happens around the focal point. the 'Cutting Lift over Layer Step' should be zero, so that the cutting action will be focused in the middle of the slab.  The default is minus 0.5, because the end mill is the more common tool.

The 'Clearance above Top' is the distance above the top of the slab the cutting tool will be lifted when will tool is off so that the cutting tool will clear the top of the slab.  The default is 5 mm.

The following examples lift the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and lift.py.


> python lift.py
This brings up the lift dialog.


> python lift.py Screw Holder Bottom.stl
The lift tool is parsing the file:
Screw Holder Bottom.stl
..
The lift tool has created the file:
.. Screw Holder Bottom_lift.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import lift
>>> lift.main()
This brings up the lift dialog.


>>> lift.writeOutput()
The lift tool is parsing the file:
Screw Holder Bottom.stl
..
The lift tool has created the file:
.. Screw Holder Bottom_lift.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.meta_plugins import polyfile
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', liftRepository = None ):
	"Lift the preface file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), liftRepository )

def getCraftedTextFromText( gcodeText, liftRepository = None ):
	"Lift the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'lift' ):
		return gcodeText
	if liftRepository == None:
		liftRepository = preferences.getReadRepository( LiftRepository() )
	if not liftRepository.activateLift.value:
		return gcodeText
	return LiftSkein().getCraftedGcode( liftRepository, gcodeText )

def getRepositoryConstructor():
	"Get the repository constructor."
	return LiftRepository()

def writeOutput( fileName = '' ):
	"Lift the carving of a gcode file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'lift' )


class LiftRepository:
	"A class to handle the lift preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Lifted', self, '' )
		self.activateLift = preferences.BooleanPreference().getFromValue( 'Activate Lift:', self, True )
		self.cuttingLiftOverLayerStep = preferences.FloatPreference().getFromValue( 'Cutting Lift over Layer Step (ratio):', self, - 0.5 )
		self.clearanceAboveTop = preferences.FloatPreference().getFromValue( 'Clearance above Top (mm):', self, 5.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Lift'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.lift.html' )

	def execute( self ):
		"Lift button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class LiftSkein:
	"A class to lift a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.layerStep = None
		self.layerThickness = 0.3333333333
		self.lineIndex = 0
		self.maximumZ = - 912345678.0
		self.oldLocation = None
		self.previousActiveMovementLine = None
		self.previousInactiveMovementLine = None

	def addPreviousInactiveMovementLineIfNecessary( self ):
		"Add the previous inactive movement line if necessary."
		if self.previousInactiveMovementLine != None:
			self.distanceFeedRate.addLine( self.previousInactiveMovementLine )
			self.previousInactiveMovementLine = None

	def getCraftedGcode( self, liftRepository, gcodeText ):
		"Parse gcode text and store the lift gcode."
		self.liftRepository = liftRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		self.oldLocation = None
		if self.layerStep == None:
			self.layerStep = self.layerThickness
		self.cuttingLift = self.layerStep * liftRepository.cuttingLiftOverLayerStep.value
		self.travelZ = self.maximumZ + 0.5 * self.layerStep + liftRepository.clearanceAboveTop.value
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getLinearMove( self, line, location, splitLine ):
		"Get the linear move."
		if self.extruderActive:
			z = location.z + self.cuttingLift
			return self.distanceFeedRate.getLineWithZ( line, splitLine, z )
		if self.previousActiveMovementLine != None:
			previousActiveMovementLineSplit = self.previousActiveMovementLine.split()
			self.distanceFeedRate.addLine( self.distanceFeedRate.getLineWithZ( self.previousActiveMovementLine, previousActiveMovementLineSplit, self.travelZ ) )
			self.previousActiveMovementLine = None
		self.distanceFeedRate.addLine( self.distanceFeedRate.getLineWithZ( line, splitLine, self.travelZ ) )
		self.previousInactiveMovementLine = line
		return ''

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'lift' )
				return
			elif firstWord == '(<layerThickness>':
				self.layerThickness = float( splitLine[ 1 ] )
			elif firstWord == '(<layerStep>':
				self.layerStep = float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the lift skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
			line = self.getLinearMove( line, location, splitLine )
			self.previousActiveMovementLine = line
			self.oldLocation = location
		elif firstWord == 'M101':
			self.addPreviousInactiveMovementLineIfNecessary()
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
		self.distanceFeedRate.addLine( line )


def main():
	"Display the lift dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
