"""
Boolean geometry utilities.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


globalNativeFunctions = 'acos asin atan atan2 ceil cos cosh degrees e exp fabs floor fmod frexp hypot'.split()
globalNativeFunctions += 'ldexp log log10 modf pi pow radians sin sinh sqrt tan tanh trunc'.split()
globalNativeFunctionSet = set(globalNativeFunctions)
#Constants from: http://www.physlink.com/reference/MathConstants.cfm
#If anyone wants to add stuff, more constants are at: http://en.wikipedia.org/wiki/Mathematical_constant
globalMathConstantDictionary = {
	'euler' : 0.5772156649015328606065120,
	'golden' : euclidean.globalGoldenRatio,
	'goldenAngle' : euclidean.globalGoldenAngle,
	'goldenRatio' : euclidean.globalGoldenRatio}


def _getAccessibleAttribute(attributeName):
	'Get the accessible attribute.'
	if attributeName in globalMathConstantDictionary:
		return globalMathConstantDictionary[attributeName]
	if attributeName in globalNativeFunctionSet:
		return math.__dict__[attributeName]
	if attributeName in globalAccessibleAttributeDictionary:
		return globalAccessibleAttributeDictionary[attributeName]
	return None


def getAbs(value):
	'Get the abs.'
	return abs(value)

def getComplex(x, y):
	'Get the complex.'
	return complex(x, y)

def getDivmod(x, y):
	'Get the divmod.'
	return divmod(x, y)

def getFloat(value):
	'Get the float.'
	return float(value)

def getHex(value):
	'Get the hex.'
	return hex(value)

def getInt(value):
	'Get the int.'
	return int(value)

def getLong(value):
	'Get the long.'
	return long(value)

def getMax(value):
	'Get the max.'
	return max(value)

def getMin(value):
	'Get the min.'
	return min(value)

def getRound(value):
	'Get the round.'
	return round(value)


globalAccessibleAttributeDictionary = {
	'abs' : getAbs,
	'complex' : getComplex,
	'divmod' : getDivmod,
	'float' : getFloat,
	'hex' : getHex,
	'int' : getInt,
	'long' : getLong,
	'max' : getMax,
	'min' : getMin,
	'round' : getRound}
