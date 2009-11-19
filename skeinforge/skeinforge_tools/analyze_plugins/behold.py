"""
Behold is an analysis script to display a gcode file in an isometric view.

The default 'Activate Behold' checkbox is on.  When it is on, the functions described below will work when called from the skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether or not the 'Activate Behold' checkbox is on, when behold is run directly.  Behold can not separate the layers when it reads gcode without comments.

The viewer is simple, the viewpoint can only be moved in a sphere around the center of the model by changing the viewpoint latitude and longitude.  Different regions of the model can be hidden by setting the width of the thread to zero.  The alternating bands act as contour bands and their brightness and width can be changed.  The layers will be displayed starting at the "Layer From" index up until the "Layer To" index.  All of the preferences can be set in the initial "Behold Preferences" window and some can be changed after the viewer is running in the "Behold Dynamic Preferences" window.  In the viewer, dragging the mouse will change the viewpoint.

The "Band Height" is height of the band in layers, with the default of five, a pair of bands is ten layers high.  The "Bottom Band Brightness" is the ratio of the brightness of the bottom band over the brightness of the top band, the default is 0.7.  The "Bottom Layer Brightness" is the ratio of the brightness of the bottom layer over the brightness of the top layer, the default is 1.0.  With a low bottom layer brightness ratio the bottom of the model will be darker than the top of the model, as if it was being illuminated by a light just above the top.  The "Bright Band Start" button group determines where the bright band starts from.  If the "From the Bottom" choice is selected, the bright bands will start from the bottom.  If the default "From the Top" choice is selected, the bright bands will start from the top.

If "Draw Arrows" is selected, arrows will be drawn at the end of each line segment, the default is on.  If "Go Around Extruder Off Travel" is selected, the display will include the travel when the extruder is off, which means it will include the nozzle wipe path if any.

When the export menu item in the file menu is clicked, the canvas will be saved as a postscript file.  If the 'Export Postscript Program' is set to a program name, the postscript file will be sent to that program to be opened.  The default is gimp, the Gnu Image Manipulation Program (Gimp), which is open source, can open postscript and save in a variety of formats.  It is available at:
http://www.gimp.org/

If the 'Export File Extension' is set to a file extension, the postscript file will be sent to the program, along with the file extension for the converted output.  The default is blank because some systems do not have an image conversion program; if you have or will install an image conversion program, a common 'Export File Extension' is png.  A good open source conversion program is Image Magick, which is available at:
http://www.imagemagick.org/script/index.php

The "Layer From" is the index of the bottom layer that will be displayed.  If the layer from is the default zero, the display will start from the lowest layer.  If the the layer from index is negative, then the display will start from the layer from index below the top layer.  The "Layer To" is the index of the top layer that will be displayed.  If the layer to index is a huge number like the default 999999999, the display will go to the top of the model, at least until we model habitats:)  If the layer to index is negative, then the display will go to the layer to index below the top layer.  The layer from until layer to index is a python slice.

The mouse tool can be changed from the 'Mouse Mode' menu button or item.  The 'Display Line' tool will display the line index of the line clicked, counting from one, and the line itself.  The 'Viewpoint Move' tool will move the viewpoint in the xy plane when the mouse is clicked and dragged on the canvas.  The 'Viewpoint Rotate' tool will rotate the viewpoint around the origin, when the mouse is clicked and dragged on the canvas.

The "Number of Fill Bottom Layers" is the number of layers at the bottom which will be colored olive.  The "Number of Fill Bottom Layers" is the number of layers at the top which will be colored blue.

The "Pixels over Extrusion Width" preference is the scale of the image, the higher the number, the greater the size of the display.  The "Screen Horizontal Inset" determines how much the display will be inset in the horizontal direction from the edge of screen, the higher the number the more it will be inset and the smaller it will be, the default is one hundred.  The "Screen Vertical Inset" determines how much the display will be inset in the vertical direction from the edge of screen, the default is fifty.

The "Viewpoint Latitude" is the latitude of the viewpoint, the default is 15 degrees.  The "Viewpoint Longitude" is the longitude of the viewpoint, the default is 210 degrees.  The viewpoint can also be moved by dragging the mouse.  The viewpoint latitude will be increased when the mouse is dragged from the center towards the edge.  The viewpoint longitude will be changed by the amount around the center the mouse is dragged.  This is not very intuitive, but I don't know how to do this the intuitive way and I have other stuff to develop.  If the shift key is pressed; if the latitude is changed more than the longitude, only the latitude will be changed, if the longitude is changed more only the longitude will be changed.

The "Width of Extrusion Thread" sets the width of the green extrusion threads, those threads which are not loops and not part of the raft.  The default is one, if the width is zero the extrusion threads will be invisible.  The "Width of Fill Bottom Thread" sets the width of the olive extrusion threads at the bottom of the model, the default is three.  The "Width of Fill Top Thread" sets the width of the blue extrusion threads at the top of the model, the default is three.  The "Width of Loop Thread" sets the width of the yellow loop threads, which are not perimeters, the default is three.  The "Width of Perimeter Inside Thread" sets the width of the orange inside perimeter threads, the default is three.  The "Width of Perimeter Outside Thread" sets the width of the red outside perimeter threads, the default is three.  The "Width of Raft Thread" sets the width of the brown raft threads, the default is one.  The "Width of Travel Thread" sets the width of the gray extruder off travel threads, the default is zero.

The "Width of X Axis" preference sets the width of the dark orange X Axis, the default is five pixels.  The "Width of Y Axis" sets the width of the gold Y Axis, the default is five.  The "Width of Z Axis" sets the width of the sky blue Z Axis, the default is five.

To run behold, in a shell in the folder which behold is in type:
> python behold.py

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example lets the viewer behold the gcode file Screw Holder.gcode.  This example is run in a terminal in the folder which contains Screw Holder.gcode and behold.py.


> python behold.py
This brings up the behold dialog.


> python behold.py Screw Holder.gcode
This brings up the behold dialog to view the gcode file.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import behold
>>> behold.main()
This brings up the behold dialog.


>>> behold.beholdFile()
This brings up the behold window to view the gcode file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.analyze_plugins.analyze_utilities import display_line
from skeinforge_tools.analyze_plugins.analyze_utilities import tableau
from skeinforge_tools.analyze_plugins.analyze_utilities import viewpoint_move
from skeinforge_tools.analyze_plugins.analyze_utilities import viewpoint_rotate
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


#bring up the preferences window, maybe make dragging more intuitive
def beholdFile( fileName = '' ):
	"Behold a gcode file.  If no fileName is specified, behold the first gcode file in this folder that is not modified."
	gcodeText = gcodec.getFileText( fileName )
	displayFileGivenText( fileName, gcodeText )

def compareLayerSequence( first, second ):
	"Get comparison in order to sort skein panes in ascending order of layer zone index then sequence index."
	if first.layerZoneIndex > second.layerZoneIndex:
		return 1
	if first.layerZoneIndex < second.layerZoneIndex:
		return - 1
	if first.sequenceIndex > second.sequenceIndex:
		return 1
	if first.sequenceIndex < second.sequenceIndex:
		return - 1
	return 0

def displayFileGivenText( fileName, gcodeText, beholdPreferences = None ):
	"Display a beholded gcode file for a gcode file."
	if gcodeText == '':
		return ''
	if beholdPreferences == None:
		beholdPreferences = BeholdPreferences()
		preferences.getReadRepository( beholdPreferences )
	skeinWindow = getWindowGivenTextPreferences( fileName, gcodeText, beholdPreferences )
	skeinWindow.updateDeiconify()

def getPolygonComplexFromColoredLines( coloredLines ):
	"Get a complex polygon from the colored lines."
	polygonComplex = []
	for coloredLine in coloredLines:
		polygonComplex.append( coloredLine.begin.dropAxis( 2 ) )
	return polygonComplex

def getRepositoryConstructor():
	"Get the repository constructor."
	return BeholdPreferences()

def getTwoHex( number ):
	"Get the first two hexadecimal digits."
	return ( '%s00' % hex( number ) )[ 2 : 4 ]

def getWindowGivenTextPreferences( fileName, gcodeText, beholdPreferences ):
	"Display the gcode text in a behold viewer."
	skein = BeholdSkein()
	skein.parseGcode( fileName, gcodeText, beholdPreferences )
	return SkeinWindow( beholdPreferences, skein )

def writeOutput( fileName, gcodeText = '' ):
	"Write a beholded gcode file for a skeinforge gcode file, if 'Activate Behold' is selected."
	beholdPreferences = BeholdPreferences()
	preferences.getReadRepository( beholdPreferences )
	if beholdPreferences.activateBehold.value:
		gcodeText = gcodec.getTextIfEmpty( fileName, gcodeText )
		displayFileGivenText( fileName, gcodeText, beholdPreferences )


class BeholdPreferences( tableau.TableauRepository ):
	"A class to handle the behold preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.phoenixUpdateFunction = None
		self.updateFunction = None
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Behold', self, '' )
		self.activateBehold = preferences.BooleanPreference().getFromValue( 'Activate Behold', self, True )
		self.bandHeight = preferences.IntPreference().getFromValue( 'Band Height (layers):', self, 5 )
		self.bandHeight.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.bottomBandBrightness = preferences.FloatPreference().getFromValue( 'Bottom Band Brightness (ratio):', self, 0.7 )
		self.bottomBandBrightness.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.bottomLayerBrightness = preferences.FloatPreference().getFromValue( 'Bottom Layer Brightness (ratio):', self, 1.0 )
		self.bottomLayerBrightness.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.brightBandStart = preferences.MenuButtonDisplay().getFromName( 'Bright Band Start:', self )
		self.fromTheBottom = preferences.MenuRadio().getFromMenuButtonDisplay( self.brightBandStart, 'From the Bottom', self, False )
		self.fromTheBottom.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.fromTheTop = preferences.MenuRadio().getFromMenuButtonDisplay( self.brightBandStart, 'From the Top', self, True )
		self.fromTheTop.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.drawArrows = preferences.BooleanPreference().getFromValue( 'Draw Arrows', self, False )
		self.drawArrows.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.exportFileExtension = preferences.StringPreference().getFromValue( 'Export File Extension:', self, '' )
		self.exportFileExtension.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.exportPostscriptProgram = preferences.StringPreference().getFromValue( 'Export Postscript Program:', self, 'gimp' )
		self.exportPostscriptProgram.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.goAroundExtruderOffTravel = preferences.BooleanPreference().getFromValue( 'Go Around Extruder Off Travel', self, False )
		self.goAroundExtruderOffTravel.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.layersFrom = preferences.IntPreference().getFromValue( 'Layers From (index):', self, 0 )
		self.layersFrom.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.layersTo = preferences.IntPreference().getFromValue( 'Layers To (index):', self, 999999999 )
		self.layersTo.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.mouseMode = preferences.MenuButtonDisplay().getFromName( 'Mouse Mode:', self )
		self.displayLine = preferences.MenuRadio().getFromMenuButtonDisplay( self.mouseMode, 'Display Line', self, True )
		self.displayLine.constructorFunction = display_line.getNewMouseTool
		self.displayLine.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.viewpointMove = preferences.MenuRadio().getFromMenuButtonDisplay( self.mouseMode, 'Viewpoint Move', self, False )
		self.viewpointMove.constructorFunction = viewpoint_move.getNewMouseTool
		self.viewpointMove.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.viewpointRotate = preferences.MenuRadio().getFromMenuButtonDisplay( self.mouseMode, 'Viewpoint Rotate', self, False )
		self.viewpointRotate.constructorFunction = viewpoint_rotate.getNewMouseTool
		self.viewpointRotate.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.numberOfFillBottomLayers = preferences.IntPreference().getFromValue( 'Number of Fill Bottom Layers (integer):', self, 1 )
		self.numberOfFillBottomLayers.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.numberOfFillTopLayers = preferences.IntPreference().getFromValue( 'Number of Fill Top Layers (integer):', self, 1 )
		self.numberOfFillTopLayers.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.scale = preferences.FloatPreference().getFromValue( 'Scale (pixels per millimeter):', self, 10.0 )
		self.scale.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.screenHorizontalInset = preferences.IntPreference().getFromValue( 'Screen Horizontal Inset (pixels):', self, 50 )
		self.screenHorizontalInset.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.screenVerticalInset = preferences.IntPreference().getFromValue( 'Screen Vertical Inset (pixels):', self, 200 )
		self.screenVerticalInset.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.viewpointLatitude = preferences.FloatPreference().getFromValue( 'Viewpoint Latitude (degrees):', self, 15.0 )
		self.viewpointLatitude.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.viewpointLongitude = preferences.FloatPreference().getFromValue( 'Viewpoint Longitude (degrees):', self, 210.0 )
		self.viewpointLongitude.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfExtrusionThread = preferences.IntPreference().getFromValue( 'Width of Extrusion Thread (pixels):', self, 1 )
		self.widthOfExtrusionThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfFillBottomThread = preferences.IntPreference().getFromValue( 'Width of Fill Bottom Thread (pixels):', self, 3 )
		self.widthOfFillBottomThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfFillTopThread = preferences.IntPreference().getFromValue( 'Width of Fill Top Thread (pixels):', self, 3 )
		self.widthOfFillTopThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfLoopThread = preferences.IntPreference().getFromValue( 'Width of Loop Thread (pixels):', self, 3 )
		self.widthOfLoopThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfPerimeterInsideThread = preferences.IntPreference().getFromValue( 'Width of Perimeter Inside Thread (pixels):', self, 4 )
		self.widthOfPerimeterInsideThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfPerimeterOutsideThread = preferences.IntPreference().getFromValue( 'Width of Perimeter Outside Thread (pixels):', self, 4 )
		self.widthOfPerimeterOutsideThread.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.raftThreadWidth = preferences.IntPreference().getFromValue( 'Width of Raft Thread (pixels):', self, 1 )
		self.raftThreadWidth.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.travelThreadWidth = preferences.IntPreference().getFromValue( 'Width of Travel Thread (pixels):', self, 0 )
		self.travelThreadWidth.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfXAxis = preferences.IntPreference().getFromValue( 'Width of X Axis (pixels):', self, 5 )
		self.widthOfXAxis.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfYAxis = preferences.IntPreference().getFromValue( 'Width of Y Axis (pixels):', self, 5 )
		self.widthOfYAxis.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.widthOfZAxis = preferences.IntPreference().getFromValue( 'Width of Z Axis (pixels):', self, 5 )
		self.widthOfZAxis.setUpdateFunction( self.setToDisplaySaveUpdate )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Behold'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.behold.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			beholdFile( fileName )


class BeholdSkein:
	"A class to write a get a scalable vector graphics text for a gcode skein."
	def __init__( self ):
		self.coloredThread = []
		self.hasASurroundingLoopBeenReached = False
		self.isLoop = False
		self.isPerimeter = False
		self.isThereALayerStartWord = False
		self.layerTops = []
		self.oldLayerZoneIndex = 0
		self.oldZ = - 999999999999.0
		self.skeinPane = None
		self.skeinPanes = []
		self.thirdLayerThickness = 0.133333

	def addToPath( self, line, location ):
		'Add a point to travel and maybe extrusion.'
		if self.oldLocation == None:
			return
		begin = self.scale * self.oldLocation - self.scaleCenterBottom
		end = self.scale * location - self.scaleCenterBottom
		tagString = '%s %s' % ( self.lineIndex + 1, line )
		coloredLine = ColoredLine( begin, '', end, tagString )
		coloredLine.z = location.z
		self.coloredThread.append( coloredLine )

	def getLayerTop( self ):
		"Get the layer top."
		if len( self.layerTops ) < 1:
			return - 9123456789123.9
		return self.layerTops[ - 1 ]

	def getLayerZoneIndex( self, z ):
		"Get the layer zone index."
		if self.layerTops[ self.oldLayerZoneIndex ] > z:
			if self.oldLayerZoneIndex == 0:
				return 0
			elif self.layerTops[ self.oldLayerZoneIndex - 1 ] < z:
				return self.oldLayerZoneIndex
		for layerTopIndex in xrange( len( self.layerTops ) ):
			layerTop = self.layerTops[ layerTopIndex ]
			if layerTop > z:
				self.oldLayerZoneIndex = layerTopIndex
				return layerTopIndex
		self.oldLayerZoneIndex = len( self.layerTops ) - 1
		return self.oldLayerZoneIndex

	def initializeActiveLocation( self ):
		"Set variables to default."
		self.extruderActive = False
		self.oldLocation = None

	def isLayerStart( self, firstWord, splitLine ):
		"Parse a gcode line and add it to the vector output."
		if self.isThereALayerStartWord:
			return firstWord == '(<layer>'
		if firstWord != 'G1' and firstWord != 'G2' and firstWord != 'G3':
			return False
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		if location.z - self.oldZ > 0.1:
			self.oldZ = location.z
			return True
		return False

	def linearCorner( self, splitLine ):
		"Update the bounding corners."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		if self.extruderActive or self.goAroundExtruderOffTravel:
			self.cornerHigh = euclidean.getPointMaximum( self.cornerHigh, location )
			self.cornerLow = euclidean.getPointMinimum( self.cornerLow, location )
		self.oldLocation = location

	def linearMove( self, line, splitLine ):
		"Get statistics for a linear move."
		if self.skeinPane == None:
			return
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.addToPath( line, location )
		self.oldLocation = location

	def moveColoredThreadToSkeinPane( self ):
		'Move a colored thread to the skein pane.'
		if len( self.coloredThread ) <= 0:
			return
		layerZoneIndex = self.getLayerZoneIndex( self.coloredThread[ 0 ].z )
		if not self.extruderActive:
			self.setColoredThread( ( 190.0, 190.0, 190.0 ), self.skeinPane.travelLines ) #gray
			return
		self.skeinPane.layerZoneIndex = layerZoneIndex
		if self.isPerimeter:
			perimeterComplex = getPolygonComplexFromColoredLines( self.coloredThread )
			if euclidean.isWiddershins( perimeterComplex ):
				self.setColoredThread( ( 255.0, 0.0, 0.0 ), self.skeinPane.perimeterOutsideLines ) #red
			else:
				self.setColoredThread( ( 255.0, 165.0, 0.0 ), self.skeinPane.perimeterInsideLines ) #orange
			return
		if self.isLoop:
			self.setColoredThread( ( 255.0, 255.0, 0.0 ), self.skeinPane.loopLines ) #yellow
			return
		if not self.hasASurroundingLoopBeenReached:
			self.setColoredThread( ( 165.0, 42.0, 42.0 ), self.skeinPane.raftLines ) #brown
			return
		if layerZoneIndex < self.beholdPreferences.numberOfFillBottomLayers.value:
			self.setColoredThread( ( 128.0, 128.0, 0.0 ), self.skeinPane.fillBottomLines ) #olive
			return
		if layerZoneIndex >= self.firstTopLayer:
			self.setColoredThread( ( 0.0, 0.0, 255.0 ), self.skeinPane.fillTopLines ) #blue
			return
		self.setColoredThread( ( 0.0, 255.0, 0.0 ), self.skeinPane.extrudeLines ) #green

	def parseCorner( self, line ):
		"Parse a gcode line and use the location to update the bounding corners."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearCorner( splitLine )
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
		elif firstWord == '(<layer>':
			self.layerTopZ = float( splitLine[ 1 ] ) + self.thirdLayerThickness
		elif firstWord == '(<layerThickness>':
			self.thirdLayerThickness = 0.33333333333 * float( splitLine[ 1 ] )
		elif firstWord == '(<surroundingLoop>)':
			if self.layerTopZ > self.getLayerTop():
				self.layerTops.append( self.layerTopZ )

	def parseGcode( self, fileName, gcodeText, beholdPreferences ):
		"Parse gcode text and store the vector output."
		self.beholdPreferences = beholdPreferences
		self.fileName = fileName
		self.gcodeText = gcodeText
		self.initializeActiveLocation()
		self.cornerHigh = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.goAroundExtruderOffTravel = beholdPreferences.goAroundExtruderOffTravel.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.isThereALayerStartWord = gcodec.isThereAFirstWord( '(<layer>', self.lines, 1 )
		for line in self.lines:
			self.parseCorner( line )
		if len( self.layerTops ) > 0:
			self.layerTops[ - 1 ] += 912345678.9
		if len( self.layerTops ) > 1:
			self.oneMinusBrightnessOverTopLayerIndex = ( 1.0 - beholdPreferences.bottomLayerBrightness.value ) / float( len( self.layerTops ) - 1 )
		self.firstTopLayer = len( self.layerTops ) - self.beholdPreferences.numberOfFillTopLayers.value
		self.centerComplex = 0.5 * ( self.cornerHigh.dropAxis( 2 ) + self.cornerLow.dropAxis( 2 ) )
		self.centerBottom = Vector3( self.centerComplex.real, self.centerComplex.imag, self.cornerLow.z )
		self.scale = beholdPreferences.scale.value
		self.scaleCenterBottom = self.scale * self.centerBottom
		self.scaleCornerHigh = self.scale * self.cornerHigh.dropAxis( 2 )
		self.scaleCornerLow = self.scale * self.cornerLow.dropAxis( 2 )
		print( "The lower left corner of the behold window is at %s, %s" % ( self.cornerLow.x, self.cornerLow.y ) )
		print( "The upper right corner of the behold window is at %s, %s" % ( self.cornerHigh.x, self.cornerHigh.y ) )
		self.cornerImaginaryTotal = self.cornerHigh.y + self.cornerLow.y
		margin = complex( 5.0, 5.0 )
		self.marginCornerLow = self.scaleCornerLow - margin
		self.screenSize = margin + 2.0 * ( self.scaleCornerHigh - self.marginCornerLow )
		self.initializeActiveLocation()
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )
		self.skeinPanes.sort( compareLayerSequence )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the vector output."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if self.isLayerStart( firstWord, splitLine ):
			self.skeinPane = SkeinPane( len( self.skeinPanes ) )
			self.skeinPanes.append( self.skeinPane )
		if firstWord == 'G1':
			self.linearMove( line, splitLine )
		elif firstWord == 'M101':
			self.moveColoredThreadToSkeinPane()
			self.extruderActive = True
		elif firstWord == 'M103':
			self.moveColoredThreadToSkeinPane()
			self.extruderActive = False
			self.isLoop = False
			self.isPerimeter = False
		elif firstWord == '(<loop>)':
			self.isLoop = True
		elif firstWord == '(<perimeter>)':
			self.isPerimeter = True
		elif firstWord == '(<surroundingLoop>)':
			self.hasASurroundingLoopBeenReached = True

	def setColoredLineColor( self, coloredLine, colorTuple ):
		'Set the color and stipple of the colored line.'
		layerZoneIndex = self.getLayerZoneIndex( coloredLine.z )
		multiplier = self.beholdPreferences.bottomLayerBrightness.value
		if len( self.layerTops ) > 1:
			multiplier += self.oneMinusBrightnessOverTopLayerIndex * float( layerZoneIndex )
		bandIndex = layerZoneIndex / self.beholdPreferences.bandHeight.value
		if self.beholdPreferences.fromTheTop.value:
			brightZoneIndex = len( self.layerTops ) - 1 - layerZoneIndex
			bandIndex = brightZoneIndex / self.beholdPreferences.bandHeight.value + 1
		if bandIndex % 2 == 0:
			multiplier *= self.beholdPreferences.bottomBandBrightness.value
		red = getTwoHex( int( colorTuple[ 0 ] * multiplier ) )
		green = getTwoHex( int( colorTuple[ 1 ] * multiplier ) )
		blue = getTwoHex( int( colorTuple[ 2 ] * multiplier ) )
		coloredLine.colorName = '#%s%s%s' % ( red, green, blue )

	def setColoredThread( self, colorTuple, lineList ):
		'Set the colored thread, then move it to the line list and stipple of the colored line.'
		for coloredLine in self.coloredThread:
			self.setColoredLineColor( coloredLine, colorTuple )
		lineList += self.coloredThread
		self.coloredThread = []


class ColoredLine:
	"A colored line."
	def __init__( self, begin, colorName, end, tagString ):
		"Set the color name and corners."
		self.begin = begin
		self.colorName = colorName
		self.end = end
		self.tagString = tagString
	
	def __repr__( self ):
		"Get the string representation of this colored line."
		return '%s, %s, %s' % ( self.colorName, self.begin, self.end, self.tagString )


class SkeinPane:
	"A class to hold the colored lines for a layer."
	def __init__( self, sequenceIndex ):
		"Create empty line lists."
		self.extrudeLines = []
		self.fillBottomLines = []
		self.fillTopLines = []
		self.layerZoneIndex = 0
		self.loopLines = []
		self.perimeterInsideLines = []
		self.perimeterOutsideLines = []
		self.raftLines = []
		self.sequenceIndex = sequenceIndex
		self.travelLines = []


class SkeinWindow( tableau.TableauWindow ):
	def __init__( self, repository, skein ):
		"Initialize the skein window."
		title = 'Behold Viewer'
		self.arrowshape = ( 24, 30, 9 )
		self.screenSize = skein.screenSize
		self.center = 0.5 * self.screenSize
		self.setMenuPanesPreferencesRootSkein( repository, skein, '_behold.ps' )
		self.motionStippleName = 'gray75'
		self.root.title( title )
		self.fileHelpMenuBar.completeMenu( self.close, repository, self )
		frame = preferences.Tkinter.Frame( self.root )
		self.xScrollbar = preferences.Tkinter.Scrollbar( self.root, orient = preferences.Tkinter.HORIZONTAL )
		self.yScrollbar = preferences.Tkinter.Scrollbar( self.root )
		self.canvasHeight = min( int( self.screenSize.imag ), self.root.winfo_screenheight() - repository.screenVerticalInset.value )
		self.canvasWidth = min( int( self.screenSize.real ), self.root.winfo_screenwidth() - repository.screenHorizontalInset.value )
		self.oneMinusCanvasHeightOverScreenHeight = 1.0 - float( self.canvasHeight ) / float( self.screenSize.imag )
		self.oneMinusCanvasWidthOverScreenWidth = 1.0 - float( self.canvasWidth ) / float( self.screenSize.real )
		self.canvas = preferences.Tkinter.Canvas( self.root, width = self.canvasWidth, height = self.canvasHeight, scrollregion = ( 0, 0, int( self.screenSize.real ), int( self.screenSize.imag ) ) )
		self.canvas.grid( row = 0, rowspan = 98, column = 0, columnspan = 99, sticky = preferences.Tkinter.W )
		self.xScrollbar.grid( row = 98, column = 0, columnspan = 99, sticky = preferences.Tkinter.E + preferences.Tkinter.W )
		self.xScrollbar.config( command = self.relayXview )
		self.yScrollbar.grid( row = 0, rowspan = 98, column = 99, sticky = preferences.Tkinter.N + preferences.Tkinter.S )
		self.yScrollbar.config( command = self.relayYview )
		self.canvas[ 'xscrollcommand' ] = self.xScrollbar.set
		self.canvas[ 'yscrollcommand' ] = self.yScrollbar.set
		preferences.CloseListener( self, self.destroyAllDialogWindows ).listenToWidget( self.canvas )
		self.setMouseToolBindButtonMotion()
		halfCenter = 0.5 * self.center.real
		self.xAxisLine = ColoredLine( Vector3(), 'darkorange', Vector3( halfCenter ), 'X Axis' )
		self.yAxisLine = ColoredLine( Vector3(), 'gold', Vector3( 0.0, halfCenter ), 'Y Axis' )
		self.zAxisLine = ColoredLine( Vector3(), 'skyblue', Vector3( 0.0, 0.0, halfCenter ), 'Z Axis' )

	def drawColoredLine( self, arrowType, coloredLine, viewVectors, width ):
		"Draw colored line."
		complexBegin = self.getViewComplex( coloredLine.begin, viewVectors )
		complexEnd = self.getViewComplex( coloredLine.end, viewVectors )
		self.canvas.create_line(
			complexBegin.real,
			complexBegin.imag,
			complexEnd.real,
			complexEnd.imag,
			fill = coloredLine.colorName,
			arrow = arrowType,
			tags = coloredLine.tagString,
			width = width )

	def drawColoredLineMotion( self, coloredLine, viewVectors, width ):
		"Draw colored line with motion stipple and tag."
		complexBegin = self.getViewComplex( coloredLine.begin, viewVectors )
		complexEnd = self.getViewComplex( coloredLine.end, viewVectors )
		self.canvas.create_line(
			complexBegin.real,
			complexBegin.imag,
			complexEnd.real,
			complexEnd.imag,
			fill = coloredLine.colorName,
			arrow = 'last',
			arrowshape = self.arrowshape,
			stipple = self.motionStippleName,
			tags = 'motion',
			width = width + 4 )

	def drawColoredLines( self, coloredLines, viewVectors, width ):
		"Draw colored lines."
		if width <= 0:
			return
		for coloredLine in coloredLines:
			self.drawColoredLine( self.arrowType, coloredLine, viewVectors, width )

	def drawSkeinPane( self, skeinPane, viewVectors ):
		"Draw colored lines."
		self.drawColoredLines( skeinPane.raftLines, viewVectors, self.repository.raftThreadWidth.value )
		self.drawColoredLines( skeinPane.travelLines, viewVectors, self.repository.travelThreadWidth.value )
		self.drawColoredLines( skeinPane.fillBottomLines, viewVectors, self.repository.widthOfFillBottomThread.value )
		self.drawColoredLines( skeinPane.extrudeLines, viewVectors, self.repository.widthOfExtrusionThread.value )
		self.drawColoredLines( skeinPane.fillTopLines, viewVectors, self.repository.widthOfFillTopThread.value )
		self.drawColoredLines( skeinPane.loopLines, viewVectors, self.repository.widthOfLoopThread.value )
		self.drawColoredLines( skeinPane.perimeterInsideLines, viewVectors, self.repository.widthOfPerimeterInsideThread.value )
		self.drawColoredLines( skeinPane.perimeterOutsideLines, viewVectors, self.repository.widthOfPerimeterOutsideThread.value )

	def drawXYAxisLines( self, viewVectors ):
		"Draw the x and y axis lines."
		if self.repository.widthOfXAxis.value > 0:
			self.drawColoredLine( 'last', self.xAxisLine, viewVectors, self.repository.widthOfXAxis.value )
		if self.repository.widthOfYAxis.value > 0:
			self.drawColoredLine( 'last', self.yAxisLine, viewVectors, self.repository.widthOfYAxis.value )

	def drawZAxisLine( self, viewVectors ):
		"Draw the z axis line."
		if self.repository.widthOfZAxis.value > 0:
			self.drawColoredLine( 'last', self.zAxisLine, viewVectors, self.repository.widthOfZAxis.value )

	def getCentered( self, coordinate ):
		"Get the centered coordinate."
		relativeToCenter = complex( coordinate.real - self.center.real, self.center.imag - coordinate.imag )
		if abs( relativeToCenter ) < 1.0:
			relativeToCenter = complex( 0.0, 1.0 )
		return relativeToCenter

	def getCenteredScreened( self, coordinate ):
		"Get the normalized centered coordinate."
		relativeToCenter = self.getCentered( coordinate )
		smallestHalfSize = 0.5 * min( float( self.canvasHeight ), float( self.canvasWidth ) )
		return relativeToCenter / smallestHalfSize

	def getScreenComplex( self, pointComplex ):
		"Get the point in screen perspective."
		return complex( pointComplex.real, - pointComplex.imag ) + self.center

	def getViewComplex( self, point, viewVectors ):
		"Get the point in view perspective."
		screenComplexX = point.dot( viewVectors.viewXVector3 )
		screenComplexY = point.dot( viewVectors.viewYVector3 )
		return self.getScreenComplex( complex( screenComplexX, screenComplexY ) )

	def phoenixUpdate( self ):
		"Update, and deiconify a new window and destroy the old."
		skeinWindow = getWindowGivenTextPreferences( self.skein.fileName, self.skein.gcodeText, self.repository )
		skeinWindow.updateDeiconify( self.getScrollPaneCenter() )
		self.root.destroy()

	def printHexadecimalColorName( self, name ):
		"Print the color name in hexadecimal."
		colorTuple = self.canvas.winfo_rgb( name )
		print( '#%s%s%s' % ( getTwoHex( colorTuple[ 0 ] ), getTwoHex( colorTuple[ 1 ] ), getTwoHex( colorTuple[ 2 ] ) ) )

	def update( self ):
		"Update the screen."
		if len( self.skeinPanes ) < 1:
			return
		self.arrowType = None
		if self.repository.drawArrows.value:
			self.arrowType = 'last'
		self.canvas.delete( preferences.Tkinter.ALL )
		self.repository.viewpointLatitude.value = viewpoint_rotate.getBoundedLatitude( self.repository.viewpointLatitude.value )
		self.repository.viewpointLongitude.value = round( self.repository.viewpointLongitude.value, 1 )
		viewVectors = viewpoint_rotate.ViewVectors( self.repository.viewpointLatitude.value, self.repository.viewpointLongitude.value )
		skeinPanesCopy = self.skeinPanes[ self.repository.layersFrom.value : self.repository.layersTo.value ]
		if viewVectors.viewpointLatitudeRatio.real > 0.0:
			self.drawXYAxisLines( viewVectors )
		else:
			skeinPanesCopy.reverse()
			self.drawZAxisLine( viewVectors )
		for skeinPane in skeinPanesCopy:
			self.drawSkeinPane( skeinPane, viewVectors )
		if viewVectors.viewpointLatitudeRatio.real > 0.0:
			self.drawZAxisLine( viewVectors )
		else:
			self.drawXYAxisLines( viewVectors )


def main():
	"Display the behold dialog."
	if len( sys.argv ) > 1:
		beholdFile( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
