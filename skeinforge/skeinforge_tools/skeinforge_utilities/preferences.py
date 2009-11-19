"""
Preferences is a collection of utilities to display, read & write preferences.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
import cStringIO
import os
import shutil
import webbrowser
try:
	import Tkinter
except:
	print( 'You do not have Tkinter, which is needed for the graphical interface, you will only be able to use the command line.' )
	print( 'Information on how to download Tkinter is at:\nwww.tcl.tk/software/tcltk/' )


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/23/04 $"
__license__ = "GPL 3.0"

globalRepositoryDialogListTable = {}
globalProfileSaveListenerListTable = {}
globalCloseListTables = [ globalRepositoryDialogListTable, globalProfileSaveListenerListTable ]
globalSpreadsheetSeparator = '\t'


def addAcceleratorCommand( acceleratorBinding, commandFunction, master, menu, text ):
	"Add accelerator command."
	acceleratorText = acceleratorBinding[ 1 : - 1 ]
	lastIndexOfMinus = acceleratorText.rfind( '-' )
	if lastIndexOfMinus > - 1:
		acceleratorText = acceleratorText[ : lastIndexOfMinus + 1 ] + acceleratorText[ lastIndexOfMinus + 1 : ].capitalize()
	acceleratorText = acceleratorText.replace( 'KeyPress-', '' )
	acceleratorText = acceleratorText.replace( '-', '+' )
	acceleratorText = acceleratorText.replace( 'Control', 'Ctrl' )
	acceleratorBinding = acceleratorBinding.replace( 'KeyPress', '' )
	menu.add_command( accelerator = acceleratorText, label = text, underline = 0, command = commandFunction )
	master.bind( acceleratorBinding, commandFunction )

def addElementToListTableIfNotThere( element, key, listTable ):
	"Add the value to the lists."
	if key in listTable:
		elements = listTable[ key ]
		if element not in elements:
			elements.append( element )
	else:
		listTable[ key ] = [ element ]

def addListsToRepository( repository ):
	"Add the value to the lists."
	repository.archive = []
	repository.displayEntities = []
	repository.menuEntities = []
	repository.saveCloseTitle = 'Save and Close'
	WindowVisibilities().getFromRepository( repository )

def addListsToRepository( repository ):
	"Add the value to the lists."
	repository.archive = []
	repository.displayEntities = []
	repository.menuEntities = []
	repository.saveCloseTitle = 'Save and Close'
	WindowVisibilities().getFromRepository( repository )

def addMenuEntitiesToMenu( menu, menuEntities ):
	"Add the menu entities to the menu."
	for menuEntity in menuEntities:
		menuEntity.addToMenu( menu )

def addPluginsParentToMenu( directoryPath, menu, parentPath, pluginFilenames ):
	"Add plugins and the parent to the menu."
	ToolDialog().addPluginToMenu( menu, parentPath[ : parentPath.rfind( '.' ) ] )
	menu.add_separator()
	addPluginsToMenu( directoryPath, menu, pluginFilenames )

def addPluginsToMenu( directoryPath, menu, pluginFilenames ):
	"Add plugins to the menu."
	for pluginFilename in pluginFilenames:
		ToolDialog().addPluginToMenu( menu, os.path.join( directoryPath, pluginFilename ) )

def deleteDirectory( directory, subfolderName ):
	"Delete the directory if it exists."
	subDirectory = os.path.join( directory, subfolderName )
	if os.path.isdir( subDirectory ):
		shutil.rmtree( subDirectory )

def deleteMenuItems( menu ):
	"Delete the menu items."
	try:
		lastMenuIndex = menu.index( Tkinter.END )
		if lastMenuIndex != None:
			menu.delete( 0, lastMenuIndex )
	except:
		print( 'this should never happen, the lastMenuIndex in deleteMenuItems in preferences could not be determined.' ) 

def getArchiveText( repository ):
	"Get the text representation of the archive."
	archiveWriter = cStringIO.StringIO()
	archiveWriter.write( 'Format is tab separated %s.\n' % repository.title.lower() )
	archiveWriter.write( 'Name                          %sValue\n' % globalSpreadsheetSeparator )
	for preference in repository.archive:
		preference.writeToArchiveWriter( archiveWriter )
	return archiveWriter.getvalue()

def getCraftTypeName( subName = '' ):
	"Get the craft type from the profile."
	profilePreferences = getReadProfileRepository()
	craftTypeName = getSelectedPluginName( profilePreferences.craftRadios )
	if subName == '':
		return craftTypeName
	return os.path.join( craftTypeName, subName )

def getCraftTypePluginModule( craftTypeName = '' ):
	"Get the craft type plugin module."
	if craftTypeName == '':
		craftTypeName = getCraftTypeName()
	profilePluginsDirectoryPath = getPluginsDirectoryPath()
	return gcodec.getModuleWithDirectoryPath( profilePluginsDirectoryPath, craftTypeName )

def getCraftTypeProfilesDirectoryPath( subfolder = '' ):
	"Get the craft type profiles directory path, which is the preferences directory joined with profiles, joined in turn with the craft type."
	craftTypeName = getCraftTypeName( subfolder )
	craftTypeProfileDirectory = getProfilesDirectoryPath( craftTypeName )
	return craftTypeProfileDirectory

def getDirectoryInAboveDirectory( directory ):
	"Get the directory in the above directory."
	aboveDirectory = os.path.dirname( os.path.dirname( os.path.abspath( __file__ ) ) )
	return os.path.join( aboveDirectory, directory )

def getDisplayedDialogFromConstructor( repository ):
	"Display the repository dialog."
	getReadRepository( repository )
	return RepositoryDialog( repository, Tkinter.Tk() )

def getDisplayedDialogFromPath( path ):
	"Display the repository dialog."
	pluginModule = gcodec.getModuleWithPath( path )
	if pluginModule == None:
		return None
	try:
		return getDisplayedDialogFromConstructor( pluginModule.getRepositoryConstructor() )
	except:
		print( 'this should never happen, getDisplayedDialogFromPath in preferences could not open' )
		print( pluginModule )
		print( path )
		return None

def getDisplayToolButtonsRepository( directoryPath, importantFilenames, names, repository ):
	"Get the display tool buttons."
	displayToolButtons = []
	for name in names:
		displayToolButton = DisplayToolButton().getFromPath( name in importantFilenames, name, os.path.join( directoryPath, name ), repository )
		displayToolButtons.append( displayToolButton )
	return displayToolButtons

def getDocumentationPath( subName = '' ):
	"Get the documentation file path."
	numberOfLevelsDeepInPackageHierarchy = 2
	packageFilePath = os.path.abspath( __file__ )
	for level in xrange( numberOfLevelsDeepInPackageHierarchy + 1 ):
		packageFilePath = os.path.dirname( packageFilePath )
	documentationIndexPath = os.path.join( packageFilePath, 'documentation' )
	return os.path.join( documentationIndexPath, subName )

def getEachWordCapitalized( name ):
	"Get the capitalized name."
	withSpaces = name.lower().replace( '_', ' ' )
	words = withSpaces.split( ' ' )
	capitalizedStrings = []
	for word in words:
		capitalizedStrings.append( word.capitalize() )
	return ' '.join( capitalizedStrings )

def getFileInAlterationsOrGivenDirectory( directory, fileName ):
	"Get the file from the fileName or the lowercase fileName in the alterations directories, if there is no file look in the given directory."
	preferencesAlterationsDirectory = getPreferencesDirectoryPath( 'alterations' )
	makeDirectory( preferencesAlterationsDirectory )
	fileInPreferencesAlterationsDirectory = getFileInGivenDirectory( preferencesAlterationsDirectory, fileName )
	if fileInPreferencesAlterationsDirectory != '':
		return fileInPreferencesAlterationsDirectory
	alterationsDirectory = getDirectoryInAboveDirectory( 'alterations' )
	fileInAlterationsDirectory = getFileInGivenDirectory( alterationsDirectory, fileName )
	if fileInAlterationsDirectory != '':
		return fileInAlterationsDirectory
	if directory == '':
		directory = os.getcwd()
	return getFileInGivenDirectory( directory, fileName )

def getFileInGivenDirectory( directory, fileName ):
	"Get the file from the fileName or the lowercase fileName in the given directory."
	directoryListing = os.listdir( directory )
	lowerFilename = fileName.lower()
	for directoryFile in directoryListing:
		if directoryFile.lower() == lowerFilename:
			return getFileTextGivenDirectoryFileName( directory, directoryFile )
	return ''

def getFileTextGivenDirectoryFileName( directory, fileName ):
	"Get the entire text of a file with the given file name in the given directory."
	absoluteFilePath = os.path.join( directory, fileName )
	return gcodec.getFileText( absoluteFilePath )

def getFolders( directory ):
	"Get the folder list in a directory."
	makeDirectory( directory )
	directoryListing = []
	try:
		directoryListing = os.listdir( directory )
	except OSError:
		print( 'Skeinforge can not list the directory:' )
		print( directory )
		print( 'so give it read/write permission for that directory.' )
	folders = []
	for fileName in directoryListing:
		if os.path.isdir( os.path.join( directory, fileName ) ):
			folders.append( fileName )
	return folders

def getLowerNameSetHelpTitleWindowPosition( repository, fileNameHelp ):
	"Set the help & preferences file path, the title and the window position archiver."
	repository.lowerName = fileNameHelp.split( '.' )[ - 2 ]
	repository.capitalizedName = getEachWordCapitalized( repository.lowerName )
	repository.title = repository.capitalizedName + ' Preferences'
	windowPositionName = 'windowPosition' + repository.title
	repository.windowPositionPreferences = WindowPosition().getFromValue( windowPositionName, repository, '0+0' )
	repository.fileNameHelp = fileNameHelp
	for preference in repository.archive:
		preference.repository = repository
	return repository.lowerName + '.csv'

def getPathFromFileNameHelp( fileNameHelp ):
	"Get the directory path from file name help."
	skeinforgePath = getSkeinforgeDirectoryPath()
	splitFileNameHelps = fileNameHelp.split( '.' )
	splitFileNameDirectoryNames = splitFileNameHelps[ : - 1 ]
	for splitFileNameDirectoryName in splitFileNameDirectoryNames:
		skeinforgePath = os.path.join( skeinforgePath, splitFileNameDirectoryName )
	return skeinforgePath

def getPluginsDirectoryPath():
	"Get the plugins directory path."
	return gcodec.getAbsoluteFolderPath( os.path.dirname( __file__ ), 'profile_plugins' )

def getPluginFilenames():
	"Get analyze plugin fileNames."
	return gcodec.getPluginFilenamesFromDirectoryPath( getPluginsDirectoryPath() )

def getPreferencesDirectoryPath( subfolder = '' ):
	"Get the preferences directory path, which is the home directory joined with .skeinforge."
	preferencesDirectory = os.path.join( os.path.expanduser( '~' ), '.skeinforge' )
	if subfolder == '':
		return preferencesDirectory
	return os.path.join( preferencesDirectory, subfolder )

def getProfilesDirectoryPath( subfolder = '' ):
	"Get the profiles directory path, which is the preferences directory joined with profiles."
	profilesDirectory = getPreferencesDirectoryPath( 'profiles' )
	if subfolder == '':
		return profilesDirectory
	return os.path.join( profilesDirectory, subfolder )

def getProfilesDirectoryInAboveDirectory( subName = '' ):
	"Get the profiles directory path in the above directory."
	aboveProfilesDirectory = getDirectoryInAboveDirectory( 'profiles' )
	if subName == '':
		return aboveProfilesDirectory
	return os.path.join( aboveProfilesDirectory, subName )

def getReadRepository( repository ):
	"Read and return preferences from a file."
	text = gcodec.getFileText( getProfilesDirectoryPath( repository.baseName ), 'r', False )
	if text == '':
		print( 'The default %s will be written in the .skeinforge folder in the home directory.' % repository.title.lower() )
		text = gcodec.getFileText( getProfilesDirectoryInAboveDirectory( repository.baseName ), 'r', False )
		if text != '':
			readPreferencesFromText( repository, text )
		writePreferences( repository )
		return repository
	readPreferencesFromText( repository, text )
	return repository

def getReadProfileRepository():
	"Get the read profile preferences."
	return getReadRepository( ProfileRepository() )

def getSelectedPluginModuleFromPath( filePath, plugins ):
	"Get the selected plugin module."
	for plugin in plugins:
		if plugin.value:
			return gcodec.getModuleFromPath( plugin.name, filePath )
	return None

def getSelectedPluginName( plugins ):
	"Get the selected plugin name."
	for plugin in plugins:
		if plugin.value:
			return plugin.name
	return ''

def getSelectedCraftTypeProfile( craftTypeName = '' ):
	"Get the selected profile.getRepositoryConstructor"
	craftTypePreferences = getCraftTypePluginModule( craftTypeName ).getRepositoryConstructor()
	getReadRepository( craftTypePreferences )
	return craftTypePreferences.profileListbox.value

def getSkeinforgeToolsDirectoryPath():
	"Get the skeinforge tools directory path."
	return os.path.dirname( os.path.dirname( os.path.abspath( __file__ ) ) )

def getSkeinforgeDirectoryPath():
	"Get the skeinforge directory path."
	return os.path.dirname( getSkeinforgeToolsDirectoryPath() )

def getSubfolderWithBasename( basename, directory ):
	"Get the subfolder in the directory with the basename."
	makeDirectory( directory )
	directoryListing = os.listdir( directory )
	for fileName in directoryListing:
		joinedFileName = os.path.join( directory, fileName )
		if os.path.isdir( joinedFileName ):
			if basename == fileName:
				return joinedFileName
	return None

def getTitleFromName( title ):
	"Get the title of this preference."
	if title[ - 1 ] == ':':
		title = title[ : - 1 ]
	spaceBracketIndex = title.find( ' (' )
	if spaceBracketIndex > - 1:
		return title[ : spaceBracketIndex ]
	return title

def liftRepositoryDialogs( repositoryDialogs ):
	"Lift the repository dialogs."
	for repositoryDialog in repositoryDialogs:
		repositoryDialog.root.withdraw() # the withdraw & deiconify trick is here because lift does not work properly on my linux computer
		repositoryDialog.root.lift() # probably not necessary, here in case the withdraw & deiconify trick does not work on some other computer
		repositoryDialog.root.deiconify()
		repositoryDialog.root.lift() # probably not necessary, here in case the withdraw & deiconify trick does not work on some other computer
		repositoryDialog.root.update_idletasks()

def makeDirectory( directory ):
	"Make a directory if it does not already exist."
	if os.path.isdir( directory ):
		return
	try:
		os.makedirs( directory )
	except OSError:
		print( 'Skeinforge can not make the directory %s so give it read/write permission for that directory and the containing directory.' % directory )

def openWebPage( webPagePath ):
	"Open a web page in a browser."
	webPagePath = '"' + os.path.normpath( webPagePath ) + '"' # " to get around space in url bug
	try:
		os.startfile( webPagePath )#this is available on some python environments, but not all
		return
	except:
		pass
	webbrowserName = webbrowser.get().name
	if webbrowserName == '':
		print( 'Skeinforge was not able to open the documentation file in a web browser.  To see the documentation, open the following file in a web browser:' )
		print( webPagePath )
		return
	os.system( webbrowser.get().name + ' ' + webPagePath )#used this instead of webbrowser.open() to workaround webbrowser open() bug

def quitWindow( root ):
	"Quit a window."
	try:
		root.destroy()
	except:
		pass

def quitWindows( event = None ):
	"Quit all windows."
	global globalRepositoryDialogListTable
	globalRepositoryDialogValues = euclidean.getListTableElements( globalRepositoryDialogListTable )
	for globalRepositoryDialogValue in globalRepositoryDialogValues:
		quitWindow( globalRepositoryDialogValue.root )

def readPreferencesFromText( repository, text ):
	"Read preferences from a text."
	lines = gcodec.getTextLines( text )
	preferenceTable = {}
	for preference in repository.archive:
		preferenceTable[ preference.name ] = preference
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

def setCraftProfileArchive( craftSequence, defaultProfile, repository, fileNameHelp ):
	"Set the craft profile archive."
	addListsToRepository( repository )
	repository.baseName = getLowerNameSetHelpTitleWindowPosition( repository, fileNameHelp )
	repository.profileList = ProfileList().getFromName( repository.lowerName, 'Profile List:' )
	repository.profileListbox = ProfileListboxPreference().getFromListPreference( repository.profileList, 'Profile Selection:', repository, defaultProfile )
	repository.addListboxSelection = AddProfile().getFromProfileListboxPreferenceRepository( repository.profileListbox, repository )
	repository.deleteListboxSelection = DeleteProfile().getFromProfileListboxPreferenceRepository( repository.profileListbox, repository )
	#Create the archive, title of the dialog & repository fileName.
	repository.executeTitle = None
	directoryName = getProfilesDirectoryPath()
	makeDirectory( directoryName )
	repository.windowPositionPreferences.value = '0+400'

def setHelpPreferencesFileNameTitleWindowPosition( repository, fileNameHelp ):
	"Set the help & repository file path, the title and the window position archiver."
	baseName = getLowerNameSetHelpTitleWindowPosition( repository, fileNameHelp )
	craftTypeName = getCraftTypeName()
	selectedCraftTypeProfileBaseName  = os.path.join( getSelectedCraftTypeProfile( craftTypeName ), baseName )
	repository.baseName = os.path.join( craftTypeName, selectedCraftTypeProfileBaseName )
	dotsMinusOne = fileNameHelp.count( '.' ) - 1
	x = 0
	xAddition = 400
	for step in xrange( dotsMinusOne ):
		x += xAddition
		xAddition /= 2
	repository.windowPositionPreferences.value = '%s+0' % x

def startMainLoopFromConstructor( repository ):
	"Display the repository dialog and start the main loop."
	getDisplayedDialogFromConstructor( repository ).root.mainloop()

def updateProfileSaveListeners():
	"Call the save function of all the update profile save listeners."
	global globalProfileSaveListenerListTable
	for globalProfileSaveListener in euclidean.getListTableElements( globalProfileSaveListenerListTable ):
		globalProfileSaveListener.save()

def writePreferences( repository ):
	"Write the preferences to a file."
	profilesDirectoryPath = getProfilesDirectoryPath( repository.baseName )
	makeDirectory( os.path.dirname( profilesDirectoryPath ) )
	gcodec.writeFileText( profilesDirectoryPath, getArchiveText( repository ) )

def writePreferencesPrintMessage( repository ):
	"Set the preferences to the dialog then write them."
	writePreferences( repository )
	print( repository.title.lower().capitalize() + ' have been saved.' )


class AddProfile:
	"A class to add a profile."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.entry = Tkinter.Entry( repositoryDialog.root )
		self.entry.bind( '<Return>', self.addSelectionWithEvent )
		self.entry.grid( row = repositoryDialog.row, column = 1, columnspan = 3, sticky = Tkinter.W )
		self.addButton = Tkinter.Button( repositoryDialog.root, activebackground = 'black', activeforeground = 'white', text = 'Add Profile', command = self.addSelection )
		self.addButton.grid( row = repositoryDialog.row, column = 0 )
		repositoryDialog.row += 1

	def addSelection( self ):
		"Add the selection of a listbox preference."
		entryText = self.entry.get()
		if entryText == '':
			print( 'To add to the profiles, enter the material name.' )
			return
		self.profileListboxPreference.listPreference.setValueToFolders()
		if entryText in self.profileListboxPreference.listPreference.value:
			print( 'There is already a profile by the name of %s, so no profile will be added.' % entryText )
			return
		self.entry.delete( 0, Tkinter.END )
		craftTypeProfileDirectory = getProfilesDirectoryPath( self.profileListboxPreference.listPreference.craftTypeName )
		destinationDirectory = os.path.join( craftTypeProfileDirectory, entryText )
		shutil.copytree( self.profileListboxPreference.getSelectedFolder(), destinationDirectory )
		self.profileListboxPreference.listPreference.setValueToFolders()
		self.profileListboxPreference.value = entryText
		self.profileListboxPreference.setListboxItems()

	def addSelectionWithEvent( self, event ):
		"Add the selection of a listbox preference, given an event."
		self.addSelection()

	def getFromProfileListboxPreferenceRepository( self, profileListboxPreference, repository ):
		"Initialize."
		self.profileListboxPreference = profileListboxPreference
		repository.displayEntities.append( self )
		return self


class StringPreference:
	"A class to display, read & write a string."
	def __init__( self ):
		"Set the update function to none."
		self.updateFunction = None

	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.entry = Tkinter.Entry( repositoryDialog.root )
		self.setStateToValue()
		self.entry.grid( row = repositoryDialog.row, column = 3, columnspan = 2, sticky = Tkinter.W )
		self.label = Tkinter.Label( repositoryDialog.root, text = self.name )
		self.label.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		repositoryDialog.row += 1
		if self.updateFunction != None:
			self.entry.bind( '<Return>', self.updateFunction )

	def addToMenu( self, repositoryMenu ):
		"Add this to the repository menu."
		repositoryMenu.add_command( label = getTitleFromName( self.name ) + '...', command = self.openEntityDialog )

	def getFromValue( self, name, repository, value ):
		"Initialize."
		self.name = name
		repository.archive.append( self )
		repository.displayEntities.append( self )
		repository.menuEntities.append( self )
		self.value = value
		return self

	def getFromValueOnly( self, name, value ):
		"Initialize."
		self.value = value
		self.name = name
		return self

	def openEntityDialog( self ):
		"Open the preference dialog."
		EntityDialog( self, Tkinter.Tk() ).completeDialog()

	def setStateToValue( self ):
		"Set the entry to the value."
		try:
			self.entry.delete( 0, Tkinter.END )
			self.entry.insert( 0, self.value )
		except:
			pass

	def setToDisplay( self ):
		"Set the string to the entry field."
		try:
			valueString = self.entry.get()
			self.setValueToString( valueString )
		except:
			pass

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
		archiveWriter.write( '%s%s%s\n' % ( self.name, globalSpreadsheetSeparator, self.value ) )


class BooleanPreference( StringPreference ):
	"A class to display, read & write a boolean."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.checkbutton = Tkinter.Checkbutton( repositoryDialog.root, command = self.toggleCheckbutton, text = self.name )
#toggleCheckbutton is being used instead of a Tkinter IntVar because there is a weird bug where it doesn't work properly if this preference is not on the first window.
		self.checkbutton.grid( row = repositoryDialog.row, columnspan = 5, sticky = Tkinter.W )
		self.setStateToValue()
		repositoryDialog.row += 1

	def addToMenu( self, repositoryMenu ):
		"Add this to the repository menu."
		self.activateToggleMenuCheckbutton = False
#activateToggleMenuCheckbutton is being used instead of setting command after because add_checkbutton does not return a checkbutton.
		repositoryMenu.add_checkbutton( label = getTitleFromName( self.name ), command = self.toggleMenuCheckbutton )
		if self.value:
			repositoryMenu.invoke( repositoryMenu.index( Tkinter.END ) )
		self.activateToggleMenuCheckbutton = True

	def setStateToValue( self ):
		"Set the checkbutton to the boolean."
		try:
			if self.value:
				self.checkbutton.select()
			else:
				self.checkbutton.deselect()
		except:
			pass

	def setToDisplay( self ):
		"Do nothing because toggleCheckbutton is handling the value."
		pass

	def setValueToString( self, valueString ):
		"Set the boolean to the string."
		self.value = ( valueString.lower() == 'true' )

	def toggleCheckbutton( self ):
		"Workaround for Tkinter bug, toggle the value."
		self.value = not self.value
		self.setStateToValue()
		if self.updateFunction != None:
			self.updateFunction()

	def toggleMenuCheckbutton( self ):
		"Workaround for Tkinter bug, toggle the value."
		if self.activateToggleMenuCheckbutton:
			self.value = not self.value
			if self.updateFunction != None:
				self.updateFunction()


class CloseListener:
	"A class to listen to link a window to the global repository dialog list table."
	def __init__( self, window, closeFunction = None ):
		"Add the window to the global repository dialog list table."
		self.closeFunction = closeFunction
		self.window = window
		self.shouldWasClosedBeBound = True
		global globalRepositoryDialogListTable
		addElementToListTableIfNotThere( window, window, globalRepositoryDialogListTable )

	def listenToWidget( self, widget ):
		"Listen to the destroy message of the widget."
		if self.shouldWasClosedBeBound:
			self.shouldWasClosedBeBound = False
			widget.bind( '<Destroy>', self.wasClosed )

	def wasClosed( self, event ):
		"The dialog was closed."
		global globalCloseListTables
		for globalCloseListTable in globalCloseListTables:
			if self.window in globalCloseListTable:
				del globalCloseListTable[ self.window ]
		if self.closeFunction != None:
			self.closeFunction()


class DeleteProfile( AddProfile ):
	"A class to delete the selection of a listbox profile."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.deleteButton = Tkinter.Button( repositoryDialog.root, activebackground = 'black', activeforeground = 'white', text = "Delete Profile", command = self.deleteSelection )
		self.deleteButton.grid( row = repositoryDialog.row, column = 0 )
		repositoryDialog.row += 1

	def deleteSelection( self ):
		"Delete the selection of a listbox preference."
		DeleteProfileDialog( self.profileListboxPreference, Tkinter.Tk() )


class DeleteProfileDialog:
	def __init__( self, profileListboxPreference, root ):
		"Display a delete dialog."
		self.profileListboxPreference = profileListboxPreference
		self.root = root
		self.row = 0
		root.title( 'Delete Warning' )
		self.label = Tkinter.Label( self.root, text = 'Do you want to delete the profile?' )
		self.label.grid( row = self.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		self.row += 1
		columnIndex = 1
		deleteButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'red', command = self.delete, fg = 'red', text = 'Delete' )
		deleteButton.grid( row = self.row, column = columnIndex )
		columnIndex += 1
		noButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'darkgreen', command = self.no, fg = 'darkgreen', text = 'Do Nothing' )
		noButton.grid( row = self.row, column = columnIndex )

	def delete( self ):
		"Delete the selection of a listbox preference."
		self.profileListboxPreference.setToDisplay()
		self.profileListboxPreference.listPreference.setValueToFolders()
		if self.profileListboxPreference.value not in self.profileListboxPreference.listPreference.value:
			return
		lastSelectionIndex = 0
		currentSelectionTuple = self.profileListboxPreference.listbox.curselection()
		if len( currentSelectionTuple ) > 0:
			lastSelectionIndex = int( currentSelectionTuple[ 0 ] )
		else:
			print( 'No profile is selected, so no profile will be deleted.' )
			return
		deleteDirectory( getProfilesDirectoryPath( self.profileListboxPreference.listPreference.craftTypeName ), self.profileListboxPreference.value )
		deleteDirectory( getProfilesDirectoryInAboveDirectory( self.profileListboxPreference.listPreference.craftTypeName ), self.profileListboxPreference.value )
		self.profileListboxPreference.listPreference.setValueToFolders()
		if len( self.profileListboxPreference.listPreference.value ) < 1:
			defaultPreferencesDirectory = getProfilesDirectoryPath( os.path.join( self.profileListboxPreference.listPreference.craftTypeName, self.profileListboxPreference.defaultValue ) )
			makeDirectory( defaultPreferencesDirectory )
			self.profileListboxPreference.listPreference.setValueToFolders()
		lastSelectionIndex = min( lastSelectionIndex, len( self.profileListboxPreference.listPreference.value ) - 1 )
		self.profileListboxPreference.value = self.profileListboxPreference.listPreference.value[ lastSelectionIndex ]
		self.profileListboxPreference.setListboxItems()
		self.no()

	def no( self ):
		"The dialog was closed."
		self.root.destroy()


class DisplayToolButton:
	"A class to display the tool dialog button, in a two column wide table."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.displayButton = Tkinter.Button( repositoryDialog.root, activebackground = 'black', activeforeground = 'white', text = getEachWordCapitalized( self.name ), command = self.displayDialog )
		try:
			weightString = 'normal'
			if self.important:
				weightString = 'bold'
			splitFont = self.displayButton[ 'font' ].split()
			self.displayButton[ 'font' ] = ( splitFont[ 0 ], splitFont[ 1 ], weightString )
		except:
			pass
		if repositoryDialog.displayToolButtonStart:
			self.displayButton.grid( row = repositoryDialog.row, column = 0 )
			repositoryDialog.row += 1
			repositoryDialog.displayToolButtonStart = False
		else:
			self.displayButton.grid( row = repositoryDialog.row - 1, column = 3 )
			repositoryDialog.displayToolButtonStart = True

	def displayDialog( self ):
		"Display function."
		ToolDialog().getFromPath( self.path ).display()

	def getFromPath( self, important, name, path, repository ):
		"Initialize."
		self.important = important
		self.name = name
		self.path = path
		repository.displayEntities.append( self )
		return self


class FileHelpMenuBar:
	def __init__( self, root ):
		"Create a menu bar with a file and help menu."
		self.underlineLetters = []
		self.menuBar = Tkinter.Menu( root )
		self.root = root
		root.config( menu = self.menuBar )
		self.fileMenu = Tkinter.Menu( self.menuBar, tearoff = 0 )
		self.menuBar.add_cascade( label = "File", menu = self.fileMenu, underline = 0 )
		self.underlineLetters.append( 'f' )

	def addMenuToMenuBar( self, labelText, menu ):
		"Add a menu to the menu bar."
		lowerLabelText = labelText.lower()
		for underlineLetterIndex in xrange( len( lowerLabelText ) ):
			underlineLetter = lowerLabelText[ underlineLetterIndex ]
			if underlineLetter not in self.underlineLetters:
				self.underlineLetters.append( underlineLetter )
				self.menuBar.add_cascade( label = labelText, menu = menu, underline = underlineLetterIndex )
				return
		self.menuBar.add_cascade( label = labelText, menu = menu )

	def addPluginToMenuBar( self, modulePath, repository, window ):
		"Add a menu to the menu bar from a tool."
		pluginModule = gcodec.getModuleWithPath( modulePath )
		if pluginModule == None:
			print( 'this should never happen, pluginModule in addMenuToMenuBar in preferences is None.' )
			return None
		repositoryMenu = Tkinter.Menu( self.menuBar, tearoff = 0 )
		labelText = getEachWordCapitalized( os.path.basename( modulePath ) )
		self.addMenuToMenuBar( labelText, repositoryMenu )
		pluginModule.addToMenu( self.root, repositoryMenu, repository, window )

	def completeMenu( self, closeFunction, repository, window ):
		"Complete the menu."
		addAcceleratorCommand( '<Control-KeyPress-w>', closeFunction, self.root, self.fileMenu, 'Close' )
		self.fileMenu.add_separator()
		addAcceleratorCommand( '<Control-KeyPress-q>', quitWindows, self.root, self.fileMenu, 'Quit' )
		skeinforgeToolsDirectoryPath = getSkeinforgeToolsDirectoryPath()
		pluginFilenames = gcodec.getPluginFilenamesFromDirectoryPath( skeinforgeToolsDirectoryPath )
		for pluginFilename in pluginFilenames:
			self.addPluginToMenuBar( os.path.join( skeinforgeToolsDirectoryPath, pluginFilename ), repository, window )


class Filename( StringPreference ):
	"A class to display, read & write a fileName."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		repositoryDialog.executables.append( self )

	def execute( self ):
		"Open the file picker."
		try:
			import tkFileDialog
			summarized = gcodec.getSummarizedFilename( self.value )
			initialDirectory = os.path.dirname( summarized )
			if len( initialDirectory ) > 0:
				initialDirectory += os.sep
			else:
				initialDirectory = "."
			fileName = tkFileDialog.askopenfilename( filetypes = self.getFilenameFirstTypes(), initialdir = initialDirectory, initialfile = os.path.basename( summarized ), title = self.name )
			self.setCancelledValue( fileName )
			return
		except:
			print( 'Could not get the old directory in preferences, so the file picker will be opened in the default directory.' )
		try:
			fileName = tkFileDialog.askopenfilename( filetypes = self.getFilenameFirstTypes(), initialdir = '.', initialfile = '', title = self.name )
			self.setCancelledValue( fileName )
		except:
			print( 'Error in execute in Filename in preferences, ' + self.name )

	def getFromFilename( self, fileTypes, name, repository, value ):
		"Initialize."
		self.getFromValueOnly( name, value )
		self.fileTypes = fileTypes
		repository.archive.append( self )
		repository.displayEntities.append( self )
		self.wasCancelled = False
		return self

	def getFilenameFirstTypes( self ):
		"Get the file types with the file type of the fileName moved to the front of the list."
		try:
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
		except:
			return [ ( 'All', '*.*' ) ]

	def setCancelledValue( self, fileName ):
		"Set the value to the file name and wasCancelled true if a file was not picked."
		if ( str( fileName ) == '()' or str( fileName ) == '' ):
			self.wasCancelled = True
		else:
			self.value = fileName

	def setToDisplay( self ):
		"Do nothing because the file dialog is handling the value."
		pass


class FloatPreference( StringPreference ):
	"A class to display, read & write a float."
	def setStateToValue( self ):
		"Set the entry to the value."
		try:
			self.entry.delete( 0, Tkinter.END )
			self.entry.insert( 0, str( self.value ) )
		except:
			pass

	def setValueToString( self, valueString ):
		"Set the float to the string."
		try:
			self.value = float( valueString )
		except:
			print( 'Oops, can not read float' + self.name + ' ' + valueString )


class HelpPage:
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.displayButton = Tkinter.Button( repositoryDialog.root, activebackground = 'black', activeforeground = 'white', text = getEachWordCapitalized( self.name ), command = self.openPage )
		self.displayButton.grid( row = repositoryDialog.row - 1, column = 3, columnspan = 2 )

	def addToMenu( self, repositoryMenu ):
		"Add this to the repository menu."
		repositoryMenu.add_command( label = getTitleFromName( self.name ), command = self.openPage )

	def getFromNameAfterHTTP( self, afterHTTP, name, repository ):
		"Initialize."
		self.setToNameRepository( name, repository )
		self.hypertextAddress = 'http://' + afterHTTP
		return self

	def getFromNameAfterWWW( self, afterWWW, name, repository ):
		"Initialize."
		self.setToNameRepository( name, repository )
		self.hypertextAddress = 'http://www.' + afterWWW
		return self

	def getFromNameSubName( self, name, repository, subName = '' ):
		"Initialize."
		self.setToNameRepository( name, repository )
		self.hypertextAddress = getDocumentationPath( subName )
		return self

	def getOpenFromAbsolute( self, hypertextAddress ):
		"Get the open help page function from the hypertext address."
		self.hypertextAddress = hypertextAddress
		return self.openPage

	def getOpenFromAfterHTTP( self, afterHTTP ):
		"Get the open help page function from the part of the address after the HTTP."
		self.hypertextAddress = 'http://' + afterHTTP
		return self.openPage

	def getOpenFromAfterWWW( self, afterWWW ):
		"Get the open help page function from the afterWWW of the address after the www."
		self.hypertextAddress = 'http://www.' + afterWWW
		return self.openPage

	def getOpenFromDocumentationSubName( self, subName = '' ):
		"Get the open help page function from the afterWWW of the address after the www."
		self.hypertextAddress = getDocumentationPath( subName )
		return self.openPage

	def openPage( self, event = None ):
		"Open the browser to the hypertext address."
		openWebPage( self.hypertextAddress )

	def setToNameRepository( self, name, repository ):
		"Set to the name and repository."
		self.name = name
		repository.displayEntities.append( self )
		repository.menuEntities.append( self )


class IntPreference( FloatPreference ):
	"A class to display, read & write an int."
	def setValueToString( self, valueString ):
		"Set the integer to the string."
		dotIndex = valueString.find( '.' )
		if dotIndex > - 1:
			valueString = valueString[ : dotIndex ]
		try:
			self.value = int( valueString )
			return
		except:
			print( 'Warning, can not read integer ' + self.name + ' ' + valueString )
			print( 'Will try reading as a boolean, which might be a mistake.' )
		self.value = 0
		if valueString.lower() == 'true':
			self.value = 1


class LabelDisplay:
	"A class to add a label."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.label = Tkinter.Label( repositoryDialog.root, text = self.name )
		self.label.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		repositoryDialog.row += 1

	def getFromName( self, name, repository ):
		"Initialize."
		self.name = name
		repository.displayEntities.append( self )
		return self


class LabelSeparator:
	"A class to add a label and menu separator."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.label = Tkinter.Label( repositoryDialog.root, text = '' )
		self.label.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		repositoryDialog.row += 1

	def addToMenu( self, repositoryMenu ):
		"Add this to the repository menu."
		repositoryMenu.add_separator()

	def getFromRepository( self, repository ):
		"Initialize."
		repository.displayEntities.append( self )
		repository.menuEntities.append( self )
		return self


class MenuButtonDisplay:
	"A class to add a menu button."
	def addRadiosToDialog( self, repositoryDialog ):
		"Add the menu radios to the dialog."
		for menuRadio in self.menuRadios:
			menuRadio.addToDialog( repositoryDialog )

	def addToMenu( self, repositoryMenu ):
		"Add this to the repository menu."
		if len( self.menuRadios ) < 1:
			print( 'The MenuButtonDisplay in preferences should have menu items.' )
			print( self.name )
			return
		self.menu = Tkinter.Menu( repositoryMenu, tearoff = 0 )
		repositoryMenu.add_cascade( label = getTitleFromName( self.name ), menu = self.menu )
		self.setRadioVarToName( self.menuRadios[ 0 ].name )

	def getFromName( self, name, repository ):
		"Initialize."
		self.menuRadios = []
		self.name = name
		self.radioVar = None
		repository.menuEntities.append( self )
		return self

	def removeMenus( self ):
		"Remove all menus."
		deleteMenuItems( self.menu )
		self.menuRadios = []

	def setRadioVarToName( self, name ):
		"Get the menu button."
		self.optionList = [ name ]
		self.radioVar = Tkinter.StringVar()
		self.radioVar.set( self.optionList[ 0 ] )

	def setToNameAddToDialog( self, name, repositoryDialog ):
		"Get the menu button."
		if self.radioVar != None:
			return
		self.setRadioVarToName( name )
		self.label = Tkinter.Label( repositoryDialog.root, text = self.name )
		self.label.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		self.menuButton = Tkinter.OptionMenu( repositoryDialog.root, self.radioVar, self.optionList )
		self.menuButton.grid( row = repositoryDialog.row, column = 3, columnspan = 2, sticky = Tkinter.W )
		self.menuButton.menu = Tkinter.Menu( self.menuButton, tearoff = 0 )
		self.menu = self.menuButton.menu
		self.menuButton[ 'menu' ]  =  self.menu
		repositoryDialog.row += 1


class MenuRadio( BooleanPreference ):
	"A class to display, read & write a boolean with associated menu radio button."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.menuButtonDisplay.setToNameAddToDialog( self.name, repositoryDialog )
		self.addToSubmenu()

	def addToMenu( self, repositoryMenu ):
		"Add this to the submenu set by MenuButtonDisplay, the repository menu is ignored"
		self.addToSubmenu()

	def addToSubmenu( self ):
		"Add this to the submenu."
		self.activate = False
		menu = self.menuButtonDisplay.menu
		menu.add_radiobutton( label = self.name, command = self.clickRadio, value = self.name, variable = self.menuButtonDisplay.radioVar )
		self.menuLength = menu.index( Tkinter.END )
		if self.value:
			self.menuButtonDisplay.radioVar.set( self.name )
			self.menuButtonDisplay.menu.invoke( self.menuLength )
		self.activate = True

	def clickRadio( self ):
		"Workaround for Tkinter bug, invoke and set the value when clicked."
		if not self.activate:
			return
		self.menuButtonDisplay.radioVar.set( self.name )
		if self.updateFunction != None:
			self.updateFunction()

	def getFromMenuButtonDisplay( self, menuButtonDisplay, name, repository, value ):
		"Initialize."
		self.getFromValueOnly( name, value )
		self.menuButtonDisplay = menuButtonDisplay
		self.menuButtonDisplay.menuRadios.append( self )
		repository.archive.append( self )
		repository.displayEntities.append( self )
		repository.menuEntities.append( self )
		return self

	def setToDisplay( self ):
		"Set the boolean to the checkbutton."
		if self.menuButtonDisplay.radioVar != None:
			self.value = ( self.menuButtonDisplay.radioVar.get() == self.name )


class ProfileList:
	"A class to list the profiles."
	def getFromName( self, craftTypeName, name ):
		"Initialize."
		self.craftTypeName = craftTypeName
		self.name = name
		self.setValueToFolders()
		return self

	def setValueToFolders( self ):
		"Set the value to the folders in the profiles directories."
		self.value = getFolders( getProfilesDirectoryPath( self.craftTypeName ) )
		defaultFolders = getFolders( getProfilesDirectoryInAboveDirectory( self.craftTypeName ) )
		for defaultFolder in defaultFolders:
			if defaultFolder not in self.value:
				self.value.append( defaultFolder )
		self.value.sort()


class ProfileListboxPreference( StringPreference ):
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
#http://www.pythonware.com/library/tkinter/introduction/x5453-patterns.htm
		self.root = repositoryDialog.root
		frame = Tkinter.Frame( repositoryDialog.root )
		scrollbar = Tkinter.Scrollbar( frame, orient = Tkinter.VERTICAL )
		self.listbox = Tkinter.Listbox( frame, selectmode = Tkinter.SINGLE, yscrollcommand = scrollbar.set )
		self.listbox.bind( '<ButtonRelease-1>', self.buttonReleaseOne )
		repositoryDialog.root.bind( '<FocusIn>', self.focusIn )
		scrollbar.config( command = self.listbox.yview )
		scrollbar.pack( side = Tkinter.RIGHT, fill = Tkinter.Y )
		self.listbox.pack( side = Tkinter.LEFT, fill = Tkinter.BOTH, expand = 1 )
		self.setListboxItems()
		frame.grid( row = repositoryDialog.row, columnspan = 5, sticky = Tkinter.W )
		repositoryDialog.row += 1
		repositoryDialog.saveListenerTable[ 'updateProfileSaveListeners' ] = updateProfileSaveListeners

	def buttonReleaseOne( self, event ):
		"Button one released."
		self.setValueToIndex( self.listbox.nearest( event.y ) )

	def focusIn( self, event ):
		"The root has gained focus."
		self.setListboxItems()

	def getFromListPreference( self, listPreference, name, repository, value ):
		"Initialize."
		self.getFromValueOnly( name, value )
		self.defaultValue = value
		self.listPreference = listPreference
		repository.archive.append( self )
		repository.displayEntities.append( self )
		return self

	def getSelectedFolder( self ):
		"Get the selected folder."
		preferenceProfileSubfolder = getSubfolderWithBasename( self.value, getProfilesDirectoryPath( self.listPreference.craftTypeName ) )
		if preferenceProfileSubfolder != None:
			return preferenceProfileSubfolder
		toolProfileSubfolder = getSubfolderWithBasename( self.value, getProfilesDirectoryInAboveDirectory( self.listPreference.craftTypeName ) )
		return toolProfileSubfolder

	def setListboxItems( self ):
		"Set the listbox items to the list preference."
		self.listbox.delete( 0, Tkinter.END )
		for item in self.listPreference.value:
			self.listbox.insert( Tkinter.END, item )
			if self.value == item:
				self.listbox.select_set( Tkinter.END )

	def setToDisplay( self ):
		"Set the selection value to the listbox selection."
		currentSelectionTuple = self.listbox.curselection()
		if len( currentSelectionTuple ) > 0:
			self.setValueToIndex( int( currentSelectionTuple[ 0 ] ) )

	def setValueToIndex( self, index ):
		"Set the selection value to the index."
		valueString = self.listbox.get( index )
		self.setValueToString( valueString )

	def setValueToString( self, valueString ):
		"Set the string to the value string."
		self.value = valueString
		if self.getSelectedFolder() == None:
			self.value = self.defaultValue
		if self.getSelectedFolder() == None:
			if len( self.listPreference.value ) > 0:
				self.value = self.listPreference.value[ 0 ]


class ProfileMenuRadio:
	"A class to display a profile menu radio button."
	def __init__( self, profilePluginFilename, menu, name, radioVar, value ):
		"Create a profile menu radio."
		self.activate = False
		self.menu = menu
		self.name = name
		self.profileJoinName = profilePluginFilename + '.& /' + name
		self.profilePluginFilename = profilePluginFilename
		self.radioVar = radioVar
		menu.add_radiobutton( label = name.replace( '_', ' ' ), command = self.clickRadio, value = self.profileJoinName, variable = self.radioVar )
		self.menuLength = menu.index( Tkinter.END )
		if value:
			self.radioVar.set( self.profileJoinName )
			self.menu.invoke( self.menuLength )
		self.activate = True

	def clickRadio( self ):
		"Workaround for Tkinter bug, invoke and set the value when clicked."
		if not self.activate:
			return
		self.radioVar.set( self.profileJoinName )
		pluginModule = getCraftTypePluginModule( self.profilePluginFilename )
		profilePluginPreferences = getReadRepository( pluginModule.getRepositoryConstructor() )
		profilePluginPreferences.profileListbox.value = self.name
		writePreferences( profilePluginPreferences )
		profilePreferences = getReadProfileRepository()
		plugins = profilePreferences.craftRadios
		for plugin in plugins:
			plugin.value = ( plugin.name == self.profilePluginFilename )
		writePreferences( profilePreferences )
		updateProfileSaveListeners()


class ProfileSelectionMenuRadio:
	"A class to display a profile selection menu radio button."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.menuButtonDisplay.setToNameAddToDialog( self.valueName, repositoryDialog )
		self.activate = False
		self.menuButtonDisplay.menu.add_radiobutton( label = self.valueName, command = self.clickRadio, value = self.valueName, variable = self.menuButtonDisplay.radioVar )
		self.menuLength = self.menuButtonDisplay.menu.index( Tkinter.END )
		if self.value:
			self.menuButtonDisplay.radioVar.set( self.valueName )
			self.menuButtonDisplay.menu.invoke( self.menuLength )
		self.activate = True
		global globalProfileSaveListenerListTable
		addElementToListTableIfNotThere( repositoryDialog.repository, repositoryDialog, globalProfileSaveListenerListTable )

	def clickRadio( self ):
		"Workaround for Tkinter bug, invoke and set the value when clicked."
		if not self.activate:
			return
		self.menuButtonDisplay.radioVar.set( self.valueName )
		pluginModule = getCraftTypePluginModule()
		profilePluginPreferences = getReadRepository( pluginModule.getRepositoryConstructor() )
		profilePluginPreferences.profileListbox.value = self.name
		writePreferences( profilePluginPreferences )
		updateProfileSaveListeners()

	def getFromMenuButtonDisplay( self, menuButtonDisplay, name, repository, value ):
		"Initialize."
		self.setToMenuButtonDisplay( menuButtonDisplay, name, repository, value )
		self.valueName = name.replace( '_', ' ' )
		return self

	def setToMenuButtonDisplay( self, menuButtonDisplay, name, repository, value ):
		"Initialize."
		self.menuButtonDisplay = menuButtonDisplay
		self.menuButtonDisplay.menuRadios.append( self )
		self.name = name
		self.value = value
		repository.displayEntities.append( self )


class ProfileTypeMenuRadio( ProfileSelectionMenuRadio ):
	"A class to display a profile type menu radio button."
	def clickRadio( self ):
		"Workaround for Tkinter bug, invoke and set the value when clicked."
		if not self.activate:
			return
		self.menuButtonDisplay.radioVar.set( self.valueName )
		profilePreferences = getReadProfileRepository()
		plugins = profilePreferences.craftRadios
		for plugin in plugins:
			plugin.value = ( plugin.name == self.name )
		writePreferences( profilePreferences )
		updateProfileSaveListeners()

	def getFromMenuButtonDisplay( self, menuButtonDisplay, name, repository, value ):
		"Initialize."
		self.setToMenuButtonDisplay( menuButtonDisplay, name, repository, value )
		self.valueName = getEachWordCapitalized( name )
		return self


class Radio( BooleanPreference ):
	"A class to display, read & write a boolean with associated radio button."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.radiobutton = Tkinter.Radiobutton( repositoryDialog.root, command = self.clickRadio, text = self.name, value = repositoryDialog.row, variable = self.getIntVar() )
		self.radiobutton.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		self.setDisplayState()
		repositoryDialog.row += 1

	def clickRadio( self ):
		"Workaround for Tkinter bug, set the value."
		self.getIntVar().set( self.radiobutton[ 'value' ] )

	def getFromRadio( self, name, radio, repository, value ):
		"Initialize."
		self.getFromValueOnly( name, value )
		self.radio = radio
		repository.archive.append( self )
		repository.displayEntities.append( self )
#when addToMenu is added to this entity, the line below should be uncommented
#		repository.menuEntities.append( self )
		return self

	def getIntVar( self ):
		"Get the IntVar for this radio button group."
		if len( self.radio ) == 0:
			self.radio.append( Tkinter.IntVar() )
		return self.radio[ 0 ]

	def setToDisplay( self ):
		"Set the boolean to the checkbutton."
		self.value = ( self.getIntVar().get() == self.radiobutton[ 'value' ] )

	def setDisplayState( self ):
		"Set the checkbutton to the boolean."
		if self.value:
			self.setSelect()

	def setSelect( self ):
		"Set the int var and select the radio button."
		self.getIntVar().set( self.radiobutton[ 'value' ] )
		self.radiobutton.select()


class RadioCapitalized( Radio ):
	"A class to display, read & write a boolean with associated radio button."
	def addRadioCapitalizedToDialog( self, repositoryDialog ):
		"Add radio capitalized button to the dialog."
		capitalizedName = getEachWordCapitalized( self.name )
		self.radiobutton = Tkinter.Radiobutton( repositoryDialog.root, command = self.clickRadio, text = capitalizedName, value = repositoryDialog.row, variable = self.getIntVar() )
		self.radiobutton.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		self.setDisplayState()
		repositoryDialog.row += 1

	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.addRadioCapitalizedToDialog( repositoryDialog )


class RadioCapitalizedButton( RadioCapitalized ):
	"A class to display, read & write a boolean with associated radio button."
	def addRadioCapitalizedButtonToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.displayButton = Tkinter.Button( repositoryDialog.root, activebackground = 'black', activeforeground = 'white', text = getEachWordCapitalized( self.name ), command = self.displayDialog )
		self.displayButton.grid( row = repositoryDialog.row, column = 3, columnspan = 2 )
		self.addRadioCapitalizedToDialog( repositoryDialog )

	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.addRadioCapitalizedButtonToDialog( repositoryDialog )

	def displayDialog( self ):
		"Display function."
		ToolDialog().getFromPath( self.path ).display()
		self.setSelect()

	def getFromPath( self, name, path, radio, repository, value ):
		"Initialize."
		self.getFromRadio( name, radio, repository, value )
		self.path = path
		return self


class RadioCapitalizedProfileButton( RadioCapitalizedButton ):
	"A class to display, read & write a boolean with associated radio button."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.addRadioCapitalizedButtonToDialog( repositoryDialog )
		repositoryDialog.saveListenerTable[ 'updateProfileSaveListeners' ] = updateProfileSaveListeners


class TextPreference( StringPreference ):
	"A class to display, read & write a text."
	def __init__( self ):
		"Set the update function to none."
		self.tokenConversions = [
			TokenConversion(),
			TokenConversion( 'carriageReturn', '\r' ),
			TokenConversion( 'doubleQuote', '"' ),
			TokenConversion( 'newline', '\n' ),
			TokenConversion( 'semicolon', ';' ),
			TokenConversion( 'singleQuote', "'" ),
			TokenConversion( 'tab', '\t' ) ]
		self.updateFunction = None

	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.label = Tkinter.Label( repositoryDialog.root, text = self.name )
		self.label.grid( row = repositoryDialog.row, column = 0, columnspan = 3, sticky = Tkinter.W )
		repositoryDialog.row += 1
		self.entry = Tkinter.Text( repositoryDialog.root )
		self.setStateToValue()
		self.entry.grid( row = repositoryDialog.row, column = 0, columnspan = 5, sticky = Tkinter.W )
		repositoryDialog.row += 1

	def getFromValue( self, name, repository, value ):
		"Initialize."
		self.name = name
		repository.archive.append( self )
		repository.displayEntities.append( self )
		self.value = value
		return self

	def setToDisplay( self ):
		"Set the string to the entry field."
		valueString = self.entry.get( 1.0, Tkinter.END )
		self.setValueToString( valueString )

	def setStateToValue( self ):
		"Set the entry to the value."
		try:
			self.entry.delete( 1.0, Tkinter.END )
			self.entry.insert( Tkinter.INSERT, self.value )
		except:
			pass

	def setValueToSplitLine( self, lineIndex, lines, splitLine ):
		"Set the value to the second word of a split line."
		replacedValue = splitLine[ 1 ]
		for tokenConversion in reversed( self.tokenConversions ):
			replacedValue = tokenConversion.getTokenizedString( replacedValue )
		self.setValueToString( replacedValue )

	def writeToArchiveWriter( self, archiveWriter ):
		"Write tab separated name and value to the archive writer."
		replacedValue = self.value
		for tokenConversion in self.tokenConversions:
			replacedValue = tokenConversion.getNamedString( replacedValue )
		archiveWriter.write( '%s%s%s\n' % ( self.name, globalSpreadsheetSeparator, replacedValue ) )


class TokenConversion:
	"A class to convert tokens in a string."
	def __init__( self, name = 'replaceToken', token = '___replaced___' ):
		"Set the name and token."
		self.replacedName = '___replaced___' + name
		self.token = token

	def getNamedString( self, text ):
		"Get a string with the tokens changed to names."
		return text.replace( self.token, self.replacedName )

	def getTokenizedString( self, text ):
		"Get a string with the names changed to tokens."
		return text.replace( self.replacedName, self.token )


class ToolDialog:
	"A class to display the tool repository dialog."
	def addPluginToMenu( self, menu, path ):
		"Add the display command to the menu."
		name = os.path.basename( path )
		self.path = path
		menu.add_command( label = getEachWordCapitalized( name ) + '...', command = self.display )

	def display( self ):
		"Display the tool repository dialog."
		global globalRepositoryDialogListTable
		for repositoryDialog in globalRepositoryDialogListTable:
			if getPathFromFileNameHelp( repositoryDialog.repository.fileNameHelp ) == self.path:
				liftRepositoryDialogs( globalRepositoryDialogListTable[ repositoryDialog ] )
				return
		self.repositoryDialog = getDisplayedDialogFromPath( self.path )

	def getFromPath( self, path ):
		"Initialize and return display function."
		self.path = path
		return self


class WindowPosition( StringPreference ):
	"A class to display, read & write a window position."
	def addToDialog( self, repositoryDialog ):
		"Set the root to later get the geometry."
		self.root = repositoryDialog.root
		self.windowPositionName = 'windowPosition' + repositoryDialog.repository.title
		self.setToDisplay()

	def getFromValue( self, name, repository, value ):
		"Initialize."
		self.name = name
		repository.archive.append( self )
		repository.displayEntities.append( self )
		self.value = value
		return self

	def setToDisplay( self ):
		"Set the string to the window position."
		if self.name != self.windowPositionName:
			return
		try:
			geometryString = self.root.geometry()
		except:
			return
		if geometryString == '1x1+0+0':
			return
		firstPlusIndexPlusOne = geometryString.find( '+' ) + 1
		self.value = geometryString[ firstPlusIndexPlusOne : ]

	def setWindowPosition( self ):
		"Set the window position."
		movedGeometryString = '%sx%s+%s' % ( self.root.winfo_reqwidth(), self.root.winfo_reqheight(), self.value )
		self.root.geometry( movedGeometryString )


class WindowVisibilities:
	"A class to read & write window visibilities and display them."
	def addToDialog( self, repositoryDialog ):
		"Add this to the dialog."
		self.isActive = repositoryDialog.isFirst
		if self.isActive:
			repositoryDialog.openDialogListeners.append( self )

	def getFromRepository( self, repository ):
		"Initialize."
		self.isActive = False
		self.name = 'WindowVisibilities'
		self.repository = repository
		repository.archive.append( self )
		repository.displayEntities.append( self )
		self.value = []
		return self

	def openDialog( self ):
		"Create the display button."
		for item in self.value:
			getDisplayedDialogFromPath( item )

	def setToDisplay( self ):
		"Set the string to the window position."
		if not self.isActive:
			return
		self.value = []
		ownPath = getPathFromFileNameHelp( self.repository.fileNameHelp )
		for repositoryDialog in globalRepositoryDialogListTable.keys():
			keyPath = getPathFromFileNameHelp( repositoryDialog.repository.fileNameHelp )
			if keyPath != ownPath:
				if keyPath not in self.value:
					self.value.append( keyPath )

	def setValueToSplitLine( self, lineIndex, lines, splitLine ):
		"Set the value to the second and later words of a split line."
		self.value = splitLine[ 1 : ]

	def writeToArchiveWriter( self, archiveWriter ):
		"Write tab separated name and list to the archive writer."
		archiveWriter.write( self.name )
		for item in self.value:
			archiveWriter.write( globalSpreadsheetSeparator )
			archiveWriter.write( item )
		archiveWriter.write( '\n' )


class EntityDialog:
	def __init__( self, entity, root ):
		"Create display preference dialog."
		self.closeListener = CloseListener( self )
		self.entity = entity
		self.repository = entity.repository
		self.root = root
		self.row = 0
		root.withdraw()
		root.title( getTitleFromName( entity.name ) )

	def completeDialog( self ):
		"Complet the display preference dialog."
		self.entity.addToDialog( self )
		Tkinter.Label( self.root ).grid( row = self.row )
		self.row += 1
		columnIndex = 0
		cancelButton = Tkinter.Button( self.root, activebackground = 'black', activeforeground = 'red', command = self.close, fg = 'red', text = 'Cancel' )
		cancelButton.grid( row = self.row, column = columnIndex )
		self.closeListener.listenToWidget( cancelButton )
		columnIndex += 1
		saveCloseButton = Tkinter.Button( self.root, activebackground = 'black', activeforeground = 'orange', command = self.saveClose, fg = 'orange', text = 'Save and Close' )
		saveCloseButton.grid( row = self.row, column = columnIndex )
		columnIndex += 1
		self.saveButton = Tkinter.Button( self.root, activebackground = 'black', activeforeground = 'darkgreen', command = self.save, fg = 'darkgreen', text = 'Save' )
		self.saveButton.grid( row = self.row, column = columnIndex )
		self.root.update_idletasks()
		self.root.deiconify()
		return self

	def close( self ):
		"The dialog was closed."
		try:
			self.root.destroy()
		except:
			pass

	def save( self ):
		"Set the preferences to the dialog then write them."
		self.entity.setToDisplay()
		fileText = gcodec.getFileText( getProfilesDirectoryPath( self.repository.baseName ), 'r', False )
		archiveText = getArchiveText( self.repository )
		if archiveText == fileText:
			return
		writePreferencesPrintMessage( self.repository )
		if self.entity.updateFunction != None:
			self.entity.updateFunction()

	def saveClose( self ):
		"Set the preferences to the dialog, write them, then destroy the window."
		self.save()
		self.close()


class RepositoryDialog:
	def __init__( self, repository, root ):
		"Add entities to the dialog."
		self.isFirst = ( len( globalRepositoryDialogListTable.keys() ) == 0 )
		self.closeListener = CloseListener( self )
		self.repository = repository
		self.displayToolButtonStart = True
		self.executables = []
		self.root = root
		self.openDialogListeners = []
		self.openHelpPage = HelpPage().getOpenFromDocumentationSubName( self.repository.fileNameHelp )
		self.row = 0
		self.saveListenerTable = {}
		repository.repositoryDialog = self
		root.title( repository.title )
		fileHelpMenuBar = FileHelpMenuBar( root )
		addAcceleratorCommand( '<Control-KeyPress-s>', self.save, self.root, fileHelpMenuBar.fileMenu, 'Save' )
		fileHelpMenuBar.fileMenu.add_command( label = "Save and Close", command = self.saveClose )
		fileHelpMenuBar.completeMenu( self.close, repository, self )
		if len( repository.displayEntities ) > 30:
			self.addButtons( repository, root )
		for preference in repository.displayEntities:
			preference.addToDialog( self )
		if self.row < 20:
			self.addEmptyRow()
		self.addButtons( repository, root )
		root.withdraw()
		root.update_idletasks()
		self.setWindowPositionDeiconify()
		root.deiconify()
		for openDialogListener in self.openDialogListeners:
			openDialogListener.openDialog()

	def __repr__( self ):
		"Get the string representation of this RepositoryDialog."
		return self.repository.title

	def addButtons( self, repository, root ):
		"Add buttons to the dialog."
		columnIndex = 0
		cancelCommand = self.close
		cancelText = 'Cancel'
		saveCommand = self.save
		saveText = 'Save'
		saveCloseCommand = self.saveClose
		saveCloseText = repository.saveCloseTitle
		if self.isFirst:
			cancelCommand = self.iconify
			cancelText = 'Iconify'
			saveCommand = self.saveAll
			saveText = 'Save All'
			saveCloseCommand = self.saveReturnAll
			saveCloseText = 'Save and Return All'
		if repository.executeTitle != None:
			executeButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'blue', text = repository.executeTitle, command = self.execute )
			executeButton.grid( row = self.row, column = columnIndex )
			columnIndex += 1
		self.helpButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'white', text = "?", command = self.openHelpPage )
		self.helpButton.grid( row = self.row, column = columnIndex )
		self.closeListener.listenToWidget( self.helpButton )
		columnIndex += 1
		cancelButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'red', command = cancelCommand, fg = 'red', text = cancelText )
		cancelButton.grid( row = self.row, column = columnIndex )
		columnIndex += 1
		if repository.saveCloseTitle != None:
			saveCloseButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'orange', command = saveCloseCommand, fg = 'orange', text = saveCloseText )
			saveCloseButton.grid( row = self.row, column = columnIndex )
			columnIndex += 1
		self.saveButton = Tkinter.Button( root, activebackground = 'black', activeforeground = 'darkgreen', command = saveCommand, fg = 'darkgreen', text = saveText )
		self.saveButton.grid( row = self.row, column = columnIndex )
		self.row += 1

	def addEmptyRow( self ):
		"Add an empty row."
		Tkinter.Label( self.root ).grid( row = self.row )
		self.row += 1

	def close( self, event = None ):
		"The dialog was closed."
		try:
			self.root.destroy()
		except:
			pass

	def execute( self ):
		"The execute button was clicked."
		for executable in self.executables:
			executable.execute()
		self.save()
		self.repository.execute()
#		self.close()

	def iconify( self ):
		"The dialog was iconified."
		self.root.iconify()
		print( 'The first window, %s, has been iconified.' % self.repository.title )

	def save( self, event = None ):
		"Set the entities to the dialog then write them."
		for preference in self.repository.archive:
			preference.setToDisplay()
		writePreferencesPrintMessage( self.repository )
		for saveListener in self.saveListenerTable.values():
			saveListener()

	def saveAll( self ):
		"Save all the dialogs."
		global globalRepositoryDialogListTable
		globalRepositoryDialogValues = euclidean.getListTableElements( globalRepositoryDialogListTable )
		for globalRepositoryDialogValue in globalRepositoryDialogValues:
			globalRepositoryDialogValue.save()

	def saveClose( self ):
		"Set the entities to the dialog, write them, then destroy the window."
		self.save()
		self.close()

	def saveReturnAll( self ):
		"Save and return all the dialogs."
		self.saveAll()
		global globalRepositoryDialogListTable
		repositoryDialogListTableCopy = globalRepositoryDialogListTable.copy()
		del repositoryDialogListTableCopy[ self ]
		repositoryDialogCopyValues = euclidean.getListTableElements( repositoryDialogListTableCopy )
		for repositoryDialogCopyValue in repositoryDialogCopyValues:
			repositoryDialogCopyValue.close()

	def setWindowPositionDeiconify( self ):
		"Set the window position if that preference exists."
		windowPositionName = 'windowPosition' + self.repository.title
		for preference in self.repository.archive:
			if isinstance( preference, WindowPosition ):
				if preference.name == windowPositionName:
					preference.setWindowPosition()
					return


class ProfileRepository:
	"A class to handle the profile entities."
	def __init__( self ):
		"Set the default entities, execute title & repository fileName."
		#Set the default entities.
		addListsToRepository( self )
		profilePluginsDirectoryPath = getPluginsDirectoryPath()
		self.craftTypeLabel = LabelDisplay().getFromName( 'Craft Types: ', self )
		craftTypeFilenames = getPluginFilenames()
		craftTypeRadio = []
		self.craftRadios = []
		craftTypeFilenames.sort()
		for craftTypeFilename in craftTypeFilenames:
			path = os.path.join( profilePluginsDirectoryPath, craftTypeFilename )
			craftRadio = RadioCapitalizedProfileButton().getFromPath( craftTypeFilename, path, craftTypeRadio, self, craftTypeFilename == 'extrusion' )
			self.craftRadios.append( craftRadio )
		#Create the archive, title of the dialog & repository fileName.
		self.executeTitle = None
		directoryName = getProfilesDirectoryPath()
		makeDirectory( directoryName )
		self.baseName = getLowerNameSetHelpTitleWindowPosition( self, 'skeinforge_tools.profile.html' )
		self.windowPositionPreferences.value = '0+200'

