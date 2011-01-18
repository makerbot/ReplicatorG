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
	functions = xmlElement.getXMLProcessor().functions
	if len(functions) < 1:
		return
	function = functions[-1]
	function.shouldReturn = True
	if xmlElement.object == None:
		if 'return' in xmlElement.attributeDictionary:
			value = xmlElement.attributeDictionary['return']
			xmlElement.object = evaluate.getEvaluatorSplitWords(value)
		else:
			xmlElement.object = []
	if len( xmlElement.object ) > 0:
		function.returnValue = evaluate.getEvaluatedExpressionValueBySplitLine( xmlElement.object, xmlElement )
