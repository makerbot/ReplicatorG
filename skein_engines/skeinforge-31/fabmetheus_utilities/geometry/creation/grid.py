"""
Grid path points.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math
import random


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def addGridRow(diameter, gridPath, loopsComplex, maximumComplex, rowIndex, x, y, zigzag):
	"Add grid row."
	row = []
	while x < maximumComplex.real:
		point = complex(x, y)
		if euclidean.getIsInFilledRegion(loopsComplex, point):
			row.append(point)
		x += diameter.real
	if zigzag and rowIndex % 2 == 1:
		row.reverse()
	gridPath += row

def getGeometryOutput(xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	inradius = lineation.getComplexByPrefixes(['demisize', 'inradius'], complex(5.0, 5.0), xmlElement)
	inradius = lineation.getComplexByMultiplierPrefix(2.0, 'size', inradius, xmlElement)
	demiwidth = lineation.getFloatByPrefixBeginEnd('demiwidth', 'width', inradius.real, xmlElement)
	demiheight = lineation.getFloatByPrefixBeginEnd('demiheight', 'height', inradius.imag, xmlElement)
	radius = lineation.getComplexByPrefixBeginEnd('elementRadius', 'elementDiameter', complex(1.0, 1.0), xmlElement)
	radius = lineation.getComplexByPrefixBeginEnd('radius', 'diameter', radius, xmlElement)
	diameter = radius + radius
	typeString = evaluate.getEvaluatedStringDefault('rectangular', 'type', xmlElement)
	typeStringTwoCharacters = typeString.lower()[: 2]
	typeStringFirstCharacter = typeStringTwoCharacters[: 1]
	zigzag = evaluate.getEvaluatedBooleanDefault(True, 'zigzag', xmlElement)
	topRight = complex(demiwidth, demiheight)
	bottomLeft = -topRight
	loopsComplex = [euclidean.getSquareLoopWiddershins(bottomLeft, topRight)]
	paths = evaluate.getTransformedPathsByKey('target', xmlElement)
	if len(paths) > 0:
		loopsComplex = euclidean.getComplexPaths(paths)
	maximumComplex = euclidean.getMaximumByPathsComplex(loopsComplex)
	minimumComplex = euclidean.getMinimumByPathsComplex(loopsComplex)
	gridPath = None
	if typeStringTwoCharacters == 'he':
		gridPath = getHexagonalGrid(diameter, loopsComplex, maximumComplex, minimumComplex, zigzag)
	elif typeStringTwoCharacters == 'ra' or typeStringFirstCharacter == 'a':
		gridPath = getRandomGrid(diameter, loopsComplex, maximumComplex, minimumComplex, xmlElement)
	elif typeStringTwoCharacters == 're' or typeStringFirstCharacter == 'e':
		gridPath = getRectangularGrid(diameter, loopsComplex, maximumComplex, minimumComplex, zigzag)
	if gridPath == None:
		print('Warning, the step type was not one of (hexagonal, random or rectangular) in getGeometryOutput in grid for:')
		print(typeString)
		print(xmlElement)
		return []
	loop = euclidean.getVector3Path(gridPath)
	xmlElement.attributeDictionary['closed'] = 'false'
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop, 0.5 * math.pi), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	if len(arguments) < 1:
		return getGeometryOutput(xmlElement)
	inradius = 0.5 * euclidean.getFloatFromValue(arguments[0])
	xmlElement.attributeDictionary['inradius.x'] = str(inradius)
	if len(arguments) > 1:
		inradius = 0.5 * euclidean.getFloatFromValue(arguments[1])
	xmlElement.attributeDictionary['inradius.y'] = str(inradius)
	return getGeometryOutput(xmlElement)

def getHexagonalGrid(diameter, loopsComplex, maximumComplex, minimumComplex, zigzag):
	"Get hexagonal grid."
	diameter = complex(diameter.real, math.sqrt(0.75) * diameter.imag)
	demiradius = 0.25 * diameter
	xRadius = 0.5 * diameter.real
	xStart = minimumComplex.real - demiradius.real
	y = minimumComplex.imag - demiradius.imag
	gridPath = []
	rowIndex = 0
	while y < maximumComplex.imag:
		x = xStart
		if rowIndex % 2 == 1:
			x -= xRadius
		addGridRow(diameter, gridPath, loopsComplex, maximumComplex, rowIndex, x, y, zigzag)
		y += diameter.imag
		rowIndex += 1
	return gridPath

def getIsPointInsideZoneAwayOthers(diameterReciprocal, loopsComplex, point, pixelDictionary):
	"Determine if the point is inside the loops zone and and away from the other points."
	if not euclidean.getIsInFilledRegion(loopsComplex, point):
		return False
	pointOverDiameter = complex(point.real * diameterReciprocal.real, point.imag * diameterReciprocal.imag)
	squareValues = euclidean.getSquareValuesFromPoint(pixelDictionary, pointOverDiameter)
	for squareValue in squareValues:
		if abs(squareValue - pointOverDiameter) < 1.0:
			return False
	euclidean.addElementToPixelListFromPoint(pointOverDiameter, pixelDictionary, pointOverDiameter)
	return True

def getRandomGrid(diameter, loopsComplex, maximumComplex, minimumComplex, xmlElement):
	"Get rectangular grid."
	packingDensity = evaluate.getEvaluatedFloatByKeys(0.2, ['packingDensity', 'density'], xmlElement)
	gridPath = []
	diameterReciprocal = complex(1.0 / diameter.real, 1.0 / diameter.imag)
	diameterSquared = diameter.real * diameter.real + diameter.imag * diameter.imag
	elements = int(math.ceil(packingDensity * euclidean.getAreaLoops(loopsComplex) / diameterSquared / math.sqrt(0.75)))
	elements = evaluate.getEvaluatedIntDefault(elements, 'elements', xmlElement)
	failedPlacementAttempts = 0
	pixelDictionary = {}
	seed = evaluate.getEvaluatedIntDefault(None, 'seed', xmlElement)
	if seed != None:
		random.seed(seed)
	successfulPlacementAttempts = 0
	while failedPlacementAttempts < 100:
		point = euclidean.getRandomComplex(minimumComplex, maximumComplex)
		if getIsPointInsideZoneAwayOthers(diameterReciprocal, loopsComplex, point, pixelDictionary):
			gridPath.append(point)
			euclidean.addElementToPixelListFromPoint(point, pixelDictionary, point)
			successfulPlacementAttempts += 1
		else:
			failedPlacementAttempts += 1
		if successfulPlacementAttempts >= elements:
			return gridPath
	return gridPath

def getRectangularGrid(diameter, loopsComplex, maximumComplex, minimumComplex, zigzag):
	"Get rectangular grid."
	demiradius = 0.25 * diameter
	xStart = minimumComplex.real - demiradius.real
	y = minimumComplex.imag - demiradius.imag
	gridPath = []
	rowIndex = 0
	while y < maximumComplex.imag:
		addGridRow(diameter, gridPath, loopsComplex, maximumComplex, rowIndex, xStart, y, zigzag)
		y += diameter.imag
		rowIndex += 1
	return gridPath

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)
