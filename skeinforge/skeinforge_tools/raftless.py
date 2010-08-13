"""
This is a back-ported version by Miles Lightwood of TeamTeamUSA of Eberhard Rensch's raftless tool.
It is compatible with the skeinforge version released with ReplicatorG-0017.
It will not work with the latest version of skeinforge. 

Raftless is a script to prepare a gcode file for raftless printing.

The Raftless script has been written by Eberhard Rensch (http://www.pleasantsoftware.com/developer/3d) and is based on the skeinforge tool chain by Enrique Perez (perez_enrique@yahoo.com).

In order to install the Raftless script within the skeinforge tool chain, put raftless.py in the skeinforge_tool/craft_plugins folder. Then edit  skeinforge_tool/profile_plugins/extrusion.py and add the Raftless script to the tool chain sequence by inserting 'raftless' into the tool sequence  in getCraftSequence(). The best place is at the end of the sequence, right before 'export'.

The default 'Activate Raftless' checkbox is off, since the mutual exclusive 'Raft' script is activated by default. In order to use the Raftless script, you want to desactivate the Raft script first. If both scripts, Raft and Raftless, are activated, the Raftless script (which runs after the Raft script) automatically detects the already created raft. In this case, the Raftless script is skipped and a warning message is printed to the console.

The "1st Perimeter Feed Rate over Feed Rate" preference defines the feed rate during the extrusion of the 1st layer's perimeter lines. The preference is a ratio of the normal extrusion feed rate as configured in the 'Speed' script. The default value is .5, which means half the normal feed rate.

The "1st Perimeter Flow Rate over Flow Rate" preference is the ratio of the filament feedrate during the extrusion of the 1st layer's perimeter lines. Since the feed rate is slower than normal, you might want to reduce also the flow rate in this case. This preference is a ratio of the normal flow rate (as configured in the 'Speed' script). The default is 1. (same flow rate as normal).

If "Add Extrusion Intro" is on, an additional straight extrusion line is added to the start of the first perimeter. This line starts at the coordinates 'Extrusion Intro Max X Absolute'/'Extrusion Intro Max Y Absolute'. However, these both values are absolute values. The script automatically negates one or both of these values, according to the location of the first regualar extrusion.

Please note, that Add Extrusion Intro doesn't check for collisions with the perimeter lines. If necessary, you want to change the max X/Y values manually.



The following examples raftless the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and raftless.py.


> python raftless.py
This brings up the raftless dialog.


> python raftless.py Screw Holder Bottom.stl
The raftless tool is parsing the file:
Screw Holder Bottom.stl
..
The raftless tool has created the file:
Screw Holder Bottom_raftless.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import raftless
>>> raftless.main()
This brings up the raftless dialog.


>>> raftless.writeOutput( 'Screw Holder Bottom.stl' )
Screw Holder Bottom.stl
The raftless tool is parsing the file:
Screw Holder Bottom.stl
..
The raftless tool has created the file:
Screw Holder Bottom_raftless.gcode

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools import analyze
from skeinforge_tools import polyfile
from skeinforge_tools import unpause
import cStringIO
import math
import sys
import time


__author__ = "Eberhard Rensch (eberhard@pleasantsoftware.com)"
__date__ = "$Date: 2009/04/12 $"
__license__ = "GPL 3.0"
__repgversion__ = "ReplicatorG-0017"

def getRaftlessChainGcode( fileName, gcodeText, raftlessPreferences = None ):
	"Raftless the file or text. Chain raftless the gcode if it is not already raftless'd."
	gcodeText = gcodec.getGcodeFileText( fileName, gcodeText )
	if not gcodec.isProcedureDone( gcodeText, 'unpause' ):
		gcodeText = unpause.getUnpauseChainGcode( fileName, gcodeText )
	return getRaftlessGcode( gcodeText, raftlessPreferences )

def getRaftlessGcode( gcodeText, raftlessPreferences = None ):
	"Raftless a gcode linear move text."
	if gcodec.isProcedureDone( gcodeText, 'raft' ):
		print( 'The gcode already contains a raft. Skipping raftless tool.' )
		return gcodeText
	if gcodec.isProcedureDone( gcodeText, 'raftless' ):
		return gcodeText
	if raftlessPreferences == None:
		raftlessPreferences = RaftlessPreferences()
		preferences.readPreferences( raftlessPreferences )
	if not raftlessPreferences.activateRaftless.value:
		return gcodeText
	skein = RaftlessSkein()
	skein.parseGcode( gcodeText, raftlessPreferences )
	return skein.output.getvalue()

def writeOutput( fileName = '' ):
	print( "raftless  - writeOutput" )
	"Raftless a gcode linear move file.  Chain raftless the gcode if it is not already raftless'd. If no fileName is specified, raftless the first unmodified gcode file in this folder."
	if fileName == '':
		unmodified = interpret.getGNUTranslatorFilesUnmodified()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	raftlessPreferences = RaftlessPreferences()
	preferences.readPreferences( raftlessPreferences )
	print( )
	startTime = time.time()
	print( "File " + gcodec.getSummarizedFilename( fileName ) + " is being chain raftless'd." )
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_raftless.gcode'
	raftlessGcode = getRaftlessChainGcode( fileName, '', raftlessPreferences )
	if raftlessGcode == '':
		return
	gcodec.writeFileText( suffixFilename, raftlessGcode )
	print( 'The raftless file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	analyze.writeOutput( suffixFilename, raftlessGcode )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to raftless the file.' )
		
class RaftlessPreferences:
	"A class to handle the raftless preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		self.archive =[]
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Raftless', '' )
		self.archive.append( self.fileNameInput )
		self.activateRaftless = preferences.BooleanPreference().getFromValue( 'Activate Raftless', False )
		self.archive.append( self.activateRaftless )
		self.firstPerimeterFeedrateOverFeedrate = preferences.FloatPreference().getFromValue( '1st Perimeter Feed Rate over Feed Rate (ratio):', 0.7 )
		self.archive.append( self.firstPerimeterFeedrateOverFeedrate )
		self.firstPerimeterFlowrateOverFlowrate = preferences.FloatPreference().getFromValue( '1st Perimeter Flow Rate over Flow Rate (ratio):', 1.0 )
		self.archive.append( self.firstPerimeterFlowrateOverFlowrate )
		self.addExtrusionIntro = preferences.BooleanPreference().getFromValue( 'Add Extrusion Intro:', True )
		self.archive.append( self.addExtrusionIntro )
		self.absMaxXIntro = preferences.FloatPreference().getFromValue( 'Extrusion Intro Max X Absolute (mm):', 20.0 )
		self.archive.append( self.absMaxXIntro )
		self.absMaxYIntro = preferences.FloatPreference().getFromValue( 'Extrusion Intro Max Y Absolute (mm):', 20.0 )
		self.archive.append( self.absMaxYIntro )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Raftless'
		self.saveTitle = 'Save Preferences'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.raftless.html' )

	def execute( self ):
		"Raftless button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class RaftlessSkein:
	"A class to raftless a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = DistanceFeedRate()
		self.feedRateMinute = 900.
		self.currentLayer = 0
		self.firstLinearGcodeMovement = None;
		self.firstPerimeterFlowrateString = None
		self.isExtruderActive = False
		self.isSurroundingLoop = False
		self.lineIndex = 0
		self.lines = None
		self.operatingFlowrateString = None
		self.wantsExtrusionIntro = False
		self.output = self.distanceFeedRate.output

	def addExtrusionIntro( self, line ):
		"Adds the additional linear gcode movement for the extrusion intro."
		splitG1Line = self.firstLinearGcodeMovement.split()
		firstMovementLocation = gcodec.getLocationFromSplitLine(None, splitG1Line)
		firstMovementFeedrate = getFeedRateMinute(self.feedRateMinute/self.raftlessPreferences.firstPerimeterFeedrateOverFeedrate.value, splitG1Line)
		introX = abs( self.raftlessPreferences.absMaxXIntro.value )
		introY = abs( self.raftlessPreferences.absMaxYIntro.value )
		xAxisFirst=False
		if abs( firstMovementLocation.x ) < abs( firstMovementLocation.y ):
			xAxisFirst=True	
		if (xAxisFirst and firstMovementLocation.x > 0) or (not xAxisFirst and firstMovementLocation.x < 0):
			introX = -introX;
		if (xAxisFirst and firstMovementLocation.y < 0) or (not xAxisFirst and firstMovementLocation.y > 0):
			introY = -introY;
		introLine = self.deriveIntroLine(self.firstLinearGcodeMovement, splitG1Line, introX, introY, firstMovementFeedrate)
		self.distanceFeedRate.addLine( introLine )
		self.distanceFeedRate.addLine( line )
		if xAxisFirst:
			introLine = self.deriveIntroLine(self.firstLinearGcodeMovement, splitG1Line, firstMovementLocation.x, introY, self.feedRateMinute)
		else:
			introLine = self.deriveIntroLine(self.firstLinearGcodeMovement, splitG1Line, introX, firstMovementLocation.y, self.feedRateMinute)
		self.distanceFeedRate.addLine(introLine)
		introLine = self.getRaftlessSpeededLine(self.firstLinearGcodeMovement, splitG1Line)
		self.distanceFeedRate.addLine(introLine)
		self.wantsExtrusionIntro = False
		
	def deriveIntroLine( self, line, splitG1Line, introX, introY, introFeed ):
		"Creates a new linear gcode movement, derived from self.firstLinearGcodeMovement."
		roundedXString = 'X' + self.distanceFeedRate.getRounded( introX )
		roundedYString = 'Y' + self.distanceFeedRate.getRounded( introY )
		roundedFString = 'F' + self.distanceFeedRate.getRounded( introFeed )
		indexOfX = gcodec.indexOfStartingWithSecond( 'X', splitG1Line )
		introLine = line
		if indexOfX == -1:
			introLine = introLine + ' ' + roundedXString;
		else:
			word = splitG1Line[ indexOfX ]
			introLine = introLine.replace( word, roundedXString )
		indexOfY = gcodec.indexOfStartingWithSecond( 'Y', splitG1Line )
		if indexOfY == -1:
			introLine = introLine + ' ' + roundedYString;
		else:
			word = splitG1Line[ indexOfY ]
			introLine = introLine.replace( word, roundedYString )
		indexOfF = gcodec.indexOfStartingWithSecond( 'F', splitG1Line )
		if indexOfF == -1:
			introLine = introLine + ' ' + roundedFString;
		else:
			word = splitG1Line[ indexOfF ]
			introLine = introLine.replace( word, roundedFString )
		return introLine;	
									  
	def parseGcode( self, gcodeText, raftlessPreferences ):
		"Parse gcode text and store the raftless gcode."
		self.raftlessPreferences = raftlessPreferences
		self.wantsExtrusionIntro = self.raftlessPreferences.addExtrusionIntro.value
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()

	def getRaftlessSpeededLine( self, line, splitLine ):
		"Get gcode line with raftless feed rate."
		roundedFString = 'F' + self.distanceFeedRate.getRounded( self.feedRateMinute )
		indexOfF = gcodec.indexOfStartingWithSecond( 'F', splitLine )
		if indexOfF == - 1:
			return line + ' ' + roundedFString
		word = splitLine[ indexOfF ]
		return line.replace( word, roundedFString )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == 'M108':
				self.setOperatingFlowString( splitLine )
			elif firstWord == '(<operatingFeedRatePerSecond>':
				self.feedRateMinute = 60.0 * float( splitLine[ 1 ] ) * self.raftlessPreferences.firstPerimeterFeedrateOverFeedrate.value
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> raftless </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the raftless skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<layer>':
			self.currentLayer=self.currentLayer+1
		elif firstWord == '(<surroundingLoop>)':
			self.isSurroundingLoop = True
		elif firstWord == '(</surroundingLoop>)':
			self.isSurroundingLoop = False
		elif firstWord == 'M108':
			self.setOperatingFlowString( splitLine )
		elif firstWord == 'G1':
		    if self.wantsExtrusionIntro and self.firstLinearGcodeMovement == None:
		    	 self.firstLinearGcodeMovement = line
		    	 return
		    if self.currentLayer==1 and self.isSurroundingLoop and self.isExtruderActive:
		    	line = self.getRaftlessSpeededLine( line, splitLine )
		elif firstWord == 'M101':
			if self.currentLayer==1 and self.isSurroundingLoop and self.firstPerimeterFlowrateString and self.firstPerimeterFlowrateString != self.operatingFlowrateString:
				self.distanceFeedRate.addLine( 'M108 S' + self.firstPerimeterFlowrateString )
			self.isExtruderActive = True
			if self.wantsExtrusionIntro and self.firstLinearGcodeMovement != None:
				self.addExtrusionIntro(line)
				return
		elif firstWord == 'M103':
			self.distanceFeedRate.addLine( line )
			self.isExtruderActive = False
			self.restorePreviousFlowrateIfNecessary()
			return
		self.distanceFeedRate.addLine( line )
		
	def restorePreviousFlowrateIfNecessary( self ):
		if self.operatingFlowrateString != None and self.firstPerimeterFlowrateString != self.operatingFlowrateString:
			self.distanceFeedRate.addLine( 'M108 S' + self.operatingFlowrateString )
		
	def setOperatingFlowString( self, splitLine ):
		"Set the operating flow string from the split line."
		self.operatingFlowrateString = splitLine[ 1 ][ 1 : ]
		self.firstPerimeterFlowrateString = self.distanceFeedRate.getRounded( float( self.operatingFlowrateString ) * self.raftlessPreferences.firstPerimeterFlowrateOverFlowrate.value )

def main( hashtable = None ):
	"Display the raftless dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( RaftlessPreferences() )

if __name__ == "__main__":
	main()
			
# All of the below copied from skeinforge 2010-02-07 gcodec.py as part of the backport effort
class DistanceFeedRate:
	"A class to limit the z feed rate and round values."
	def __init__( self ):
		self.absoluteDistanceMode = True
		self.decimalPlacesCarried = 3
		self.extrusionDistanceFormat = ''
		self.maximumZDrillFeedRatePerSecond = None
		self.maximumZFeedRatePerSecond = None
		self.maximumZTravelFeedRatePerSecond = None
		self.oldAddedLocation = None
		self.output = cStringIO.StringIO()

	def addGcodeFromFeedRateThreadZ( self, feedRateMinute, thread, z ):
		"Add a thread to the output."
		if len( thread ) > 0:
			self.addGcodeMovementZWithFeedRate( feedRateMinute, thread[ 0 ], z )
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			print( "thread of only one point in addGcodeFromFeedRateThreadZ in gcodec, this should never happen" )
			print( thread )
			return
		self.addLine( "M101" ) # Turn extruder on.
		for point in thread[ 1 : ]:
			self.addGcodeMovementZWithFeedRate( feedRateMinute, point, z )
		self.addLine( "M103" ) # Turn extruder off.

	def addGcodeFromLoop( self, loop, z ):
		"Add the gcode loop."
		euclidean.addSurroundingLoopBeginning( self, loop, z )
		self.addPerimeterBlock( loop, z )
		self.addLine( '(</boundaryPerimeter>)' )
		self.addLine( '(</surroundingLoop>)' )

	def addGcodeFromThreadZ( self, thread, z ):
		"Add a thread to the output."
		if len( thread ) > 0:
			self.addGcodeMovementZ( thread[ 0 ], z )
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			print( "thread of only one point in addGcodeFromThreadZ in gcodec, this should never happen" )
			print( thread )
			return
		self.addLine( "M101" ) # Turn extruder on.
		for point in thread[ 1 : ]:
			self.addGcodeMovementZ( point, z )
		self.addLine( "M103" ) # Turn extruder off.

	def addGcodeMovementZ( self, point, z ):
		"Add a movement to the output."
		self.addLine( self.getLinearGcodeMovement( point, z ) )

	def addGcodeMovementZWithFeedRate( self, feedRateMinute, point, z ):
		"Add a movement to the output."
		self.addLine( self.getLinearGcodeMovementWithFeedRate( feedRateMinute, point, z ) )

	def addLineOld( self, line ):
		"Add a line of text and a newline to the output."
		self.output.write( line + "\n" )

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		if len( line ) <= 0:
			return
		splitLine = getSplitLineBeforeBracketSemicolon( line )
		firstWord = getFirstWord( splitLine )
		if firstWord == 'G90':
			self.absoluteDistanceMode = True
		elif firstWord == 'G91':
			self.absoluteDistanceMode = False
		elif firstWord == 'G1':
			feedRateMinute = getFeedRateMinute( None, splitLine )
			if self.absoluteDistanceMode:
				location = getLocationFromSplitLine( self.oldAddedLocation, splitLine )
				line = self.getLineWithZLimitedFeedRate( feedRateMinute, line, location, splitLine )
				self.oldAddedLocation = location
			else:
				if self.oldAddedLocation == None:
					print( 'Warning: There was no absolute location when the G91 command was parsed, so the absolute location will be set to the origin.' )
					self.oldAddedLocation = Vector3()
				self.oldAddedLocation += getLocationFromSplitLine( None, splitLine )
		elif firstWord == 'G92':
			self.oldAddedLocation = getLocationFromSplitLine( self.oldAddedLocation, splitLine )
		elif firstWord == 'M101':
			self.maximumZFeedRatePerSecond = self.maximumZDrillFeedRatePerSecond
		elif firstWord == 'M103':
			self.maximumZFeedRatePerSecond = self.maximumZTravelFeedRatePerSecond
		self.output.write( line + "\n" )

	def addLines( self, lines ):
		"Add lines of text to the output."
		for line in lines:
			self.addLine( line )

	def addLinesSetAbsoluteDistanceMode( self, lines ):
		"Add lines of text to the output."
		self.addLines( lines )
		self.absoluteDistanceMode = True

	def addPerimeterBlock( self, loop, z ):
		"Add the perimeter gcode block for the loop."
		if len( loop ) < 2:
			return
		if euclidean.isWiddershins( loop ): # Indicate that a perimeter is beginning.
			self.addLine( '(<perimeter> outer )' )
		else:
			self.addLine( '(<perimeter> inner )' )
		self.addGcodeFromThreadZ( loop + [ loop[ 0 ] ], z )
		self.addLine( '(</perimeter>)' ) # Indicate that a perimeter is beginning.

	def addTagBracketedLine( self, tagName, value ):
		"Add a begin tag, balue and end tag."
		self.addLine( self.getTagBracketedLine( tagName, value ) )

	def getBoundaryLine( self, location ):
		"Get boundary gcode line."
		return '(<boundaryPoint> X%s Y%s Z%s </boundaryPoint>)' % ( self.getRounded( location.x ), self.getRounded( location.y ), self.getRounded( location.z ) )

	def getFirstWordMovement( self, firstWord, location ):
		"Get the start of the arc line."
		return '%s X%s Y%s Z%s' % ( firstWord, self.getRounded( location.x ), self.getRounded( location.y ), self.getRounded( location.z ) )

	def getLinearGcodeMovement( self, point, z ):
		"Get a linear gcode movement."
		return "G1 X%s Y%s Z%s" % ( self.getRounded( point.real ), self.getRounded( point.imag ), self.getRounded( z ) )

	def getLinearGcodeMovementWithFeedRate( self, feedRateMinute, point, z ):
		"Get a z limited gcode movement."
		addedLocation = Vector3( point.real, point.imag, z )
		if addedLocation == self.oldAddedLocation:
			return ''
		linearGcodeMovement = self.getLinearGcodeMovement( point, z )
		if feedRateMinute == None:
			return linearGcodeMovement
		return linearGcodeMovement + ' F' + self.getRounded( feedRateMinute )

	def getLinearGcodeMovementWithZLimitedFeedRate( self, feedRateMinute, location ):
		"Get a z limited gcode movement."
		if location == self.oldAddedLocation:
			return ''
		distance = 0.0
		extrusionDistanceString = ''
		if self.oldAddedLocation != None:
			distance = abs( location - self.oldAddedLocation )
		linearGcodeMovement = self.getLinearGcodeMovement( location.dropAxis( 2 ), location.z )
		if feedRateMinute == None:
			return linearGcodeMovement
		if self.oldAddedLocation != None:
			deltaZ = abs( location.z - self.oldAddedLocation.z )
			feedRateMinute = self.getZLimitedFeedRate( deltaZ, distance, feedRateMinute )
		return linearGcodeMovement + ' F' + self.getRounded( feedRateMinute )

	def getLineWithFeedRate( self, feedRateMinute, line, splitLine ):
		"Get the line with a feed rate."
		roundedFeedRateString = 'F' + self.getRounded( feedRateMinute )
		indexOfF = indexOfStartingWithSecond( 'F', splitLine )
		if indexOfF < 0:
			return line + ' ' + roundedFeedRateString
		word = splitLine[ indexOfF ]
		return line.replace( word, roundedFeedRateString )

	def getLineWithX( self, line, splitLine, x ):
		"Get the line with an x."
		roundedXString = 'X' + self.getRounded( x )
		indexOfX = indexOfStartingWithSecond( 'X', splitLine )
		if indexOfX == - 1:
			return line + ' ' + roundedXString
		word = splitLine[ indexOfX ]
		return line.replace( word, roundedXString )

	def getLineWithY( self, line, splitLine, y ):
		"Get the line with a y."
		roundedYString = 'Y' + self.getRounded( y )
		indexOfY = indexOfStartingWithSecond( 'Y', splitLine )
		if indexOfY == - 1:
			return line + ' ' + roundedYString
		word = splitLine[ indexOfY ]
		return line.replace( word, roundedYString )

	def getLineWithZ( self, line, splitLine, z ):
		"Get the line with a z."
		roundedZString = 'Z' + self.getRounded( z )
		indexOfZ = indexOfStartingWithSecond( 'Z', splitLine )
		if indexOfZ == - 1:
			return line + ' ' + roundedZString
		word = splitLine[ indexOfZ ]
		return line.replace( word, roundedZString )

	def getLineWithZLimitedFeedRate( self, feedRateMinute, line, location, splitLine ):
		"Get a replaced limited gcode movement line."
		if location == self.oldAddedLocation:
			return ''
		if feedRateMinute == None:
			return line
		if self.oldAddedLocation != None:
			deltaZ = abs( location.z - self.oldAddedLocation.z )
			distance = abs( location - self.oldAddedLocation )
			feedRateMinute = self.getZLimitedFeedRate( deltaZ, distance, feedRateMinute )
		return self.getLineWithFeedRate( feedRateMinute, line, splitLine )

	def getRounded( self, number ):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToDecimalPlacesString( self.decimalPlacesCarried, number )

	def getTagBracketedLine( self, tagName, value ):
		"Add a begin tag, balue and end tag."
		return '(<%s> %s </%s>)' % ( tagName, value, tagName )

	def getZLimitedFeedRate( self, deltaZ, distance, feedRateMinute ):
		"Get the z limited feed rate."
		if self.maximumZFeedRatePerSecond == None:
			return feedRateMinute
		zFeedRateSecond = feedRateMinute * deltaZ / distance / 60.0
		if zFeedRateSecond > self.maximumZFeedRatePerSecond:
			feedRateMinute *= self.maximumZFeedRatePerSecond / zFeedRateSecond
		return feedRateMinute

	def parseSplitLine( self, firstWord, splitLine ):
		"Parse gcode split line and store the parameters."
		firstWord = getWithoutBracketsEqualTab( firstWord )
		if firstWord == 'decimalPlacesCarried':
			self.decimalPlacesCarried = int( splitLine[ 1 ] )
		elif firstWord == 'maximumZDrillFeedRatePerSecond':
			self.maximumZDrillFeedRatePerSecond = float( splitLine[ 1 ] )
			self.maximumZFeedRatePerSecond = self.maximumZDrillFeedRatePerSecond
		elif firstWord == 'maximumZTravelFeedRatePerSecond':
			self.maximumZTravelFeedRatePerSecond = float( splitLine[ 1 ] )

def getFeedRateMinute( feedRateMinute, splitLine ):
	"Get the feed rate per minute if the split line has a feed rate."
	indexOfF = indexOfStartingWithSecond( "F", splitLine )
	if indexOfF > 0:
		return getDoubleAfterFirstLetter( splitLine[ indexOfF ] )
	return feedRateMinute

def getDoubleAfterFirstLetter( word ):
	"Get the double value of the word after the first letter."
	return float( word[ 1 : ] )
		
def indexOfStartingWithSecond( letter, splitLine ):
	"Get index of the first occurence of the given letter in the split line, starting with the second word.  Return - 1 if letter is not found"
	for wordIndex in xrange( 1, len( splitLine ) ):
		word = splitLine[ wordIndex ]
		firstLetter = word[ 0 ]
		if firstLetter == letter:
			return wordIndex
	return - 1
				
def getFirstWord( splitLine ):
	"Get the first word of a split line."
	if len( splitLine ) > 0:
		return splitLine[ 0 ]
	return ''

def getFirstWordFromLine( line ):
	"Get the first word of a line."
	return getFirstWord( line.split() )
				
def getWithoutBracketsEqualTab( line ):
	"Get a string without the greater than sign, the bracket and less than sign, the equal sign or the tab."
	line = line.replace( '=', ' ' )
	line = line.replace( '(<', '' )
	line = line.replace( '>', '' )
	return line.replace( '\t', '' )

def getSplitLineBeforeBracketSemicolon( line ):
	"Get the split line before a bracket or semicolon."
	bracketSemicolonIndex = min( line.find( ';' ), line.find( '(' ) )
	if bracketSemicolonIndex < 0:
		return line.split()
	return line[ : bracketSemicolonIndex ].split()
			
def getLocationFromSplitLine( oldLocation, splitLine ):
	"Get the location from the split line."
	if oldLocation == None:
		oldLocation = Vector3()
	return Vector3(
		getDoubleFromCharacterSplitLineValue( 'X', splitLine, oldLocation.x ),
		getDoubleFromCharacterSplitLineValue( 'Y', splitLine, oldLocation.y ),
		getDoubleFromCharacterSplitLineValue( 'Z', splitLine, oldLocation.z ) )
			
def getDoubleFromCharacterSplitLine( character, splitLine ):
	"Get the double value of the string after the first occurence of the character in the split line."
	indexOfCharacter = indexOfStartingWithSecond( character, splitLine )
	if indexOfCharacter < 0:
		return None
	floatString = splitLine[ indexOfCharacter ][ 1 : ]
	try:
		return float( floatString )
	except ValueError:
		return None

def getDoubleFromCharacterSplitLineValue( character, splitLine, value ):
	"Get the double value of the string after the first occurence of the character in the split line, if it does not exist return the value."
	splitLineFloat = getDoubleFromCharacterSplitLine( character, splitLine )
	if splitLineFloat == None:
		return value
	return splitLineFloat
