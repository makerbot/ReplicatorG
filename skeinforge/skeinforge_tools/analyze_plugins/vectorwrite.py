"""
Vectorwrite is a script to write Scalable Vector Graphics for a gcode file.

The default 'Activate Vectorwrite' checkbox is on.  When it is on, the functions described below will work when called from the skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether or not the 'Activate Vectorwrite' checkbox is on, when vectorwrite is run directly.

The "Layer From" is the index of the bottom layer that will be displayed.  If the layer from is the default zero, the display will start from the lowest layer.  If the the layer from index is negative, then the display will start from the layer from index below the top layer.  The "Layer To" is the index of the top layer that will be displayed.  If the layer to index is a huge number like the default 999999999, the display will go to the top of the model, at least until we model habitats:)  If the layer to index is negative, then the display will go to the layer to index below the top layer.  The layer from until layer to index is a python slice.

To run vectorwrite, in a shell in the folder which vectorwrite is in type:
> python vectorwrite.py

The Scalable Vector Graphics file can be opened by an SVG viewer or an SVG capable browser like Mozilla:
http://www.mozilla.com/firefox/

This example writes vector graphics for the gcode file Screw Holder.gcode.  This example is run in a terminal in the folder which contains Screw Holder.gcode and vectorwrite.py.


> python vectorwrite.py
This brings up the vectorwrite dialog.


> python vectorwrite.py Screw Holder.gcode
The vectorwrite file is saved as Screw_Holder_vectorwrite.svg


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import vectorwrite
>>> vectorwrite.main()
This brings up the vectorwrite dialog.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import svg_codec
from skeinforge_tools.meta_plugins import polyfile
import cStringIO
import os
import sys
import time

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


#add open webbrowser first time file is created choice
def getRepositoryConstructor():
	"Get the repository constructor."
	return VectorwritePreferences()

def writeGivenPreferences( fileName, gcodeText, vectorwritePreferences ):
	"Write scalable vector graphics for a gcode file given the preferences."
	if gcodeText == '':
		return ''
	startTime = time.time()
	vectorwriteGcode = VectorwriteSkein().getSVG( fileName, gcodeText, vectorwritePreferences )
	if vectorwriteGcode == '':
		return
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_vectorwrite.svg'
	suffixDirectoryName = os.path.dirname( suffixFilename )
	suffixReplacedBaseName = os.path.basename( suffixFilename ).replace( ' ', '_' )
	suffixFilename = os.path.join( suffixDirectoryName, suffixReplacedBaseName )
	gcodec.writeFileText( suffixFilename, vectorwriteGcode )
	print( 'The vectorwrite file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to vectorwrite the file.' )
	preferences.openWebPage( suffixFilename )

def writeOutput( fileName, gcodeText = '' ):
	"Write scalable vector graphics for a skeinforge gcode file, if activate vectorwrite is selected."
	vectorwritePreferences = preferences.getReadRepository( VectorwritePreferences() )
	if not vectorwritePreferences.activateVectorwrite.value:
		return
	gcodeText = gcodec.getTextIfEmpty( fileName, gcodeText )
	writeGivenPreferences( fileName, gcodeText, vectorwritePreferences )

def writeRegardless( fileName ):
	"Write scalable vector graphics for a gcode file."
	vectorwritePreferences = preferences.getReadRepository( VectorwritePreferences() )
	gcodeText = gcodec.getFileText( fileName )
	writeGivenPreferences( fileName, gcodeText, vectorwritePreferences )


class VectorwritePreferences:
	"A class to handle the vectorwrite preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.activateVectorwrite = preferences.BooleanPreference().getFromValue( 'Activate Vectorwrite', self, False )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Write Vector Graphics for', self, '' )
		self.layersFrom = preferences.IntPreference().getFromValue( 'Layers From (index):', self, 0 )
		self.layersTo = preferences.IntPreference().getFromValue( 'Layers To (index):', self, 999999999 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Vectorwrite'
		self.saveCloseTitle = 'Save and Close'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.vectorwrite.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeRegardless( fileName )


class VectorwriteSkein( svg_codec.SVGCodecSkein ):
	"A class to vectorwrite a carving."
	def addRotatedLoopLayer( self, z ):
		"Add rotated loop layer."
		self.rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		self.rotatedBoundaryLayer.paths = []
		self.rotatedBoundaryLayers.append( self.rotatedBoundaryLayer )

	def addRotatedLoopLayersToOutput( self, rotatedBoundaryLayers ):
		"Add rotated boundary layers to the output."
		truncatedRotatedBoundaryLayers = rotatedBoundaryLayers[ self.vectorwritePreferences.layersFrom.value : self.vectorwritePreferences.layersTo.value ]
		for truncatedRotatedBoundaryLayerIndex in xrange( len( truncatedRotatedBoundaryLayers ) ):
			truncatedRotatedBoundaryLayer = truncatedRotatedBoundaryLayers[ truncatedRotatedBoundaryLayerIndex ]
			self.addRotatedLoopLayerToOutput( truncatedRotatedBoundaryLayerIndex, truncatedRotatedBoundaryLayer )

	def addRotatedLoopLayerToOutput( self, layerIndex, rotatedBoundaryLayer ):
		"Add rotated boundary layer to the output."
		self.addLayerStart( layerIndex, rotatedBoundaryLayer.z )
		pathStart = '\t\t\t<path transform="scale(%s, %s) translate(%s, %s)" d="' % ( self.unitScale, - self.unitScale, self.getRounded( - self.cornerMinimum.x ), self.getRounded( - self.cornerMinimum.y ) )
		loopString = ''
		for loop in rotatedBoundaryLayer.loops:
			loopString += self.getSVGLoopString( loop ) + ' '
		if len( loopString ) > 0:
			self.addLine( pathStart + loopString[ : - 1 ] + '"/>' )
		pathString = ''
		for path in rotatedBoundaryLayer.paths:
			pathString += self.getSVGPathString( path ) + ' '
		if len( pathString ) > 0:
			self.addLine( pathStart + pathString[ : - 1 ] + '" fill="none" stroke="#F00"/>' )
		self.addLine( '\t\t</g>' )

	def getSVG( self, fileName, gcodeText, vectorwritePreferences ):
		"Parse gnu triangulated surface text and store the vectorwrite gcode."
		self.cornerMaximum = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerMinimum = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.extruderActive = False
		self.isPerimeter = False
		self.lines = gcodec.getTextLines( gcodeText )
		self.oldLocation = None
		self.thread = []
		self.rotatedBoundaryLayers = []
		self.vectorwritePreferences = vectorwritePreferences
		self.parseInitialization()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		self.extent = self.cornerMaximum - self.cornerMinimum
		self.svgTemplateLines = self.getReplacedSVGTemplateLines( fileName, self.rotatedBoundaryLayers )
		self.addInitializationToOutputSVG( 'vectorwrite' )
		self.addRotatedLoopLayersToOutput( self.rotatedBoundaryLayers )
		self.addShutdownToOutput()
		return self.output.getvalue()

	def linearMove( self, splitLine ):
		"Get statistics for a linear move."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.cornerMaximum = euclidean.getPointMaximum( self.cornerMaximum, location )
		self.cornerMinimum = euclidean.getPointMinimum( self.cornerMinimum, location )
		self.thread.append( location.dropAxis( 2 ) )
		self.oldLocation = location

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == '(<decimalPlacesCarried>':
				self.decimalPlacesCarried = int( splitLine[ 1 ] )
			elif firstWord == '(<layerThickness>':
				self.layerThickness = float( splitLine[ 1 ] )
			elif firstWord == '(<extrusion>)':
				return
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )

	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the outset skein."
		line = self.lines[ lineIndex ]
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == '(<layer>':
			self.addRotatedLoopLayer( float( splitLine[ 1 ] ) )
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
			if self.isPerimeter:
				self.rotatedBoundaryLayer.loops.append( self.thread )
			else:
				self.rotatedBoundaryLayer.paths.append( self.thread )
			self.isPerimeter = False
			self.thread = []
		elif firstWord == '(<perimeter>)':
			self.isPerimeter = True


def main():
	"Display the vectorwrite dialog."
	if len( sys.argv ) > 1:
		writeRegardless( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
