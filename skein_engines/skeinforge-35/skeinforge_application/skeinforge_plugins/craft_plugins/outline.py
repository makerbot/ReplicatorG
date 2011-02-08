"""
Outline is a script to add an intial extruded bounding box outline printed prior to the main object. The purpose is both to help let the extruder clear any initial void (PLA tends to slowly ooze out the barrel when sitting idle) and to allow early verification whether the object to be printed will fit on the build platform.

The Outline script was written by Len Trigg, based on the Rpmify script.

In order to install the Outline script within the skeinforge tool chain, put outline.py in the skeinforge_application/skeinforge_plugins/craft_plugins/ folder. Then edit  skeinforge_application/skeinforge_plugins/profile_plugins/extrusion.py and add the Outline script to the tool chain sequence by inserting 'outline' into the tool sequence  in getCraftSequence(). The best place is at the end of the sequence, right before 'export'.

==Operation==
The default 'Activate Outline' checkbox is off, enable it if you would like an outline printed.

==Settings==
===Outline Margin===
"Outline Margin" is the number of mm margin between the outline and the bounding box of the base of the object.

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_application.skeinforge_utilities import skeinforge_profile
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
from fabmetheus_utilities import gcodec
from fabmetheus_utilities import archive
from fabmetheus_utilities.fabmetheus_tools import fabmetheus_interpret
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_craft
import sys

__author__ = "Len Trigg (lenbok@gmail.com)"
__date__ = "$Date: 2010/11/20 $"
__license__ = "GPL 3.0"


def getCraftedText( fileName, text = '', repository = None ):
	"Outline the file or text."
	return getCraftedTextFromText( archive.getTextIfEmpty( fileName, text ), repository )

def getCraftedTextFromText( gcodeText, repository = None ):
	"Outline a gcode linear move text."
	if gcodec.isProcedureDoneOrFileIsEmpty( gcodeText, 'outline' ):
		return gcodeText
	if repository == None:
		repository = settings.getReadRepository( OutlineRepository() )
	if not repository.activateOutline.value:
		return gcodeText
	return OutlineSkein().getCraftedGcode( gcodeText, repository )

#def getRepositoryConstructor():
#	"Get the repository constructor."
#	return OutlineRepository()

def getNewRepository():
	"Get the repository constructor."
	return OutlineRepository()

def writeOutput( fileName = ''):
	"Outline a gcode linear move file."
	fileName = fabmetheus_interpret.getFirstTranslatorFileNameUnmodified(fileName)
	if fileName == '':
		return
	skeinforge_craft.writeChainTextWithNounMessage( fileName, 'outline')

class OutlineRepository:
	"A class to handle the outline settings."
	def __init__( self ):
		"Set the default settings, execute title & settings fileName."
		skeinforge_profile.addListsToCraftTypeRepository( 'skeinforge_tools.craft_plugins.outline.html', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( fabmetheus_interpret.getGNUTranslatorGcodeFileTypeTuples(), 'Open File for Outline', self, '' )
		self.activateOutline = settings.BooleanSetting().getFromValue( 'Activate Outline', self, False )
		self.outlineMargin = settings.FloatSpin().getFromValue( 0.5, 'Outline Margin:', self, 20.0, 3.0 )
		self.executeTitle = 'Outline'

	def execute( self ):
		"Outline button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, fabmetheus_interpret.getImportPluginFilenames(), self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName )
			
			

class OutlineSkein:
	"A class to outline a skein of extrusions."
	def __init__( self ):
		self.distanceFeedRate = gcodec.DistanceFeedRate()
		self.lineIndex = 0
		self.lines = None
		self.location = Vector3(0,0,0)
		self.oldLocation = Vector3(0,0,0)
                self.currentLayer = 0
		self.minX = 0
		self.minY = 0
		self.maxX = 0
		self.maxY = 0
                self.firstZ = 0
                self.firstFeed = -1
		self.wantsOutline = False
	
        def updateBoundingBox( self, splitLine ):
                "Updates the information about the first layer bounding box"
                location = gcodec.getLocationFromSplitLine(None, splitLine);
                self.firstZ = location.z
                if location.x < self.minX or self.firstFeed == -1:
                        self.minX = location.x
                elif location.x > self.maxX or self.firstFeed == -1:
                        self.maxX = location.x
                if location.y < self.minY or self.firstFeed == -1:
                        self.minY = location.y
                elif location.y > self.maxY or self.firstFeed == -1:
                        self.maxY = location.y
                if gcodec.getFeedRateMinute(self.firstFeed, splitLine) < self.firstFeed or self.firstFeed == -1:
                        self.firstFeed = gcodec.getFeedRateMinute(self.firstFeed, splitLine)

	def getFirstLayerBoundingBox( self ):
                "Find the bounding coords of the first layer, return as soon as subsequent layer is hit"
                layer = 0
                extruding = False
		for line in self.lines:
                        #print("First line scan", line)
                        splitLine = line.split()
                        if len( splitLine ) >= 1:
                                firstWord = splitLine[ 0 ]
                                if firstWord == '(<layer>':
                                        layer = layer+1
                                        if layer > 1:
                                                return
                                elif firstWord == 'M101':
                                        extruding = True
                                elif firstWord == 'M103':
                                        extruding = False
                                elif layer==1 and extruding and firstWord == 'G1':
                                        self.updateBoundingBox(splitLine)

        def addBoundingBox( self ):
                "Outputs the bounding box gcodes after adding on the specified margin."
                self.wantsOutline = False
                if self.firstFeed == -1:
                        print("No bounding box has been extracted from the first layer, so no outline added")
                        return
                #print("Adding margin ", self.repository.outlineMargin.value)
                self.minX = self.minX - self.repository.outlineMargin.value
                self.maxX = self.maxX + self.repository.outlineMargin.value
                self.minY = self.minY - self.repository.outlineMargin.value
                self.maxY = self.maxY + self.repository.outlineMargin.value

                bbox = ( Vector3(self.minX, self.minY, self.firstZ),
                         Vector3(self.minX, self.maxY, self.firstZ),
                         Vector3(self.maxX, self.maxY, self.firstZ),
                         Vector3(self.maxX, self.minY, self.firstZ) )


                dist = [self.oldLocation.distanceSquared(vec) for vec in bbox]
                closestidx = dist.index(min(dist))

                for i in range(5):
                        pos = bbox[(closestidx + i) % 4]
                        self.distanceFeedRate.addLine('G1 ' + 'X' + self.distanceFeedRate.getRounded(pos.x) + ' Y' + self.distanceFeedRate.getRounded(pos.y) + ' Z' + self.distanceFeedRate.getRounded(pos.z) + ' F' + self.distanceFeedRate.getRounded( self.firstFeed ))
                        if i == 0: self.distanceFeedRate.addLine('M101')
                self.distanceFeedRate.addLine('M103')


	def getCraftedGcode( self, gcodeText, repository ):
		"Parse gcode text and add the outline gcode."
		self.repository = repository
		self.wantsOutline = self.repository.activateOutline.value
		self.lines = archive.getTextLines( gcodeText )
                self.parseInitialization()
                self.getFirstLayerBoundingBox( )
		#print( "First layer bounding box is ", self.minX, ",", self.minY, " - ", self.maxX, ",", self.maxY, " at Z", self.firstZ, ", selected F", self.firstFeed )
		for line in self.lines[ self.lineIndex : ]:
			self.parseLine( line )
		return self.distanceFeedRate.output.getvalue()
	
	def parseInitialization( self ):
		"Parse gcode initialization and store the parameters."
		for self.lineIndex in xrange( len( self.lines ) ):
			line = self.lines[ self.lineIndex ]
			splitLine = line.split()
			firstWord = gcodec.getFirstWord( splitLine )
                        if firstWord == 'G1':
                                self.oldLocation = self.location
                                self.location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
			elif firstWord == '(</extruderInitialization>)':
				self.distanceFeedRate.addLine( '(<procedureDone> outline </procedureDone>)' )
				return
			self.distanceFeedRate.addLine( line )
	
	def parseLine( self, line ):
		"Parse a gcode line and add it to the outline skein. Insert bounding box before first printing line of first layer."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == '(<layer>':
			self.currentLayer=self.currentLayer+1
                elif firstWord == 'G1':
                        self.oldLocation = self.location
                        self.location = gcodec.getLocationFromSplitLine(self.oldLocation, splitLine)
                        if self.wantsOutline and self.currentLayer==1:
                                self.addBoundingBox()
		self.distanceFeedRate.addLine( line )

def main():
	"Display the outline dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		settings.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()
