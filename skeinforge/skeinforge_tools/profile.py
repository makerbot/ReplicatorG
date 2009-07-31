"""
Profile is a script to set the profile preference for the skeinforge chain.

On the profile dialog, clicking the 'Add Profile' button will duplicate the selected profile and give it the name in the input
field.  For example, if extrude_ABS is selected and the name extrude_ABS_black is in the input field, clicking the 'Add
Profile' button will duplicate extrude_ABS and save it as extrude_ABS_black.  The 'Delete Profile' button deletes the
selected profile.

The preference is the selection.  If you hit 'Save Preferences' the selection will be saved, if you hit 'Cancel' the selection
will not be saved.  However; adding and deleting a profile is a permanent action, for example 'Cancel' will not bring back
any deleted profiles.

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

from skeinforge_tools.skeinforge_utilities import preferences


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def main( hashtable = None ):
	"Display the profile dialog."
	preferences.displayDialog( preferences.ProfilePreferences() )

if __name__ == "__main__":
	main()
