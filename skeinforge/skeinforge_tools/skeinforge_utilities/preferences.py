"""
Preferences is a collection of utilities to display, read & write preferences.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

import cStringIO
from skeinforge_tools.skeinforge_utilities import gcodec
import os
import webbrowser
try:
	import Tkinter
except:
	print( 'You do not have Tkinter, which is needed for the graphical interface, you will only be able to use the command line.' )
	print( 'Information on how to download Tkinter is at:\nwww.tcl.tk/software/tcltk/' )


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/23/04 $"
__license__ = "GPL 3.0"

globalIsMainLoopRunning = False
globalSpreadsheetSeparator = '\t'

def displayDialog( displayPreferences ):
	"Display the preferences dialog."
	readPreferences( displayPreferences )
	root = Tkinter.Tk()
	preferencesDialog = PreferencesDialog( displayPreferences, root )
	global globalIsMainLoopRunning
#	print( globalIsMainLoopRunning )
	if globalIsMainLoopRunning:
		return
	globalIsMainLoopRunning = True
	root.mainloop()
	globalIsMainLoopRunning = False

def getArchiveText( archivablePreferences ):
	"Get the text representation of the archive."
	archiveWriter = cStringIO.StringIO()
	archiveWriter.write( 'Format is tab separated preferences.\n' )
	for preference in archivablePreferences.archive:
		preference.writeToArchiveWriter( archiveWriter )
	return archiveWriter.getvalue()

def getFileInGivenDirectory( directory, fileName ):
	"Get the file from the fileName or the lowercase fileName in the given directory."
	directoryListing = os.listdir( directory )
	if fileName in directoryListing:
		return getFileTextGivenDirectoryFileName( directory, fileName )
	lowerFilename = fileName.lower()
	if lowerFilename in directoryListing:
		return getFileTextGivenDirectoryFileName( directory, lowerFilename )
	return ''

def getFileInGivenPreferencesDirectory( directory, fileName ):
	"Get the file from the fileName or the lowercase fileName in the given directory, if there is no file look in the gcode_scripts folder in the preferences directory."
	if directory == '':
		directory = os.getcwd()
	fileInGivenPreferencesDirectory = getFileInGivenDirectory( directory, fileName )
	if fileInGivenPreferencesDirectory != '':
		return fileInGivenPreferencesDirectory
	gcodeDirectoryPath = os.path.join( getPreferencesDirectoryPath(), 'gcode_scripts' )
	try:
		os.mkdir( gcodeDirectoryPath )
	except OSError:
		pass
	return getFileInGivenDirectory( gcodeDirectoryPath, fileName )

def getFileTextGivenDirectoryFileName( directory, fileName ):
	"Get the entire text of a file with the given file name in the given directory."
	absoluteFilePath = os.path.join( directory, fileName )
	return gcodec.getFileText( absoluteFilePath )

def getPreferencesDirectoryPath():
	"Get the preferences directory path, which is the home directory joined with .skeinforge."
	return os.path.join( os.path.expanduser( '~' ), '.skeinforge' )

def getPreferencesFilePath( fileName ):
	"Get the preferences file path, which is the home directory joined with .skeinforge and fileName."
	directoryName = getPreferencesDirectoryPath()
	try:
		os.mkdir( directoryName )
	except OSError:
		pass
	return os.path.join( directoryName, fileName )

def readPreferences( archivablePreferences ):
	"Set an archive to the preferences read from a file."
	text = gcodec.getFileText( archivablePreferences.fileNamePreferences )
	if text == '':
		print( 'Since the preferences file:' )
		print( archivablePreferences.fileNamePreferences )
		print( 'does not exist, the default preferences will be written to that file.' )
		text = gcodec.getFileText( os.path.join( 'defaults', os.path.basename( archivablePreferences.fileNamePreferences ) ) )
		if text != '':
			readPreferencesFromText( archivablePreferences, text )
		writePreferences( archivablePreferences )
		return
	readPreferencesFromText( archivablePreferences, text )

def readPreferencesFromText( archivablePreferences, text ):
	"Set an archive to the preferences read from a text."
	lines = gcodec.getTextLines( text )
	preferenceTable = {}
	for preference in archivablePreferences.archive:
		preference.addToPreferenceTable( preferenceTable )
	for lineIndex in xrange( len( lines ) ):
		setArchiveToLine( lineIndex, lines, preferenceTable )

def setArchiveToLine( lineIndex, lines, preferenceTable ):
	"Set an archive to a preference line."
	line = lines[ lineIndex ]
	splitLine = line.split( globalSpreadsheetSeparator )
	if len( splitLine ) < 2:
		return
	filePreferenceName = splitLine[ 0 ]
	if filePreferenceName in preferenceTable:
		preferenceTable[ filePreferenceName ].setValueToSplitLine( lineIndex, lines, splitLine )

def setHelpPreferencesFileNameTitleWindowPosition( displayPreferences, fileNameHelp ):
	"Set the help & preferences file path, the title and the window position archiver."
	DotIndex = fileNameHelp.rfind( '.' )
	lastDotIndex = fileNameHelp.rfind( '.' )
	lowerName = fileNameHelp[ : lastDotIndex ]
	lastTruncatedDotIndex = lowerName.rfind( '.' )
	lowerName = lowerName[ lastTruncatedDotIndex + 1 : ]
	displayPreferences.title = lowerName.replace( '_', ' ' ).capitalize() + ' Preferences'
	windowPositionName = 'windowPosition' + displayPreferences.title
	displayPreferences.windowPositionBeholdPreferences = WindowPosition().getFromValue( 'windowPositionBehold Preferences', '0+0' )
	displayPreferences.archive.append( displayPreferences.windowPositionBeholdPreferences )
	displayPreferences.fileNamePreferences = getPreferencesFilePath( lowerName + '.csv' )
	displayPreferences.fileNameHelp = fileNameHelp

def writePreferences( archivablePreferences ):
	"Write the preferences to a file."
	gcodec.writeFileText( archivablePreferences.fileNamePreferences, getArchiveText( archivablePreferences ) )


class AddListboxSelection:
	"A class to add the selection of a listbox preference."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.entry = Tkinter.Entry( preferencesDialog.master )
		self.entry.bind( '<Return>', self.addSelectionWithEvent )
		self.entry.grid( row = preferencesDialog.row, column = 1, columnspan = 2, sticky = Tkinter.W )
		self.addButton = Tkinter.Button( preferencesDialog.master, text = 'Add Listbox Selection', command = self.addSelection )
		self.addButton.grid( row = preferencesDialog.row, column = 0 )
		preferencesDialog.row += 1

	def addSelection( self ):
		"Add the selection of a listbox preference."
		entryText = self.entry.get()
		if entryText == '':
			print( 'To add to the selection, enter the material name.' )
			return
		self.entry.delete( 0, Tkinter.END )
		self.listboxPreference.listPreference.value.append( entryText )
		self.listboxPreference.listPreference.value.sort()
		self.listboxPreference.listbox.delete( 0, Tkinter.END )
		self.listboxPreference.value = entryText
		self.listboxPreference.setListboxItems()
		self.listboxPreference.setToDisplay()

	def addSelectionWithEvent( self, event ):
		"Add the selection of a listbox preference, given an event."
		self.addSelection()

	def addToPreferenceTable( self, preferenceTable ):
		"Do nothing because the add listbox selection is not archivable."
		pass

	def getFromListboxPreference( self, listboxPreference ):
		"Initialize."
		self.listboxPreference = listboxPreference
		return self

	def setToDisplay( self ):
		"Do nothing because the add listbox selection is not archivable."
		pass

	def writeToArchiveWriter( self, archiveWriter ):
		"Do nothing because the add listbox selection is not archivable."
		pass


class StringPreference:
	"A class to display, read & write a string."
	def __init__( self ):
		"Set the update function to none."
		self.updateFunction = None

	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.entry = Tkinter.Entry( preferencesDialog.master )
		self.setStateToValue()
		self.entry.grid( row = preferencesDialog.row, column = 2, columnspan = 2, sticky = Tkinter.W )
		self.label = Tkinter.Label( preferencesDialog.master, text = self.name )
		self.label.grid( row = preferencesDialog.row, column = 0, columnspan = 2, sticky = Tkinter.W )
		preferencesDialog.row += 1

	def addToPreferenceTable( self, preferenceTable ):
		"Add this to the preference table."
		preferenceTable[ self.name ] = self

	def getFromValue( self, name, value ):
		"Initialize."
		self.value = value
		self.name = name
		return self

	def setStateToValue( self ):
		"Set the entry to the value."
		try:
			self.entry.delete( 0, Tkinter.END )
			self.entry.insert( 0, self.value )
		except:
			pass

	def setToDisplay( self ):
		"Set the string to the entry field."
		valueString = self.entry.get()
		self.setValueToString( valueString )

	def setUpdateFunction( self, updateFunction ):
		"Set the update function."
		self.updateFunction = updateFunction

	def setValueToSplitLine( self, lineIndex, lines, splitLine ):
		"Set the value to the second word of a split line."
		self.setValueToString( splitLine[ 1 ] )

	def setValueToString( self, valueString ):
		"Set the string to the value string."
		self.value = valueString

	def writeToArchiveWriter( self, archiveWriter ):
		"Write tab separated name and value to the archive writer."
		archiveWriter.write( self.name + globalSpreadsheetSeparator + str( self.value ) + '\n' )


class BooleanPreference( StringPreference ):
	"A class to display, read & write a boolean."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.checkbutton = Tkinter.Checkbutton( preferencesDialog.master, command = self.toggleCheckbox, text = self.name )
#toggleCheckbox is being used instead of a Tkinter IntVar because there is a weird bug where it doesn't work properly if this preference is not on the first window.
		self.checkbutton.grid( row = preferencesDialog.row, columnspan = 4, sticky = Tkinter.W )
		self.setStateToValue()
		preferencesDialog.row += 1

	def setStateToValue( self ):
		"Set the checkbox to the boolean."
		if self.value:
			self.checkbutton.select()
		else:
			self.checkbutton.deselect()

	def setToDisplay( self ):
		"Do nothing because toggleCheckbox is handling the value."
		pass

	def setValueToString( self, valueString ):
		"Set the boolean to the string."
		self.value = ( valueString.lower() == 'true' )

	def toggleCheckbox( self ):
		"Workaround for Tkinter bug, toggle the value."
		self.value = not self.value
		self.setStateToValue()
		if self.updateFunction != None:
			self.updateFunction()


class DeleteListboxSelection( AddListboxSelection ):
	"A class to delete the selection of a listbox preference."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.deleteButton = Tkinter.Button( preferencesDialog.master, text = "Delete Listbox Selection", command = self.deleteSelection )
		self.deleteButton.grid( row = preferencesDialog.row, column = 0 )
		preferencesDialog.row += 1

	def deleteSelection( self ):
		"Delete the selection of a listbox preference."
		self.listboxPreference.setToDisplay()
		if self.listboxPreference.value not in self.listboxPreference.listPreference.value:
			return
		self.listboxPreference.listPreference.value.remove( self.listboxPreference.value )
		self.listboxPreference.listbox.delete( 0, Tkinter.END )
		self.listboxPreference.setListboxItems()
		self.listboxPreference.listbox.select_set( 0 )
		self.listboxPreference.setToDisplay()


class DisplayToolButton:
	"A class to display the tool preferences dialog."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		withSpaces = self.name.lower().replace( '_', ' ' )
		words = withSpaces.split( ' ' )
		capitalizedStrings = []
		for word in words:
			capitalizedStrings.append( word.capitalize() )
		capitalizedName = ' '.join( capitalizedStrings )
		self.displayButton = Tkinter.Button( preferencesDialog.master, activebackground = 'black', activeforeground = 'violet', command = self.displayTool, text = capitalizedName )
		self.displayButton.grid( row = preferencesDialog.row, column = 0 )
		preferencesDialog.row += 1

	def addToPreferenceTable( self, preferenceTable ):
		"Do nothing because the add listbox selection is not archivable."
		pass

	def displayTool( self ):
		"Display the tool preferences dialog."
		pluginModule = gcodec.getModule( self.name, self.folderName, self.moduleFilename )
		if pluginModule != None:
			pluginModule.main()

	def getFromFolderName( self, folderName, moduleFilename, name ):
		"Initialize."
		self.folderName = folderName
		self.moduleFilename = moduleFilename
		self.name = name
		return self

	def getLowerName( self ):
		"Get the lower case name."
		return self.name.lower()

	def setToDisplay( self ):
		"Do nothing because the display tool button is not archivable."
		pass

	def writeToArchiveWriter( self, archiveWriter ):
		"Do nothing because the display tool button is not archivable."
		pass


class DisplayToolButtonBesidePrevious( DisplayToolButton ):
	"A class to display the tool preferences dialog beside the previous preference dialog element."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		withSpaces = self.name.lower().replace( '_', ' ' )
		words = withSpaces.split( ' ' )
		capitalizedStrings = []
		for word in words:
			capitalizedStrings.append( word.capitalize() )
		capitalizedName = ' '.join( capitalizedStrings )
		self.displayButton = Tkinter.Button( preferencesDialog.master, text = capitalizedName, command = self.displayTool )
		self.displayButton.grid( row = preferencesDialog.row - 1, column = 2, columnspan = 2 )


class Filename( StringPreference ):
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		preferencesDialog.executables.append( self )

	"A class to display, read & write a fileName."
	def execute( self ):
		try:
			import tkFileDialog
			summarized = gcodec.getSummarizedFilename( self.value )
			initialDirectory = os.path.dirname( summarized )
			if len( initialDirectory ) > 0:
				initialDirectory += os.sep
			else:
				initialDirectory = "."
			fileName = tkFileDialog.askopenfilename( filetypes = self.getFilenameFirstTypes(), initialdir = initialDirectory, initialfile = os.path.basename( summarized ), title = self.name )
			if ( str( fileName ) == '()' or str( fileName ) == '' ):
				self.wasCancelled = True
			else:
				self.value = fileName
		except:
			print( 'Oops, ' + self.name + ' could not get fileName.' )

	def getFromFilename( self, fileTypes, name, value ):
		"Initialize."
		self.getFromValue( name, value )
		self.fileTypes = fileTypes
		self.wasCancelled = False
		return self

	def getFilenameFirstTypes( self ):
		"Get the file types with the file type of the fileName moved to the front of the list."
		basename = os.path.basename( self.value )
		splitFile = basename.split( '.' )
		allReadables = []
		if len( self.fileTypes ) > 1:
			for fileType in self.fileTypes:
				allReadable = ( ( 'All Readable', fileType[ 1 ] ) )
				allReadables.append( allReadable )
		if len( splitFile ) < 1:
			return self.fileTypes + allReadables
		baseExtension = splitFile[ - 1 ]
		for fileType in self.fileTypes:
			fileExtension = fileType[ 1 ].split( '.' )[ - 1 ]
			if fileExtension == baseExtension:
				fileNameFirstTypes = self.fileTypes[ : ]
				fileNameFirstTypes.remove( fileType )
				return [ fileType ] + fileNameFirstTypes + allReadables
		return self.fileTypes + allReadables

	def setToDisplay( self ):
		"Do nothing because the file dialog is handling the value."
		pass


class FloatPreference( StringPreference ):
	"A class to display, read & write a float."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.entry = Tkinter.Entry( preferencesDialog.master )
		self.entry.insert( 0, str( self.value ) )
		self.entry.grid( row = preferencesDialog.row, column = 2, columnspan = 2, sticky = Tkinter.W )
		self.label = Tkinter.Label( preferencesDialog.master, text = self.name )
		self.label.grid( row = preferencesDialog.row, column = 0, columnspan = 2, sticky = Tkinter.W )
		preferencesDialog.row += 1

	def setUpdateFunction( self, updateFunction ):
		"Set the update function."
		self.entry.bind( '<Return>', updateFunction )

	def setValueToString( self, valueString ):
		"Set the float to the string."
		try:
			self.value = float( valueString )
		except:
			print( 'Oops, can not read float' + self.name + ' ' + valueString )


class IntPreference( FloatPreference ):
	"A class to display, read & write an int."
	def setValueToString( self, valueString ):
		"Set the integer to the string."
		dotIndex = valueString.find( '.' )
		if dotIndex > - 1:
			valueString = valueString[ : dotIndex ]
		try:
			self.value = int( valueString )
		except:
			print( 'Oops, can not read integer ' + self.name + ' ' + valueString )


class LabelDisplay:
	"A class to add a label."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.label = Tkinter.Label( preferencesDialog.master, text = self.name )
		self.label.grid( row = preferencesDialog.row, column = 0, columnspan = 2, sticky = Tkinter.W )
		preferencesDialog.row += 1

	def addToPreferenceTable( self, preferenceTable ):
		"Do nothing because the label display is not archivable."
		pass

	def getFromName( self, name ):
		"Initialize."
		self.name = name
		return self

	def getName( self ):
		"Get name for key sorting."
		return self.name

	def setToDisplay( self ):
		"Do nothing because the label display is not archivable."
		pass

	def writeToArchiveWriter( self, archiveWriter ):
		"Do nothing because the label display is not archivable."
		pass


class ListPreference( StringPreference ):
	def addToDialog( self, preferencesDialog ):
		"Do nothing because the list preference does not have a graphical interface."
		pass

	def setToDisplay( self ):
		"Do nothing because the list preference does not have a graphical interface."
		pass

	def setValueToSplitLine( self, lineIndex, lines, splitLine ):
		"Set the value to the second and later words of a split line."
		self.value = splitLine[ 1 : ]

	def setValueToString( self, valueString ):
		"Do nothing because the list preference does not have a graphical interface."
		pass

	def writeToArchiveWriter( self, archiveWriter ):
		"Write tab separated name and list to the archive writer."
		archiveWriter.write( self.name + globalSpreadsheetSeparator )
		for item in self.value:
			archiveWriter.write( item )
			if item != self.value[ - 1 ]:
				archiveWriter.write( globalSpreadsheetSeparator )
		archiveWriter.write( '\n' )


class ListboxPreference( StringPreference ):
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
#http://www.pythonware.com/library/tkinter/introduction/x5453-patterns.htm
		frame = Tkinter.Frame( preferencesDialog.master )
		scrollbar = Tkinter.Scrollbar( frame, orient = Tkinter.VERTICAL )
		self.listbox = Tkinter.Listbox( frame, selectmode = Tkinter.SINGLE, yscrollcommand = scrollbar.set )
		scrollbar.config( command = self.listbox.yview )
		scrollbar.pack( side = Tkinter.RIGHT, fill = Tkinter.Y )
		self.listbox.pack( side = Tkinter.LEFT, fill = Tkinter.BOTH, expand = 1 )
		self.setListboxItems()
		frame.grid( row = preferencesDialog.row, columnspan = 4, sticky = Tkinter.W )
		preferencesDialog.row += 1

	def getFromListPreference( self, listPreference, name, value ):
		"Initialize."
		self.getFromValue( name, value )
		self.listPreference = listPreference
		return self

	def setListboxItems( self ):
		"Set the listbox items to the list preference."
		for item in self.listPreference.value:
			self.listbox.insert( Tkinter.END, item )
			if self.value == item:
				self.listbox.select_set( Tkinter.END )

	def setToDisplay( self ):
		"Set the selection value to the listbox selection."
		valueString = self.listbox.get( Tkinter.ACTIVE )
		self.setValueToString( valueString )


class Radio( BooleanPreference ):
	"A class to display, read & write a boolean with associated radio button."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		self.radiobutton = Tkinter.Radiobutton( preferencesDialog.master, command = self.clickRadio, text = self.name, value = preferencesDialog.row, variable = self.getIntVar() )
		self.radiobutton.grid( row = preferencesDialog.row, column = 0, columnspan = 2, sticky = Tkinter.W )
		self.setDisplayState( preferencesDialog.row )
		preferencesDialog.row += 1

	def clickRadio( self ):
		"Workaround for Tkinter bug, set the value."
		self.getIntVar().set( self.radiobutton[ 'value' ] )

	def getFromRadio( self, name, radio, value ):
		"Initialize."
		self.getFromValue( name, value )
		self.radio = radio
		return self

	def getIntVar( self ):
		"Get the IntVar for this radio button group."
		if len( self.radio ) == 0:
			self.radio.append( Tkinter.IntVar() )
		return self.radio[ 0 ]

	def setToDisplay( self ):
		"Set the boolean to the checkbox."
		self.value = ( self.getIntVar().get() == self.radiobutton[ 'value' ] )

	def setDisplayState( self, row ):
		"Set the boolean to the checkbox."
		if self.value:
			self.getIntVar().set( self.radiobutton[ 'value' ] )
			self.radiobutton.select()


class RadioCapitalized( Radio ):
	"A class to display, read & write a boolean with associated radio button."
	def addToDialog( self, preferencesDialog ):
		"Add this to the dialog."
		withSpaces = self.name.lower().replace( '_', ' ' )
		words = withSpaces.split( ' ' )
		capitalizedStrings = []
		for word in words:
			capitalizedStrings.append( word.capitalize() )
		capitalizedName = ' '.join( capitalizedStrings )
		self.radiobutton = Tkinter.Radiobutton( preferencesDialog.master, command = self.clickRadio, text = capitalizedName, value = preferencesDialog.row, variable = self.getIntVar() )
		self.radiobutton.grid( row = preferencesDialog.row, column = 0, columnspan = 2, sticky = Tkinter.W )
		self.setDisplayState( preferencesDialog.row )
		preferencesDialog.row += 1

	def getLowerName( self ):
		"Get the lower case name."
		return self.name.lower()


class WindowPosition( StringPreference ):
	"A class to display, read & write a window position."
	def addToDialog( self, preferencesDialog ):
		"Set the master to later get the geometry."
		self.master = preferencesDialog.master
		self.windowPositionName = 'windowPosition' + preferencesDialog.displayPreferences.title
		self.setToDisplay()

	def setToDisplay( self ):
		"Set the string to the window position."
		if self.name != self.windowPositionName:
			return
		geometryString = self.master.geometry()
		if geometryString == '1x1+0+0':
			return
		firstPlusIndexPlusOne = geometryString.find( '+' ) + 1
		self.value = geometryString[ firstPlusIndexPlusOne : ]

	def setWindowPosition( self ):
		"Set the window position."
		geometryString = self.master.geometry()
		if geometryString == '1x1+0+0':
			return
		firstPlusIndexPlusOne = geometryString.find( '+' ) + 1
		if self.value.count( '+' ) == 1:
			geometryString = geometryString[ : firstPlusIndexPlusOne ] + self.value
			self.master.geometry( geometryString )


class PreferencesDialog:
	def __init__( self, displayPreferences, master ):
		"Add display preferences to the dialog."
		self.column = 0
		self.displayPreferences = displayPreferences
		self.executables = []
		self.master = master
		self.row = 0
		master.title( displayPreferences.title )
		frame = Tkinter.Frame( master )
		for preference in displayPreferences.archive:
			preference.addToDialog( self )
		if self.row < 30:
			Tkinter.Label( master ).grid( row = self.row, column = 0 )
			self.row += 1
		cancelColor = 'red'
		cancelTitle = 'Close'
		if displayPreferences.saveTitle != None:
			cancelTitle = 'Cancel'
		if displayPreferences.executeTitle != None:
			executeButton = Tkinter.Button( master, activebackground = 'black', activeforeground = 'blue', text = displayPreferences.executeTitle, command = self.execute )
			executeButton.grid( row = self.row, column = self.column )
			self.column += 1
		helpButton = Tkinter.Button( master, activebackground = 'black', activeforeground = 'white', text = "       ?       ", command = self.openBrowser )
		helpButton.grid( row = self.row, column = self.column )
		self.column += 1
		cancelButton = Tkinter.Button( master, activebackground = 'black', activeforeground = cancelColor, command = master.destroy, fg = cancelColor, text = cancelTitle )
		cancelButton.grid( row = self.row, column = self.column )
		self.column += 1
		if displayPreferences.saveTitle != None:
			saveButton = Tkinter.Button( master, activebackground = 'black', activeforeground = 'darkgreen', command = self.savePreferencesDestroy, fg = 'darkgreen', text = displayPreferences.saveTitle )
			saveButton.grid( row = self.row, column = self.column )
		self.setWindowPosition()

	def execute( self ):
		"The execute button was clicked."
		for executable in self.executables:
			executable.execute()
		self.savePreferences()
		self.displayPreferences.execute()
		self.master.destroy()

	def openBrowser( self ):
		"Open the browser to the help page."
		numberOfLevelsDeepInPackageHierarchy = 2
		packageFilePath = os.path.abspath( __file__ )
		for level in xrange( numberOfLevelsDeepInPackageHierarchy + 1 ):
			packageFilePath = os.path.dirname( packageFilePath )
		documentationPath = os.path.join( os.path.join( packageFilePath, 'documentation' ), self.displayPreferences.fileNameHelp )
		os.system( webbrowser.get().name + ' ' + documentationPath )#used this instead of webbrowser.open() to workaround webbrowser open() bug

	def savePreferences( self ):
		"Set the preferences to the dialog then write them."
		for preference in self.displayPreferences.archive:
			preference.setToDisplay()
		writePreferences( self.displayPreferences )

	def savePreferencesDestroy( self ):
		"Set the preferences to the dialog, write them, then destroy the window."
		self.savePreferences()
		self.master.destroy()

	def setWindowPosition( self ):
		"Set the window position if that preference exists."
		windowPositionName = 'windowPosition' + self.displayPreferences.title
		for preference in self.displayPreferences.archive:
			if isinstance( preference, WindowPosition ):
				if preference.name == windowPositionName:
					self.master.update_idletasks()
					preference.setWindowPosition()
					self.master.update_idletasks()
					return
