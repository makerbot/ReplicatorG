"""
Feed is a script to set the feed rate.

The default 'Activate Feed' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.  The feed script sets the maximum feed rate, operating feed rate & travel feed rate.

The feed rate for the shape will be set to the 'Feed Rate" preference.  The 'Travel Feed Rate' is the feed rate when the cutter is off.  The default is 16 mm / s and it could be set as high as the cutter can be moved, it does not have to be limited by the maximum cutter rate.

The 'Maximum Z Feed Rate' is the maximum feed that the tool head will move in the z direction.  If your firmware limits the z feed rate, you do not need to set this preference.  The default of 8 millimeters per second is the maximum z feed of Nophead's direct drive z stage, the belt driven z stages have a lower maximum feed rate.

The following examples feed the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and feed.py.


> python feed.py
This brings up the feed dialog.


> python feed.py Screw Holder Bottom.stl
The feed tool is parsing the file:
Screw Holder Bottom.stl
..
The feed tool has created the file:
.. Screw Holder Bottom_feed.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import feed
>>> feed.main()
This brings up the feed dialog.


>>> feed.writeOutput()
The feed tool is parsing the file:
Screw Holder Bottom.stl
..
The feed tool has created the file:
.. Screw Holder Bottom_feed.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import analyze
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.meta_plugins import polyfile
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', feedRepository = None ):
	"Feed the file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), feedRepository )

def getCraftedTextFromText( gcodeText, feedRepository = None ):
	"Feed a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'feed' ):
		return gcodeText
	if feedRepository == None:
		feedRepository = preferences.getReadRepository( FeedRepository() )
	if not feedRepository.activateFeed.value:
		return gcodeText
	return FeedSkein().getCraftedGcode( gcodeText, feedRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return FeedRepository()

def writeOutput( fileName = '' ):
	"Feed a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'feed' )


class FeedRepository:
	"A class to handle the feed preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Feed', self, '' )
		self.activateFeed = preferences.BooleanPreference().getFromValue( 'Activate Feed:', self, True )
		self.feedRatePerSecond = preferences.FloatPreference().getFromValue( 'Feed Rate (mm/s):', self, 16.0 )
		self.maximumZFeedRatePerSecond = preferences.FloatPreference().getFromValue( 'Maximum Z Feed Rate (mm/s):', self, 8.0 )
		self.travelFeedRatePerSecond = preferences.FloatPreference().getFromValue( 'Travel Feed Rate (mm/s):', self, 16.0 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Feed'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.feed.html' )

	def execute( self ):
		"Feed button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class FeedSkein:
	"A class to feed a skein of cuttings."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.feedRatePerSecond = 16.0
		self.isExtruderActive = False
		self.lineIndex = 0
		self.lines = None
		self.oldFlowrateString = None
		self.oldLocation = None

	def getCraftedGcode( self, gcodeText, feedRepository ):
		"Parse gcode text and store the feed gcode."
		self.distanceFeedRate.maximumZFeedRatePerSecond = feedRepository.maximumZFeedRatePerSecond.value
		self.feedRepository = feedRepository
		self.feedRatePerSecond = feedRepository.feedRatePerSecond.value
		self.travelFeedRatePerMinute = 60.0 * self.feedRepository.travelFeedRatePerSecond.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getFeededLine( self, splitLine ):
		"Get gcode line with feed rate."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.oldLocation = location
		feedRateMinute = 60.0 * self.feedRatePerSecond
		if not self.isExtruderActive:
			feedRateMinute = self.travelFeedRatePerMinute
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate( feedRateMinute, location.dropAxis( 2 ), location.z )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> feed </procedureDone>)' )
				return
			elif firstWord == '(<perimeterWidth>':
				self.absolutePerimeterWidth = abs( float( splitLine[ 1 ] ) )
				self.distanceFeedRate.addLine( '(<maximumZFeedRatePerSecond> %s </maximumZFeedRatePerSecond>)' % self.distanceFeedRate.maximumZFeedRatePerSecond )
				self.distanceFeedRate.addLine( '(<operatingFeedRatePerSecond> %s </operatingFeedRatePerSecond>)' % self.feedRatePerSecond )
				self.distanceFeedRate.addLine( '(<travelFeedRatePerSecond> %s </travelFeedRatePerSecond>)' % self.feedRepository.travelFeedRatePerSecond.value )
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the feed skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			line = self.getFeededLine( splitLine )
		elif firstWord == 'M101':
			self.isExtruderActive = True
		elif firstWord == 'M103':
			self.isExtruderActive = False
		self.distanceFeedRate.addLine( line )


def main():
	"Display the feed dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
