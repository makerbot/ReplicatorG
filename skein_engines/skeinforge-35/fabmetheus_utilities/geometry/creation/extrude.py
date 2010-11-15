"""
Boolean geometry extrusion.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.creation import solid
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities.vector3index import Vector3Index
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def addLoop(derivation, endMultiplier, loopLists, path, portionDirectionIndex, portionDirections, vertexes):
	"Add an indexed loop to the vertexes."
	portionDirection = portionDirections[ portionDirectionIndex ]
	if portionDirection.directionReversed == True:
		loopLists.append( [] )
	loops = loopLists[-1]
	interpolationOffset = derivation.interpolationDictionary['offset']
	offset = interpolationOffset.getVector3ByPortion( portionDirection )
	if endMultiplier != None:
		if portionDirectionIndex == 0:
			setOffsetByMultiplier( interpolationOffset.path[1], interpolationOffset.path[0], endMultiplier, offset )
		elif portionDirectionIndex == len( portionDirections ) - 1:
			setOffsetByMultiplier( interpolationOffset.path[ - 2 ], interpolationOffset.path[-1], endMultiplier, offset )
	scale = derivation.interpolationDictionary['scale'].getComplexByPortion( portionDirection )
	twist = derivation.interpolationDictionary['twist'].getYByPortion( portionDirection )
	projectiveSpace = euclidean.ProjectiveSpace()
	if derivation.tiltTop == None:
		tilt = derivation.interpolationDictionary['tilt'].getComplexByPortion( portionDirection )
		projectiveSpace = projectiveSpace.getByTilt( tilt )
	else:
		normals = getNormals( interpolationOffset, offset, portionDirection )
		normalFirst = normals[0]
		normalAverage = getNormalAverage(normals)
		if derivation.tiltFollow and derivation.oldProjectiveSpace != None:
			projectiveSpace = derivation.oldProjectiveSpace.getNextSpace( normalAverage )
		else:
			projectiveSpace = projectiveSpace.getByBasisZTop( normalAverage, derivation.tiltTop )
		derivation.oldProjectiveSpace = projectiveSpace
		projectiveSpace.unbuckle( derivation.maximumUnbuckling, normalFirst )
	projectiveSpace = projectiveSpace.getSpaceByXYScaleAngle( twist, scale )
	loop = []
	if ( abs( projectiveSpace.basisX ) + abs( projectiveSpace.basisY ) ) < 0.0001:
		vector3Index = Vector3Index(len(vertexes))
		addOffsetAddToLists( loop, offset, vector3Index, vertexes )
		loops.append(loop)
		return
	for point in path:
		vector3Index = Vector3Index(len(vertexes))
		projectedVertex = projectiveSpace.getVector3ByPoint(point)
		vector3Index.setToVector3( projectedVertex )
		addOffsetAddToLists( loop, offset, vector3Index, vertexes )
	loops.append(loop)

def addNegatives(derivation, negatives, paths):
	"Add pillars output to negatives."
	portionDirections = getSpacedPortionDirections(derivation.interpolationDictionary)
	for path in paths:
		endMultiplier = 1.000001
		loopLists = getLoopListsByPath(derivation, endMultiplier, path, portionDirections)
		geometryOutput = trianglemesh.getPillarsOutput(loopLists)
		negatives.append(geometryOutput)

def addNegativesPositives(derivation, negatives, paths, positives):
	"Add pillars output to negatives and positives."
	portionDirections = getSpacedPortionDirections(derivation.interpolationDictionary)
	for path in paths:
		endMultiplier = None
		if not euclidean.getIsWiddershinsByVector3(path):
			endMultiplier = 1.000001
		loopLists = getLoopListsByPath(derivation, endMultiplier, path, portionDirections)
		geometryOutput = trianglemesh.getPillarsOutput(loopLists)
		if endMultiplier == None:
			positives.append(geometryOutput)
		else:
			negatives.append(geometryOutput)

def addOffsetAddToLists( loop, offset, vector3Index, vertexes ):
	"Add an indexed loop to the vertexes."
	vector3Index += offset
	loop.append( vector3Index )
	vertexes.append( vector3Index )

def addSpacedPortionDirection( portionDirection, spacedPortionDirections ):
	"Add spaced portion directions."
	lastSpacedPortionDirection = spacedPortionDirections[-1]
	if portionDirection.portion - lastSpacedPortionDirection.portion > 0.003:
		spacedPortionDirections.append( portionDirection )
		return
	if portionDirection.directionReversed > lastSpacedPortionDirection.directionReversed:
		spacedPortionDirections.append( portionDirection )

def addTwistPortions( interpolationTwist, remainderPortionDirection, twistPrecision ):
	"Add twist portions."
	lastPortionDirection = interpolationTwist.portionDirections[-1]
	if remainderPortionDirection.portion == lastPortionDirection.portion:
		return
	lastTwist = interpolationTwist.getYByPortion( lastPortionDirection )
	remainderTwist = interpolationTwist.getYByPortion( remainderPortionDirection )
	twistSegments = int( math.floor( abs( remainderTwist - lastTwist ) / twistPrecision ) )
	if twistSegments < 1:
		return
	portionDifference = remainderPortionDirection.portion - lastPortionDirection.portion
	twistSegmentsPlusOne = float( twistSegments + 1 )
	for twistSegment in xrange( twistSegments ):
		additionalPortion = portionDifference * float( twistSegment + 1 ) / twistSegmentsPlusOne
		portionDirection = PortionDirection( lastPortionDirection.portion + additionalPortion )
		interpolationTwist.portionDirections.append( portionDirection )

def comparePortionDirection( portionDirection, otherPortionDirection ):
	"Comparison in order to sort portion directions in ascending order of portion then direction."
	if portionDirection.portion > otherPortionDirection.portion:
		return 1
	if portionDirection.portion < otherPortionDirection.portion:
		return - 1
	if portionDirection.directionReversed < otherPortionDirection.directionReversed:
		return - 1
	return portionDirection.directionReversed > otherPortionDirection.directionReversed

def getGeometryOutput(derivation, xmlElement):
	"Get triangle mesh from attribute dictionary."
	if derivation == None:
		derivation = ExtrudeDerivation()
		derivation.setToXMLElement(xmlElement)
	if derivation.radius != complex():
		maximumRadius = max(derivation.radius.real, derivation.radius.imag)
		sides = int(math.ceil(evaluate.getSidesMinimumThreeBasedOnPrecisionSides(maximumRadius, xmlElement)))
		loop = []
		sideAngle = 2.0 * math.pi / sides
		angleTotal = 0.0
		for side in xrange(sides):
			point = euclidean.getWiddershinsUnitPolar(angleTotal)
			loop.append(Vector3(point.real * derivation.radius.real, point.imag * derivation.radius.imag))
			angleTotal += sideAngle
		derivation.target = [loop] + derivation.target
	if len(euclidean.getConcatenatedList(derivation.target)) == 0:
		print('Warning, in extrude there are no paths.')
		print(xmlElement.attributeDictionary)
		return None
	negatives = []
	positives = []
	addNegativesPositives(derivation, negatives, derivation.target, positives)
	return getGeometryOutputByNegativesPositives(derivation, negatives, positives, xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get triangle mesh from attribute dictionary by arguments."
	return getGeometryOutput(None, xmlElement)

def getGeometryOutputByConnection(connectionEnd, connectionStart, geometryOutput, xmlElement):
	"Get solid output by connection."
	firstValue = geometryOutput.values()[0]
	firstValue['connectionStart'] = connectionStart
	firstValue['connectionEnd'] = connectionEnd
	return solid.getGeometryOutputByManipulation(geometryOutput, xmlElement)

def getGeometryOutputByNegativesPositives(derivation, negatives, positives, xmlElement):
	"Get triangle mesh from derivation, negatives, positives and xmlElement."
	interpolationOffset = derivation.interpolationDictionary['offset']
	positiveOutput = trianglemesh.getUnifiedOutput(positives)
	if len(negatives) < 1:
		return getGeometryOutputByOffset(positiveOutput, interpolationOffset, xmlElement)
	if len(positives) < 1:
		negativeOutput = trianglemesh.getUnifiedOutput(negatives)
		return getGeometryOutputByOffset(negativeOutput, interpolationOffset, xmlElement)
	return getGeometryOutputByOffset({'difference' : {'shapes' : [positiveOutput] + negatives}}, interpolationOffset, xmlElement)

def getGeometryOutputByOffset( geometryOutput, interpolationOffset, xmlElement ):
	"Get solid output by interpolationOffset."
	geometryOutputValues = geometryOutput.values()
	if len(geometryOutputValues) < 1:
		return geometryOutput
	connectionStart = interpolationOffset.getVector3ByPortion(PortionDirection(0.0))
	connectionEnd = interpolationOffset.getVector3ByPortion(PortionDirection(1.0))
	return getGeometryOutputByConnection(connectionEnd, connectionStart, geometryOutput, xmlElement)

def getLoopListsByPath(derivation, endMultiplier, path, portionDirections):
	"Get loop lists from path."
	vertexes = []
	loopLists = [[]]
	derivation.oldProjectiveSpace = None
	for portionDirectionIndex in xrange(len(portionDirections)):
		addLoop(derivation, endMultiplier, loopLists, path, portionDirectionIndex, portionDirections, vertexes)
	return loopLists

def getNormalAverage(normals):
	"Get normal."
	if len(normals) < 2:
		return normals[0]
	return (normals[0] + normals[1]).getNormalized()

def getNormals( interpolationOffset, offset, portionDirection ):
	"Get normals."
	normals = []
	portionFrom = portionDirection.portion - 0.0001
	portionTo = portionDirection.portion + 0.0001
	if portionFrom >= 0.0:
		normals.append( ( offset - interpolationOffset.getVector3ByPortion( PortionDirection( portionFrom ) ) ).getNormalized() )
	if portionTo <= 1.0:
		normals.append( ( interpolationOffset.getVector3ByPortion( PortionDirection( portionTo ) ) - offset ).getNormalized() )
	return normals

def getSpacedPortionDirections( interpolationDictionary ):
	"Get sorted portion directions."
	portionDirections = []
	for interpolationDictionaryValue in interpolationDictionary.values():
		portionDirections += interpolationDictionaryValue.portionDirections
	portionDirections.sort( comparePortionDirection )
	if len( portionDirections ) < 1:
		return []
	spacedPortionDirections = [ portionDirections[0] ]
	for portionDirection in portionDirections[1 :]:
		addSpacedPortionDirection( portionDirection, spacedPortionDirections )
	return spacedPortionDirections

def insertTwistPortions(derivation, xmlElement):
	"Insert twist portions and radian the twist."
	interpolationDictionary = derivation.interpolationDictionary
	interpolationTwist = Interpolation().getByPrefixX(derivation.twistPathDefault, 'twist', xmlElement)
	interpolationDictionary['twist'] = interpolationTwist
	for point in interpolationTwist.path:
		point.y = math.radians(point.y)
	remainderPortionDirections = interpolationTwist.portionDirections[1 :]
	interpolationTwist.portionDirections = [interpolationTwist.portionDirections[0]]
	twistPrecision = 5.0
	if xmlElement != None:
		twistPrecision = math.radians(xmlElement.getCascadeFloat(twistPrecision, 'twistprecision'))
	for remainderPortionDirection in remainderPortionDirections:
		addTwistPortions(interpolationTwist, remainderPortionDirection, twistPrecision)
		interpolationTwist.portionDirections.append(remainderPortionDirection)

def processXMLElement(xmlElement):
	"Process the xml element."
	solid.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)

def setOffsetByMultiplier( begin, end, multiplier, offset ):
	"Set the offset by the multiplier."
	segment = end - begin
	delta = segment * multiplier - segment
	offset.setToVector3( offset + delta )


class ExtrudeDerivation:
	"Class to hold extrude variables."
	def __init__(self):
		'Set defaults.'
		self.maximumUnbuckling = 5.0
		self.interpolationDictionary = {}
		self.offsetAlongDefault = [Vector3(), Vector3(1.0, 0.0, 0.0)]
		self.offsetPathDefault = [Vector3(), Vector3(0.0, 0.0, 1.0)]
		self.radius = complex()
		self.scalePathDefault = [Vector3(1.0, 1.0, 0.0), Vector3(1.0, 1.0, 1.0)]
		self.target = []
		self.tiltFollow = True
		self.tiltPathDefault = [Vector3(), Vector3(0.0, 0.0, 1.0)]
		self.tiltTop = None
		self.twist = 0.0
		self.twistPathDefault = [Vector3(), Vector3(1.0)]

	def __repr__(self):
		"Get the string representation of this ExtrudeDerivation."
		return '%s, %s' % ( self.interpolationDictionary, self.tiltTop )

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.radius = lineation.getRadiusComplex(self.radius, xmlElement)
		self.tiltFollow = evaluate.getEvaluatedBooleanDefault(self.tiltFollow, 'tiltfollow', xmlElement)
		self.tiltTop = evaluate.getVector3ByPrefix(self.tiltTop, 'tilttop', xmlElement)
		self.maximumUnbuckling = evaluate.getEvaluatedFloatDefault(self.maximumUnbuckling, 'maximumUnbuckling', xmlElement)
		self.interpolationDictionary['scale'] = Interpolation().getByPrefixZ(self.scalePathDefault, 'scale', xmlElement)
		if len(self.target) < 1:
			self.target = evaluate.getTransformedPathsByKey('target', xmlElement)
		if self.tiltTop == None:
			self.interpolationDictionary['offset'] = Interpolation().getByPrefixZ(self.offsetPathDefault, '', xmlElement)
			self.interpolationDictionary['tilt'] = Interpolation().getByPrefixZ(self.tiltPathDefault, 'tilt', xmlElement)
			for point in self.interpolationDictionary['tilt'].path:
				point.x = math.radians(point.x)
				point.y = math.radians(point.y)
		else:
			self.interpolationDictionary['offset'] = Interpolation().getByPrefixAlong(self.offsetAlongDefault, '', xmlElement)
		self.twist = evaluate.getEvaluatedFloatDefault(self.twist, 'twist', xmlElement )
		if self.twist != 0.0:
			self.twistPathDefault = [Vector3(), Vector3(1.0, self.twist) ]
		insertTwistPortions(self, xmlElement)


class Interpolation:
	"Class to interpolate a path."
	def __init__(self):
		"Set index."
		self.interpolationIndex = 0

	def getByDistances(self):
		"Get by distances."
		beginDistance = self.distances[0]
		self.interpolationLength = self.distances[-1] - beginDistance
		self.close = abs( 0.000001 * self.interpolationLength )
		self.portionDirections = []
		oldDistance = beginDistance - self.interpolationLength
		for distance in self.distances:
			portionDirection = PortionDirection( distance / self.interpolationLength )
			if abs( distance - oldDistance ) < self.close:
				portionDirection.directionReversed = True
			self.portionDirections.append( portionDirection )
			oldDistance = distance
		return self

	def getByPrefixAlong(self, path, prefix, xmlElement):
		"Get interpolation from prefix and xml element along the path."
		if len(path) < 2:
			print('Warning, path is too small in evaluate in Interpolation.')
			return
		if xmlElement == None:
			self.path = path
		else:
			self.path = evaluate.getTransformedPathByPrefix(path, prefix, xmlElement)
		self.distances = [0.0]
		previousPoint = self.path[0]
		for point in self.path[1 :]:
			distanceDifference = abs(point - previousPoint)
			self.distances.append(self.distances[-1] + distanceDifference)
			previousPoint = point
		return self.getByDistances()

	def getByPrefixX(self, path, prefix, xmlElement):
		"Get interpolation from prefix and xml element in the z direction."
		if len(path) < 2:
			print('Warning, path is too small in evaluate in Interpolation.')
			return
		if xmlElement == None:
			self.path = path
		else:
			self.path = evaluate.getTransformedPathByPrefix(path, prefix, xmlElement)
		self.distances = []
		for point in self.path:
			self.distances.append(point.x)
		return self.getByDistances()

	def getByPrefixZ(self, path, prefix, xmlElement):
		"Get interpolation from prefix and xml element in the z direction."
		if len(path) < 2:
			print('Warning, path is too small in evaluate in Interpolation.')
			return
		if xmlElement == None:
			self.path = path
		else:
			self.path = evaluate.getTransformedPathByPrefix(path, prefix, xmlElement)
		self.distances = []
		for point in self.path:
			self.distances.append(point.z)
		return self.getByDistances()

	def getComparison( self, first, second ):
		"Compare the first with the second."
		if abs( second - first ) < self.close:
			return 0
		if second > first:
			return 1
		return - 1

	def getComplexByPortion( self, portionDirection ):
		"Get complex from z portion."
		self.setInterpolationIndexFromTo( portionDirection )
		return self.oneMinusInnerPortion * self.startVertex.dropAxis(2) + self.innerPortion * self.endVertex.dropAxis(2)

	def getInnerPortion(self):
		"Get inner x portion."
		fromDistance = self.distances[ self.interpolationIndex ]
		innerLength = self.distances[ self.interpolationIndex + 1 ] - fromDistance
		if abs( innerLength ) == 0.0:
			return 0.0
		return ( self.absolutePortion - fromDistance ) / innerLength

	def getVector3ByPortion( self, portionDirection ):
		"Get vector3 from z portion."
		self.setInterpolationIndexFromTo( portionDirection )
		return self.oneMinusInnerPortion * self.startVertex + self.innerPortion * self.endVertex

	def getYByPortion( self, portionDirection ):
		"Get y from x portion."
		self.setInterpolationIndexFromTo( portionDirection )
		return self.oneMinusInnerPortion * self.startVertex.y + self.innerPortion * self.endVertex.y

	def setInterpolationIndex( self, portionDirection ):
		"Set the interpolation index."
		self.absolutePortion = self.distances[0] + self.interpolationLength * portionDirection.portion
		interpolationIndexes = range( 0, len( self.distances ) - 1 )
		if portionDirection.directionReversed:
			interpolationIndexes.reverse()
		for self.interpolationIndex in interpolationIndexes:
			begin = self.distances[ self.interpolationIndex ]
			end = self.distances[ self.interpolationIndex + 1 ]
			if self.getComparison( begin, self.absolutePortion ) != self.getComparison( end, self.absolutePortion ):
				return

	def setInterpolationIndexFromTo( self, portionDirection ):
		"Set the interpolation index, the start vertex and the end vertex."
		self.setInterpolationIndex( portionDirection )
		self.innerPortion = self.getInnerPortion()
		self.oneMinusInnerPortion = 1.0 - self.innerPortion
		self.startVertex = self.path[ self.interpolationIndex ]
		self.endVertex = self.path[ self.interpolationIndex + 1 ]


class PortionDirection:
	"Class to hold a portion and direction."
	def __init__( self, portion ):
		"Initialize."
		self.directionReversed = False
		self.portion = portion

	def __repr__(self):
		"Get the string representation of this PortionDirection."
		return '%s: %s' % ( self.portion, self.directionReversed )
