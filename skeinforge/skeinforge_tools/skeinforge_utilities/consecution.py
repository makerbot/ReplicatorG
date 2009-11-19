"""
Consecution is a collection of utilities to chain together the craft plugins.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import analyze
import os
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftModule( fileName ):
	"Get craft module."
	craftPluginsDirectoryPath = gcodec.getAbsoluteFolderPath( os.path.dirname( __file__ ), 'craft_plugins' )
	return gcodec.getModuleWithDirectoryPath( craftPluginsDirectoryPath, fileName )

def getChainText( fileName, procedure ):
	"Get a crafted shape file."
	text = gcodec.getFileText( fileName )
	procedures = getProcedures( procedure, text )
	return getChainTextFromProcedures( fileName, procedures, text )

def getChainTextFromProcedures( fileName, procedures, text ):
	"Get a crafted shape file from a list of procedures."
	lastProcedureTime = time.time()
	for procedure in procedures:
		craftModule = getCraftModule( procedure )
		text = craftModule.getCraftedText( fileName, text )
		if gcodec.isProcedureDone( text, procedure ):
			print( '%s procedure took %s seconds.' % ( procedure.capitalize(), int( round( time.time() - lastProcedureTime ) ) ) )
			lastProcedureTime = time.time()
	return text

def getLastModule():
	"Get the last tool."
	craftSequence = getReadCraftSequence()
	if len( craftSequence ) < 1:
		return None
	return getCraftModule( craftSequence[ - 1 ] )

def getProcedures( procedure, text ):
	"Get the procedures up to and including the given procedure."
	craftSequence = getReadCraftSequence()
	sequenceIndexPlusOneFromText = getSequenceIndexPlusOneFromText( text )
	sequenceIndexFromProcedure = getSequenceIndexFromProcedure( procedure )
	return craftSequence[ sequenceIndexPlusOneFromText : sequenceIndexFromProcedure + 1 ]

def getReadCraftSequence():
	"Get profile sequence."
	return preferences.getCraftTypePluginModule().getCraftSequence()

def getSequenceIndexPlusOneFromText( fileText ):
	"Get the profile sequence index of the file plus one.  Return zero if the procedure is not in the file"
	craftSequence = getReadCraftSequence()
	for craftSequenceIndex in xrange( len( craftSequence ) - 1, - 1, - 1 ):
		procedure = craftSequence[ craftSequenceIndex ]
		if gcodec.isProcedureDone( fileText, procedure ):
			return craftSequenceIndex + 1
	return 0

def getSequenceIndexFromProcedure( procedure ):
	"Get the profile sequence index of the procedure.  Return None if the procedure is not in the sequence"
	craftSequence = getReadCraftSequence()
	if procedure not in craftSequence:
		return 0
	return craftSequence.index( procedure )

def writeChainTextWithNounMessage( fileName, procedure ):
	"Get and write a crafted shape file."
	print( '' )
	print( 'The %s tool is parsing the file:' % procedure )
	print( os.path.basename( fileName ) )
	print( '' )
	startTime = time.time()
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_' + procedure + '.gcode'
	craftText = getChainText( fileName, procedure )
	if craftText == '':
		return
	gcodec.writeFileText( suffixFilename, craftText )
	print( '' )
	print( 'The %s tool has created the file:' % procedure )
	print( suffixFilename )
	print( '' )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to craft the file.' )
	analyze.writeOutput( suffixFilename, craftText )
