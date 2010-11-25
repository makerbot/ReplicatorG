"""
This page is in the table of contents.
Polyfile is a script to choose whether the skeinforge toolchain will operate on one file or all the files in a directory.

==Settings==
===Polyfile Choice===
Default is 'Execute File',

====Execute File====
When selected, the toolchain will operate on only the chosen file.

====Execute All Unmodified Files in a Directory'====
When selected, the toolchain will operate on all the unmodifed files in the directory that the chosen file is in.

==Examples==
Examples of using polyfile follow below.


> python polyfile.py
This brings up the polyfile dialog.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import polyfile
>>> polyfile.main()
This brings up the polyfile dialog.


>>> polyfile.isDirectorySetting()
This returns true if 'Execute All Unmodified Files in a Directory' is chosen and returns false if 'Execute File' is chosen.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_application.skeinforge_utilities import skeinforge_polyfile


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getNewRepository():
	"Get the repository constructor."
	return skeinforge_polyfile.PolyfileRepository()


def main():
	"Display the file or directory dialog."
	settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
