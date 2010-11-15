"""
Equation for vertexes.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


globalExecutionOrder = - 100


def equateCylindrical( point, returnValue ):
	"Get equation for cylindrical."
	point = evaluate.getVector3ByFloatList( returnValue, point )
	azimuthComplex = euclidean.getWiddershinsUnitPolar( math.radians( point.y ) ) * point.x
	point.x = azimuthComplex.real
	point.y = azimuthComplex.imag

def equateCylindricalDotAzimuth( point, returnValue ):
	"Get equation for cylindrical azimuth."
	azimuthComplex = euclidean.getWiddershinsUnitPolar( math.radians( returnValue ) ) * abs( point.dropAxis() )
	point.x = azimuthComplex.real
	point.y = azimuthComplex.imag

def equateCylindricalDotRadius( point, returnValue ):
	"Get equation for cylindrical radius."
	originalRadius = abs( point.dropAxis() )
	if originalRadius > 0.0:
		radiusMultipler = returnValue / originalRadius
		point.x *= radiusMultipler
		point.y *= radiusMultipler

def equateCylindricalDotZ( point, returnValue ):
	"Get equation for cylindrical z."
	point.z = returnValue

def equatePoints( points, prefix, revolutions, xmlElement ):
	"Equate the points."
	equateVertexesByFunction( equateCylindrical, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateCylindricalDotAzimuth, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateCylindricalDotRadius, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateCylindricalDotZ, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equatePolar, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equatePolarDotAzimuth, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equatePolarDotRadius, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateRectangular, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateRectangularDotX, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateRectangularDotY, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateRectangularDotZ, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateSpherical, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateSphericalDotAzimuth, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateSphericalDotElevation, points, prefix, revolutions, xmlElement )
	equateVertexesByFunction( equateSphericalDotRadius, points, prefix, revolutions, xmlElement )

def equatePolar( point, returnValue ):
	"Get equation for polar."
	equateCylindrical( point, returnValue )

def equatePolarDotAzimuth( point, returnValue ):
	"Get equation for polar azimuth."
	equateCylindricalDotAzimuth( point, returnValue )

def equatePolarDotRadius( point, returnValue ):
	"Get equation for polar radius."
	equateCylindricalDotRadius( point, returnValue )

def equateRectangular( point, returnValue ):
	"Get equation for rectangular."
	point.setToVector3( evaluate.getVector3ByDictionaryListValue( returnValue, point ) )

def equateRectangularDotX( point, returnValue ):
	"Get equation for rectangular x."
	point.x = returnValue

def equateRectangularDotY( point, returnValue ):
	"Get equation for rectangular y."
	point.y = returnValue

def equateRectangularDotZ( point, returnValue ):
	"Get equation for rectangular z."
	point.z = returnValue

def equateSpherical( point, returnValue ):
	"Get equation for spherical."
	spherical = evaluate.getVector3ByFloatList( returnValue, point )
	radius = spherical.x
	elevationComplex = euclidean.getWiddershinsUnitPolar( math.radians( spherical.z ) ) * radius
	azimuthComplex = euclidean.getWiddershinsUnitPolar( math.radians( spherical.y ) ) * elevationComplex.real
	point.x = azimuthComplex.real
	point.y = azimuthComplex.imag
	point.z = elevationComplex.imag

def equateSphericalDotAzimuth( point, returnValue ):
	"Get equation for spherical azimuth."
	azimuthComplex = euclidean.getWiddershinsUnitPolar( math.radians( returnValue ) ) * abs( point.dropAxis() )
	point.x = azimuthComplex.real
	point.y = azimuthComplex.imag

def equateSphericalDotElevation( point, returnValue ):
	"Get equation for spherical elevation."
	radius = abs(point)
	if radius <= 0.0:
		return
	azimuthComplex = point.dropAxis()
	azimuthRadius = abs( azimuthComplex )
	if azimuthRadius <= 0.0:
		return
	elevationComplex = euclidean.getWiddershinsUnitPolar( math.radians( returnValue ) )
	azimuthComplex *= radius / azimuthRadius * elevationComplex.real
	point.x = azimuthComplex.real
	point.y = azimuthComplex.imag
	point.z = elevationComplex.imag * radius

def equateSphericalDotRadius( point, returnValue ):
	"Get equation for spherical radius."
	originalRadius = abs(point)
	if originalRadius > 0.0:
		point *= returnValue / originalRadius

def equateVertexesByFunction( equationFunction, points, prefix, revolutions, xmlElement ):
	"Get equated points by equation function."
	prefixedEquationName = prefix + equationFunction.__name__[ len('equate') : ].replace('Dot', '.').lower()
	if prefixedEquationName not in xmlElement.attributeDictionary:
		return
	equationResult = EquationResult( prefixedEquationName, revolutions, xmlElement )
	for point in points:
		returnValue = equationResult.getReturnValue(point)
		if returnValue == None:
			print('Warning, returnValue in alterVertexesByEquation in equation is None for:')
			print(point)
			print(xmlElement)
		else:
			equationFunction( point, returnValue )
	equationResult.function.reset()

def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	"Get equated paths."
	equatePoints( loop, prefix, 0.0, xmlElement )
	return [loop]

def getManipulatedGeometryOutput(geometryOutput, xmlElement):
	"Get equated geometryOutput."
	equatePoints( matrix.getConnectionVertexes(geometryOutput), 'equation.', None, xmlElement )
	return geometryOutput


class EquationResult:
	"Class to get equation results."
	def __init__( self, key, revolutions, xmlElement ):
		"Initialize."
		self.distance = 0.0
		self.function = evaluate.Function( evaluate.getEvaluatorSplitWords(xmlElement.attributeDictionary[key]), xmlElement )
		self.points = []
		self.revolutions = revolutions

	def getReturnValue(self, point):
		"Get return value."
		if self.function == None:
			return point
		self.function.localDictionary['azimuth'] = math.degrees(math.atan2(point.y, point.x))
		if len(self.points) > 0:
			self.distance += abs(point - self.points[-1])
		self.function.localDictionary['distance'] = self.distance
		self.function.localDictionary['radius'] = abs(point.dropAxis())
		if self.revolutions != None:
			if len( self.points ) > 0:
				self.revolutions += 0.5 / math.pi * euclidean.getAngleAroundZAxisDifference(point, self.points[-1])
			self.function.localDictionary['revolutions'] = self.revolutions
		self.function.localDictionary['vertex'] = point
		self.function.localDictionary['vertexes'] = self.points
		self.function.localDictionary['vertexindex'] = len(self.points)
		self.function.localDictionary['x'] = point.x
		self.function.localDictionary['y'] = point.y
		self.function.localDictionary['z'] = point.z
		self.points.append(point)
		return self.function.getReturnValueWithoutDeletion()
