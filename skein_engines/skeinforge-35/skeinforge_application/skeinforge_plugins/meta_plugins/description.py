"""
This page is in the table of contents.
Description is a script to store a description of the profile.

==Settings==
===Description Text===
Default is empty.

The suggested format is a description, followed by a link to a profile post or web page.

==Examples==
Examples of using description follow below.


> python description.py
This brings up the description dialog.

> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import description
>>> description.main()
This brings up the description setting dialog.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import settings


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getNewRepository():
	"Get the repository constructor."
	return DescriptionRepository()


class DescriptionRepository:
	"A class to handle the description settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		settings.addListsToRepository('skeinforge_application.skeinforge_plugins.meta_plugins.description.html', None, self )
		description = 'Write your description of the profile here.\n\nSuggested format is a description, followed by a link to the profile post or web page.'
		self.descriptionText = settings.TextSetting().getFromValue('Description Text:', self, description )


def main():
	"Display the file or directory dialog."
	settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
