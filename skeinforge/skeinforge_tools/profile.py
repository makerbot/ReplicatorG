"""
Profile is a script to set the craft types preference for the skeinforge chain.

Profile presents the user with a choice of the craft types in the profile_plugins folder.  The chosen craft type is used to determine the craft type profile for the skeinforge chain.  The default craft type is extrusion.

The preference is the selection.  If you hit 'Save and Close' the selection will be saved, if you hit 'Cancel' the selection will not be saved.

To change the profile preference, in a shell in the profile folder type:
> python profile.py

An example of using profile from the python interpreter follows below.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import profile
>>> profile.main()
This brings up the profile preference dialog.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import preferences
import os


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addSubmenus( craftTypeName, menu, pluginFilename, pluginPath, profileRadioVar ):
	"Add a tool plugin menu."
	submenu = preferences.Tkinter.Menu( menu, tearoff = 0 )
	menu.add_cascade( label = pluginFilename.capitalize(), menu = submenu )
	preferences.ToolDialog().addPluginToMenu( submenu, pluginPath )
	submenu.add_separator()
	pluginModule = preferences.getCraftTypePluginModule( pluginFilename )
	profilePluginPreferences = preferences.getReadRepository( pluginModule.getRepositoryConstructor() )
	isSelected = ( craftTypeName == pluginFilename )
	for profileName in profilePluginPreferences.profileList.value:
		value = isSelected and profileName == profilePluginPreferences.profileListbox.value
		preferences.ProfileMenuRadio( pluginFilename, submenu, profileName, profileRadioVar, value )

def addToProfileMenu( menu ):
	"Add a profile menu."
	preferences.ToolDialog().addPluginToMenu( menu, __file__[ : __file__.rfind( '.' ) ] )
	menu.add_separator()
	directoryPath = preferences.getPluginsDirectoryPath()
	pluginFilenames = preferences.getPluginFilenames()
	craftTypeName = preferences.getCraftTypeName()
	profileRadioVar = preferences.Tkinter.StringVar()
	for pluginFilename in pluginFilenames:
		addSubmenus( craftTypeName, menu, pluginFilename, os.path.join( directoryPath, pluginFilename ), profileRadioVar )

def addToMenu( master, menu, repository, window ):
	"Add a tool plugin menu."
	ProfileMenuSaveListener( menu, window )

def getRepositoryConstructor():
	"Get the repository constructor."
	return preferences.ProfileRepository()


class ProfileMenuSaveListener:
	"A class to update a profile menu."
	def __init__( self, menu, window ):
		"Set the menu."
		self.menu = menu
		addToProfileMenu( menu )
		preferences.addElementToListTableIfNotThere( self, window, preferences.globalProfileSaveListenerListTable )

	def save( self ):
		"Profile has been saved and profile menu should be updated."
		preferences.deleteMenuItems( self.menu )
		addToProfileMenu( self.menu )


def main():
	"Display the profile dialog."
	preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
