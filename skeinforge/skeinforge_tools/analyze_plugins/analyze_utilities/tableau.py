"""
Tableau has a couple of base classes for analyze viewers.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
import os

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getScrollBarCenter( scrollbar ):
	"Get the center of the scrollbar."
	scrollbarRange = scrollbar.get()
	scrollbarDenominator = 1 - scrollbarRange[ 1 ] + scrollbarRange[ 0 ]
	if scrollbarDenominator < 0.001:
		return 0.5
	return scrollbarRange[ 0 ] / scrollbarDenominator


class MouseToolBase:
	"The mouse tool base class, which does nothing."
	def __init__( self ):
		"Initialize."
		self.items = []

	def button1( self, event ):
		"The left button was clicked, <Button-1> function."
		pass

	def buttonRelease1( self, event ):
		"The left button was released, <ButtonRelease-1> function."
		pass

	def destroyEverything( self ):
		"Destroy items."
		self.destroyItems()

	def destroyItems( self ):
		"Destroy items."
		for item in self.items:
			self.canvas.delete( item )
		self.items = []

	def getReset( self, window ):
		"Reset the mouse tool to default."
		self.setCanvasItems( window.canvas )
		return self

	def getTagsGivenXY( self, x, y ):
		"Get the tag for the x and y."
		tags = self.canvas.itemcget( self.canvas.find_closest( x, y ), 'tags' )
		currentEnd = ' current'
		if tags.find( currentEnd ) != - 1:
			return tags[ : - len( currentEnd ) ]
		return tags

	def motion( self, event ):
		"The mouse moved, <Motion> function."
		pass

	def setCanvasItems( self, canvas ):
		"Set the canvas and items."
		self.canvas = canvas


class TableauRepository:
	"The viewer base repository class."
	def setToDisplaySave( self, event = None ):
		"Set the preference values to the display, save the new values."
		for menuEntity in self.menuEntities:
			if menuEntity in self.archive:
				menuEntity.setToDisplay()
		preferences.writePreferences( self )

	def setToDisplaySavePhoenixUpdate( self, event = None ):
		"Set the preference values to the display, save the new values, then call the update function."
		self.setToDisplaySave()
		if self.phoenixUpdateFunction != None:
			self.phoenixUpdateFunction()

	def setToDisplaySaveUpdate( self, event = None ):
		"Set the preference values to the display, save the new values, then call the update function."
		self.setToDisplaySave()
		if self.updateFunction != None:
			self.updateFunction()


class TableauWindow:
	def button1( self, event ):
		"The button was clicked."
		self.mouseTool.button1( event )

	def buttonRelease1( self, event ):
		"The button was released."
		self.mouseTool.buttonRelease1( event )

	def centerUpdateSetWindowGeometryShowPreferences( self, center ):
		"Center the scroll region, update, set the window geometry, and show the preferences."
		self.preferencesMenu = preferences.Tkinter.Menu( self.fileHelpMenuBar.menuBar, tearoff = 0 )
		self.fileHelpMenuBar.addMenuToMenuBar( "Preferences", self.preferencesMenu )
		preferences.addMenuEntitiesToMenu( self.preferencesMenu, self.repository.menuEntities )
		self.relayXview( preferences.Tkinter.MOVETO, center.real * self.oneMinusCanvasWidthOverScreenWidth )
		self.relayYview( preferences.Tkinter.MOVETO, center.imag * self.oneMinusCanvasHeightOverScreenHeight )
		self.root.withdraw()
		self.root.update_idletasks()
		movedGeometryString = '%sx%s+%s' % ( self.root.winfo_reqwidth(), self.root.winfo_reqheight(), '0+0' )
		self.root.geometry( movedGeometryString )
		self.repository.phoenixUpdateFunction = self.phoenixUpdate
		self.repository.updateFunction = self.update

	def close( self, event = None ):
		"The dialog was closed."
		try:
			self.root.destroy()
		except:
			pass

	def destroyAllDialogWindows( self ):
		"Destroy all the dialog windows."
		for menuEntity in self.repository.menuEntities:
			lowerName = menuEntity.name.lower()
			if lowerName in preferences.globalRepositoryDialogListTable:
				globalRepositoryDialogValues = preferences.globalRepositoryDialogListTable[ lowerName ]
				for globalRepositoryDialogValue in globalRepositoryDialogValues:
					preferences.quitWindow( globalRepositoryDialogValue.root )

	def export( self ):
		"Export the canvas as a postscript file."
		postscriptFileName = gcodec.getFilePathWithUnderscoredBasename( self.skein.fileName, self.suffix )
		boundingBox = self.canvas.bbox( preferences.Tkinter.ALL ) # tuple (w, n, e, s)
		boxW = boundingBox[ 0 ]
		boxN = boundingBox[ 1 ]
		boxWidth = boundingBox[ 2 ] - boxW
		boxHeight = boundingBox[ 3 ] - boxN
		print( 'Exported postscript file saved as ' + postscriptFileName )
		self.canvas.postscript( file = postscriptFileName, height = boxHeight, width = boxWidth, pageheight = boxHeight, pagewidth = boxWidth, x = boxW, y = boxN )
		fileExtension = self.repository.exportFileExtension.value
		postscriptProgram = self.repository.exportPostscriptProgram.value
		if postscriptProgram == '':
			return
		postscriptFilePath = '"' + os.path.normpath( postscriptFileName ) + '"' # " to send in file name with spaces
		shellCommand = postscriptProgram + ' ' + postscriptFilePath
		print( '' )
		if fileExtension == '':
			print( 'Sending the shell command:' )
			print( shellCommand )
			commandResult = os.system( shellCommand )
			if commandResult != 0:
				print( 'It may be that the system could not find the %s program.' % postscriptProgram )
				print( 'If so, try installing the %s program or look for another one, like the Gnu Image Manipulation Program (Gimp) which can be found at:' % postscriptProgram )
				print( 'http://www.gimp.org/' )
			return
		convertedFileName = gcodec.getFilePathWithUnderscoredBasename( postscriptFilePath, '.' + fileExtension + '"' )
		shellCommand += ' ' + convertedFileName
		print( 'Sending the shell command:' )
		print( shellCommand )
		commandResult = os.system( shellCommand )
		if commandResult != 0:
			print( 'The %s program could not convert the postscript to the %s file format.' % ( postscriptProgram, fileExtension ) )
			print( 'Try installing the %s program or look for another one, like Image Magick which can be found at:' % postscriptProgram )
			print( 'http://www.imagemagick.org/script/index.php' )

	def getScrollPaneCenter( self ):
		"Get the center of the scroll pane."
		return complex( getScrollBarCenter( self.xScrollbar ), getScrollBarCenter( self.yScrollbar ) )

	def motion( self, event ):
		"The mouse moved."
		self.mouseTool.motion( event )

	def relayXview( self, *args ):
		"Relay xview changes."
		self.canvas.xview( *args )

	def relayYview( self, *args ):
		"Relay yview changes."
		self.canvas.yview( *args )

	def save( self ):
		"Set the preference values to the display, save the new values."
		self.repository.setToDisplaySave()

	def saveUpdate( self ):
		"Save and update."
		self.save()
		self.update()

	def setMenuPanesPreferencesRootSkein( self, repository, skein, suffix ):
		"Set the menu bar, skein panes, tableau preferences, root and skein."
		self.movementTextID = None
		self.repository = repository
		self.root = preferences.Tkinter.Tk()
		self.skein = skein
		self.skeinPanes = skein.skeinPanes
		self.suffix = suffix
		self.fileHelpMenuBar = preferences.FileHelpMenuBar( self.root )
		self.fileHelpMenuBar.fileMenu.add_command( label = "Export", command = self.export )
		self.fileHelpMenuBar.fileMenu.add_separator()

	def setMouseTool( self ):
		"Set the mouse tool."
		for menuRadio in self.repository.mouseMode.menuRadios:
			menuRadio.mouseTool.destroyEverything()
			if menuRadio.value:
				self.mouseTool = menuRadio.mouseTool
#				self.mouseTool.getReset( self )
				return

	def setMouseToolBindButtonMotion( self ):
		"Set the mouse tool and bind button one clicked, button one released and motion."
		for menuRadio in self.repository.mouseMode.menuRadios:
			menuRadio.mouseTool = menuRadio.constructorFunction().getReset( self )
		self.setMouseTool()
		self.canvas.bind( '<Button-1>', self.button1 )
		self.canvas.bind( '<ButtonRelease-1>', self.buttonRelease1 )
		self.canvas.bind( '<Motion>', self.motion )
		self.canvas.bind( '<Shift-ButtonRelease-1>', self.shiftButtonRelease1 )
		self.canvas.bind( '<Shift-Motion>', self.shiftMotion )

	def shiftButtonRelease1( self, event ):
		"The button was released while the shift key was pressed."
		self.mouseTool.buttonRelease1( event, True )

	def shiftMotion( self, event ):
		"The mouse moved."
		self.mouseTool.motion( event, True )

	def updateDeiconify( self, center = complex( 0.5, 0.5 ) ):
		"Update and deiconify the window."
		self.centerUpdateSetWindowGeometryShowPreferences( center )
		self.update()
		self.root.deiconify()
