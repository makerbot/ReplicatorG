"""
Outset is a script to outset the perimeters of a gcode file.

Outset outsets the perimeters of the slices of a gcode file.  The outside perimeters will be outset by half the perimeter width, and the inside perimeters will be inset by half the perimeter width.  Outset is needed for subtractive machining, like cutting or milling.

The default 'Activate Outset' checkbox is on.  When it is on, the gcode will be outset, when it is off, the gcode will not be changed.

The following examples outset the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and outset.py.


> python outset.py
This brings up the outset dialog.


> python outset.py Screw Holder Bottom.stl
The outset tool is parsing the file:
Screw Holder Bottom.stl
..
The outset tool has created the file:
.. Screw Holder Bottom_outset.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import outset
>>> outset.main()
This brings up the outset dialog.


>>> outset.writeOutput()
The outset tool is parsing the file:
Screw Holder Bottom.stl
..
The outset tool has created the file:
.. Screw Holder Bottom_outset.gcode

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
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import triangle_mesh
from skeinforge_tools.meta_plugins import polyfile
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"


def compareAreaAscending( loopArea, otherLoopArea ):
	"Get comparison in order to sort loop areas in ascending order of area."
	if loopArea.area > otherLoopArea.area:
		return 1
	if loopArea.area < otherLoopArea.area:
		return - 1
	return 0

def getCraftedText( fileName, text = '', outsetRepository = None ):
	"Outset the preface file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), outsetRepository )

def getCraftedTextFromText( gcodeText, outsetRepository = None ):
	"Outset the preface gcode text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'outset' ):
		return gcodeText
	if outsetRepository == None:
		outsetRepository = preferences.getReadRepository( OutsetRepository() )
	if not outsetRepository.activateOutset.value:
		return gcodeText
	return OutsetSkein().getCraftedGcode( outsetRepository, gcodeText )

def getRepositoryConstructor():
	"Get the repository constructor."
	return OutsetRepository()

def writeOutput( fileName = '' ):
	"Outset the carving of a gcode file.  If no fileName is specified, outset the first unmodified gcode file in this folder."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'outset' )


class OutsetRepository:
	"A class to handle the outset preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Outsetted', self, '' )
		self.activateOutset = preferences.BooleanPreference().getFromValue( 'Activate Outset:', self, True )
		self.executeTitle = 'Outset'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.outset.html' )

	def execute( self ):
		"Outset button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class OutsetSkein:
	"A class to outset a skein of extrusions."
	def __init__( self ):
		self.boundary = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0
		self.rotatedBoundaryLayers = []
		self.shutdownLines = None

	def addGcodeFromRemainingLoop( self, loop, radius, z ):
		"Add the remainder of the loop which does not overlap the alreadyFilledArounds loops."
		boundary = intercircle.getLargestInsetLoopFromLoopNoMatterWhat( loop, radius )
		euclidean.addSurroundingLoopBeginning( boundary, self, z )
		self.distanceFeedRate.addPerimeterBlock( loop, z )
		self.distanceFeedRate.addLine( '(</surroundingLoop>)' )

	def addOutset( self, rotatedBoundaryLayer ):
		"Add outset to the layer."
		self.distanceFeedRate.addLine( '(<layer> %s )' % rotatedBoundaryLayer.z ) # Indicate that a new layer is starting.
		extrudateLoops = intercircle.getInsetLoopsFromLoops( - self.absoluteHalfPerimeterWidth, rotatedBoundaryLayer.loops )
		sortedLoops = triangle_mesh.getLoopsInOrderOfArea( compareAreaAscending, extrudateLoops )
		for sortedLoop in sortedLoops:
			self.addGcodeFromRemainingLoop( sortedLoop, self.absoluteHalfPerimeterWidth, rotatedBoundaryLayer.z )
		self.distanceFeedRate.addLine( '(</layer>)' )

	def addRotatedLoopLayer( self, z ):
		"Add rotated loop layer."
		self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		self.rotatedBoundaryLayers.append( self.rotatedBoundaryLayer )

	def addShutdownToOutput( self ):
		"Add shutdown gcode to the output."
		self.distanceFeedRate.addLines( self.shutdownLines )

	def getCraftedGcode( self, outsetRepository, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.outsetRepository = outsetRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			self.addOutset( rotatedBoundaryLayer )
		self.addShutdownToOutput()
		return self.distanceFeedRate.output.getvalue()

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ].lstrip()
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addTagBracketedLine( 'procedureDone', 'outset' )
			elif firstWord == '(<perimeterWidth>':
				self.absoluteHalfPerimeterWidth = 0.5 * abs( float( splitLine[ 1 ] ) )
			elif firstWord == '(<layer>':
				self.lineIndex -= 1
				return
			self.distanceFeedRate.addLine( line )

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the outset skein."
		line = self.lines[ lineIndex ].lstrip()
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine( None, splitLine )
			self.boundary.append( location.dropAxis( 2 ) )
		elif firstWord == '(<layer>':
			self.addRotatedLoopLayer( float( splitLine[ 1 ] ) )
		elif firstWord == '(<surroundingLoop>)':
			self.boundary = []
			self.rotatedBoundaryLayer.loops.append( self.boundary )
		elif firstWord == '(</extrusion>)':
			self.shutdownLines = []
		if self.shutdownLines != None:
			self.shutdownLines.append( line )


def main():
	"Display the outset dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
