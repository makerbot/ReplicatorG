"""
The gts.py script is an import translator plugin to get a carving from an gts file.

An import plugin is a script in the import_plugins folder which has the function getCarving.  It is meant to be run from the interpret tool.  To ensure that the plugin works on platforms which do not handle file capitalization properly, give the plugin a lower case name.

The getCarving function takes the file name of an gts file and returns the carving.

The GNU Triangulated Surface (.gts) format is described at:
http://gts.sourceforge.net/reference/gts-surfaces.html#GTS-SURFACE-WRITE

This example gets a carving for the gts file Screw Holder Bottom.gts.  This example is run in a terminal in the folder which contains Screw Holder Bottom.gts and gts.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import gts
>>> gts.getCarving()
[11.6000003815, 10.6837882996, 7.80209827423
..
many more lines of the carving
..

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities.vector3 import Vector3
from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import triangle_mesh

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def getFromGNUTriangulatedSurfaceText( gnuTriangulatedSurfaceText, triangleMesh ):
	"Initialize from a GNU Triangulated Surface Text."
	if gnuTriangulatedSurfaceText == '':
		return None
	lines = gcodec.getTextLines( gnuTriangulatedSurfaceText )
	linesWithoutComments = []
	for line in lines:
		if len( line ) > 0:
			firstCharacter = line[ 0 ]
			if firstCharacter != '#' and firstCharacter != '!':
				linesWithoutComments.append( line )
	splitLine = linesWithoutComments[ 0 ].split()
	numberOfVertices = int( splitLine[ 0 ] )
	numberOfEdges = int( splitLine[ 1 ] )
	numberOfFaces = int( splitLine[ 2 ] )
	faceTriples = []
	for vertexIndex in xrange( numberOfVertices ):
		line = linesWithoutComments[ vertexIndex + 1 ]
		splitLine = line.split()
		vertex = Vector3( float( splitLine[ 0 ] ), float( splitLine[ 1 ] ), float( splitLine[ 2 ] ) )
		triangleMesh.vertices.append( vertex )
	edgeStart = numberOfVertices + 1
	for edgeIndex in xrange( numberOfEdges ):
		line = linesWithoutComments[ edgeIndex + edgeStart ]
		splitLine = line.split()
		vertexIndexes = []
		for word in splitLine[ : 2 ]:
			vertexIndexes.append( int( word ) - 1 )
		edge = triangle_mesh.Edge().getFromVertexIndexes( edgeIndex, vertexIndexes )
		triangleMesh.edges.append( edge )
	faceStart = edgeStart + numberOfEdges
	for faceIndex in xrange( numberOfFaces ):
		line = linesWithoutComments[ faceIndex + faceStart ]
		splitLine = line.split()
		edgeIndexes = []
		for word in splitLine[ : 3 ]:
			edgeIndexes.append( int( word ) - 1 )
		face = triangle_mesh.Face().getFromEdgeIndexes( edgeIndexes, triangleMesh.edges, faceIndex )
		triangleMesh.faces.append( face )
	return triangleMesh

def getCarving( fileName = '' ):
	"Get the triangle mesh for the gts file."
	if fileName == '':
		unmodified = gcodec.getFilesWithFileTypeWithoutWords( 'gts' )
		if len( unmodified ) == 0:
			print( "There is no gts file in this folder." )
			return None
		fileName = unmodified[ 0 ]
	return getFromGNUTriangulatedSurfaceText( gcodec.getFileText( fileName ), triangle_mesh.TriangleMesh() )
