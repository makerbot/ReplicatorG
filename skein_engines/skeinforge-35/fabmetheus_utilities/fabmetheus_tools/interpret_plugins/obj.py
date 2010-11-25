"""
This page is in the table of contents.
The obj.py script is an import translator plugin to get a carving from an obj file.

An example obj file is box.obj in the models folder.

An import plugin is a script in the interpret_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an obj file and returns the carving.

From wikipedia, OBJ (or .OBJ) is a geometry definition file format first developed by Wavefront Technologies for its Advanced Visualizer animation package:
http://en.wikipedia.org/wiki/Obj

The Object File specification is at:
http://local.wasp.uwa.edu.au/~pbourke/dataformats/obj/

An excellent link page about obj files is at:
http://people.sc.fsu.edu/~burkardt/data/obj/obj.html

This example gets a carving for the obj file box.obj.  This example is run in a terminal in the folder which contains box.obj and obj.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import obj
>>> obj.getCarving()
[-62.0579, -41.4791, 0.0, 58.8424, -41.4791, 0.0, -62.0579, 22.1865, 0.0, 58.8424, 22.1865, 0.0,
-62.0579, -41.4791, 39.8714, 58.8424, -41.4791, 39.8714, -62.0579, 22.1865, 39.8714, 58.8424, 22.1865, 39.8714]
[0 [0, 10] [0, 2], 1 [0, 1] [0, 3], 2 [0, 8] [2, 3], 3 [1, 6] [1, 3], 4 [1, 4] [0, 1], 5 [2, 5] [4, 5], 6 [2, 3] [4, 7], 7 [2, 7] [5, 7],
8 [3, 9] [6, 7], 9 [3, 11] [4, 6], 10 [4, 5] [0, 5], 11 [4, 7] [1, 5], 12 [5, 10] [0, 4], 13 [6, 7] [1, 7], 14 [6, 9] [3, 7],
15 [8, 9] [3, 6], 16 [8, 11] [2, 6], 17 [10, 11] [2, 4]]
[0 [0, 1, 2] [0, 2, 3], 1 [3, 1, 4] [3, 1, 0], 2 [5, 6, 7] [4, 5, 7], 3 [8, 6, 9] [7, 6, 4], 4 [4, 10, 11] [0, 1, 5], 5 [5, 10, 12] [5, 4, 0],
6 [3, 13, 14] [1, 3, 7], 7 [7, 13, 11] [7, 5, 1], 8 [2, 15, 16] [3, 2, 6], 9 [8, 15, 14] [6, 7, 3], 10 [0, 17, 12] [2, 0, 4],
11 [9, 17, 16] [4, 6, 2]][11.6000003815, 10.6837882996, 7.80209827423

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.geometry_tools import face
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import archive
from fabmetheus_utilities import gcodec
from struct import unpack

__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def addFacesGivenText( objText, triangleMesh ):
	"Add faces given obj text."
	lines = archive.getTextLines( objText )
	for line in lines:
		splitLine = line.split()
		firstWord = gcodec.getFirstWord(splitLine)
		if firstWord == 'v':
			triangleMesh.vertexes.append( getVertexGivenLine(line) )
		elif firstWord == 'f':
			triangleMesh.faces.append( getFaceGivenLine( line, triangleMesh ) )

def getFaceGivenLine( line, triangleMesh ):
	"Add face given line index and lines."
	faceGivenLine = face.Face()
	faceGivenLine.index = len( triangleMesh.faces )
	splitLine = line.split()
	for vertexStringIndex in xrange( 1, 4 ):
		vertexString = splitLine[ vertexStringIndex ]
		vertexStringWithSpaces = vertexString.replace('/', ' ')
		vertexStringSplit = vertexStringWithSpaces.split()
		vertexIndex = int( vertexStringSplit[0] ) - 1
		faceGivenLine.vertexIndexes.append(vertexIndex)
	return faceGivenLine

def getCarving(fileName=''):
	"Get the triangle mesh for the obj file."
	if fileName == '':
		return None
	objText = archive.getFileText( fileName, 'rb')
	if objText == '':
		return None
	triangleMesh = trianglemesh.TriangleMesh()
	addFacesGivenText( objText, triangleMesh )
	triangleMesh.setEdgesForAllFaces()
	return triangleMesh

def getVertexGivenLine(line):
	"Get vertex given obj vertex line."
	splitLine = line.split()
	return Vector3( float(splitLine[1]), float( splitLine[2] ), float( splitLine[3] ) )
