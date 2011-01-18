"""
Polygon path.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def processXMLElement(xmlElement):
	"Process the xml element."
	if xmlElement.object == None:
		if 'condition' in xmlElement.attributeDictionary:
			value = xmlElement.attributeDictionary['condition']
			xmlElement.object = evaluate.getEvaluatorSplitWords(value)
		else:
			xmlElement.object = []
	if len( xmlElement.object ) < 1:
		return
	xmlProcessor = xmlElement.getXMLProcessor()
	if len( xmlProcessor.functions ) < 1:
		return
	function = xmlProcessor.functions[-1]
	while evaluate.getEvaluatedExpressionValueBySplitLine( xmlElement.object, xmlElement ) > 0:
		function.processChildren(xmlElement)
