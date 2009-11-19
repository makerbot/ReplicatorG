"""
Interpret is a collection of utilities to list the import plugins.

An import plugin is a script in the import_plugins folder which has the function getTriangleMesh.

The following examples shows functions of interpret.  The examples are run in a terminal in the folder which contains interpret.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import interpret
>>> interpret.getGNUTranslatorGcodeFileTypeTuples()
[('GTS files', '*.gts'), ('Gcode text files', '*.gcode'), ('STL files', '*.stl'), ('SVG files', '*.svg')]

>>> interpret.getImportPluginFilenames()
['gts', 'stl', 'svg']

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
import os


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getFirstTranslatorFileNameUnmodified( fileName ):
	"Get the first file name from the translators in the import plugins folder, if the file name is not already set."
	if fileName != '':
		return fileName
	unmodified = getGNUTranslatorFilesUnmodified()
	if len( unmodified ) == 0:
		print( "There are no unmodified gcode files in this folder." )
		return ''
	return unmodified[ 0 ]

def getGNUTranslatorGcodeFileTypeTuples():
	"Get the file type tuples from the translators in the import plugins folder plus gcode."
	fileTypeTuples = getTranslatorFileTypeTuples()
	fileTypeTuples.append( ( 'Gcode text files', '*.gcode' ) )
	fileTypeTuples.sort()
	return fileTypeTuples

def getGNUTranslatorFilesUnmodified():
	"Get the file types from the translators in the import plugins folder."
	return gcodec.getFilesWithFileTypesWithoutWords( getImportPluginFilenames() ) + [ gcodec.getUnmodifiedGCodeFiles() ]

def getImportPluginFilenames():
	"Get analyze plugin fileNames."
	return gcodec.getPluginFilenamesFromDirectoryPath( gcodec.getAbsoluteFolderPath( os.path.dirname( __file__ ), 'import_plugins' ) )

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
