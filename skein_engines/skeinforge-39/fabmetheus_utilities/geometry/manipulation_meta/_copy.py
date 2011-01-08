"""
Boolean geometry copy.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.manipulation_matrix import matrix
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import euclidean


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GPL 3.0'


def processXMLElement(xmlElement):
	'Process the xml element.'
	target = evaluate.getXMLElementByKey('target', xmlElement)
	if target == None:
		print('Warning, copy could not get target.')
		return
	del xmlElement.attributeDictionary['target']
	copyMatrix = matrix.getFromObjectOrXMLElement(xmlElement)
	targetMatrix = matrix.getFromObjectOrXMLElement(target)
	targetDictionaryCopy = target.attributeDictionary.copy()
	euclidean.removeElementsFromDictionary(targetDictionaryCopy, ['id', 'name'])
	targetDictionaryCopy.update(xmlElement.attributeDictionary)
	xmlElement.attributeDictionary = targetDictionaryCopy
	euclidean.removeTrueFromDictionary(xmlElement.attributeDictionary, 'visible')
	xmlElement.className = target.className
	target.copyXMLChildren(xmlElement.getIDSuffix(), xmlElement)
	xmlElement.getXMLProcessor().processXMLElement(xmlElement)
	if copyMatrix != None and targetMatrix != None:
		xmlElement.object.matrix4X4 = copyMatrix.getSelfTimesOther(targetMatrix.tetragrid)
