"""
Boolean geometry transform.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import solid
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.geometry_utilities import matrix
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


globalExecutionOrder = 320


def getManipulatedGeometryOutput(elementNode, geometryOutput, prefix):
	'Get equated geometryOutput.'
	transformPoints(elementNode, matrix.getVertexes(geometryOutput), prefix)
	return geometryOutput

def getManipulatedPaths(close, elementNode, loop, prefix, sideLength):
	'Get equated paths.'
	transformPoints(elementNode, loop, prefix)
	return [loop]

def manipulateElementNode(elementNode, target):
	'Manipulate the xml element.'
	transformTetragrid = matrix.getTransformTetragrid(elementNode, '')
	if transformTetragrid == None:
		print('Warning, transformTetragrid was None in transform so nothing will be done for:')
		print(elementNode)
		return
	matrix.setAttributesToMultipliedTetragrid(target, transformTetragrid)

def processElementNode(elementNode):
	'Process the xml element.'
	solid.processElementNodeByFunction(elementNode, manipulateElementNode)

def transformPoints(elementNode, points, prefix):
	'Transform the points.'
	transformTetragrid = matrix.getTransformTetragrid(elementNode, prefix)
	if transformTetragrid == None:
		print('Warning, transformTetragrid was None in transform so nothing will be done for:')
		print(elementNode)
		return
	for point in points:
		matrix.transformVector3ByMatrix(transformTetragrid, point)
