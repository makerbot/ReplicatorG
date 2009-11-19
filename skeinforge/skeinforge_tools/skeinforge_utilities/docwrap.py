"""
Docwrap is a script to add spaces to the pydoc files and move them to the documentation folder.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
#import cStringIO
import os


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getWrappedHypertext( fileText ):
	"Get the wrapped pydoc hypertext help."
	helpTextEnd = fileText.find( '</p>' )
	if helpTextEnd < 0:
		print( 'Failed to find the helpTextEnd in getWrappedHypertext in docwrap.' )
	helpTextStart = fileText.find( '<p>' )
	if helpTextStart < 0:
		print( 'Failed to find the helpTextStart in getWrappedHypertext in docwrap.' )
	helpText = fileText[ helpTextStart : helpTextEnd ]
#	print( helpText )
	helpText = helpText.replace( '&nbsp;', ' ' )
#	wrappedText = cStringIO.StringIO()
#	for characterIndex in xrange( len( helpText ) ):
#		character = helpText[ characterIndex ]
#		wrappedText.write( character )
#	print( wrappedText.getvalue() )
	return fileText[ : helpTextStart ] + helpText + fileText[ helpTextEnd : ]

def readWriteDeleteHypertextHelp( documentDirectoryPath, fileName ):
	"Read the pydoc hypertext help documents, write them in the documentation folder then delete the originals."
	print( fileName )
	filePath = os.path.join( documentDirectoryPath, fileName )
	fileText = gcodec.getFileText( fileName )
	fileText = getWrappedHypertext( fileText )
	gcodec.writeFileText( filePath, fileText )
	os.remove( fileName )

def removeFilesInDirectory( directoryPath ):
	"Remove all the files in a directory."
	fileNames = os.listdir( directoryPath )
	for fileName in fileNames:
		filePath = os.path.join( directoryPath, fileName )
		os.remove( filePath )

def writeHypertext():
	"Run pydoc, then read, write and delete each of the files."
	shellCommand = 'pydoc -w ./'
	commandResult = os.system( shellCommand )
	if commandResult != 0:
		print( 'Failed to execute the following command in writeHypertext in docwrap.' )
		print( shellCommand )
	hypertextFiles = gcodec.getFilesWithFileTypeWithoutWords( 'html' )
	if len( hypertextFiles ) <= 0:
		print( 'Failed to find any help files in writeHypertext in docwrap.' )
		return
	documentDirectoryPath = gcodec.getAbsoluteFolderPath( hypertextFiles[ 0 ], 'documentation' )
	removeFilesInDirectory( documentDirectoryPath )
#	for hypertextFile in hypertextFiles[ : 1 ]:
	for hypertextFile in hypertextFiles:
		readWriteDeleteHypertextHelp( documentDirectoryPath, hypertextFile )
	print( '%s files were wrapped.' % len( hypertextFiles ) )


def main():
	"Display the craft dialog."
	writeHypertext()

if __name__ == "__main__":
	main()
