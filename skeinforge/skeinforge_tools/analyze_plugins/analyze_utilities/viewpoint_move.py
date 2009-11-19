"""
Viewpoint move is a mouse tool to move the viewpoint in the xy plane when the mouse is clicked and dragged on the canvas.

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
	return ViewpointMove()


class ViewpointMove( tableau.MouseToolBase ):
	"Display the line when it is clicked."
	def button1( self, event, shift = False ):
		"Print line text and connection line."
		self.destroyEverything()
		self.buttonOnePressedScreenCoordinate = complex( event.x, event.y )
		self.scrollPaneCenter = self.window.getScrollPaneCenter()

	def buttonRelease1( self, event, shift = False ):
		"The left button was released, <ButtonRelease-1> function."
		self.destroyEverything()

	def destroyEverything( self ):
		"Destroy items."
		self.buttonOnePressedScreenCoordinate = None
		self.destroyItems()

	def getReset( self, window ):
		"Reset the mouse tool to default."
		self.setCanvasItems( window.canvas )
		self.buttonOnePressedScreenCoordinate = None
		self.window = window
		return self

	def motion( self, event, shift = False ):
		"The mouse moved, <Motion> function."
		if self.buttonOnePressedScreenCoordinate == None:
			return
		motionCoordinate = complex( event.x, event.y )
		relativeMotionCoordinate = motionCoordinate - self.buttonOnePressedScreenCoordinate
		relativeScrollPaneMotionX = - relativeMotionCoordinate.real / float( self.window.screenSize.real )
		relativeScrollPaneMotionY = - relativeMotionCoordinate.imag / float( self.window.screenSize.imag )
		self.window.relayXview( preferences.Tkinter.MOVETO, self.scrollPaneCenter.real * self.window.oneMinusCanvasWidthOverScreenWidth + relativeScrollPaneMotionX )
		self.window.relayYview( preferences.Tkinter.MOVETO, self.scrollPaneCenter.imag * self.window.oneMinusCanvasHeightOverScreenHeight + relativeScrollPaneMotionY )
