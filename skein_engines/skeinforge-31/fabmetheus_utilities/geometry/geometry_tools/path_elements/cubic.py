"""
Cubic vertexes.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import svg_reader


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getCubicPath(xmlElement):
	"Get the cubic path."
	end = evaluate.getVector3FromXMLElement(xmlElement)
	previousXMLElement = xmlElement.getPreviousXMLElement()
	if previousXMLElement == None:
		print('Warning, can not get previousXMLElement in getCubicPath in cubic for:')
		print(xmlElement)
		return [end]
	begin = xmlElement.getPreviousVertex(Vector3())
	controlPoint0 = evaluate.getVector3ByPrefix('controlPoint0', None, xmlElement)
	if controlPoint0 == None:
		oldControlPoint = evaluate.getVector3ByPrefixes(['controlPoint','controlPoint1'], None, previousXMLElement)
		if oldControlPoint == None:
			print('Warning, can not get oldControlPoint in getCubicPath in cubic for:')
			print(xmlElement)
			return [end]
		controlPoint0 = begin + begin - oldControlPoint
	controlPoints = [controlPoint0, evaluate.getVector3ByPrefix('controlPoint1', None, xmlElement)]
	return svg_reader.getCubicPoints(begin, controlPoints, end, lineation.getNumberOfBezierPoints(begin, end, xmlElement))

def processXMLElement(xmlElement):
	"Process the xml element."
	xmlElement.parent.object.vertexes += getCubicPath(xmlElement)
