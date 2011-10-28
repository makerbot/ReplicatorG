"""
Boolean geometry utilities.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


def _getAccessibleAttribute(attributeName, elementNode):
	'Get the accessible attribute.'
	if attributeName in globalGetAccessibleAttributeSet:
		return getattr(Setting(elementNode), attributeName, None)
	return None

def getCascadeFloatWithoutSelf(defaultFloat, elementNode, key):
	'Get the cascade float.'
	if key in elementNode.attributes:
		value = elementNode.attributes[key]
		functionName = 'get' + key[0].upper() + key[1 :]
		if functionName in value:
			if elementNode.parentNode == None:
				return defaultFloat
			else:
				elementNode = elementNode.parentNode
	return elementNode.getCascadeFloat(defaultFloat, key)

def getImportRadius(elementNode):
	'Get the importRadius.'
	if elementNode == None:
		return 0.6
	return getCascadeFloatWithoutSelf(1.5 * getLayerThickness(elementNode), elementNode, 'importRadius')

def getInteriorOverhangAngle(elementNode):
	'Get the interior overhang support angle in degrees.'
	return getCascadeFloatWithoutSelf(30.0, elementNode, 'interiorOverhangAngle')

def getInteriorOverhangRadians(elementNode):
	'Get the interior overhang support angle in radians.'
	return math.radians(getInteriorOverhangAngle(elementNode))

def getLayerThickness(elementNode):
	'Get the layer thickness.'
	if elementNode == None:
		return 0.4
	return getCascadeFloatWithoutSelf(0.4, elementNode, 'layerThickness')

def getOverhangAngle(elementNode):
	'Get the overhang support angle in degrees.'
	return getCascadeFloatWithoutSelf(45.0, elementNode, 'overhangAngle')

def getOverhangRadians(elementNode):
	'Get the overhang support angle in radians.'
	return math.radians(getOverhangAngle(elementNode))

def getOverhangSpan(elementNode):
	'Get the overhang span.'
	return getCascadeFloatWithoutSelf(2.0 * getLayerThickness(elementNode), elementNode, 'overhangSpan')

def getPrecision(elementNode):
	'Get the cascade precision.'
	return getCascadeFloatWithoutSelf(0.2 * getLayerThickness(elementNode), elementNode, 'precision')

def getSheetThickness(elementNode):
	'Get the sheet thickness.'
	return getCascadeFloatWithoutSelf(3.0, elementNode, 'sheetThickness')

def getTwistPrecision(elementNode):
	'Get the twist precision in degrees.'
	return getCascadeFloatWithoutSelf(5.0, elementNode, 'twistPrecision')

def getTwistPrecisionRadians(elementNode):
	'Get the twist precision in radians.'
	return math.radians(getTwistPrecision(elementNode))


class Setting:
	'Class to get handle elementNodes in a setting.'
	def __init__(self, elementNode):
		'Initialize.'
		self.elementNode = elementNode

	def __repr__(self):
		'Get the string representation of this Setting.'
		return self.elementNode

	def getImportRadius(self):
		'Get the importRadius.'
		return getImportRadius(self.elementNode)

	def getInteriorOverhangAngle(self):
		'Get the interior overhang support angle in degrees.'
		return getInteriorOverhangAngle(self.elementNode)

	def getInteriorOverhangRadians(self):
		'Get the interior overhang support angle in radians.'
		return getInteriorOverhangRadians(self.elementNode)

	def getLayerThickness(self):
		'Get the layer thickness.'
		return getLayerThickness(self.elementNode)

	def getOverhangAngle(self):
		'Get the overhang support angle in degrees.'
		return getOverhangAngle(self.elementNode)

	def getOverhangRadians(self):
		'Get the overhang support angle in radians.'
		return getOverhangRadians(self.elementNode)

	def getOverhangSpan(self):
		'Get the overhang span.'
		return getOverhangSpan(self.elementNode)

	def getPrecision(self):
		'Get the cascade precision.'
		return getPrecision(self.elementNode)

	def getSheetThickness(self):
		'Get the sheet thickness.'
		return getSheetThickness(self.elementNode)

	def getTwistPrecision(self):
		'Get the twist precision in degrees.'
		return getTwistPrecision(self.elementNode)

	def getTwistPrecisionRadians(self):
		'Get the twist precision in radians.'
		return getTwistPrecisionRadians(self.elementNode)


globalAccessibleAttributeDictionary = 'getImportRadius getInteriorOverhangAngle getInteriorOverhangRadians'.split()
globalAccessibleAttributeDictionary += 'getLayerThickness getOverhangSpan getOverhangAngle getOverhangRadians'.split()
globalAccessibleAttributeDictionary += 'getPrecision getSheetThickness getTwistPrecision getTwistPrecisionRadians'.split()
globalGetAccessibleAttributeSet = set(globalAccessibleAttributeDictionary)
