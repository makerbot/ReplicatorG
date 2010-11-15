"""
Boolean geometry utilities.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def _getAccessibleAttribute(attributeName, xmlElement):
	'Get the accessible attribute.'
	if attributeName in globalAccessibleAttributeSet:
		documentObject = Document(xmlElement)
		return getattr(documentObject, attributeName, None)
	return None


class Document:
	'Class to get handle xmlElements in a document.'
	def __init__(self, xmlElement):
		'Initialize.'
		self.xmlElement = xmlElement

	def __repr__(self):
		"Get the string representation of this Document."
		return self.xmlElement

	def getByID(self, idKey):
		"Get element by id."
		return self.getElementByID(idKey)

	def getByName(self, nameKey):
		"Get element by name."
		return self.getElementsByName(nameKey)

	def getCascadeValue(self, defaultValue, key):
		"Get cascade value."
		return self.xmlElement.getCascadeValue(defaultValue, key)

	def getElementByID(self, idKey):
		"Get element by id."
		elementByID = self.xmlElement.getXMLElementByImportID(idKey)
		if elementByID == None:
			print('Warning, could not get elementByID in getElementByID in document for:')
			print(idKey)
			print(self.xmlElement)
		return elementByID

	def getElementsByName(self, nameKey):
		"Get element by name."
		elementsByName = self.xmlElement.getXMLElementsByImportName(nameKey)
		if elementsByName == None:
			print('Warning, could not get elementsByName in getElementsByName in document for:')
			print(nameKey)
			print(self.xmlElement)
		return elementsByName

	def getParent(self):
		"Get parent element."
		return self.getParentElement()

	def getParentElement(self):
		"Get parent element."
		return self.xmlElement.parent

	def getPrevious(self):
		"Get previous element."
		return self.getPreviousElement()

	def getPreviousElement(self):
		"Get previous element."
		return self.xmlElement.getPreviousXMLElement()

	def getPreviousVertex(self):
		'Get previous element.'
		return self.xmlElement.getPreviousVertex()

	def getRoot(self):
		"Get root element."
		return self.getRootElement()

	def getRootElement(self):
		"Get root element."
		return self.xmlElement.getRoot()

	def getSelf(self):
		"Get self element."
		return self.getSelfElement()

	def getSelfElement(self):
		"Get self element."
		return self.xmlElement


globalAccessibleAttributes = 'getByID getByName getCascadeValue getElementByID getElementsByName getParent'.split()
globalAccessibleAttributes += 'getParentElement getPrevious getPreviousElement getPreviousVertex getRoot'.split()
globalAccessibleAttributes += 'getRootElement getSelf getSelfElement'.split()
globalAccessibleAttributeSet = set(globalAccessibleAttributes)
