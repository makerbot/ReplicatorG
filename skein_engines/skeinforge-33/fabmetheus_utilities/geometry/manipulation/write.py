"""
Boolean geometry write.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
import os

__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def processXMLElement(xmlElement):
	"Process the xml element."
	targets = evaluate.getXMLElementsByKey('target', xmlElement )
	if len(targets) < 1:
		print('Warning, processXMLElement in write could not get targets for:')
		print(xmlElement)
		return
	fileNames = []
	for target in targets:
		writeXMLElement(fileNames, target, xmlElement)

def writeXMLElement(fileNames, target, xmlElement):
	"Write target."
	object = target.object
	if object == None:
		print('Warning, writeTarget in write could not get object for:')
		print(xmlElement)
		return
	fileNameRoot = evaluate.getEvaluatedStringDefault('', 'name', target)
	fileNameRoot = evaluate.getEvaluatedStringDefault(fileNameRoot, 'id', target)
	fileNameRoot = evaluate.getEvaluatedStringDefault(fileNameRoot, 'file', xmlElement)
	fileNameRoot += evaluate.getEvaluatedStringDefault('', 'suffix', xmlElement)
	extension = evaluate.getEvaluatedStringDefault(object.getFabricationExtension(), 'extension', xmlElement)
	fileName = '%s.%s' % (fileNameRoot, extension)
	suffixIndex = 1
	while fileName in fileNames:
		fileName = '%s_%s.%s' % (fileNameRoot, suffixIndex, extension)
		suffixIndex += 1
	fileNames.append(fileName)
	folderName = evaluate.getEvaluatedStringDefault('', 'folder', xmlElement)
	absoluteFolderDirectory = os.path.join(os.path.dirname(xmlElement.getRoot().parser.fileName), folderName)
	archive.makeDirectory(absoluteFolderDirectory)
	archive.writeFileText(os.path.join(absoluteFolderDirectory, fileName), object.getFabricationText())
