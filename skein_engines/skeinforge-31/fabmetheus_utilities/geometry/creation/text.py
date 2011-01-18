"""
Text vertexes.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_tools import path
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import svg_reader


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getGeometryOutput(xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	fontFamily = evaluate.getEvaluatedStringDefault('Gentium Basic Regular', 'font-family', xmlElement)
	fontFamily = evaluate.getEvaluatedStringDefault(fontFamily, 'fontFamily', xmlElement)
	fontSize = evaluate.getEvaluatedFloatDefault(12.0, 'font-size', xmlElement)
	fontSize = evaluate.getEvaluatedFloatDefault(fontSize, 'fontSize', xmlElement)
	textString = evaluate.getEvaluatedStringDefault(xmlElement.text, 'text', xmlElement)
	if textString == '':
		print('Warning, textString is empty in getGeometryOutput in text for:')
		print(xmlElement)
		return []
	geometryOutput = []
	for textComplexLoop in svg_reader.getTextComplexLoops(fontFamily, fontSize, textString):
		textComplexLoop.reverse()
		vector3Path = euclidean.getVector3Path(textComplexLoop)
		sideLoop = lineation.SideLoop(vector3Path, None, None)
		sideLoop.rotate(xmlElement)
		geometryOutput += lineation.getGeometryOutputByManipulation(sideLoop, xmlElement)
	return geometryOutput

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['text', 'fontSize', 'fontFamily'], arguments, xmlElement)
	return getGeometryOutput(xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	path.convertProcessXMLElementRenameByPaths(getGeometryOutput(xmlElement), xmlElement)
