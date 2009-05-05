"""
The xml.py script is an import translator plugin to get a carving from an Art of Illusion xml file.

An import plugin is a script in the import_plugins folder which has the function getCarving.  It is meant to be run from the
interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin
a lower case name.

The getCarving function takes the file name of an xml file and returns the carving.

This example gets a triangle mesh for the xml file boolean.xml.  This example is run in a terminal in the folder which contains
boolean.xml and xml.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import xml
>>> xml.getCarving().getCarveRotatedBoundaryLayers()
[-1.159765625, None, [[(-18.925000000000001-2.4550000000000001j), (-18.754999999999981-2.4550000000000001j)
..
many more lines of the carving
..


An xml file can be exported from Art of Illusion by going to the "File" menu, then going into the "Export" menu item, then
picking the XML choice.  This will bring up the XML file chooser window, choose a place to save the file then click "OK".
Leave the "compressFile" checkbox unchecked.  All the objects from the scene will be exported, this plugin will ignore
the light and camera.  If you want to fabricate more than one object at a time, you can have multiple objects in the Art of
Illusion scene and they will all be carved, then fabricated together.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities.xml_simple_parser import XMLSimpleParser
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import triangle_mesh
import math
import sys

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


#check that matrices & bridge are working, see how to handle a list of objects in Art of Illusion for subtracting
def addCarvableObjectInfo( carvableObjectInfos, objectInfoElement ):
	"Add the object info if it is carvable."
	carvableObjectInfo = getCarvableObjectInfo( objectInfoElement )
	if carvableObjectInfo == None:
		return
	if objectInfoElement.attributeTable[ 'visible' ] == 'false':
		return
	carvableObjectInfo.setShape( carvableObjectInfo.matrix4By4 )
	carvableObjectInfos.append( carvableObjectInfo )

def addCarvableObjectInfoWithMatrix( carvableObjectInfos, matrix4By4, objectInfoElement ):
	"Add the object info if it is carvable."
	carvableObjectInfo = getCarvableObjectInfo( objectInfoElement )
	if carvableObjectInfo == None:
		return
	newMatrix4By4 = matrix4By4.getMultiplied( carvableObjectInfo.matrix4By4.matrix )
	carvableObjectInfo.setShape( newMatrix4By4 )
	carvableObjectInfos.append( carvableObjectInfo )

def addLineLoopsIntersections( loopLoopsIntersections, loops, pointBegin, pointEnd ):
	"Add intersections of the line with the loops."
	normalizedSegment = pointEnd - pointBegin
	normalizedSegmentLength = abs( normalizedSegment )
	if normalizedSegmentLength <= 0.0:
		return
	lineLoopsIntersections = []
	normalizedSegment /= normalizedSegmentLength
	segmentYMirror = complex( normalizedSegment.real, - normalizedSegment.imag )
	pointBeginRotated = segmentYMirror * pointBegin
	pointEndRotated = segmentYMirror * pointEnd
	addLoopsXSegmentIntersections( lineLoopsIntersections, loops, pointBeginRotated.real, pointEndRotated.real, segmentYMirror, pointBeginRotated.imag )
	for lineLoopsIntersection in lineLoopsIntersections:
		point = complex( lineLoopsIntersection, pointBeginRotated.imag ) * normalizedSegment
		loopLoopsIntersections.append( point )

def addLineXSegmentIntersection( lineLoopsIntersections, segmentFirstX, segmentSecondX, vector3First, vector3Second, y ):
	"Add intersections of the line with the x segment."
	isYAboveFirst = y > vector3First.imag
	isYAboveSecond = y > vector3Second.imag
	if isYAboveFirst == isYAboveSecond:
		return
	xIntersection = euclidean.getXIntersection( vector3First, vector3Second, y )
	if xIntersection <= min( segmentFirstX, segmentSecondX ):
		return
	if xIntersection >= max( segmentFirstX, segmentSecondX ):
		return
	lineLoopsIntersections.append( xIntersection )

def addLoopLoopsIntersections( loop, loopsLoopsIntersections, otherLoops ):
	"Add intersections of the loop with the other loops."
	for pointIndex in xrange( len( loop ) ):
		pointBegin = loop[ pointIndex ]
		pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
		addLineLoopsIntersections( loopsLoopsIntersections, otherLoops, pointBegin, pointEnd )

def addLoopsXSegmentIntersections( lineLoopsIntersections, loops, segmentFirstX, segmentSecondX, segmentYMirror, y ):
	"Add intersections of the loops with the x segment."
	for loop in loops:
		addLoopXSegmentIntersections( lineLoopsIntersections, loop, segmentFirstX, segmentSecondX, segmentYMirror, y )

def addLoopXSegmentIntersections( lineLoopsIntersections, loop, segmentFirstX, segmentSecondX, segmentYMirror, y ):
	"Add intersections of the loop with the x segment."
	rotatedLoop = euclidean.getPointsRoundZAxis( segmentYMirror, loop )
	for pointIndex in xrange( len( rotatedLoop ) ):
		pointFirst = rotatedLoop[ pointIndex ]
		pointSecond = rotatedLoop[ ( pointIndex + 1 ) % len( rotatedLoop ) ]
		addLineXSegmentIntersection( lineLoopsIntersections, segmentFirstX, segmentSecondX, pointFirst, pointSecond, y )

def getBottom( points ):
	"Get the bottom of the points."
	bottom = 999999999.9
	for point in points:
		bottom = min( bottom, point.z )
	return bottom

def getCarvableObjectInfo( objectInfoElement ):
	"Get the object info if it is carvable."
	if objectInfoElement == None:
		return
	object = objectInfoElement.getFirstChildWithClassName( 'object' )
	shapeType = object.attributeTable[ 'bf:type' ]
	if shapeType not in globalCarvableClassObjectInfoTable:
		return
	carvableClassObjectInfo = globalCarvableClassObjectInfoTable[ shapeType ]
	newCarvableObjectInfo = carvableClassObjectInfo.getNewCarvableObjectInfo( objectInfoElement )
	return newCarvableObjectInfo

def getCarvableClassObjectInfoTable():
	"Get the carvable class object info table."
	carvableClassObjectInfos = [ CSGObjectObjectInfo(), CubeObjectInfo(), CylinderObjectInfo(), SphereObjectInfo(), TriangleMeshObjectInfo() ]
	carvableClassObjectInfoTable = {}
	for carvableClassObjectInfo in carvableClassObjectInfos:
		className = carvableClassObjectInfo.__class__.__name__
		truncatedClassName = className[ : className.find( 'ObjectInfo' ) ]
		carvableClassObjectInfoTable[ truncatedClassName ] = carvableClassObjectInfo
	return carvableClassObjectInfoTable

def getCarving( fileName = '' ):
	"Get the carving for the xml file."
	if fileName == '':
		unmodified = gcodec.getFilesWithFileTypeWithoutWords( 'xml' )
		if len( unmodified ) == 0:
			print( "There is no xml file in this folder." )
			return None
		fileName = unmodified[ 0 ]
	carving = XMLCarving()
	carving.parseXML( gcodec.getFileText( fileName ) )
	return carving

def getInBetweenPointsFromLoops( importRadius, loops ):
	"Get the in between points from loops."
	inBetweenPoints = []
	for loop in loops:
		for pointIndex in xrange( len( loop ) ):
			pointBegin = loop[ pointIndex ]
			pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
			intercircle.addPointsFromSegment( inBetweenPoints, importRadius, pointBegin, pointEnd, 0.2123 )
	return inBetweenPoints

def getInBetweenPointsFromLoopsBoundarySideOtherLoops( inside, importRadius, loops, otherLoops, radiusSide ):
	"Get the in between points from loops."
	inBetweenPoints = []
	for loop in loops:
		for pointIndex in xrange( len( loop ) ):
			pointBegin = loop[ pointIndex ]
			pointEnd = loop[ ( pointIndex + 1 ) % len( loop ) ]
			inBetweenSegmentPoints = []
			intercircle.addPointsFromSegment( inBetweenSegmentPoints, importRadius, pointBegin, pointEnd, 0.2123 )
			for inBetweenSegmentPoint in inBetweenSegmentPoints:
				if isPointOrEitherLineBoundarySideInsideLoops( inside, otherLoops, pointBegin, inBetweenSegmentPoint, pointEnd, radiusSide ):
					inBetweenPoints.append( inBetweenSegmentPoint )
	return inBetweenPoints

def getJoinedList( originalLists ):
	"Get the lists as one joined list."
	concatenatedList = []
	for originalList in originalLists:
		concatenatedList += originalList
	return concatenatedList

def getLoopsListsIntersections( loopsList ):
	"Get intersections betweens the loops lists."
	loopsListsIntersections = []
	for loopsIndex in xrange( len( loopsList ) ):
		loops = loopsList[ loopsIndex ]
		for otherLoops in loopsList[ : loopsIndex ]:
			loopsListsIntersections += getLoopsLoopsIntersections( loops, otherLoops )
	return loopsListsIntersections

def getLoopsLoopsIntersections( loops, otherLoops ):
	"Get all the intersections of the loops with the other loops."
	loopsLoopsIntersections = []
	for loop in loops:
		addLoopLoopsIntersections( loop, loopsLoopsIntersections, otherLoops )
	return loopsLoopsIntersections

def getPointsBoundarySideLoops( inside, loops, points, radius ):
	"Get the points inside the loops."
	pointsInsideLoops = []
	for pointIndex in xrange( len( points ) ):
		pointBegin = points[ ( pointIndex + len( points ) - 1 ) % len( points ) ]
		pointCenter = points[ pointIndex ]
		pointEnd = points[ ( pointIndex + 1 ) % len( points ) ]
		if isPointOrEitherBoundarySideInsideLoops( inside, loops, pointBegin, pointCenter, pointEnd, radius ):
			pointsInsideLoops.append( pointCenter )
	return pointsInsideLoops

def getSubObjectInfoLoopsList( importRadius, subObjectInfos, z ):
	"Get subObjectInfo loops list."
	subObjectInfoLoopsList = []
	for subObjectInfo in subObjectInfos:
		subObjectInfoLoops = subObjectInfo.getLoops( importRadius, z )
		subObjectInfoLoopsList.append( subObjectInfoLoops )
	return subObjectInfoLoopsList

def getTop( points ):
	"Get the top of the points."
	top = - 999999999.9
	for point in points:
		top = max( top, point.z )
	return top

def getTransformedByList( floatList, point ):
	"Get the point transformed by the array."
	return floatList[ 0 ] * point.x + floatList[ 1 ] * point.y + floatList[ 2 ] * point.z + floatList[ 3 ]

def getValueInQuotes( name, text, value ):
	"Get value in quotes after the name."
	nameAndQuote = name + '="'
	nameIndexStart = text.find( nameAndQuote )
	if nameIndexStart == - 1:
		return value
	valueStartIndex = nameIndexStart + len( nameAndQuote )
	nameIndexEnd = text.find( '"', valueStartIndex )
	if nameIndexEnd == - 1:
		return value
	return float( text[ valueStartIndex : nameIndexEnd ] )

def getVector3TransformedByMatrix( matrix, vector3 ):
	"Get the vector3 multiplied by a vector3."
	vector3Transformed = Vector3()
	vector3Transformed.x = getTransformedByList( matrix[ 0 ], vector3 )
	vector3Transformed.y = getTransformedByList( matrix[ 1 ], vector3 )
	vector3Transformed.z = getTransformedByList( matrix[ 2 ], vector3 )
	return vector3Transformed

def isPointOrEitherBoundarySideInsideLoops( inside, loops, pointBegin, pointCenter, pointEnd, radius ):
	"Determine if the point or a point on either side of the point, is inside the loops."
	if euclidean.isPointInsideLoops( loops, pointCenter ) != inside:
		return False
	segmentBegin = pointBegin - pointCenter
	segmentEnd = pointEnd - pointCenter
	segmentBeginLength = abs( segmentBegin )
	segmentEndLength = abs( segmentEnd )
	if segmentBeginLength <= 0.0 or segmentEndLength <= 0.0:
		return False
	segmentBegin /= segmentBeginLength
	segmentEnd /= segmentEndLength
	addedSegment = segmentBegin + segmentEnd
	addedSegmentLength = abs( addedSegment )
	if addedSegmentLength > 0.0:
		addedSegment *= radius / addedSegmentLength
	else:
		addedSegment = radius * complex( segmentEnd.imag, - segmentEnd.real )
	if euclidean.isPointInsideLoops( loops,  pointCenter + addedSegment ) != inside:
		return False
	return euclidean.isPointInsideLoops( loops,  pointCenter - addedSegment ) == inside

def isPointOrEitherLineBoundarySideInsideLoops( inside, loops, pointBegin, pointCenter, pointEnd, radius ):
	"Determine if the point or a point on either side of the point, is inside the loops."
	if euclidean.isPointInsideLoops( loops, pointCenter ) != inside:
		return False
	segment = pointEnd - pointBegin
	segmentLength = abs( segment )
	if segmentLength <= 0.0:
		return False
	segment /= segmentLength
	addedSegment = radius * complex( segment.imag, - segment.real )
	if euclidean.isPointInsideLoops( loops,  pointCenter + addedSegment ) != inside:
		return False
	return euclidean.isPointInsideLoops( loops,  pointCenter - addedSegment ) == inside


class Matrix4By4:
	"A four by four matrix."
	def __init__( self ):
		"Add empty lists."
		self.matrix = None

	def __repr__( self ):
		"Get the string representation of this four by four matrix."
		return str( self.matrix )

	def getFromAttributeTable( self, attributeTable ):
		"Get the from row column attribute strings, counting from one."
		for column in xrange( 4 ):
			for row in xrange( 4 ):
				columnString = str( column + 1 )
				rowString = str( row + 1 )
				key = 'm' + columnString + rowString
				if key in attributeTable:
					if self.matrix == None:
						self.setMatrixToZero()
					self.matrix[ column ][ row ] = float( attributeTable[ key ] )
				else:
					self.matrix = None
					return self
		return self

	def getMultiplied( self, otherMatrix ):
		"Get this matrix multiplied by the other matrix."
		if otherMatrix == None or self.matrix == None:
			return None
		#A down, B right from http://en.wikipedia.org/wiki/Matrix_multiplication
		newMatrix4By4 = Matrix4By4()
		newMatrix4By4.setMatrixToZero()
		for column in xrange( 4 ):
			for row in xrange( 4 ):
				matrixColumn = self.matrix[ column ]
				dotProduct = 0
				for elementIndex in xrange( 4 ):
					dotProduct += matrixColumn[ elementIndex ] * otherMatrix[ elementIndex ][ row ]
				newMatrix4By4.matrix[ column ][ row ] = dotProduct
		return newMatrix4By4

	def setMatrixToZero( self ):
		"Get the matrix elements to zero."
		self.matrix = [ [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ], [ 0.0, 0.0, 0.0, 0.0 ] ]


class XMLCarving:
	"An svg carving."
	def __init__( self ):
		"Add empty lists."
		self.belowLoops = []
		self.bridgeLayerThickness = None
		self.carvableObjectInfos = []
		self.importRadius = 0.3
		self.layerThickness = 0.4
		self.rotatedBoundaryLayers = []
	
	def __repr__( self ):
		"Get the string representation of this carving."
		return str( self.rotatedBoundaryLayers )

	def getCarveCornerMaximum( self ):
		"Get the corner maximum of the vertices."
		return self.cornerMaximum

	def getCarveCornerMinimum( self ):
		"Get the corner minimum of the vertices."
		return self.cornerMinimum

	def getCarveLayerThickness( self ):
		"Get the layer thickness."
		return self.layerThickness

	def getCarveRotatedBoundaryLayers( self ):
		"Get the rotated boundary layers."
		if len( self.carvableObjectInfos ) < 1:
			return []
		self.cornerMaximum = Vector3( - 999999999.0, - 999999999.0, - 9999999999.9 )
		self.cornerMinimum = Vector3( 999999999.0, 999999999.0, 9999999999.9 )
		for carvableObjectInfo in self.carvableObjectInfos:
			self.cornerMaximum.z = max( self.cornerMaximum.z, carvableObjectInfo.top )
			self.cornerMinimum.z = min( self.cornerMinimum.z, carvableObjectInfo.bottom )
		halfHeight = 0.5 * self.layerThickness
		layerTop = self.cornerMaximum.z - halfHeight
		self.setActualMinimumZ( halfHeight, layerTop )
		self.zZoneInterval = triangle_mesh.getZoneInterval( self.layerThickness )
		z = self.cornerMinimum.z + halfHeight
		while z < layerTop:
			z = self.getZAddExtruderPaths( z )
		for rotatedBoundaryLayer in self.rotatedBoundaryLayers:
			for loop in rotatedBoundaryLayer.loops:
				for point in loop:
					pointVector3 = Vector3( point.real, point.imag, rotatedBoundaryLayer.z )
					self.cornerMaximum = euclidean.getPointMaximum( self.cornerMaximum, pointVector3 )
					self.cornerMinimum = euclidean.getPointMinimum( self.cornerMinimum, pointVector3 )
		self.cornerMaximum.z = layerTop + halfHeight
		for rotatedBoundaryLayerIndex in xrange( len( self.rotatedBoundaryLayers ) - 1, - 1, - 1 ):
			rotatedBoundaryLayer = self.rotatedBoundaryLayers[ rotatedBoundaryLayerIndex ]
			if len( rotatedBoundaryLayer.loops ) > 0:
				return self.rotatedBoundaryLayers[ : rotatedBoundaryLayerIndex + 1 ]
		return []

	def getExtruderPaths( self, z ):
		"Get extruder loops."
		rotatedBoundaryLayer = euclidean.RotatedLoopLayer( z )
		for carvableObjectInfo in self.carvableObjectInfos:
			rotatedBoundaryLayer.loops += carvableObjectInfo.getLoops( self.importRadius, z )
		return rotatedBoundaryLayer

	def getZAddExtruderPaths( self, z ):
		"Get next z and add extruder loops."
		zoneArray = []
		vertices = []
		for carvableObjectInfo in self.carvableObjectInfos:
			vertices += carvableObjectInfo.getVertices()
		for point in vertices:
			triangle_mesh.addToZoneArray( point, z, zoneArray, self.zZoneInterval )
		lowestZoneIndex = triangle_mesh.getLowestZoneIndex( zoneArray, z )
		halfAround = int( math.ceil( float( lowestZoneIndex ) / 2.0 ) )
		zAround = float( halfAround ) * self.zZoneInterval
		if lowestZoneIndex % 2 == 1:
			zAround = - zAround
		zPlusAround = z + zAround
		rotatedBoundaryLayer = self.getExtruderPaths( zPlusAround )
		self.rotatedBoundaryLayers.append( rotatedBoundaryLayer )
		if self.bridgeLayerThickness == None:
			return z + self.layerThickness
		allExtrudateLoops = []
		for loop in rotatedBoundaryLayer.loops:
			allExtrudateLoops += triangle_mesh.getBridgeLoops( self.layerThickness, loop )
		rotatedBoundaryLayer.rotation = triangle_mesh.getBridgeDirection( self.belowLoops, allExtrudateLoops, self.layerThickness )
		self.belowLoops = allExtrudateLoops
		if rotatedBoundaryLayer.rotation == None:
			return z + self.layerThickness
		return z + self.bridgeLayerThickness

	def parseXML( self, xmlText ):
		"Parse XML text and store the layers."
		if xmlText == '':
			return None
		xmlParser = XMLSimpleParser()
		xmlParser.parseXMLText( xmlText )
		artOfIllusionElement = xmlParser.rootElement.getFirstChildWithClassName( 'ArtOfIllusion' )
		sceneElement = artOfIllusionElement.getFirstChildWithClassName( 'Scene' )
		rootElement = sceneElement.getFirstChildWithClassName( 'objects' )
		objectInfoElements = rootElement.getChildrenWithClassName( 'bf:Elem' )
		for objectInfoElement in objectInfoElements:
			addCarvableObjectInfo( self.carvableObjectInfos, objectInfoElement )

	def setActualMinimumZ( self, halfHeight, layerTop ):
		"Get the actual minimum z at the lowest rotated boundary layer."
		while self.cornerMinimum.z < layerTop:
			if len( self.getExtruderPaths( self.cornerMinimum.z ).loops ) > 0:
				increment = - halfHeight
				while abs( increment ) > 0.001 * halfHeight:
					self.cornerMinimum.z += increment
					increment = 0.5 * abs( increment )
					if len( self.getExtruderPaths( self.cornerMinimum.z ).loops ) > 0:
						increment = - increment
				return
			self.cornerMinimum.z += self.layerThickness

	def setCarveBridgeLayerThickness( self, bridgeLayerThickness ):
		"Set the bridge layer thickness.  If the infill is not in the direction of the bridge, the bridge layer thickness should be given as None or not set at all."
		self.bridgeLayerThickness = bridgeLayerThickness

	def setCarveLayerThickness( self, layerThickness ):
		"Set the layer thickness."
		self.layerThickness = layerThickness

	def setCarveImportRadius( self, importRadius ):
		"Set the import radius."
		self.importRadius = importRadius

	def setCarveIsCorrectMesh( self, isCorrectMesh ):
		"Set the is correct mesh flag."
		self.isCorrectMesh = isCorrectMesh


class TriangleMeshObjectInfo:
	"An Art of Illusion object info."
	def __init__( self ):
		"Set name to None."
		self.name = None

	def __repr__( self ):
		"Get the string representation of this object info."
		if self.name == None:
			return self.__class__.__name__
		return "%s %s\n%s" % ( self.name, self.__class__.__name__, self.triangleMesh )

	def getLoops( self, importRadius, z ):
		"Get loops sliced through shape."
		self.triangleMesh.importRadius = importRadius
		return self.triangleMesh.getLoopsFromMesh( z )

	def getNewCarvableObjectInfo( self, objectInfoElement ):
		"Get new carvable object info."
		newCarvableObjectInfo = self.__class__()
		newCarvableObjectInfo.name = objectInfoElement.getFirstChildWithClassName( 'name' ).text
		newCarvableObjectInfo.object = objectInfoElement.getFirstChildWithClassName( 'object' )
		coords = objectInfoElement.getFirstChildWithClassName( 'coords' )
		transformAttributeTable = self.getTransformAttributeTable( coords, 'transformFrom' )
		if len( transformAttributeTable ) < 16:
			transformAttributeTable = self.getTransformAttributeTable( coords, 'transformTo' )
		newCarvableObjectInfo.matrix4By4 = Matrix4By4().getFromAttributeTable( transformAttributeTable )
		return newCarvableObjectInfo

	def getTransformAttributeTable( self, coords, transformName ):
		"Get the transform attributes."
		transformAttributeTable = coords.getFirstChildWithClassName( transformName ).attributeTable
		if len( transformAttributeTable ) < 16:
			if 'bf:ref' in transformAttributeTable:
				idReference = transformAttributeTable[ 'bf:ref' ]
				return coords.rootElement.getSubChildWithID( idReference ).attributeTable
		return transformAttributeTable

	def getVertices( self ):
		"Get all vertices."
		return self.triangleMesh.vertices

	def setShape( self, matrix4By4 ):
		"Set the shape of this carvable object info."
		self.triangleMesh = triangle_mesh.TriangleMesh()
		vertexElement = self.object.getFirstChildWithClassName( 'vertex' )
		vertexPointElements = vertexElement.getChildrenWithClassName( 'bf:Elem' )
		for vertexPointElement in vertexPointElements:
			coordinateElement = vertexPointElement.getFirstChildWithClassName( 'r' )
			vertex = Vector3( float( coordinateElement.attributeTable[ 'x' ] ), float( coordinateElement.attributeTable[ 'y' ] ), float( coordinateElement.attributeTable[ 'z' ] ) )
			self.triangleMesh.vertices.append( getVector3TransformedByMatrix( matrix4By4.matrix, vertex ) )
		edgeElement = self.object.getFirstChildWithClassName( 'edge' )
		edgeSubelements = edgeElement.getChildrenWithClassName( 'bf:Elem' )
		for edgeSubelementIndex in xrange( len( edgeSubelements ) ):
			edgeSubelement = edgeSubelements[ edgeSubelementIndex ]
			vertexIndexes = [ int( edgeSubelement.attributeTable[ 'v1' ] ), int( edgeSubelement.attributeTable[ 'v2' ] ) ]
			edge = triangle_mesh.Edge().getFromVertexIndexes( edgeSubelementIndex, vertexIndexes )
			self.triangleMesh.edges.append( edge )
		faceElement = self.object.getFirstChildWithClassName( 'face' )
		faceSubelements = faceElement.getChildrenWithClassName( 'bf:Elem' )
		for faceSubelementIndex in xrange( len( faceSubelements ) ):
			faceSubelement = faceSubelements[ faceSubelementIndex ]
			edgeIndexes = [ int( faceSubelement.attributeTable[ 'e1' ] ), int( faceSubelement.attributeTable[ 'e2' ] ), int( faceSubelement.attributeTable[ 'e3' ] ) ]
			face = triangle_mesh.Face().getFromEdgeIndexes( edgeIndexes, self.triangleMesh.edges, faceSubelementIndex )
			self.triangleMesh.faces.append( face )
		self.bottom = getBottom( self.triangleMesh.vertices )
		self.top = getTop( self.triangleMesh.vertices )


class CSGObjectObjectInfo( TriangleMeshObjectInfo ):
	"An Art of Illusion CSG object info."
	def __repr__( self ):
		"Get the string representation of this object info."
		if self.name == None:
			return self.__class__.__name__
		stringRepresentation = '%s %s\n%s' % ( self.name, self.__class__.__name__ )
		for subObjectInfo in self.subObjectInfos:
			stringRepresentation += '\n%s' % subObjectInfo
		return stringRepresentation

	def getIntersectedLoops( self, importRadius, subObjectInfoLoopsList ):
		"Get intersected loops sliced through shape."
		firstLoops = subObjectInfoLoopsList[ 0 ]
		lastLoops = getJoinedList( subObjectInfoLoopsList[ 1 : ] )
		radiusSide = 0.01 * importRadius
		corners = getPointsBoundarySideLoops( True, firstLoops, getJoinedList( lastLoops ), radiusSide )
		corners += getPointsBoundarySideLoops( True, lastLoops, getJoinedList( firstLoops ), radiusSide )
		corners += getLoopsListsIntersections( subObjectInfoLoopsList )
		allPoints = corners[ : ]
		allPoints += getInBetweenPointsFromLoopsBoundarySideOtherLoops( True, importRadius, lastLoops, firstLoops, radiusSide )
		allPoints += getInBetweenPointsFromLoopsBoundarySideOtherLoops( True, importRadius, firstLoops, lastLoops, radiusSide )
		return triangle_mesh.getInclusiveLoops( allPoints, corners, importRadius, False )

	def getJoinedLoops( self, importRadius, subObjectInfoLoopsList ):
		"Get joined loops sliced through shape."
		loops = []
		for subObjectInfoLoops in subObjectInfoLoopsList:
			loops += subObjectInfoLoops
		corners = []
		for loop in loops:
			corners += loop
		corners += getLoopsListsIntersections( subObjectInfoLoopsList )
		allPoints = corners[ : ]
		allPoints += getInBetweenPointsFromLoops( importRadius, loops )
		return triangle_mesh.getInclusiveLoops( allPoints, corners, importRadius, False )

	def getLoops( self, importRadius, z ):
		"Get loops sliced through shape."
		if len( self.subObjectInfos ) < 1:
			return []
		operationString = self.object.attributeTable[ 'operation' ]
#		operationString = '1'#
		subObjectInfoLoopsList = getSubObjectInfoLoopsList( importRadius, self.subObjectInfos, z )
		if operationString == '0':
			return self.getJoinedLoops( importRadius, subObjectInfoLoopsList )
		if operationString == '1':
			return self.getIntersectedLoops( importRadius, subObjectInfoLoopsList )
		if operationString == '2':
			return self.getSubtractedLoops( importRadius, subObjectInfoLoopsList )
		if operationString == '3':
			subObjectInfoLoopsList.reverse()
			return self.getSubtractedLoops( importRadius, subObjectInfoLoopsList )
		return []

	def getSubtractedLoops( self, importRadius, subObjectInfoLoopsList ):
		"Get subtracted loops sliced through shape."
		negativeLoops = getJoinedList( subObjectInfoLoopsList[ 1 : ] )
		positiveLoops = subObjectInfoLoopsList[ 0 ]
		radiusSide = 0.01 * importRadius
		corners = getPointsBoundarySideLoops( True, positiveLoops, getJoinedList( negativeLoops ), radiusSide )
		corners += getPointsBoundarySideLoops( False, negativeLoops, getJoinedList( positiveLoops ), radiusSide )
		loopsListsIntersections = getLoopsListsIntersections( subObjectInfoLoopsList )
		corners += loopsListsIntersections
		allPoints = corners[ : ]
		allPoints += getInBetweenPointsFromLoopsBoundarySideOtherLoops( True, importRadius, negativeLoops, positiveLoops, radiusSide )
		allPoints += getInBetweenPointsFromLoopsBoundarySideOtherLoops( False, importRadius, positiveLoops, negativeLoops, radiusSide )
		return triangle_mesh.getInclusiveLoops( allPoints, corners, importRadius, False )

	def getVertices( self ):
		"Get all vertices."
		vertices = []
		for subObjectInfo in self.subObjectInfos:
			vertices += subObjectInfo.getVertices()
		return vertices

	def setShape( self, matrix4By4 ):
		"Set the shape of this carvable object info."
		self.subObjectInfos = []
		addCarvableObjectInfoWithMatrix( self.subObjectInfos, matrix4By4, self.object.getFirstChildWithClassName( 'obj1' ) )
		addCarvableObjectInfoWithMatrix( self.subObjectInfos, matrix4By4, self.object.getFirstChildWithClassName( 'obj2' ) )
		self.bottom = 999999999.9
		self.top = - 999999999.9
		for subObjectInfo in self.subObjectInfos:
			self.bottom = min( self.bottom, subObjectInfo.bottom )
			self.top = max( self.top, subObjectInfo.top )


class CubeObjectInfo( TriangleMeshObjectInfo ):
	"An Art of Illusion Cube object info."
	def setBottomTopTriangleMesh( self, edgeTriples, matrix4By4, vertexPairs, vertices ):
		"Set the bottom, top and triangle mesh of this carvable object info."
		self.triangleMesh = triangle_mesh.TriangleMesh()
		for vertex in vertices:
			self.triangleMesh.vertices.append( getVector3TransformedByMatrix( matrix4By4.matrix, vertex ) )
		for vertexPairsIndex in xrange( len( vertexPairs ) ):
			vertexPair = vertexPairs[ vertexPairsIndex ]
			edge = triangle_mesh.Edge().getFromVertexIndexes( vertexPairsIndex, vertexPair )
			self.triangleMesh.edges.append( edge )
		for edgeTriplesIndex in xrange( len( edgeTriples ) ):
			edgeTriple = edgeTriples[ edgeTriplesIndex ]
			face = triangle_mesh.Face().getFromEdgeIndexes( edgeTriple, self.triangleMesh.edges, edgeTriplesIndex )
			self.triangleMesh.faces.append( face )
		self.bottom = getBottom( self.triangleMesh.vertices )
		self.top = getTop( self.triangleMesh.vertices )

	def setShape( self, matrix4By4 ):
		"Set the shape of this carvable object info."
		halfX = float( self.object.attributeTable[ 'halfx' ] )
		halfY = float( self.object.attributeTable[ 'halfy' ] )
		halfZ = float( self.object.attributeTable[ 'halfz' ] )
		vertices = [
			Vector3( - 1.0, - 1.0, 1.0 ),
			Vector3( 1.0, - 1.0, 1.0 ),
			Vector3( 1.0, - 1.0, - 1.0 ),
			Vector3( - 1.0, - 1.0, - 1.0 ),
			Vector3( - 1.0, 1.0, 1.0 ),
			Vector3( 1.0, 1.0, 1.0 ),
			Vector3( 1.0, 1.0, - 1.0 ),
			Vector3( - 1.0, 1.0, - 1.0 ) ]
		for vertex in vertices:
			vertex.x *= halfX
			vertex.y *= halfY
			vertex.z *= halfZ
		vertexPairs = [
			[ 6, 4 ],
			[ 7, 6 ],
			[ 6, 2 ],
			[ 3, 2 ],
			[ 2, 1 ],
			[ 3, 1 ],
			[ 1, 0 ],
			[ 7, 2 ],
			[ 6, 1 ],
			[ 6, 5 ],
			[ 5, 1 ],
			[ 4, 3 ],
			[ 3, 0 ],
			[ 7, 3 ],
			[ 5, 0 ],
			[ 5, 4 ],
			[ 4, 0 ],
			[ 7, 4 ] ]
		edgeTriples = [
			[ 9, 0, 15 ],
			[ 1, 2, 7 ],
			[ 3, 4, 5 ],
			[ 12, 5, 6 ],
			[ 13, 7, 3 ],
			[ 2, 8, 4 ],
			[ 9, 10, 8 ],
			[ 16, 11, 12 ],
			[ 17, 13, 11 ],
			[ 10, 14, 6 ],
			[ 15, 16, 14 ],
			[ 1, 17, 0 ] ]
		self.setBottomTopTriangleMesh( edgeTriples, matrix4By4, vertexPairs, vertices )


class CylinderObjectInfo( CubeObjectInfo ):
	"An Art of Illusion Cylinder object info."
	def setShape( self, matrix4By4 ):
		"Set the shape of this carvable object info."
		numberOfSides = 31
		height = float( self.object.attributeTable[ 'height' ] )
		halfHeight = 0.5 * height
		radiusX = float( self.object.attributeTable[ 'rx' ] )
		ratioTopOverBottom = float( self.object.attributeTable[ 'ratio' ] )
		radiusZ = float( self.object.attributeTable[ 'rz' ] )
		vertices = []
		sideAngle = 2.0 * math.pi / float( numberOfSides )
		halfSideAngle = 0.5 * sideAngle
		edgeTriples = []
		vertexPairs = []
		numberOfVertices = numberOfSides + numberOfSides
		numberOfCircumferentialEdges = numberOfVertices + numberOfVertices
		for side in xrange( numberOfSides ):
			bottomAngle = float( side ) * sideAngle
			bottomComplex = euclidean.getPolar( bottomAngle, 1.0 )
			bottomPoint = Vector3( bottomComplex.real * radiusX, - halfHeight, bottomComplex.imag * radiusZ )
			vertices.append( bottomPoint )
			topPoint = Vector3( bottomPoint.x * ratioTopOverBottom, halfHeight, bottomPoint.z * ratioTopOverBottom )
			vertices.append( topPoint )
			vertexPairBottom = [ side + side, ( side + side + 2 ) % numberOfVertices ]
			vertexPairBottomIndex = len( vertexPairs )
			vertexPairs.append( vertexPairBottom )
			vertexPairDiagonal = [ ( side + side + 2 ) % numberOfVertices, side + side + 1 ]
			vertexPairDiagonalIndex = len( vertexPairs )
			vertexPairs.append( vertexPairDiagonal )
			vertexPairVertical = [ side + side + 1, side + side ]
			vertexPairVerticalIndex = len( vertexPairs )
			vertexPairs.append( vertexPairVertical )
			vertexPairTop = [ side + side + 1, ( side + side + 3 ) % numberOfVertices ]
			vertexPairTopIndex = len( vertexPairs )
			vertexPairs.append( vertexPairTop )
			edgeTripleBottomVertical = [ vertexPairBottomIndex, vertexPairDiagonalIndex, vertexPairVerticalIndex ]
			edgeTriples.append( edgeTripleBottomVertical )
			edgeTripleBottomVertical = [ vertexPairTopIndex, vertexPairDiagonalIndex, ( vertexPairVerticalIndex + 4 ) % numberOfCircumferentialEdges ]
			edgeTriples.append( edgeTripleBottomVertical )
		for side in xrange( 2, numberOfSides - 1 ):
			vertexPairBottomHorizontal = [ 0, side + side ]
			vertexPairs.append( vertexPairBottomHorizontal )
			vertexPairTopHorizontal = [ 1, side + side + 1 ]
			vertexPairs.append( vertexPairTopHorizontal )
		for side in xrange( 1, numberOfSides - 1 ):
			vertexPairBottomIndex = 4 * side
			vertexPairBottomDiagonalIndex = vertexPairBottomIndex + 4
			vertexPairBottomBeforeIndex = vertexPairBottomIndex - 4
			vertexPairTopIndex = 4 * side + 3
			vertexPairTopDiagonalIndex = vertexPairTopIndex + 4
			vertexPairTopBeforeIndex = vertexPairTopIndex - 4
			if side > 1:
				vertexPairBottomBeforeIndex = numberOfCircumferentialEdges + 2 * side - 4
				vertexPairTopBeforeIndex = vertexPairBottomBeforeIndex + 1
			if side < numberOfSides - 2:
				vertexPairBottomDiagonalIndex = numberOfCircumferentialEdges + 2 * side - 2
				vertexPairTopDiagonalIndex = vertexPairBottomDiagonalIndex + 1
			edgeTripleBottomHorizontal = [ vertexPairBottomIndex, vertexPairBottomDiagonalIndex, vertexPairBottomBeforeIndex ]
			edgeTriples.append( edgeTripleBottomHorizontal )
			edgeTripleTopHorizontal = [ vertexPairTopIndex, vertexPairTopDiagonalIndex, vertexPairTopBeforeIndex ]
			edgeTriples.append( edgeTripleTopHorizontal )
		self.setBottomTopTriangleMesh( edgeTriples, matrix4By4, vertexPairs, vertices )


class SphereObjectInfo( CubeObjectInfo ):
	"An Art of Illusion Sphere object info."
	def setShape( self, matrix4By4 ):
		"Set the shape of this carvable object info."
		self.numberOfInBetweens = 19
		self.numberOfDivisions = self.numberOfInBetweens + 1
		squareRadius = 0.5 * float( self.numberOfInBetweens )
		vertexPairs = []
		edgeTriples = []
		vertices = []
		edgeDiagonalTable = {}
		edgeHorizontalTable = {}
		edgeVerticalTable = {}
		vertexTable = {}
		for row in xrange( self.numberOfDivisions ):
			for column in xrange( self.numberOfDivisions ):
				columnMinusRadius = float( column - squareRadius )
				rowMinusRadius = float( row - squareRadius )
				height = min( squareRadius - abs( columnMinusRadius ), squareRadius - abs( rowMinusRadius ) )
				squarePoint = Vector3( rowMinusRadius, columnMinusRadius, - height )
				vertexTable[ row, column, 0 ] = len( vertices )
				if row != 0 and row != self.numberOfInBetweens and column != 0 and column != self.numberOfInBetweens:
					vertices.append( squarePoint )
					squarePoint = Vector3( rowMinusRadius, columnMinusRadius, height )
				vertexTable[ row, column, 1 ] = len( vertices )
				vertices.append( squarePoint )
		for row in xrange( self.numberOfInBetweens ):
			for column in xrange( self.numberOfDivisions ):
				horizontalEdgeBottom = [ vertexTable[ row, column, 0 ], vertexTable[ row + 1, column, 0 ] ]
				edgeHorizontalTable[ row, column, 0 ] = len( vertexPairs )
				vertexPairs.append( horizontalEdgeBottom )
				horizontalEdgeTop = [ vertexTable[ row, column, 1 ], vertexTable[ row + 1, column, 1 ] ]
				edgeHorizontalTable[ row, column, 1 ] = len( vertexPairs )
				vertexPairs.append( horizontalEdgeTop )
		for row in xrange( self.numberOfDivisions ):
			for column in xrange( self.numberOfInBetweens ):
				verticalEdgeBottom = [ vertexTable[ row, column, 0 ], vertexTable[ row, column + 1, 0 ] ]
				edgeVerticalTable[ row, column, 0 ] = len( vertexPairs )
				vertexPairs.append( verticalEdgeBottom )
				verticalEdgeTop = [ vertexTable[ row, column, 1 ], vertexTable[ row, column + 1, 1 ] ]
				edgeVerticalTable[ row, column, 1 ] = len( vertexPairs )
				vertexPairs.append( verticalEdgeTop )
		for row in xrange( self.numberOfInBetweens ):
			for column in xrange( self.numberOfInBetweens ):
				diagonalEdgeBottom = [ vertexTable[ row, column, 0 ], vertexTable[ row + 1, column + 1, 0 ] ]
				edgeDiagonalTable[ row, column, 0 ] = len( vertexPairs )
				vertexPairs.append( diagonalEdgeBottom )
				diagonalEdgeTop = [ vertexTable[ row, column, 1 ], vertexTable[ row + 1, column + 1, 1 ] ]
				edgeDiagonalTable[ row, column, 1 ] = len( vertexPairs )
				vertexPairs.append( diagonalEdgeTop )
		for row in xrange( self.numberOfInBetweens ):
			for column in xrange( self.numberOfInBetweens ):
				fourThirtyOClockFaceBottom = [ edgeHorizontalTable[ row, column, 0 ], edgeVerticalTable[ row + 1, column, 0 ], edgeDiagonalTable[ row, column, 0 ] ]
				edgeTriples.append( fourThirtyOClockFaceBottom )
				tenThirtyOClockFaceBottom = [ edgeHorizontalTable[ row, column + 1, 0 ], edgeVerticalTable[ row, column, 0 ], edgeDiagonalTable[ row, column, 0 ] ]
				edgeTriples.append( tenThirtyOClockFaceBottom )
				fourThirtyOClockFaceTop = [ edgeHorizontalTable[ row, column, 1 ], edgeVerticalTable[ row + 1, column, 1 ], edgeDiagonalTable[ row, column, 1 ] ]
				edgeTriples.append( fourThirtyOClockFaceTop )
				tenThirtyOClockFaceTop = [ edgeHorizontalTable[ row, column + 1, 1 ], edgeVerticalTable[ row, column, 1 ], edgeDiagonalTable[ row, column, 1 ] ]
				edgeTriples.append( tenThirtyOClockFaceTop )
		radiusX = float( self.object.attributeTable[ 'rx' ] )
		radiusY = float( self.object.attributeTable[ 'ry' ] )
		radiusZ = float( self.object.attributeTable[ 'rz' ] )
		for vertex in vertices:
			vertex.normalize()
			vertex.x *= radiusX
			vertex.y *= radiusY
			vertex.z *= radiusZ
		self.setBottomTopTriangleMesh( edgeTriples, matrix4By4, vertexPairs, vertices )


globalCarvableClassObjectInfoTable = getCarvableClassObjectInfoTable()

def main( hashtable = None ):
	"Display the inset dialog."
	if len( sys.argv ) > 1:
		getCarving( ' '.join( sys.argv[ 1 : ] ) )

if __name__ == "__main__":
	main()
