"""
Tower is a script to extrude a few layers up, then go across to other regions.

The default 'Activate Tower' checkbox is off.  The default is off because tower could result in the extruder collidiing with an already extruded part of the shape and because extruding in one region for more than one layer could result in the shape melting.  When it is on, the functions described below will work, when it is off, the functions will not be called.

This script commands the fabricator to extrude a disconnected region for a few layers, then go to another disconnected region and extrude there.  Its purpose is to reduce the number of stringers between a shape and reduce extruder travel.  The important value for the tower preferences is "Maximum Tower Height (layers)" which is the maximum number of layers that the extruder will extrude in one region before going to another.

Tower works by looking for islands in each layer and if it finds another island in the layer above, it goes to the next layer above instead of going across to other regions on the original layer.  It checks for collision with shapes already extruded within a cone from the nozzle tip.  The "Extruder Possible Collision Cone Angle (degrees)" preference is the angle of that cone.  Realistic values for the cone angle range between zero and ninety.  The higher the angle, the less likely a collision with the rest of the shape is, generally the extruder will stay in the region for only a few layers before a collision is detected with the wide cone.  The default angle is sixty degrees.

The "Tower Start Layer" is the layer which the script starts extruding towers, after the last raft layer which does not have support material.  It is best to not tower at least the first layer because the temperature of the first layer should sometimes be different than that of the other layers.  The default preference is one.

The following examples tower the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and tower.py.


> python tower.py
This brings up the tower dialog.


> python tower.py Screw Holder Bottom.stl
The tower tool is parsing the file:
Screw Holder Bottom.stl
..
The tower tool has created the file:
.. Screw Holder Bottom_tower.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import tower
>>> tower.main()
This brings up the tower dialog.


>>> tower.writeOutput()
The tower tool is parsing the file:
Screw Holder Bottom.stl
..
The tower tool has created the file:
.. Screw Holder Bottom_tower.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.meta_plugins import polyfile
from skeinforge_tools.skeinforge_utilities import consecution
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
import math
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"

def getCraftedText( fileName, text, towerRepository = None ):
	"Tower a gcode linear move file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), towerRepository )

def getCraftedTextFromText( gcodeText, towerRepository = None ):
	"Tower a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'tower' ):
		return gcodeText
	if towerRepository == None:
		towerRepository = preferences.getReadRepository( TowerRepository() )
	if not towerRepository.activateTower.value:
		return gcodeText
	return TowerSkein().getCraftedGcode( gcodeText, towerRepository )

def getRepositoryConstructor():
	"Get the repository constructor."
	return TowerRepository()

def transferFillLoops( fillLoops, surroundingLoop ):
	"Transfer fill loops."
	for innerSurrounding in surroundingLoop.innerSurroundings:
		transferFillLoopsToSurroundingLoops( fillLoops, innerSurrounding.innerSurroundings )
	surroundingLoop.extraLoops = euclidean.getTransferredPaths( fillLoops, surroundingLoop.boundary )

def transferFillLoopsToSurroundingLoops( fillLoops, surroundingLoops ):
	"Transfer fill loops to surrounding loops."
	for surroundingLoop in surroundingLoops:
		transferFillLoops( fillLoops, surroundingLoop )

def writeOutput( fileName = '' ):
	"Tower a gcode linear move file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'tower' )


class ThreadLayer:
	"A layer of loops and paths."
	def __init__( self ):
		"Thread layer constructor."
		self.afterExtrusionLines = []
		self.beforeExtrusionLines = []
		self.boundaries = []
		self.loops = []
		self.paths = []
		self.surroundingLoops = []

	def __repr__( self ):
		"Get the string representation of this thread layer."
		return '%s, %s, %s, %s' % ( self.boundaries, self.loops, self.paths, self.surroundingLoops )


class TowerRepository:
	"A class to handle the tower preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Towered', self, '' )
		self.activateTower = preferences.BooleanPreference().getFromValue( 'Activate Tower', self, False )
		self.extruderPossibleCollisionConeAngle = preferences.FloatPreference().getFromValue( 'Extruder Possible Collision Cone Angle (degrees):', self, 60.0 )
		self.maximumTowerHeight = preferences.IntPreference().getFromValue( 'Maximum Tower Height (layers):', self, 5 )
		self.towerStartLayer = preferences.IntPreference().getFromValue( 'Tower Start Layer (integer):', self, 1 )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Tower'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.tower.html' )

	def execute( self ):
		"Tower button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class TowerSkein:
	"A class to tower a skein of extrusions."
	def __init__( self ):
		self.afterExtrusionLines = []
		self.beforeExtrusionLines = []
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.feedRateMinute = 959.0
		self.feedRateTable = {}
		self.halfLayerThickness = 0.4
		self.islandLayers = []
		self.isLayerStarted = False
		self.isLoop = False
		self.isPerimeter = False
		self.layerIndex = 0
		self.lineIndex = 0
		self.lines = None
		self.oldLayerIndex = None
		self.oldLocation = None
		self.oldOrderedLocation = Vector3()
		self.oldZ = - 999999999.0
		self.perimeterWidth = 0.6
		self.shutdownLineIndex = sys.maxint
		self.surroundingLoop = None
		self.thread = None
		self.threadLayer = None
		self.threadLayers = []
		self.threadSequence = [ 'perimeter', 'loops', 'infill' ]
		self.travelFeedRatePerMinute = None

	def addEntireLayer( self, layerIndex ):
		"Add entire thread layer."
		surroundingLoops = self.islandLayers[ layerIndex ]
		threadLayer = self.threadLayers[ layerIndex ]
		self.distanceFeedRate.addLines( threadLayer.beforeExtrusionLines )
		euclidean.addToThreadsRemoveFromSurroundings( self.oldOrderedLocation, surroundingLoops, self )
		self.distanceFeedRate.addLines( threadLayer.afterExtrusionLines )

	def addGcodeFromThreadZ( self, thread, z ):
		"Add a gcode thread to the output."
		if len( thread ) > 0:
			firstPoint = thread[ 0 ]
			if z + self.halfLayerThickness < self.oldZ:
				highPoint = complex( firstPoint.real, firstPoint.imag )
				if self.oldLocation != None:
					oldLocationComplex = self.oldLocation.dropAxis( 2 )
					complexToPoint = firstPoint - oldLocationComplex
					toPointLength = abs( complexToPoint )
					if toPointLength > 0.0:
						truncatedLength = max( 0.5 * toPointLength, toPointLength - 0.2 * self.perimeterWidth ) # if it is truncated too much it can not be combed properly
						complexToPointTruncated = complexToPoint * truncatedLength / toPointLength
						highPoint = oldLocationComplex + complexToPointTruncated
				self.addGcodeMovementZ( self.travelFeedRatePerMinute, highPoint, z )
			self.addGcodeMovementZ( self.travelFeedRatePerMinute, firstPoint, z )
			self.oldZ = z
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			return
		self.distanceFeedRate.addLine( 'M101' ) # Turn extruder on.
		for point in thread[ 1 : ]:
			self.addGcodeMovementZ( self.feedRateMinute, point, z )
		self.distanceFeedRate.addLine( "M103" ) # Turn extruder off.

	def addGcodeMovementZ( self, feedRateMinute, point, z ):
		"Add a movement to the output."
		pointVector3 = Vector3( point.real, point.imag, z )
		if pointVector3 in self.feedRateTable:
			feedRateMinute = self.feedRateTable[ pointVector3 ]
		self.distanceFeedRate.addGcodeMovementZWithFeedRate( feedRateMinute, point, z )

	def addIfTravel( self, splitLine ):
		"Add travel move around loops if this the extruder is off."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.oldLocation = location

	def addIslandLayer( self, threadLayer ):
		"Add a layer of surrounding islands."
		surroundingLoops = euclidean.getOrderedSurroundingLoops( self.perimeterWidth, threadLayer.surroundingLoops )
		for surroundingLoop in surroundingLoops:
			surroundingLoop.boundingLoop = intercircle.BoundingLoop().getFromLoop( surroundingLoop.boundary )
		euclidean.transferPathsToSurroundingLoops( threadLayer.paths[ : ], surroundingLoops )
		transferFillLoopsToSurroundingLoops( threadLayer.loops[ : ], surroundingLoops )
		self.islandLayers.append( surroundingLoops )

	def addThreadLayerIfNone( self ):
		"Add a thread layer if it is none."
		if self.threadLayer != None:
			return
		self.threadLayer = ThreadLayer()
		self.threadLayers.append( self.threadLayer )
		self.threadLayer.beforeExtrusionLines = self.beforeExtrusionLines
		self.beforeExtrusionLines = []

	def addToExtrusion( self, location ):
		"Add a location to the thread."
		if self.oldLocation == None:
			return
		if self.threadLayer == None:
			return
		if self.surroundingLoop != None:
			if self.isPerimeter:
				if self.surroundingLoop.loop == None:
					self.surroundingLoop.loop = []
				self.surroundingLoop.addToLoop( location )
				return
			elif self.thread == None:
				self.thread = [ self.oldLocation.dropAxis( 2 ) ]
				self.surroundingLoop.perimeterPaths.append( self.thread )
		if self.thread == None:
			self.thread = []
			if self.isLoop: #do not add to loops because a closed loop does not have to restate its beginning
				self.threadLayer.loops.append( self.thread )
			else:
				self.thread.append( self.oldLocation.dropAxis( 2 ) )
				self.threadLayer.paths.append( self.thread )
		self.thread.append( location.dropAxis( 2 ) )

	def addTowers( self ):
		"Add towers."
		bottomLayerIndex = self.getBottomLayerIndex()
		if bottomLayerIndex == None:
			return
		removedIsland = self.getRemovedIslandAddLayerLinesIfDifferent( self.islandLayers[ bottomLayerIndex ], bottomLayerIndex )
		while 1:
			self.climbTower( removedIsland )
			bottomLayerIndex = self.getBottomLayerIndex()
			if bottomLayerIndex == None:
				return
			removedIsland = self.getRemovedIslandAddLayerLinesIfDifferent( self.islandLayers[ bottomLayerIndex ], bottomLayerIndex )

	def climbTower( self, removedIsland ):
		"Climb up the island to any islands directly above."
		outsetDistance = 1.5 * self.perimeterWidth
		for step in xrange( self.towerRepository.maximumTowerHeight.value ):
			aboveIndex = self.oldLayerIndex + 1
			if aboveIndex >= len( self.islandLayers ):
				return
			outsetRemovedLoop = removedIsland.boundingLoop.getOutsetBoundingLoop( outsetDistance )
			islandsWithin = []
			for island in self.islandLayers[ aboveIndex ]:
				if self.isInsideRemovedOutsideCone( island, outsetRemovedLoop, aboveIndex ):
					islandsWithin.append( island )
			if len( islandsWithin ) < 1:
				return
			removedIsland = self.getRemovedIslandAddLayerLinesIfDifferent( islandsWithin, aboveIndex )
			self.islandLayers[ aboveIndex ].remove( removedIsland )

	def getBottomLayerIndex( self ):
		"Get the index of the first island layer which has islands."
		for islandLayerIndex in xrange( len( self.islandLayers ) ):
			if len( self.islandLayers[ islandLayerIndex ] ) > 0:
				return islandLayerIndex
		return None

	def getCraftedGcode( self, gcodeText, towerRepository ):
		"Parse gcode text and store the tower gcode."
		self.lines = gcodec.getTextLines( gcodeText )
		self.towerRepository = towerRepository
		self.parseInitialization()
		self.oldLocation = None
		if gcodec.isThereAFirstWord( '(<operatingLayerEnd>', self.lines, self.lineIndex ):
			self.parseUntilOperatingLayer()
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for threadLayer in self.threadLayers:
			self.addIslandLayer( threadLayer )
		for self.layerIndex in xrange( min( len( self.islandLayers ), towerRepository.towerStartLayer.value ) ):
			self.addEntireLayer( self.layerIndex )
		self.addTowers()
		self.distanceFeedRate.addLines( self.lines[ self.shutdownLineIndex : ] )
		return self.distanceFeedRate.output.getvalue()

	def getRemovedIslandAddLayerLinesIfDifferent( self, islands, layerIndex ):
		"Add gcode lines for the layer if it is different than the old bottom layer index."
		threadLayer = None
		if layerIndex != self.oldLayerIndex:
			self.oldLayerIndex = layerIndex
			threadLayer = self.threadLayers[ layerIndex ]
			self.distanceFeedRate.addLines( threadLayer.beforeExtrusionLines )
		removedIsland = euclidean.getTransferClosestSurroundingLoop( self.oldOrderedLocation, islands, self )
		if threadLayer != None:
			self.distanceFeedRate.addLines( threadLayer.afterExtrusionLines )
		return removedIsland

	def isInsideRemovedOutsideCone( self, island, removedBoundingLoop, untilLayerIndex ):
		"Determine if the island is entirely inside the removed bounding loop and outside the collision cone of the remaining islands."
		if not island.boundingLoop.isEntirelyInsideAnother( removedBoundingLoop ):
			return False
		bottomLayerIndex = self.getBottomLayerIndex()
		coneAngleTangent = math.tan( math.radians( self.towerRepository.extruderPossibleCollisionConeAngle.value ) )
		for layerIndex in xrange( bottomLayerIndex, untilLayerIndex ):
			islands = self.islandLayers[ layerIndex ]
			outsetDistance = self.perimeterWidth * ( untilLayerIndex - layerIndex ) * coneAngleTangent + 0.5 * self.perimeterWidth
			for belowIsland in self.islandLayers[ layerIndex ]:
				outsetIslandLoop = belowIsland.boundingLoop.getOutsetBoundingLoop( outsetDistance )
				if island.boundingLoop.isOverlappingAnother( outsetIslandLoop ):
					return False
		return True

	def linearMove( self, splitLine ):
		"Add a linear move to the loop."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.feedRateMinute = gcodec.getFeedRateMinute( self.feedRateMinute, splitLine )
		self.feedRateTable[ location ] = self.feedRateMinute
		if self.extruderActive:
			self.addToExtrusion( location )
		self.oldLocation = location

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> tower </procedureDone>)' )
				return
			elif firstWord == '(<layerThickness>':
				self.halfLayerThickness = 0.5 * float( splitLine[ 1 ] )
			elif firstWord == '(<threadSequenceString>':
				self.threadSequence = [ splitLine[ 1 ], splitLine[ 2 ], splitLine[ 3 ] ]
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
			elif firstWord == '(<travelFeedRatePerSecond>':
				self.travelFeedRatePerMinute = 60.0 * float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )

	def parseLine( self, lineIndex ):
		"Parse a gcode line."
		line = self.lines[ lineIndex ]
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		self.afterExtrusionLines.append( line )
		if firstWord == 'G1':
			self.addThreadLayerIfNone()
			self.linearMove( splitLine )
		if firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.afterExtrusionLines = []
			self.extruderActive = False
			self.thread = None
			self.isLoop = False
			self.isPerimeter = False
		elif firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine( None, splitLine )
			self.surroundingLoop.addToBoundary( location )
		elif firstWord == '(</extrusion>)':
			self.shutdownLineIndex = lineIndex
		elif firstWord == '(<layer>':
			self.beforeExtrusionLines = [ line ]
			self.threadLayer = None
			self.thread = None
			return
		elif firstWord == '(</layer>)':
			if self.threadLayer != None:
				self.threadLayer.afterExtrusionLines = self.afterExtrusionLines
			self.afterExtrusionLines = []
		elif firstWord == '(<loop>)':
			self.isLoop = True
		elif firstWord == '(</loop>)':
			self.afterExtrusionLines = []
		elif firstWord == '(<perimeter>)':
			self.isPerimeter = True
		elif firstWord == '(</perimeter>)':
			self.afterExtrusionLines = []
		elif firstWord == '(<surroundingLoop>)':
			self.surroundingLoop = euclidean.SurroundingLoop( self.threadSequence )
			self.addThreadLayerIfNone()
			self.threadLayer.surroundingLoops.append( self.surroundingLoop )
			self.threadLayer.boundaries.append( self.surroundingLoop.boundary )
		elif firstWord == '(</surroundingLoop>)':
			self.afterExtrusionLines = []
			self.surroundingLoop = None
		if len( self.beforeExtrusionLines ) > 0:
			self.beforeExtrusionLines.append( line )

	def parseUntilOperatingLayer( self ):
		"Parse gcode until the operating layer if there is one."
		for self.lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.addLine( line )
			if firstWord == '(<operatingLayerEnd>':
				return


def main():
	"Display the tower dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
