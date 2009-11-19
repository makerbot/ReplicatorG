#! /usr/bin/env python
"""
Fill is a script to fill the perimeters of a gcode file.

Allan Ecker aka The Masked Retriever has written the "Skeinforge Quicktip: Fill" at:
http://blog.thingiverse.com/2009/07/21/mysteries-of-skeinforge-fill/

The diaphragm is a solid group of layers, at regular intervals.  It can be used with a sparse infill to give the object watertight, horizontal compartments and/or a higher shear strength.  The "Diaphragm Period" is the number of layers between diaphrams.  The "Diaphragm Thickness" is the number of layers the diaphram is composed of.  The default diaphragm is zero, because the diaphragm feature is rarely used.

The "Extra Shells on Alternating Solid Layers" preference is the number of extra shells, which are interior perimeter loops, on the alternating solid layers.  The "Extra Shells on Base" preference is the number of extra shells on the bottom, base layer and every even solid layer after that.  Setting this to a different value than the "Extra Shells on Alternating Solid Layers" means the infill pattern will alternate, creating a strong interleaved bond even if the perimeter loop shrinks.  The "Extra Shells on Sparse Layer" preference is the number of extra shells on the sparse layers.  The solid layers are those at the top & bottom, and wherever the object has a plateau or overhang, the sparse layers are the layers in between.  Adding extra shells makes the object stronger & heavier.

The "Infill Pattern" can be set to "Grid Hexagonal", "Grid Rectangular" or "Line".  The grid rectangular option makes a funky octogon square honeycomb like pattern which gives the object extra strength.  However, the  grid pattern means extra turns for the extruder and therefore extra wear & tear, also it takes longer to generate, so the default is line.  The grid has extra diagonal lines, so when choosing the grid option, set the infill solidity to 0.2 or less so that there is not too much plastic and the grid generation time, which increases with the fourth power of solidity, will be reasonable.  The grid hexagonal option makes a hexagonal grid, but because it is made with threads rather than with molding or milling, only a partial hexagon is possible, so the rectangular grid pattern is generally better.  The "Grid Extra Overlap" preference is the amount of extra overlap added when extruding the grid to compensate for the fact that when the first thread going through a grid point is extruded, since there is nothing there yet for it to connect to it will shrink extra.  The "Grid Junction Separation over Octogon Radius At End" preference is the ratio of the amount the grid square is increased in each direction over the extrusion width at the end, the default is zero.  With a value of one or so the grid pattern will have large squares to go with the octogons.  The "Grid Junction Separation over Octogon Radius At Middle" preference is the increase at the middle, the default is zero.  If this value is different than the value at the end, the grid would have an accordion pattern, which would give it a higher shear strength.  The "Grid Junction Separation Band Height" is the height of the bands of the accordion pattern.

The "Infill Begin Rotation" preference is the amount the infill direction of the base and every second layer thereafter is rotated.  The default value of forty five degrees gives a diagonal infill.  The "Infill Odd Layer Extra Rotation" preference is the extra amount the infill direction of the odd layers is rotated compared to the base layer.  With the default value of ninety degrees the odd layer infill will be perpendicular to the base layer.  The "Infill Begin Rotation Repeat" preference is the number of layers that the infill begin rotation will repeat.  With the default of one, the object will have alternating cross hatching.  With a value higher than one, the infill will go in one direction more often, giving the object more strength in one direction and less in the other, this is useful for beams and cantilevers.

The 'Infill Perimeter Overlap' ratio is the amount the infill overlaps the perimeter over the average of the perimeter and infill width.  The higher the value the more the infill will overlap the perimeter, and the thicker join between the infill and the perimeter.  If the value is too high, the join will be so thick that the nozzle will run plow through the join below making a mess, also when it is above 0.7 fill may not be able to create infill correctly, the default is 0.15.  If you want to stretch the infill a lot, set 'Path Stretch over Perimeter Width' in stretch to a high value.

The most important preference in fill is the "Infill Solidity".  A value of one means the infill lines will be right beside each other, resulting in a solid, strong, heavy shape which takes a long time to extrude.  A low value means the infill will be sparse, the interior will be mosty empty space, the object will be weak, light and quick to build.  The default is 0.2.

The "Interior Infill Density over Exterior Density" preference is the ratio of the infill density of the interior over the infill density of the exterior surfaces, the default is 0.9.  The exterior should have a high infill density, so that the surface will be strong and watertight.  With the interior infill density a bit lower than the exterior, the plastic will not fill up higher than the extruder nozzle.  If the interior density is too high that could happen, as Nophead described in the Hydraraptor "Bearing Fruit" post at:
http://hydraraptor.blogspot.com/2008/08/bearing-fruit.html

The "Solid Surface Thickness" preference is the number of solid layers that are at the bottom, top, plateaus and overhang.  With a value of zero, the entire object will be composed of a sparse infill, and water could flow right through it.  With a value of one, water will leak slowly through the surface and with a value of three, the object could be watertight.  The higher the solid surface thickness, the stronger and heavier the object will be.  The default is three.

The 'Thread Sequence Choice' is the sequence in which the threads will be extruded.  There are three kinds of thread, the perimeter threads on the outside of the object, the loop threads aka inner shell threads, and the interior infill threads.  This gives the following six sequence combinations:
'Infill > Loops > Perimeter'
'Infill > Perimeter > Loops'
'Loops > Infill > Perimeter'
'Loops > Perimeter > Infill'
'Perimeter > Infill > Loops'
'Perimeter > Loops > Infill'

The default choice is 'Perimeter > Loops > Infill', which the default stretch parameters are based on.  If you change from the default sequence choice preference of perimeter, then loops, then infill, the optimal stretch thread parameters would also be different.  In general, if the infill is extruded first, the infill would have to be stretched more so that even after the filament shrinkage, it would still be long enough to connect to the loop or perimeter.

The following examples fill the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and fill.py.


> python fill.py
This brings up the fill dialog.


> python fill.py Screw Holder Bottom.stl
The fill tool is parsing the file:
Screw Holder Bottom.stl
..
The fill tool has created the file:
.. Screw Holder Bottom_fill.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import fill
>>> fill.main()
This brings up the fill dialog.


>>> fill.writeOutput()
The fill tool is parsing the file:
Screw Holder Bottom.stl
..
The fill tool has created the file:
.. Screw Holder Bottom_fill.gcode

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
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
__date__ = "$Date: 2008/28/04 $"
__license__ = "GPL 3.0"

# infill width & bridge documentation
# removalWidthOverPerimeterWidth documentation
#
# getPicture button for mouse tools, reset mouse tool if zooming, zoom, start on screen, set scroll bar around canvas on new screen
# addSupportSegmentTable in raft global
#set addedLocation in distanceFeedRate after arc move
# cool feed and flow rate slowdown option _extrusion
#drilling, do not throw out small loops
# documentation
# check analyze plugins documentation
# add self.maximumCool = preferences.FloatPreference().. ( 'Maximum Cool (Celcius):', 5.0 ) documentation
#
#milling
# mouse select
# raft to temperature, raft
# check alterations folder first, if there is something copy it to the home directory, if not check the home directory
# move alterations and profiles to top level
#
#
#
# add spiral _extrusion
# behold index simulation
# cache edges on first carving and slice from array in svg_codec if bridgeThickness ratio is 1.0 _speed
# layer color, for multilayer start _extrusion
# make frame for first level plugins
#email marius about bridge extrusion width http://reprap.org/bin/view/Main/ExtruderImprovementsAndAlternatives
#xml & svg more forgiving, svg make defaults for layerThickness, maxZ, minZ, add layer z to svg_template, make the slider on the template track even when mouse is outside
# maybe make rulers on behold
# maybe make subrulings and ruling colors on skeinview
# splodge
#implement acceleration & collinear removal in viewers _extrusion
#
# winding
# check pixels instead of lines in fill grid _speed
# zoom, mouse buttons
# add hook _extrusion
# add move, select line from 7/8 of end
# thin check when removing intersecting paths in inset
# move skeinforge_utilities to fabmetheus_utilities
# close, save keyboard shortcuts
#
#
#
# maybe raft follow outline _extrusion
#boundaries, center radius z bottom top, circular or rectangular, polygon, put cool minimum radius orbits within boundaries
#laminate tool head
#maybe add layer updates in behold, skeinview and maybe others
#lathe winding, extrusion and cutting; synonym for rotation or turning, loop angle
#add Ddistance option in preface
# maybe make preference backups
# maybe preferences in gcode or saved versions
# maybe behold axis rulings
#
#
#
# pick and place
# single or double layer option?
# handle equation _extrusion
# integral thin width _extrusion
# simulate
#document gear script
#transform
#searchable help
#extrude loops I guess make circles? and/or run along sparse infill
#custom inclined plane, inclined plane from model, screw, fillet travel as well maybe
#maybe much afterwards make congajure multistep view
#maybe bridge supports although staggered spans are probably better
#maybe update carve to add perimeter path intersection information to the large loop also
#maybe stripe although mosaic alone can handle it
#stretch fiber around shape, maybe modify winding for asymmetric shapes
#multiple heads around edge
#maybe add full underscored date name for version
#maybe make buttons on top of behold and skeinview
#maybe add rarely used tool option
#angle shape for overhang extrusions
# maybe double height shells option _extrusion
#maybe m111? countdown
#maybe option of using G0 when extruder is off
#maybe variable flowrate for base: http://dev.forums.reprap.org/read.php?12,27293,27293#msg-27293
#make stl instead of essentially gts the default format
#maybe split preface into preface and extrusion distance
#def getGNUTranslatorGcodeFileTypeTuples() to include .bfb means making different extensions for unfold and rapman, lower priority since now rapman can handle gcodes
#common tool
#first time tool tip
#individual tool tip to place in text
# maybe try to simplify raft layer start
# maybe make temp directory
# maybe display coordinates in skeinview
# maybe carve aoi xml testing and check xml gcode
# maybe cross hatch support polishing???
# fix multiply tower bug if someone gives their preferences
# maybe toggle fan at each layer
# maybe print svg view from current layer or zero layer in single view
# maybe different background color popup button menu for plugins
# maybe check if tower is picking the nearest island
# maybe isometric svg option
# maybe combine skein classes in fillet
# maybe look for behold cancel bug

# concept, three perpendicular slices to get display spheres
# concept, go from vertex to two orthogonal edges, then from edges to each other, if not to a common point, then simplify polygons by removing points which do not change the area much
# concept, in file, store polygon mesh and centers
# concept, display spheres or polygons would have original triangle for work plane
# .. then again no point with slices
# concept, filled slices, about 2 mm thick
# concept, rgb color triangle switch to get inside color, color golden ratio on 5:11 slope with a modulo 3 face
# concept, teslocracy

#Manual
#10,990
#11,1776
#devocracy?
#make one piece electromagnet spool
#stepper rotor with ceramic disk magnet in middle, electromagnet with long thin spool line?
#stepper motor
#make plastic coated thread in vat with pulley
#tensile stuart platform
#kayak
#gear vacuum pump
#gear turbine
#heat engine
#solar power
#sailboat
#yacht
#house
#condo with reflected gardens in between buildings
#medical equipment
#cell counter, etc..
#pipe clamp lathe
# square tube driller & cutter

# archihedron
# look from top of intersection circle plane to look for next, add a node; tree out until all are stepped on then connect, when more than three intersections are close
# when loading a file, we should have a preview of the part and orientation in space
# second (and most important in my opinion) would be the ability to rotate the part on X/Y/Z axis to chose it's orientation
# third, a routine to detect the largest face and orient the part accordingly. Mat http://reprap.kumy.net/

def addAroundGridPoint( arounds, gridPoint, gridPointInsetX, gridPointInsetY, gridPoints, gridSearchRadius, isBothOrNone, isDoubleJunction, isJunctionWide, paths, pixelTable, width ):
	"Add the path around the grid point."
	closestPathIndex = None
	aroundIntersectionPaths = []
	for aroundIndex in xrange( len( arounds ) ):
		loop = arounds[ aroundIndex ]
		for pointIndex in xrange( len( loop ) ):
			pointFirst = loop[ pointIndex ]
			pointSecond = loop[ ( pointIndex + 1 ) % len( loop ) ]
			yIntersection = getYIntersectionIfExists( pointFirst, pointSecond, gridPoint.real )
			addYIntersectionPathToList( aroundIndex, pointIndex, gridPoint.imag, yIntersection, aroundIntersectionPaths )
	if len( aroundIntersectionPaths ) < 2:
		print( 'This should never happen, aroundIntersectionPaths is less than 2 in fill.' )
		print( aroundIntersectionPaths )
		print( gridPoint )
		print( arounds )
		return
	yCloseToCenterArounds = getClosestOppositeIntersectionPaths( aroundIntersectionPaths )
	if len( yCloseToCenterArounds ) < 2:
		print( 'This should never happen, yCloseToCenterArounds is less than 2 in fill.' )
		print( yCloseToCenterArounds )
		print( gridPoint )
		print( arounds )
		return
	segmentFirstY = min( yCloseToCenterArounds[ 0 ].y, yCloseToCenterArounds[ 1 ].y )
	segmentSecondY = max( yCloseToCenterArounds[ 0 ].y, yCloseToCenterArounds[ 1 ].y )
	yIntersectionPaths = []
	for pathIndex in xrange( len( paths ) ):
		path = paths[ pathIndex ]
		for pointIndex in xrange( len( path ) - 1 ):
			pointFirst = path[ pointIndex ]
			pointSecond = path[ pointIndex + 1 ]
			yIntersection = getYIntersectionInsideYSegment( segmentFirstY, segmentSecondY, pointFirst, pointSecond, gridPoint.real )
			addYIntersectionPathToList( pathIndex, pointIndex, gridPoint.imag, yIntersection, yIntersectionPaths )
	if len( yIntersectionPaths ) < 1:
		return
	yCloseToCenterPaths = []
	if isDoubleJunction:
		yCloseToCenterPaths = getClosestOppositeIntersectionPaths( yIntersectionPaths )
	else:
		yIntersectionPaths.sort( compareDistanceFromCenter )#
		yCloseToCenterPaths = [ yIntersectionPaths[ 0 ] ]#
	for yCloseToCenterPath in yCloseToCenterPaths:
		setIsOutside( yCloseToCenterPath, yIntersectionPaths )
	if len( yCloseToCenterPaths ) < 2:
		yCloseToCenterPaths[ 0 ].gridPoint = gridPoint
		insertGridPointPair( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, paths, pixelTable, yCloseToCenterPaths[ 0 ], width )
		return
	plusMinusSign = getPlusMinusSign( yCloseToCenterPaths[ 1 ].y - yCloseToCenterPaths[ 0 ].y )
	yCloseToCenterPaths[ 0 ].gridPoint = complex( gridPoint.real, gridPoint.imag - plusMinusSign * gridPointInsetY )
	yCloseToCenterPaths[ 1 ].gridPoint = complex( gridPoint.real, gridPoint.imag + plusMinusSign * gridPointInsetY )
	yCloseToCenterPaths.sort( comparePointIndexDescending )
	insertGridPointPairs( gridPoint, gridPointInsetX, gridPoints, yCloseToCenterPaths[ 0 ], yCloseToCenterPaths[ 1 ], isBothOrNone, isJunctionWide, paths, pixelTable, width )

def addPath( infillWidth, infillPaths, path, rotationPlaneAngle ):
	"Add simplified path to fill."
	simplifiedPath = euclidean.getSimplifiedPath( path, infillWidth )
	if len( simplifiedPath ) < 2:
		return
	planeRotated = euclidean.getPointsRoundZAxis( rotationPlaneAngle, simplifiedPath )
	infillPaths.append( planeRotated )

def addPointOnPath( path, pixelTable, point, pointIndex, width ):
	"Add a point to a path and the pixel table."
	pointIndexMinusOne = pointIndex - 1
	if pointIndex < len( path ) and pointIndexMinusOne >= 0:
		segmentTable = {}
		begin = path[ pointIndexMinusOne ]
		end = path[ pointIndex ]
		euclidean.addSegmentToPixelTable( begin, end, segmentTable, 0.0, 0.0, width )
		euclidean.removePixelTableFromPixelTable( segmentTable, pixelTable )
	if pointIndexMinusOne >= 0:
		begin = path[ pointIndexMinusOne ]
		euclidean.addSegmentToPixelTable( begin, point, pixelTable, 0.0, 0.0, width )
	if pointIndex < len( path ):
		end = path[ pointIndex ]
		euclidean.addSegmentToPixelTable( point, end, pixelTable, 0.0, 0.0, width )
	path.insert( pointIndex, point )

def addShortenedLineSegment( lineSegment, shortenDistance, shortenedSegments ):
	"Add shortened line segment."
	pointBegin = lineSegment[ 0 ].point
	pointEnd = lineSegment[ 1 ].point
	segment = pointEnd - pointBegin
	segmentLength = abs( segment )
	if segmentLength < 2.1 * shortenDistance:
		return
	segmentShorten = segment * shortenDistance / segmentLength
	lineSegment[ 0 ].point = pointBegin + segmentShorten
	lineSegment[ 1 ].point = pointEnd - segmentShorten
	shortenedSegments.append( lineSegment )

def addSparseEndpoints( doubleExtrusionWidth, endpoints, fillLine, horizontalSegmentLists, infillSolidity, removedEndpoints, solidSurfaceThickness, surroundingXIntersections ):
	"Add sparse endpoints."
	horizontalEndpoints = horizontalSegmentLists[ fillLine ]
	for segment in horizontalEndpoints:
		addSparseEndpointsFromSegment( doubleExtrusionWidth, endpoints, fillLine, horizontalSegmentLists, infillSolidity, removedEndpoints, segment, solidSurfaceThickness, surroundingXIntersections )

def addSparseEndpointsFromSegment( doubleExtrusionWidth, endpoints, fillLine, horizontalSegmentLists, infillSolidity, removedEndpoints, segment, solidSurfaceThickness, surroundingXIntersections ):
	"Add sparse endpoints from a segment."
	endpointFirstPoint = segment[ 0 ].point
	endpointSecondPoint = segment[ 1 ].point
	if fillLine < 1 or fillLine >= len( horizontalSegmentLists ) - 1 or surroundingXIntersections == None:
		endpoints += segment
		return
	if infillSolidity > 0.0:
		if int( round( round( fillLine * infillSolidity ) / infillSolidity ) ) == fillLine:
			endpoints += segment
			return
	if abs( endpointFirstPoint - endpointSecondPoint ) < doubleExtrusionWidth:
		endpoints += segment
		return
	if not isSegmentAround( horizontalSegmentLists[ fillLine - 1 ], segment ):
		endpoints += segment
		return
	if not isSegmentAround( horizontalSegmentLists[ fillLine + 1 ], segment ):
		endpoints += segment
		return
	if solidSurfaceThickness == 0:
		removedEndpoints += segment
		return
	if isSegmentCompletelyInAnIntersection( segment, surroundingXIntersections ):
		removedEndpoints += segment
		return
	endpoints += segment

def addYIntersectionPathToList( pathIndex, pointIndex, y, yIntersection, yIntersectionPaths ):
	"Add the y intersection path to the y intersection paths."
	if yIntersection == None:
		return
	yIntersectionPath = YIntersectionPath( pathIndex, pointIndex, yIntersection )
	yIntersectionPath.yMinusCenter = yIntersection - y
	yIntersectionPaths.append( yIntersectionPath )

def compareDistanceFromCenter( self, other ):
	"Get comparison in order to sort y intersections in ascending order of distance from the center."
	distanceFromCenter = abs( self.yMinusCenter )
	distanceFromCenterOther = abs( other.yMinusCenter )
	if distanceFromCenter > distanceFromCenterOther:
		return 1
	if distanceFromCenter < distanceFromCenterOther:
		return - 1
	return 0

def comparePointIndexDescending( self, other ):
	"Get comparison in order to sort y intersections in descending order of point index."
	if self.pointIndex > other.pointIndex:
		return - 1
	if self.pointIndex < other.pointIndex:
		return 1
	return 0

def createExtraFillLoops( radius, surroundingLoop ):
	"Create extra fill loops."
	for innerSurrounding in surroundingLoop.innerSurroundings:
		createFillForSurroundings( radius, innerSurrounding.innerSurroundings )
	outsides = []
	insides = euclidean.getInsidesAddToOutsides( surroundingLoop.getFillLoops(), outsides )
	allFillLoops = []
	for outside in outsides:
		transferredLoops = euclidean.getTransferredPaths( insides, outside )
		allFillLoops += getExtraFillLoops( transferredLoops, outside, radius )
	surroundingLoop.lastFillLoops = allFillLoops
	surroundingLoop.extraLoops += allFillLoops

def createFillForSurroundings( radius, surroundingLoops ):
	"Create extra fill loops for surrounding loops."
	for surroundingLoop in surroundingLoops:
		createExtraFillLoops( radius, surroundingLoop )

def getAdditionalLength( path, point, pointIndex ):
	"Get the additional length added by inserting a point into a path."
	if pointIndex == 0:
		return abs( point - path[ 0 ] )
	if pointIndex == len( path ):
		return abs( point - path[ - 1 ] )
	return abs( point - path[ pointIndex - 1 ] ) + abs( point - path[ pointIndex ] ) - abs( path[ pointIndex ] - path[ pointIndex - 1 ] )

def getCraftedText( fileName, text = '', fillRepository = None ):
	"Fill the inset file or text."
	return getCraftedTextFromText( gcodec.getTextIfEmpty( fileName, text ), fillRepository )

def getCraftedTextFromText( gcodeText, fillRepository = None ):
	"Fill the inset text.self."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'fill' ):
		return gcodeText
	if fillRepository == None:
		fillRepository = preferences.getReadRepository( FillRepository() )
	if not fillRepository.activateFill.value:
		return gcodeText
	return FillSkein().getCraftedGcode( fillRepository, gcodeText )

def getClosestOppositeIntersectionPaths( yIntersectionPaths ):
	"Get the close to center paths, starting with the first and an additional opposite if it exists."
	yIntersectionPaths.sort( compareDistanceFromCenter )
	beforeFirst = yIntersectionPaths[ 0 ].yMinusCenter < 0.0
	yCloseToCenterPaths = [ yIntersectionPaths[ 0 ] ]
	for yIntersectionPath in yIntersectionPaths[ 1 : ]:
		beforeSecond = yIntersectionPath.yMinusCenter < 0.0
		if beforeFirst != beforeSecond:
			yCloseToCenterPaths.append( yIntersectionPath )
			return yCloseToCenterPaths
	return yCloseToCenterPaths

def getExtraFillLoops( insideLoops, outsideLoop, radius ):
	"Get extra loops between inside and outside loops."
	greaterThanRadius = 1.4 * radius
	muchGreaterThanRadius = 2.5 * radius
	extraFillLoops = []
	pointComplexes = intercircle.getPointsFromLoop( outsideLoop, greaterThanRadius )
	for inside in insideLoops:
		pointComplexes += intercircle.getPointsFromLoop( inside, greaterThanRadius )
	circleNodes = intercircle.getCircleNodesFromPoints( pointComplexes, greaterThanRadius )
	centers = intercircle.getCentersFromCircleNodes( circleNodes )
	otherLoops = insideLoops + [ outsideLoop ]
	for center in centers:
		inset = intercircle.getSimplifiedInsetFromClockwiseLoop( center, radius )
		if intercircle.isLargeSameDirection( inset, center, muchGreaterThanRadius ):
			if isPathAlwaysInsideLoop( outsideLoop, inset ):
				if isPathAlwaysOutsideLoops( insideLoops, inset ):
					if not euclidean.isLoopIntersectingLoops( inset, otherLoops ):
						inset.reverse()
						extraFillLoops.append( inset )
	return extraFillLoops

def getIntersectionOfXIntersectionIndexes( totalSolidSurfaceThickness, xIntersectionIndexList ):
	"Get x intersections from surrounding layers."
	xIntersectionList = []
	solidTable = {}
	solid = False
	xIntersectionIndexList.sort()
	for xIntersectionIndex in xIntersectionIndexList:
		euclidean.toggleHashtable( solidTable, xIntersectionIndex.index, "" )
		oldSolid = solid
		solid = len( solidTable ) >= totalSolidSurfaceThickness
		if oldSolid != solid:
			xIntersectionList.append( xIntersectionIndex.x )
	return xIntersectionList

def getNonIntersectingGridPointLine( gridPointInsetX, isJunctionWide, paths, pixelTable, yIntersectionPath, width ):
	"Get the points around the grid point that is junction wide that do not intersect."
	pointIndexPlusOne = yIntersectionPath.getPointIndexPlusOne()
	path = yIntersectionPath.getPath( paths )
	begin = path[ yIntersectionPath.pointIndex ]
	end = path[ pointIndexPlusOne ]
	plusMinusSign = getPlusMinusSign( end.real - begin.real )
	if isJunctionWide:
		gridPointXFirst = complex( yIntersectionPath.gridPoint.real - plusMinusSign * gridPointInsetX, yIntersectionPath.gridPoint.imag )
		gridPointXSecond = complex( yIntersectionPath.gridPoint.real + plusMinusSign * gridPointInsetX, yIntersectionPath.gridPoint.imag )
		if isAddedPointOnPathFree( path, pixelTable, gridPointXSecond, pointIndexPlusOne, width ):
			if isAddedPointOnPathFree( path, pixelTable, gridPointXFirst, pointIndexPlusOne, width ):
				return [ gridPointXSecond, gridPointXFirst ]
			if isAddedPointOnPathFree( path, pixelTable, yIntersectionPath.gridPoint, pointIndexPlusOne, width ):
				return [ gridPointXSecond, yIntersectionPath.gridPoint ]
			return [ gridPointXSecond ]
	if isAddedPointOnPathFree( path, pixelTable, yIntersectionPath.gridPoint, pointIndexPlusOne, width ):
		return [ yIntersectionPath.gridPoint ]
	return []

def getPlusMinusSign( number ):
	"Get one if the number is zero or positive else negative one."
	if number >= 0.0:
		return 1.0
	return - 1.0

def getRepositoryConstructor():
	"Get the repository constructor."
	return FillRepository()

def getWithLeastLength( path, point ):
	"Insert a point into a path, at the index at which the path would be shortest."
	if len( path ) < 1:
		return 0
	shortestPointIndex = None
	shortestAdditionalLength = 999999999999999999.0
	for pointIndex in xrange( len( path ) + 1 ):
		additionalLength = getAdditionalLength( path, point, pointIndex )
		if additionalLength < shortestAdditionalLength:
			shortestAdditionalLength = additionalLength
			shortestPointIndex = pointIndex
	return shortestPointIndex

def getYIntersection( firstPoint, secondPoint, x ):
	"Get where the line crosses x."
	secondMinusFirst = secondPoint - firstPoint
	xMinusFirst = x - firstPoint.real
	return xMinusFirst / secondMinusFirst.real * secondMinusFirst.imag + firstPoint.imag

def getYIntersectionIfExists( complexFirst, complexSecond, x ):
	"Get the y intersection if it exists."
	isXAboveFirst = x > complexFirst.real
	isXAboveSecond = x > complexSecond.real
	if isXAboveFirst == isXAboveSecond:
		return None
	return getYIntersection( complexFirst, complexSecond, x )

def getYIntersectionInsideYSegment( segmentFirstY, segmentSecondY, complexFirst, complexSecond, x ):
	"Get the y intersection inside the y segment if it does, else none."
	isXAboveFirst = x > complexFirst.real
	isXAboveSecond = x > complexSecond.real
	if isXAboveFirst == isXAboveSecond:
		return None
	yIntersection = getYIntersection( complexFirst, complexSecond, x )
	if yIntersection <= min( segmentFirstY, segmentSecondY ):
		return None
	if yIntersection < max( segmentFirstY, segmentSecondY ):
		return yIntersection
	return None

def insertGridPointPair( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, paths, pixelTable, yIntersectionPath, width ):
	"Insert a pair of points around the grid point is is junction wide, otherwise inset one point."
	linePath = getNonIntersectingGridPointLine( gridPointInsetX, isJunctionWide, paths, pixelTable, yIntersectionPath, width )
	insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, linePath, paths, pixelTable, yIntersectionPath, width )

def insertGridPointPairs( gridPoint, gridPointInsetX, gridPoints, intersectionPathFirst, intersectionPathSecond, isBothOrNone, isJunctionWide, paths, pixelTable, width ):
	"Insert a pair of points around a pair of grid points."
	gridPointLineFirst = getNonIntersectingGridPointLine( gridPointInsetX, isJunctionWide, paths, pixelTable, intersectionPathFirst, width )
	if len( gridPointLineFirst ) < 1:
		if isBothOrNone:
			return
		intersectionPathSecond.gridPoint = gridPoint
		insertGridPointPair( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, paths, pixelTable, intersectionPathSecond, width )
		return
	gridPointLineSecond = getNonIntersectingGridPointLine( gridPointInsetX, isJunctionWide, paths, pixelTable, intersectionPathSecond, width )
	if len( gridPointLineSecond ) > 0:
		insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, gridPointLineFirst, paths, pixelTable, intersectionPathFirst, width )
		insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, gridPointLineSecond, paths, pixelTable, intersectionPathSecond, width )
		return
	if isBothOrNone:
		return
	originalGridPointFirst = intersectionPathFirst.gridPoint
	intersectionPathFirst.gridPoint = gridPoint
	gridPointLineFirstCenter = getNonIntersectingGridPointLine( gridPointInsetX, isJunctionWide, paths, pixelTable, intersectionPathFirst, width )
	if len( gridPointLineFirstCenter ) > 0:
		insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, gridPointLineFirstCenter, paths, pixelTable, intersectionPathFirst, width )
		return
	intersectionPathFirst.gridPoint = originalGridPointFirst
	insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, gridPointLineFirst, paths, pixelTable, intersectionPathFirst, width )

def insertGridPointPairWithLinePath( gridPoint, gridPointInsetX, gridPoints, isJunctionWide, linePath, paths, pixelTable, yIntersectionPath, width ):
	"Insert a pair of points around the grid point is is junction wide, otherwise inset one point."
	if len( linePath ) < 1:
		return
	if gridPoint in gridPoints:
		gridPoints.remove( gridPoint )
	intersectionBeginPoint = None
	moreThanInset = 2.1 * gridPointInsetX
	path = yIntersectionPath.getPath( paths )
	begin = path[ yIntersectionPath.pointIndex ]
	end = path[ yIntersectionPath.getPointIndexPlusOne() ]
	if yIntersectionPath.isOutside:
		distanceX = end.real - begin.real
		if abs( distanceX ) > 2.1 * moreThanInset:
			intersectionBeginXDistance = yIntersectionPath.gridPoint.real - begin.real
			endIntersectionXDistance = end.real - yIntersectionPath.gridPoint.real
			intersectionPoint = begin * endIntersectionXDistance / distanceX + end * intersectionBeginXDistance / distanceX
			distanceYAbsoluteInset = max( abs( yIntersectionPath.gridPoint.imag - intersectionPoint.imag ), moreThanInset )
			intersectionEndSegment = end - intersectionPoint
			intersectionEndSegmentLength = abs( intersectionEndSegment )
			if intersectionEndSegmentLength > 1.1 * distanceYAbsoluteInset:
				intersectionEndPoint = intersectionPoint + intersectionEndSegment * distanceYAbsoluteInset / intersectionEndSegmentLength
				path.insert( yIntersectionPath.getPointIndexPlusOne(), intersectionEndPoint )
			intersectionBeginSegment = begin - intersectionPoint
			intersectionBeginSegmentLength = abs( intersectionBeginSegment )
			if intersectionBeginSegmentLength > 1.1 * distanceYAbsoluteInset:
				intersectionBeginPoint = intersectionPoint + intersectionBeginSegment * distanceYAbsoluteInset / intersectionBeginSegmentLength
	for point in linePath:
		addPointOnPath( path, pixelTable, point, yIntersectionPath.getPointIndexPlusOne(), width )
	if intersectionBeginPoint != None:
		addPointOnPath( path, pixelTable, intersectionBeginPoint, yIntersectionPath.getPointIndexPlusOne(), width )

def isAddedPointOnPathFree( path, pixelTable, point, pointIndex, width ):
	"Determine if the point added to a path is intersecting the pixel table."
	if pointIndex > 0 and pointIndex < len( path ):
		if isSharpCorner( ( path[ pointIndex - 1 ] ), point, ( path[ pointIndex ] ) ):
			return False
	pointIndexMinusOne = pointIndex - 1
	if pointIndexMinusOne >= 0:
		maskTable = {}
		begin = path[ pointIndexMinusOne ]
		if pointIndex < len( path ):
			end = path[ pointIndex ]
			euclidean.addSegmentToPixelTable( begin, end, maskTable, 0.0, 0.0, width )
		segmentTable = {}
		euclidean.addSegmentToPixelTable( point, begin, segmentTable, 0.0, 3.0, width )
		if euclidean.isPixelTableIntersecting( pixelTable, segmentTable, maskTable ):
			return False
	if pointIndex < len( path ):
		maskTable = {}
		begin = path[ pointIndex ]
		if pointIndexMinusOne >= 0:
			end = path[ pointIndexMinusOne ]
			euclidean.addSegmentToPixelTable( begin, end, maskTable, 0.0, 0.0, width )
		segmentTable = {}
		euclidean.addSegmentToPixelTable( point, begin, segmentTable, 0.0, 3.0, width )
		if euclidean.isPixelTableIntersecting( pixelTable, segmentTable, maskTable ):
			return False
	return True

def isIntersectingLoopsPaths( loops, paths, pointBegin, pointEnd ):
	"Determine if the segment between the first and second point is intersecting the loop list."
	normalizedSegment = pointEnd.dropAxis( 2 ) - pointBegin.dropAxis( 2 )
	normalizedSegmentLength = abs( normalizedSegment )
	if normalizedSegmentLength == 0.0:
		return False
	normalizedSegment /= normalizedSegmentLength
	segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
	pointBeginRotated = euclidean.getRoundZAxisByPlaneAngle( segmentYMirror, pointBegin )
	pointEndRotated = euclidean.getRoundZAxisByPlaneAngle( segmentYMirror, pointEnd )
	if euclidean.isLoopListIntersectingInsideXSegment( loops, pointBeginRotated.real, pointEndRotated.real, segmentYMirror, pointBeginRotated.imag ):
		return True
	return euclidean.isXSegmentIntersectingPaths( paths, pointBeginRotated.real, pointEndRotated.real, segmentYMirror, pointBeginRotated.imag )

def isPathAlwaysInsideLoop( loop, path ):
	"Determine if all points of a path are inside another loop."
	for point in path:
		if euclidean.getNumberOfIntersectionsToLeft( loop, point ) % 2 == 0:
			return False
	return True

def isPathAlwaysOutsideLoops( loops, path ):
	"Determine if all points in a path are outside another loop in a list."
	for loop in loops:
		for point in path:
			if euclidean.getNumberOfIntersectionsToLeft( loop, point ) % 2 == 1:
				return False
	return True

def isPerimeterPathInSurroundLoops( surroundingLoops ):
	"Determine if there is a perimeter path in the surrounding loops."
	for surroundingLoop in surroundingLoops:
		if len( surroundingLoop.perimeterPaths ) > 0:
			return True
	return False

def isPointAddedAroundClosest( aroundPixelTable, layerExtrusionWidth, paths, removedEndpointPoint, width ):
	"Add the closest removed endpoint to the path, with minimal twisting."
	closestDistanceSquared = 999999999999999999.0
	closestPathIndex = None
	for pathIndex in xrange( len( paths ) ):
		path = paths[ pathIndex ]
		for pointIndex in xrange( len( path ) ):
			point = path[ pointIndex ]
			distanceSquared = abs( point - removedEndpointPoint )
			if distanceSquared < closestDistanceSquared:
				closestDistanceSquared = distanceSquared
				closestPathIndex = pathIndex
	if closestPathIndex == None:
		return
	if closestDistanceSquared < 0.8 * layerExtrusionWidth * layerExtrusionWidth:
		return
	closestPath = paths[ closestPathIndex ]
	closestPointIndex = getWithLeastLength( closestPath, removedEndpointPoint )
	if isAddedPointOnPathFree( closestPath, aroundPixelTable, removedEndpointPoint, closestPointIndex, width ):
		addPointOnPath( closestPath, aroundPixelTable, removedEndpointPoint, closestPointIndex, width )
		return True
	return isSidePointAdded( aroundPixelTable, closestPath, closestPointIndex, layerExtrusionWidth, removedEndpointPoint, width )

def isSegmentAround( aroundSegments, segment ):
	"Determine if there is another segment around."
	for aroundSegment in aroundSegments:
		endpoint = aroundSegment[ 0 ]
		if isSegmentInX( segment, endpoint.point.real, endpoint.otherEndpoint.point.real ):
			return True
	return False

def isSegmentCompletelyInAnIntersection( segment, xIntersections ):
	"Add sparse endpoints from a segment."
	for xIntersectionIndex in xrange( 0, len( xIntersections ), 2 ):
		surroundingXFirst = xIntersections[ xIntersectionIndex ]
		surroundingXSecond = xIntersections[ xIntersectionIndex + 1 ]
		if euclidean.isSegmentCompletelyInX( segment, surroundingXFirst, surroundingXSecond ):
			return True
	return False

def isSegmentInX( segment, xFirst, xSecond ):
	"Determine if the segment overlaps within x."
	segmentFirstX = segment[ 0 ].point.real
	segmentSecondX = segment[ 1 ].point.real
	if min( segmentFirstX, segmentSecondX ) > max( xFirst, xSecond ):
		return False
	return max( segmentFirstX, segmentSecondX ) > min( xFirst, xSecond )

def isSharpCorner( beginComplex, centerComplex, endComplex ):
	"Determine if the three complex points form a sharp corner."
	centerBeginComplex = beginComplex - centerComplex
	centerEndComplex = endComplex - centerComplex
	centerBeginLength = abs( centerBeginComplex )
	centerEndLength = abs( centerEndComplex )
	if centerBeginLength <= 0.0 or centerEndLength <= 0.0:
		return False
	centerBeginComplex /= centerBeginLength
	centerEndComplex /= centerEndLength
	return euclidean.getDotProduct( centerBeginComplex, centerEndComplex ) > 0.9

def isSidePointAdded( aroundPixelTable, closestPath, closestPointIndex, layerExtrusionWidth, removedEndpointPoint, width ):
	"Add side point along with the closest removed endpoint to the path, with minimal twisting."
	if closestPointIndex <= 0 or closestPointIndex >= len( closestPath ):
		return False
	pointBegin = closestPath[ closestPointIndex - 1 ]
	pointEnd = closestPath[ closestPointIndex ]
	removedEndpointPoint = removedEndpointPoint
	closest = pointBegin
	farthest = pointEnd
	removedMinusClosest = removedEndpointPoint - pointBegin
	removedMinusClosestLength = abs( removedMinusClosest )
	if removedMinusClosestLength <= 0.0:
		return False
	removedMinusOther = removedEndpointPoint - pointEnd
	removedMinusOtherLength = abs( removedMinusOther )
	if removedMinusOtherLength <= 0.0:
		return False
	insertPointAfter = None
	insertPointBefore = None
	if removedMinusOtherLength < removedMinusClosestLength:
		closest = pointEnd
		farthest = pointBegin
		removedMinusClosest = removedMinusOther
		removedMinusClosestLength = removedMinusOtherLength
		insertPointBefore = removedEndpointPoint
	else:
		insertPointAfter = removedEndpointPoint
	removedMinusClosestNormalized = removedMinusClosest / removedMinusClosestLength
	perpendicular = removedMinusClosestNormalized * complex( 0.0, layerExtrusionWidth )
	sidePoint = removedEndpointPoint + perpendicular
	#extra check in case the line to the side point somehow slips by the line to the perpendicular
	sidePointOther = removedEndpointPoint - perpendicular
	if abs( sidePoint -  farthest ) > abs( sidePointOther -  farthest ):
		perpendicular = - perpendicular
		sidePoint = sidePointOther
	maskTable = {}
	closestSegmentTable = {}
	toPerpendicularTable = {}
	euclidean.addSegmentToPixelTable( pointBegin, pointEnd, maskTable, 1.0, 1.0, width )
	euclidean.addSegmentToPixelTable( closest, removedEndpointPoint, closestSegmentTable, 0.0, 0.0, width )
	euclidean.addSegmentToPixelTable( sidePoint, farthest, toPerpendicularTable, 0.0, 3.0, width )
	if euclidean.isPixelTableIntersecting( aroundPixelTable, toPerpendicularTable, maskTable ) or euclidean.isPixelTableIntersecting( closestSegmentTable, toPerpendicularTable, maskTable ):
		sidePoint = removedEndpointPoint - perpendicular
		toPerpendicularTable = {}
		euclidean.addSegmentToPixelTable( sidePoint, farthest, toPerpendicularTable, 0.0, 3.0, width )
		if euclidean.isPixelTableIntersecting( aroundPixelTable, toPerpendicularTable, maskTable ) or euclidean.isPixelTableIntersecting( closestSegmentTable, toPerpendicularTable, maskTable ):
			return False
	if insertPointBefore != None:
		addPointOnPath( closestPath, aroundPixelTable, insertPointBefore, closestPointIndex, width )
	addPointOnPath( closestPath, aroundPixelTable, sidePoint, closestPointIndex, width )
	if insertPointAfter != None:
		addPointOnPath( closestPath, aroundPixelTable, insertPointAfter, closestPointIndex, width )
	return True

def removeEndpoints( aroundPixelTable, layerExtrusionWidth, paths, removedEndpoints, aroundWidth ):
	"Remove endpoints which are added to the path."
	for removedEndpointIndex in xrange( len( removedEndpoints ) - 1, - 1, - 1 ):
		removedEndpoint = removedEndpoints[ removedEndpointIndex ]
		removedEndpointPoint = removedEndpoint.point
		if isPointAddedAroundClosest( aroundPixelTable, layerExtrusionWidth, paths, removedEndpointPoint, aroundWidth ):
			removedEndpoints.remove( removedEndpoint )

def setIsOutside( yCloseToCenterPath, yIntersectionPaths ):
	"Determine if the yCloseToCenterPath is outside."
	beforeClose = yCloseToCenterPath.yMinusCenter < 0.0
	for yIntersectionPath in yIntersectionPaths:
		if yIntersectionPath != yCloseToCenterPath:
			beforePath = yIntersectionPath.yMinusCenter < 0.0
			if beforeClose == beforePath:
				yCloseToCenterPath.isOutside = False
				return
	yCloseToCenterPath.isOutside = True

def writeOutput( fileName = '' ):
	"Fill an inset gcode file."
	fileName = interpret.getFirstTranslatorFileNameUnmodified( fileName )
	if fileName != '':
		consecution.writeChainTextWithNounMessage( fileName, 'fill' )


class FillRepository:
	"A class to handle the fill preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Filled', self, '' )
		self.activateFill = preferences.BooleanPreference().getFromValue( 'Activate Fill:', self, True )
		self.diaphragmPeriod = preferences.IntPreference().getFromValue( 'Diaphragm Period (layers):', self, 999999 )
		self.diaphragmThickness = preferences.IntPreference().getFromValue( 'Diaphragm Thickness (layers):', self, 0 )
		self.extraShellsAlternatingSolidLayer = preferences.IntPreference().getFromValue( 'Extra Shells on Alternating Solid Layer (layers):', self, 1 )
		self.extraShellsBase = preferences.IntPreference().getFromValue( 'Extra Shells on Base (layers):', self, 0 )
		self.extraShellsSparseLayer = preferences.IntPreference().getFromValue( 'Extra Shells on Sparse Layer (layers):', self, 1 )
		self.gridExtraOverlap = preferences.FloatPreference().getFromValue( 'Grid Extra Overlap (ratio):', self, 0.1 )
		self.gridJunctionSeparationBandHeight = preferences.IntPreference().getFromValue( 'Grid Junction Separation Band Height (layers):', self, 10 )
		self.gridJunctionSeparationOverOctogonRadiusAtEnd = preferences.FloatPreference().getFromValue( 'Grid Junction Separation over Octogon Radius At End (ratio):', self, 0.0 )
		self.gridJunctionSeparationOverOctogonRadiusAtMiddle = preferences.FloatPreference().getFromValue( 'Grid Junction Separation over Octogon Radius At Middle (ratio):', self, 0.0 )
		self.infillBeginRotation = preferences.FloatPreference().getFromValue( 'Infill Begin Rotation (degrees):', self, 45.0 )
		self.infillBeginRotationRepeat = preferences.IntPreference().getFromValue( 'Infill Begin Rotation Repeat (layers):', self, 1 )
		self.infillInteriorDensityOverExteriorDensity = preferences.FloatPreference().getFromValue( 'Infill Interior Density over Exterior Density (ratio):', self, 0.9 )
		self.infillOddLayerExtraRotation = preferences.FloatPreference().getFromValue( 'Infill Odd Layer Extra Rotation (degrees):', self, 90.0 )
		self.infillPatternLabel = preferences.LabelDisplay().getFromName( 'Infill Pattern:', self )
		infillPatternRadio = []
		self.infillPatternGridHexagonal = preferences.Radio().getFromRadio( 'Grid Hexagonal', infillPatternRadio, self, False )
		self.infillPatternGridRectangular = preferences.Radio().getFromRadio( 'Grid Rectangular', infillPatternRadio, self, False )
		self.infillPatternLine = preferences.Radio().getFromRadio( 'Line', infillPatternRadio, self, True )
		self.infillPerimeterOverlap = preferences.FloatPreference().getFromValue( 'Infill Perimeter Overlap (ratio):', self, 0.15 )
		self.infillSolidity = preferences.FloatPreference().getFromValue( 'Infill Solidity (ratio):', self, 0.2 )
		self.infillWidthOverThickness = preferences.FloatPreference().getFromValue( 'Infill Width over Thickness (ratio):', self, 1.5 )
		self.solidSurfaceThickness = preferences.IntPreference().getFromValue( 'Solid Surface Thickness (layers):', self, 3 )
		self.threadSequenceChoice = preferences.MenuButtonDisplay().getFromName( 'Thread Sequence Choice:', self )
		self.threadSequenceInfillLoops = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Infill > Loops > Perimeter', self, False )
		self.threadSequenceInfillPerimeter = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Infill > Perimeter > Loops', self, False )
		self.threadSequenceLoopsInfill = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Loops > Infill > Perimeter', self, False )
		self.threadSequenceLoopsPerimeter = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Loops > Perimeter > Infill', self, False )
		self.threadSequencePerimeterInfill = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Perimeter > Infill > Loops', self, False )
		self.threadSequencePerimeterLoops = preferences.MenuRadio().getFromMenuButtonDisplay( self.threadSequenceChoice, 'Perimeter > Loops > Infill', self, True )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Fill'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.craft_plugins.fill.html' )

	def execute( self ):
		"Fill button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class FillSkein:
	"A class to fill a skein of extrusions."
	def __init__( self ):
		self.bridgeWidthMultiplier = 1.0
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extruderActive = False
		self.fillInset = 0.18
		self.isPerimeter = False
		self.lastExtraShells = - 1
		self.lineIndex = 0
		self.oldLocation = None
		self.oldOrderedLocation = Vector3()
		self.rotatedLayer = None
		self.rotatedLayers = []
		self.shutdownLineIndex = sys.maxint
		self.surroundingLoop = None
		self.thread = None

	def addFill( self, layerIndex ):
		"Add fill to the carve layer."
		alreadyFilledArounds = []
		arounds = []
		halfWidth = 0.5 * self.perimeterWidth
		self.layerExtrusionWidth = self.infillWidth
		layerFillInset = self.fillInset
		rotatedLayer = self.rotatedLayers[ layerIndex ]
		self.distanceFeedRate.addLine( '(<layer> %s )' % rotatedLayer.z ) # Indicate that a new layer is starting.
#		print( 'layer index: %s  z: %s' % ( layerIndex, rotatedLayer.z ) )
		if rotatedLayer.rotation != None:
			halfWidth *= self.bridgeWidthMultiplier
			self.layerExtrusionWidth = self.infillWidth * self.bridgeWidthMultiplier
			layerFillInset = self.fillInset * self.bridgeWidthMultiplier
			self.distanceFeedRate.addLine( '(<bridgeRotation> %s )' % rotatedLayer.rotation ) # Indicate that this is a bridge layer.
		gridPointInsetX = 0.5 * layerFillInset
		doubleExtrusionWidth = 2.0 * self.layerExtrusionWidth
		endpoints = []
		infillPaths = []
		aroundInset = 0.4 * self.layerExtrusionWidth
		aroundWidth = 0.3 * aroundInset
		layerInfillSolidity = self.infillSolidity
		self.isDoubleJunction = True
		self.isJunctionWide = True
		layerRotationAroundZAngle = self.getLayerRoundZ( layerIndex )
		if self.fillRepository.infillPatternGridHexagonal.value:
			if abs( euclidean.getDotProduct( layerRotationAroundZAngle, euclidean.getPolar( self.infillBeginRotation, 1.0 ) ) ) < math.sqrt( 0.5 ):
				layerInfillSolidity *= 0.5
				self.isDoubleJunction = False
			else:
				self.isJunctionWide = False
		reverseZRotationAngle = complex( layerRotationAroundZAngle.real, - layerRotationAroundZAngle.imag )
		rotatedExtruderLoops = []
		stretch = 0.5 * self.layerExtrusionWidth
		surroundingCarves = []
		layerRemainder = layerIndex % int( round( self.fillRepository.diaphragmPeriod.value ) )
		if layerRemainder >= int( round( self.fillRepository.diaphragmThickness.value ) ):
			for surroundingIndex in xrange( 1, self.solidSurfaceThickness + 1 ):
				self.addRotatedCarve( layerIndex - surroundingIndex, reverseZRotationAngle, surroundingCarves )
				self.addRotatedCarve( layerIndex + surroundingIndex, reverseZRotationAngle, surroundingCarves )
		extraShells = self.fillRepository.extraShellsSparseLayer.value
		if len( surroundingCarves ) < self.doubleSolidSurfaceThickness:
			extraShells = self.fillRepository.extraShellsAlternatingSolidLayer.value
			if self.lastExtraShells != self.fillRepository.extraShellsBase.value:
				extraShells = self.fillRepository.extraShellsBase.value
			self.lastExtraShells = extraShells
		else:
			self.lastExtraShells = - 1
		surroundingLoops = euclidean.getOrderedSurroundingLoops( self.layerExtrusionWidth, rotatedLayer.surroundingLoops )
		if isPerimeterPathInSurroundLoops( surroundingLoops ):
			extraShells = 0
		for extraShellIndex in xrange( extraShells ):
			radius = self.layerExtrusionWidth
			if extraShellIndex == 0:
				radius += halfWidth
			createFillForSurroundings( radius, surroundingLoops )
		fillLoops = euclidean.getFillOfSurroundings( surroundingLoops )
		aroundPixelTable = {}
		if extraShells == 0:
			layerFillInset += halfWidth
		slightlyGreaterThanFill = 1.01 * layerFillInset
		muchGreaterThanLayerFillInset = 2.5 * layerFillInset
		for loop in fillLoops:
			alreadyFilledLoop = []
			alreadyFilledArounds.append( alreadyFilledLoop )
			planeRotatedPerimeter = euclidean.getPointsRoundZAxis( reverseZRotationAngle, loop )
			rotatedExtruderLoops.append( planeRotatedPerimeter )
			centers = intercircle.getCentersFromLoop( planeRotatedPerimeter, slightlyGreaterThanFill )
			euclidean.addLoopToPixelTable( planeRotatedPerimeter, aroundPixelTable, aroundWidth )
			for center in centers:
				alreadyFilledInset = intercircle.getSimplifiedInsetFromClockwiseLoop( center, layerFillInset )
				if euclidean.getMaximumSpan( alreadyFilledInset ) > muchGreaterThanLayerFillInset:
					alreadyFilledLoop.append( alreadyFilledInset )
					around = intercircle.getSimplifiedInsetFromClockwiseLoop( center, aroundInset )
					if euclidean.isPathInsideLoop( planeRotatedPerimeter, around ) == euclidean.isWiddershins( planeRotatedPerimeter ):
						arounds.append( around )
		if len( arounds ) < 1:
			self.addThreadsBridgeLayer( rotatedLayer, surroundingLoops )
			return
		back = euclidean.getBackOfLoops( arounds )
		front = euclidean.getFrontOfLoops( arounds )
		area = self.getCarveArea( layerIndex )
		if area > 0.0:
			areaChange = 1.0
			for surroundingIndex in xrange( 1, self.solidSurfaceThickness + 1 ):
				areaChange = min( areaChange, self.getAreaChange( area, layerIndex - surroundingIndex ) )
				areaChange = min( areaChange, self.getAreaChange( area, layerIndex + surroundingIndex ) )
			if areaChange > 0.5 or self.solidSurfaceThickness == 0:
				if self.fillRepository.infillInteriorDensityOverExteriorDensity.value <= 0.0:
					self.addThreadsBridgeLayer( rotatedLayer, surroundingLoops )
				self.layerExtrusionWidth /= self.fillRepository.infillInteriorDensityOverExteriorDensity.value
		front = math.ceil( front / self.layerExtrusionWidth ) * self.layerExtrusionWidth
		fillWidth = back - front
		numberOfLines = int( math.ceil( fillWidth / self.layerExtrusionWidth ) )
		self.frontOverWidth = 0.0
		self.horizontalSegmentLists = euclidean.getHorizontalSegmentListsFromLoopLists( alreadyFilledArounds, front, numberOfLines, rotatedExtruderLoops, self.layerExtrusionWidth )
		self.surroundingXIntersectionLists = []
		self.yList = []
		removedEndpoints = []
		if len( surroundingCarves ) >= self.doubleSolidSurfaceThickness:
			xIntersectionIndexLists = []
			self.frontOverWidth = euclidean.getFrontOverWidthAddXListYList( front, surroundingCarves, numberOfLines, xIntersectionIndexLists, self.layerExtrusionWidth, self.yList )
			for fillLine in xrange( len( self.horizontalSegmentLists ) ):
				xIntersectionIndexList = xIntersectionIndexLists[ fillLine ]
				surroundingXIntersections = getIntersectionOfXIntersectionIndexes( self.doubleSolidSurfaceThickness, xIntersectionIndexList )
				self.surroundingXIntersectionLists.append( surroundingXIntersections )
				addSparseEndpoints( doubleExtrusionWidth, endpoints, fillLine, self.horizontalSegmentLists, layerInfillSolidity, removedEndpoints, self.solidSurfaceThickness, surroundingXIntersections )
		else:
			for fillLine in xrange( len( self.horizontalSegmentLists ) ):
				addSparseEndpoints( doubleExtrusionWidth, endpoints, fillLine, self.horizontalSegmentLists, layerInfillSolidity, removedEndpoints, self.solidSurfaceThickness, None )
		if len( endpoints ) < 1:
			self.addThreadsBridgeLayer( rotatedLayer, surroundingLoops )
			return
		paths = euclidean.getPathsFromEndpoints( endpoints, self.layerExtrusionWidth, aroundPixelTable, aroundWidth )
		if self.isGridToBeExtruded():
			self.addGrid( arounds, fillLoops, gridPointInsetX, layerIndex, paths, aroundPixelTable, aroundWidth, reverseZRotationAngle, surroundingCarves )
		oldRemovedEndpointLength = len( removedEndpoints ) + 1
		while oldRemovedEndpointLength - len( removedEndpoints ) > 0:
			oldRemovedEndpointLength = len( removedEndpoints )
			removeEndpoints( aroundPixelTable, self.layerExtrusionWidth, paths, removedEndpoints, aroundWidth )
		for path in paths:
			addPath( self.layerExtrusionWidth, infillPaths, path, layerRotationAroundZAngle )
		euclidean.transferPathsToSurroundingLoops( infillPaths, surroundingLoops )
		self.addThreadsBridgeLayer( rotatedLayer, surroundingLoops )

	def addGcodeFromThreadZ( self, thread, z ):
		"Add a gcode thread to the output."
		self.distanceFeedRate.addGcodeFromThreadZ( thread, z )

	def addGrid( self, arounds, fillLoops, gridPointInsetX, layerIndex, paths, pixelTable, width, reverseZRotationAngle, surroundingCarves ):
		"Add the grid to the infill layer."
		if len( surroundingCarves ) < self.doubleSolidSurfaceThickness:
			return
		gridPoints = self.getGridPoints( fillLoops, reverseZRotationAngle )
		gridPointInsetY = gridPointInsetX * ( 1.0 - self.fillRepository.gridExtraOverlap.value )
		if self.fillRepository.infillPatternGridRectangular.value:
			gridBandHeight = self.fillRepository.gridJunctionSeparationBandHeight.value
			gridLayerRemainder = ( layerIndex - self.solidSurfaceThickness ) % gridBandHeight
			halfBandHeight = 0.5 * float( gridBandHeight )
			halfBandHeightFloor = math.floor( halfBandHeight )
			fromMiddle = math.floor( abs( gridLayerRemainder - halfBandHeight ) )
			fromEnd = halfBandHeightFloor - fromMiddle
			gridJunctionSeparation = self.gridJunctionSeparationAtEnd * fromMiddle + self.gridJunctionSeparationAtMiddle * fromEnd
			gridJunctionSeparation /= halfBandHeightFloor
			gridPointInsetX += gridJunctionSeparation
			gridPointInsetY += gridJunctionSeparation
		oldGridPointLength = len( gridPoints ) + 1
		while oldGridPointLength - len( gridPoints ) > 0:
			oldGridPointLength = len( gridPoints )
			self.addRemainingGridPoints( arounds, gridPointInsetX, gridPointInsetY, gridPoints, True, paths, pixelTable, width )
		oldGridPointLength = len( gridPoints ) + 1
		while oldGridPointLength - len( gridPoints ) > 0:
			oldGridPointLength = len( gridPoints )
			self.addRemainingGridPoints( arounds, gridPointInsetX, gridPointInsetY, gridPoints, False, paths, pixelTable, width )

	def addGridLinePoints( self, begin, end, gridPoints, gridRotationAngle, offset, y ):
		"Add the segments of one line of a grid to the infill."
		if self.gridRadius == 0.0:
			return
		gridWidth = self.gridWidthMultiplier * self.gridRadius
		gridXStep = int( math.floor( ( begin ) / gridWidth ) ) - 3
		gridXOffset = offset + gridWidth * float( gridXStep )
		while gridXOffset < begin:
			gridXStep = self.getNextGripXStep( gridXStep )
			gridXOffset = offset + gridWidth * float( gridXStep )
		while gridXOffset < end:
			gridPointComplex = complex( gridXOffset, y ) * gridRotationAngle
			if self.isPointInsideLineSegments( gridPointComplex ):
				gridPoints.append( gridPointComplex )
			gridXStep = self.getNextGripXStep( gridXStep )
			gridXOffset = offset + gridWidth * float( gridXStep )

	def addRemainingGridPoints( self, arounds, gridPointInsetX, gridPointInsetY, gridPoints, isBothOrNone, paths, pixelTable, width ):
		"Add the remaining grid points to the grid point list."
		for gridPointIndex in xrange( len( gridPoints ) - 1, - 1, - 1 ):
			gridPoint = gridPoints[ gridPointIndex ]
			addAroundGridPoint( arounds, gridPoint, gridPointInsetX, gridPointInsetY, gridPoints, self.gridRadius, isBothOrNone, self.isDoubleJunction, self.isJunctionWide, paths, pixelTable, width )

	def addRotatedCarve( self, layerIndex, reverseZRotationAngle, surroundingCarves ):
		"Add a rotated carve to the surrounding carves."
		if layerIndex < 0 or layerIndex >= len( self.rotatedLayers ):
			return
		surroundingLoops = self.rotatedLayers[ layerIndex ].surroundingLoops
		rotatedCarve = []
		for surroundingLoop in surroundingLoops:
			planeRotatedLoop = euclidean.getPointsRoundZAxis( reverseZRotationAngle, surroundingLoop.boundary )
			rotatedCarve.append( planeRotatedLoop )
		surroundingCarves.append( rotatedCarve )

	def addThreadsBridgeLayer( self, rotatedLayer, surroundingLoops ):
		"Add the threads, add the bridge end & the layer end tag."
		euclidean.addToThreadsRemoveFromSurroundings( self.oldOrderedLocation, surroundingLoops, self )
		if rotatedLayer.rotation != None:
			self.distanceFeedRate.addLine( '(</bridgeRotation>)' ) # Indicate that this is a bridge layer.
		self.distanceFeedRate.addLine( '(</layer>)' )

	def addToThread( self, location ):
		"Add a location to thread."
		if self.oldLocation == None:
			return
		if self.isPerimeter:
			self.surroundingLoop.addToLoop( location )
			return
		elif self.thread == None:
			self.thread = [ self.oldLocation.dropAxis( 2 ) ]
			self.surroundingLoop.perimeterPaths.append( self.thread )
		self.thread.append( location.dropAxis( 2 ) )

	def getAreaChange( self, area, layerIndex ):
		"Get the difference between the area of the carve at the layer index and the given area."
		layerArea = self.getCarveArea( layerIndex )
		return min( area, layerArea ) / max( area, layerArea )

	def getCraftedGcode( self, fillRepository, gcodeText ):
		"Parse gcode text and store the bevel gcode."
		self.fillRepository = fillRepository
		self.lines = gcodec.getTextLines( gcodeText )
		self.threadSequence = None
		if fillRepository.threadSequenceInfillLoops.value:
			self.threadSequence = [ 'infill', 'loops', 'perimeter' ]
		if fillRepository.threadSequenceInfillPerimeter.value:
			self.threadSequence = [ 'infill', 'perimeter', 'loops' ]
		if fillRepository.threadSequenceLoopsInfill.value:
			self.threadSequence = [ 'loops', 'infill', 'perimeter' ]
		if fillRepository.threadSequenceLoopsPerimeter.value:
			self.threadSequence = [ 'loops', 'perimeter', 'infill' ]
		if fillRepository.threadSequencePerimeterInfill.value:
			self.threadSequence = [ 'perimeter', 'infill', 'loops' ]
		if fillRepository.threadSequencePerimeterLoops.value:
			self.threadSequence = [ 'perimeter', 'loops', 'infill' ]
		self.parseInitialization()
		self.infillWidth = fillRepository.infillWidthOverThickness.value * self.layerThickness
		self.interiorExtrusionWidth = self.infillWidth
		self.fillInset = self.perimeterWidth + 0.5 * self.infillWidth - self.infillWidth * self.fillRepository.infillPerimeterOverlap.value
		if self.fillRepository.infillInteriorDensityOverExteriorDensity.value > 0:
			self.interiorExtrusionWidth /= self.fillRepository.infillInteriorDensityOverExteriorDensity.value
		self.infillSolidity = fillRepository.infillSolidity.value
		if self.isGridToBeExtruded():
			self.setGridVariables( fillRepository )
		self.infillBeginRotation = math.radians( fillRepository.infillBeginRotation.value )
		self.infillOddLayerExtraRotation = math.radians( fillRepository.infillOddLayerExtraRotation.value )
		self.solidSurfaceThickness = int( round( self.fillRepository.solidSurfaceThickness.value ) )
		self.doubleSolidSurfaceThickness = self.solidSurfaceThickness + self.solidSurfaceThickness
		for lineIndex in xrange( self.lineIndex, len( self.lines ) ):
			self.parseLine( lineIndex )
		for layerIndex in xrange( len( self.rotatedLayers ) ):
			self.addFill( layerIndex )
		self.distanceFeedRate.addLines( self.lines[ self.shutdownLineIndex : ] )
		return self.distanceFeedRate.output.getvalue()

	def getGridPoints( self, fillLoops, reverseZRotationAngle ):
		"Get the grid pointsl."
		if self.infillSolidity > 0.8:
			return []
		gridPoints = []
		rotationBaseAngle = euclidean.getPolar( self.infillBeginRotation, 1.0 )
		reverseRotationBaseAngle = complex( rotationBaseAngle.real, - rotationBaseAngle.imag )
		gridRotationAngle = reverseZRotationAngle * rotationBaseAngle
		gridAlreadyFilledArounds = []
		gridRotatedExtruderLoops = []
		back = - 999999999.0
		front = - back
		gridInset = 1.2 * self.interiorExtrusionWidth
		muchGreaterThanLayerFillInset = 1.5 * gridInset
		slightlyGreaterThanFill = 1.01 * gridInset
		for loop in fillLoops:
			gridAlreadyFilledLoop = []
			gridAlreadyFilledArounds.append( gridAlreadyFilledLoop )
			planeRotatedPerimeter = euclidean.getPointsRoundZAxis( reverseRotationBaseAngle, loop )
			gridRotatedExtruderLoops.append( planeRotatedPerimeter )
			circleNodes = intercircle.getCircleNodesFromLoop( planeRotatedPerimeter, slightlyGreaterThanFill )
			centers = intercircle.getCentersFromCircleNodes( circleNodes )
			for center in centers:
				alreadyFilledInset = intercircle.getSimplifiedInsetFromClockwiseLoop( center, gridInset )
				if euclidean.getMaximumSpan( alreadyFilledInset ) > muchGreaterThanLayerFillInset:
					gridAlreadyFilledLoop.append( alreadyFilledInset )
					if euclidean.isPathInsideLoop( planeRotatedPerimeter, alreadyFilledInset ) == euclidean.isWiddershins( planeRotatedPerimeter ):
						for point in alreadyFilledInset:
							back = max( back, point.imag )
							front = min( front, point.imag )
		front = ( 0.01 + math.ceil( front / self.gridRadius ) ) * self.gridRadius
		fillWidth = back - front
		numberOfLines = int( math.ceil( fillWidth / self.gridRadius ) )
		gridSegmentLists = euclidean.getHorizontalSegmentListsFromLoopLists( gridAlreadyFilledArounds, front, numberOfLines, gridRotatedExtruderLoops, self.gridRadius )
		shortenedSegmentLists = []
		for fillLine in xrange( numberOfLines ):
			lineSegments = gridSegmentLists[ fillLine ]
			shortenedSegments = []
			for lineSegment in lineSegments:
				addShortenedLineSegment( lineSegment, self.interiorExtrusionWidth, shortenedSegments )
			shortenedSegmentLists.append( shortenedSegments )
		for shortenedSegmentList in shortenedSegmentLists:
			for shortenedSegment in shortenedSegmentList:
				endpointFirst = shortenedSegment[ 0 ]
				endpointSecond = shortenedSegment[ 1 ]
				begin = min( endpointFirst.point.real, endpointSecond.point.real )
				end = max( endpointFirst.point.real, endpointSecond.point.real )
				y = endpointFirst.point.imag
				offset = self.offsetMultiplier * self.gridRadius * ( round( y / self.gridRadius ) % 2 )
				self.addGridLinePoints( begin, end, gridPoints, gridRotationAngle, offset, y )
		return gridPoints

	def getLayerRoundZ( self, layerIndex ):
		"Get the plane angle around z that the layer is rotated by."
		rotation = self.rotatedLayers[ layerIndex ].rotation
		if rotation != None:
			return rotation
		infillBeginRotationRepeat = self.fillRepository.infillBeginRotationRepeat.value
		infillOddLayerRotationMultiplier = float( layerIndex % ( infillBeginRotationRepeat + 1 ) == infillBeginRotationRepeat )
		return euclidean.getPolar( self.infillBeginRotation + infillOddLayerRotationMultiplier * self.infillOddLayerExtraRotation, 1.0 )

	def getNextGripXStep( self, gridXStep ):
		"Get the next grid x step, increment by an extra one every three if hexagonal grid is chosen."
		gridXStep += 1
		if self.fillRepository.infillPatternGridHexagonal.value:
			if gridXStep % 3 == 0:
				gridXStep += 1
		return gridXStep

	def getCarveArea( self, layerIndex ):
		"Get the area of the carve."
		if layerIndex < 0 or layerIndex >= len( self.rotatedLayers ):
			return 0.0
		surroundingLoops = self.rotatedLayers[ layerIndex ].surroundingLoops
		area = 0.0
		for surroundingLoop in surroundingLoops:
			area += euclidean.getPolygonArea( surroundingLoop.boundary )
		return area

	def isGridToBeExtruded( self ):
		"Determine if the grid is to be extruded."
		return ( not self.fillRepository.infillPatternLine.value ) and self.fillRepository.infillInteriorDensityOverExteriorDensity.value > 0

	def isPointInsideLineSegments( self, gridPoint ):
		"Is the point inside the line segments of the loops."
		if self.solidSurfaceThickness <= 0:
			return True
		fillLine = int( round( gridPoint.imag / self.layerExtrusionWidth - self.frontOverWidth ) )
		if fillLine >= len( self.horizontalSegmentLists ) or fillLine < 0:
			return False
		lineSegments = self.horizontalSegmentLists[ fillLine ]
		surroundingXIntersections = self.surroundingXIntersectionLists[ fillLine ]
		for lineSegment in lineSegments:
			if isSegmentCompletelyInAnIntersection( lineSegment, surroundingXIntersections ):
				xFirst = lineSegment[ 0 ].point.real
				xSecond = lineSegment[ 1 ].point.real
				if gridPoint.real > min( xFirst, xSecond ) and gridPoint.real < max( xFirst, xSecond ):
					return True
		return False

	def linearMove( self, splitLine ):
		"Add a linear move to the thread."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		if self.extruderActive:
			self.addToThread( location )
		self.oldLocation = location

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			self.distanceFeedRate.parseSplitLine( firstWord, splitLine )
			if firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float( splitLine[ 1 ] )
				threadSequenceString = ' '.join( self.threadSequence )
				self.distanceFeedRate.addTagBracketedLine( 'threadSequenceString', threadSequenceString )
			elif firstWord == '(<extrusion>)':
				self.distanceFeedRate.addLine( line )
				return
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> fill </procedureDone>)' )
			elif firstWord == '(<fillInset>':
				self.fillInset = float( splitLine[ 1 ] )
			elif firstWord == '(<bridgeWidthMultiplier>':
				self.bridgeWidthMultiplier = float( splitLine[ 1 ] )
			elif firstWord == '(<layerThickness>':
				self.layerThickness = float( splitLine[ 1 ] )
			self.distanceFeedRate.addLine( line )
 
	def parseLine( self, lineIndex ):
		"Parse a gcode line and add it to the fill skein."
		line = self.lines[ lineIndex ]
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == 'M101':
			self.extruderActive = True
		elif firstWord == 'M103':
			self.extruderActive = False
			self.thread = None
			self.isPerimeter = False
		elif firstWord == '(<boundaryPoint>':
			location = gcodec.getLocationFromSplitLine( None, splitLine )
			self.surroundingLoop.addToBoundary( location )
		elif firstWord == '(<bridgeRotation>':
			secondWordWithoutBrackets = splitLine[ 1 ].replace( '(', '' ).replace( ')', '' )
			self.rotatedLayer.rotation = complex( secondWordWithoutBrackets )
		elif firstWord == '(</extrusion>)':
			self.shutdownLineIndex = lineIndex
		elif firstWord == '(<layer>':
			self.rotatedLayer = RotatedLayer( float( splitLine[ 1 ] ) )
			self.rotatedLayers.append( self.rotatedLayer )
			self.thread = None
		elif firstWord == '(<perimeter>)':
			self.isPerimeter = True
		elif firstWord == '(<surroundingLoop>)':
			self.surroundingLoop = euclidean.SurroundingLoop( self.threadSequence )
			self.rotatedLayer.surroundingLoops.append( self.surroundingLoop )
		elif firstWord == '(</surroundingLoop>)':
			self.surroundingLoop = None

	def setGridVariables( self, fillRepository ):
		"Set the grid variables."
		self.gridRadius = self.interiorExtrusionWidth / self.infillSolidity
		self.gridWidthMultiplier = 2.0
		self.offsetMultiplier = 1.0
		if self.fillRepository.infillPatternGridHexagonal.value:
			self.gridWidthMultiplier = 2.0 / math.sqrt( 3.0 )
			self.offsetMultiplier = 2.0 / math.sqrt( 3.0 ) * 1.5
		if self.fillRepository.infillPatternGridRectangular.value:
			halfGridRadiusMinusInteriorExtrusionWidth = 0.5 * ( self.gridRadius - self.interiorExtrusionWidth )
			self.gridJunctionSeparationAtEnd = halfGridRadiusMinusInteriorExtrusionWidth * fillRepository.gridJunctionSeparationOverOctogonRadiusAtEnd.value
			self.gridJunctionSeparationAtMiddle = halfGridRadiusMinusInteriorExtrusionWidth * fillRepository.gridJunctionSeparationOverOctogonRadiusAtMiddle.value


class RotatedLayer:
	"A rotated layer."
	def __init__( self, z ):
		self.rotation = None
		self.surroundingLoops = []
		self.z = z

	def __repr__( self ):
		"Get the string representation of this RotatedLayer."
		return '%s, %s, %s' % ( self.z, self.rotation, self.surroundingLoops )


class YIntersectionPath:
	"A class to hold the y intersection position, the loop which it intersected and the point index of the loop which it intersected."
	def __init__( self, pathIndex, pointIndex, y ):
		"Initialize from the path, point index, and y."
		self.pathIndex = pathIndex
		self.pointIndex = pointIndex
		self.y = y

	def __repr__( self ):
		"Get the string representation of this y intersection."
		return '%s, %s, %s' % ( self.pathIndex, self.pointIndex, self.y )

	def getPath( self, paths ):
		"Get the path from the paths and path index."
		return paths[ self.pathIndex ]

	def getPointIndexPlusOne( self ):
		"Get the point index plus one."
		return self.pointIndex + 1


def main():
	"Display the fill dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
