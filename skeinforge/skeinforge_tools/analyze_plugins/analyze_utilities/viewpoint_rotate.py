"""
Viewpoint rotate is a mouse tool to rotate the viewpoint around the origin, when the mouse is clicked and dragged on the canvas.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.analyze_plugins.analyze_utilities import tableau
from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import preferences
import math

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getBoundedLatitude( latitude ):
	"Get the bounded latitude.later get rounded"
	return round( min( 179.9, max( 0.1, latitude ) ), 1 )

def getNewMouseTool():
	"Get a new mouse tool."
	return ViewpointRotate()


class LatitudeLongitude:
	"A latitude and longitude."
	def __init__( self, buttonOnePressedCanvasCoordinate, newCoordinate, skeinWindow, shift ):
		"Set the latitude and longitude."
		buttonOnePressedCentered = skeinWindow.getCenteredScreened( buttonOnePressedCanvasCoordinate )
		buttonOnePressedRadius = abs( buttonOnePressedCentered )
		buttonOnePressedComplexMirror = complex( buttonOnePressedCentered.real, - buttonOnePressedCentered.imag )
		buttonOneReleasedCentered = skeinWindow.getCenteredScreened( newCoordinate )
		buttonOneReleasedRadius = abs( buttonOneReleasedCentered )
		pressedReleasedRotationComplex = buttonOneReleasedCentered * buttonOnePressedComplexMirror
		self.deltaLatitude = math.degrees( buttonOneReleasedRadius - buttonOnePressedRadius )
		self.originalDeltaLongitude = math.degrees( math.atan2( pressedReleasedRotationComplex.imag, pressedReleasedRotationComplex.real ) )
		self.deltaLongitude = self.originalDeltaLongitude
		if skeinWindow.repository.viewpointLatitude.value > 90.0:
			self.deltaLongitude = - self.deltaLongitude
		if shift:
			if abs( self.deltaLatitude ) > abs( self.deltaLongitude ):
				self.deltaLongitude = 0.0
			else:
				self.deltaLatitude = 0.0
		self.latitude = getBoundedLatitude( skeinWindow.repository.viewpointLatitude.value + self.deltaLatitude )
		self.longitude = round( ( skeinWindow.repository.viewpointLongitude.value + self.deltaLongitude ) % 360.0, 1 )


class ViewVectors:
	def __init__( self, viewpointLatitude, viewpointLongitude ):
		"Initialize the view vectors."
		longitudeComplex = euclidean.getPolar( math.radians( 90.0 - viewpointLongitude ) )
		self.viewpointLatitudeRatio = euclidean.getPolar( math.radians( viewpointLatitude ) )
		self.viewpointVector3 = Vector3( self.viewpointLatitudeRatio.imag * longitudeComplex.real, self.viewpointLatitudeRatio.imag * longitudeComplex.imag, self.viewpointLatitudeRatio.real )
		self.viewXVector3 = Vector3( - longitudeComplex.imag, longitudeComplex.real, 0.0 )
		self.viewXVector3.normalize()
		self.viewYVector3 = self.viewpointVector3.cross( self.viewXVector3 )
		self.viewYVector3.normalize()


class ViewpointRotate( tableau.MouseToolBase ):
	"Display the line when it is clicked."
	def button1( self, event, shift = False ):
		"Print line text and connection line."
		self.destroyEverything()
		x = self.canvas.canvasx( event.x )
		y = self.canvas.canvasy( event.y )
		self.buttonOnePressedCanvasCoordinate = complex( x, y )

	def buttonRelease1( self, event, shift = False ):
		"The left button was released, <ButtonRelease-1> function."
		if self.buttonOnePressedCanvasCoordinate == None:
			return
		x = self.canvas.canvasx( event.x )
		y = self.canvas.canvasy( event.y )
		buttonOneReleasedCanvasCoordinate = complex( x, y )
		if abs( self.buttonOnePressedCanvasCoordinate - buttonOneReleasedCanvasCoordinate ) < 3:
			self.buttonOnePressedCanvasCoordinate = None
			self.canvas.delete( 'motion' )
			return
		latitudeLongitude = LatitudeLongitude( self.buttonOnePressedCanvasCoordinate, buttonOneReleasedCanvasCoordinate, self.window, shift )
		self.repository.viewpointLatitude.value = latitudeLongitude.latitude
		self.repository.viewpointLatitude.setStateToValue()
		self.repository.viewpointLongitude.value = latitudeLongitude.longitude
		self.repository.viewpointLongitude.setStateToValue()
		self.buttonOnePressedCanvasCoordinate = None
		preferences.writePreferences( self.repository )
		self.window.update()
		self.destroyEverything()

	def destroyEverything( self ):
		"Destroy items."
		self.buttonOnePressedCanvasCoordinate = None
		self.destroyItems()

	def getReset( self, window ):
		"Reset the mouse tool to default."
		self.setCanvasItems( window.canvas )
		self.buttonOnePressedCanvasCoordinate = None
		self.repository = window.repository
		self.window = window
		return self

	def motion( self, event, shift = False ):
		"Move the viewpoint if the mouse was moved."
		if self.buttonOnePressedCanvasCoordinate == None:
			return
		x = self.canvas.canvasx( event.x )
		y = self.canvas.canvasy( event.y )
		motionCoordinate = complex( x, y )
		latitudeLongitude = LatitudeLongitude( self.buttonOnePressedCanvasCoordinate, motionCoordinate, self.window, shift )
		viewVectors = ViewVectors( latitudeLongitude.latitude, latitudeLongitude.longitude )
		motionCentered = self.window.getCentered( motionCoordinate )
		motionCenteredNormalized = motionCentered / abs( motionCentered )
		buttonOnePressedCentered = self.window.getCentered( self.buttonOnePressedCanvasCoordinate )
		buttonOnePressedAngle = math.degrees( math.atan2( buttonOnePressedCentered.imag, buttonOnePressedCentered.real ) )
		buttonOnePressedLength = abs( buttonOnePressedCentered )
		buttonOnePressedCorner = complex( buttonOnePressedLength, buttonOnePressedLength )
		buttonOnePressedCornerBottomLeft = self.window.getScreenComplex( - buttonOnePressedCorner )
		buttonOnePressedCornerUpperRight = self.window.getScreenComplex( buttonOnePressedCorner )
		motionPressedStart = buttonOnePressedLength * motionCenteredNormalized
		motionPressedScreen = self.window.getScreenComplex( motionPressedStart )
		motionColorName = '#4B0082'
		motionWidth = 9
		self.canvas.delete( 'motion' )
		if abs( latitudeLongitude.deltaLongitude ) > 0.0:
			self.canvas.create_arc(
				buttonOnePressedCornerBottomLeft.real,
				buttonOnePressedCornerBottomLeft.imag,
				buttonOnePressedCornerUpperRight.real,
				buttonOnePressedCornerUpperRight.imag,
				extent = latitudeLongitude.originalDeltaLongitude,
				start = buttonOnePressedAngle,
				outline = motionColorName,
				outlinestipple = self.window.motionStippleName,
				style = preferences.Tkinter.ARC,
				tags = 'motion',
				width = motionWidth )
		if abs( latitudeLongitude.deltaLatitude ) > 0.0:
			self.canvas.create_line(
				motionPressedScreen.real,
				motionPressedScreen.imag,
				x,
				y,
				fill = motionColorName,
				arrow = 'last',
				arrowshape = self.window.arrowshape,
				stipple = self.window.motionStippleName,
				tags = 'motion',
				width = motionWidth )
		if self.repository.widthOfXAxis.value > 0:
			self.window.drawColoredLineMotion( self.window.xAxisLine, viewVectors, self.repository.widthOfXAxis.value )
		if self.repository.widthOfYAxis.value > 0:
			self.window.drawColoredLineMotion( self.window.yAxisLine, viewVectors, self.repository.widthOfYAxis.value )
		if self.repository.widthOfZAxis.value > 0:
			self.window.drawColoredLineMotion( self.window.zAxisLine, viewVectors, self.repository.widthOfZAxis.value )
