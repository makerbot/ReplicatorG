"""
Help has buttons and menu items to open help, blog and forum pages in your primary browser.

The Fabmetheus Blog is the skeinforge announcements blog and the place to post questions, bugs and skeinforge requests.

The 'Index of Local Documentation' is a list of the pages in the documentation folder.  The Manual is the skeinforge wiki with pictures and charts.  It is the best and most readable source of skeinforge information and you are welcome to contribute.  The 'Overview of Skeinforge' is also the help page of the skeinforge tool.  It is a general description of skeinforge, has answers to frequently asked questions and has many links to skeinforge, fabrication and python pages.

In the forum section, the 'Bits from Bytes Printing Board' is about printing questions, problems and solutions.  Most of the people on that forum use the rapman, but many of the solutions apply to any reprap.  The 'Bits from Bytes Software Board' is about software, and has some skeinforge threads.  The 'Skeinforge Contributions Thread' is a thread about how to contribute to skeinforge development.  The 'Skeinforge Settings Thread' is a thread for people to post, download and discuss skeinforge settings.

The help menu has an item for each button on the help page.  It also has a link to the tool documentation at the very top.  For example, if you open the help menu from the raft tool, the first item opens the raft page in the documentation folder.  Clicking F1 will also open the raft page.

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


def addToMenu( master, menu, repository, window ):
	"Add a tool plugin menu."
	path = preferences.getPathFromFileNameHelp( repository.fileNameHelp )
	openDocumentationCommand = preferences.HelpPage().getOpenFromDocumentationSubName( repository.fileNameHelp )
	preferences.addAcceleratorCommand( '<F1>', openDocumentationCommand, master, menu, os.path.basename( path ).capitalize() )
	menu.add_separator()
	helpRepository = HelpRepository()
	preferences.addMenuEntitiesToMenu( menu, helpRepository.menuEntities )

def getRepositoryConstructor():
	"Get the repository constructor."
	return HelpRepository()


class HelpRepository:
	"A class to handle the help preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		preferences.LabelDisplay().getFromName( 'Fabmetheus Blog, Announcements & Questions:', self )
		preferences.HelpPage().getFromNameAfterHTTP( 'fabmetheus.blogspot.com/', 'Fabmetheus Blog', self )
		preferences.LabelDisplay().getFromName( 'Index of Local Documentation: ', self )
		preferences.HelpPage().getFromNameSubName( 'Index', self )
		preferences.LabelDisplay().getFromName( 'Manual, Wiki with Pictures & Charts: ', self )
		preferences.HelpPage().getFromNameAfterWWW( 'bitsfrombytes.com/wiki/index.php?title=Skeinforge', 'Manual', self )
		preferences.LabelDisplay().getFromName( 'Overview of Skeinforge: ', self )
		preferences.HelpPage().getFromNameSubName( 'Overview', self, 'skeinforge.html' )
		preferences.LabelSeparator().getFromRepository( self )
		preferences.LabelDisplay().getFromName( 'Forums:', self )
		preferences.LabelDisplay().getFromName( 'Bits from Bytes Printing Board:', self )
		preferences.HelpPage().getFromNameAfterWWW( 'bitsfrombytes.com/fora/user/index.php?board=5.0', 'Bits from Bytes Printing Board', self )
		preferences.LabelDisplay().getFromName( 'Bits from Bytes Software Board:', self )
		preferences.HelpPage().getFromNameAfterWWW( 'bitsfrombytes.com/fora/user/index.php?board=4.0', 'Bits from Bytes Software Board', self )
		preferences.LabelDisplay().getFromName( 'Skeinforge Contributions Thread:', self )
		preferences.HelpPage().getFromNameAfterHTTP( 'dev.forums.reprap.org/read.php?12,27562', 'Skeinforge Contributions Thread', self )
		preferences.LabelDisplay().getFromName( 'Skeinforge Settings Thread:', self )
		preferences.HelpPage().getFromNameAfterHTTP( 'dev.forums.reprap.org/read.php?12,27434', 'Skeinforge Settings Thread', self )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = None
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.help.html' )


def main():
	"Display the help dialog."
	preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
