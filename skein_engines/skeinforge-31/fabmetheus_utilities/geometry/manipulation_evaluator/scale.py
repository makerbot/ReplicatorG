"""
Boolean geometry scale.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import solid
from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


globalExecutionOrder = 340


def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	"Get equated paths."
	scalePoints( loop, prefix, xmlElement )
	return [loop]

def getManipulatedGeometryOutput(geometryOutput, xmlElement):
	"Get equated geometryOutput."
	scalePoints( matrix.getConnectionVertexes(geometryOutput), 'scale.', xmlElement )
	return geometryOutput

def manipulateXMLElement(target, xmlElement):
	"Manipulate the xml element."
	scaleMatrixTetragrid = matrix.getScaleMatrixTetragrid('', xmlElement)
	if scaleMatrixTetragrid == None:
		print('Warning, scaleMatrixTetragrid was None in scale so nothing will be done for:')
		print(xmlElement)
		return
	matrix.setAttributeDictionaryToMultipliedTetragrid(scaleMatrixTetragrid, target)

def processXMLElement(xmlElement):
	"Process the xml element."
	solid.processXMLElementByFunction( manipulateXMLElement, xmlElement)

def scalePoints(points, prefix, xmlElement):
	"Scale the points."
	scaleDefaultVector3 = Vector3(1.0, 1.0, 1.0)
	scaleVector3 = matrix.getCumulativeVector3Remove(prefix, scaleDefaultVector3.copy(), xmlElement)
	if scaleVector3 == scaleDefaultVector3:
		return
	for point in points:
		point.x *= scaleVector3.x
		point.y *= scaleVector3.y
		point.z *= scaleVector3.z
