"""
Import translator is a script to access and display the import plugins.

Import shows the client which import plugins are in the import_plugins folder.  If "Translate" is clicked, the chosen file will be
translated and saved as a GNU Triangulated Surface format file.

An import plugin is a script in the import_plugins folder which has the function getTriangleMesh.

The following examples import the files Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains
Screw Holder Bottom.stl & import_translator.py.


> python import_translator.py
This brings up the dialog, after clicking 'Translate', the following is printed:
File Screw Holder Bottom.stl is being translated to the GNU Triangulated Surface format.'
The translated file is saved as Screw Holder Bottom.stl
It took 0 seconds to translate the file.


> python import_translator.py Screw Holder Bottom.stl
File Screw Holder Bottom.stl is being translated to the GNU Triangulated Surface format.
The translated file is saved as Screw Holder Bottom.stl
It took 0 seconds to translate the file.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import import
>>> import.writeOutput()
File Screw Holder Bottom.stl is being translated to the GNU Triangulated Surface format.
The translated file is saved as Screw Holder Bottom.stl
It took 0 seconds to translate the file.


>>> import.main()
This brings up the import dialog.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import polyfile
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getGNUTranslatorGcodeFileTypeTuples():
	"Get the file type tuples from the translators in the import plugins folder."
	fileTypeTuples = getGNUTranslatorFileTypeTuples()
	fileTypeTuples.append( ( 'Gcode text files', '*.gcode' ) )
	fileTypeTuples.sort()
	return fileTypeTuples

def getGNUTranslatorFileTypes():
	"Get the file types from the translators in the import plugins folder."
	gnuTranslatorFileTypes = getImportPluginFilenames()
	gnuTranslatorFileTypes.append( 'gts' )
	return gnuTranslatorFileTypes

def getGNUTranslatorFileTypeTuples():
	"Get the file type tuples from the translators in the import plugins folder."
	fileTypeTuples = [ ( 'GNU Triangulated Surface text files', '*.gts' ) ] + getTranslatorFileTypeTuples()
	fileTypeTuples.sort()
	return fileTypeTuples

def getGNUTranslatorFilesUnmodified():
	"Get the file types from the translators in the import plugins folder."
	return gcodec.getFilesWithFileTypesWithoutWords( getGNUTranslatorFileTypes() ) + [ gcodec.getUnmodifiedGCodeFiles() ]

def getImportPluginFilenames():
	"Get analyze plugin filenames."
	return gcodec.getPluginFilenames( 'import_plugins', __file__ )

def getTranslatorFileTypeTuples():
	"Get the file types from the translators in the import plugins folder."
	importPluginFilenames = getImportPluginFilenames()
	fileTypeTuples = []
	for importPluginFilename in importPluginFilenames:
		fileTypeTitle = importPluginFilename.upper() + ' files'
		fileType = ( fileTypeTitle, '*.' + importPluginFilename )
		fileTypeTuples.append( fileType )
	fileTypeTuples.sort()
	return fileTypeTuples

def getTriangleMesh( filename ):
	"Get a triangle mesh for the file using an import plugin."
	importPluginFilenames = getImportPluginFilenames()
	for importPluginFilename in importPluginFilenames:
		fileTypeDot = '.' + importPluginFilename
		if filename[ - len( fileTypeDot ) : ].lower() == fileTypeDot:
			pluginModule = gcodec.getModule( importPluginFilename, 'import_plugins', __file__ )
			if pluginModule != None:
				return pluginModule.getTriangleMesh( filename )
	print( 'Could not find plugin to handle ' + filename )
	return None

def writeOutput( filename = '' ):
	"Translate a file to the GNU Triangulated Surface format.  If no filename is specified, translate the first file for which there is an import plugin."
	if filename == '':
		unmodified = gcodec.getFilesWithFileTypesWithoutWords( getImportPluginFilenames() )
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		filename = unmodified[ 0 ]
	startTime = time.time()
	print( 'File ' + gcodec.getSummarizedFilename( filename ) + ' is being translated to the GNU Triangulated Surface format.' )
	triangleMesh = getTriangleMesh( filename )
	if triangleMesh == None:
		return ''
	gnuTriangulatedSurfaceText = triangleMesh.getGNUTriangulatedSurfaceText()
	suffixFilename = filename[ : filename.rfind( '.' ) ] + '.gts'
	gcodec.writeFileText( suffixFilename, gnuTriangulatedSurfaceText )
	print( 'The translated file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to translate the file.' )


class ImportPreferences:
	"A class to handle the import preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences filename."
		#Set the default preferences.
		self.archive = []
		importPluginFilenames = getImportPluginFilenames()
		self.importLabel = preferences.LabelDisplay().getFromName( 'Import Translators: ' )
		self.archive.append( self.importLabel )
		self.importOperations = []
		self.importPlugins = []
		for importPluginFilename in importPluginFilenames:
			importPlugin = preferences.LabelDisplay().getFromName( importPluginFilename.upper() )
			self.importPlugins.append( importPlugin )
		self.importPlugins.sort( key = preferences.LabelDisplay.getName )
		self.archive += self.importPlugins
		self.filenameInput = preferences.Filename().getFromFilename( getTranslatorFileTypeTuples(), 'Open File to be Imported', '' )
		self.archive.append( self.filenameInput )
		#Create the archive, title of the execute button, title of the dialog & preferences filename.
		self.executeTitle = 'Translate'
		self.filenamePreferences = preferences.getPreferencesFilePath( 'import_translator.csv' )
		self.filenameHelp = 'skeinforge_tools.import_translator.html'
		self.saveTitle = None
		self.title = 'Import Preferences'

	def execute( self ):
		"Import button has been clicked."
		filenames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.filenameInput.value, getImportPluginFilenames(), self.filenameInput.wasCancelled )
		for filename in filenames:
			writeOutput( filename )


def main( hashtable = None ):
	"Display the import dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( ImportPreferences() )

if __name__ == "__main__":
	main()
