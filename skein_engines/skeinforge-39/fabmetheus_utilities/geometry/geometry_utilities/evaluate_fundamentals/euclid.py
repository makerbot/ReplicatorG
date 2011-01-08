"""
Boolean geometry utilities.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities.vector3index import Vector3Index
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def _getAccessibleAttribute(attributeName):
	'Get the accessible attribute.'
	if attributeName in globalAccessibleAttributeDictionary:
		return globalAccessibleAttributeDictionary[attributeName]
	return None


def getComplex(x=0.0, y=0.0):
	'Get the complex.'
	return complex(x, y)

def getNestedVectorTestExample(x=0.0, y=0.0, z=0.0):
	'Get the NestedVectorTestExample.'
	return NestedVectorTestExample(Vector3(x, y, z))

def getVector3(x=0.0, y=0.0, z=0.0):
	'Get the vector3.'
	return Vector3(x, y, z)

def getVector3Index(index=0, x=0.0, y=0.0, z=0.0):
	'Get the vector3.'
	return Vector3Index(index, x, y, z)


class NestedVectorTestExample:
	'Class to test local attribute.'
	def __init__(self, vector3):
		'Get the accessible attribute.'
		self.vector3 = vector3

	def _getAccessibleAttribute(self, attributeName):
		"Get the accessible attribute."
		if attributeName == 'vector3':
			return getattr(self, attributeName, None)
		return None


globalAccessibleAttributeDictionary = {
	'complex' : getComplex,
	'NestedVectorTestExample' : getNestedVectorTestExample,
	'Vector3' : getVector3,
	'Vector3Index' : getVector3Index}
