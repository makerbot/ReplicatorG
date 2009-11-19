"""
Carve shape is a script to carve a list of slice layers.

Carve carves a list of slices into svg slice layers.  The 'Layer Thickness' is the thickness the extrusion layer at default extruder speed, this is the most important carve preference.  The 'Perimeter Width over Thickness' is the ratio of the extrusion perimeter width to the layer thickness.  The higher the value the more the perimeter will be inset, the default is 1.8.  A ratio of one means the extrusion is a circle, a typical ratio of 1.8 means the extrusion is a wide oval.  These values should be measured from a test extrusion line.

When a triangle mesh has holes in it, the triangle mesh slicer switches over to a slow algorithm that spans gaps in the mesh.  The higher the 'Import Coarseness' setting, the wider the gaps in the mesh it will span.  An import coarseness of one means it will span gaps of the perimeter width.  When the Mesh Type preference is Correct Mesh, the mesh will be accurately carved, and if a hole is found, carve will switch over to the algorithm that spans gaps.  If the Mesh Type preference is Unproven Mesh, carve will use the gap spanning algorithm from the start.  The problem with the gap spanning algothm is that it will span gaps, even if there is not actually a gap in the model.

If 'Infill in Direction of Bridges'  is selected, the infill will be in the direction of bridges across gaps, so that the fill will be able to span a bridge easier.

The 'Extra Decimal Places' is the number of extra decimal places export will output compared to the number of decimal places in the layer thickness.  The higher the 'Extra Decimal Places', the more significant figures the output numbers will have, the default is one.

Carve slices from bottom to top.  The output will go from the "Layers From" index to the "Layers To" index.  The default for the "Layers From" index is zero and the default for the "Layers To" is a really big number.  To get a single layer, set the "Layers From" to zero and the "Layers To" to one.

The following examples carve the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and carve.py.


> python carve.py
This brings up the carve dialog.


> python carve.py Screw Holder Bottom.stl
The carve tool is parsing the file:
Screw Holder Bottom.stl
..
The carve tool has created the file:
.. Screw Holder Bottom_carve.svg


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import carve
>>> carve.main()
This brings up the carve dialog.


>>> carve.writeOutput()
The carve tool is parsing the file:
Screw Holder Bottom.stl
..
The carve tool has created the file:
.. Screw Holder Bottom_carve.svg

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities import svg_codec
from skeinforge_tools.skeinforge_utilities import triangle_mesh
from skeinforge_tools.meta_plugins import polyfile
import math
import os
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/02/05 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', carveRepository = None ):
	"Get carved text."
	if gcodec.getHasSuffix( fileName, '.svg' ):
		text = gcodec.getTextIfEmpty( fileName, text )
		return text
	return getCraftedTextFromFileName( fileName, carveRepository = None )

def getCraftedTextFromFileName( fileName, carveRepository = None ):
	"Carve a shape file."
	carving = svg_codec.getCarving( fileName )
	if carving == None:
		return ''
	if carveRepository == None:
		carveRepository = CarveRepository()
		preferences.getReadRepository( carveRepository )
	return CarveSkein().getCarvedSVG( carveRepository, carving, fileName )

def getRepositoryConstructor():
	"Get the repository constructor."
	return CarveRepository()

def writeOutput( fileName = '' ):
	"Carve a GNU Triangulated Surface file."
	startTime = time.time()
	print( 'File ' + gcodec.getSummarizedFilename( fileName ) + ' is being carved.' )
	carveGcode = getCraftedText( fileName )
	if carveGcode == '':
		return
	suffixFilename = gcodec.getFilePathWithUnderscoredBasename( fileName, '_carve.svg' )
	gcodec.writeFileText( suffixFilename, carveGcode )
	print( 'The carved file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to carve the file.' )
	preferences.openWebPage( suffixFilename )


class CarveRepository:
	"A class to handle the carve preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getTranslatorFileTypeTuples(), 'Open File to be Carved', self, '' )
		self.bridgeThicknessMultiplier = preferences.FloatPreference().getFromValue( 'Bridge Thickness Multiplier (ratio):', self, 1.0 )
		self.extraDecimalPlaces = preferences.IntPreference().getFromValue( 'Extra Decimal Places (integer):', self, 1 )
		self.importCoarseness = preferences.FloatPreference().getFromValue( 'Import Coarseness (ratio):', self, 1.0 )
		self.infillDirectionBridge = preferences.BooleanPreference().getFromValue( 'Infill in Direction of Bridges', self, True )
		self.layerThickness = preferences.FloatPreference().getFromValue( 'Layer Thickness (mm):', self, 0.4 )
		self.layersFrom = preferences.IntPreference().getFromValue( 'Layers From (index):', self, 0 )
		self.layersTo = preferences.IntPreference().getFromValue( 'Layers To (index):', self, 999999999 )
		self.meshTypeLabel = preferences.LabelDisplay().getFromName( 'Mesh Type: ', self )
		importRadio = []
		self.correctMesh = preferences.Radio().getFromRadio( 'Correct Mesh', importRadio, self, True )
		self.unprovenMesh = preferences.Radio().getFromRadio( 'Unproven Mesh', importRadio, self, False )
		self.perimeterWidthOverThickness = preferences.FloatPreference().getFromValue( 'Perimeter Width over Thickness (ratio):', self, 1.8 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Carve'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.carve.html' )

	def execute( self ):
		"Carve button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypes( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class CarveSkein( svg_codec.SVGCodecSkein ):
	"A class to carve a carving."
	def addRotatedLoopLayersToOutput( self, rotatedBoundaryLayers ):
		"Add rotated boundary layers to the output."
		truncatedRotatedBoundaryLayers = rotatedBoundaryLayers[ self.carveRepository.layersFrom.value : self.carveRepository.layersTo.value ]
		for truncatedRotatedBoundaryLayerIndex in xrange( len( truncatedRotatedBoundaryLayers ) ):
			truncatedRotatedBoundaryLayer = truncatedRotatedBoundaryLayers[ truncatedRotatedBoundaryLayerIndex ]
			self.addRotatedLoopLayerToOutput( truncatedRotatedBoundaryLayerIndex, truncatedRotatedBoundaryLayer )

	def addRotatedLoopLayerToOutput( self, layerIndex, rotatedBoundaryLayer ):
		"Add rotated boundary layer to the output."
		self.addLayerStart( layerIndex, rotatedBoundaryLayer.z )
		if rotatedBoundaryLayer.rotation != None:
			self.addLine('\t\t\t<!--bridgeRotation--> %s' % rotatedBoundaryLayer.rotation ) # Indicate the bridge rotation.
#			<path transform="scale(3.7, -3.7) translate(0, 5)" d="M 0 -5 L 50 0 L60 50 L 5 50 z M 5 3 L5 15 L15 15 L15 5 z"/>
#		transform = 'scale(' + unitScale + ' ' + (unitScale * -1) + ') translate(' + (sliceMinX * -1) + ' ' + (sliceMinY * -1) + ')'
		pathString = '\t\t\t<path transform="scale(%s, %s) translate(%s, %s)" d="' % ( self.unitScale, - self.unitScale, self.getRounded( - self.cornerMinimum.x ), self.getRounded( - self.cornerMinimum.y ) )
		if len( rotatedBoundaryLayer.loops ) > 0:
			pathString += self.getSVGLoopString( rotatedBoundaryLayer.loops[ 0 ] )
		for loop in rotatedBoundaryLayer.loops[ 1 : ]:
			pathString += ' ' + self.getSVGLoopString( loop )
		pathString += '"/>'
		self.addLine( pathString )
		self.addLine( '\t\t</g>' )

	def getCarvedSVG( self, carveRepository, carving, fileName ):
		"Parse gnu triangulated surface text and store the carved gcode."
		self.carveRepository = carveRepository
		self.layerThickness = carveRepository.layerThickness.value
		self.setExtrusionDiameterWidth( carveRepository )
		if carveRepository.infillDirectionBridge.value:
			carving.setCarveBridgeLayerThickness( self.bridgeLayerThickness )
		carving.setCarveLayerThickness( self.layerThickness )
		importRadius = 0.5 * carveRepository.importCoarseness.value * abs( self.perimeterWidth )
		carving.setCarveImportRadius( max( importRadius, 0.01 * self.layerThickness ) )
		carving.setCarveIsCorrectMesh( carveRepository.correctMesh.value )
		rotatedBoundaryLayers = carving.getCarveRotatedBoundaryLayers()
		if len( rotatedBoundaryLayers ) < 1:
			return ''
		self.cornerMaximum = carving.getCarveCornerMaximum()
		self.cornerMinimum = carving.getCarveCornerMinimum()
		#reset from slicable
		self.layerThickness = carving.getCarveLayerThickness()
		self.setExtrusionDiameterWidth( carveRepository )
		self.decimalPlacesCarried = max( 0, 1 + carveRepository.extraDecimalPlaces.value - int( math.floor( math.log10( self.layerThickness ) ) ) )
		self.extent = self.cornerMaximum - self.cornerMinimum
		self.svgTemplateLines = self.getReplacedSVGTemplateLines( fileName, rotatedBoundaryLayers )
		self.addInitializationToOutputSVG( 'carve' )
		self.addRotatedLoopLayersToOutput( rotatedBoundaryLayers )
		self.addShutdownToOutput()
		return self.output.getvalue()

	def setExtrusionDiameterWidth( self, carveRepository ):
		"Set the extrusion diameter & width and the bridge thickness & width."
		self.bridgeLayerThickness = self.layerThickness * carveRepository.bridgeThicknessMultiplier.value
		self.perimeterWidth = carveRepository.perimeterWidthOverThickness.value * self.layerThickness


def main():
	"Display the carve dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
