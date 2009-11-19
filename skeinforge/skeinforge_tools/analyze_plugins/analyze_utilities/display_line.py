"""
Display line is a mouse tool to display the line index of the line clicked, counting from one, and the line itself.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.analyze_plugins.analyze_utilities import tableau
from skeinforge_tools.skeinforge_utilities import preferences


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getNewMouseTool():
	"Get a new mouse tool."
	return DisplayLine()


class DisplayLine( tableau.MouseToolBase ):
	"Display the line when it is clicked."
	def button1( self, event, shift = False ):
		"Print line text and connection line."
		x = self.canvas.canvasx( event.x )
		y = self.canvas.canvasy( event.y )
		self.destroyEverything()
		tags = self.getTagsGivenXY( x, y )
		if tags != '':
			self.items = [ self.canvas.create_text ( x, y, anchor = preferences.Tkinter.SW, text = 'The line is: ' + tags ) ]
