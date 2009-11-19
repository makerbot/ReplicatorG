"""
Jitter is a script to jitter the ends of the loops of a gcode file.

The default 'Activate Jitter' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.

Jitter jitters the loop end position to a different place on each layer to prevent the a ridge from forming.  The "Jitter Over Extrusion Width (ratio)" is the amount the loop ends will be jittered over the extrusion width.  A high value means the loops will start all over the place and a low value means loops will start at roughly the same place on each layer.

The following examples jitter the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and jitter.py.


> python jitter.py
This brings up the jitter dialog.


> python jitter.py Screw Holder Bottom.stl
The jitter tool is parsing the file:
Screw Holder Bottom.stl
..
The jitter tool has created the file:
.. Screw Holder Bottom_jitter.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import jitter
>>> jitter.main()
This brings up the jitter dialog.


>>> jitter.writeOutput()
The jitter tool is parsing the file:
Screw Holder Bottom.stl
..
The jitter tool has created the file:
.. Screw Holder Bottom_jitter.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"

def getCraftedText( fileName, text, jitterRepository = None ):
	"Jitter a gcode linear move text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), jitterRepository )

def getCraftedTextFromText( gcodeText, jitterRepository = None ):
	"Jitter a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'jitter' ):
		return gcodeText
	if jitterRepository == None:
		jitterRepository = preferences.getReadRepository( JitterRepository() )
	if not jitterRepository.activateJitter.value:
		return gcodeText
	return JitterSkein().getCraftedGcode( jitterRepository, gcodeText )

def getRepositoryConstructor():
	"Get the repository constructor."
	return JitterRepository()

def isLoopNumberEqual( betweenX, betweenXIndex, loopNumber ):
	"Determine if the loop number is equal."
	if betweenXIndex >= len( betweenX ):
		return False
	return betweenX[ betweenXIndex ].index == loopNumber

def writeOutput( fileName = '' ):
	"Jitter a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'jitter' )


class JitterRepository:
	"A class to handle the jitter preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Jitter', self, '' )
		self.activateJitter = preferences.BooleanPreference().getFromValue( 'Activate Jitter', self, True )
		self.jitterOverExtrusionWidth = preferences.FloatPreference().getFromValue( 'Jitter Over Extrusion Width (ratio):', self, 2.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Jitter'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.jitter.html' )

	def execute( self ):
		"Jitter button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class JitterSkein:
	"A class to jitter a skein of extrusions."
	def __init__( self ):
		self.beforeLoopLocation = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRateMinute = None
		self.isLoopPerimeter = False
		self.layerGolden = 0.0
		self.lineIndex = 0
		self.lines = None
		self.loopPath = None
		self.oldLocation = None

	def addGcodeFromThreadZ( self, thread, z ):
		"Add a gcode thread to the output."
		if len( thread ) > 0:
			self.addGcodeMovementZ( self.travelFeedRatePerMinute, thread[ 0 ], z )
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			return
		self.distanceFeedRate.addLine( 'M101' )
		self.addGcodePathZ( self.feedRateMinute, thread[ 1 : ], z )

	def addGcodeMovementZ( self, feedRateMinute, point, z ):
		"Add a movement to the output."
		if feedRateMinute == None:
			feedRateMinute = self.operatingFeedRatePerMinute
		self.distanceFeedRate.addGcodeMovementZWithFeedRate( feedRateMinute, point, z )

	def addGcodePathZ( self, feedRateMinute, path, z ):
		"Add a gcode path, without modifying the extruder, to the output."
		for point in path:
			self.addGcodeMovementZ( feedRateMinute, point, z )

	def addTailoredLoopPath( self ):
		"Add a clipped and jittered loop path."
		loop = self.loopPath.path[ : - 1 ]
		if self.beforeLoopLocation != None:
			perimeterHalfWidth = 0.5 * self.perimeterWidth
			loop = euclidean.getLoopStartingNearest( perimeterHalfWidth, self.beforeLoopLocation, loop )
		if self.layerJitter != 0.0:
			loop = self.getJitteredLoop( self.layerJitter, loop )
			loop = euclidean.getAwayPoints( loop, 0.2 * self.perimeterWidth )
		self.loopPath.path = loop + [ loop[ 0 ] ]
		self.addGcodeFromThreadZ( self.loopPath.path, self.loopPath.z )
		self.loopPath = None

	def getCraftedGcode( self, jitterRepository, gcodeText ):
		"Parse gcode text and store the jitter gcode."
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization( jitterRepository )
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseAddJitter( line )
		return self.distanceFeedRate.output.getvalue()

	def getJitteredLoop( self, jitterDistance, jitterLoop ):
		"Get a jittered loop path."
		loopLength = euclidean.getPolygonLength( jitterLoop )
		lastLength = 0.0
		pointIndex = 0
		totalLength = 0.0
		jitterPosition = ( jitterDistance + 256.0 * loopLength ) % loopLength
		while totalLength < jitterPosition and pointIndex < len( jitterLoop ):
			firstPoint = jitterLoop[ pointIndex ]
			secondPoint  = jitterLoop[ ( pointIndex + 1 ) % len( jitterLoop ) ]
			pointIndex += 1
			lastLength = totalLength
			totalLength += abs( firstPoint - secondPoint )
		remainingLength = jitterPosition - lastLength
		pointIndex = pointIndex % len( jitterLoop )
		ultimateJitteredPoint = jitterLoop[ pointIndex ]
		penultimateJitteredPointIndex = ( pointIndex + len( jitterLoop ) - 1 ) % len( jitterLoop )
		penultimateJitteredPoint = jitterLoop[ penultimateJitteredPointIndex ]
		segment = ultimateJitteredPoint - penultimateJitteredPoint
		segmentLength = abs( segment )
		originalOffsetLoop = euclidean.getAroundLoop( pointIndex, pointIndex, jitterLoop )
		if segmentLength <= 0.0:
			return originalOffsetLoop
		newUltimatePoint = penultimateJitteredPoint + segment * remainingLength / segmentLength
		return [ newUltimatePoint ] + originalOffsetLoop

	def getLinearMove( self, line, splitLine ):
		"Add to loop path if this is a loop or path."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		if self.isLoopPerimeter:
			if self.isNextExtruderOn():
				self.loopPath = euclidean.PathZ( location.z )
				if self.oldLocation != None:
					self.beforeLoopLocation = self.oldLocation.dropAxis( 2 )
		self.oldLocation = location
		if self.loopPath == None:
			return line
		self.loopPath.path.append( location.dropAxis( 2 ) )
		return ''

	def isNextExtruderOn( self ):
		"Determine if there is an extruder on command before a move command."
		line = self.lines[ self.lineIndex ]
		splitLine = line.split()
		for afterIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ afterIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'G1' or firstWord == 'M103':
				return False
			elif firstWord == 'M101':
				return True
		return False

	def parseAddJitter( self, line ):
		"Parse a gcode line, jitter it and add it to the jitter skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			line = self.getLinearMove( line, splitLine )
		elif firstWord == 'M101':
			if self.loopPath != None:
				return
		elif firstWord == 'M103':
			self.isLoopPerimeter = False
			if self.loopPath != None:
				self.addTailoredLoopPath()
		elif firstWord == '(<layer>':
			self.layerGolden += 0.61803398874989479
			self.layerJitter = self.jitter * ( math.fmod( self.layerGolden, 1.0 ) - 0.5 )
		elif firstWord == '(<loop>)' or firstWord == '(<perimeter>)':
			self.isLoopPerimeter = True
		self.distanceFeedRate.addLine( line )

	def parseInitialization( self, jitterRepository ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> jitter </procedureDone>)' )
				return
			elif firstWord == '(<operatingFeedRatePerSecond>':
				self.operatingFeedRatePerMinute = 60.0 * float( splitLine[ 1 ] )
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
				self.jitter = jitterRepository.jitterOverExtrusionWidth.value * self.perimeterWidth
			elif firstWord == '(<travelFeedRatePerSecond>':
				self.travelFeedRatePerMinute = 60.0 * float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )


def main():
	"Display the jitter dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
