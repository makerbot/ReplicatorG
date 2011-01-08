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
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GPL 3.0'


globalExecutionOrder = 300


def addVertexes(geometryOutput, vertexes):
	'Add the vertexes.'
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

def getCumulativeVector3Remove(prefix, vector3, xmlElement):
	'Get cumulative vector3 and delete the prefixed attributes.'
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
	'Get the diagonals and switched matrix.'
	unitPolar = euclidean.getWiddershinsUnitPolar(math.radians(angleDegrees))
	diagonalSwitchedTetragrid = getIdentityTetragrid()
	for diagonal in diagonals:
		diagonalSwitchedTetragrid[diagonal][diagonal] = unitPolar.real
	diagonalSwitchedTetragrid[diagonals[0]][diagonals[1]] = - unitPolar.imag
	diagonalSwitchedTetragrid[diagonals[1]][diagonals[0]] = unitPolar.imag
	return diagonalSwitchedTetragrid

def getFromObjectOrXMLElement(xmlElement):
	'Get matrix starting from the object if it exists, otherwise get a matrix starting from stratch.'
	xmlElementMatrix = None
	if xmlElement.object != None:
		xmlElementMatrix = xmlElement.object.getMatrix4X4()
		if xmlElementMatrix == None:
			return None
	else:
		xmlElementMatrix = Matrix()
	return xmlElementMatrix.getFromXMLElement('matrix.', xmlElement)

def getIdentityTetragrid(tetragrid=None):
	'Get four by four matrix with diagonal elements set to one.'
	if tetragrid == None:
		return [[1.0, 0.0, 0.0, 0.0], [0.0, 1.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]]
	return tetragrid

def getKeyA(row, column, prefix=''):
	'Get the a format key string from row & column, counting from zero.'
	return '%sa%s%s' % (prefix, row, column)

def getKeyM(row, column, prefix=''):
	'Get the m format key string from row & column, counting from one.'
	return '%sm%s%s' % (prefix, row + 1, column + 1)

def getKeysA(prefix=''):
	'Get the matrix keys, counting from zero.'
	keysA = []
	for row in xrange(4):
		for column in xrange(4):
			key = getKeyA(row, column, prefix)
			keysA.append(key)
	return keysA

def getKeysM(prefix=''):
	'Get the matrix keys, counting from one.'
	keysM = []
	for row in xrange(4):
		for column in xrange(4):
			key = getKeyM(row, column, prefix)
			keysM.append(key)
	return keysM

def getManipulatedGeometryOutput(geometryOutput, prefix, xmlElement):
	'Get equated geometryOutput.'
	matrixPoints(getVertexes(geometryOutput), prefix, xmlElement)
	return geometryOutput

def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	'Get equated paths.'
	matrixPoints(loop, prefix, xmlElement)
	return [loop]

def getRemovedFloat(defaultFloat, key, prefix, xmlElement):
	'Get the float by the key and the prefix.'
	prefixKey = prefix + key
	if prefixKey in xmlElement.attributeDictionary:
		floatValue = evaluate.getEvaluatedFloat(prefixKey, xmlElement)
		if floatValue == None:
			print('Warning, evaluated value in getRemovedFloatByKeys in matrix is None for key:')
			print(prefixKey)
			print('for xmlElement dictionary value:')
			print(xmlElement.attributeDictionary[prefixKey])
			print('for xmlElement dictionary:')
			print(xmlElement.attributeDictionary)
		else:
			defaultFloat = floatValue
		del xmlElement.attributeDictionary[prefixKey]
	return defaultFloat

def getRemovedFloatByKeys(defaultFloat, keys, prefix, xmlElement):
	'Get the float by the keys and the prefix.'
	for key in keys:
		defaultFloat = getRemovedFloat(defaultFloat, key, prefix, xmlElement)
	return defaultFloat

def getRotateAroundAxisTetragrid(prefix, xmlElement):
	'Get rotate around axis tetragrid and delete the axis and angle attributes.'
	angle = getRemovedFloatByKeys(0.0, ['angle', 'counterclockwise'], prefix, xmlElement)
	angle -= getRemovedFloat(0.0, 'clockwise', prefix, xmlElement)
	if angle == 0.0:
		return None
	angleRadians = math.radians(angle)
	axis = getCumulativeVector3Remove(prefix + 'axis.', Vector3(), xmlElement)
	axisLength = abs(axis)
	if axisLength <= 0.0:
		print('Warning, axisLength was zero in getRotateAroundAxisTetragrid in matrix so nothing will be done for:')
		print(xmlElement)
		return None
	axis /= axisLength
	tetragrid = getIdentityTetragrid()
	cosAngle = math.cos(angleRadians)
	sinAngle = math.sin(angleRadians)
	oneMinusCos = 1.0 - math.cos(angleRadians)
	xx = axis.x * axis.x
	xy = axis.x * axis.y
	xz = axis.x * axis.z
	yy = axis.y * axis.y
	yz = axis.y * axis.z
	zz = axis.z * axis.z
	tetragrid[0] = [cosAngle + xx * oneMinusCos, xy * oneMinusCos - axis.z * sinAngle, xz * oneMinusCos + axis.y * sinAngle, 0.0]
	tetragrid[1] = [xy * oneMinusCos + axis.z * sinAngle, cosAngle + yy * oneMinusCos, yz * oneMinusCos - axis.x * sinAngle, 0.0]
	tetragrid[2] = [xz * oneMinusCos - axis.y * sinAngle, yz * oneMinusCos + axis.x * sinAngle, cosAngle + zz * oneMinusCos, 0.0]
	return tetragrid

def getRotateTetragrid(prefix, xmlElement):
	'Get rotate tetragrid and delete the rotate attributes.'
	# http://en.wikipedia.org/wiki/Rotation_matrix
	rotateMatrix = Matrix()
	rotateMatrix.tetragrid = getRotateAroundAxisTetragrid(prefix, xmlElement)
	zAngle = getRemovedFloatByKeys(0.0, ['axisclockwisez', 'observerclockwisez', 'z'], prefix, xmlElement)
	zAngle -= getRemovedFloatByKeys(0.0, ['axiscounterclockwisez', 'observercounterclockwisez'], prefix, xmlElement)
	if zAngle != 0.0:
		rotateMatrix.tetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(-zAngle, [0, 1])).tetragrid
	xAngle = getRemovedFloatByKeys(0.0, ['axisclockwisex', 'observerclockwisex', 'x'], prefix, xmlElement)
	xAngle -= getRemovedFloatByKeys(0.0, ['axiscounterclockwisex', 'observercounterclockwisex'], prefix, xmlElement)
	if xAngle != 0.0:
		rotateMatrix.tetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(-xAngle, [1, 2])).tetragrid
	yAngle = getRemovedFloatByKeys(0.0, ['axiscounterclockwisey', 'observerclockwisey', 'y'], prefix, xmlElement)
	yAngle -= getRemovedFloatByKeys(0.0, ['axisclockwisey', 'observercounterclockwisey'], prefix, xmlElement)
	if yAngle != 0.0:
		rotateMatrix.tetragrid = rotateMatrix.getOtherTimesSelf(getDiagonalSwitchedTetragrid(yAngle, [0, 2])).tetragrid
	return rotateMatrix.tetragrid

def getScaleTetragrid(prefix, xmlElement):
	'Get scale matrix and delete the scale attributes.'
	scaleDefaultVector3 = Vector3(1.0, 1.0, 1.0)
	scale = getCumulativeVector3Remove(prefix, scaleDefaultVector3.copy(), xmlElement)
	if scale == scaleDefaultVector3:
		return None
	return [[scale.x, 0.0, 0.0, 0.0], [0.0, scale.y, 0.0, 0.0], [0.0, 0.0, scale.z, 0.0], [0.0, 0.0, 0.0, 1.0]]

def getTetragridA(prefix, tetragrid, xmlElement):
	'Get the tetragrid from the xmlElement letter a values.'
	keysA = getKeysA(prefix)
	evaluatedDictionary = evaluate.getEvaluatedDictionary(keysA, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return tetragrid
	for row in xrange(4):
		for column in xrange(4):
			key = getKeyA(row, column, prefix)
			if key in evaluatedDictionary:
				value = evaluatedDictionary[key]
				if value == None or value == 'None':
					print('Warning, value in getTetragridA in matrix is None for key for dictionary:')
					print(key)
					print(evaluatedDictionary)
				else:
					tetragrid = getIdentityTetragrid(tetragrid)
					tetragrid[row][column] = float(value)
	euclidean.removeElementsFromDictionary(xmlElement.attributeDictionary, keysA)
	return tetragrid

def getTetragridC(prefix, tetragrid, xmlElement):
	'Get the matrix Tetragrid from the xmlElement letter c values.'
	columnKeys = 'Pc1 Pc2 Pc3 Pc4'.replace('P', prefix).split()
	evaluatedDictionary = evaluate.getEvaluatedDictionary(columnKeys, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return tetragrid
	for columnKeyIndex, columnKey in enumerate(columnKeys):
		if columnKey in evaluatedDictionary:
			value = evaluatedDictionary[columnKey]
			if value == None or value == 'None':
				print('Warning, value in getTetragridC in matrix is None for columnKey for dictionary:')
				print(columnKey)
				print(evaluatedDictionary)
			else:
				tetragrid = getIdentityTetragrid(tetragrid)
				for elementIndex, element in enumerate(value):
					tetragrid[elementIndex][columnKeyIndex] = element
	euclidean.removeElementsFromDictionary(xmlElement.attributeDictionary, columnKeys)
	return tetragrid

def getTetragridCopy(tetragrid):
	'Get tetragrid copy.'
	tetragridCopy = []
	for tetragridRow in tetragrid:
		tetragridCopy.append(tetragridRow[:])
	return tetragridCopy

def getTetragridM(prefix, tetragrid, xmlElement):
	'Get the tetragrid from the xmlElement letter m values.'
	keysM = getKeysM(prefix)
	evaluatedDictionary = evaluate.getEvaluatedDictionary(keysM, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return tetragrid
	for row in xrange(4):
		for column in xrange(4):
			key = getKeyM(row, column, prefix)
			if key in evaluatedDictionary:
				value = evaluatedDictionary[key]
				if value == None or value == 'None':
					print('Warning, value in getTetragridM in matrix is None for key for dictionary:')
					print(key)
					print(evaluatedDictionary)
				else:
					tetragrid = getIdentityTetragrid(tetragrid)
					tetragrid[row][column] = float(value)
	euclidean.removeElementsFromDictionary(xmlElement.attributeDictionary, keysM)
	return tetragrid

def getTetragridMatrix(prefix, tetragrid, xmlElement):
	'Get the tetragrid from the xmlElement matrix value.'
	matrixKey = prefix + 'matrix'
	evaluatedDictionary = evaluate.getEvaluatedDictionary([matrixKey], xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return tetragrid
	value = evaluatedDictionary[matrixKey]
	if value == None or value == 'None':
		print('Warning, value in getTetragridMatrix in matrix is None for matrixKey for dictionary:')
		print(matrixKey)
		print(evaluatedDictionary)
	else:
		tetragrid = getIdentityTetragrid(tetragrid)
		for rowIndex, row in enumerate(value):
			for elementIndex, element in enumerate(row):
				tetragrid[rowIndex][elementIndex] = element
	euclidean.removeElementsFromDictionary(xmlElement.attributeDictionary, [matrixKey])
	return tetragrid

def getTetragridR(prefix, tetragrid, xmlElement):
	'Get the tetragrid from the xmlElement letter r values.'
	rowKeys = 'Pr1 Pr2 Pr3 Pr4'.replace('P', prefix).split()
	evaluatedDictionary = evaluate.getEvaluatedDictionary(rowKeys, xmlElement)
	if len(evaluatedDictionary.keys()) < 1:
		return tetragrid
	for rowKeyIndex, rowKey in enumerate(rowKeys):
		if rowKey in evaluatedDictionary:
			value = evaluatedDictionary[rowKey]
			if value == None or value == 'None':
				print('Warning, value in getTetragridR in matrix is None for rowKey for dictionary:')
				print(rowKey)
				print(evaluatedDictionary)
			else:
				tetragrid = getIdentityTetragrid(tetragrid)
				for elementIndex, element in enumerate(value):
					tetragrid[rowKeyIndex][elementIndex] = element
	euclidean.removeElementsFromDictionary(xmlElement.attributeDictionary, rowKeys)
	return tetragrid

def getTetragridTimesOther(firstTetragrid, otherTetragrid ):
	'Get this matrix multiplied by the other matrix.'
	#A down, B right from http://en.wikipedia.org/wiki/Matrix_multiplication
	tetragridTimesOther = []
	for row in xrange(4):
		matrixRow = firstTetragrid[row]
		tetragridTimesOtherRow = []
		tetragridTimesOther.append(tetragridTimesOtherRow)
		for column in xrange(4):
			dotProduct = 0
			for elementIndex in xrange(4):
				dotProduct += matrixRow[elementIndex] * otherTetragrid[elementIndex][column]
			tetragridTimesOtherRow.append(dotProduct)
	return tetragridTimesOther

def getTransformedByList(floatList, point):
	'Get the point transformed by the array.'
	return floatList[0] * point.x + floatList[1] * point.y + floatList[2] * point.z + floatList[3]

def getTransformedVector3s(tetragrid, vector3s):
	'Get the vector3s multiplied by a matrix.'
	transformedVector3s = []
	for vector3 in vector3s:
		transformedVector3s.append(getVector3TransformedByMatrix(tetragrid, vector3))
	return transformedVector3s

def getTransformTetragrid(prefix, xmlElement):
	'Get the tetragrid from the xmlElement.'
	tetragrid = getTetragridA(prefix, None, xmlElement)
	tetragrid = getTetragridC(prefix, tetragrid, xmlElement)
	tetragrid = getTetragridM(prefix, tetragrid, xmlElement)
	tetragrid = getTetragridMatrix(prefix, tetragrid, xmlElement)
	tetragrid = getTetragridR(prefix, tetragrid, xmlElement)
	return tetragrid

def getTranslateTetragrid(prefix, xmlElement):
	'Get translate matrix and delete the translate attributes.'
	translation = getCumulativeVector3Remove(prefix, Vector3(), xmlElement)
	if translation.getIsDefault():
		return None
	return [[1.0, 0.0, 0.0, translation.x], [0.0, 1.0, 0.0, translation.y], [0.0, 0.0, 1.0, translation.z], [0.0, 0.0, 0.0, 1.0]]

def getVector3TransformedByMatrix(tetragrid, vector3):
	'Get the vector3 multiplied by a matrix.'
	if tetragrid == None:
		return vector3.copy()
	return Vector3(
		getTransformedByList(tetragrid[0], vector3),
		getTransformedByList(tetragrid[1], vector3),
		getTransformedByList(tetragrid[2], vector3))

def getVertexes(geometryOutput):
	'Get the vertexes.'
	vertexes = []
	addVertexes(geometryOutput, vertexes)
	return vertexes

def matrixPoints(points, prefix, xmlElement):
	'Matrix the points.'
	matrixTetragrid = getTransformTetragrid(prefix, xmlElement)
	if matrixTetragrid == None:
		print('Warning, matrixTetragrid was None in matrix so nothing will be done for:')
		print(xmlElement)
		return
	for point in points:
		transformVector3ByMatrix(matrixTetragrid, point)

def processXMLElement(xmlElement):
	'Process the xml element.'
	xmlElement.parent.object.matrix4X4.getFromXMLElement('', xmlElement)
	setAttributeDictionaryToMatrix(xmlElement.parent.attributeDictionary, xmlElement.parent.object.matrix4X4)

def setAttributeDictionaryToMatrix(attributeDictionary, matrix4X4):
	'Set the attribute dictionary to the matrix.'
	if matrix4X4 == None:
		return
	tetragrid = getIdentityTetragrid(matrix4X4.tetragrid)
	for row in xrange(4):
		for column in xrange(4):
			key = getKeyM(row, column)
			attributeDictionary[key] = str(tetragrid[row][column])

def setAttributeDictionaryMatrixToMatrix(matrix4X4, xmlElement):
	'Set the element attribute dictionary and element matrix to the matrix.'
	setAttributeDictionaryToMatrix(xmlElement.attributeDictionary, matrix4X4)
	if xmlElement.object != None:
		xmlElement.object.matrix4X4 = matrix4X4

def setAttributeDictionaryToMultipliedTetragrid(tetragrid, xmlElement):
	'Set the element attribute dictionary and element matrix to the matrix times the tetragrid.'
	targetMatrix = getFromObjectOrXMLElement(xmlElement).getOtherTimesSelf(tetragrid)
	setAttributeDictionaryMatrixToMatrix(targetMatrix, xmlElement)

def setXMLElementDictionaryToOtherElementDictionary(fromXMLElement, matrix4X4, prefix, xmlElement):
	'Set the xml element to the matrix attribute dictionary.'
	if matrix4X4 == None:
		return
	matrix4X4.getFromXMLElement(prefix, fromXMLElement)
	setAttributeDictionaryToMatrix(xmlElement.attributeDictionary, matrix4X4)

def transformVector3ByMatrix( tetragrid, vector3 ):
	'Transform the vector3 by a matrix.'
	vector3.setToVector3(getVector3TransformedByMatrix(tetragrid, vector3))


class Matrix:
	'A four by four matrix.'
	def __init__(self, tetragrid=None):
		'Add empty lists.'
		if tetragrid == None:
			self.tetragrid = None
			return
		self.tetragrid = getTetragridCopy(tetragrid)

	def __eq__(self, other):
		'Determine whether this matrix is identical to other one.'
		if other == None:
			return False
		if other.__class__ != self.__class__:
			return False
		return other.tetragrid == self.tetragrid

	def __ne__(self, other):
		'Determine whether this vector is not identical to other one.'
		return not self.__eq__(other)

	def __repr__(self):
		'Get the string representation of this four by four matrix.'
		output = cStringIO.StringIO()
		self.addXML(0, output)
		return output.getvalue()

	def addXML(self, depth, output):
		'Add xml for this object.'
		attributeDictionary = self.getAttributeDictionary()
		if len(attributeDictionary) > 0:
			xml_simple_writer.addClosedXMLTag(attributeDictionary, self.__class__.__name__.lower(), depth, output)

	def getAttributeDictionary(self):
		'Get the attributeDictionary from row column attribute strings, counting from one.'
		attributeDictionary = {}
		if self.tetragrid == None:
			return attributeDictionary
		for row in xrange(4):
			for column in xrange(4):
				default = float(column == row)
				value = self.tetragrid[row][column]
				if abs( value - default ) > 0.00000000000001:
					if abs(value) < 0.00000000000001:
						value = 0.0
					attributeDictionary[getKeyM(row, column)] = value
		return attributeDictionary

	def getFromXMLElement(self, prefix, xmlElement):
		'Get the values from row column attribute strings, counting from one.'
		attributeDictionary = xmlElement.attributeDictionary
		if attributeDictionary == None:
			return self
		self.tetragrid = self.getOtherTimesSelf(getTransformTetragrid(prefix, xmlElement)).tetragrid
		self.tetragrid = self.getOtherTimesSelf(getScaleTetragrid('scale.', xmlElement)).tetragrid
		self.tetragrid = self.getOtherTimesSelf(getRotateTetragrid('rotate.', xmlElement)).tetragrid
		self.tetragrid = self.getOtherTimesSelf(getTranslateTetragrid('translate.', xmlElement)).tetragrid
		return self

	def getOtherTimesSelf(self, otherTetragrid):
		'Get this matrix reverse multiplied by the other matrix.'
		if otherTetragrid == None:
			return Matrix(self.tetragrid)
		if self.tetragrid == None:
			return Matrix(otherTetragrid)
		return Matrix(getTetragridTimesOther(otherTetragrid, self.tetragrid))

	def getSelfTimesOther(self, otherTetragrid):
		'Get this matrix multiplied by the other matrix.'
		if otherTetragrid == None:
			return Matrix(self.tetragrid)
		if self.tetragrid == None:
			return None
		return Matrix(getTetragridTimesOther(self.tetragrid, otherTetragrid))
