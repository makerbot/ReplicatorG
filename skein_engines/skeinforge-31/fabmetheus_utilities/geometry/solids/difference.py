"""
Boolean geometry difference of solids.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_utilities import booleansolid
from fabmetheus_utilities.geometry.solids import group


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def convertXMLElement(geometryOutput, xmlElement):
	"Convert the xml element to a difference xml element."
	xmlElement.getXMLProcessor().createChildren(geometryOutput['shapes'], xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	group.processShape(Difference, xmlElement)


class Difference( booleansolid.BooleanSolid ):
	"A difference object."
	def getLoopsFromObjectLoopsList( self, importRadius, visibleObjectLoopsList ):
		"Get loops from visible object loops list."
		return self.getDifference( importRadius, visibleObjectLoopsList )

	def getXMLClassName(self):
		"Get xml class name."
		return self.__class__.__name__.lower()
