"""
Craft is a script to access the plugins which craft a gcode file.

The plugin buttons which are commonly used are bolded and the ones which are rarely used have normal font weight.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import os
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addSubmenus( menu, pluginFilename, pluginFolderPath, pluginPath ):
	"Add a tool plugin menu."
	submenu = preferences.Tkinter.Menu( menu, tearoff = 0 )
	menu.add_cascade( label = pluginFilename.capitalize(), menu = submenu )
	preferences.ToolDialog().addPluginToMenu( submenu, pluginPath )
	submenu.add_separator()
	submenuFilenames = gcodec.getPluginFilenamesFromDirectoryPath( pluginFolderPath )
	for submenuFilename in submenuFilenames:
		preferences.ToolDialog().addPluginToMenu( submenu, os.path.join( pluginFolderPath, submenuFilename ) )

def addToCraftMenu( menu ):
	"Add a craft plugin menu."
	preferences.ToolDialog().addPluginToMenu( menu, gcodec.getUntilDot( os.path.abspath( __file__ ) ) )
	menu.add_separator()
	directoryPath = getPluginsDirectoryPath()
	directoryFolders = preferences.getFolders( directoryPath )
	pluginFilenames = getPluginFilenames()
	for pluginFilename in pluginFilenames:
		pluginFolderName = pluginFilename + '_plugins'
		pluginPath = os.path.join( directoryPath, pluginFilename )
		if pluginFolderName in directoryFolders:
			addSubmenus( menu, pluginFilename, os.path.join( directoryPath, pluginFolderName ), pluginPath )
		else:
			preferences.ToolDialog().addPluginToMenu( menu, pluginPath )

def addToMenu( master, menu, repository, window ):
	"Add a tool plugin menu."
	CraftMenuSaveListener( menu, window )

def getPluginsDirectoryPath():
	"Get the plugins directory path."
	return gcodec.getAbsoluteFolderPath( __file__, 'craft_plugins' )

def getPluginFilenames():
	"Get craft plugin fileNames."
	craftSequence = consecution.getReadCraftSequence()
	craftSequence.sort()
	return craftSequence

def getRepositoryConstructor():
	"Get the repository constructor."
	return CraftRepository()

def writeOutput( fileName = '' ):
	"Craft a gcode file.  If no fileName is specified, comment the first gcode file in this folder that is not modified."
	pluginModule = consecution.getLastModule()
	if pluginModule != None:
		pluginModule.writeOutput( fileName )


class CraftRepository:
	"A class to handle the craft preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Crafted', self, '' )
		self.craftLabel = preferences.LabelDisplay().getFromName( 'Open Preferences: ', self )
		importantFilenames = [ 'carve', 'chop', 'feed', 'flow', 'lift', 'raft', 'speed' ]
		preferences.getDisplayToolButtonsRepository( getPluginsDirectoryPath(), importantFilenames, getPluginFilenames(), self )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Craft'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft.html' )

	def execute( self ):
		"Craft button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, [], self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class CraftMenuSaveListener:
	"A class to update a craft menu."
	def __init__( self, menu, window ):
		"Set the menu."
		self.menu = menu
		addToCraftMenu( menu )
		preferences.addElementToListTableIfNotThere( self, window, preferences.globalProfileSaveListenerListTable )

	def save( self ):
		"Profile has been saved and profile menu should be updated."
		preferences.deleteMenuItems( self.menu )
		addToCraftMenu( self.menu )


def main():
	"Display the craft dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
