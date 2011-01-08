"""
Boolean geometry write.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.manipulation_matrix import matrix
from fabmetheus_utilities import archive
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
	folderName = evaluate.getEvaluatedStringDefault('', 'folder', xmlElement)
	absoluteFolderDirectory = os.path.join(os.path.dirname(xmlElement.getRoot().parser.fileName), folderName)
	absoluteFileName = os.path.abspath(os.path.join(absoluteFolderDirectory, fileName))
	if 'models/' not in absoluteFileName:
		print('Warning, models/ was not in the absolute file path, so for security nothing will be done for:')
		print(xmlElement)
		print('For which the absolute file path is:')
		print(absoluteFileName)
		print('The write tool can only write a file which has models/ in the file path.')
		print('To write the file, move the file into a folder called model/ or a subfolder which is inside the model folder tree.')
		return
	fileNames.append(fileName)
	archive.makeDirectory(absoluteFolderDirectory)
	if not evaluate.getEvaluatedBooleanDefault(True, 'writeMatrix', xmlElement):
		object.matrix4X4 = matrix.Matrix()
	print('The write tool generated the file:')
	print(absoluteFileName)
	archive.writeFileText(absoluteFileName, object.getFabricationText())
