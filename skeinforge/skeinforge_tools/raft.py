"""
Raft is a script to create a reusable raft, elevate the nozzle and set the temperature.

The default 'Activate Raft' checkbox is on.  When it is on, the functions described below will work, when it is off, the
functions will not be called.  The raft script sets the temperature.  If the "Activate Raft, Elevate Nozzle, Orbit and Set
Altitude" checkbox is checked, the script will also create a raft, elevate the nozzle, orbit and set the altitude of the bottom
of the raft.

Raft is based on the Nophead's reusable raft, which has a base layer running one way, and a couple of perpendicular layers
above.  Each set of layers can be set to a different temperature.  There is the option of having the extruder orbit the raft for a
while, so the heater barrel has time to reach a different temperature, without ooze accumulating around the nozzle.  To run
raft, in a shell type:
> python raft.py

The important values for the raft preferences are the temperatures of the raft, the first layer and the next layers.  These will be
different for each material.  The default preferences for ABS, HDPE, PCL & PLA are extrapolated from Nophead's
experiments.  To change the material, in a shell type:
> python material.py

This brings up the material preferences dialog.  In that dialog you can add or delete a material on the listbox and you change
the selected material.  After you can change the selected material, run raft again.  If there are preferences for the new material,
those will be in the raft dialog.  If there are no preferences for the new material, the preferences will be set to defaults and you
will have to set new preferences for the new material.

The "Base Infill Density" preference is the infill density ratio of the base of the raft, the default ratio is half.  The "Base Layer
Height over Layer Thickness" preference is the ratio of the height & width of the base layer compared to the height and width
of the shape infill, the default is two.  The feedrate will be slower for raft layers which have thicker extrusions than the shape
infill.  The "Base Layers" preference is the number of base layers, the default is one.  The "Base Nozzle Lift over Half Base
Layer Thickness" is the amount the nozzle is above the center of the extrusion divided by half the base layer thickness.

The interface of the raft has equivalent preferences called "Interface Infill Density", "Interface Layer Thickness over Extrusion
Height", "Interface Layers" and "Interface Nozzle Lift over Half Base Layer Thickness".  The shape has the equivalent
preference of called "Operating Nozzle Lift over Half Layer Thickness".

The altitude that the bottom of the raft will be set to the "Bottom Altitude" preference.

The raft fills a rectangle whose size is the rectangle around the bottom layer of the shape expanded on each side by the
"Raft Outset Radius over Extrusion Width" preference times the extrusion width, minus the "Infill Overhang" ratio times the
width of the extrusion of the raft.

In the "Support Material Choice" radio button group, if "No Support Material" is selected then raft will not add support
material, this is the default because the raft takes time to generate.  If "Support Material Everywhere" is selected, support
material will be added wherever there are overhangs, even inside the object; because support material inside objects is hard
or impossible to remove, this option should only be chosen if the shape has a cavity that needs support and there is some
way to extract the support material.  If "Support Material on Exterior Only" is selected, support material will be added only
the exterior of the object; this is the best option for most objects which require support material.  The "Support Minimum
Angle" preference is the minimum angle that a surface overhangs before support material is added, the default is sixty
degrees. The "Support Inset over Perimeter Extrusion Width" is the amount that the support material is inset into the object
over the perimeter extrusion width, the default is zero.

The extruder will orbit for at least "Temperature Change Time Before Raft" seconds before extruding the raft.  It will orbit for
at least "Temperature Change Time Before First Layer Outline" seconds before extruding the outline of the first layer of the
shape.  It will orbit for at least "Temperature Change Time Before Next Threads" seconds before extruding within the outline
of the first layer of the shape and before extruding the next layers of the shape.  It will orbit for at least "Temperature
Change Time Before Support Layers" seconds before extruding the support layers.  It will orbit for at least "Temperature
Change Time Before Supported Layers" seconds before extruding the layer of the shape above the support layer.  If a time
is zero, it will not orbit.

The "Temperature of Raft" preference sets the temperature of the raft.  The "Temperature of Shape First Layer Outline"
preference sets the temperature of the outline of the first layer of the shape.  The "Temperature of Shape First Layer Within"
preference sets the temperature within the outline of the first layer of the shape.  The "Temperature of Shape Next Layers"
preference sets the temperature of the next layers of the shape.  The "Temperature of Support Layers" preference sets the
temperature of the support layer.  The "Temperature of Supported Layers" preference sets the temperature of the layer of the
shape above the support layer.

The following examples raft the files Screw Holder Bottom.gcode & Screw Holder Bottom.stl.  The examples are run in a terminal in the folder
which contains Screw Holder Bottom.gcode, Screw Holder Bottom.stl and raft.py.  The raft function will raft if "Activate Raft, Elevate Nozzle,
Orbit and Set Altitude" is true, which can be set in the dialog or by changing the preferences file 'raft.csv' with a text editor or a
spreadsheet program set to separate tabs.  The functions writeOutput and getRaftChainGcode check to see if the text has
been rafted, if not they call getSpeedChainGcode in speed.py to get speeded gcode; once they have the speeded text, then
they raft.  Pictures of rafting in action are available from the Metalab blog at:
http://reprap.soup.io/?search=rafting


> python raft.py
This brings up the dialog, after clicking 'Raft', the following is printed:
File Screw Holder Bottom.stl is being chain rafted.
The rafted file is saved as Screw Holder Bottom_raft.gcode


>python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import raft
>>> raft.main()
This brings up the raft dialog.


>>> raft.writeOutput()
Screw Holder Bottom.stl
File Screw Holder Bottom.stl is being chain rafted.
The rafted file is saved as Screw Holder Bottom_raft.gcode


>>> raft.getRaftGcode("
( GCode generated by May 8, 2008 carve.py )
( Extruder Initialization )
..
many lines of gcode
..
")


>>> raft.getRaftChainGcode("
( GCode generated by May 8, 2008 carve.py )
( Extruder Initialization )
..
many lines of gcode
..
")

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import intercircle
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools import analyze
from skeinforge_tools.skeinforge_utilities import interpret
from skeinforge_tools import material
from skeinforge_tools import polyfile
from skeinforge_tools import speed
import cStringIO
import math
import sys
import time


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


#inside and outside inset loops basically around loops
#maybe later wide support
def addXIntersectionsFromSegment( index, segment, xIntersectionIndexList ):
	"Add the x intersections from the segment."
	for endpoint in segment:
		xIntersectionIndexList.append( euclidean.XIntersectionIndex( index, endpoint.point.real ) )

def addXIntersectionsFromSegments( index, segments, xIntersectionIndexList ):
	"Add the x intersections from the segments."
	for segment in segments:
		addXIntersectionsFromSegment( index, segment, xIntersectionIndexList )

def getEndpointsFromSegments( segments ):
	"Get the endpoints from the segments."
	endpoints = []
	for segment in segments:
		for endpoint in segment:
			endpoints.append( endpoint )
	return endpoints

def getExtendedLineSegment( extensionDistance, lineSegment ):
	"Add shortened line segment."
	pointBegin = lineSegment[ 0 ].point
	pointEnd = lineSegment[ 1 ].point
	segment = pointEnd - pointBegin
	segmentLength = abs( segment )
	if segmentLength <= 0.0:
		print( "This should never happen in getExtendedLineSegment in raft, the segment should have a length greater than zero." )
		print( lineSegment )
		return None
	segmentExtend = segment * extensionDistance / segmentLength
	lineSegment[ 0 ].point = pointBegin - segmentExtend
	lineSegment[ 1 ].point = pointEnd + segmentExtend
	return lineSegment

def getFillXIntersectionIndexes( fillLoops, y ):
	"Get fill x intersection indexes inside loops."
	xIntersectionIndexList = []
	euclidean.addXIntersectionIndexesFromLoops( fillLoops, 0, xIntersectionIndexList, y )
	return xIntersectionIndexList

def getHorizontalSegments( fillLoops, alreadyFilledArounds, y ):
	"Get horizontal segments inside loops."
	xIntersectionIndexList = []
	euclidean.addXIntersectionIndexesFromLoops( fillLoops, - 1, xIntersectionIndexList, y )
	euclidean.addXIntersectionIndexesFromLoops( alreadyFilledArounds, 0, xIntersectionIndexList, y )
	return euclidean.getSegmentsFromXIntersectionIndexes( xIntersectionIndexList, y )

def getJoinOfXIntersectionIndexes( xIntersectionIndexList ):
	"Get x intersections from surrounding layers."
	xIntersectionList = []
	solidTable = {}
	solid = False
	xIntersectionIndexList.sort()
	for xIntersectionIndex in xIntersectionIndexList:
		euclidean.toggleHashtable( solidTable, xIntersectionIndex.index, "" )
		oldSolid = solid
		solid = len( solidTable ) > 0
		if oldSolid != solid:
			xIntersectionList.append( xIntersectionIndex.x )
	return xIntersectionList

#raft outline temperature http://hydraraptor.blogspot.com/2008/09/screw-top-pot.html
def getRaftChainGcode( fileName, gcodeText, raftPreferences = None ):
	"Raft a gcode linear move text.  Chain raft the gcode if it is not already rafted."
	gcodeText = gcodec.getGcodeFileText( fileName, gcodeText )
	if not gcodec.isProcedureDone( gcodeText, 'speed' ):
		gcodeText = speed.getSpeedChainGcode( fileName, gcodeText )
	return getRaftGcode( gcodeText, raftPreferences )

def getRaftGcode( gcodeText, raftPreferences = None ):
	"Raft a gcode linear move text."
	if gcodeText == '':
		return ''
	if gcodec.isProcedureDone( gcodeText, 'raft' ):
		return gcodeText
	if raftPreferences == None:
		raftPreferences = RaftPreferences()
		preferences.readPreferences( raftPreferences )
	if not raftPreferences.activateRaft.value:
		return gcodeText
	skein = RaftSkein()
	skein.parseGcode( gcodeText, raftPreferences )
	return skein.output.getvalue()

def getSquareLoop( beginComplex, endComplex ):
	"Get a square loop from the beginning to the end and back."
	loop = [ beginComplex ]
	loop.append( complex( beginComplex.real, endComplex.imag ) )
	loop.append( endComplex )
	loop.append( complex( endComplex.real, beginComplex.imag ) )
	return loop

def joinSegmentTables( fromTable, intoTable ):
	"Join both segment tables and put the join into the intoTable."
	intoTableKeys = intoTable.keys()
	fromTableKeys = fromTable.keys()
	joinedKeyTable = {}
	concatenatedSegmentTableKeys = intoTableKeys + fromTableKeys
	for concatenatedSegmentTableKey in concatenatedSegmentTableKeys:
		joinedKeyTable[ concatenatedSegmentTableKey ] = None
	joinedKeys = joinedKeyTable.keys()
	joinedKeys.sort()
	joinedSegmentTable = {}
	for joinedKey in joinedKeys:
		xIntersectionIndexList = []
		if joinedKey in intoTable:
			addXIntersectionsFromSegments( 0, intoTable[ joinedKey ], xIntersectionIndexList )
		if joinedKey in fromTable:
			addXIntersectionsFromSegments( 1, fromTable[ joinedKey ], xIntersectionIndexList )
		xIntersections = getJoinOfXIntersectionIndexes( xIntersectionIndexList )
		lineSegments = euclidean.getSegmentsFromXIntersections( xIntersections, joinedKey )
		if len( lineSegments ) > 0:
			intoTable[ joinedKey ] = lineSegments
		else:
			print( "This should never happen, there are no line segments in joinSegments in raft." )

def subtractFill( fillXIntersectionIndexTable, supportSegmentLayerTable ):
	"Subtract fill from the support layer table."
	supportSegmentLayerTableKeys = supportSegmentLayerTable.keys()
	supportSegmentLayerTableKeys.sort()
	if len( supportSegmentLayerTableKeys ) < 1:
		return
	for supportSegmentLayerTableKey in supportSegmentLayerTableKeys:
		xIntersectionIndexList = []
		addXIntersectionsFromSegments( - 1, supportSegmentLayerTable[ supportSegmentLayerTableKey ], xIntersectionIndexList )
		if supportSegmentLayerTableKey in fillXIntersectionIndexTable:
			addXIntersectionsFromSegments( 0, fillXIntersectionIndexTable[ supportSegmentLayerTableKey ], xIntersectionIndexList )
		lineSegments = euclidean.getSegmentsFromXIntersectionIndexes( xIntersectionIndexList, supportSegmentLayerTableKey )
		if len( lineSegments ) > 0:
			supportSegmentLayerTable[ supportSegmentLayerTableKey ] = lineSegments
		else:
			del supportSegmentLayerTable[ supportSegmentLayerTableKey ]

def writeOutput( fileName = '' ):
	"""Raft a gcode linear move file.  Chain raft the gcode if it is not already rafted.
	If no fileName is specified, raft the first unmodified gcode file in this folder."""
	if fileName == '':
		unmodified = interpret.getGNUTranslatorFilesUnmodified()
		if len( unmodified ) == 0:
			print( "There are no unmodified gcode files in this folder." )
			return
		fileName = unmodified[ 0 ]
	raftPreferences = RaftPreferences()
	preferences.readPreferences( raftPreferences )
	startTime = time.time()
	print( 'File ' + gcodec.getSummarizedFilename( fileName ) + ' is being chain rafted.' )
	suffixFilename = fileName[ : fileName.rfind( '.' ) ] + '_raft.gcode'
	raftGcode = getRaftChainGcode( fileName, '', raftPreferences )
	if raftGcode == '':
		return
	gcodec.writeFileText( suffixFilename, raftGcode )
	print( 'The rafted file is saved as ' + gcodec.getSummarizedFilename( suffixFilename ) )
	analyze.writeOutput( suffixFilename, raftGcode )
	print( 'It took ' + str( int( round( time.time() - startTime ) ) ) + ' seconds to raft the file.' )


class RaftPreferences:
	"A class to handle the raft preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		materialName = material.getSelectedMaterial()
		#Set the default preferences.
		self.archive = []
		self.activateRaft = preferences.BooleanPreference().getFromValue( 'Activate Raft:', True )
		self.archive.append( self.activateRaft )
		self.addRaftElevateNozzleOrbitSetAltitude = preferences.BooleanPreference().getFromValue( 'Add Raft, Elevate Nozzle, Orbit and Set Altitude:', True )
		self.archive.append( self.addRaftElevateNozzleOrbitSetAltitude )
		self.baseInfillDensity = preferences.FloatPreference().getFromValue( 'Base Infill Density (ratio):', 0.5 )
		self.archive.append( self.baseInfillDensity )
		self.baseLayerThicknessOverLayerThickness = preferences.FloatPreference().getFromValue( 'Base Layer Thickness over Layer Thickness:', 2.0 )
		self.archive.append( self.baseLayerThicknessOverLayerThickness )
		self.baseLayers = preferences.IntPreference().getFromValue( 'Base Layers (integer):', 1 )
		self.archive.append( self.baseLayers )
		self.baseNozzleLiftOverHalfBaseLayerThickness = preferences.FloatPreference().getFromValue( 'Base Nozzle Lift over Half Base Layer Thickness (ratio):', 0.75 )
		self.archive.append( self.baseNozzleLiftOverHalfBaseLayerThickness )
		self.bottomAltitude = preferences.FloatPreference().getFromValue( 'Bottom Altitude:', 0.0 )
		self.archive.append( self.bottomAltitude )
		self.fileNameInput = preferences.Filename().getFromFilename( interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File to be Rafted', '' )
		self.archive.append( self.fileNameInput )
		self.infillOverhang = preferences.FloatPreference().getFromValue( 'Infill Overhang (ratio):', 0.1 )
		self.archive.append( self.infillOverhang )
		self.interfaceInfillDensity = preferences.FloatPreference().getFromValue( 'Interface Infill Density (ratio):', 0.5 )
		self.archive.append( self.interfaceInfillDensity )
		self.interfaceLayerThicknessOverLayerThickness = preferences.FloatPreference().getFromValue( 'Interface Layer Thickness over Layer Thickness:', 1.0 )
		self.archive.append( self.interfaceLayerThicknessOverLayerThickness )
		self.interfaceLayers = preferences.IntPreference().getFromValue( 'Interface Layers (integer):', 2 )
		self.archive.append( self.interfaceLayers )
		self.interfaceNozzleLiftOverHalfInterfaceLayerThickness = preferences.FloatPreference().getFromValue( 'Interface Nozzle Lift over Half Interface Layer Thickness (ratio):', 1.0 )
		self.archive.append( self.interfaceNozzleLiftOverHalfInterfaceLayerThickness )
		self.material = preferences.LabelDisplay().getFromName( 'Material: ' + materialName )
		self.archive.append( self.material )
		self.operatingNozzleLiftOverHalfLayerThickness = preferences.FloatPreference().getFromValue( 'Operating Nozzle Lift over Half Layer Thickness (ratio):', 1.0 )
		self.archive.append( self.operatingNozzleLiftOverHalfLayerThickness )
		self.raftOutsetRadiusOverExtrusionWidth = preferences.FloatPreference().getFromValue( 'Raft Outset Radius over Extrusion Width (ratio):', 15.0 )
		self.archive.append( self.raftOutsetRadiusOverExtrusionWidth )
		self.supportInsetOverPerimeterExtrusionWidth = preferences.FloatPreference().getFromValue( 'Support Inset over Perimeter Extrusion Width (ratio):', 0.0 )
		self.archive.append( self.supportInsetOverPerimeterExtrusionWidth )
		supportRadio = []
		self.supportChoiceLabel = preferences.LabelDisplay().getFromName( 'Support Material Choice: ' )
		self.archive.append( self.supportChoiceLabel )
		self.supportChoiceNoSupportMaterial = preferences.Radio().getFromRadio( 'No Support Material', supportRadio, True )
		self.archive.append( self.supportChoiceNoSupportMaterial )
		self.supportChoiceSupportMateriaEverywhere = preferences.Radio().getFromRadio( 'Support Material Everywhere', supportRadio, False )
		self.archive.append( self.supportChoiceSupportMateriaEverywhere )
		self.supportChoiceSupportMaterialOnExteriorOnly = preferences.Radio().getFromRadio( 'Support Material on Exterior Only', supportRadio, False )
		self.archive.append( self.supportChoiceSupportMaterialOnExteriorOnly )
		self.supportMinimumAngle = preferences.FloatPreference().getFromValue( 'Support Minimum Angle (degrees):', 60.0 )
		self.archive.append( self.supportMinimumAngle )
		self.temperatureChangeBeforeTimeRaft = preferences.FloatPreference().getFromValue( 'Temperature Change Time Before Raft (seconds):', 120.0 )
		self.archive.append( self.temperatureChangeBeforeTimeRaft )
		self.temperatureChangeTimeBeforeFirstLayerOutline = preferences.FloatPreference().getFromValue( 'Temperature Change Time Before First Layer Outline (seconds):', 120.0 )
		self.archive.append( self.temperatureChangeTimeBeforeFirstLayerOutline )
		self.temperatureChangeTimeBeforeNextThreads = preferences.FloatPreference().getFromValue( 'Temperature Change Time Before Next Threads (seconds):', 120.0 )
		self.archive.append( self.temperatureChangeTimeBeforeNextThreads )
		self.temperatureChangeTimeBeforeSupportLayers = preferences.FloatPreference().getFromValue( 'Temperature Change Time Before Support Layers (seconds):', 120.0 )
		self.archive.append( self.temperatureChangeTimeBeforeSupportLayers )
		self.temperatureChangeTimeBeforeSupportedLayers = preferences.FloatPreference().getFromValue( 'Temperature Change Time Before Supported Layers (seconds):', 120.0 )
		self.archive.append( self.temperatureChangeTimeBeforeSupportedLayers )
		self.temperatureRaft = preferences.FloatPreference().getFromValue( 'Temperature of Raft (Celcius):', 200.0 )
		self.archive.append( self.temperatureRaft )
		self.temperatureShapeFirstLayerOutline = preferences.FloatPreference().getFromValue( 'Temperature of Shape First Layer Outline (Celcius):', 220.0 )
		self.archive.append( self.temperatureShapeFirstLayerOutline )
		self.temperatureShapeFirstLayerWithin = preferences.FloatPreference().getFromValue( 'Temperature of Shape First Layer Within (Celcius):', 195.0 )
		self.archive.append( self.temperatureShapeFirstLayerWithin )
		self.temperatureShapeNextLayers = preferences.FloatPreference().getFromValue( 'Temperature of Shape Next Layers (Celcius):', 230.0 )
		self.archive.append( self.temperatureShapeNextLayers )
		self.temperatureShapeSupportLayers = preferences.FloatPreference().getFromValue( 'Temperature of Support Layers (Celcius):', 200.0 )
		self.archive.append( self.temperatureShapeSupportLayers )
		self.temperatureShapeSupportedLayers = preferences.FloatPreference().getFromValue( 'Temperature of Supported Layers (Celcius):', 230.0 )
		self.archive.append( self.temperatureShapeSupportedLayers )
		self.windowPositionRaftPreferences = preferences.WindowPosition().getFromValue( 'windowPositionRaft Preferences', '0+0' )
		self.archive.append( self.windowPositionRaftPreferences )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Raft'
		self.fileNamePreferences = preferences.getPreferencesFilePath( 'raft_' + materialName + '.csv' )
		self.fileNameHelp = 'skeinforge_tools.raft.html'
		self.saveTitle = 'Save Preferences'
		self.title = 'Raft Preferences'

	def execute( self ):
		"Raft button has been clicked."
		fileNames = polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )


class RaftSkein:
	"A class to raft a skein of extrusions."
	def __init__( self ):
		self.addLineLayerStart = True
		self.boundaryLayers = []
		self.decimalPlacesCarried = 3
		self.layerThickness = 0.4
		self.extrusionStart = True
		self.extrusionTop = 0.0
		self.extrusionWidth = 0.6
		self.feedrateMinute = 961.0
		self.interfaceStepsUntilEnd = []
		self.isFirstLayerWithinTemperatureAdded = False
		self.isStartupEarly = False
		self.isSurroundingLoop = True
		self.layerIndex = - 1
		self.layerStarted = False
		self.lineIndex = 0
		self.lines = None
		self.oldFlowrateString = None
		self.oldLocation = None
		self.operatingFlowrateString = None
		self.operatingLayerEndLine = '(<operatingLayerEnd> </operatingLayerEnd>)'
		self.operatingJump = None
		self.output = cStringIO.StringIO()
		self.supportFlowrateString = None
		self.supportLoops = []
		self.supportSegmentTables = []

	def addBaseLayer( self, baseExtrusionWidth, baseStep, stepBegin, stepEnd ):
		"Add a base layer."
		baseLayerThickness = self.layerThickness * self.baseLayerThicknessOverLayerThickness
		halfBaseLayerThickness = 0.5 * baseLayerThickness
		halfBaseExtrusionWidth = 0.5 * baseExtrusionWidth
		stepsUntilEnd = self.getStepsUntilEnd( stepBegin.real + halfBaseExtrusionWidth, stepEnd.real, baseStep )
		baseOverhang = self.raftPreferences.infillOverhang.value * halfBaseExtrusionWidth - halfBaseExtrusionWidth
		beginY = stepBegin.imag - baseOverhang
		endY = stepEnd.imag + baseOverhang
		segments = []
		zCenter = self.extrusionTop + halfBaseLayerThickness
		z = zCenter + halfBaseLayerThickness * self.raftPreferences.baseNozzleLiftOverHalfBaseLayerThickness.value
		for x in stepsUntilEnd:
			begin = complex( x, beginY )
			end = complex( x, endY )
			segments.append( euclidean.getSegmentFromPoints( begin, end ) )
		if len( segments ) < 1:
			print( 'This should never happen, the base layer has a size of zero.' )
			return
		self.addLayerFromSegments( self.feedrateMinute / self.baseLayerThicknessOverLayerThickness / self.baseLayerThicknessOverLayerThickness, baseLayerThickness, segments, z )

	def addFlowrateLineIfNecessary( self, flowrateString ):
		"Add a line of flowrate if different."
		if flowrateString == self.oldFlowrateString:
			return
		if flowrateString != None:
			self.addLine( 'M108 S' + flowrateString ) # Set flowrate.
		self.oldFlowrateString = flowrateString

	def addGcodeFromFeedrateThreadZ( self, feedrateMinute, thread, z ):
		"Add a thread to the output."
		if len( thread ) > 0:
			self.addGcodeFromFeedrateMovementZ( self.travelFeedratePerMinute, thread[ 0 ], z )
		else:
			print( "zero length vertex positions array which was skipped over, this should never happen" )
		if len( thread ) < 2:
			return
		self.addLine( "M101" ) # Turn extruder on.
		for point in thread[ 1 : ]:
			self.addGcodeFromFeedrateMovementZ( feedrateMinute, point, z )
		self.addLine( "M103" ) # Turn extruder off.

	def addGcodeFromFeedrateMovementZ( self, feedrateMinute, point, z ):
		"Add a movement to the output."
		self.addLine( self.getGcodeFromFeedrateMovementZ( feedrateMinute, point, z ) )

	def addInterfaceLayer( self ):
		"Add an interface layer."
		interfaceLayerThickness = self.layerThickness * self.interfaceLayerThicknessOverLayerThickness
		halfInterfaceLayerThickness = 0.5 * interfaceLayerThickness
		segments = []
		zCenter = self.extrusionTop + halfInterfaceLayerThickness
		z = zCenter + halfInterfaceLayerThickness * self.raftPreferences.interfaceNozzleLiftOverHalfInterfaceLayerThickness.value
		for y in self.interfaceStepsUntilEnd:
			begin = complex( self.interfaceBeginX, y )
			end = complex( self.interfaceEndX, y )
			segments.append( euclidean.getSegmentFromPoints( begin, end ) )
		if len( segments ) < 1:
			print( 'This should never happen, the interface layer has a size of zero.' )
			return
		self.addLayerFromSegments( self.feedrateMinute / self.interfaceLayerThicknessOverLayerThickness / self.interfaceLayerThicknessOverLayerThickness, interfaceLayerThickness, segments, z )

	def addLayerFromSegments( self, feedrateMinute, layerLayerThickness, segments, z ):
		"Add a layer from segments and raise the extrusion top."
		firstSegment = segments[ 0 ]
		nearestPoint = firstSegment[ 1 ].point
		path = [ firstSegment[ 0 ].point, nearestPoint ]
		for segment in segments[ 1 : ]:
			segmentBegin = segment[ 0 ]
			segmentEnd = segment[ 1 ]
			nextEndpoint = segmentBegin
			if abs( nearestPoint - segmentBegin.point ) > abs( nearestPoint - segmentEnd.point ):
				nextEndpoint = segmentEnd
			path.append( nextEndpoint.point )
			nextEndpoint = nextEndpoint.otherEndpoint
			nearestPoint = nextEndpoint.point
			path.append( nearestPoint )
		self.addLayerLine( z )
		self.addGcodeFromFeedrateThreadZ( feedrateMinute, path, z )
		self.extrusionTop += layerLayerThickness

	def addLayerLine( self, z ):
		"Add the layer gcode line and close the last layer gcode block."
		if self.layerStarted:
			self.addLine( '(</layer>)' )
		self.addLine( '(<layer> ' + self.getRounded( z ) + ' )' ) # Indicate that a new layer is starting.
		self.layerStarted = True

	def addLine( self, line ):
		"Add a line of text and a newline to the output."
		if len( line ) > 0:
			self.output.write( line + "\n" )

	def addRaft( self ):
		self.extrusionTop = self.raftPreferences.bottomAltitude.value
		complexRadius = complex( self.raftOutsetRadius, self.raftOutsetRadius )
		self.baseLayerThicknessOverLayerThickness = self.raftPreferences.baseLayerThicknessOverLayerThickness.value
		baseExtrusionWidth = self.extrusionWidth * self.baseLayerThicknessOverLayerThickness
		baseStep = baseExtrusionWidth / self.raftPreferences.baseInfillDensity.value
		self.interfaceLayerThicknessOverLayerThickness = self.raftPreferences.interfaceLayerThicknessOverLayerThickness.value
		interfaceExtrusionWidth = self.extrusionWidth * self.interfaceLayerThicknessOverLayerThickness
		self.interfaceStep = interfaceExtrusionWidth / self.raftPreferences.interfaceInfillDensity.value
		self.setCornersZ()
		self.cornerLowComplex = self.cornerLow.dropAxis( 2 )
		halfLayerThickness = 0.5 * self.layerThickness
		self.complexHigh = complexRadius + self.cornerHighComplex
		self.complexLow = self.cornerLowComplex - complexRadius
		extent = self.complexHigh - self.complexLow
		extentStepX = interfaceExtrusionWidth + 2.0 * self.interfaceStep * math.ceil( 0.5 * ( extent.real - self.interfaceStep ) / self.interfaceStep )
		extentStepY = baseExtrusionWidth + 2.0 * baseStep * math.ceil( 0.5 * ( extent.imag - baseStep ) / baseStep )
		center = 0.5 * ( self.complexHigh + self.complexLow )
		extentStep = complex( extentStepX, extentStepY )
		stepBegin = center - 0.5 * extentStep
		stepEnd = stepBegin + extentStep
		zBegin = self.extrusionTop + self.layerThickness
		beginLoop = getSquareLoop( self.cornerLowComplex, self.cornerHighComplex )
		extrudeRaft = self.raftPreferences.baseLayers.value > 0 or self.raftPreferences.interfaceLayers.value > 0
		if extrudeRaft:
			self.addTemperature( self.raftPreferences.temperatureRaft.value )
		else:
			self.addTemperature( self.raftPreferences.temperatureShapeFirstLayerOutline.value )
		self.addLayerLine( zBegin )
		intercircle.addOrbits( beginLoop, self, self.raftPreferences.temperatureChangeBeforeTimeRaft.value, zBegin )
		for baseLayerIndex in xrange( self.raftPreferences.baseLayers.value ):
			self.addBaseLayer( baseExtrusionWidth, baseStep, stepBegin, stepEnd )
		self.setInterfaceVariables( interfaceExtrusionWidth, stepBegin, stepEnd )
		for interfaceLayerIndex in xrange( self.raftPreferences.interfaceLayers.value ):
			self.addInterfaceLayer()
		self.operatingJump = self.extrusionTop - self.cornerLow.z + halfLayerThickness + halfLayerThickness * self.raftPreferences.operatingNozzleLiftOverHalfLayerThickness.value
		self.setBoundaryLayers()
		if extrudeRaft and len( self.boundaryLayers ) > 0:
			boundaryZ = self.boundaryLayers[ 0 ].z
			self.addLayerLine( boundaryZ )
			self.addTemperature( self.raftPreferences.temperatureShapeFirstLayerOutline.value )
			squareLoop = getSquareLoop( stepBegin, stepEnd )
			intercircle.addOrbits( squareLoop, self, self.raftPreferences.temperatureChangeTimeBeforeFirstLayerOutline.value, boundaryZ )
			self.addLineLayerStart = False

	def addSupportSegmentTable( self, layerIndex ):
		"Add support segments from the boundary layers."
		aboveLayer = self.boundaryLayers[ layerIndex + 1 ]
		if len( aboveLayer.loops ) < 1:
			self.supportSegmentTables.append( {} )
			return
		aboveLoops = self.supportLoops[ layerIndex + 1 ]
		horizontalSegmentTable = {}
		rise = aboveLayer.z - self.boundaryLayers[ layerIndex ].z
		outsetSupportLayer = intercircle.getInsetLoops( - self.minimumSupportRatio * rise, self.supportLoops[ layerIndex ] )
		numberOfSubSteps = 10
		subStepSize = self.interfaceStep / float( numberOfSubSteps )
		for y in self.interfaceStepsUntilEnd:
			xTotalIntersectionIndexList = []
			for subStepIndex in xrange( 2 * numberOfSubSteps + 1 ):
				ySubStep = y + ( subStepIndex - numberOfSubSteps ) * subStepSize
				xIntersectionIndexList = []
				euclidean.addXIntersectionIndexesFromLoops( aboveLoops, - 1, xIntersectionIndexList, ySubStep )
				euclidean.addXIntersectionIndexesFromLoops( outsetSupportLayer, 0, xIntersectionIndexList, ySubStep )
				xIntersections = euclidean.getXIntersectionsFromIntersections( xIntersectionIndexList )
				for xIntersection in xIntersections:
					xTotalIntersectionIndexList.append( euclidean.XIntersectionIndex( subStepIndex, xIntersection ) )
			xTotalIntersections = getJoinOfXIntersectionIndexes( xTotalIntersectionIndexList )
			lineSegments = euclidean.getSegmentsFromXIntersections( xTotalIntersections, y )
			if len( lineSegments ) > 0:
				horizontalSegmentTable[ y ] = lineSegments
		self.supportSegmentTables.append( horizontalSegmentTable )

	def addSupportLayerTemperature( self, supportSegments, z ):
		"Add support layer and temperature before the object layer."
		self.addTemperatureOrbits( supportSegments, self.raftPreferences.temperatureShapeSupportLayers, self.raftPreferences.temperatureChangeTimeBeforeSupportLayers, z )
		endpoints = getEndpointsFromSegments( supportSegments )
		aroundPixelTable = {}
		layerFillInset = 0.9 * self.extrusionWidth
		aroundWidth = 0.12 * layerFillInset
		boundaryLoops = self.boundaryLayers[ self.layerIndex ].loops
		halfSupportOutset = 0.5 * self.supportOutset
		aroundBoundaryLoops = intercircle.getInsetLoops( halfSupportOutset, boundaryLoops ) + intercircle.getInsetLoops( - halfSupportOutset, boundaryLoops )
		for aroundBoundaryLoop in aroundBoundaryLoops:
			euclidean.addLoopToPixelTable( aroundBoundaryLoop, aroundPixelTable, aroundWidth )
		paths = euclidean.getPathsFromEndpoints( endpoints, layerFillInset, aroundPixelTable, aroundWidth )
		self.addFlowrateLineIfNecessary( self.supportFlowrateString )
		for path in paths:
			self.addGcodeFromFeedrateThreadZ( self.feedrateMinute, path, z )
		self.addFlowrateLineIfNecessary( self.operatingFlowrateString )
		self.addTemperatureOrbits( supportSegments, self.raftPreferences.temperatureShapeSupportedLayers, self.raftPreferences.temperatureChangeTimeBeforeSupportedLayers, z )

	def addTemperature( self, temperature ):
		"Add a line of temperature."
		self.addLine( 'M104 S' + euclidean.getRoundedToThreePlaces( temperature ) ) # Set temperature.

	def addTemperatureOrbits( self, segments, temperaturePreference, temperatureTimeChangePreference, z ):
		"Add the temperature and orbits around the support layer."
		if self.layerIndex < 0:
			return
		boundaryLoops = self.boundaryLayers[ self.layerIndex ].loops
		self.addTemperature( temperaturePreference.value )
		if len( boundaryLoops ) < 1:
			endpoints = getEndpointsFromSegments( segments )
			layerCornerHigh = complex( - 999999999.0, - 999999999.0 )
			layerCornerLow = complex( 999999999.0, 999999999.0 )
			for endpoint in endpoints:
				layerCornerHigh = euclidean.getMaximum( layerCornerHigh, endpoint.point )
				layerCornerLow = euclidean.getMinimum( layerCornerLow, endpoint.point )
			squareLoop = getSquareLoop( layerCornerLow, layerCornerHigh )
			intercircle.addOrbits( squareLoop, self, temperatureTimeChangePreference.value, z )
			return
		perimeterInset = 0.4 * self.extrusionPerimeterWidth
		insetBoundaryLoops = intercircle.getInsetLoops( perimeterInset, boundaryLoops )
		if len( insetBoundaryLoops ) < 1:
			insetBoundaryLoops = boundaryLoops
		largestLoop = euclidean.getLargestLoop( insetBoundaryLoops )
		intercircle.addOrbits( largestLoop, self, temperatureTimeChangePreference.value, z )

	def addToFillXIntersectionIndexTables( self, fillXIntersectionIndexTables, layerIndex ):
		"Add fill segments from the boundary layers."
		supportLoops = self.supportLoops[ layerIndex ]
		if len( supportLoops ) < 1:
			fillXIntersectionIndexTables.append( {} )
			return
		fillXIntersectionIndexTable = {}
		for y in self.interfaceStepsUntilEnd:
			xIntersectionIndexes = getFillXIntersectionIndexes( supportLoops, y )
			if len( xIntersectionIndexes ) > 0:
				xIntersections = getJoinOfXIntersectionIndexes( xIntersectionIndexes )
				lineSegments = euclidean.getSegmentsFromXIntersections( xIntersections, y )
				fillXIntersectionIndexTable[ y ] = lineSegments
		fillXIntersectionIndexTables.append( fillXIntersectionIndexTable )

	def extendSegments( self, supportSegmentTable ):
		"Extend the support segments."
		supportLayerKeys = supportSegmentTable.keys()
		horizontalSegmentSegmentTable = {}
		for supportLayerKey in supportLayerKeys:
			lineSegments = supportSegmentTable[ supportLayerKey ]
			xIntersectionIndexList = []
			for lineSegmentIndex in xrange( len( lineSegments ) ):
				lineSegment = lineSegments[ lineSegmentIndex ]
				extendedLineSegment = getExtendedLineSegment( self.raftOutsetRadius, lineSegment )
				if extendedLineSegment != None:
					addXIntersectionsFromSegment( lineSegmentIndex, extendedLineSegment, xIntersectionIndexList )
			xIntersections = getJoinOfXIntersectionIndexes( xIntersectionIndexList )
			for xIntersectionIndex in xrange( len( xIntersections ) ):
				xIntersection = xIntersections[ xIntersectionIndex ]
				xIntersection = max( xIntersection, self.interfaceBeginX )
				xIntersection = min( xIntersection, self.interfaceEndX )
				xIntersections[ xIntersectionIndex ] = xIntersection
			if len( xIntersections ) > 0:
				extendedLineSegments = euclidean.getSegmentsFromXIntersections( xIntersections, supportLayerKey )
				supportSegmentTable[ supportLayerKey ] = extendedLineSegments
			else:
				del supportSegmentTable[ supportLayerKey ]

	def getBoundaryLine( self, splitLine ):
		"Get elevated boundary gcode line."
		location = gcodec.getLocationFromSplitLine( None, splitLine )
		if self.operatingJump != None:
			location.z += self.operatingJump
		return '(<boundaryPoint> X%s Y%s Z%s </boundaryPoint>)' % ( self.getRounded( location.x ), self.getRounded( location.y ), self.getRounded( location.z ) )

	def getGcodeFromFeedrateMovementZ( self, feedrateMinute, point, z ):
		"Get a gcode movement."
		return "G1 X%s Y%s Z%s F%s" % ( self.getRounded( point.real ), self.getRounded( point.imag ), self.getRounded( z ), self.getRounded( feedrateMinute ) )

	def getRaftedLine( self, splitLine ):
		"Get elevated gcode line with operating feedrate."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.feedrateMinute = gcodec.getFeedrateMinute( self.feedrateMinute, splitLine )
		self.oldLocation = location
		z = location.z
		if self.operatingJump != None:
			z += self.operatingJump
		if not self.isFirstLayerWithinTemperatureAdded and not self.isSurroundingLoop:
			self.isFirstLayerWithinTemperatureAdded = True
			self.addTemperature( self.raftPreferences.temperatureShapeFirstLayerWithin.value )
			if self.raftPreferences.addRaftElevateNozzleOrbitSetAltitude.value:
				boundaryLoops = self.boundaryLayers[ self.layerIndex ].loops
				if len( boundaryLoops ) > 1:
					intercircle.addOperatingOrbits( boundaryLoops, euclidean.getXYComplexFromVector3( self.oldLocation ), self, self.raftPreferences.temperatureChangeTimeBeforeNextThreads.value, z )
		return self.getGcodeFromFeedrateMovementZ( self.feedrateMinute, location.dropAxis( 2 ), z )

	def getRounded( self, number ):
		"Get number rounded to the number of carried decimal places as a string."
		return euclidean.getRoundedToDecimalPlacesString( self.decimalPlacesCarried, number )

	def getStepsUntilEnd( self, begin, end, stepSize ):
		"Get steps from the beginning until the end."
		step = begin
		steps = []
		while step < end:
			steps.append( step )
			step += stepSize
		return steps

	def getSupportSegments( self ):
		"Get the support layer segments."
		if len( self.supportSegmentTables ) <= self.layerIndex:
			return []
		supportSegmentTable = self.supportSegmentTables[ self.layerIndex ]
		segments = []
		segmentTableKeys = supportSegmentTable.keys()
		segmentTableKeys.sort()
		for segmentTableKey in segmentTableKeys:
			segments += supportSegmentTable[ segmentTableKey ]
		return segments

	def joinSegments( self, supportSegmentTableIndex ):
		"Join the support segments of this layer with those of the layer above."
		horizontalSegmentTable = self.supportSegmentTables[ supportSegmentTableIndex ]
		horizontalSegmentTableKeys = horizontalSegmentTable.keys()
		aboveHorizontalSegmentTable = self.supportSegmentTables[ supportSegmentTableIndex + 1 ]
		aboveHorizontalSegmentTableKeys = aboveHorizontalSegmentTable.keys()
		joinSegmentTables( aboveHorizontalSegmentTable, horizontalSegmentTable )

	def parseGcode( self, gcodeText, raftPreferences ):
		"Parse gcode text and store the raft gcode."
		self.raftPreferences = raftPreferences
		self.minimumSupportRatio = math.tan( math.radians( raftPreferences.supportMinimumAngle.value ) )
		self.raftOutsetRadius = self.raftPreferences.raftOutsetRadiusOverExtrusionWidth.value * self.extrusionWidth
		self.lines = gcodec.getTextLines( gcodeText )
		self.parseInitialization()
		if raftPreferences.addRaftElevateNozzleOrbitSetAltitude.value:
			self.addRaft()
		self.addTemperature( raftPreferences.temperatureShapeFirstLayerOutline.value )
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )

	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'M108':
				self.setOperatingFlowString( splitLine )
			elif firstWord == '(<decimalPlacesCarried>':
				self.decimalPlacesCarried = int( splitLine[ 1 ] )
			elif firstWord == '(<extrusionPerimeterWidth>':
				self.extrusionPerimeterWidth = float( splitLine[ 1 ] )
				self.supportOutset = self.extrusionPerimeterWidth - self.extrusionPerimeterWidth * self.raftPreferences.supportInsetOverPerimeterExtrusionWidth.value
			elif firstWord == '(<extrusionWidth>':
				self.extrusionWidth = float( splitLine[ 1 ] )
			elif firstWord == '(</extruderInitialization>)':
				self.addLine( '(<procedureDone> raft /<procedureDone>)' )
			elif firstWord == '(<feedrateMinute>':
				self.feedrateMinute = float( splitLine[ 1 ] )
			elif firstWord == '(<layer>':
				return
			elif firstWord == '(<layerThickness>':
				self.layerThickness = float( splitLine[ 1 ] )
			elif firstWord == '(<orbitalFeedratePerSecond>':
				self.orbitalFeedratePerSecond = float( splitLine[ 1 ] )
			elif firstWord == '(<supportFlowrate>':
				self.supportFlowrateString = splitLine[ 1 ]
			elif firstWord == '(<travelFeedratePerSecond>':
				self.travelFeedratePerMinute = 60.0 * float( splitLine[ 1 ] )
			self.addLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the raft skein."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			if self.extrusionStart:
				line = self.getRaftedLine( splitLine )
		elif firstWord == 'M101':
			if self.isStartupEarly:
				self.isStartupEarly = False
				return
		elif firstWord == 'M108':
			self.setOperatingFlowString( splitLine )
		elif firstWord == '(<boundaryPoint>':
			line = self.getBoundaryLine( splitLine )
		elif firstWord == '(</extrusion>)':
			self.extrusionStart = False
			self.addLine( self.operatingLayerEndLine )
		elif firstWord == '(<layer>':
			self.layerIndex += 1
			boundaryLayer = None
			layerHeight = self.extrusionTop + float( splitLine[ 1 ] )
			if len( self.boundaryLayers ) > 0:
				boundaryLayer = self.boundaryLayers[ self.layerIndex ]
				layerHeight = boundaryLayer.z
			if self.operatingJump != None:
				line = '(<layer> ' + self.getRounded( layerHeight ) + ' )'
			if self.layerStarted and self.addLineLayerStart:
				self.addLine( '(</layer>)' )
			self.layerStarted = False
			if self.layerIndex > len( self.supportSegmentTables ) + 1:
				self.addLine( self.operatingLayerEndLine )
				self.operatingLayerEndLine = ''
			if self.addLineLayerStart:
				self.addLine( line )
			self.addLineLayerStart = True
			line = ''
			supportSegments = self.getSupportSegments()
			if self.layerIndex == 1:
				if len( supportSegments ) < 1:
					self.addTemperature( self.raftPreferences.temperatureShapeNextLayers.value )
					if self.raftPreferences.addRaftElevateNozzleOrbitSetAltitude.value:
						boundaryLoops = boundaryLayer.loops
						if len( boundaryLoops ) > 0:
							temperatureChangeTimeBeforeNextThreads = self.raftPreferences.temperatureChangeTimeBeforeNextThreads.value
							intercircle.addOperatingOrbits( boundaryLoops, euclidean.getXYComplexFromVector3( self.oldLocation ), self, temperatureChangeTimeBeforeNextThreads, layerHeight )
			if len( supportSegments ) > 0:
				self.addSupportLayerTemperature( supportSegments, layerHeight )
		self.addLine( line )

	def setBoundaryLayers( self ):
		"Set the boundary layers."
		boundaryLoop = None
		boundaryLayer = None
		for line in self.lines[ self.lineIndex : ]:
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == '(<boundaryPoint>':
				location = gcodec.getLocationFromSplitLine( None, splitLine )
				if boundaryLoop == None:
					boundaryLoop = []
					boundaryLayer.loops.append( boundaryLoop )
				boundaryLoop.append( location.dropAxis( 2 ) )
			elif firstWord == '(<layer>':
				z = float( splitLine[ 1 ] )
				if self.operatingJump != None:
					z += self.operatingJump
				boundaryLayer = euclidean.LoopLayer( z )
				self.boundaryLayers.append( boundaryLayer )
			elif firstWord == '(</surroundingLoop>)':
				boundaryLoop = None
		if self.raftPreferences.supportChoiceNoSupportMaterial.value:
			return
		if len( self.interfaceStepsUntilEnd ) < 1:
			return
		if len( self.boundaryLayers ) < 2:
			return
		for boundaryLayer in self.boundaryLayers:
			supportLoops = intercircle.getInsetLoops( - self.supportOutset, boundaryLayer.loops )
			self.supportLoops.append( supportLoops )
		for layerIndex in xrange( len( self.supportLoops ) - 1 ):
			self.addSupportSegmentTable( layerIndex )
		self.truncateSupportSegmentTables()
		fillXIntersectionIndexTables = []
		for supportSegmentTableIndex in xrange( len( self.supportSegmentTables ) ):
			self.addToFillXIntersectionIndexTables( fillXIntersectionIndexTables, supportSegmentTableIndex )
		if self.raftPreferences.supportChoiceSupportMaterialOnExteriorOnly.value:
			for supportSegmentTableIndex in xrange( 1, len( self.supportSegmentTables ) ):
				self.subtractJoinedFill( fillXIntersectionIndexTables, supportSegmentTableIndex )
		for supportSegmentTableIndex in xrange( len( self.supportSegmentTables ) - 2, - 1, - 1 ):
			self.joinSegments( supportSegmentTableIndex )
		for supportSegmentTable in self.supportSegmentTables:
			self.extendSegments( supportSegmentTable )
		for supportSegmentTableIndex in xrange( len( self.supportSegmentTables ) ):
			subtractFill( fillXIntersectionIndexTables[ supportSegmentTableIndex ], self.supportSegmentTables[ supportSegmentTableIndex ] )

	def setCornersZ( self ):
		"Set maximum and minimum corners and z."
		layerIndex = - 1
		self.cornerHighComplex = complex( - 999999999.0, - 999999999.0 )
		self.cornerLow = Vector3( 999999999.0, 999999999.0, 999999999.0 )
		for line in self.lines[ self.lineIndex : ]:
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
			if firstWord == 'G1':
				location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
				self.cornerHighComplex = euclidean.getMaximum( self.cornerHighComplex, location.dropAxis( 2 ) )
				self.cornerLow = euclidean.getPointMinimum( self.cornerLow, location )
				self.oldLocation = location
			elif firstWord == '(<layer>':
				layerIndex += 1
				if self.raftPreferences.supportChoiceNoSupportMaterial.value:
					if layerIndex > 1:
						return

	def setInterfaceVariables( self, interfaceExtrusionWidth, stepBegin, stepEnd ):
		"Set the interface variables."
		halfInterfaceExtrusionWidth = 0.5 * interfaceExtrusionWidth
		self.interfaceStepsUntilEnd = self.getStepsUntilEnd( stepBegin.imag + halfInterfaceExtrusionWidth, stepEnd.imag, self.interfaceStep )
		self.interfaceOverhang = self.raftPreferences.infillOverhang.value * halfInterfaceExtrusionWidth - halfInterfaceExtrusionWidth
		self.interfaceBeginX = stepBegin.real - self.interfaceOverhang
		self.interfaceEndX = stepEnd.real + self.interfaceOverhang

	def setOperatingFlowString( self, splitLine ):
		"Set the operating flow string from the split line."
		self.operatingFlowrateString = splitLine[ 1 ][ 1 : ]

	def subtractJoinedFill( self, fillXIntersectionIndexTables, supportSegmentTableIndex ):
		"Join the fill then subtract it from the support layer table."
		supportSegmentTable = self.supportSegmentTables[ supportSegmentTableIndex ]
		fillXIntersectionIndexTable = fillXIntersectionIndexTables[ supportSegmentTableIndex ]
		fillXIntersectionIndexTableKeys = fillXIntersectionIndexTable.keys()
		belowHorizontalSegmentTable = fillXIntersectionIndexTables[ supportSegmentTableIndex - 1 ]
		belowHorizontalSegmentTableKeys = belowHorizontalSegmentTable.keys()
		joinSegmentTables( belowHorizontalSegmentTable, fillXIntersectionIndexTable )
		subtractFill( fillXIntersectionIndexTable, supportSegmentTable )

	def truncateSupportSegmentTables( self ):
		"Truncate the support segments after the last support segment which contains elements."
		for supportSegmentTableIndex in xrange( len( self.supportSegmentTables ) - 1, - 1, - 1 ):
			if len( self.supportSegmentTables[ supportSegmentTableIndex ] ) > 0:
				self.supportSegmentTables = self.supportSegmentTables[ : supportSegmentTableIndex + 1 ]
				return
		self.supportSegmentTables = []


def main():
	"Display the raft dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.displayDialog( RaftPreferences() )

if __name__ == "__main__":
	main()
