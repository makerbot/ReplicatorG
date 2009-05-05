"""
Euclidean is a collection of python utilities for complex numbers, paths, polygons & Vector3s.

To use euclidean, install python 2.x on your machine, which is avaliable from http://www.python.org/download/

Then in the folder which euclidean is in, type 'python' in a shell to run the python interpreter.  Finally type
'import euclidean' to import these utilities and 'from vector3 import Vector3' to import the Vector3 class.


Below are examples of euclidean use.

>>> from euclidean import *
>>> origin=complex()
>>> right=complex(1.0,0.0)
>>> back=complex(0.0,1.0)
>>> getMaximum(right,back)
1.0, 1.0
>>> polygon=[origin, right, back]
>>> getPolygonLength(polygon)
3.4142135623730949
>>> getPolygonArea(polygon)
0.5
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
import math


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def addCircleToPixelTable( pixelTable, pointComplex ):
	"Add pixel to the pixel table."
	xStep = int( round( pointComplex.real ) )
	yStep = int( round( pointComplex.imag ) )
	for xCircleStep in xrange( xStep - 2, xStep + 3 ):
		for yCircleStep in xrange( yStep - 2, yStep + 3 ):
			stepKey = ( xCircleStep, yCircleStep )
			pixelTable[ stepKey ] = None

def addElementToPixelList( element, pixelTable, x, y ):
	"Add an element to the pixel list."
	stepKey = getStepKey( x, y )
	if stepKey in pixelTable:
		pixelTable[ stepKey ].append( element )
	else:
		pixelTable[ stepKey ] = [ element ]

def addLoopToPixelTable( loop, pixelTable, width ):
	"Add loop to the pixel table."
	for pointIndex in xrange( len( loop ) ):
		pointBegin = loop[ pointIndex ]
		pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
		addSegmentToPixelTable( pointBegin, pointEnd, pixelTable, 0, 0, width )

def addPathToPixelTable( path, pixelTable, width ):
	"Add path to the pixel table."
	for pointIndex in xrange( len( path ) - 1 ):
		pointBegin = path[ pointIndex ]
		pointEnd = path[ pointIndex + 1 ]
		addSegmentToPixelTable( pointBegin, pointEnd, pixelTable, 0, 0, width )

def addPixelToPixelTable( pixelTable, value, x, y ):
	"Add pixel to the pixel table."
	pixelTable[ getStepKey( x, y ) ] = value

def addPixelToPixelTableWithSteepness( isSteep, pixelTable, x, y ):
	"Add pixels to the pixel table with steepness."
	if isSteep:
		addPixelToPixelTable( pixelTable, None, y, x )
	else:
		addPixelToPixelTable( pixelTable, None, x, y )

def addPointToPath( path, pixelTable, point, width ):
	"Add a point to a path and the pixel table."
	path.append( point )
	if len( path ) < 2:
		return
	begin = path[ - 2 ]
	addSegmentToPixelTable( begin, point, pixelTable, 0, 0, width )

def addSegmentToPixelTable( beginComplex, endComplex, pixelTable, shortenDistanceBegin, shortenDistanceEnd, width ):
	"Add line segment to the pixel table."
	if abs( beginComplex - endComplex ) <= 0.0:
		return
	beginComplex /= width
	endComplex /= width
	if shortenDistanceBegin > 0.0:
		endMinusBeginComplex = endComplex - beginComplex
		endMinusBeginComplexLength = abs( endMinusBeginComplex )
		if endMinusBeginComplexLength < shortenDistanceBegin:
			return
		beginComplex = beginComplex + endMinusBeginComplex * shortenDistanceBegin / endMinusBeginComplexLength
	if shortenDistanceEnd > 0.0:
		beginMinusEndComplex = beginComplex - endComplex
		beginMinusEndComplexLength = abs( beginMinusEndComplex )
		if beginMinusEndComplexLength < shortenDistanceEnd:
			return
		endComplex = endComplex + beginMinusEndComplex * shortenDistanceEnd / beginMinusEndComplexLength
	deltaX = endComplex.real - beginComplex.real
	deltaY = endComplex.imag - beginComplex.imag
	isSteep = abs( deltaY ) > abs( deltaX )
	if isSteep:
		beginComplex = complex( beginComplex.imag, beginComplex.real )
		endComplex = complex( endComplex.imag, endComplex.real )
	if beginComplex.real > endComplex.real:
		newBeginComplex = endComplex
		endComplex = beginComplex
		beginComplex = newBeginComplex
	deltaX = endComplex.real - beginComplex.real
	deltaY = endComplex.imag - beginComplex.imag
	gradient = deltaY / deltaX
	xEnd = int( round( beginComplex.real ) )
	yEnd = beginComplex.imag + gradient * ( xEnd - beginComplex.real )
	xGap = getReverseFloatPart( beginComplex.real + 0.5 )
	beginStep = xEnd
	addPixelToPixelTableWithSteepness( isSteep, pixelTable, xEnd, int( math.floor( yEnd ) ) )
	addPixelToPixelTableWithSteepness( isSteep, pixelTable, xEnd, int( math.floor( yEnd ) ) + 1 )
	intersectionY = yEnd + gradient
	xEnd = int( round( endComplex.real ) )
	yEnd = endComplex.imag + gradient * ( xEnd - endComplex.real )
	xGap = getReverseFloatPart( endComplex.real + 0.5 )
	addPixelToPixelTableWithSteepness( isSteep, pixelTable,  xEnd, int( math.floor( yEnd ) ) )
	addPixelToPixelTableWithSteepness( isSteep, pixelTable, xEnd, int( math.floor( yEnd ) ) + 1 )
	for x in xrange( beginStep + 1, xEnd ):
		addPixelToPixelTableWithSteepness( isSteep, pixelTable, x, int( math.floor( intersectionY ) ) )
		addPixelToPixelTableWithSteepness( isSteep, pixelTable, x, int( math.floor( intersectionY ) ) + 1 )
		intersectionY += gradient

def addSurroundingLoopBeginning( loop, skein, z ):
	"Add surrounding loop beginning to gcode output."
	skein.addLine( '(<surroundingLoop>)' )
	for point in loop:
		skein.addLine( '(<boundaryPoint> X%s Y%s Z%s </boundaryPoint>)' % ( skein.getRounded( point.real ), skein.getRounded( point.imag ), skein.getRounded( z ) ) )

def addToThreadsFromLoop( extrusionHalfWidth, gcodeType, loop, oldOrderedLocation, skein ):
	"Add to threads from the last location from loop."
	loop = getLoopStartingNearest( extrusionHalfWidth, oldOrderedLocation.dropAxis( 2 ), loop )
	oldOrderedLocation.x = loop[ 0 ].real
	oldOrderedLocation.y = loop[ 0 ].imag
	skein.addLine( gcodeType )
	skein.addGcodeFromThreadZ( loop + [ loop[ 0 ] ], oldOrderedLocation.z ) # Turn extruder on and indicate that a loop is beginning.
	skein.addLine( gcodeType.replace( '(<', '(</' ) )

def addToThreadsRemoveFromSurroundings( oldOrderedLocation, surroundingLoops, skein ):
	"Add to threads from the last location from surrounding loops."
	if len( surroundingLoops ) < 1:
		return
	while len( surroundingLoops ) > 0:
		getTransferClosestSurroundingLoop( oldOrderedLocation, surroundingLoops, skein )

def addXIntersectionIndexes( loop, solidIndex, xIntersectionIndexList, y ):
	"Add the x intersections for a loop."
	for pointIndex in xrange( len( loop ) ):
		pointFirst = loop[ pointIndex ]
		pointSecond = loop[ ( pointIndex + 1 ) % len( loop ) ]
		isYAboveFirst = y > pointFirst.imag
		isYAboveSecond = y > pointSecond.imag
		if isYAboveFirst != isYAboveSecond:
			xIntersection = getXIntersection( pointFirst, pointSecond, y )
			xIntersectionIndexList.append( XIntersectionIndex( solidIndex, xIntersection ) )

def addXIntersectionIndexesFromLoopLists( loopLists, xIntersectionIndexList, y ):
	"Add the x intersections for the loop lists."
	for loopListIndex in xrange( len( loopLists ) ):
		loopList = loopLists[ loopListIndex ]
		for loop in loopList:
			addXIntersectionIndexes( loop, loopListIndex, xIntersectionIndexList, y )

def addXIntersectionIndexesFromLoops( loops, solidIndex, xIntersectionIndexList, y ):
	"Add the x intersections for the loops."
	for loop in loops:
		addXIntersectionIndexes( loop, solidIndex, xIntersectionIndexList, y )

def compareSegmentLength( endpoint, otherEndpoint ):
	"Get comparison in order to sort endpoints in ascending order of segment length."
	if endpoint.segmentLength > otherEndpoint.segmentLength:
		return 1
	if endpoint.segmentLength < otherEndpoint.segmentLength:
		return - 1
	return 0

def getAngleAroundZAxisDifference( subtractFromVec3, subtractVec3 ):
	"""Get the angle around the Z axis difference between a pair of Vector3s.

	Keyword arguments:
	subtractFromVec3 -- Vector3 whose angle will be subtracted from
	subtractVec3 -- Vector3 whose angle will be subtracted"""
	subtractVectorMirror = complex( subtractVec3.x , - subtractVec3.y )
	differenceVector = getRoundZAxisByPlaneAngle( subtractVectorMirror, subtractFromVec3 )
	return math.atan2( differenceVector.y, differenceVector.x )

def getAroundLoop( begin, end, loop ):
	"Get an arc around a loop."
	aroundLoop = []
	if end <= begin:
		end += len( loop )
	for pointIndex in xrange( begin, end ):
		aroundLoop.append( loop[ pointIndex % len( loop ) ] )
	return aroundLoop

def getAwayPoints( pointComplexes, radius ):
	"Get a path with only the points that are far enough away from each other."
	away = []
	overlapDistance = 0.01 * radius
	pixelTable = {}
	width = 1.01 * radius
	for pointComplex in pointComplexes:
		pointComplexOverWidth = pointComplex / width
		x = int( round( pointComplexOverWidth.real ) )
		y = int( round( pointComplexOverWidth.imag ) )
		if not isCloseXYPlane( overlapDistance, pixelTable, pointComplex, x, y ):
			away.append( pointComplex )
			addElementToPixelList( pointComplex, pixelTable, x, y )
	return away

def getBackOfLoops( loops ):
	"Get the back of the loops."
	negativeFloat = - 999999999.75342341
	back = negativeFloat
	for loop in loops:
		for point in loop:
			back = max( back, point.imag )
	if back == negativeFloat:
		print( "This should never happen, there are no loops for getBackOfLoops in euclidean." )
	return back

def getClippedAtEndLoopPath( clip, loopPath ):
	"Get a clipped loop path."
	if clip <= 0.0:
		return loopPath
	loopPathLength = getPathLength( loopPath )
	clip = min( clip, 0.3 * loopPathLength )
	lastLength = 0.0
	pointIndex = 0
	totalLength = 0.0
	clippedLength = loopPathLength - clip
	while totalLength < clippedLength and pointIndex < len( loopPath ) - 1:
		firstPoint = loopPath[ pointIndex ]
		secondPoint  = loopPath[ pointIndex + 1 ]
		pointIndex += 1
		lastLength = totalLength
		totalLength += abs( firstPoint - secondPoint )
	remainingLength = clippedLength - lastLength
	clippedLoopPath = loopPath[ : pointIndex ]
	ultimateClippedPoint = loopPath[ pointIndex ]
	penultimateClippedPoint = clippedLoopPath[ - 1 ]
	segment = ultimateClippedPoint - penultimateClippedPoint
	segmentLength = abs( segment )
	if segmentLength <= 0.0:
		return clippedLoopPath
	newUltimatePoint = penultimateClippedPoint + segment * remainingLength / segmentLength
	return clippedLoopPath + [ newUltimatePoint ]

def getClippedLoopPath( clip, loopPath ):
	"Get a clipped loop path."
	if clip <= 0.0:
		return loopPath
	loopPathLength = getPathLength( loopPath )
	clip = min( clip, 0.3 * loopPathLength )
	lastLength = 0.0
	pointIndex = 0
	totalLength = 0.0
	while totalLength < clip and pointIndex < len( loopPath ) - 1:
		firstPoint = loopPath[ pointIndex ]
		secondPoint  = loopPath[ pointIndex + 1 ]
		pointIndex += 1
		lastLength = totalLength
		totalLength += abs( firstPoint - secondPoint )
	remainingLength = clip - lastLength
	clippedLoopPath = loopPath[ pointIndex : ]
	ultimateClippedPoint = clippedLoopPath[ 0 ]
	penultimateClippedPoint = loopPath[ pointIndex - 1 ]
	segment = ultimateClippedPoint - penultimateClippedPoint
	segmentLength = abs( segment )
	loopPath = clippedLoopPath
	if segmentLength > 0.0:
		newUltimatePoint = penultimateClippedPoint + segment * remainingLength / segmentLength
		loopPath = [ newUltimatePoint ] + loopPath
	return getClippedAtEndLoopPath( clip, loopPath )

def getCrossProduct( firstComplex, secondComplex ):
	"Get z component cross product of a pair of complexes."
	return firstComplex.real * secondComplex.imag - firstComplex.imag * secondComplex.real

def getDistanceToPlaneSegment( segmentBegin, segmentEnd, point ):
	"Get the distance squared from a point to the x & y components of a segment."
	segmentDifference = segmentEnd - segmentBegin
	pointMinusSegmentBegin = point - segmentBegin
	beginPlaneDot = getDotProduct( pointMinusSegmentBegin, segmentDifference )
	if beginPlaneDot <= 0.0:
		return abs( point - segmentBegin ) * abs( point - segmentBegin )
	differencePlaneDot = getDotProduct( segmentDifference, segmentDifference )
	if differencePlaneDot <= beginPlaneDot:
		return abs( point - segmentEnd ) * abs( point - segmentEnd )
	intercept = beginPlaneDot / differencePlaneDot
	interceptPerpendicular = segmentBegin + segmentDifference * intercept
	return abs( point - interceptPerpendicular ) * abs( point - interceptPerpendicular )

def getDotProduct( firstComplex, secondComplex ):
	"Get the dot product of a pair of complexes."
	return firstComplex.real * secondComplex.real + firstComplex.imag * secondComplex.imag

def getDotProductPlusOne( firstComplex, secondComplex ):
	"Get the dot product plus one of the x and y components of a pair of Vector3s."
	return 1.0 + getDotProduct( firstComplex, secondComplex )

def getFillOfSurroundings( surroundingLoops ):
	"Get extra fill loops of surrounding loops."
	fillSurroundings = []
	for surroundingLoop in surroundingLoops:
		fillSurroundings += surroundingLoop.getFillLoops()
	return fillSurroundings

def getFloatPart( number ):
	"Get the float part of the number."
	return number - math.floor( number )

def getFrontOfLoops( loops ):
	"Get the front of the loops."
	bigFloat = 999999999.196854654
	front = bigFloat
	for loop in loops:
		for point in loop:
			front = min( front, point.imag )
	if front == bigFloat:
		print( "This should never happen, there are no loops for getFrontOfLoops in euclidean." )
	return front

def getHalfSimplifiedLoop( loopComplex, radius, remainder ):
	"Get the loop with half of the points inside the channel removed."
	if len( loopComplex ) < 2:
		return loopComplex
	channelRadius = radius * .01
	simplified = []
	addIndex = 0
	if remainder == 1:
		addIndex = len( loopComplex ) - 1
	for pointIndex in xrange( len( loopComplex ) ):
		point = loopComplex[ pointIndex ]
		if pointIndex % 2 == remainder or pointIndex == addIndex:
			simplified.append( point )
		elif not isWithinChannel( channelRadius, pointIndex, loopComplex ):
			simplified.append( point )
	return simplified

def getHalfSimplifiedPath( path, radius, remainder ):
	"Get the path with half of the points inside the channel removed."
	if len( path ) < 2:
		return path
	channelRadius = radius * .01
	simplified = []
	addIndex = len( path ) - 1
	for pointIndex in xrange( len( path ) ):
		point = path[ pointIndex ]
		if pointIndex % 2 == remainder or pointIndex == 0 or pointIndex == addIndex:
			simplified.append( point )
		elif not isWithinChannel( channelRadius, pointIndex, path ):
			simplified.append( point )
	return simplified

def getInsidesAddToOutsides( loops, outsides ):
	"Add loops to either the insides or outsides."
	insides = []
	for loopIndex in xrange( len( loops ) ):
		loop = loops[ loopIndex ]
		if isInsideOtherLoops( loopIndex, loops ):
			insides.append( loop )
		else:
			outsides.append( loop )
	return insides

def getIntermediateLocation( alongWay, begin, end ):
	"Get the intermediate location between begin and end."
	return begin * ( 1.0 - alongWay ) + end * alongWay

def getLargestLoop( loops ):
	"Get largest loop from loops."
	largestArea = - 999999999.0
	largestLoop = None
	for loop in loops:
		loopArea = abs( getPolygonArea( loop ) )
		if loopArea > largestArea:
			largestArea = loopArea
			largestLoop = loop
	return largestLoop

def getLeftPoint( pointComplexes ):
	"Get the leftmost complex point in the points."
	leftmost = 999999999.0
	leftPointComplex = None
	for pointComplex in pointComplexes:
		if pointComplex.real < leftmost:
			leftmost = pointComplex.real
			leftPointComplex = pointComplex
	return leftPointComplex

def getLoopStartingNearest( extrusionHalfWidth, location, loop ):
	"Add to threads from the last location from loop."
	nearestIndex = getNearestDistanceIndex( location, loop ).index
	loop = getAroundLoop( nearestIndex, nearestIndex, loop )
	nearestPoint = getNearestPointOnSegment( loop[ 0 ], loop[ 1 ], location )
	if abs( nearestPoint - loop[ 0 ] ) > extrusionHalfWidth and abs( nearestPoint - loop[ 1 ] ) > extrusionHalfWidth:
		loop = [ nearestPoint ] + loop[ 1 : ] + [ loop[ 0 ] ]
	elif abs( nearestPoint - loop[ 0 ] ) > abs( nearestPoint - loop[ 1 ] ):
		loop = loop[ 1 : ] + [ loop[ 0 ] ]
	return loop

def getMaximum( firstComplex, secondComplex ):
	"Get a complex with each component the maximum of the respective components of a pair of complexes."
	return complex( max( firstComplex.real, secondComplex.real ), max( firstComplex.imag, secondComplex.imag ) )

def getMaximumFromPoints( pointComplexes ):
	"Get a complex with each component the maximum of the respective components of a list of complex points."
	maximum = complex( - 999999999.0, - 999999999.0 )
	for pointComplex in pointComplexes:
		maximum = getMaximum( maximum, pointComplex )
	return maximum

def getMaximumSpan( loop ):
	"Get the maximum span of the loop."
	extent = getMaximumFromPoints( loop ) - getMinimumFromPoints( loop )
	return max( extent.real, extent.imag )

def getMinimum( firstComplex, secondComplex ):
	"Get a complex with each component the minimum of the respective components of a pair of complexes."
	return complex( min( firstComplex.real, secondComplex.real ), min( firstComplex.imag, secondComplex.imag ) )

def getMinimumFromPoints( pointComplexes ):
	"Get a complex with each component the minimum of the respective components of a list of complex points."
	minimum = complex( 999999999.0, 999999999.0 )
	for pointComplex in pointComplexes:
		minimum = getMinimum( minimum, pointComplex )
	return minimum

def getMinimumFromVec3List( vec3List ):
	"Get a complex with each component the minimum of the respective components of a list of Vector3s."
	minimum = complex( 999999999.0, 999999999.0 )
	for point in vec3List:
		minimum = getMinimum( minimum, point.dropAxis( 2 ) )
	return minimum

def getNearestDistanceIndex( point, loop ):
	"Get the distance squared to the nearest segment of the loop and index of that segment."
	smallestDistance = 999999999999999999.0
	nearestDistanceIndex = None
	for pointIndex in xrange( len( loop ) ):
		segmentBegin = loop[ pointIndex ]
		segmentEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
		distance = getDistanceToPlaneSegment( segmentBegin, segmentEnd, point )
		if distance < smallestDistance:
			smallestDistance = distance
			nearestDistanceIndex = DistanceIndex( distance, pointIndex )
	return nearestDistanceIndex

def getNearestPointOnSegment( segmentBegin, segmentEnd, point ):
	"Get the nearest point on the segment."
	segmentDifference = segmentEnd - segmentBegin
	pointMinusSegmentBegin = point - segmentBegin
	beginPlaneDot = getDotProduct( pointMinusSegmentBegin, segmentDifference )
	differencePlaneDot = getDotProduct( segmentDifference, segmentDifference )
	intercept = beginPlaneDot / differencePlaneDot
	intercept = max( intercept, 0.0 )
	intercept = min( intercept, 1.0 )
	return segmentBegin + segmentDifference * intercept

def getNormalized( complexNumber ):
	"Get the normalized complex."
	complexNumberLength = abs( complexNumber )
	if complexNumberLength > 0.0:
		return complexNumber / complexNumberLength
	return complexNumber

def getNumberOfIntersectionsToLeft( leftPointComplex, loopComplex ):
	"Get the number of intersections through the loop for the line starting from the left point and going left."
	numberOfIntersectionsToLeft = 0
	for pointIndex in xrange( len( loopComplex ) ):
		firstPointComplex = loopComplex[ pointIndex ]
		secondPointComplex = loopComplex[ ( pointIndex + 1 ) % len( loopComplex ) ]
		isLeftAboveFirst = leftPointComplex.imag > firstPointComplex.imag
		isLeftAboveSecond = leftPointComplex.imag > secondPointComplex.imag
		if isLeftAboveFirst != isLeftAboveSecond:
			if getXIntersection( firstPointComplex, secondPointComplex, leftPointComplex.imag ) < leftPointComplex.real:
				numberOfIntersectionsToLeft += 1
	return numberOfIntersectionsToLeft

def getOrderedSurroundingLoops( extrusionWidth, surroundingLoops ):
	"Get ordered surrounding loops from surrounding loops."
	insides = []
	orderedSurroundingLoops = []
	for loopIndex in xrange( len( surroundingLoops ) ):
		surroundingLoop = surroundingLoops[ loopIndex ]
		otherLoops = []
		for beforeIndex in xrange( loopIndex ):
			otherLoops.append( surroundingLoops[ beforeIndex ].boundary )
		for afterIndex in xrange( loopIndex + 1, len( surroundingLoops ) ):
			otherLoops.append( surroundingLoops[ afterIndex ].boundary )
		if isPathInsideLoops( otherLoops, surroundingLoop.boundary ):
			insides.append( surroundingLoop )
		else:
			orderedSurroundingLoops.append( surroundingLoop )
	for outside in orderedSurroundingLoops:
		outside.getFromInsideSurroundings( extrusionWidth, insides )
	return orderedSurroundingLoops

def getPathLength( path ):
	"Get the length of a path ( an open polyline )."
	pathLength = 0.0
	for pointIndex in xrange( len( path ) - 1 ):
		firstPoint = path[ pointIndex ]
		secondPoint  = path[ pointIndex + 1 ]
		pathLength += abs( firstPoint - secondPoint )
	return pathLength

def getPathsFromEndpoints( endpoints, fillInset, pixelTable, width ):
	"Get paths from endpoints."
	for beginningEndpoint in endpoints[ : : 2 ]:
		beginningPoint = beginningEndpoint.point
		addSegmentToPixelTable( beginningPoint, beginningEndpoint.otherEndpoint.point, pixelTable, 0, 0, width )
	endpointFirst = endpoints[ 0 ]
	endpoints.remove( endpointFirst )
	otherEndpoint = endpointFirst.otherEndpoint
	endpoints.remove( otherEndpoint )
	nextEndpoint = None
	path = []
	paths = [ path ]
	if len( endpoints ) > 1:
		nextEndpoint = otherEndpoint.getNearestMiss( endpoints, path, pixelTable, width )
		if nextEndpoint != None:
			if abs( nextEndpoint.point - endpointFirst.point ) < abs( nextEndpoint.point - otherEndpoint.point ):
				endpointFirst = endpointFirst.otherEndpoint
				otherEndpoint = endpointFirst.otherEndpoint
	addPointToPath( path, pixelTable, endpointFirst.point, width )
	addPointToPath( path, pixelTable, otherEndpoint.point, width )
	while len( endpoints ) > 1:
		nextEndpoint = otherEndpoint.getNearestMiss( endpoints, path, pixelTable, width )
		if nextEndpoint == None:
			path = []
			paths.append( path )
			nextEndpoint = otherEndpoint.getNearestEndpoint( endpoints )
		addPointToPath( path, pixelTable, nextEndpoint.point, width )
		endpoints.remove( nextEndpoint )
		otherEndpoint = nextEndpoint.otherEndpoint
		hop = nextEndpoint.getHop( fillInset, path )
		if hop != None:
			path = [ hop ]
			paths.append( path )
		addPointToPath( path, pixelTable, otherEndpoint.point, width )
		endpoints.remove( otherEndpoint )
	return paths

def getPlaneDot( vec3First, vec3Second ):
	"Get the dot product of the x and y components of a pair of Vector3s."
	return vec3First.x * vec3Second.x + vec3First.y * vec3Second.y

def getPointsRoundZAxis( planeAngle, points ):
	"Get points rotated by the plane angle"
	planeArray = []
	for point in points:
		planeArray.append( planeAngle * point )
	return planeArray

def getPointMaximum( firstPoint, secondPoint ):
	"Get a point with each component the maximum of the respective components of a pair of Vector3s."
	return Vector3( max( firstPoint.x, secondPoint.x ), max( firstPoint.y, secondPoint.y ), max( firstPoint.z, secondPoint.z ) )

def getPointMinimum( firstPoint, secondPoint ):
	"Get a point with each component the minimum of the respective components of a pair of Vector3s."
	return Vector3( min( firstPoint.x, secondPoint.x ), min( firstPoint.y, secondPoint.y ), min( firstPoint.z, secondPoint.z ) )

def getPointPlusSegmentWithLength( length, point, segment ):
	"Get point plus a segment scaled to a given length."
	return segment * length / abs( segment ) + point

def getPolar( angle, radius = 1.0 ):
	"Get polar complex from counterclockwise angle from 1, 0 and radius."
	return complex( radius * math.cos( angle ), radius * math.sin( angle ) )

def getPolygonArea( polygonComplex ):
	"Get the area of a complex polygon."
	polygonComplexArea = 0.0
	for pointIndex in xrange( len( polygonComplex ) ):
		pointComplex = polygonComplex[ pointIndex ]
		secondPointComplex  = polygonComplex[ ( pointIndex + 1 ) % len( polygonComplex ) ]
		area  = pointComplex.real * secondPointComplex.imag - secondPointComplex.real * pointComplex.imag
		polygonComplexArea += area
	return 0.5 * polygonComplexArea

def getPolygonLength( polygon ):
	"Get the length of a polygon perimeter."
	polygonLength = 0.0
	for pointIndex in xrange( len( polygon ) ):
		point = polygon[ pointIndex ]
		secondPoint  = polygon[ ( pointIndex + 1 ) % len( polygon ) ]
		polygonLength += abs( point - secondPoint )
	return polygonLength

def getReverseFloatPart( number ):
	"Get the reverse float part of the number."
	return 1.0 - getFloatPart( number )

def getRotatedWiddershinsQuarterAroundZAxis( vector3 ):
	"Get Vector3 rotated a quarter widdershins turn around Z axis."
	return Vector3( - vector3.y, vector3.x, vector3.z )

def getRoundedPoint( point ):
	"Get point with each component rounded."
	return Vector3( round( point.x ), round( point.y ), round( point.z ) )

def getRoundedToDecimalPlaces( decimalPlaces, number ):
	"Get number rounded to a number of decimal places."
	decimalPlacesRounded = max( 1, int( round( decimalPlaces ) ) )
	return round( number, decimalPlacesRounded )

def getRoundedToDecimalPlacesString( decimalPlaces, number ):
	"Get number rounded to a number of decimal places as a string."
	return str( getRoundedToDecimalPlaces( decimalPlaces, number ) )

def getRoundedToThreePlaces( number ):
	"Get number rounded to three places as a string."
	return str( 0.001 * round( number * 1000.0 ) )

def getRoundZAxisByPlaneAngle( planeAngle, vector3 ):
	"""Get Vector3 rotated by a plane angle.

	Keyword arguments:
	planeAngle - plane angle of the rotation
	vector3 - Vector3 whose rotation will be returned"""
	return Vector3( vector3.x * planeAngle.real - vector3.y * planeAngle.imag, vector3.x * planeAngle.imag + vector3.y * planeAngle.real, vector3.z )

def getSegmentFromPoints( begin, end ):
	"Get endpoint segment from a pair of points."
	endpointFirst = Endpoint()
	endpointSecond = Endpoint().getFromOtherPoint( endpointFirst, end )
	endpointFirst.getFromOtherPoint( endpointSecond, begin )
	return ( endpointFirst, endpointSecond )

def getSegmentsFromXIntersections( xIntersections, y ):
	"Get endpoint segments from the x intersections."
	segments = []
	for xIntersectionIndex in xrange( 0, len( xIntersections ), 2 ):
		firstX = xIntersections[ xIntersectionIndex ]
		secondX = xIntersections[ xIntersectionIndex + 1 ]
		if firstX != secondX:
			segments.append( getSegmentFromPoints( complex( firstX, y ), complex( secondX, y ) ) )
	return segments

def getSegmentsFromXIntersectionIndexes( xIntersectionIndexList, y ):
	"Get endpoint segments from the x intersection indexes."
	xIntersections = getXIntersectionsFromIntersections( xIntersectionIndexList )
	return getSegmentsFromXIntersections( xIntersections, y )

def getSimplifiedLoop( loopComplex, radius ):
	"Get loop with points inside the channel removed."
	if len( loopComplex ) < 2:
		return loopComplex
	simplificationMultiplication = 256
	simplificationRadius = radius / float( simplificationMultiplication )
	maximumIndex = len( loopComplex ) * simplificationMultiplication
	pointIndex = 1
	while pointIndex < maximumIndex:
		loopComplex = getHalfSimplifiedLoop( loopComplex, simplificationRadius, 0 )
		loopComplex = getHalfSimplifiedLoop( loopComplex, simplificationRadius, 1 )
		simplificationRadius += simplificationRadius
		simplificationRadius = min( simplificationRadius, radius )
		pointIndex += pointIndex
	return getAwayPoints( loopComplex, radius )

def getSimplifiedPath( path, radius ):
	"Get path with points inside the channel removed."
	if len( path ) < 2:
		return path
	simplificationMultiplication = 256
	simplificationRadius = radius / float( simplificationMultiplication )
	maximumIndex = len( path ) * simplificationMultiplication
	pointIndex = 1
	while pointIndex < maximumIndex:
		path = getHalfSimplifiedPath( path, simplificationRadius, 0 )
		path = getHalfSimplifiedPath( path, simplificationRadius, 1 )
		simplificationRadius += simplificationRadius
		simplificationRadius = min( simplificationRadius, radius )
		pointIndex += pointIndex
	return getAwayPoints( path, radius )

def getSquareValues( pixelTable, x, y ):
	"Get a list of the values in a square around the x and y pixel coordinates."
	squareValues = []
	for xStep in xrange( x - 1, x + 2 ):
		for yStep in xrange( y - 1, y + 2 ):
			stepKey = getStepKey( xStep, yStep )
			if stepKey in pixelTable:
				squareValues += pixelTable[ stepKey ]
	return squareValues

def getStepKey( x, y ):
	"Get step key for x and y."
	return ( x, y )

def getTransferClosestSurroundingLoop( oldOrderedLocation, remainingSurroundingLoops, skein ):
	"Get and transfer the closest remaining surrounding loop."
	if len( remainingSurroundingLoops ) > 0:
		oldOrderedLocation.z = remainingSurroundingLoops[ 0 ].z
	closestDistance = 999999999999999999.0
	closestSurroundingLoop = None
	for remainingSurroundingLoop in remainingSurroundingLoops:
		distance = getNearestDistanceIndex( oldOrderedLocation.dropAxis( 2 ), remainingSurroundingLoop.boundary ).distance
		if distance < closestDistance:
			closestDistance = distance
			closestSurroundingLoop = remainingSurroundingLoop
	remainingSurroundingLoops.remove( closestSurroundingLoop )
	closestSurroundingLoop.addToThreads( oldOrderedLocation, skein )
	return closestSurroundingLoop

def getTransferredPaths( insides, loop ):
	"Get transferred paths from inside paths."
	transferredPaths = []
	for insideIndex in xrange( len( insides ) - 1, - 1, - 1 ):
		inside = insides[ insideIndex ]
		if isPathInsideLoop( loop, inside ):
			transferredPaths.append( inside )
			del insides[ insideIndex ]
	return transferredPaths

def getTransferredSurroundingLoops( insides, loop ):
	"Get transferred paths from inside surrounding loops."
	transferredSurroundings = []
	for insideIndex in xrange( len( insides ) - 1, - 1, - 1 ):
		insideSurrounding = insides[ insideIndex ]
		if isPathInsideLoop( loop, insideSurrounding.boundary ):
			transferredSurroundings.append( insideSurrounding )
			del insides[ insideIndex ]
	return transferredSurroundings

def getXIntersection( firstComplex, secondComplex, y ):
	"Get where the line crosses y."
	secondMinusFirstComplex = secondComplex - firstComplex
	yMinusFirst = y - firstComplex.imag
	return yMinusFirst / secondMinusFirstComplex.imag * secondMinusFirstComplex.real + firstComplex.real

def getXIntersectionsFromIntersections( xIntersectionIndexList ):
	"Get x intersections from the x intersection index list, in other words subtract non negative intersections from negatives."
	xIntersections = []
	fill = False
	solid = False
	solidTable = {}
	xIntersectionIndexList.sort()
	for solidX in xIntersectionIndexList:
		if solidX.index >= 0:
			toggleHashtable( solidTable, solidX.index, "" )
		else:
			fill = not fill
		oldSolid = solid
		solid = ( len( solidTable ) == 0 and fill )
		if oldSolid != solid:
			xIntersections.append( solidX.x )
	return xIntersections

def getXYComplexFromVector3( vector3 ):
	"Get an xy complex from a vector3 if it exists, otherwise return None."
	if vector3 == None:
		return None
	return vector3.dropAxis( 2 )

def getWiddershinsDotGiven( complexFirst, complexSecond ):
	"Get the magintude of the positive dot product plus one of the x and y components of a pair of complexes, with the reversed sign of the cross product."
	dot = getDotProductPlusOne( complexFirst, complexSecond )
	if getCrossProduct( complexFirst, complexSecond ) >= 0.0:
		return - dot
	return dot

def getZComponentCrossProduct( vec3First, vec3Second ):
	"Get z component cross product of a pair of Vector3s."
	return vec3First.x * vec3Second.y - vec3First.y * vec3Second.x

def isCloseXYPlane( overlapDistance, pixelTable, pointComplex, x, y ):
	"Determine if the point is close to another point on the loop."
	squareValues = getSquareValues( pixelTable, x, y )
	for squareValue in squareValues:
		if abs( squareValue - pointComplex ) < overlapDistance:
			return True
	return False

def isInsideOtherLoops( loopIndex, loops ):
	"Determine if a loop in a list is inside another loop in that list."
	return isPathInsideLoops( loops[ : loopIndex ] + loops[ loopIndex + 1 : ], loops[ loopIndex ] )

def isLargeSameDirection( inset, loop, requiredSize ):
	"Determine if the inset is in the same direction as the loop and if the inset is as large as the required size."
	if isWiddershins( inset ) != isWiddershins( loop ):
		return False
	return getMaximumSpan( inset ) > requiredSize

def isLineIntersectingInsideXSegment( segmentFirstX, segmentSecondX, vector3First, vector3Second, y ):
	"Determine if the line is crossing inside the x segment."
	isYAboveFirst = y > vector3First.imag
	isYAboveSecond = y > vector3Second.imag
	if isYAboveFirst == isYAboveSecond:
		return False
	xIntersection = getXIntersection( vector3First, vector3Second, y )
	if xIntersection <= min( segmentFirstX, segmentSecondX ):
		return False
	return xIntersection < max( segmentFirstX, segmentSecondX )

def isLineIntersectingLoops( loops, pointBegin, pointEnd ):
	"Determine if the line is intersecting loops."
	normalizedSegment = pointEnd - pointBegin
	normalizedSegmentLength = abs( normalizedSegment )
	if normalizedSegmentLength > 0.0:
		normalizedSegment /= normalizedSegmentLength
		segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
		pointBeginRotated = segmentYMirror * pointBegin
		pointEndRotated = segmentYMirror * pointEnd
		if isLoopListIntersectingInsideXSegment( loops, pointBeginRotated.real, pointEndRotated.real, segmentYMirror, pointBeginRotated.imag ):
			return True
	return False

def isLoopIntersectingInsideXSegment( loop, segmentFirstX, segmentSecondX, segmentYMirror, y ):
	"Determine if the loop is intersecting inside the x segment."
	rotatedLoop = getPointsRoundZAxis( segmentYMirror, loop )
	for pointIndex in xrange( len( rotatedLoop ) ):
		pointFirst = rotatedLoop[ pointIndex ]
		pointSecond = rotatedLoop[ ( pointIndex + 1 ) % len( rotatedLoop ) ]
		if isLineIntersectingInsideXSegment( segmentFirstX, segmentSecondX, pointFirst, pointSecond, y ):
			return True
	return False

def isLoopIntersectingLoops( loop, otherLoops ):
	"Determine if the loop is intersecting other loops."
	for pointIndex in xrange( len( loop ) ):
		pointBegin = loop[ pointIndex ]
		pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
		if isLineIntersectingLoops( otherLoops, pointBegin, pointEnd ):
			return True
	return False

def isLoopListIntersectingInsideXSegment( loopList, segmentFirstX, segmentSecondX, segmentYMirror, y ):
	"Determine if the loop list is crossing inside the x segment."
	for alreadyFilledLoop in loopList:
		if isLoopIntersectingInsideXSegment( alreadyFilledLoop, segmentFirstX, segmentSecondX, segmentYMirror, y ):
			return True
	return False

def isPathInsideLoop( loop, path ):
	"Determine if a path is inside another loop."
	leftPoint = getLeftPoint( path )
	return isPointInsideLoop( loop, leftPoint )

def isPathInsideLoops( loops, path ):
	"Determine if a path is inside another loop in a list."
	for loop in loops:
		if isPathInsideLoop( loop, path ):
			return True
	return False

def isPixelTableIntersecting( bigTable, littleTable, maskTable = {} ):
	"Add path to the pixel table."
	littleTableKeys = littleTable.keys()
	for littleTableKey in littleTableKeys:
		if littleTableKey not in maskTable:
			if littleTableKey in bigTable:
				return True
	return False

def isPointInsideLoop( loop, point ):
	"Determine if a point is inside another loop."
	return getNumberOfIntersectionsToLeft( point, loop ) % 2 == 1

def isPointInsideLoops( loops, point ):
	"Determine if a point is inside a loop list."
	for loop in loops:
		if isPointInsideLoop( loop, point ):
			return True
	return False

def isSegmentCompletelyInX( segment, xFirst, xSecond ):
	"Determine if the segment overlaps within x."
	segmentFirstX = segment[ 0 ].point.real
	segmentSecondX = segment[ 1 ].point.real
	if max( segmentFirstX, segmentSecondX ) > max( xFirst, xSecond ):
		return False
	return min( segmentFirstX, segmentSecondX ) >= min( xFirst, xSecond )

def isWiddershins( polygonComplex ):
	"Determine if the complex polygon goes round in the widdershins direction."
	return getPolygonArea( polygonComplex ) > 0.0

def isWithinChannel( channelRadius, pointIndex, loopComplex ):
	"Determine if the the point is within the channel between two adjacent points."
	pointComplex = loopComplex[ pointIndex ]
	behindSegmentComplex = loopComplex[ ( pointIndex + len( loopComplex ) - 1 ) % len( loopComplex ) ] - pointComplex
	behindSegmentComplexLength = abs( behindSegmentComplex )
	if behindSegmentComplexLength < channelRadius:
		return True
	aheadSegmentComplex = loopComplex[ ( pointIndex + 1 ) % len( loopComplex ) ] - pointComplex
	aheadSegmentComplexLength = abs( aheadSegmentComplex )
	if aheadSegmentComplexLength < channelRadius:
		return True
	behindSegmentComplex /= behindSegmentComplexLength
	aheadSegmentComplex /= aheadSegmentComplexLength
	absoluteZ = getDotProductPlusOne( aheadSegmentComplex, behindSegmentComplex )
	if behindSegmentComplexLength * absoluteZ < channelRadius:
		return True
	if aheadSegmentComplexLength * absoluteZ < channelRadius:
		return True
	return False

def isXSegmentIntersectingPaths( paths, segmentFirstX, segmentSecondX, segmentYMirror, y ):
	"Determine if a path list is crossing inside the x segment."
	for path in paths:
		rotatedPath = getPointsRoundZAxis( segmentYMirror, path )
		for pointIndex in xrange( len( rotatedPath ) - 1 ):
			pointFirst = rotatedPath[ pointIndex ]
			pointSecond = rotatedPath[ pointIndex + 1 ]
			if isLineIntersectingInsideXSegment( segmentFirstX, segmentSecondX, pointFirst, pointSecond, y ):
				return True
	return False

def removePixelTableFromPixelTable( pixelTableToBeRemoved, pixelTableToBeRemovedFrom ):
	"Remove pixel from the pixel table."
	pixelTableToBeRemovedKeys = pixelTableToBeRemoved.keys()
	for pixelTableToBeRemovedKey in pixelTableToBeRemovedKeys:
		if pixelTableToBeRemovedKey in pixelTableToBeRemovedFrom:
			del pixelTableToBeRemovedFrom[ pixelTableToBeRemovedKey ]

def toggleHashtable( hashtable, key, value ):
	"Toggle a hashtable between having and not having a key."
	if key in hashtable:
		del hashtable[ key ]
	else:
		hashtable[ key ] = value

def transferClosestFillLoop( extrusionHalfWidth, oldOrderedLocation, remainingFillLoops, skein ):
	"Transfer the closest remaining fill loop."
	closestDistance = 999999999999999999.0
	closestFillLoop = None
	for remainingFillLoop in remainingFillLoops:
		distance = getNearestDistanceIndex( oldOrderedLocation.dropAxis( 2 ), remainingFillLoop ).distance
		if distance < closestDistance:
			closestDistance = distance
			closestFillLoop = remainingFillLoop
	remainingFillLoops.remove( closestFillLoop )
	addToThreadsFromLoop( extrusionHalfWidth, '(<loop>)', closestFillLoop[ : ], oldOrderedLocation, skein )

def transferClosestPath( oldOrderedLocation, remainingPaths, skein ):
	"Transfer the closest remaining path."
	closestDistance = 999999999999999999.0
	closestPath = None
	oldOrderedLocationComplex = oldOrderedLocation.dropAxis( 2 )
	for remainingPath in remainingPaths:
		distance = min( abs( oldOrderedLocationComplex - remainingPath[ 0 ] ), abs( oldOrderedLocationComplex - remainingPath[ - 1 ] ) )
		if distance < closestDistance:
			closestDistance = distance
			closestPath = remainingPath
	remainingPaths.remove( closestPath )
	skein.addGcodeFromThreadZ( closestPath, oldOrderedLocation.z )
	oldOrderedLocation.x = closestPath[ - 1 ].real
	oldOrderedLocation.y = closestPath[ - 1 ].imag

def transferClosestPaths( oldOrderedLocation, remainingPaths, skein ):
	"Transfer the closest remaining paths."
	while len( remainingPaths ) > 0:
		transferClosestPath( oldOrderedLocation, remainingPaths, skein )

def transferPathsToSurroundingLoops( paths, surroundingLoops ):
	"Transfer paths to surrounding loops."
	for surroundingLoop in surroundingLoops:
		surroundingLoop.transferPaths( paths )


class DistanceIndex:
	"A class to hold the distance and the index of the loop."
	def __init__( self, distance, index ):
		self.distance = distance
		self.index = index

	def __repr__( self ):
		"Get the string representation of this distance index."
		return '%s, %s' % ( self.distance, self.index )


class Endpoint:
	"The endpoint of a segment."
	def __repr__( self ):
		"Get the string representation of this Endpoint."
		return 'Endpoint %s, %s' % ( self.point, self.otherEndpoint.point )

	def getFromOtherPoint( self, otherEndpoint, point ):
		"Initialize from other endpoint."
		self.otherEndpoint = otherEndpoint
		self.point = point
		return self

	def getHop( self, fillInset, path ):
		"Get a hop away from the endpoint if the other endpoint is doubling back."
		if len( path ) < 2:
			return None
		penultimateMinusPoint = path[ - 2 ] - self.point
		if abs( penultimateMinusPoint ) == 0.0:
			return None
		penultimateMinusPoint /= abs( penultimateMinusPoint )
		normalizedComplexSegment = self.otherEndpoint.point - self.point
		normalizedComplexSegmentLength = abs( normalizedComplexSegment )
		if normalizedComplexSegmentLength == 0.0:
			return None
		normalizedComplexSegment /= normalizedComplexSegmentLength
		if getDotProduct( penultimateMinusPoint, normalizedComplexSegment ) < 0.9:
			return None
		alongRatio = 0.8
		hop = self.point * alongRatio + self.otherEndpoint.point * ( 1.0 - alongRatio )
		normalizedSegment = self.otherEndpoint.point - self.point
		normalizedSegmentLength = abs( normalizedSegment )
		absoluteCross = abs( getCrossProduct( penultimateMinusPoint, normalizedComplexSegment ) )
		reciprocalCross = 1.0 / max( absoluteCross, 0.01 )
		alongWay = min( fillInset * reciprocalCross, normalizedSegmentLength )
		return self.point + normalizedSegment * alongWay / normalizedSegmentLength

	def getNearestEndpoint( self, endpoints ):
		"Get nearest endpoint."
		smallestDistance = 999999999999999999.0
		nearestEndpoint = None
		for endpoint in endpoints:
			distance = abs( self.point - endpoint.point )
			if distance < smallestDistance:
				smallestDistance = distance
				nearestEndpoint = endpoint
		return nearestEndpoint

	def getNearestMiss( self, endpoints, path, pixelTable, width ):
		"Get the nearest endpoint which the segment to that endpoint misses the other extrusions."
		smallestDistance = 9999999999.0
		nearestMiss = None
		penultimateMinusPoint = complex( 0.0, 0.0 )
		if len( path ) > 1:
			penultimateMinusPoint = path[ - 2 ] - self.point
			if abs( penultimateMinusPoint ) > 0.0:
				penultimateMinusPoint /= abs( penultimateMinusPoint )
		for endpoint in endpoints:
			segment = endpoint.point - self.point
			segmentLength = abs( segment )
			if segmentLength > 0.0:
				endpoint.segment = segment
				endpoint.segmentLength = segmentLength
			else:
				print( 'This should never happen, the endpoints are touching' )
				print( endpoint )
				print( path )
				return
		endpoints.sort( compareSegmentLength )
		for endpoint in endpoints:
			if getDotProduct( penultimateMinusPoint, endpoint.segment / endpoint.segmentLength ) < 0.9:
				segmentTable = {}
				addSegmentToPixelTable( endpoint.point, self.point, segmentTable, 4, 4, width )
				if not isPixelTableIntersecting( pixelTable, segmentTable ):
					return endpoint
		return nearestMiss


class LoopLayer:
	"Loops with a z."
	def __init__( self, z ):
		self.loops = []
		self.z = z

	def __repr__( self ):
		"Get the string representation of this loop layer."
		return '%s, %s' % ( self.loops, self.z )


class PathZ:
	"Complex path with a z."
	def __init__( self, z ):
		self.path = []
		self.z = z

	def __repr__( self ):
		"Get the string representation of this path z."
		return '%s, %s' % ( self.path, self.z )


class RotatedLoopLayer:
	"A rotated layer."
	def __init__( self, z ):
		self.loops = []
		self.rotation = None
		self.z = z

	def __repr__( self ):
		"Get the string representation of this rotated loop layer."
		return '%s, %s, %s' % ( self.z, self.rotation, self.loops )


class SurroundingLoop:
	"A loop that surrounds paths."
	def __init__( self, isOutsideExtrudedFirst = True ):
		self.boundary = []
		self.extraLoops = []
		self.innerSurroundings = None
		self.isOutsideExtrudedFirst = isOutsideExtrudedFirst
		self.lastFillLoops = None
		self.loop = None
		self.paths = []
		self.perimeterPaths = []
		self.z = None

	def __repr__( self ):
		"Get the string representation of this surrounding loop."
		return '%s, %s, %s, %s' % ( self.boundary, self.innerSurroundings, self.paths, self.perimeterPaths )

	def addToBoundary( self, vector3 ):
		"Add vector3 to boundary."
		self.boundary.append( vector3.dropAxis( 2 ) )
		self.z = vector3.z

	def addToLoop( self, vector3 ):
		"Add vector3 to loop."
		if self.loop == None:
			self.loop = []
		self.loop.append( vector3.dropAxis( 2 ) )
		self.z = vector3.z

	def addToThreads( self, oldOrderedLocation, skein ):
		"Add to paths from the last location."
		addSurroundingLoopBeginning( self.boundary, skein, self.z )
		if not self.isOutsideExtrudedFirst:
			self.transferClosestFillLoops( oldOrderedLocation, skein )
		if self.loop == None:
			transferClosestPaths( oldOrderedLocation, self.perimeterPaths[ : ], skein )
		else:
			addToThreadsFromLoop( self.extrusionHalfWidth, '(<perimeter>)', self.loop[ : ], oldOrderedLocation, skein )#later when comb is updated replace perimeter with loop
		skein.addLine( '(</surroundingLoop>)' )
		addToThreadsRemoveFromSurroundings( oldOrderedLocation, self.innerSurroundings[ : ], skein )
		if self.isOutsideExtrudedFirst:
			self.transferClosestFillLoops( oldOrderedLocation, skein )
		transferClosestPaths( oldOrderedLocation, self.paths[ : ], skein )

	def getFillLoops( self ):
		"Get last fill loops from the outside loop and the loops inside the inside loops."
		fillLoops = self.getLoopsToBeFilled()[ : ]
		for surroundingLoop in self.innerSurroundings:
			fillLoops += getFillOfSurroundings( surroundingLoop.innerSurroundings )
		return fillLoops

	def getFromInsideSurroundings( self, extrusionWidth, inputSurroundingInsides ):
		"Initialize from inside surrounding loops."
		self.extrusionHalfWidth = 0.5 * extrusionWidth
		self.extrusionWidth = extrusionWidth
		transferredSurroundings = getTransferredSurroundingLoops( inputSurroundingInsides, self.boundary )
		self.innerSurroundings = getOrderedSurroundingLoops( extrusionWidth, transferredSurroundings )
		return self

	def getLoopsToBeFilled( self ):
		"Get last fill loops from the outside loop and the loops inside the inside loops."
		if self.lastFillLoops != None:
			return self.lastFillLoops
		loopsToBeFilled = [ self.boundary ]
		for surroundingLoop in self.innerSurroundings:
			loopsToBeFilled.append( surroundingLoop.boundary )
		return loopsToBeFilled

	def transferClosestFillLoops( self, oldOrderedLocation, skein ):
		"Transfer closest fill loops."
		if len( self.extraLoops ) < 1:
			return
		remainingFillLoops = self.extraLoops[ : ]
		while len( remainingFillLoops ) > 0:
			transferClosestFillLoop( self.extrusionHalfWidth, oldOrderedLocation, remainingFillLoops, skein )

	def transferPaths( self, paths ):
		"Transfer paths."
		for surroundingLoop in self.innerSurroundings:
			transferPathsToSurroundingLoops( paths, surroundingLoop.innerSurroundings )
		self.paths = getTransferredPaths( paths, self.boundary )


class XIntersectionIndex:
	"A class to hold the x intersection position and the index of the loop which intersected."
	def __init__( self, index, x ):
		self.index = index
		self.x = x

	def __cmp__( self, other ):
		"Get comparison in order to sort x intersections in ascending order of x."
		if self.x > other.x:
			return 1
		if self.x < other.x:
			return - 1
		return 0

	def __repr__( self ):
		"Get the string representation of this x intersection."
		return '%s, %s' % ( self.index, self.x )
