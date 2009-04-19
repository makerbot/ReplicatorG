"""
Skeinview is a script to display each layer of a gcode file.

The default 'Activate Skeinview' checkbox is on.  When it is on, the functions described below will work when called from the
skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether
or not the 'Activate Skeinview' checkbox is on, when skeinview is run directly.  Skeinview has trouble separating the layers
when it reads gcode without comments.

If "Draw Arrows" is selected, arrows will be drawn at the end of each line segment, the default is on.  If "Go Around Extruder
Off Travel" is selected, the display will include the travel when the extruder is off, which means it will include the nozzle wipe
path if any.  The "Pixels over Extrusion Width" preference is the scale of the image, the higher the number, the greater the
size of the display.  The "Screen Horizontal Inset" determines how much the display will be inset in the horizontal direction
from the edge of screen, the higher the number the more it will be inset and the smaller it will be, the default is one hundred.
The "Screen Vertical Inset" determines how much the display will be inset in the vertical direction from the edge of screen,
the default is fifty.

On the skeinview display window, the up button increases the layer index shown by one, and the down button decreases the
layer index by one.  When the index displayed in the index field is changed then "<return>" is hit, the layer index shown will
be set to the index field, to a mimimum of zero and to a maximum of the highest index layer.

To run skeinview, in a shell in the folder which skeinview is in type:
> python skeinview.py

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example displays a skein view for the gcode file Screw Holder.gcode.  This example is run in a terminal in the folder which
contains Screw Holder.gcode and skeinview.py.


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


>>> skeinview.skeinviewFile()
This brings up a skein window to view each layer of a gcode file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import polyfile
import cStringIO
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def displaySkeinviewFileGivenText( gcodeText, skeinviewPreferences = None ):
	"Display a skeinviewed gcode file for a gcode file."
	if gcodeText == '':
		return ''
	if skeinviewPreferences == None:
		skeinviewPreferences = SkeinviewPreferences()
		preferences.readPreferences( skeinviewPreferences )
	skein = SkeinviewSkein()
	skein.parseGcode( gcodeText, skeinviewPreferences )
	SkeinWindow( skein.arrowType, skeinviewPreferences.screenHorizontalInset.value, skeinviewPreferences.screenVerticalInset.value, skein.scaleSize, skein.skeinPanes )

def skeinviewFile( fileName = '' ):
	"Skeinview a gcode file.  If no fileName is specified, skeinview the first gcode file in this folder that is not modified."
	if fileName == '':
		unmodified = gcodec.getUnmodifiedGCodeFiles()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	gcodeText = gcodec.getFileText( fileName )
	displaySkeinviewFileGivenText( gcodeText )

def writeOutput( fileName, gcodeText = '' ):
	"Write a skeinviewed gcode file for a skeinforge gcode file, if 'Activate Skeinview' is selected."
	skeinviewPreferences = SkeinviewPreferences()
	preferences.readPreferences( skeinviewPreferences )
	if skeinviewPreferences.activateSkeinview.value:
		if gcodeText == '':
			gcodeText = gcodec.getFileText( fileName )
		displaySkeinviewFileGivenText( gcodeText, skeinviewPreferences )


class ColoredLine:
	"A colored line."
	def __init__( self, colorName, complexBegin, complexEnd, line, lineIndex, width ):
		"Set the color name and corners."
		self.colorName = colorName
		self.complexBegin = complexBegin
		self.complexEnd = complexEnd
		self.line = line
		self.lineIndex = lineIndex
		self.width = width
	
	def __repr__( self ):
		"Get the string representation of this colored line."
		return '%s, %s, %s, %s' % ( self.colorName, self.complexBegin, self.complexEnd, self.line, self.lineIndex, self.width )


class SkeinviewPreferences:
	"A class to handle the skeinview preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive = []
		self.activateSkeinview = preferences.BooleanPreference().getFromValue( 'Activate Skeinview', True )
		self.archive.append( self.activateSkeinview )
		self.drawArrows = preferences.BooleanPreference().getFromValue( 'Draw Arrows', True )
		self.archive.append( self.drawArrows )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Skeinview', '' )
		self.archive.append( self.fileNameInput )
		self.goAroundExtruderOffTravel = preferences.BooleanPreference().getFromValue( 'Go Around Extruder Off Travel', False )
		self.archive.append( self.goAroundExtruderOffTravel )
		self.pixelsWidthExtrusion = preferences.FloatPreference().getFromValue( 'Pixels over Extrusion Width (ratio):', 10.0 )
		self.archive.append( self.pixelsWidthExtrusion )
		self.screenHorizontalInset = preferences.IntPreference().getFromValue( 'Screen Horizontal Inset (pixels):', 100 )
		self.archive.append( self.screenHorizontalInset )
		self.screenVerticalInset = preferences.IntPreference().getFromValue( 'Screen Vertical Inset (pixels):', 50 )
		self.archive.append( self.screenVerticalInset )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Skeinview'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.skeinview.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			skeinviewFile( fileName )


class SkeinviewSkein:
	"A class to write a get a scalable vector graphics text for a gcode skein."
	def __init__( self ):
		self.extrusionNumber = 0
		self.extrusionWidth = 0.6
		self.isThereALayerStartWord = False
		self.oldZ = - 999999999999.0
		self.skeinPanes = []

	def addToPath( self, line, location ):
		"Add a point to travel and maybe extrusion."
		if self.oldLocation == None:
			return
		beginningComplex = complex( self.oldLocation.x, self.cornerImaginaryTotal - self.oldLocation.y )
		endComplex = complex( location.x, self.cornerImaginaryTotal - location.y )
		colorName = 'gray'
		width = 1
		if self.extruderActive:
			colorName = self.colorNames[ self.extrusionNumber % len( self.colorNames ) ]
			width = 2
		coloredLine = ColoredLine( colorName, self.scale * beginningComplex - self.marginCornerLow, self.scale * endComplex - self.marginCornerLow, line, self.lineIndex, width )
		self.skeinPane.append( coloredLine )

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
		elif firstWord == '(<extrusionWidth>':
			self.extrusionWidth = float( splitLine[ 1 ] )

	def parseGcode( self, gcodeText, skeinviewPreferences ):
		"Parse gcode text and store the vector output."
		self.arrowType = None
		if skeinviewPreferences.drawArrows.value:
			self.arrowType = 'last'
		self.initializeActiveLocation()
		self.cornerHigh = Vector3( - 999999999.0, - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		self.goAroundExtruderOffTravel = skeinviewPreferences.goAroundExtruderOffTravel.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.isThereALayerStartWord = gcodec.isThereAFirstWord( '(<layer>', self.lines, 1 )
		for line in self.lines:
			self.parseCorner( line )
		self.scale = skeinviewPreferences.pixelsWidthExtrusion.value / abs( self.extrusionWidth )
		self.scaleCornerHigh = self.scale * self.cornerHigh.dropAxis( 2 )
		self.scaleCornerLow = self.scale * self.cornerLow.dropAxis( 2 )
		print( "The lower left corner of the skeinview window is at %s, %s" % ( self.cornerLow.x, self.cornerLow.y ) )
		print( "The upper right corner of the skeinview window is at %s, %s" % ( self.cornerHigh.x, self.cornerHigh.y ) )
		self.cornerImaginaryTotal = self.cornerHigh.y + self.cornerLow.y
		margin = complex( 5.0, 5.0 )
		self.marginCornerLow = self.scaleCornerLow - margin
		self.scaleSize = margin + self.scaleCornerHigh - self.marginCornerLow
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


class SkeinWindow:
	def __init__( self, arrowType, screenHorizontalInset, screenVerticalInset, size, skeinPanes ):
		self.arrowType = arrowType
		self.index = 0
		self.skeinPanes = skeinPanes
		self.root = preferences.Tkinter.Tk()
		self.root.title( "Skeinview from HydraRaptor" )
		frame = preferences.Tkinter.Frame( self.root )
		xScrollbar = preferences.Tkinter.Scrollbar( self.root, orient = preferences.Tkinter.HORIZONTAL )
		yScrollbar = preferences.Tkinter.Scrollbar( self.root )
		canvasHeight = min( int( size.imag ), self.root.winfo_screenheight() - screenHorizontalInset )
		canvasWidth = min( int( size.real ), self.root.winfo_screenwidth() - screenVerticalInset )
		self.canvas = preferences.Tkinter.Canvas( self.root, width = canvasWidth, height = canvasHeight, scrollregion = ( 0, 0, int( size.real ), int( size.imag ) ) )
		self.canvas.grid( row = 0, rowspan = 98, column = 0, columnspan = 99, sticky = preferences.Tkinter.W )
		xScrollbar.grid( row = 98, column = 0, columnspan = 99, sticky = preferences.Tkinter.E + preferences.Tkinter.W )
		xScrollbar.config( command = self.canvas.xview )
		yScrollbar.grid( row = 0, rowspan = 98, column = 99, sticky = preferences.Tkinter.N + preferences.Tkinter.S )
		yScrollbar.config( command = self.canvas.yview )
		self.canvas[ 'xscrollcommand' ] = xScrollbar.set
		self.canvas[ 'yscrollcommand' ] = yScrollbar.set
		self.exitButton = preferences.Tkinter.Button( self.root, text = 'Exit', activebackground = 'black', activeforeground = 'red', command = self.root.quit, fg = 'red' )
		self.exitButton.grid( row = 99, column = 95, columnspan = 5, sticky = preferences.Tkinter.W )
		self.downButton = preferences.Tkinter.Button( self.root, activebackground = 'black', activeforeground = 'purple', command = self.down, text = 'Down \\/' )
		self.downButton.grid( row = 99, column = 0, sticky = preferences.Tkinter.W )
		self.upButton = preferences.Tkinter.Button( self.root, activebackground = 'black', activeforeground = 'purple', command = self.up, text = 'Up /\\' )
		self.upButton.grid( row = 99, column = 1, sticky = preferences.Tkinter.W )
		self.indexEntry = preferences.Tkinter.Entry( self.root )
		self.indexEntry.bind( '<Return>', self.indexEntryReturnPressed )
		self.indexEntry.grid( row = 99, column = 2, columnspan = 10, sticky = preferences.Tkinter.W )
		self.canvas.bind('<Button-1>', self.buttonOneClicked )
		self.update()
		if preferences.globalIsMainLoopRunning:
			return
		preferences.globalIsMainLoopRunning = True
		self.root.mainloop()
		preferences.globalIsMainLoopRunning = False

	def buttonOneClicked( self, event ):
		x = self.canvas.canvasx( event.x )
		y = self.canvas.canvasx( event.y )
		tags = self.canvas.itemcget( self.canvas.find_closest( x, y ), 'tags' )
		currentEnd = ' current'
		if tags.find( currentEnd ) != - 1:
			tags = tags[ : - len( currentEnd ) ]
		if len( tags ) > 0:
			print( tags )

	def down( self ):
		self.index -= 1
		self.update()

	def indexEntryReturnPressed( self, event ):
		self.index = int( self.indexEntry.get() )
		self.index = max( 0, self.index )
		self.index = min( len( self.skeinPanes ) - 1, self.index )
		self.update()

	def up( self ):
		self.index += 1
		self.update()

	def update( self ):
		if len( self.skeinPanes ) < 1:
			return
		skeinPane = self.skeinPanes[ self.index ]
		self.canvas.delete( preferences.Tkinter.ALL )
		for coloredLine in skeinPane:
			complexBegin = coloredLine.complexBegin
			complexEnd = coloredLine.complexEnd
			self.canvas.create_line(
				complexBegin.real,
				complexBegin.imag,
				complexEnd.real,
				complexEnd.imag,
				fill = coloredLine.colorName,
				arrow = self.arrowType,
				tags = 'The line clicked is: %s %s' % ( coloredLine.lineIndex, coloredLine.line ),
				width = coloredLine.width )
		if self.index < len( self.skeinPanes ) - 1:
			self.upButton.config( state = preferences.Tkinter.NORMAL )
		else:
			self.upButton.config( state = preferences.Tkinter.DISABLED )
		if self.index > 0:
			self.downButton.config( state = preferences.Tkinter.NORMAL )
		else:
			self.downButton.config( state = preferences.Tkinter.DISABLED )
		self.indexEntry.delete( 0, preferences.Tkinter.END )
		self.indexEntry.insert( 0, str( self.index ) )


def main():
	"Display the skeinview dialog."
	if len( sys.argv ) > 1:
		skeinviewFile( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( SkeinviewPreferences() )

if __name__ == "__main__":
	main()
