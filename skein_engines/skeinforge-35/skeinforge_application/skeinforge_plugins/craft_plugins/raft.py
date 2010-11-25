"""
This page is in the table of contents.
Raft is a script to create a raft, elevate the nozzle and set the temperature.

The raft manual page is at:
http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Raft

Allan Ecker aka The Masked Retriever's has written two quicktips for raft which follow below.
"Skeinforge Quicktip: The Raft, Part 1" at:
http://blog.thingiverse.com/2009/07/14/skeinforge-quicktip-the-raft-part-1/
"Skeinforge Quicktip: The Raft, Part II" at:
http://blog.thingiverse.com/2009/08/04/skeinforge-quicktip-the-raft-part-ii/

Pictures of rafting in action are available from the Metalab blog at:
http://reprap.soup.io/?search=rafting

Raft is based on the Nophead's reusable raft, which has a base layer running one way, and a couple of perpendicular layers above.  Each set of layers can be set to a different temperature.  There is the option of having the extruder orbit the raft for a while, so the heater barrel has time to reach a different temperature, without ooze accumulating around the nozzle.

The important values for the raft settings are the temperatures of the raft, the first layer and the next layers.  These will be different for each material.  The default settings for ABS, HDPE, PCL & PLA are extrapolated from Nophead's experiments.

==Operation==
The default 'Activate Raft' checkbox is on.  When it is on, the functions described below will work, when it is off, the functions will not be called.  The raft script sets the temperature.

==Settings==
===Add Raft, Elevate Nozzle, Orbit and Set Altitude===
Default is on.

When selected, the script will also create a raft, elevate the nozzle, orbit and set the altitude of the bottom of the raft.

===Base===
====Base Feed Rate Multiplier====
Default is one.

Defines the base feed rate multiplier.  The greater the 'Base Feed Rate Multiplier', the thinner the base, the lower the 'Base Feed Rate Multiplier', the thicker the base.

====Base Flow Rate Multiplier====
Default is one.

Defines the base flow rate multiplier.  The greater the 'Base Flow Rate Multiplier', the thicker the base, the lower the 'Base Flow Rate Multiplier', the thinner the base.

====Base Infill Density====
Default is 0.5.

Defines the infill density ratio of the base of the raft.

====Base Layer Height over Layer Thickness====
Default is two.

Defines the ratio of the height & width of the base layer compared to the height and width of the object infill.  The feed rate will be slower for raft layers which have thicker extrusions than the object infill.

====Base Layers====
Default is one.

Defines the number of base layers.

====Base Nozzle Lift over Base Layer Thickness====
Default is 0.4.

Defines the amount the nozzle is above the center of the base extrusion divided by the base layer thickness.

===Bottom Altitude===
Default is zero.

Defines the altitude of the bottom of the raft.

===Initial Circling===
Default is off.

When selected, the extruder will initially circle around until it reaches operating temperature.

===Infill Overhang over Extrusion Width===
Default is 0.05.

Defines the ratio of the infill overhang over the the extrusion width of the raft.

===Interface===
====Interface Feed Rate Multiplier====
Default is one.

Defines the interface feed rate multiplier.  The greater the 'Interface Feed Rate Multiplier', the thinner the interface, the lower the 'Interface Feed Rate Multiplier', the thicker the interface.

====Interface Flow Rate Multiplier====
Default is one.

Defines the interface flow rate multiplier.  The greater the 'Interface Flow Rate Multiplier', the thicker the interface, the lower the 'Interface Flow Rate Multiplier', the thinner the interface.

====Interface Infill Density====
Default is 0.5.

Defines the infill density ratio of the interface of the raft.

====Interface Layer Thickness over Extrusion Height====
Default is one.

Defines the ratio of the height & width of the interface layer compared to the height and width of the object infill.  The feed rate will be slower for raft layers which have thicker extrusions than the object infill.

====Interface Layers====
Default is two.

Defines the number of interface layers.

====Interface Nozzle Lift over Interface Layer Thickness====
Default is 0.45.

Defines the amount the nozzle is above the center of the interface extrusion divided by the interface layer thickness.

===Object First Layer Feed Rate Infill Multiplier===
Default is one.

Defines the object first layer infill feed rate multiplier.  The greater the 'Object First Layer Feed Rate Infill Multiplier, the thinner the infill, the lower the 'Object First Layer Feed Rate Infill Multiplier', the thicker the infill.

===Object First Layer Feed Rate Perimeter Multiplier===
Default is one.

Defines the object first layer perimeter feed rate multiplier.  The greater the 'Object First Layer Feed Rate Perimeter Multiplier, the thinner the perimeter, the lower the 'Object First Layer Feed Rate Perimeter Multiplier', the thicker the perimeter.

====Object First Layer Flow Rate Multiplier====
Default is one.

Defines the object first layer flow rate multiplier.  The greater the 'Object First Layer Flow Rate Multiplier', the thicker the first layer, the lower the 'Object First Layer Flow Rate Multiplier, the thinner the first layer.

===Operating Nozzle Lift over Layer Thickness===
Default is 0.5.

Defines the amount the nozzle is above the center of the operating extrusion divided by the layer thickness.

===Raft Size===
The raft fills a rectangle whose base size is the rectangle around the bottom layer of the object expanded on each side by the 'Raft Margin' plus the 'Raft Additional Margin over Length (%)' percentage times the length of the side.

====Raft Margin====
Default is three millimeters.

====Raft Additional Margin over Length====
Default is 1 percent.

===Support===
Good articles on support material are at:
http://davedurant.wordpress.com/2010/07/31/skeinforge-support-part-1/
http://davedurant.wordpress.com/2010/07/31/skeinforge-support-part-2/

====Support Cross Hatch====
Default is off.

When selected, the support material will cross hatched.  Cross hatching the support makes it stronger and harder to remove, which is why the default is off.

====Support Flow Rate over Operating Flow Rate====
Default is 0.9.

Defines the ratio of the flow rate when the support is extruded over the operating flow rate.  With a number less than one, the support flow rate will be smaller so the support will be thinner and easier to remove.

====Support Gap over Perimeter Extrusion Width====
Default is 0.5.

Defines the gap between the support material and the object over the perimeter extrusion width.

====Support Material Choice====
Default is 'None' because the raft takes time to generate.

=====Empty Layers Only=====
When selected, support material will be only on the empty layers.  This is useful when making identical objects in a stack.

=====Everywhere=====
When selected, support material will be added wherever there are overhangs, even inside the object.  Because support material inside objects is hard or impossible to remove, this option should only be chosen if the object has a cavity that needs support and there is some way to extract the support material.

=====Exterior Only=====
When selected, support material will be added only the exterior of the object.  This is the best option for most objects which require support material.

=====None=====
When selected, raft will not add support material.

====Support Minimum Angle====
Default is sixty degrees.

Defines the minimum angle that a surface overhangs before support material is added.  This angle is defined from the vertical, so zero is a vertical wall, ten is a wall with a bit of overhang, thirty is the typical safe angle for filament extrusion, sixty is a really high angle for extrusion and ninety is an unsupported horizontal ceiling.

==Alterations==
If support material is generated, raft looks for alteration files in the alterations folder in the .skeinforge folder in the home directory.  Raft does not care if the text file names are capitalized, but some file systems do not handle file name cases properly, so to be on the safe side you should give them lower case names.  If it doesn't find the file it then looks in the alterations folder in the skeinforge_plugins folder. If it doesn't find anything there it looks in the craft_plugins folder.

===support_start.gcode===
If support material is generated, raft will add support_start.gcode, if it exists, to the start of the support gcode.

===support_end.gcode===
If support material is generated, raft will add support_end.gcode, if it exists, to the end of the support gcode.

==Examples==
The following examples raft the file Screw Holder Bottom.stl.  The examples are run in a terminal in the folder which contains Screw Holder Bottom.stl and raft.py.


> python raft.py
This brings up the raft dialog.


> python raft.py Screw Holder Bottom.stl
The raft tool is parsing the file:
Screw Holder Bottom.stl
..
The raft tool has created the file:
Screw Holder Bottom_raft.gcode


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import raft
>>> raft.main()
This brings up the raft dialog.


>>> raft.writeOutput('Screw Holder Bottom.stl')
Screw Holder Bottom.stl
The raft tool is parsing the file:
Screw Holder Bottom.stl
..
The raft tool has created the file:
Screw Holder Bottom_raft.gcode


"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import archive
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import intercircle
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from skeinforge_application.skeinforge_utilities import skeinforge_profile
import math
import os
import sys


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


#maybe later wide support
#raft outline temperature http://hydraraptor.blogspot.com/2008/09/screw-top-pot.html
def getCraftedText( fileName, text = '', repository=None):
	"Raft the file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), repository )

def getCraftedTextFromText(gcodeText, repository=None):
	"Raft a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'raft'):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository( RaftRepository() )
	if not repository.activateRaft.value:
		return gcodeText
	return RaftSkein().getCraftedGcode(gcodeText, repository)

def getCrossHatchPointLine( crossHatchPointLineTable, y ):
	"Get the cross hatch point line."
	if not crossHatchPointLineTable.has_key(y):
		crossHatchPointLineTable[ y ] = {}
	return crossHatchPointLineTable[ y ]

def getEndpointsFromYIntersections( x, yIntersections ):
	"Get endpoints from the y intersections."
	endpoints = []
	for yIntersectionIndex in xrange( 0, len( yIntersections ), 2 ):
		firstY = yIntersections[ yIntersectionIndex ]
		secondY = yIntersections[ yIntersectionIndex + 1 ]
		if firstY != secondY:
			firstComplex = complex( x, firstY )
			secondComplex = complex( x, secondY )
			endpointFirst = euclidean.Endpoint()
			endpointSecond = euclidean.Endpoint().getFromOtherPoint( endpointFirst, secondComplex )
			endpointFirst.getFromOtherPoint( endpointSecond, firstComplex )
			endpoints.append( endpointFirst )
			endpoints.append( endpointSecond )
	return endpoints

def getExtendedLineSegment( extensionDistance, lineSegment, loopXIntersections ):
	"Get extended line segment."
	pointBegin = lineSegment[0].point
	pointEnd = lineSegment[1].point
	segment = pointEnd - pointBegin
	segmentLength = abs( segment )
	if segmentLength <= 0.0:
		print( "This should never happen in getExtendedLineSegment in raft, the segment should have a length greater than zero." )
		print( lineSegment )
		return None
	segmentExtend = segment * extensionDistance / segmentLength
	lineSegment[0].point -= segmentExtend
	lineSegment[1].point += segmentExtend
	for loopXIntersection in loopXIntersections:
		setExtendedPoint( lineSegment[0], pointBegin, loopXIntersection )
		setExtendedPoint( lineSegment[1], pointEnd, loopXIntersection )
	return lineSegment

def getNewRepository():
	"Get the repository constructor."
	return RaftRepository()

def setExtendedPoint( lineSegmentEnd, pointOriginal, x ):
	"Set the point in the extended line segment."
	if x > min( lineSegmentEnd.point.real, pointOriginal.real ) and x < max( lineSegmentEnd.point.real, pointOriginal.real ):
		lineSegmentEnd.point = complex( x, pointOriginal.imag )

def writeOutput(fileName=''):
	"Raft a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName == '':
		return
	skeinforge_craft.writeChainTextWithNounMessage( fileName, 'raft')


class RaftRepository:
	"A class to handle the raft settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository('skeinforge_application.skeinforge_plugins.craft_plugins.raft.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Raft', self, '')
		self.openWikiManualHelpPage = settings.HelpPage().getOpenFromAbsolute('http://www.bitsfrombytes.com/wiki/index.php?title=Skeinforge_Raft')
		self.activateRaft = settings.BooleanSetting().getFromValue('Activate Raft', self, True )
		self.addRaftElevateNozzleOrbitSetAltitude = settings.BooleanSetting().getFromValue('Add Raft, Elevate Nozzle, Orbit and Set Altitude:', self, True )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Base -', self )
		self.baseFeedRateMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Base Feed Rate Multiplier (ratio):', self, 1.1, 1.0 )
		self.baseFlowRateMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Base Flow Rate Multiplier (ratio):', self, 1.1, 1.0 )
		self.baseInfillDensity = settings.FloatSpin().getFromValue( 0.3, 'Base Infill Density (ratio):', self, 0.9, 0.5 )
		self.baseLayerThicknessOverLayerThickness = settings.FloatSpin().getFromValue( 1.0, 'Base Layer Thickness over Layer Thickness:', self, 3.0, 2.0 )
		self.baseLayers = settings.IntSpin().getFromValue( 0, 'Base Layers (integer):', self, 3, 1 )
		self.baseNozzleLiftOverBaseLayerThickness = settings.FloatSpin().getFromValue( 0.2, 'Base Nozzle Lift over Base Layer Thickness (ratio):', self, 0.8, 0.4 )
		settings.LabelSeparator().getFromRepository(self)
		self.initialCircling = settings.BooleanSetting().getFromValue('Initial Circling:', self, False )
		self.infillOverhangOverExtrusionWidth = settings.FloatSpin().getFromValue( 0.0, 'Infill Overhang over Extrusion Width (ratio):', self, 0.5, 0.05 )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Interface -', self )
		self.interfaceFeedRateMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Interface Feed Rate Multiplier (ratio):', self, 1.1, 1.0 )
		self.interfaceFlowRateMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Interface Flow Rate Multiplier (ratio):', self, 1.1, 1.0 )
		self.interfaceInfillDensity = settings.FloatSpin().getFromValue( 0.3, 'Interface Infill Density (ratio):', self, 0.9, 0.5 )
		self.interfaceLayerThicknessOverLayerThickness = settings.FloatSpin().getFromValue( 1.0, 'Interface Layer Thickness over Layer Thickness:', self, 3.0, 1.0 )
		self.interfaceLayers = settings.IntSpin().getFromValue( 0, 'Interface Layers (integer):', self, 3, 2 )
		self.interfaceNozzleLiftOverInterfaceLayerThickness = settings.FloatSpin().getFromValue( 0.25, 'Interface Nozzle Lift over Interface Layer Thickness (ratio):', self, 0.85, 0.45 )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Object First Layer -', self )
		self.objectFirstLayerFeedRateInfillMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Object First Layer Feed Rate Infill Multiplier (ratio):', self, 1.1, 1.0 )
		self.objectFirstLayerFeedRatePerimeterMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Object First Layer Feed Rate Perimeter Multiplier (ratio):', self, 1.1, 1.0 )
		self.objectFirstLayerFlowRateMultiplier = settings.FloatSpin().getFromValue( 0.7, 'Object First Layer Flow Rate Multiplier (ratio):', self, 1.1, 1.0 )
		settings.LabelSeparator().getFromRepository(self)
		self.operatingNozzleLiftOverLayerThickness = settings.FloatSpin().getFromValue( 0.3, 'Operating Nozzle Lift over Layer Thickness (ratio):', self, 0.7, 0.5 )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Raft Size -', self )
		self.raftAdditionalMarginOverLengthPercent = settings.FloatSpin().getFromValue( 0.5, 'Raft Additional Margin over Length (%):', self, 1.5, 1.0 )
		self.raftMargin = settings.FloatSpin().getFromValue( 1.0, 'Raft Margin (mm):', self, 5.0, 3.0 )
		settings.LabelSeparator().getFromRepository(self)
		settings.LabelDisplay().getFromName('- Support -', self )
		self.supportCrossHatch = settings.BooleanSetting().getFromValue('Support Cross Hatch', self, False )
		self.supportFlowRateOverOperatingFlowRate = settings.FloatSpin().getFromValue( 0.7, 'Support Flow Rate over Operating Flow Rate (ratio):', self, 1.1, 1.0 )
		self.supportGapOverPerimeterExtrusionWidth = settings.FloatSpin().getFromValue( 0.5, 'Support Gap over Perimeter Extrusion Width (ratio):', self, 1.5, 1.0 )
		self.supportMaterialChoice = settings.MenuButtonDisplay().getFromName('Support Material Choice: ', self )
		self.supportChoiceNone = settings.MenuRadio().getFromMenuButtonDisplay( self.supportMaterialChoice, 'None', self, True )
		self.supportChoiceEmptyLayersOnly = settings.MenuRadio().getFromMenuButtonDisplay( self.supportMaterialChoice, 'Empty Layers Only', self, False )
		self.supportChoiceEverywhere = settings.MenuRadio().getFromMenuButtonDisplay( self.supportMaterialChoice, 'Everywhere', self, False )
		self.supportChoiceExteriorOnly = settings.MenuRadio().getFromMenuButtonDisplay( self.supportMaterialChoice, 'Exterior Only', self, False )
		self.supportMinimumAngle = settings.FloatSpin().getFromValue( 40.0, 'Support Minimum Angle (degrees):', self, 80.0, 60.0 )
		self.executeTitle = 'Raft'

	def execute(self):
		"Raft button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode(self.fileNameInput.value, fabmetheus_interpret.getImportPluginFileNames(), self.fileNameInput.wasCancelled)
		for fileName in fileNames:
			writeOutput(fileName)


class RaftSkein:
	"A class to raft a skein of extrusions."
	def __init__(self):
		self.addLineLayerStart = True
		self.baseTemperature = None
		self.beginLoop = None
		self.boundaryLayers = []
		self.coolingRate = None
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.extrusionStart = True
		self.extrusionTop = 0.0
		self.feedRateMinute = 961.0
		self.heatingRate = None
		self.insetTable = {}
		self.interfaceTemperature = None
		self.isPerimeter = False
		self.isStartupEarly = False
		self.isSurroundingLoop = True
		self.layerIndex = - 1
		self.layerStarted = False
		self.layerThickness = 0.4
		self.lineIndex = 0
		self.lines = None
		self.objectFirstLayerInfillTemperature = None
		self.objectFirstLayerPerimeterTemperature = None
		self.objectNextLayersTemperature = None
		self.oldFlowRateInput = 1.0
		self.oldFlowRateOutputString = None
		self.oldLocation = None
		self.oldTemperatureOutputString = None
		self.operatingFlowRate = None
		self.operatingLayerEndLine = '(<operatingLayerEnd> </operatingLayerEnd>)'
		self.operatingJump = None
		self.orbitalFeedRatePerSecond = 2.01
		self.perimeterWidth = 0.6
		self.supportFlowRate = None
		self.supportLayers = []
		self.supportLayersTemperature = None
		self.supportedLayersTemperature = None
		self.travelFeedRatePerMinute = None

	def addBaseLayer(self):
		"Add a base layer."
		baseLayerThickness = self.layerThickness * self.baseLayerThicknessOverLayerThickness
		zCenter = self.extrusionTop + 0.5 * baseLayerThickness
		z = zCenter + baseLayerThickness * self.repository.baseNozzleLiftOverBaseLayerThickness.value
		if len( self.baseSegments ) < 1:
			print('This should never happen, the base layer has a size of zero.')
			return
		feedRateMultiplier = self.repository.baseFeedRateMultiplier.value
		self.addLayerFromSegments( feedRateMultiplier, self.repository.baseFlowRateMultiplier.value, baseLayerThickness, self.baseLayerThicknessOverLayerThickness, self.baseSegments, z )

	def addBaseSegments( self, baseExtrusionWidth, baseStep ):
		"Add the base segments."
		baseOverhang = self.repository.infillOverhangOverExtrusionWidth.value * baseExtrusionWidth
		interfaceSegmentsTableKeys = self.interfaceSegmentsTable.keys()
		interfaceSegmentsTableKeys.sort()
		baseYTableTable = {}
		for interfaceSegmentsTableKey in interfaceSegmentsTableKeys:
			interfaceSegments = self.interfaceSegmentsTable[ interfaceSegmentsTableKey ]
			for interfaceSegment in interfaceSegments:
				begin = int( round( interfaceSegment[0].point.real / baseStep ) )
				end = int( round( interfaceSegment[1].point.real / baseStep ) )
				for stepIndex in xrange( begin, end + 1 ):
					if stepIndex not in baseYTableTable:
						baseYTableTable[ stepIndex ] = {}
					baseYTableTable[ stepIndex ][ interfaceSegmentsTableKey ] = None
		baseYTableTableKeys = baseYTableTable.keys()
		baseYTableTableKeys.sort()
		self.baseSegments = []
		for baseYTableTableKey in baseYTableTableKeys:
			baseYTable = baseYTableTable[ baseYTableTableKey ]
			baseYTableKeys = baseYTable.keys()
			baseYTableKeys.sort()
			xIntersections = []
			for baseYTableKey in baseYTableKeys:
				y = baseYTableKey * self.interfaceStep
				if baseYTableKey - 1 not in baseYTableKeys:
					xIntersections.append( y - baseOverhang )
				if baseYTableKey + 1 not in baseYTableKeys:
					xIntersections.append( y + baseOverhang )
			self.baseSegments += euclidean.getSegmentsFromXIntersections( xIntersections, baseYTableTableKey * baseStep )
		for segment in self.baseSegments:
			for endpoint in segment:
				endpoint.point = complex( endpoint.point.imag, endpoint.point.real )

	def addEmptyLayerSupport( self, boundaryLayerIndex ):
		"Add support material to a layer if it is empty."
		supportLayer = SupportLayer( [] )
		self.supportLayers.append( supportLayer )
		if len( self.boundaryLayers[ boundaryLayerIndex ].loops ) > 0:
			return
		aboveXIntersectionsTable = {}
		euclidean.addXIntersectionsFromLoopsForTable( self.getInsetLoopsAbove( boundaryLayerIndex ), aboveXIntersectionsTable, self.interfaceStep )
		belowXIntersectionsTable = {}
		euclidean.addXIntersectionsFromLoopsForTable( self.getInsetLoopsBelow( boundaryLayerIndex ), belowXIntersectionsTable, self.interfaceStep )
		supportLayer.xIntersectionsTable = euclidean.getIntersectionOfXIntersectionsTables( [ aboveXIntersectionsTable, belowXIntersectionsTable ] )

	def addFlowRateLineIfDifferent( self, flowRateOutputString ):
		"Add a line of flow rate if different."
		if self.operatingFlowRate == None:
			return
		if flowRateOutputString == self.oldFlowRateOutputString:
			return
		if flowRateOutputString != None:
			self.distanceFeedRate.addLine('M108 S' + flowRateOutputString )
		self.oldFlowRateOutputString = flowRateOutputString

	def addFlowRateValueIfDifferent( self, flowRate ):
		"Add a flow rate value if different."
		if flowRate != None:
			self.addFlowRateLineIfDifferent( euclidean.getFourSignificantFigures( flowRate ) )

	def addInterfaceLayer(self):
		"Add an interface layer."
		interfaceLayerThickness = self.layerThickness * self.interfaceLayerThicknessOverLayerThickness
		zCenter = self.extrusionTop + 0.5 * interfaceLayerThickness
		z = zCenter + interfaceLayerThickness * self.repository.interfaceNozzleLiftOverInterfaceLayerThickness.value
		self.interfaceIntersectionsTableKeys.sort()
		if len( self.interfaceSegments ) < 1:
			print('This should never happen, the interface layer has a size of zero.')
			return
		feedRateMultiplier = self.repository.interfaceFeedRateMultiplier.value
		flowRateMultiplier = self.repository.interfaceFlowRateMultiplier.value
		self.addLayerFromSegments( feedRateMultiplier, flowRateMultiplier, interfaceLayerThickness, self.interfaceLayerThicknessOverLayerThickness, self.interfaceSegments, z )

	def addInterfaceTables( self, baseStep, interfaceExtrusionWidth ):
		"Add a interface tables."
		interfaceOverhang = self.repository.infillOverhangOverExtrusionWidth.value * interfaceExtrusionWidth
		self.interfaceSegments = []
		self.interfaceIntersectionsTableKeys = self.interfaceIntersectionsTable.keys()
		self.interfaceSegmentsTable = {}
		for yKey in self.interfaceIntersectionsTableKeys:
			self.interfaceIntersectionsTable[ yKey ].sort()
			y = yKey * self.interfaceStep
			lineSegments = euclidean.getSegmentsFromXIntersections( self.interfaceIntersectionsTable[ yKey ], y )
			xIntersectionIndexList = []
			for lineSegmentIndex in xrange( len( lineSegments ) ):
				lineSegment = lineSegments[ lineSegmentIndex ]
				endpointBegin = lineSegment[0]
				endpointEnd = lineSegment[1]
				endpointBegin.point = complex( baseStep * math.floor( endpointBegin.point.real / baseStep ) - interfaceOverhang, y )
				endpointEnd.point = complex( baseStep * math.ceil( endpointEnd.point.real / baseStep ) + interfaceOverhang, y )
				if endpointEnd.point.real > endpointBegin.point.real:
					euclidean.addXIntersectionIndexesFromSegment( lineSegmentIndex, lineSegment, xIntersectionIndexList )
			xIntersections = euclidean.getJoinOfXIntersectionIndexes( xIntersectionIndexList )
			joinedSegments = euclidean.getSegmentsFromXIntersections( xIntersections, y )
			if len( joinedSegments ) > 0:
				self.interfaceSegmentsTable[ yKey ] = joinedSegments
			self.interfaceSegments += joinedSegments

	def addLayerFromSegments( self, feedRateMultiplier, flowRateMultiplier, layerLayerThickness, layerThicknessRatio, segments, z ):
		"Add a layer from segments and raise the extrusion top."
		layerThicknessRatioSquared = layerThicknessRatio * layerThicknessRatio
		feedRateMinute = self.feedRateMinute * feedRateMultiplier / layerThicknessRatioSquared
		endpoints = euclidean.getEndpointsFromSegments( segments )
		if len( endpoints ) < 1:
			return
		aroundPixelTable = {}
		aroundWidth = 0.25 * layerLayerThickness
		paths = euclidean.getPathsFromEndpoints( endpoints, layerLayerThickness, aroundPixelTable, aroundWidth )
		paths = euclidean.getConnectedPaths( paths, aroundPixelTable, aroundWidth ) # this is probably unnecesary
		self.addLayerLine(z)
		self.addFlowRateValueIfDifferent( flowRateMultiplier * self.oldFlowRateInput )
		for path in paths:
			simplifiedPath = euclidean.getSimplifiedPath( path, layerLayerThickness )
			self.distanceFeedRate.addGcodeFromFeedRateThreadZ( feedRateMinute, simplifiedPath, z )
		self.extrusionTop += layerLayerThickness
		self.addFlowRateValueIfDifferent( self.oldFlowRateInput )

	def addLayerLine(self, z):
		"Add the layer gcode line and close the last layer gcode block."
		if self.layerStarted:
			self.distanceFeedRate.addLine('(</layer>)')
		self.distanceFeedRate.addLine('(<layer> %s )' % self.distanceFeedRate.getRounded(z)) # Indicate that a new layer is starting.
		if self.beginLoop != None:
			zBegin = self.extrusionTop + self.layerThickness
			intercircle.addOrbitsIfLarge(self.distanceFeedRate, self.beginLoop, self.orbitalFeedRatePerSecond, self.temperatureChangeTimeBeforeRaft, zBegin)
			self.beginLoop = None
		self.layerStarted = True

	def addOperatingOrbits( self, boundaryLoops, pointComplex, temperatureChangeTime, z ):
		"Add the orbits before the operating layers."
		if len( boundaryLoops ) < 1:
			return
		insetBoundaryLoops = intercircle.getInsetLoopsFromLoops( self.perimeterWidth, boundaryLoops )
		if len( insetBoundaryLoops ) < 1:
			insetBoundaryLoops = boundaryLoops
		largestLoop = euclidean.getLargestLoop( insetBoundaryLoops )
		if pointComplex != None:
			largestLoop = euclidean.getLoopStartingNearest( self.perimeterWidth, pointComplex, largestLoop )
		intercircle.addOrbitsIfLarge( self.distanceFeedRate, largestLoop, self.orbitalFeedRatePerSecond, temperatureChangeTime, z )

	def addRaft(self):
		"Add the raft."
		if len( self.boundaryLayers ) < 0:
			print('this should never happen, there are no boundary layers in addRaft')
			return
		self.baseLayerThicknessOverLayerThickness = self.repository.baseLayerThicknessOverLayerThickness.value
		baseExtrusionWidth = self.perimeterWidth * self.baseLayerThicknessOverLayerThickness
		baseStep = baseExtrusionWidth / self.repository.baseInfillDensity.value
		self.interfaceLayerThicknessOverLayerThickness = self.repository.interfaceLayerThicknessOverLayerThickness.value
		interfaceExtrusionWidth = self.perimeterWidth * self.interfaceLayerThicknessOverLayerThickness
		self.interfaceStep = interfaceExtrusionWidth / self.repository.interfaceInfillDensity.value
		self.setCornersZ()
		self.cornerLowComplex = self.cornerLow.dropAxis(2)
		originalExtent = self.cornerHighComplex - self.cornerLowComplex
		self.raftOutsetRadius = self.repository.raftMargin.value + self.repository.raftAdditionalMarginOverLengthPercent.value * 0.01 * max( originalExtent.real, originalExtent.imag )
		self.setBoundaryLayers()
		outsetSeparateLoops = intercircle.getInsetSeparateLoopsFromLoops( - self.raftOutsetRadius, self.boundaryLayers[0].loops, 0.8 )
		self.interfaceIntersectionsTable = {}
		euclidean.addXIntersectionsFromLoopsForTable( outsetSeparateLoops, self.interfaceIntersectionsTable, self.interfaceStep )
		if len( self.supportLayers ) > 0:
			supportIntersectionsTable = self.supportLayers[0].xIntersectionsTable
			euclidean.joinXIntersectionsTables( supportIntersectionsTable, self.interfaceIntersectionsTable )
		self.addInterfaceTables( baseStep, interfaceExtrusionWidth )
		self.baseIntersectionsTable = {}
		complexRadius = complex( self.raftOutsetRadius, self.raftOutsetRadius )
		self.complexHigh = complexRadius + self.cornerHighComplex
		self.complexLow = self.cornerLowComplex - complexRadius
		self.beginLoop = euclidean.getSquareLoopWiddershins( self.cornerLowComplex, self.cornerHighComplex )
		if not intercircle.orbitsAreLarge( self.beginLoop, self.temperatureChangeTimeBeforeRaft ):
			self.beginLoop = None
		if self.repository.baseLayers.value > 0:
			self.addTemperatureLineIfDifferent( self.baseTemperature )
			self.addBaseSegments( baseExtrusionWidth, baseStep )
		for baseLayerIndex in xrange( self.repository.baseLayers.value ):
			self.addBaseLayer()
		if self.repository.interfaceLayers.value > 0:
			self.addTemperatureLineIfDifferent( self.interfaceTemperature )
		for interfaceLayerIndex in xrange( self.repository.interfaceLayers.value ):
			self.addInterfaceLayer()
		self.operatingJump = self.extrusionTop + self.layerThickness * self.repository.operatingNozzleLiftOverLayerThickness.value
		for boundaryLayer in self.boundaryLayers:
			if self.operatingJump != None:
				boundaryLayer.z += self.operatingJump
		if self.repository.baseLayers.value > 0 or self.repository.interfaceLayers.value > 0:
			boundaryZ = self.boundaryLayers[0].z
			if self.layerStarted:
				self.distanceFeedRate.addLine('(</layer>)')
				self.layerStarted = False
			self.distanceFeedRate.addLine('(<raftLayerEnd> </raftLayerEnd>)')
			self.addLayerLine( boundaryZ )
			temperatureChangeTimeBeforeFirstLayer = self.getTemperatureChangeTime( self.objectFirstLayerPerimeterTemperature )
			self.addTemperatureLineIfDifferent( self.objectFirstLayerPerimeterTemperature )
			largestOutsetLoop = intercircle.getLargestInsetLoopFromLoop( euclidean.getLargestLoop( outsetSeparateLoops ), - self.raftOutsetRadius )
			intercircle.addOrbitsIfLarge( self.distanceFeedRate, largestOutsetLoop, self.orbitalFeedRatePerSecond, temperatureChangeTimeBeforeFirstLayer, boundaryZ )
			self.addLineLayerStart = False

	def addSegmentTablesToSupportLayers(self):
		"Add segment tables to the support layers."
		for supportLayer in self.supportLayers:
			supportLayer.supportSegmentTable = {}
			xIntersectionsTable = supportLayer.xIntersectionsTable
			for xIntersectionsTableKey in xIntersectionsTable:
				y = xIntersectionsTableKey * self.interfaceStep
				supportLayer.supportSegmentTable[ xIntersectionsTableKey ] = euclidean.getSegmentsFromXIntersections( xIntersectionsTable[ xIntersectionsTableKey ], y )

	def addSupportSegmentTable( self, layerIndex ):
		"Add support segments from the boundary layers."
		aboveLayer = self.boundaryLayers[ layerIndex + 1 ]
		aboveLoops = aboveLayer.loops
		supportLayer = self.supportLayers[ layerIndex ]
		if len( aboveLoops ) < 1:
			return
		boundaryLayer = self.boundaryLayers[ layerIndex ]
		rise = aboveLayer.z - boundaryLayer.z
		outsetSupportLoops = intercircle.getInsetSeparateLoopsFromLoops( - self.minimumSupportRatio * rise, boundaryLayer.loops )
		numberOfSubSteps = 4
		subStepSize = self.interfaceStep / float( numberOfSubSteps )
		aboveIntersectionsTable = {}
		euclidean.addXIntersectionsFromLoopsForTable( aboveLoops, aboveIntersectionsTable, subStepSize )
		outsetIntersectionsTable = {}
		euclidean.addXIntersectionsFromLoopsForTable( outsetSupportLoops, outsetIntersectionsTable, subStepSize )
		euclidean.subtractXIntersectionsTable( aboveIntersectionsTable, outsetIntersectionsTable )
		for aboveIntersectionsTableKey in aboveIntersectionsTable.keys():
			supportIntersectionsTableKey = int( round( float( aboveIntersectionsTableKey ) / numberOfSubSteps ) )
			xIntersectionIndexList = []
			if supportIntersectionsTableKey in supportLayer.xIntersectionsTable:
				euclidean.addXIntersectionIndexesFromXIntersections( 0, xIntersectionIndexList, supportLayer.xIntersectionsTable[ supportIntersectionsTableKey ] )
			euclidean.addXIntersectionIndexesFromXIntersections( 1, xIntersectionIndexList, aboveIntersectionsTable[ aboveIntersectionsTableKey ] )
			supportLayer.xIntersectionsTable[ supportIntersectionsTableKey ] = euclidean.getJoinOfXIntersectionIndexes( xIntersectionIndexList )

	def addSupportLayerTemperature( self, endpoints, z ):
		"Add support layer and temperature before the object layer."
		self.distanceFeedRate.addLinesSetAbsoluteDistanceMode( self.supportStartLines )
		self.addTemperatureOrbits( endpoints, self.supportedLayersTemperature, z )
		aroundPixelTable = {}
		layerFillInset = 0.9 * self.perimeterWidth
		aroundWidth = 0.12 * layerFillInset
		boundaryLoops = self.boundaryLayers[ self.layerIndex ].loops
		halfSupportOutset = 0.5 * self.supportOutset
		aroundBoundaryLoops = intercircle.getAroundsFromLoops( boundaryLoops, halfSupportOutset )
		for aroundBoundaryLoop in aroundBoundaryLoops:
			euclidean.addLoopToPixelTable( aroundBoundaryLoop, aroundPixelTable, aroundWidth )
		paths = euclidean.getPathsFromEndpoints( endpoints, layerFillInset, aroundPixelTable, aroundWidth )
		self.addFlowRateValueIfDifferent( self.supportFlowRate )
		for path in paths:
			self.distanceFeedRate.addGcodeFromFeedRateThreadZ( self.feedRateMinute, path, z )
		self.addFlowRateLineIfDifferent( str( self.oldFlowRateInput ) )
		self.addTemperatureOrbits( endpoints, self.supportLayersTemperature, z )
		self.distanceFeedRate.addLinesSetAbsoluteDistanceMode( self.supportEndLines )

	def addTemperatureLineIfDifferent( self, temperature ):
		"Add a line of temperature if different."
		if temperature == None:
			return
		temperatureOutputString = euclidean.getRoundedToThreePlaces( temperature )
		if temperatureOutputString == self.oldTemperatureOutputString:
			return
		if temperatureOutputString != None:
			self.distanceFeedRate.addLine('M104 S' + temperatureOutputString ) # Set temperature.
		self.oldTemperatureOutputString = temperatureOutputString

	def addTemperatureOrbits( self, endpoints, temperature, z ):
		"Add the temperature and orbits around the support layer."
		if self.layerIndex < 0:
			return
		boundaryLoops = self.boundaryLayers[ self.layerIndex ].loops
		temperatureTimeChange = self.getTemperatureChangeTime( temperature )
		self.addTemperatureLineIfDifferent( temperature )
		if len( boundaryLoops ) < 1:
			layerCornerHigh = complex(-999999999.0, -999999999.0)
			layerCornerLow = complex(999999999.0, 999999999.0)
			for endpoint in endpoints:
				layerCornerHigh = euclidean.getMaximum( layerCornerHigh, endpoint.point )
				layerCornerLow = euclidean.getMinimum( layerCornerLow, endpoint.point )
			squareLoop = euclidean.getSquareLoopWiddershins( layerCornerLow, layerCornerHigh )
			intercircle.addOrbitsIfLarge( self.distanceFeedRate, squareLoop, self.orbitalFeedRatePerSecond, temperatureTimeChange, z )
			return
		perimeterInset = 0.4 * self.perimeterWidth
		insetBoundaryLoops = intercircle.getInsetLoopsFromLoops( perimeterInset, boundaryLoops )
		if len( insetBoundaryLoops ) < 1:
			insetBoundaryLoops = boundaryLoops
		largestLoop = euclidean.getLargestLoop( insetBoundaryLoops )
		intercircle.addOrbitsIfLarge( self.distanceFeedRate, largestLoop, self.orbitalFeedRatePerSecond, temperatureTimeChange, z )

	def addToFillXIntersectionIndexTables( self, supportLayer ):
		"Add fill segments from the boundary layers."
		supportLoops = supportLayer.supportLoops
		supportLayer.fillXIntersectionsTable = {}
		if len( supportLoops ) < 1:
			return
		euclidean.addXIntersectionsFromLoopsForTable( supportLoops, supportLayer.fillXIntersectionsTable, self.interfaceStep )

	def extendXIntersections( self, loops, radius, xIntersectionsTable ):
		"Extend the support segments."
		xIntersectionsTableKeys = xIntersectionsTable.keys()
		for xIntersectionsTableKey in xIntersectionsTableKeys:
			lineSegments = euclidean.getSegmentsFromXIntersections( xIntersectionsTable[ xIntersectionsTableKey ], xIntersectionsTableKey )
			xIntersectionIndexList = []
			loopXIntersections = []
			euclidean.addXIntersectionsFromLoops( loops, loopXIntersections, xIntersectionsTableKey )
			for lineSegmentIndex in xrange( len( lineSegments ) ):
				lineSegment = lineSegments[ lineSegmentIndex ]
				extendedLineSegment = getExtendedLineSegment( radius, lineSegment, loopXIntersections )
				if extendedLineSegment != None:
					euclidean.addXIntersectionIndexesFromSegment( lineSegmentIndex, extendedLineSegment, xIntersectionIndexList )
			xIntersections = euclidean.getJoinOfXIntersectionIndexes( xIntersectionIndexList )
			if len( xIntersections ) > 0:
				xIntersectionsTable[ xIntersectionsTableKey ] = xIntersections
			else:
				del xIntersectionsTable[ xIntersectionsTableKey ]

	def getCraftedGcode(self, gcodeText, repository):
		"Parse gcode text and store the raft gcode."
		self.repository = repository
		self.minimumSupportRatio = math.tan( math.radians( repository.supportMinimumAngle.value ) )
		self.supportEndText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Support_End.gcode')
		self.supportEndLines = archive.getTextLines( self.supportEndText )
		self.supportStartText = settings.getFileInAlterationsOrGivenDirectory( os.path.dirname( __file__ ), 'Support_Start.gcode')
		self.supportStartLines = archive.getTextLines( self.supportStartText )
		self.lines = archive.getTextLines(gcodeText)
		self.parseInitialization()
		self.temperatureChangeTimeBeforeRaft = 0.0
		if self.repository.initialCircling.value:
			maxBaseInterfaceTemperature = max(self.baseTemperature, self.interfaceTemperature)
			firstMaxTemperature = max(maxBaseInterfaceTemperature, self.objectFirstLayerPerimeterTemperature)
			self.temperatureChangeTimeBeforeRaft = self.getTemperatureChangeTime(firstMaxTemperature)
		if repository.addRaftElevateNozzleOrbitSetAltitude.value:
			self.addRaft()
		self.addTemperatureLineIfDifferent( self.objectFirstLayerPerimeterTemperature )
		for line in self.lines[self.lineIndex :]:
			self.parseLine(line)
		return self.distanceFeedRate.output.getvalue()

	def getElevatedBoundaryLine( self, splitLine ):
		"Get elevated boundary gcode line."
		location = gcodec.getLocationFromSplitLine(None, splitLine)
		if self.operatingJump != None:
			location.z += self.operatingJump
		return self.distanceFeedRate.getBoundaryLine( location )

	def getInsetLoops( self, boundaryLayerIndex ):
		"Inset the support loops if they are not already inset."
		if boundaryLayerIndex not in self.insetTable:
			self.insetTable[ boundaryLayerIndex ] = intercircle.getInsetSeparateLoopsFromLoops( self.quarterPerimeterWidth, self.boundaryLayers[ boundaryLayerIndex ].loops )
		return self.insetTable[ boundaryLayerIndex ]

	def getInsetLoopsAbove( self, boundaryLayerIndex ):
		"Get the inset loops above the boundary layer index."
		for aboveLayerIndex in xrange( boundaryLayerIndex + 1, len( self.boundaryLayers ) ):
			if len( self.boundaryLayers[ aboveLayerIndex ].loops ) > 0:
				return self.getInsetLoops( aboveLayerIndex )
		return []

	def getInsetLoopsBelow( self, boundaryLayerIndex ):
		"Get the inset loops below the boundary layer index."
		for belowLayerIndex in xrange( boundaryLayerIndex - 1, - 1, - 1 ):
			if len( self.boundaryLayers[ belowLayerIndex ].loops ) > 0:
				return self.getInsetLoops( belowLayerIndex )
		return []

	def getRaftedLine( self, splitLine ):
		"Get elevated gcode line with operating feed rate."
		location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
		self.feedRateMinute = gcodec.getFeedRateMinute(self.feedRateMinute, splitLine)
		feedRateMinuteMultiplied = self.feedRateMinute
		self.oldLocation = location
		z = location.z
		if self.operatingJump != None:
			z += self.operatingJump
		if self.layerIndex == 0:
			self.addFlowRateValueIfDifferent(self.repository.objectFirstLayerFlowRateMultiplier.value * self.oldFlowRateInput)
			if self.isPerimeter:
				feedRateMinuteMultiplied *= self.repository.objectFirstLayerFeedRatePerimeterMultiplier.value
				self.addTemperatureLineIfDifferent(self.objectFirstLayerPerimeterTemperature)
			else:
				feedRateMinuteMultiplied *= self.repository.objectFirstLayerFeedRateInfillMultiplier.value
				self.addTemperatureLineIfDifferent(self.objectFirstLayerInfillTemperature)
		else:
			self.addFlowRateValueIfDifferent(self.oldFlowRateInput)
			self.addTemperatureLineIfDifferent(self.objectNextLayersTemperature)
		return self.distanceFeedRate.getLinearGcodeMovementWithFeedRate(feedRateMinuteMultiplied, location.dropAxis(2), z)

	def getStepsUntilEnd( self, begin, end, stepSize ):
		"Get steps from the beginning until the end."
		step = begin
		steps = []
		while step < end:
			steps.append( step )
			step += stepSize
		return steps

	def getSupportEndpoints(self):
		"Get the support layer segments."
		if len( self.supportLayers ) <= self.layerIndex:
			return []
		supportSegmentTable = self.supportLayers[ self.layerIndex ].supportSegmentTable
		endpoints = euclidean.getEndpointsFromSegmentTable( supportSegmentTable )
		if self.layerIndex % 2 == 0 or not self.repository.supportCrossHatch.value:
			return endpoints
		crossEndpoints = []
		crossHatchPointLineTable = {}
		for endpoint in endpoints:
			segmentBeginXStep = int( math.ceil( min( endpoint.point.real, endpoint.otherEndpoint.point.real ) / self.interfaceStep ) )
			segmentEndXStep = int( math.ceil( max( endpoint.point.real, endpoint.otherEndpoint.point.real ) / self.interfaceStep ) )
			for step in xrange( segmentBeginXStep, segmentEndXStep ):
				x = self.interfaceStep * step
				crossHatchPointLine = getCrossHatchPointLine( crossHatchPointLineTable, x )
				crossHatchPointLine[ int( round( endpoint.point.imag / self.interfaceStep ) ) ] = True
		crossHatchPointLineTableKeys = crossHatchPointLineTable.keys()
		crossHatchPointLineTableKeys.sort()
		for crossHatchPointLineTableKey in crossHatchPointLineTableKeys:
			crossHatchPointLine = crossHatchPointLineTable[ crossHatchPointLineTableKey ]
			crossHatchPointLineKeys = crossHatchPointLine.keys()
			for crossHatchPointLineKey in crossHatchPointLineKeys:
				if not crossHatchPointLine.has_key( crossHatchPointLineKey - 1 ) and not crossHatchPointLine.has_key( crossHatchPointLineKey + 1 ):
					del crossHatchPointLine[ crossHatchPointLineKey ]
			crossHatchPointLineKeys = crossHatchPointLine.keys()
			crossHatchPointLineKeys.sort()
			yIntersections = []
			for crossHatchPointLineKey in crossHatchPointLineKeys:
				if crossHatchPointLine.has_key( crossHatchPointLineKey - 1 ) != crossHatchPointLine.has_key( crossHatchPointLineKey + 1 ):
					yIntersection = self.interfaceStep * crossHatchPointLineKey
					yIntersections.append( yIntersection )
			crossEndpoints += getEndpointsFromYIntersections( crossHatchPointLineTableKey, yIntersections )
		return crossEndpoints

	def getTemperatureChangeTime( self, temperature ):
		"Get the temperature change time."
		if temperature == None:
			return 0.0
		oldTemperature = 25.0 # typical chamber temperature
		if self.oldTemperatureOutputString != None:
			oldTemperature = float( self.oldTemperatureOutputString )
		if temperature == oldTemperature:
			return 0.0
		if temperature > oldTemperature:
			return ( temperature - oldTemperature ) / self.heatingRate
		return ( oldTemperature - temperature ) / abs( self.coolingRate )

	def parseInitialization(self):
		'Parse gcode initialization and store the parameters.'
		for self.lineIndex in xrange(len(self.lines)):
			line = self.lines[self.lineIndex]
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			self.distanceFeedRate.parseSplitLine(firstWord, splitLine)
			if firstWord == '(<coolingRate>':
				self.coolingRate = float(splitLine[1])
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine('(<procedureDone> raft </procedureDone>)')
			elif firstWord == '(<heatingRate>':
				self.heatingRate = float(splitLine[1])
			elif firstWord == '(<layer>':
				return
			elif firstWord == '(<layerThickness>':
				self.layerThickness = float(splitLine[1])
			elif firstWord == '(<orbitalFeedRatePerSecond>':
				self.orbitalFeedRatePerSecond = float(splitLine[1])
			elif firstWord == '(<operatingFeedRatePerSecond>':
				self.feedRateMinute = 60.0 * float(splitLine[1])
			elif firstWord == '(<operatingFlowRate>':
				self.oldFlowRateInput = float(splitLine[1])
				self.operatingFlowRate = self.oldFlowRateInput
				self.supportFlowRate = self.operatingFlowRate * self.repository.supportFlowRateOverOperatingFlowRate.value
			elif firstWord == '(<perimeterWidth>':
				self.perimeterWidth = float(splitLine[1])
				self.quarterPerimeterWidth = 0.25 * self.perimeterWidth
				self.supportOutset = self.perimeterWidth + self.perimeterWidth * self.repository.supportGapOverPerimeterExtrusionWidth.value
			elif firstWord == '(<baseTemperature>':
				self.baseTemperature = float(splitLine[1])
			elif firstWord == '(<interfaceTemperature>':
				self.interfaceTemperature = float(splitLine[1])
			elif firstWord == '(<objectFirstLayerInfillTemperature>':
				self.objectFirstLayerInfillTemperature = float(splitLine[1])
			elif firstWord == '(<objectFirstLayerPerimeterTemperature>':
				self.objectFirstLayerPerimeterTemperature = float(splitLine[1])
			elif firstWord == '(<objectNextLayersTemperature>':
				self.objectNextLayersTemperature = float(splitLine[1])
			elif firstWord == '(<supportLayersTemperature>':
				self.supportLayersTemperature = float(splitLine[1])
			elif firstWord == '(<supportedLayersTemperature>':
				self.supportedLayersTemperature = float(splitLine[1])
			elif firstWord == '(<travelFeedRatePerSecond>':
				self.travelFeedRatePerMinute = 60.0 * float(splitLine[1])
			self.distanceFeedRate.addLine(line)

	def parseLine(self, line):
		"Parse a gcode line and add it to the raft skein."
		splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
		if len(splitLine) < 1:
			return
		firstWord = splitLine[0]
		if firstWord == 'G1':
			if self.extrusionStart:
				line = self.getRaftedLine(splitLine)
		elif firstWord == 'M101':
			if self.isStartupEarly:
				self.isStartupEarly = False
				return
		elif firstWord == 'M103':
			self.isPerimeter = False
		elif firstWord == 'M108':
			flowRateOutputString = splitLine[1][1 :]
			self.addFlowRateLineIfDifferent( flowRateOutputString )
			self.oldFlowRateInput = float( flowRateOutputString )
		elif firstWord == '(<boundaryPoint>':
			line = self.getElevatedBoundaryLine(splitLine)
		elif firstWord == '(</extrusion>)':
			self.extrusionStart = False
			self.distanceFeedRate.addLine( self.operatingLayerEndLine )
		elif firstWord == '(<layer>':
			settings.printProgress(self.layerIndex, 'raft')
			self.layerIndex += 1
			boundaryLayer = None
			layerZ = self.extrusionTop + float(splitLine[1])
			if len( self.boundaryLayers ) > 0:
				boundaryLayer = self.boundaryLayers[ self.layerIndex ]
				layerZ = boundaryLayer.z
			if self.operatingJump != None:
				line = '(<layer> %s )' % self.distanceFeedRate.getRounded( layerZ )
			if self.layerStarted and self.addLineLayerStart:
				self.distanceFeedRate.addLine('(</layer>)')
			self.layerStarted = False
			if self.layerIndex > len( self.supportLayers ) + 1:
				self.distanceFeedRate.addLine( self.operatingLayerEndLine )
				self.operatingLayerEndLine = ''
			if self.addLineLayerStart:
				self.distanceFeedRate.addLine(line)
			self.addLineLayerStart = True
			line = ''
			endpoints = self.getSupportEndpoints()
			if self.layerIndex == 1:
				if len( endpoints ) < 1:
					temperatureChangeTimeBeforeNextLayers = self.getTemperatureChangeTime( self.objectNextLayersTemperature )
					self.addTemperatureLineIfDifferent( self.objectNextLayersTemperature )
					if self.repository.addRaftElevateNozzleOrbitSetAltitude.value and len( boundaryLayer.loops ) > 0:
						self.addOperatingOrbits( boundaryLayer.loops, euclidean.getXYComplexFromVector3( self.oldLocation ), temperatureChangeTimeBeforeNextLayers, layerZ )
			if len( endpoints ) > 0:
				self.addSupportLayerTemperature( endpoints, layerZ )
		elif firstWord == '(<perimeter>':
			self.isPerimeter = True
		self.distanceFeedRate.addLine(line)

	def setBoundaryLayers(self):
		"Set the boundary layers."
		if self.repository.supportChoiceNone.value:
			return
		if len( self.boundaryLayers ) < 2:
			return
		if self.repository.supportChoiceEmptyLayersOnly.value:
			supportLayer = SupportLayer( [] )
			self.supportLayers.append( supportLayer )
			for boundaryLayerIndex in xrange( 1, len( self.boundaryLayers ) - 1 ):
				self.addEmptyLayerSupport( boundaryLayerIndex )
			self.truncateSupportSegmentTables()
			self.addSegmentTablesToSupportLayers()
			return
		for boundaryLayer in self.boundaryLayers:
			supportLoops = intercircle.getInsetSeparateLoopsFromLoops( - self.supportOutset, boundaryLayer.loops )
			supportLayer = SupportLayer( supportLoops )
			self.supportLayers.append( supportLayer )
		for supportLayerIndex in xrange( len( self.supportLayers ) - 1 ):
			self.addSupportSegmentTable( supportLayerIndex )
		self.truncateSupportSegmentTables()
		for supportLayerIndex in xrange( len( self.supportLayers ) - 1 ):
			self.extendXIntersections( self.boundaryLayers[ supportLayerIndex ].loops, self.supportOutset, self.supportLayers[ supportLayerIndex ].xIntersectionsTable )
		for supportLayer in self.supportLayers:
			self.addToFillXIntersectionIndexTables( supportLayer )
		if self.repository.supportChoiceExteriorOnly.value:
			for supportLayerIndex in xrange( 1, len( self.supportLayers ) ):
				self.subtractJoinedFill( supportLayerIndex )
		for supportLayer in self.supportLayers:
			euclidean.subtractXIntersectionsTable( supportLayer.xIntersectionsTable, supportLayer.fillXIntersectionsTable )
		for supportLayerIndex in xrange( len( self.supportLayers ) - 2, - 1, - 1 ):
			xIntersectionsTable = self.supportLayers[ supportLayerIndex ].xIntersectionsTable
			aboveXIntersectionsTable = self.supportLayers[ supportLayerIndex + 1 ].xIntersectionsTable
			euclidean.joinXIntersectionsTables( aboveXIntersectionsTable, xIntersectionsTable )
		for supportLayerIndex in xrange( len( self.supportLayers ) ):
			supportLayer = self.supportLayers[ supportLayerIndex ]
			self.extendXIntersections( supportLayer.supportLoops, self.raftOutsetRadius, supportLayer.xIntersectionsTable )
		for supportLayer in self.supportLayers:
			euclidean.subtractXIntersectionsTable( supportLayer.xIntersectionsTable, supportLayer.fillXIntersectionsTable )
		self.addSegmentTablesToSupportLayers()

	def setCornersZ(self):
		"Set maximum and minimum corners and z."
		boundaryLoop = None
		boundaryLayer = None
		layerIndex = - 1
		self.cornerHighComplex = complex(-999999999.0, -999999999.0)
		self.cornerLow = Vector3(999999999.0, 999999999.0, 999999999.0)
		self.firstLayerLoops = []
		for line in self.lines[self.lineIndex :]:
			splitLine = gcodec.getSplitLineBeforeBracketSemicolon(line)
			firstWord = gcodec.getFirstWord(splitLine)
			if firstWord == '(</boundaryPerimeter>)':
				boundaryLoop = None
			elif firstWord == '(<boundaryPoint>':
				location = gcodec.getLocationFromSplitLine(None, splitLine)
				if boundaryLoop == None:
					boundaryLoop = []
					boundaryLayer.loops.append( boundaryLoop )
				boundaryLoop.append( location.dropAxis(2) )
				self.cornerHighComplex = euclidean.getMaximum( self.cornerHighComplex, location.dropAxis(2) )
				self.cornerLow = euclidean.getPointMinimum( self.cornerLow, location )
			elif firstWord == '(<layer>':
				z = float(splitLine[1])
				boundaryLayer = euclidean.LoopLayer(z)
				self.boundaryLayers.append( boundaryLayer )
			elif firstWord == '(<layer>':
				layerIndex += 1
				if self.repository.supportChoiceNone.value:
					if layerIndex > 1:
						return

	def subtractJoinedFill( self, supportLayerIndex ):
		"Join the fill then subtract it from the support layer table."
		supportLayer = self.supportLayers[ supportLayerIndex ]
		fillXIntersectionsTable = supportLayer.fillXIntersectionsTable
		belowFillXIntersectionsTable = self.supportLayers[ supportLayerIndex - 1 ].fillXIntersectionsTable
		euclidean.joinXIntersectionsTables( belowFillXIntersectionsTable, supportLayer.fillXIntersectionsTable )
		euclidean.subtractXIntersectionsTable( supportLayer.xIntersectionsTable, supportLayer.fillXIntersectionsTable )

	def truncateSupportSegmentTables(self):
		"Truncate the support segments after the last support segment which contains elements."
		for supportLayerIndex in xrange( len( self.supportLayers ) - 1, - 1, - 1 ):
			if len( self.supportLayers[ supportLayerIndex ].xIntersectionsTable ) > 0:
				self.supportLayers = self.supportLayers[ : supportLayerIndex + 1 ]
				return
		self.supportLayers = []


class SupportLayer:
	"Support loops with segment tables."
	def __init__( self, supportLoops ):
		self.supportLoops = supportLoops
		self.supportSegmentTable = {}
		self.xIntersectionsTable = {}

	def __repr__(self):
		"Get the string representation of this loop layer."
		return '%s' % ( self.supportLoops )


def main():
	"Display the raft dialog."
	if len(sys.argv) > 1:
		writeOutput(' '.join(sys.argv[1 :]))
	else:
		settings.startMainLoopFromConstructor( getNewRepository() )

if __name__ == "__main__":
	main()
