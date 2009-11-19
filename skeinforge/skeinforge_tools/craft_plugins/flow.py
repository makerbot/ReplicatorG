"""
Flow is a script to set the flow rate.

The default 'Activate Flow' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.  The flow script sets the flow rate by writing the M108 gcode.

The 'Flow Rate (arbitrary units)' will be written following the M108 command.  The flow rate is usually a PWM setting, but could be anything, like the rpm of the tool or the duty cycle of the tool.  The default is 210.0.

The following examples flow the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and flow.py.


> python flow.py
This brings up the flow dialog.


> python flow.py Screw Holder Bottom.stl
The flow tool is parsing the file:
Screw Holder Bottom.stl
..
The flow tool has created the file:
.. Screw Holder Bottom_flow.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import flow
>>> flow.main()
This brings up the flow dialog.


>>> flow.writeOutput()
The flow tool is parsing the file:
Screw Holder Bottom.stl
..
The flow tool has created the file:
.. Screw Holder Bottom_flow.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.meta_plugins import polyfile
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', flowRepository = None ):
	"Flow the file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), flowRepository )

def getCraftedTextFromText( gcodeText, flowRepository = None ):
	"Flow a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'flow' ):
		return gcodeText
	if flowRepository == None:
		flowRepository = preferences.getReadRepository( FlowRepository() )
	if not flowRepository.activateFlow.value:
		return gcodeText
	return FlowSkein().getCraftedGcode( gcodeText, flowRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return FlowRepository()

def writeOutput( fileName = '' ):
	"Flow a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'flow' )


class FlowRepository:
	"A class to handle the flow preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Flow', self, '' )
		self.activateFlow = preferences.BooleanPreference().getFromValue( 'Activate Flow:', self, True )
		self.flowRate = preferences.FloatPreference().getFromValue( 'Flow Rate (arbitrary units):', self, 210.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Flow'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.flow.html' )

	def execute( self ):
		"Flow button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class FlowSkein:
	"A class to flow a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0
		self.lines = None
		self.oldFlowRateString = None
		self.oldLocation = None

	def addFlowRateLineIfNecessary( self ):
		"Add flow rate line."
		flowRateString = euclidean.getRoundedToThreePlaces( self.flowRepository.flowRate.value )
		if flowRateString != self.oldFlowRateString:
			self.distanceFeedRate.addLine( 'M108 S' + flowRateString )
		self.oldFlowRateString = flowRateString

	def getCraftedGcode( self, gcodeText, flowRepository ):
		"Parse gcode text and store the flow gcode."
		self.flowRepository = flowRepository
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
				self.distanceFeedRate.addLine( '(<procedureDone> flow </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the flow skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1' or firstWord == '(<layer>':
			self.addFlowRateLineIfNecessary()
		self.distanceFeedRate.addLine( line )


def main():
	"Display the flow dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
