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


def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = TextDerivation()
		derivation.setToXMLElement(xmlElement)
	if derivation.textString == '':
		print('Warning, textString is empty in getGeometryOutput in text for:')
		print(xmlElement)
		return []
	geometryOutput = []
	for textComplexLoop in svg_reader.getTextComplexLoops(derivation.fontFamily, derivation.fontSize, derivation.textString):
		textComplexLoop.reverse()
		vector3Path = euclidean.getVector3Path(textComplexLoop)
		sideLoop = lineation.SideLoop(vector3Path, None, None)
		sideLoop.rotate(xmlElement)
		geometryOutput += lineation.getGeometryOutputByManipulation(sideLoop, xmlElement)
	return geometryOutput

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['text', 'fontSize', 'fontFamily'], arguments, xmlElement)
	return getGeometryOutput(None, xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	path.convertProcessXMLElementRenameByPaths(getGeometryOutput(None, xmlElement), xmlElement)


class TextDerivation:
	"Class to hold text variables."
	def __init__(self):
		'Set defaults.'
		self.fontFamily = 'Gentium Basic Regular'
		self.fontSize = 12.0
		self.textString = ''

	def __repr__(self):
		"Get the string representation of this TextDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.fontFamily = evaluate.getEvaluatedStringDefault(self.fontFamily, 'font-family', xmlElement)
		self.fontFamily = evaluate.getEvaluatedStringDefault(self.fontFamily, 'fontFamily', xmlElement)
		self.fontSize = evaluate.getEvaluatedFloatDefault(self.fontSize, 'font-size', xmlElement)
		self.fontSize = evaluate.getEvaluatedFloatDefault(self.fontSize, 'fontSize', xmlElement)
		if self.textString == '':
			self.textString = xmlElement.text
		self.textString = evaluate.getEvaluatedStringDefault(self.textString, 'text', xmlElement)
