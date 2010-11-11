"""
Boolean geometry four by four matrix.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import xml_simple_writer
import cStringIO
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


globalExecutionOrder = 300


def addConnectionVertexes(connectionVertexes, geometryOutput):
	"Add the connections and vertexes."
	if geometryOutput.__class__ == list:
		for element in geometryOutput:
			addConnectionVertexes(connectionVertexes, element)
		return
	if geometryOutput.__class__ != dict:
		return
	for geometryOutputKey in geometryOutput.keys():
		geometryOutputValue = geometryOutput[geometryOutputKey]
		if geometryOutputKey == 'connectionStart' or geometryOutputKey == 'connectionEnd':
			connectionVertexes.append(geometryOutputValue)
		elif geometryOutputKey == 'vertex':
			for vertex in geometryOutputValue:
				connectionVertexes.append(vertex)
		else:
			addConnectionVertexes(connectionVertexes, geometryOutputValue)

def addVertexes(geometryOutput, vertexes):
	"Add the vertexes."
	if geometryOutput.__class__ == list:
		for element in geometryOutput:
			addVertexes(element, vertexes)
		return
	if geometryOutput.__class__ != dict:
		return
	for geometryOutputKey in geometryOutput.keys():
		geometryOutputValue = geometryOutput[geometryOutputKey]
		if geometryOutputKey == 'vertex':
			for vertex in geometryOutputValue:
				vertexes.append(vertex)
		else:
			addVertexes(geometryOutputValue, vertexes)

def getConnectionVertexes(geometryOutput):
	"Get the connections and vertexes."
	connectionVertexes = []
	addConnectionVertexes(connectionVertexes, geometryOutput)
	return connectionVertexes

def getCumulativeVector3Remove(prefix, vector3, xmlElement):
	"Get cumulative vector3 and delete the prefixed attributes."
	cumulativeVector3 = evaluate.getVector3RemoveByPrefix(prefix + 'rectangular', vector3, xmlElement)
	cylindrical = evaluate.getVector3RemoveByPrefix(prefix + 'cylindrical', Vector3(), xmlElement)
	if not cylindrical.getIsDefault():
		cylindricalComplex = euclidean.getWiddershinsUnitPolar(math.radians(cylindrical.y)) * cylindrical.x
		cumulativeVector3 += Vector3(cylindricalComplex.real, cylindricalComplex.imag, cylindrical.z)
	polar = evaluate.getVector3RemoveByPrefix(prefix + 'polar', Vector3(), xmlElement)
	if not polar.getIsDefault():
		polarComplex = euclidean.getWiddershinsUnitPolar(math.radians(polar.y)) * polar.x
		cumulativeVector3 += Vector3(polarComplex.real, polarComplex.imag)
	spherical = evaluate.getVector3RemoveByPrefix(prefix + 'spherical', Vector3(), xmlElement)
	if not spherical.getIsDefault():
		radius = spherical.x
		elevationComplex = euclidean.getWiddershinsUnitPolar(math.radians(spherical.z)) * radius
		azimuthComplex = euclidean.getWiddershinsUnitPolar(math.radians(spherical.y)) * elevationComplex.real
		cumulativeVector3 += Vector3(azimuthComplex.real, azimuthComplex.imag, elevationComplex.imag)
	return cumulativeVector3

def getDiagonalSwitchedTetragrid(angleDegrees, diagonals):
	"Get the diagonals and switched matrix."
	unitPolar = euclidean.getWiddershinsUnitPolar(math.radians(angleDegrees))
	diagonalSwitchedTetragrid = getIdentityMatrixTetragrid()
	for diagonal in diagonals:
		diagonalSwitchedTetragrid[ diagonal ][ diagonal ] = unitPolar.real
	diagonalSwitchedTetragrid[diagonals[0]][diagonals[1]] = - unitPolar.imag
	diagonalSwitchedTetragrid[diagonals[1]][diagonals[0]] = unitPolar.imag
	return diagonalSwitchedTetragrid

def getFromObjectOrXMLElement(xmlElement):
	"Get matrix starting from the object if it exists, otherwise get a matrix starting from stratch."
	xmlElementMatrix = None
	if xmlElement.object != None:
		xmlElementMatrix = xmlElement.object.matrix4X4
	else:
		xmlElementMatrix = Matrix()
	return xmlElementMatrix.getFromXMLElement('matrix.', xmlElement)

def getIdentityMatrixTetragrid(tetragrid=None):
	"Get four by four matrix with diagonal elements set to one."
	if tetragrid == None:
		return [[1.0, 0.0, 0.0, 0.0], [0.0, 1.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]]
	return tetragrid

def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	"Get equated paths."
	matrixPoints( loop, prefix, xmlElement )
	return [loop]

def getManipulatedGeometryOutput(geometryOutput, xmlElement):
	"Get equated geometryOutput."
	matrixPoints( getConnectionVertexes(geometryOutput), 'scale.', xmlElement )
	return geometryOutput

def getMatrixKey(row, column, prefix=''):
	"Get the key string from row & column, counting from one."
	return prefix + 'm' + str(row + 1) + str(column + 1)

def getMatrixKeys(prefix=''):
	"Get the matrix keys."
	matrixKeys = []
	for row in xrange(4):
		for column in xrange(4):
			key = getMatrixKey(row, column, prefix)
			matrixKeys.append(key)
	return matrixKeys

def getMatrixTetragrid(prefix, xmlElement):
	"Get the matrix Tetragrid from the xmlElement."
	matrixTetragrid = getMatrixTetragridC(None, prefix, xmlElement)
	matrixTetragrid = getMatrixTetragridM(matrixTetragrid, prefix, xmlElement)
	matrixTetragrid = getMatrixTetragridMatrix(matrixTetragrid, prefix, xmlElement)
	matrixTetragrid = getMatrixTetragridR(matrixTetragrid, prefix, xmlElement)
	return matrixTetragrid

def getMatrixTetragridC(matrixTetragrid, prefix, xmlElement):
	"Get the matrix Tetragrid from the xmlElement c values."
	columnKeys = 'Pc1 Pc2 Pc3 Pc4'.replace('P', prefix).split()
	evaluatedDictionary = evaluate.getEvaluatedDictionary(columnKeys, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return matrixTetragrid
	for columnKeyIndex, columnKey in enumerate(columnKeys):
		if columnKey in evaluatedDictionary:
			value = evaluatedDictionary[columnKey]
			if value == None or value == 'None':
				print('Warning, value in getMatrixTetragridC in matrix is None for columnKey for dictionary:')
				print(columnKey)
				print(evaluatedDictionary)
			else:
				matrixTetragrid = getIdentityMatrixTetragrid(matrixTetragrid)
				for elementIndex, element in enumerate(value):
					matrixTetragrid[elementIndex][columnKeyIndex] = element
	euclidean.removeListFromDictionary(xmlElement.attributeDictionary, columnKeys)
	return matrixTetragrid

def getMatrixTetragridM(matrixTetragrid, prefix, xmlElement):
	"Get the matrix Tetragrid from the xmlElement m values."
	matrixKeys = getMatrixKeys(prefix)
	evaluatedDictionary = evaluate.getEvaluatedDictionary(matrixKeys, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return matrixTetragrid
	for row in xrange(4):
		for column in xrange(4):
			key = getMatrixKey(row, column, prefix)
			if key in evaluatedDictionary:
				value = evaluatedDictionary[key]
				if value == None or value == 'None':
					print('Warning, value in getMatrixTetragridM in matrix is None for key for dictionary:')
					print(key)
					print(evaluatedDictionary)
				else:
					matrixTetragrid = getIdentityMatrixTetragrid(matrixTetragrid)
					matrixTetragrid[row][column] = float(value)
	euclidean.removeListFromDictionary(xmlElement.attributeDictionary, matrixKeys)
	return matrixTetragrid

def getMatrixTetragridMatrix(matrixTetragrid, prefix, xmlElement):
	"Get the matrix Tetragrid from the xmlElement matrix value."
	matrixKey = prefix + 'matrix'
	evaluatedDictionary = evaluate.getEvaluatedDictionary([matrixKey], xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return matrixTetragrid
	value = evaluatedDictionary[matrixKey]
	if value == None or value == 'None':
		print('Warning, value in getMatrixTetragridMatrix in matrix is None for matrixKey for dictionary:')
		print(matrixKey)
		print(evaluatedDictionary)
	else:
		matrixTetragrid = getIdentityMatrixTetragrid(matrixTetragrid)
		for rowIndex, row in enumerate(value):
			for elementIndex, element in enumerate(row):
				matrixTetragrid[rowIndex][elementIndex] = element
	euclidean.removeListFromDictionary(xmlElement.attributeDictionary, [matrixKey])
	return matrixTetragrid

def getMatrixTetragridR(matrixTetragrid, prefix, xmlElement):
	"Get the matrix Tetragrid from the xmlElement r values."
	rowKeys = 'Pr1 Pr2 Pr3 Pr4'.replace('P', prefix).split()
	evaluatedDictionary = evaluate.getEvaluatedDictionary(rowKeys, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return matrixTetragrid
	for rowKeyIndex, rowKey in enumerate(rowKeys):
		if rowKey in evaluatedDictionary:
			value = evaluatedDictionary[rowKey]
			if value == None or value == 'None':
				print('Warning, value in getMatrixTetragridR in matrix is None for rowKey for dictionary:')
				print(rowKey)
				print(evaluatedDictionary)
			else:
				matrixTetragrid = getIdentityMatrixTetragrid(matrixTetragrid)
				for elementIndex, element in enumerate(value):
					matrixTetragrid[rowKeyIndex][elementIndex] = element
	euclidean.removeListFromDictionary(xmlElement.attributeDictionary, rowKeys)
	return matrixTetragrid

def getRemovedFloatByKeys( keys, prefix, xmlElement ):
	"Get the float by the keys and the prefix."
	removedFloat = 0.0
	for key in keys:
		prefixKey = prefix + key
		if prefixKey in xmlElement.attributeDictionary:
			floatValue = evaluate.getEvaluatedFloat( prefixKey, xmlElement )
			if floatValue == None:
				print('Warning, evaluated value in getEvaluatedFloatByPrefixes in matrix is None for key:')
				print( prefixKey )
				print('for xmlElement dictionary value:')
				print( xmlElement.attributeDictionary[ prefixKey ] )
				print('for xmlElement dictionary:')
				print(xmlElement.attributeDictionary)
			else:
				removedFloat += floatValue
			del xmlElement.attributeDictionary[ prefixKey ]
	return removedFloat

def getRotateMatrixTetragrid(prefix, xmlElement):
	"Get rotate matrix tetragrid and delete the rotate attributes."
	# http://en.wikipedia.org/wiki/Rotation_matrix zxy
	rotateMatrix = Matrix()
	zAngle = getRemovedFloatByKeys( ['axisclockwisez', 'observerclockwisez', 'z'], prefix, xmlElement )
	zAngle -= getRemovedFloatByKeys( ['axiscounterclockwisez', 'observercounterclockwisez'], prefix, xmlElement )
	if zAngle != 0.0:
		rotateMatrix.matrixTetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(-zAngle, [0, 1])).matrixTetragrid
	xAngle = getRemovedFloatByKeys( ['axisclockwisex', 'observerclockwisex', 'x'], prefix, xmlElement )
	xAngle -= getRemovedFloatByKeys( ['axiscounterclockwisex', 'observercounterclockwisex'], prefix, xmlElement )
	if xAngle != 0.0:
		rotateMatrix.matrixTetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(-xAngle, [1, 2])).matrixTetragrid
	yAngle = getRemovedFloatByKeys( ['axiscounterclockwisey', 'observerclockwisey', 'y'], prefix, xmlElement )
	yAngle -= getRemovedFloatByKeys( ['axisclockwisey', 'observercounterclockwisey'], prefix, xmlElement )
	if yAngle != 0.0:
		rotateMatrix.matrixTetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(yAngle, [0, 2])).matrixTetragrid
	return rotateMatrix.matrixTetragrid

def getScaleMatrixTetragrid(prefix, xmlElement):
	"Get scale matrix and delete the scale attributes."
	scaleDefaultVector3 = Vector3(1.0, 1.0, 1.0)
	scale = getCumulativeVector3Remove( prefix, scaleDefaultVector3.copy(), xmlElement )
	if scale == scaleDefaultVector3:
		return None
	return [[scale.x, 0.0, 0.0, 0.0], [0.0, scale.y, 0.0, 0.0], [0.0, 0.0, scale.z, 0.0], [0.0, 0.0, 0.0, 1.0]]

def getTetragridCopy(tetragrid):
	"Get tetragrid copy."
	tetragridCopy = []
	for tetragridRow in tetragrid:
		tetragridCopy.append(tetragridRow[:])
	return tetragridCopy

def getTetragridTimesOther(firstTetragrid, otherTetragrid ):
	"Get this matrix multiplied by the other matrix."
	#A down, B right from http://en.wikipedia.org/wiki/Matrix_multiplication
	tetragridTimesOther = []
	for row in xrange( 4 ):
		matrixRow = firstTetragrid[ row ]
		tetragridTimesOtherRow = []
		tetragridTimesOther.append(tetragridTimesOtherRow)
		for column in xrange( 4 ):
			dotProduct = 0
			for elementIndex in xrange( 4 ):
				dotProduct += matrixRow[ elementIndex ] * otherTetragrid[ elementIndex ][ column ]
			tetragridTimesOtherRow.append(dotProduct)
	return tetragridTimesOther

def getTransformedByList( floatList, point ):
	"Get the point transformed by the array."
	return floatList[0] * point.x + floatList[1] * point.y + floatList[2] * point.z + floatList[3]

def getTransformedVector3s(matrixTetragrid, vector3s):
	"Get the vector3s multiplied by a matrix."
	transformedVector3s = []
	for vector3 in vector3s:
		transformedVector3s.append(getVector3TransformedByMatrix(matrixTetragrid, vector3))
	return transformedVector3s

def getTranslateMatrixTetragrid(prefix, xmlElement):
	"Get translate matrix and delete the translate attributes."
	translation = getCumulativeVector3Remove(prefix, Vector3(), xmlElement)
	if translation.getIsDefault():
		return None
	return [[1.0, 0.0, 0.0, translation.x], [0.0, 1.0, 0.0, translation.y], [0.0, 0.0, 1.0, translation.z], [0.0, 0.0, 0.0, 1.0]]

def getVector3TransformedByMatrix(matrixTetragrid, vector3):
	"Get the vector3 multiplied by a matrix."
	if matrixTetragrid == None:
		return vector3.copy()
	return Vector3(
		getTransformedByList( matrixTetragrid[0], vector3),
		getTransformedByList( matrixTetragrid[1], vector3),
		getTransformedByList( matrixTetragrid[2], vector3))

def getVertexes(geometryOutput):
	"Get the vertexes."
	vertexes = []
	addVertexes(geometryOutput, vertexes)
	return vertexes

def matrixPoints(points, prefix, xmlElement):
	"Rotate the points."
	matrixMatrixTetragrid = matrix.getMatrixTetragrid(prefix, xmlElement)
	if matrixMatrixTetragrid == None:
		print('Warning, matrixMatrixTetragrid was None in matrix so nothing will be done for:')
		print(xmlElement)
		return
	for point in points:
		transformVector3ByMatrix(matrixMatrixTetragrid, point)

def processXMLElement(xmlElement):
	"Process the xml element."
	xmlElement.parent.object.matrix4X4.getFromXMLElement('', xmlElement)
	setAttributeDictionaryToMatrix(xmlElement.parent.attributeDictionary, xmlElement.parent.object.matrix4X4)

def setAttributeDictionaryToMatrix( attributeDictionary, matrix4X4 ):
	"Set the attribute dictionary to the matrix."
	matrixTetragrid = getIdentityMatrixTetragrid(matrix4X4.matrixTetragrid)
	for row in xrange( 4 ):
		for column in xrange( 4 ):
			key = getMatrixKey( row, column )
			attributeDictionary[key] = str( matrixTetragrid[ row ][ column ] )

def setAttributeDictionaryMatrixToMatrix(matrix4X4, xmlElement):
	"Set the element attribute dictionary and element matrix to the matrix."
	setAttributeDictionaryToMatrix(xmlElement.attributeDictionary, matrix4X4)
	if xmlElement.object != None:
		xmlElement.object.matrix4X4 = matrix4X4

def setAttributeDictionaryToMultipliedTetragrid(tetragrid, xmlElement):
	"Set the element attribute dictionary and element matrix to the matrix times the tetragrid."
	targetMatrix = getFromObjectOrXMLElement(xmlElement).getOtherTimesSelf(tetragrid)
	setAttributeDictionaryMatrixToMatrix(targetMatrix, xmlElement)

def setXMLElementDictionaryToOtherElementDictionary(fromXMLElement, matrix4X4, prefix, xmlElement):
	"Set the xml element to the matrix attribute dictionary."
	matrix4X4.getFromXMLElement(prefix, fromXMLElement)
	setAttributeDictionaryToMatrix(xmlElement.attributeDictionary, matrix4X4)

def transformVector3ByMatrix( matrixTetragrid, vector3 ):
	"Transform the vector3 by a matrix."
	vector3.setToVector3( getVector3TransformedByMatrix( matrixTetragrid, vector3 ) )


class Matrix:
	"A four by four matrix."
	def __init__(self, matrixTetragrid=None):
		"Add empty lists."
		if matrixTetragrid == None:
			self.matrixTetragrid = None
			return
		self.matrixTetragrid = getTetragridCopy(matrixTetragrid)

	def __eq__(self, other):
		"Determine whether this matrix is identical to other one."
		if other == None:
			return False
		if other.__class__ != self.__class__:
			return False
		return other.matrixTetragrid == self.matrixTetragrid

	def __ne__(self, other):
		"Determine whether this vector is not identical to other one."
		return not self.__eq__( other )

	def __repr__(self):
		"Get the string representation of this four by four matrix."
		output = cStringIO.StringIO()
		self.addXML(0, output)
		return output.getvalue()

	def addXML(self, depth, output):
		"Add xml for this object."
		attributeDictionary = self.getAttributeDictionary()
		if len(attributeDictionary) > 0:
			xml_simple_writer.addClosedXMLTag( attributeDictionary, self.__class__.__name__.lower(), depth, output )

	def getAttributeDictionary(self):
		"Get the attributeDictionary from row column attribute strings, counting from one."
		attributeDictionary = {}
		if self.matrixTetragrid == None:
			return attributeDictionary
		for row in xrange( 4 ):
			for column in xrange( 4 ):
				default = float( column == row )
				value = self.matrixTetragrid[ row ][ column ]
				if abs( value - default ) > 0.00000000000001:
					if abs(value) < 0.00000000000001:
						value = 0.0
					attributeDictionary[ getMatrixKey( row, column ) ] = value
		return attributeDictionary

	def getFromXMLElement(self, prefix, xmlElement):
		"Get the values from row column attribute strings, counting from one."
		attributeDictionary = xmlElement.attributeDictionary
		if attributeDictionary == None:
			return self
		self.matrixTetragrid = self.getOtherTimesSelf(getMatrixTetragrid(prefix, xmlElement)).matrixTetragrid
		self.matrixTetragrid = self.getOtherTimesSelf(getScaleMatrixTetragrid('scale.', xmlElement)).matrixTetragrid
		self.matrixTetragrid = self.getOtherTimesSelf(getRotateMatrixTetragrid('rotate.', xmlElement)).matrixTetragrid
		self.matrixTetragrid = self.getOtherTimesSelf(getTranslateMatrixTetragrid('translate.', xmlElement)).matrixTetragrid
		return self

	def getOtherTimesSelf(self, otherMatrixTetragrid):
		"Get this matrix reverse multiplied by the other matrix."
		if otherMatrixTetragrid == None:
			return Matrix(self.matrixTetragrid)
		if self.matrixTetragrid == None:
			return Matrix(otherMatrixTetragrid)
		return Matrix( getTetragridTimesOther( otherMatrixTetragrid, self.matrixTetragrid ) )

	def getSelfTimesOther( self, otherMatrixTetragrid ):
		"Get this matrix multiplied by the other matrix."
		if otherMatrixTetragrid == None:
			return Matrix( self.matrixTetragrid )
		if self.matrixTetragrid == None:
			return None
		return Matrix( getTetragridTimesOther( self.matrixTetragrid, otherMatrixTetragrid ) )
