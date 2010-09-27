"""
Boolean geometry translation.

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


globalExecutionOrder = 380


def getManipulatedPaths(close, loop, prefix, sideLength, xmlElement):
	"Get equated paths."
	translatePoints( loop, prefix, xmlElement )
	return [loop]

def getManipulatedGeometryOutput(geometryOutput, xmlElement):
	"Get equated geometryOutput."
	translatePoints( matrix.getConnectionVertexes(geometryOutput), 'translate.', xmlElement )
	return geometryOutput

def manipulateXMLElement(target, xmlElement):
	"Manipulate the xml element."
	translateMatrixTetragrid = matrix.getTranslateMatrixTetragrid('', xmlElement)
	if translateMatrixTetragrid == None:
		print('Warning, translateMatrixTetragrid was None in translate so nothing will be done for:')
		print(xmlElement)
		return
	matrix.setAttributeDictionaryToMultipliedTetragrid(translateMatrixTetragrid, target)

def processXMLElement(xmlElement):
	"Process the xml element."
	solid.processXMLElementByFunction(manipulateXMLElement, xmlElement)

def translatePoints(points, prefix, xmlElement):
	"Translate the points."
	translateVector3 = matrix.getCumulativeVector3Remove(prefix, Vector3(), xmlElement)
	if abs(translateVector3) > 0.0:
		euclidean.translateVector3Path(points, translateVector3)
