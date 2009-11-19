"""
Stretch is a script to stretch the threads to partially compensate for filament shrinkage when extruded.

The default 'Activate Stretch' checkbox is off.  When it is on, the functions described below will work, when it is off, the functions will not be called.

The important value for the stretch preferences is "Perimeter Inside Stretch Over Perimeter Width" which is the ratio of the maximum amount the inside perimeter thread will be stretched compared to the perimeter width, the default is 0.32.  The higher the value the more it will stretch the perimeter and the wider holes will be.  If the value is too small, the holes could be drilled out after fabrication, if the value is too high, the holes would be too wide and the part would have to junked.  The 'Perimeter Outside Stretch Over Perimeter Width' is the ratio of the maximum amount the outside perimeter thread will be stretched compared to the perimeter width, in general this value should be around a third of the 'Perimeter Inside Stretch Over Perimeter Width' preference.  The 'Loop Stretch Over Perimeter Width' is the ratio of the maximum amount the loop aka inner shell threads will be stretched compared to the perimeter width, in general this value should be the same as the 'Perimeter Outside Stretch Over Perimeter Width' preference.  The 'Path Stretch Over Perimeter Width' is the ratio of the maximum amount the threads which are not loops, like the infill threads, will be stretched compared to the perimeter width, the default is 0.

All these defaults assume that the thread sequence choice preference in fill is the perimeter being extruded first, then the loops, then the infill.  If the thread sequence choice is different, the optimal thread parameters will also be different.  In general, if the infill is extruded first, the infill would have to be stretched more so that even after the filament shrinkage, it would still be long enough to connect to the loop or perimeter.

In general, stretch will widen holes and push corners out.  The algorithm works by checking at each turning point on the extrusion path what the direction of the thread is at a distance of "Stretch from Distance over Perimeter Width (ratio)" times the perimeter width, on both sides, and moves the thread in the opposite direction.  The magnitude of the stretch increases with the amount that the direction of the two threads is similar and by the Stretch Over Perimeter Width ratio.  The script then also stretches the thread at two locations on the path on close to the turning points.  In practice the filament contraction will be similar but different from the algorithm, so even once the optimal parameters are determined, the stretch script will not be able to eliminate the inaccuracies caused by contraction, but it should reduce them.

The following examples stretch the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and stretch.py.


> python stretch.py
This brings up the stretch dialog.


> python stretch.py Screw Holder Bottom.stl
The stretch tool is parsing the file:
Screw Holder Bottom.stl
..
The stretch tool has created the file:
.. Screw Holder Bottom_stretch.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import stretch
>>> stretch.main()
This brings up the stretch dialog.


>>> stretch.writeOutput()
The stretch tool is parsing the file:
Screw Holder Bottom.stl
..
The stretch tool has created the file:
.. Screw Holder Bottom_stretch.gcode


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
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.meta_plugins import polyfile
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


#maybe speed up feedRate option
def getCraftedText( fileName, text, stretchRepository = None ):
	"Stretch a gcode linear move text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), stretchRepository )

def getCraftedTextFromText( gcodeText, stretchRepository = None ):
	"Stretch a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'stretch' ):
		return gcodeText
	if stretchRepository == None:
		stretchRepository = preferences.getReadRepository( StretchRepository() )
	if not stretchRepository.activateStretch.value:
		return gcodeText
	return StretchSkein().getCraftedGcode( gcodeText, stretchRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return StretchRepository()

def writeOutput( fileName = '' ):
	"Stretch a gcode linear move file.  Chain stretch the gcode if it is not already stretched.  If no fileName is specified, stretch the first unmodified gcode file in this folder."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'stretch' )


class LineIteratorBackward:
	"Backward line iterator class."
	def __init__( self, isLoop, lineIndex, lines ):
		self.firstLineIndex = None
		self.isLoop = isLoop
		self.lineIndex = lineIndex
		self.lines = lines

	def getIndexBeforeNextDeactivate( self ):
		"Get index two lines before the deactivate command."
		for lineIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'M103':
				return lineIndex - 2
		print( 'This should never happen in stretch, no deactivate command was found for this thread.' )
		raise StopIteration, "You've reached the end of the line."

	def getNext( self ):
		"Get next line going backward or raise exception."
		while self.lineIndex > 3:
			if self.lineIndex == self.firstLineIndex:
				raise StopIteration, "You've reached the end of the line."
			if self.firstLineIndex == None:
				self.firstLineIndex = self.lineIndex
			nextLineIndex = self.lineIndex - 1
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'M103':
				if self.isLoop:
					nextLineIndex = self.getIndexBeforeNextDeactivate()
				else:
					raise StopIteration, "You've reached the end of the line."
			if firstWord == 'G1':
				if self.isBeforeExtrusion():
					if self.isLoop:
						nextLineIndex = self.getIndexBeforeNextDeactivate()
					else:
						raise StopIteration, "You've reached the end of the line."
				else:
					self.lineIndex = nextLineIndex
					return line
			self.lineIndex = nextLineIndex
		raise StopIteration, "You've reached the end of the line."

	def isBeforeExtrusion( self ):
		"Determine if index is two or more before activate command."
		linearMoves = 0
		for lineIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'G1':
				linearMoves += 1
			if firstWord == 'M101':
				return linearMoves > 0
			if firstWord == 'M103':
				return False
		print( 'This should never happen in isBeforeExtrusion in stretch, no activate command was found for this thread.' )
		return False


class LineIteratorForward:
	"Forward line iterator class."
	def __init__( self, isLoop, lineIndex, lines ):
		self.firstLineIndex = None
		self.isLoop = isLoop
		self.lineIndex = lineIndex
		self.lines = lines

	def getIndexJustAfterActivate( self ):
		"Get index just after the activate command."
		for lineIndex in xrange( self.lineIndex - 1, 3, - 1 ):
			line = self.lines[ lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'M101':
				return lineIndex + 1
		print( 'This should never happen in stretch, no activate command was found for this thread.' )
		raise StopIteration, "You've reached the end of the line."

	def getNext( self ):
		"Get next line or raise exception."
		while self.lineIndex < len( self.lines ):
			if self.lineIndex == self.firstLineIndex:
				raise StopIteration, "You've reached the end of the line."
			if self.firstLineIndex == None:
				self.firstLineIndex = self.lineIndex
			nextLineIndex = self.lineIndex + 1
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'M103':
				if self.isLoop:
					nextLineIndex = self.getIndexJustAfterActivate()
				else:
					raise StopIteration, "You've reached the end of the line."
			self.lineIndex = nextLineIndex
			if firstWord == 'G1':
				return line
		raise StopIteration, "You've reached the end of the line."


class StretchRepository:
	"A class to handle the stretch preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Stretched', self, '' )
		self.activateStretch = preferences.BooleanPreference().getFromValue( 'Activate Stretch', self, False )
		self.loopStretchOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Loop Stretch Over Perimeter Width (ratio):', self, 0.11 )
		self.pathStretchOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Path Stretch Over Perimeter Width (ratio):', self, 0.0 )
		self.perimeterInsideStretchOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Perimeter Inside Stretch Over Perimeter Width (ratio):', self, 0.32 )
		self.perimeterOutsideStretchOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Perimeter Outside Stretch Over Perimeter Width (ratio):', self, 0.1 )
		self.stretchFromDistanceOverPerimeterWidth = preferences.FloatPreference().getFromValue( 'Stretch From Distance Over Perimeter Width (ratio):', self, 2.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Stretch'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.stretch.html' )

	def execute( self ):
		"Stretch button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class StretchSkein:
	"A class to stretch a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.feedRateMinute = 959.0
		self.isLoop = False
		self.lineIndex = 0
		self.lines = None
		self.oldLocation = None
		self.perimeterWidth = 0.4

	def addAlongWayLine( self, alongWay, location ):
		"Add stretched gcode line, along the way from the location to the old location."
		alongWayLocation = euclidean.getIntermediateLocation( alongWay, location, self.oldLocation )
		alongWayLine = self.getStretchedLineFromIndexLocation( self.lineIndex - 1, self.lineIndex, alongWayLocation )
		self.distanceFeedRate.addLine( alongWayLine )

	def addStretchesBeforePoint( self, location ):
		"Get stretched gcode line."
		distanceToOld = location.distance( self.oldLocation )
		if distanceToOld == 0.0:
			print( 'This should never happen, stretch should never see two identical points in a row.' )
			print( location )
			return
		alongRatio = self.stretchFromDistance / distanceToOld
		if alongRatio > 0.7:
			return
		if alongRatio > 0.33333333333:
			alongRatio = 0.33333333333
		self.addAlongWayLine( 1.0 - alongRatio, location )
		self.addAlongWayLine( alongRatio, location )

	def getIsThreadWiddershins( self ):
		"Determine if the thread is widdershins."
		oldThreadLocation = self.oldLocation
		thread = None
		for lineIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine( oldThreadLocation, splitLine )
				if thread != None:
					thread.append( location.dropAxis( 2 ) )
				oldThreadLocation = location
			if firstWord == 'M101':
				thread = [ oldThreadLocation.dropAxis( 2 ) ]
			if firstWord == 'M103':
				if thread != None:
					return euclidean.isWiddershins( thread )
				else:
					return True

	def getCraftedGcode( self, gcodeText, stretchRepository ):
		"Parse gcode text and store the stretch gcode."
		self.lines = gcodec.getTextLines( gcodeText )
		self.stretchRepository = stretchRepository
		self.parseInitialization()
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			self.parseStretch( line )
		return self.distanceFeedRate.output.getvalue()

	def getRelativeStretch( self, location, lineIndexRange ):
		"Get relative stretch for a location minus a point."
		locationComplex = location.dropAxis( 2 )
		lastLocationComplex = locationComplex
		oldTotalLength = 0.0
		pointComplex = locationComplex
		totalLength = 0.0
		while 1:
			try:
				line = lineIndexRange.getNext()
			except StopIteration:
				locationMinusPoint = locationComplex - pointComplex
				locationMinusPointLength = abs( locationMinusPoint )
				if locationMinusPointLength > 0.0:
					return locationMinusPoint / locationMinusPointLength
				return complex()
			splitLine = line.split()
			firstWord = splitLine[ 0 ]
			pointComplex = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine ).dropAxis( 2 )
			locationMinusPoint = lastLocationComplex - pointComplex
			locationMinusPointLength = abs( locationMinusPoint )
			totalLength += locationMinusPointLength
			if totalLength >= self.stretchFromDistance:
				distanceFromRatio = ( self.stretchFromDistance - oldTotalLength ) / locationMinusPointLength
				totalPoint = distanceFromRatio * pointComplex + ( 1.0 - distanceFromRatio ) * lastLocationComplex
				locationMinusTotalPoint = locationComplex - totalPoint
				return locationMinusTotalPoint / self.stretchFromDistance
			lastLocationComplex = pointComplex
			oldTotalLength = totalLength

	def getStretchedLine( self, splitLine ):
		"Get stretched gcode line."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		if self.oldLocation != None:
			if self.extruderActive and self.threadMaximumAbsoluteStretch > 0.0:
				self.addStretchesBeforePoint( location )
		self.oldLocation = location
		if self.extruderActive and self.threadMaximumAbsoluteStretch > 0.0:
			return self.getStretchedLineFromIndexLocation( self.lineIndex - 1, self.lineIndex + 1, location )
		if self.isJustBeforeExtrusion() and self.threadMaximumAbsoluteStretch > 0.0:
			return self.getStretchedLineFromIndexLocation( self.lineIndex - 1, self.lineIndex + 1, location )
		return self.lines[ self.lineIndex ]

	def getStretchedLineFromIndexLocation( self, indexPreviousStart, indexNextStart, location ):
		"Get stretched gcode line from line index and location."
		nextRange = LineIteratorForward( self.isLoop, indexNextStart, self.lines )
		previousRange = LineIteratorBackward( self.isLoop, indexPreviousStart, self.lines )
		relativeStretch = self.getRelativeStretch( location, nextRange ) + self.getRelativeStretch( location, previousRange )
		relativeStretch *= 0.8
		relativeStretchLength = abs( relativeStretch )
		if relativeStretchLength > 1.0:
			relativeStretch /= relativeStretchLength
		absoluteStretch = relativeStretch * self.threadMaximumAbsoluteStretch
		stretchedPoint = location.dropAxis( 2 ) + absoluteStretch
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( self.feedRateMinute, stretchedPoint, location.z )

	def isJustBeforeExtrusion( self ):
		"Determine if activate command is before linear move command."
		for lineIndex in xrange( self.lineIndex + 1, len( self.lines ) ):
			line = self.lines[ lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'G1' or firstWord == 'M103':
				return False
			if firstWord == 'M101':
				return True
		print( 'This should never happen in isJustBeforeExtrusion in stretch, no activate or deactivate command was found for this thread.' )
		return False

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> stretch </procedureDone>)' )
				return
			elif firstWord == '(<perimeterWidth>':
				perimeterWidth = float( splitLine[ 1 ] )
				self.loopMaximumAbsoluteStretch = self.perimeterWidth * self.stretchRepository.loopStretchOverPerimeterWidth.value
				self.pathAbsoluteStretch = self.perimeterWidth * self.stretchRepository.pathStretchOverPerimeterWidth.value
				self.perimeterInsideAbsoluteStretch = self.perimeterWidth * self.stretchRepository.perimeterInsideStretchOverPerimeterWidth.value
				self.perimeterOutsideAbsoluteStretch = self.perimeterWidth * self.stretchRepository.perimeterOutsideStretchOverPerimeterWidth.value
				self.stretchFromDistance = self.stretchRepository.stretchFromDistanceOverPerimeterWidth.value * perimeterWidth
				self.threadMaximumAbsoluteStretch = self.pathAbsoluteStretch
			self.distanceFeedRate.addLine( line )

	def parseStretch( self, line ):
		"Parse a gcode line and add it to the stretch skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			line = self.getStretchedLine( splitLine )
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
			self.isLoop = False
			self.threadMaximumAbsoluteStretch = self.pathAbsoluteStretch
		elif firstWord == '(<loop>)':
			self.isLoop = True
			self.threadMaximumAbsoluteStretch = self.loopMaximumAbsoluteStretch
		elif firstWord == '(<perimeter>)':
			self.isLoop = True
			self.threadMaximumAbsoluteStretch = self.perimeterInsideAbsoluteStretch
			if self.getIsThreadWiddershins():
				self.threadMaximumAbsoluteStretch = self.perimeterOutsideAbsoluteStretch
		self.distanceFeedRate.addLine( line )


def main():
	"Display the stretch dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
