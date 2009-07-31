"""
Intercircle is a collection of utilities for intersecting circles, used to get smooth loops around a collection of points and inset & outset loops.

"""

from __future__ import absolute_import
try:
	import psyco
	psyco.full()
except:
	pass
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
import math


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addCircleIntersectionLoop( circleIntersectionPathComplexes, circleIntersections ):
	"Add a circle intersection loop."
	firstCircleIntersection = circleIntersectionPathComplexes[ 0 ]
	circleIntersectionAhead = firstCircleIntersection
	for circleIntersectionIndex in xrange( len( circleIntersections ) + 1 ):
		circleIntersectionAhead = circleIntersectionAhead.getCircleIntersectionAhead()
		if circleIntersectionAhead.index == firstCircleIntersection.index:
			firstCircleIntersection.steppedOn = True
			return
		if circleIntersectionAhead.steppedOn == True:
			print( 'circleIntersectionAhead.steppedOn == True in intercircle.' )
			print( circleIntersectionAhead )
		circleIntersectionAhead.addToList( circleIntersectionPathComplexes )
	firstCircleIntersection.steppedOn = True
	print( "addCircleIntersectionLoop would have gone into an endless loop, this should never happen." )
	print( "circleIntersectionPathComplexes" )
	for circleIntersectionComplex in circleIntersectionPathComplexes:
		print( circleIntersectionComplex )
		print( circleIntersectionComplex.circleNodeAhead )
		print( circleIntersectionComplex.circleNodeBehind )
	print( "firstCircleIntersection" )
	print( firstCircleIntersection )
	print( "circleIntersections" )
	for circleIntersectionComplex in circleIntersections:
		print( circleIntersectionComplex )

def addOperatingOrbits( boundaryLoops, pointComplex, skein, temperatureChangeTime, z ):
	"Add the orbits before the operating layers."
	if len( boundaryLoops ) < 1:
		return
	largestLength = - 999999999.0
	largestLoop = None
	perimeterOutset = 0.4 * skein.extrusionPerimeterWidth
	greaterThanPerimeterOutset = 1.1 * perimeterOutset
	for boundaryLoop in boundaryLoops:
		centers = getCentersFromLoopDirection( True, boundaryLoop, greaterThanPerimeterOutset )
		for center in centers:
			outset = getSimplifiedInsetFromClockwiseLoop( center, perimeterOutset )
			if isLargeSameDirection( outset, center, perimeterOutset ):
				loopLength = euclidean.getPolygonLength( outset )
				if loopLength > largestLength:
					largestLength = loopLength
					largestLoop = outset
	if largestLoop == None:
		return
	if pointComplex != None:
		largestLoop = euclidean.getLoopStartingNearest( skein.extrusionPerimeterWidth, pointComplex, largestLoop )
	addOrbits( largestLoop, skein, temperatureChangeTime, z )

def addOrbits( loop, skein, temperatureChangeTime, z ):
	"Add orbits with the extruder off."
	if len( loop ) < 1:
		print( 'Zero length loop which was skipped over, this should never happen.' )
	if temperatureChangeTime < 1.5:
		return
	timeInOrbit = 0.0
	while timeInOrbit < temperatureChangeTime:
		for point in loop:
			skein.addGcodeFromFeedrateMovementZ( 60.0 * skein.orbitalFeedratePerSecond, point, z )
		timeInOrbit += euclidean.getPolygonLength( loop ) / skein.orbitalFeedratePerSecond

def addPointsFromSegment( pointComplexes, radius, pointBeginComplex, pointEndComplex, thresholdRatio = 0.9 ):
	"Add point complexes between the endpoints of a segment."
	if radius <= 0.0:
		print( 'This should never happen, radius should never be zero or less in addPointsFromSegment in intercircle.' )
	thresholdRadius = radius * thresholdRatio # a higher number would be faster but would leave bigger dangling loops.
	thresholdDiameter = thresholdRadius * 2.0
	segmentComplex = pointEndComplex - pointBeginComplex
	segmentComplexLength = abs( segmentComplex )
	extraCircles = int( math.floor( segmentComplexLength / thresholdDiameter ) )
	lengthIncrement = segmentComplexLength / ( float( extraCircles ) + 1.0 )
	if segmentComplexLength == 0.0:
		print( 'This should never happen, segmentComplexLength = 0.0 in intercircle.' )
		print( 'pointBeginComplex' )
		print( pointBeginComplex )
		print( pointEndComplex )
		return
	segmentComplex *= lengthIncrement / segmentComplexLength
	nextCircleCenterComplex = pointBeginComplex + segmentComplex
	for circleIndex in xrange( extraCircles ):
		pointComplexes.append( nextCircleCenterComplex )
		nextCircleCenterComplex += segmentComplex

def getArounds( loops, radius ):
	"Get the complex centers of the circle intersection loops from circle nodes."
	arounds = []
	circleNodes = []
	muchGreaterThanRadius = 2.5 * radius
	slightlyGreaterThanRadius = 1.01 * radius
	for loop in loops:
		circleNodes += getCircleNodesFromLoop( loop, slightlyGreaterThanRadius )
	centers = getCentersFromCircleNodes( circleNodes )
	for center in centers:
		inset = getSimplifiedInsetFromClockwiseLoop( center, radius )
		if isLargeSameDirection( inset, center, muchGreaterThanRadius ):
			arounds.append( inset )
	return arounds

def getCentersFromCircleNodes( circleNodesComplex ):
	"Get the complex centers of the circle intersection loops from circle nodes."
	if len( circleNodesComplex ) < 2:
		return []
	circleIntersections = getCircleIntersectionsFromCircleNodes( circleNodesComplex )
	circleIntersectionLoopComplexes = getCircleIntersectionLoops( circleIntersections )
	return getCentersFromIntersectionLoops( circleIntersectionLoopComplexes )

def getCentersFromIntersectionLoop( circleIntersectionLoopComplex ):
	"Get the centers from the intersection loop."
	loop = []
	for circleIntersectionComplex in circleIntersectionLoopComplex:
		loop.append( circleIntersectionComplex.circleNodeAhead.circle )
	return loop

def getCentersFromIntersectionLoops( circleIntersectionLoopComplexes ):
	"Get the centers from the intersection loops."
	centers = []
	for circleIntersectionLoopComplex in circleIntersectionLoopComplexes:
		centers.append( getCentersFromIntersectionLoop( circleIntersectionLoopComplex ) )
	return centers

def getCentersFromLoop( loop, radius ):
	"Get the centers of the loop."
	circleNodes = getCircleNodesFromLoop( loop, radius )
	return getCentersFromCircleNodes( circleNodes )

def getCentersFromLoopDirection( isWiddershins, loop, radius ):
	"Get the centers of the loop which go around in the given direction."
	centers = getCentersFromLoop( loop, radius )
	return getLoopsFromLoopsDirection( isWiddershins, centers )

def getCircleIntersectionsFromCircleNodes( circleNodesComplex ):
	"Get all the circle intersections which exist between all the circle nodes."
	if len( circleNodesComplex ) < 1:
		return
	circleIntersections = []
	index = 0
	pixelTable = {}
	slightlyGreaterThanRadius = 1.01 * circleNodesComplex[ 0 ].radius
	for circleNode in circleNodesComplex:
		circleOverWidth = circleNode.circle / slightlyGreaterThanRadius
		x = int( round( circleOverWidth.real ) )
		y = int( round( circleOverWidth.imag ) )
		euclidean.addElementToPixelList( circleNode, pixelTable, x, y )
	slightlyGreaterThanDiameter = slightlyGreaterThanRadius + slightlyGreaterThanRadius
	accumulatedCircleNodeTable = {}
	for circleNodeIndex in xrange( len( circleNodesComplex ) ):
		circleNodeBehind = circleNodesComplex[ circleNodeIndex ]
		circleNodeIndexMinusOne = circleNodeIndex - 1
		if circleNodeIndexMinusOne >= 0:
			circleNodeAdditional = circleNodesComplex[ circleNodeIndexMinusOne ]
			circleOverSlightlyGreaterThanDiameter = circleNodeAdditional.circle / slightlyGreaterThanDiameter
			x = int( round( circleOverSlightlyGreaterThanDiameter.real ) )
			y = int( round( circleOverSlightlyGreaterThanDiameter.imag ) )
			euclidean.addElementToPixelList( circleNodeAdditional, accumulatedCircleNodeTable, x, y )
		withinNodes = circleNodeBehind.getWithinNodes( accumulatedCircleNodeTable, slightlyGreaterThanDiameter )
		for circleNodeAhead in withinNodes:
			circleIntersectionForward = CircleIntersection( circleNodeAhead, index, circleNodeBehind )
			if not circleIntersectionForward.isWithinCircles( pixelTable, slightlyGreaterThanRadius ):
				circleIntersections.append( circleIntersectionForward )
				circleNodeBehind.circleIntersections.append( circleIntersectionForward )
				index += 1
			circleIntersectionBackward = CircleIntersection( circleNodeBehind, index, circleNodeAhead )
			if not circleIntersectionBackward.isWithinCircles( pixelTable, slightlyGreaterThanRadius ):
				circleIntersections.append( circleIntersectionBackward )
				circleNodeAhead.circleIntersections.append( circleIntersectionBackward )
				index += 1
	return circleIntersections

def getCircleIntersectionLoops( circleIntersections ):
	"Get all the loops going through the circle intersections."
	circleIntersectionLoopComplexes = []
	for circleIntersectionComplex in circleIntersections:
		if not circleIntersectionComplex.steppedOn:
			circleIntersectionLoopComplex = [ circleIntersectionComplex ]
			circleIntersectionLoopComplexes.append( circleIntersectionLoopComplex )
			addCircleIntersectionLoop( circleIntersectionLoopComplex, circleIntersections )
	return circleIntersectionLoopComplexes

def getCircleNodesFromLoop( loop, radius ):
	"Get the circle nodes from every point on a loop and between points."
	radius = abs( radius )
	pointComplexes = []
	for pointComplexIndex in xrange( len( loop ) ):
		pointComplex = loop[ pointComplexIndex ]
		pointComplexSecond = loop[ ( pointComplexIndex + 1 ) % len( loop ) ]
		pointComplexes.append( pointComplex )
		addPointsFromSegment( pointComplexes, radius, pointComplex, pointComplexSecond )
	return getCircleNodesFromPoints( pointComplexes, radius )

def getCircleNodesFromPoints( pointComplexes, radius ):
	"Get the circle nodes from a path."
	circleNodesComplex = []
	pointComplexes = euclidean.getAwayPoints( pointComplexes, 0.001 * radius )
	for pointComplex in pointComplexes:
		circleNodesComplex.append( CircleNode( pointComplex, len( circleNodesComplex ), radius ) )
	return circleNodesComplex

def getInsetFromClockwiseTriple( aheadAbsoluteComplex, behindAbsoluteComplex, centerComplex, radius ):
	"Get loop inset from clockwise triple, out from widdershins loop."
	originalCenterMinusBehindComplex = euclidean.getNormalized( centerComplex - behindAbsoluteComplex )
	reverseRoundZAngle = complex( originalCenterMinusBehindComplex.real, - originalCenterMinusBehindComplex.imag )
	aheadAbsoluteComplex *= reverseRoundZAngle
	behindAbsoluteComplex *= reverseRoundZAngle
	centerComplex *= reverseRoundZAngle
	aheadIntersectionComplex = getIntersectionAtInset( aheadAbsoluteComplex, centerComplex, radius )
	behindIntersectionComplex = getIntersectionAtInset( centerComplex, behindAbsoluteComplex, radius )
	centerComplexMinusAhead = centerComplex - aheadAbsoluteComplex
	if abs( centerComplexMinusAhead.imag ) < abs( 0.000001 * centerComplexMinusAhead.real ):
		between = 0.5 * ( aheadIntersectionComplex + behindIntersectionComplex )
		return originalCenterMinusBehindComplex * between
	yMinusAhead = behindIntersectionComplex.imag - aheadIntersectionComplex.imag
	x = aheadIntersectionComplex.real + yMinusAhead * centerComplexMinusAhead.real / centerComplexMinusAhead.imag
	return originalCenterMinusBehindComplex * complex( x, behindIntersectionComplex.imag )

def getInsetFromClockwiseLoop( loop, radius ):
	"Get loop inset from clockwise loop, out from widdershins loop."
	insetLoopComplex = []
	for pointComplexIndex in xrange( len( loop ) ):
		behindAbsoluteComplex = loop[ ( pointComplexIndex + len( loop ) - 1 ) % len( loop ) ]
		centerComplex = loop[ pointComplexIndex ]
		aheadAbsoluteComplex = loop[ ( pointComplexIndex + 1 ) % len( loop ) ]
		insetLoopComplex.append( getInsetFromClockwiseTriple( aheadAbsoluteComplex, behindAbsoluteComplex, centerComplex, radius ) )
	return insetLoopComplex

def getInsetSeparateLoopsFromLoops( inset, loops ):
	"Get the separate inset loops."
	isInset = inset > 0
	insetSeparateLoops = []
	radius = abs( inset )
	arounds = getArounds( loops, radius )
	for around in arounds:
		leftPoint = euclidean.getLeftPoint( around )
		if isInset == euclidean.isInFilledRegion( leftPoint, loops ):
			insetSeparateLoops.append( around )
	return insetSeparateLoops

def getIntersectionAtInset( aheadComplex, behindComplex, inset ):
	"Get circle intersection loop at inset from segment."
	aheadComplexMinusBehindComplex = 0.5 * ( aheadComplex - behindComplex )
	rotatedClockwiseQuarter = complex( aheadComplexMinusBehindComplex.imag, - aheadComplexMinusBehindComplex.real )
	rotatedClockwiseQuarter *= inset / abs( rotatedClockwiseQuarter )
	return aheadComplexMinusBehindComplex + behindComplex + rotatedClockwiseQuarter

def getLoopsFromLoopsDirection( isWiddershins, loops ):
	"Get the loops going round in a given direction."
	directionalLoopComplexes = []
	for loop in loops:
		if euclidean.isWiddershins( loop ) == isWiddershins:
			directionalLoopComplexes.append( loop )
	return directionalLoopComplexes

def getSimplifiedInsetFromClockwiseLoop( loop, radius ):
	"Get loop inset from clockwise loop, out from widdershins loop."
	return getWithoutIntersections( euclidean.getSimplifiedLoop( getInsetFromClockwiseLoop( loop, radius ), radius ) )

def getWithoutIntersections( loop ):
	"Get loop without intersections."
	lastLoopLength = len( loop )
	while lastLoopLength > 3:
		removeIntersection( loop )
		if len( loop ) == lastLoopLength:
			return loop
		lastLoopLength = len( loop )
	return loop

def isLarge( loop, requiredSize ):
	"Determine if the loop is as large as the required size."
	return euclidean.getMaximumSpan( loop ) > abs( requiredSize )

def isLargeSameDirection( inset, loop, requiredSize ):
	"Determine if the inset is in the same direction as the loop and if the inset is as large as the required size."
	if euclidean.isWiddershins( inset ) != euclidean.isWiddershins( loop ):
		return False
	return isLarge( inset, requiredSize )

def isLoopIntersectingLoop( anotherLoop, loop ):
	"Determine if the a loop is intersecting another loop."
	for pointIndex in xrange( len( loop ) ):
		pointFirst = loop[ pointIndex ]
		pointSecond = loop[ ( pointIndex + 1 ) % len( loop ) ]
		segment = pointFirst - pointSecond
		normalizedSegment = euclidean.getNormalized( segment )
		segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
		segmentFirstPoint = segmentYMirror * pointFirst
		segmentSecondPoint = segmentYMirror * pointSecond
		if euclidean.isLoopIntersectingInsideXSegment( anotherLoop, segmentFirstPoint.real, segmentSecondPoint.real, segmentYMirror, segmentFirstPoint.imag ):
			return True
	return False

def removeIntersection( loop ):
	"Get loop without the first intersection."
	withoutIntersection = []
	for pointIndex in xrange( len( loop ) ):
		behindComplex = loop[ ( pointIndex + len( loop ) - 1 ) % len( loop ) ]
		behindEndComplex = loop[ ( pointIndex + len( loop ) - 2 ) % len( loop ) ]
		behindMidpointComplex = 0.5 * ( behindComplex + behindEndComplex )
		aheadComplex = loop[ pointIndex ]
		aheadEndComplex = loop[ ( pointIndex + 1 ) % len( loop ) ]
		aheadMidpointComplex = 0.5 * ( aheadComplex + aheadEndComplex )
		normalizedSegment = behindComplex - behindMidpointComplex
		normalizedSegmentLength = abs( normalizedSegment )
		if normalizedSegmentLength > 0.0:
			normalizedSegment /= normalizedSegmentLength
			segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
			behindRotated = segmentYMirror * behindComplex
			behindMidpointRotated = segmentYMirror * behindMidpointComplex
			aheadRotated = segmentYMirror * aheadComplex
			aheadMidpointRotated = segmentYMirror * aheadMidpointComplex
			y = behindRotated.imag
			isYAboveFirst = y > aheadRotated.imag
			isYAboveSecond = y > aheadMidpointRotated.imag
			if isYAboveFirst != isYAboveSecond:
				xIntersection = euclidean.getXIntersection( aheadRotated, aheadMidpointRotated, y )
				if xIntersection > min( behindMidpointRotated.real, behindRotated.real ) and xIntersection < max( behindMidpointRotated.real, behindRotated.real ):
					intersectionPoint = normalizedSegment * complex( xIntersection, y )
					loop[ ( pointIndex + len( loop ) - 1 ) % len( loop ) ] = intersectionPoint
					del loop[ pointIndex ]
					return


class BoundingLoop:
	"A class to hold a bounding loop composed of a minimum complex, a maximum complex and an outset loop."
	def __cmp__( self, other ):
		"Get comparison in order to sort bounding loops in descending order of area."
		if self.area < other.area:
			return 1
		if self.area > other.area:
			return - 1
		return 0

	def __repr__( self ):
		"Get the string representation of this bounding loop."
		return '%s, %s, %s' % ( self.minimum, self.maximum, self.loop )

	def getFromLoop( self, loop ):
		"Get the bounding loop from a path."
		self.loop = loop
		self.maximum = euclidean.getMaximumFromPoints( loop )
		self.minimum = euclidean.getMaximumFromPoints( loop )
		return self

	def getOutsetBoundingLoop( self, outsetDistance ):
		"Outset the bounding rectangle and loop by a distance."
		outsetBoundingLoop = BoundingLoop()
		outsetBoundingLoop.maximum = self.maximum + complex( outsetDistance, outsetDistance )
		outsetBoundingLoop.minimum = self.minimum - complex( outsetDistance, outsetDistance )
		greaterThanOutsetDistance = 1.1 * outsetDistance
		centers = getCentersFromLoopDirection( True, self.loop, greaterThanOutsetDistance )
		outsetBoundingLoop.loop = getSimplifiedInsetFromClockwiseLoop( centers[ 0 ], outsetDistance )
		return outsetBoundingLoop

	def isEntirelyInsideAnother( self, anotherBoundingLoop ):
		"Determine if this bounding loop is entirely inside another bounding loop."
		if self.minimum.imag < anotherBoundingLoop.minimum.imag or self.minimum.real < anotherBoundingLoop.minimum.real:
			return False
		if self.maximum.imag > anotherBoundingLoop.maximum.imag or self.maximum.real > anotherBoundingLoop.maximum.real:
			return False
		for point in self.loop:
			if euclidean.getNumberOfIntersectionsToLeft( point, anotherBoundingLoop.loop ) % 2 == 0:
				return False
		return not isLoopIntersectingLoop( anotherBoundingLoop.loop, self.loop ) #later check for intersection on only acute angles

	def isOverlappingAnother( self, anotherBoundingLoop ):
		"Determine if this bounding loop is intersecting another bounding loop."
		if self.isRectangleMissingAnother( anotherBoundingLoop ):
			return False
		for point in self.loop:
			if euclidean.getNumberOfIntersectionsToLeft( point, anotherBoundingLoop.loop ) % 2 == 1:
				return True
		for point in anotherBoundingLoop.loop:
			if euclidean.getNumberOfIntersectionsToLeft( point, self.loop ) % 2 == 1:
				return True
		return isLoopIntersectingLoop( anotherBoundingLoop.loop, self.loop ) #later check for intersection on only acute angles

	def isRectangleMissingAnother( self, anotherBoundingLoop ):
		"Determine if the rectangle of this bounding loop is missing the rectangle of another bounding loop."
		if self.maximum.imag < anotherBoundingLoop.minimum.imag or self.maximum.real < anotherBoundingLoop.minimum.real:
			return True
		return self.minimum.imag > anotherBoundingLoop.maximum.imag or self.minimum.real > anotherBoundingLoop.maximum.real


class CircleIntersection:
	"An intersection of two complex circles."
	def __init__( self, circleNodeAhead, index, circleNodeBehind ):
		self.circleNodeAhead = circleNodeAhead
		self.circleNodeBehind = circleNodeBehind
		self.index = index
		self.steppedOn = False

	def __repr__( self ):
		"Get the string representation of this CircleIntersection."
		return '%s, %s, %s, %s, %s' % ( self.index, self.getAbsolutePosition(), self.circleNodeBehind.index, self.circleNodeAhead.index, self.getCircleIntersectionAhead().index )

	def addToList( self, circleIntersectionPath ):
		self.steppedOn = True
		circleIntersectionPath.append( self )

	def getAbsolutePosition( self ):
		return self.getPositionRelativeToBehind() + self.circleNodeBehind.circle

	def getCircleIntersectionAhead( self ):
		circleIntersections = self.circleNodeAhead.circleIntersections
		circleIntersectionAhead = None
		smallestWiddershinsDot = 999999999.0
		positionRelativeToAhead = self.getAbsolutePosition() - self.circleNodeAhead.circle
		positionRelativeToAhead = euclidean.getNormalized( positionRelativeToAhead )
		for circleIntersection in circleIntersections:
			if not circleIntersection.steppedOn:
				circleIntersectionRelative = circleIntersection.getPositionRelativeToBehind()
				circleIntersectionRelative = euclidean.getNormalized( circleIntersectionRelative )
				widdershinsDot = euclidean.getWiddershinsDotGiven( positionRelativeToAhead, circleIntersectionRelative )
				if widdershinsDot < smallestWiddershinsDot:
					smallestWiddershinsDot = widdershinsDot
					circleIntersectionAhead = circleIntersection
		if circleIntersectionAhead == None:
			print( 'this should never happen, circleIntersectionAhead in intercircle is None' )
			print( self.circleNodeAhead.circle )
			for circleIntersection in circleIntersections:
				print( circleIntersection.circleNodeAhead.circle )
		return circleIntersectionAhead

	def getPositionRelativeToBehind( self ):
		aheadMinusBehind = 0.5 * ( self.circleNodeAhead.circle - self.circleNodeBehind.circle )
		radius = self.circleNodeAhead.radius
		halfChordWidth = math.sqrt( radius * radius - aheadMinusBehind.real * aheadMinusBehind.real - aheadMinusBehind.imag * aheadMinusBehind.imag )
		rotatedClockwiseQuarter = complex( aheadMinusBehind.imag, - aheadMinusBehind.real )
		if abs( rotatedClockwiseQuarter ) == 0:
			print( self.circleNodeAhead.circle )
			print( self.circleNodeBehind.circle )
		return aheadMinusBehind + rotatedClockwiseQuarter * ( halfChordWidth / abs( rotatedClockwiseQuarter ) )

	def isWithinCircles( self, pixelTable, width ):
		absolutePosition = self.getAbsolutePosition()
		absolutePositionOverWidth = absolutePosition / width
		x = int( round( absolutePositionOverWidth.real ) )
		y = int( round( absolutePositionOverWidth.imag ) )
		squareValues = euclidean.getSquareValues( pixelTable, x, y )
		for squareValue in squareValues:
			if abs( squareValue.circle - absolutePosition ) < self.circleNodeAhead.radius:
				if squareValue != self.circleNodeAhead and squareValue != self.circleNodeBehind:
					return True
		return False


class CircleNode:
	"A complex node of complex circle intersections."
	def __init__( self, circle, index, radius ):
		self.circle = circle
		self.circleIntersections = []
		self.diameter = radius + radius
		self.index = index
		self.radius = radius

	def __repr__( self ):
		"Get the string representation of this CircleNode."
		return '%s, %s' % ( self.index, self.circle )

	def getWithinNodes( self, pixelTable, width ):
		circleOverWidth = self.circle / width
		x = int( round( circleOverWidth.real ) )
		y = int( round( circleOverWidth.imag ) )
		withinNodes = []
		squareValues = euclidean.getSquareValues( pixelTable, x, y )
		for squareValue in squareValues:
			if abs( self.circle - squareValue.circle ) < self.diameter:
				withinNodes.append( squareValue )
		return withinNodes
