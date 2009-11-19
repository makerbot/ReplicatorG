"""
Skeinview is a script to display each layer of a gcode file.

Skeinview is derived from Nophead's preview script.  The extruded lines are in the resistor colors red, orange, yellow, green, blue, purple & brown.  When the extruder is off, the travel line is grey.  Skeinview is useful for a detailed view of the extrusion, behold is better to see the orientation of the shape.  To get an initial overview of the skein, when the skeinview display window appears, click the Soar button.

The default 'Activate Skeinview' checkbox is on.  When it is on, the functions described below will work when called from the skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether or not the 'Activate Skeinview' checkbox is on, when skeinview is run directly.  Skeinview has trouble separating the layers when it reads gcode without comments.

If 'Draw Arrows' is selected, arrows will be drawn at the end of each line segment, the default is on.  If 'Go Around Extruder Off Travel' is selected, the display will include the travel when the extruder is off, which means it will include the nozzle wipe path if any.  The 'Pixels over Extrusion Width' preference is the scale of the image, the higher the number, the greater the size of the display.  The 'Screen Horizontal Inset' determines how much the display will be inset in the horizontal direction from the edge of screen, the higher the number the more it will be inset and the smaller it will be, the default is one hundred.  The 'Screen Vertical Inset' determines how much the display will be inset in the vertical direction from the edge of screen, the default is fifty.  The 'Slide Show Rate' determines how fast the layer index changes when soar or dive is operating.

When the export menu item in the file menu is clicked, the canvas will be saved as a postscript file.  If the 'Export Postscript Program' is set to a program name, the postscript file will be sent to that program to be opened.  The default is gimp, the Gnu Image Manipulation Program (Gimp), which is open source, can open postscript and save in a variety of formats.  It is available at:
http://www.gimp.org/

If the 'Export File Extension' is set to a file extension, the postscript file will be sent to the program, along with the file extension for the converted output.  The default is blank because some systems do not have an image conversion program; if you have or will install an image conversion program, a common 'Export File Extension' is png.  A good open source conversion program is Image Magick, which is available at:
http://www.imagemagick.org/script/index.php

On the skeinview display window, the Up button increases the 'Layer Index' by one, and the Down button decreases the layer index by one.  When the index displayed in the index field is changed then <return> is hit, the layer index shown will be set to the index field, to a mimimum of zero and to a maximum of the highest index layer.  The layer index can also be changed from the Preferences menu or the skeinview dialog.  The Soar button increases the layer index at the 'Slide Show Rate', and the Dive button decreases the layer index at the slide show rate.

The mouse tool can be changed from the 'Mouse Mode' menu button or item.  The 'Display Line' tool will display the line index of the line clicked, counting from one, and the line itself.  The 'Viewpoint Move' tool will move the viewpoint in the xy plane when the mouse is clicked and dragged on the canvas.

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example displays a skein view for the gcode file Screw Holder.gcode.  This example is run in a terminal in the folder which contains Screw Holder.gcode and skeinview.py.


> python skeinview.py
This brings up the skeinview dialog.


> python skeinview.py Screw Holder.gcode
This brings up a skein window to view each layer of a gcode file.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import skeinview
>>> skeinview.main()
This brings up the skeinview dialog.


>>> skeinview.displayFile()
This brings up a skein window to view each layer of a gcode file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.analyze_plugins.analyze_utilities import display_line
from skeinforge_tools.analyze_plugins.analyze_utilities import tableau
from skeinforge_tools.analyze_plugins.analyze_utilities import viewpoint_move
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import math
import os
import sys

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def displayFile( fileName ):
	"Display a gcode file in a skeinview window."
	gcodeText = gcodec.getFileText( fileName )
	displayFileGivenText( fileName, gcodeText )

def displayFileGivenText( fileName, gcodeText, skeinviewPreferences = None ):
	"Display a gcode file in a skeinview window given the text."
	if gcodeText == '':
		return
	if skeinviewPreferences == None:
		skeinviewPreferences = SkeinviewPreferences()
		preferences.getReadRepository( skeinviewPreferences )
	skeinWindow = getWindowGivenTextPreferences( fileName, gcodeText, skeinviewPreferences )
	skeinWindow.updateDeiconify()

def getGeometricDifference( first, second ):
	"Get the geometric difference of the two numbers."
	return max( first, second ) / min( first, second )

def getRepositoryConstructor():
	"Get the repository constructor."
	return SkeinviewPreferences()

def getRankIndex( rulingSeparationWidthMillimeters, screenOrdinate ):
	"Get rank index."
	return int( round( screenOrdinate / rulingSeparationWidthMillimeters ) )

def getRulingSeparationWidthMillimeters( rank ):
	"Get the separation width in millimeters."
	rankZone = int( math.floor( rank / 3 ) )
	rankModulo = rank % 3
	powerOfTen = pow( 10, rankZone )
	moduloMultipliers = ( 1, 2, 5 )
	separationWidthMillimeters = float( powerOfTen * moduloMultipliers[ rankModulo ] )
	return separationWidthMillimeters

def getWindowGivenTextPreferences( fileName, gcodeText, skeinviewPreferences ):
	"Display a gcode file in a skeinview window given the text and preferences."
	skein = SkeinviewSkein()
	skein.parseGcode( fileName, gcodeText, skeinviewPreferences )
	return SkeinWindow( skeinviewPreferences, skein )

def writeOutput( fileName, gcodeText = '' ):
	"Display a skeinviewed gcode file for a skeinforge gcode file, if 'Activate Skeinview' is selected."
	skeinviewPreferences = SkeinviewPreferences()
	preferences.getReadRepository( skeinviewPreferences )
	if skeinviewPreferences.activateSkeinview.value:
		gcodeText = gcodec.getTextIfEmpty( fileName, gcodeText )
		displayFileGivenText( fileName, gcodeText, skeinviewPreferences )


class ColoredIndexLine:
	"A colored index line."
	def __init__( self, colorName, complexBegin, complexEnd, line, lineIndex, width ):
		"Set the color name and corners."
		self.colorName = colorName
		self.complexBegin = complexBegin
		self.complexEnd = complexEnd
		self.line = line
		self.lineIndex = lineIndex
		self.width = width
	
	def __repr__( self ):
		"Get the string representation of this colored index line."
		return '%s, %s, %s, %s' % ( self.colorName, self.complexBegin, self.complexEnd, self.line, self.lineIndex, self.width )


class SkeinviewPreferences( tableau.TableauRepository ):
	"A class to handle the skeinview preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.phoenixUpdateFunction = None
		self.updateFunction = None
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Skeinview', self, '' )
		self.activateSkeinview = preferences.BooleanPreference().getFromValue( 'Activate Skeinview', self, True )
		self.drawArrows = preferences.BooleanPreference().getFromValue( 'Draw Arrows', self, True )
		self.drawArrows.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.exportFileExtension = preferences.StringPreference().getFromValue( 'Export File Extension:', self, '' )
		self.exportFileExtension.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.exportPostscriptProgram = preferences.StringPreference().getFromValue( 'Export Postscript Program:', self, 'gimp' )
		self.exportPostscriptProgram.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.layerIndex = preferences.IntPreference().getFromValue( 'Layer Index (integer):', self, 1 )
		self.layerIndex.setUpdateFunction( self.setToDisplaySaveUpdate )
		self.goAroundExtruderOffTravel = preferences.BooleanPreference().getFromValue( 'Go Around Extruder Off Travel', self, False )
		self.goAroundExtruderOffTravel.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.mouseMode = preferences.MenuButtonDisplay().getFromName( 'Mouse Mode:', self )
		self.displayLine = preferences.MenuRadio().getFromMenuButtonDisplay( self.mouseMode, 'Display Line', self, True )
		self.displayLine.constructorFunction = display_line.getNewMouseTool
		self.displayLine.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.viewpointMove = preferences.MenuRadio().getFromMenuButtonDisplay( self.mouseMode, 'Viewpoint Move', self, False )
		self.viewpointMove.constructorFunction = viewpoint_move.getNewMouseTool
		self.viewpointMove.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.scale = preferences.FloatPreference().getFromValue( 'Scale (pixels per millimeter):', self, 10.0 )
		self.scale.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.screenHorizontalInset = preferences.IntPreference().getFromValue( 'Screen Horizontal Inset (pixels):', self, 50 )
		self.screenHorizontalInset.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.screenVerticalInset = preferences.IntPreference().getFromValue( 'Screen Vertical Inset (pixels):', self, 200 )
		self.screenVerticalInset.setUpdateFunction( self.setToDisplaySavePhoenixUpdate )
		self.slideShowRate = preferences.FloatPreference().getFromValue( 'Slide Show Rate (layers/second):', self, 1.0 )
		self.slideShowRate.setUpdateFunction( self.setToDisplaySaveUpdate )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Skeinview'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.skeinview.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			displayFile( fileName )


class SkeinviewSkein:
	"A class to write a get a scalable vector graphics text for a gcode skein."
	def __init__( self ):
		self.extrusionNumber = 0
		self.isThereALayerStartWord = False
		self.oldZ = - 999999999999.0
		self.skeinPane = None
		self.skeinPanes = []

	def addToPath( self, line, location ):
		"Add a point to travel and maybe extrusion."
		if self.oldLocation == None:
			return
		oldLocationComplex = self.oldLocation.dropAxis( 2 )
		locationComplex = location.dropAxis( 2 )
		colorName = 'gray'
		width = 1
		if self.extruderActive:
			colorName = self.colorNames[ self.extrusionNumber % len( self.colorNames ) ]
			width = 2
		coloredIndexLine = ColoredIndexLine( colorName, self.getScreenCoordinates( oldLocationComplex ), self.getScreenCoordinates( locationComplex ), line, self.lineIndex, width )
		self.skeinPane.append( coloredIndexLine )

	def getScreenCoordinates( self, pointComplex ):
		"Get the screen coordinates.self.cornerImaginaryTotal - self.marginCornerLow"
		pointComplex = complex( pointComplex.real, self.cornerImaginaryTotal - pointComplex.imag )
		return self.scale * pointComplex - self.marginCornerLow

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

	def parseGcode( self, fileName, gcodeText, skeinviewPreferences ):
		"Parse gcode text and store the vector output."
		self.fileName = fileName
		self.gcodeText = gcodeText
		self.initializeActiveLocation()
		self.cornerHigh = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.goAroundExtruderOffTravel = skeinviewPreferences.goAroundExtruderOffTravel.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.isThereALayerStartWord = gcodec.isThereAFirstWord( '(<layer>', self.lines, 1 )
		for line in self.lines:
			self.parseCorner( line )
		self.cornerHighComplex = self.cornerHigh.dropAxis( 2 )
		self.cornerLowComplex = self.cornerLow.dropAxis( 2 )
		self.scale = skeinviewPreferences.scale.value
		self.scaleCornerHigh = self.scale * self.cornerHighComplex
		self.scaleCornerLow = self.scale * self.cornerLowComplex
		self.cornerImaginaryTotal = self.cornerHigh.y + self.cornerLow.y
		self.margin = complex( 10.0, 10.0 )
		self.marginCornerHigh = self.scaleCornerHigh + self.margin
		self.marginCornerLow = self.scaleCornerLow - self.margin
		self.screenSize = self.marginCornerHigh - self.marginCornerLow
		self.initializeActiveLocation()
		self.colorNames = [ 'brown', 'red', 'orange', 'yellow', 'green', 'blue', 'purple' ]
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the vector output."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if self.isLayerStart( firstWord, splitLine ):
			self.extrusionNumber = 0
			self.skeinPane = []
			self.skeinPanes.append( self.skeinPane )
		if firstWord == 'G1':
			self.linearMove( line, splitLine )
		elif firstWord == 'M101':
			self.extruderActive = True
			self.extrusionNumber += 1
		elif firstWord == 'M103':
			self.extruderActive = False


class SkeinWindow( tableau.TableauWindow ):
	def __init__( self, repository, skein ):
		title = 'Skeinview Viewer from Hydraraptor'
		self.rulingExtent = 24
		self.rulingTargetSeparation = 150.0
		self.screenSize = skein.screenSize
		self.setMenuPanesPreferencesRootSkein( repository, skein, '_skeinview.ps' )
		self.column = 0
		self.imagesDirectoryPath = os.path.join( preferences.getSkeinforgeDirectoryPath(), 'images' )
		self.photoImages = {}
		self.row = 99
		self.root.title( title )
		repository.slideShowRate.value = max( repository.slideShowRate.value, 0.01 )
		repository.slideShowRate.value = min( repository.slideShowRate.value, 85.0 )
		self.timerID = None
		self.fileHelpMenuBar.completeMenu( self.close, repository, self )
		frame = preferences.Tkinter.Frame( self.root )
		self.xScrollbar = preferences.Tkinter.Scrollbar( self.root, orient = preferences.Tkinter.HORIZONTAL )
		self.yScrollbar = preferences.Tkinter.Scrollbar( self.root )
		self.canvasHeight = min( int( skein.screenSize.imag ), self.root.winfo_screenheight() - repository.screenVerticalInset.value )
		self.canvasWidth = min( int( skein.screenSize.real ), self.root.winfo_screenwidth() - repository.screenHorizontalInset.value )
		self.oneMinusCanvasHeightOverScreenHeight = 1.0 - float( self.canvasHeight ) / float( self.screenSize.imag )
		self.oneMinusCanvasWidthOverScreenWidth = 1.0 - float( self.canvasWidth ) / float( self.screenSize.real )
		scrollRegionBoundingBox = ( 0, 0, int( skein.screenSize.real ), int( skein.screenSize.imag ) )
		self.xScrollbar.grid( row = 98, column = 1, columnspan = 97, sticky = preferences.Tkinter.E + preferences.Tkinter.W )
		self.xScrollbar.config( command = self.relayXview )
		self.yScrollbar.grid( row = 1, rowspan = 97, column = 99, sticky = preferences.Tkinter.N + preferences.Tkinter.S )
		self.yScrollbar.config( command = self.relayYview )
		self.canvas = preferences.Tkinter.Canvas( self.root, width = self.canvasWidth, height = self.canvasHeight, scrollregion = scrollRegionBoundingBox )
		self.canvas.grid( row = 1, rowspan = 97, column = 1, columnspan = 97, sticky = preferences.Tkinter.W )
		self.canvas[ 'xscrollcommand' ] = self.xScrollbar.set
		self.canvas[ 'yscrollcommand' ] = self.yScrollbar.set
		preferences.CloseListener( self, self.destroyAllDialogWindows ).listenToWidget( self.canvas )
		horizontalRulerBoundingBox = ( 0, 0, int( skein.screenSize.real ), self.rulingExtent )
		self.horizontalRulerCanvas = preferences.Tkinter.Canvas( self.root, width = self.canvasWidth, height = self.rulingExtent, scrollregion = horizontalRulerBoundingBox )
		self.horizontalRulerCanvas.grid( row = 0, column = 1, columnspan = 97, sticky = preferences.Tkinter.E + preferences.Tkinter.W )
		self.horizontalRulerCanvas[ 'xscrollcommand' ] = self.xScrollbar.set
		self.photoImages[ 'stop' ] = preferences.Tkinter.PhotoImage( file = os.path.join( self.imagesDirectoryPath, 'stop.ppm' ), master = self.root )
		self.diveButton = self.getPhotoButtonGridIncrement( self.dive, 'dive.ppm' )
		self.downButton = self.getPhotoButtonGridIncrement( self.down, 'down.ppm' )
		self.indexEntry = preferences.Tkinter.Entry( self.root )
		self.indexEntry.bind( '<Return>', self.indexEntryReturnPressed )
		self.indexEntry.grid( row = self.row, column = self.column, sticky = preferences.Tkinter.W )
		self.column += 1
		self.upButton = self.getPhotoButtonGridIncrement( self.up, 'up.ppm' )
		self.soarButton = self.getPhotoButtonGridIncrement( self.soar, 'soar.ppm' )
#		self.zoomInImage = preferences.Tkinter.PhotoImage( master = self.root, file = os.path.join( imagesDirectoryPath, 'zoom_in.ppm' ) )
#		self.zoomInButton = preferences.Tkinter.Button( self.root, activebackground = 'black', command = self.zoomIn, image = self.zoomInImage )
#		self.zoomInButton.grid( row = 99, column = 17, sticky = preferences.Tkinter.W )
		self.resetPeriodicButtonsText()
		verticalRulerBoundingBox = ( 0, 0, self.rulingExtent, int( skein.screenSize.imag ) )
		self.verticalRulerCanvas = preferences.Tkinter.Canvas( self.root, width = self.rulingExtent, height = self.canvasHeight, scrollregion = verticalRulerBoundingBox )
		self.verticalRulerCanvas.grid( row = 1, rowspan = 97, column = 0, sticky = preferences.Tkinter.N + preferences.Tkinter.S )
		self.verticalRulerCanvas[ 'yscrollcommand' ] = self.yScrollbar.set
		self.setMouseToolBindButtonMotion()
		self.createRulers()

	def addHorizontalRulerRuling( self, xMillimeters ):
		"Add a ruling to the horizontal ruler."
		xPixel = self.skein.getScreenCoordinates( complex( xMillimeters, 0.0 ) ).real
		self.horizontalRulerCanvas.create_line( xPixel, 0.0, xPixel, self.rulingExtent, fill = 'black' )
		self.horizontalRulerCanvas.create_text( xPixel + 2, 0, anchor = preferences.Tkinter.NW, text = self.getRoundedRulingText( xMillimeters ) )

	def addVerticalRulerRuling( self, yMillimeters ):
		"Add a ruling to the vertical ruler."
		fontHeight = 12
		yPixel = self.skein.getScreenCoordinates( complex( 0.0, yMillimeters ) ).imag
		self.verticalRulerCanvas.create_line( 0.0, yPixel, self.rulingExtent, yPixel, fill = 'black' )
		yPixel += 2
		roundedRulingText = self.getRoundedRulingText( yMillimeters )
		effectiveRulingTextLength = len( roundedRulingText )
		if roundedRulingText.find( '.' ) != - 1:
			effectiveRulingTextLength -= 1
		if effectiveRulingTextLength < 4:
			self.verticalRulerCanvas.create_text( 0, yPixel, anchor = preferences.Tkinter.NW, text = roundedRulingText )
			return
		for character in roundedRulingText:
			if character == '.':
				yPixel -= fontHeight * 2 / 3
			self.verticalRulerCanvas.create_text( 0, yPixel, anchor = preferences.Tkinter.NW, text = character )
			yPixel += fontHeight

	def cancelTimer( self ):
		"Cancel the timer and set it to none."
		if self.timerID != None:
			self.canvas.after_cancel ( self.timerID )
			self.timerID = None

	def cancelTimerResetButtons( self ):
		"Cancel the timer and set it to none."
		self.cancelTimer()
		self.resetPeriodicButtonsText()

	def createRulers( self ):
		"Create the rulers.."
		rankZeroSeperation = self.getRulingSeparationWidthPixels( 0 )
		zoom = self.rulingTargetSeparation / rankZeroSeperation
		floatRank = 3.0 * math.log10( zoom )
		rank = int( math.floor( floatRank ) )
		rankTop = rank + 1
		seperationBottom = self.getRulingSeparationWidthPixels( rank )
		seperationTop = self.getRulingSeparationWidthPixels( rankTop )
		bottomDifference = getGeometricDifference( self.rulingTargetSeparation, seperationBottom )
		topDifference = getGeometricDifference( self.rulingTargetSeparation, seperationTop )
		if topDifference < bottomDifference:
			rank = rankTop
		self.rulingSeparationWidthMillimeters = getRulingSeparationWidthMillimeters( rank )
		rulingSeparationWidthPixels = self.getRulingSeparationWidthPixels( rank )
		marginOverScale = self.skein.margin / self.skein.scale
		cornerHighMargin = self.skein.cornerHighComplex + marginOverScale
		cornerLowMargin = self.skein.cornerLowComplex - marginOverScale
		xRankIndexHigh = getRankIndex( self.rulingSeparationWidthMillimeters, cornerHighMargin.real )
		xRankIndexLow = getRankIndex( self.rulingSeparationWidthMillimeters, cornerLowMargin.real )
		for xRankIndex in xrange( xRankIndexLow - 2, xRankIndexHigh + 2 ): # 1 is enough, 2 is to be on the safe side
			self.addHorizontalRulerRuling( xRankIndex * self.rulingSeparationWidthMillimeters )
		yRankIndexHigh = getRankIndex( self.rulingSeparationWidthMillimeters, cornerHighMargin.imag )
		yRankIndexLow = getRankIndex( self.rulingSeparationWidthMillimeters, cornerLowMargin.imag )
		for yRankIndex in xrange( yRankIndexLow - 2, yRankIndexHigh + 2 ): # 1 is enough, 2 is to be on the safe side
			self.addVerticalRulerRuling( yRankIndex * self.rulingSeparationWidthMillimeters )

	def dive( self ):
		"Dive, go up periodically."
		oldDiveButtonText = self.diveButton[ 'text' ]
		self.cancelTimerResetButtons()
		if oldDiveButtonText == 'stop':
			return
		self.diveCycle()

	def diveCycle( self ):
		"Start the dive cycle."
		self.cancelTimer()
		self.repository.layerIndex.value -= 1
		self.saveUpdate()
		if self.repository.layerIndex.value < 1:
			self.resetPeriodicButtonsText()
			return
		self.diveButton[ 'image' ] = self.photoImages[ 'stop' ]
		self.diveButton[ 'text' ] = 'stop'
		self.timerID = self.canvas.after( self.getSlideShowDelay(), self.diveCycle )

	def down( self ):
		"Go down a layer."
		self.cancelTimerResetButtons()
		self.repository.layerIndex.value -= 1
		self.saveUpdate()

	def getPhotoButtonGridIncrement( self, commandFunction, fileName ):
		"Get a PhotoImage button, grid the button and increment the grid position."
		photoImage = preferences.Tkinter.PhotoImage( file = os.path.join( self.imagesDirectoryPath, fileName ), master = self.root )
		untilDotFileName = gcodec.getUntilDot( fileName )
		self.photoImages[ untilDotFileName ] = photoImage
		photoButton = preferences.Tkinter.Button( self.root, activebackground = 'black', command = commandFunction, image = photoImage, text = untilDotFileName )
		photoButton.grid( row = self.row, column = self.column, sticky = preferences.Tkinter.W )
		self.column += 1
		return photoButton

	def getRoundedRulingText( self, number ):
		"Get the rounded ruling text."
		rulingText = euclidean.getRoundedToDecimalPlacesString( 1 - math.floor( math.log10( self.rulingSeparationWidthMillimeters ) ), number )
		if self.rulingSeparationWidthMillimeters < .99:
			return rulingText
		if rulingText[ - len( '.0' ) : ] == '.0':
			return rulingText[ : - len( '.0' ) ]
		return rulingText

	def getRulingSeparationWidthPixels( self, rank ):
		"Get the separation width in pixels."
		return getRulingSeparationWidthMillimeters( rank ) * self.skein.scale

	def getSlideShowDelay( self ):
		"Get the slide show delay in milliseconds."
		slideShowDelay = int( round( 1000.0 / self.repository.slideShowRate.value ) )
		return max( slideShowDelay, 1 )

	def indexEntryReturnPressed( self, event ):
		"The index entry return was pressed."
		self.cancelTimerResetButtons()
		self.repository.layerIndex.value = int( self.indexEntry.get() )
		self.limitIndex()
		self.saveUpdate()

	def limitIndex( self ):
		"Limit the index so it is not below zero or above the top."
		self.repository.layerIndex.value = max( 0, self.repository.layerIndex.value )
		self.repository.layerIndex.value = min( len( self.skeinPanes ) - 1, self.repository.layerIndex.value )

	def phoenixUpdate( self ):
		"Update, and deiconify a new window and destroy the old."
		skeinWindow = getWindowGivenTextPreferences( self.skein.fileName, self.skein.gcodeText, self.repository )
		skeinWindow.index = self.repository.layerIndex.value
		skeinWindow.updateDeiconify( self.getScrollPaneCenter() )
		self.root.destroy()

	def relayXview( self, *args ):
		"Relay xview changes."
		self.canvas.xview( *args )
		self.horizontalRulerCanvas.xview( *args )

	def relayYview( self, *args ):
		"Relay yview changes."
		self.canvas.yview( *args )
		self.verticalRulerCanvas.yview( *args )

	def resetPeriodicButtonsText( self ):
		"Reset the text of the periodic buttons."
		self.diveButton[ 'image' ] = self.photoImages[ 'dive' ]
		self.diveButton[ 'text' ] = 'dive'
		self.soarButton[ 'image' ] = self.photoImages[ 'soar' ]
		self.soarButton[ 'text' ] = 'soar'

	def soar( self ):
		"Soar, go up periodically."
		oldSoarButtonText = self.soarButton[ 'text' ]
		self.cancelTimerResetButtons()
		if oldSoarButtonText == 'stop':
			return
		self.soarCycle()

	def soarCycle( self ):
		"Start the soar cycle."
		self.cancelTimer()
		self.repository.layerIndex.value += 1
		self.saveUpdate()
		if self.repository.layerIndex.value > len( self.skeinPanes ) - 2:
			self.resetPeriodicButtonsText()
			return
		self.soarButton[ 'image' ] = self.photoImages[ 'stop' ]
		self.soarButton[ 'text' ] = 'stop'
		self.timerID = self.canvas.after( self.getSlideShowDelay(), self.soarCycle )

	def up( self ):
		"Go up a layer."
		self.cancelTimerResetButtons()
		self.repository.layerIndex.value += 1
		self.saveUpdate()

	def zoomIn( self ):
		"Zoom in."
		print('zzz')
		self.zoomInButton[ 'relief' ] = preferences.Tkinter.SUNKEN

	def update( self ):
		"Update the window."
		if len( self.skeinPanes ) < 1:
			return
		self.limitIndex()
		self.arrowType = None
		if self.repository.drawArrows.value:
			self.arrowType = 'last'
		skeinPane = self.skeinPanes[ self.repository.layerIndex.value ]
		self.canvas.delete( preferences.Tkinter.ALL )
		for coloredIndexLine in skeinPane:
			complexBegin = coloredIndexLine.complexBegin
			complexEnd = coloredIndexLine.complexEnd
			self.canvas.create_line(
				complexBegin.real,
				complexBegin.imag,
				complexEnd.real,
				complexEnd.imag,
				fill = coloredIndexLine.colorName,
				arrow = self.arrowType,
				tags = '%s %s' % ( coloredIndexLine.lineIndex + 1, coloredIndexLine.line ),
				width = coloredIndexLine.width )
		if self.repository.layerIndex.value < len( self.skeinPanes ) - 1:
			self.soarButton.config( state = preferences.Tkinter.NORMAL )
			self.upButton.config( state = preferences.Tkinter.NORMAL )
		else:
			self.soarButton.config( state = preferences.Tkinter.DISABLED )
			self.upButton.config( state = preferences.Tkinter.DISABLED )
		if self.repository.layerIndex.value > 0:
			self.downButton.config( state = preferences.Tkinter.NORMAL )
			self.diveButton.config( state = preferences.Tkinter.NORMAL )
		else:
			self.downButton.config( state = preferences.Tkinter.DISABLED )
			self.diveButton.config( state = preferences.Tkinter.DISABLED )
		self.indexEntry.delete( 0, preferences.Tkinter.END )
		self.indexEntry.insert( 0, str( self.repository.layerIndex.value ) )


def main():
	"Display the skeinview dialog."
	if len( sys.argv ) > 1:
		displayFile( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
