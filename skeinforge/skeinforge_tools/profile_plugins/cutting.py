"""
Cutting is a script to set the cutting profile for the skeinforge chain.

On the cutting dialog, clicking the 'Add Profile' button will duplicate the selected profile and give it the name in the input field.  For example, if laser is selected and the name laser_10mm is in the input field, clicking the 'Add Profile' button will duplicate laser and save it as laser_10mm.  The 'Delete Profile' button deletes the selected profile.

The profile selection is the preference.  If you hit 'Save and Close' the selection will be saved, if you hit 'Cancel' the selection will not be saved.  However; adding and deleting a profile is a permanent action, for example 'Cancel' will not bring back any deleted profiles.

To change the cutting profile, in a shell in the profile_plugins folder type:
> python cutting.py

An example of using cutting from the python interpreter follows below.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import cutting
>>> cutting.main()
This brings up the cutting preference dialog.

"""


from __future__ import absolute_import
import __init__
from skeinforge_tools.skeinforge_utilities import preferences
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftSequence():
	"Get the sequence chop,preface,outset,multiply,whittle,lift,flow,feed,wipe,home,lash,fillet,unpause,export."
	return 'chop,preface,outset,multiply,whittle,lift,flow,feed,wipe,home,lash,fillet,unpause,export'.split( ',' )

def getRepositoryConstructor():
	"Get the repository constructor."
	return CuttingRepository()


class CuttingRepository:
	"A class to handle the cutting preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		preferences.setCraftProfileArchive( getCraftSequence(), 'end_mill', self, 'skeinforge_tools.profile_plugins.cutting.html' )


def main():
	"Display the export dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
