"""
Create outline.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import intercircle


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GNU Affero General Public License http://www.gnu.org/licenses/agpl.html'


globalExecutionOrder = 80


def getManipulatedPaths(close, elementNode, loop, prefix, sideLength):
	"Get path with outline."
	if len(loop) < 2:
		return [loop]
	isClosed = evaluate.getEvaluatedBoolean(False, elementNode, prefix + 'closed')
	radius = lineation.getStrokeRadiusByPrefix(elementNode, prefix )
	loopComplex = euclidean.getComplexPath(loop)
	if isClosed:
		loopComplexes = intercircle.getAroundsFromLoop(loopComplex, radius)
	else:
		loopComplexes = intercircle.getAroundsFromPath(loopComplex, radius)
	return euclidean.getVector3Paths(loopComplexes, loop[0].z)

def processElementNode(elementNode):
	"Process the xml element."
	lineation.processElementNodeByFunction(elementNode, getManipulatedPaths)
