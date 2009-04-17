"""
Material is a script to set the material preference for the skeinforge chain.

To change the material preference, in a shell in the material folder type:
> python material.py

An example of using material from the python interpreter follows below.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import material
>>> material.main()
This brings up the material preference dialog.


>>> material.getSelectedMaterial()
ABS

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getSelectedMaterial():
	"Get the selected material."
	materialPreferences = MaterialPreferences()
	preferences.readPreferences( materialPreferences )
	return materialPreferences.materialListbox.value


class MaterialPreferences:
	"A class to handle the material preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.materialList = preferences.ListPreference().getFromValue( 'Material List:', [ 'ABS', 'HDPE', 'PCL', 'PLA' ] )
		self.materialListbox = preferences.ListboxPreference().getFromListPreference( self.materialList, 'Material Selection:', 'ABS' )
		self.addListboxSelection = preferences.AddListboxSelection().getFromListboxPreference( self.materialListbox )
		self.deleteListboxSelection = preferences.DeleteListboxSelection().getFromListboxPreference( self.materialListbox )
		#Create the archive, title of the dialog & preferences fileName.
		self.archive = [ self.materialList, self.materialListbox, self.addListboxSelection, self.deleteListboxSelection ]
		self.executeTitle = None
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.material.html' )


def main( hashtable = None ):
	"Display the material dialog."
	preferences.displayDialog( MaterialPreferences() )

if __name__ == "__main__":
	main()
