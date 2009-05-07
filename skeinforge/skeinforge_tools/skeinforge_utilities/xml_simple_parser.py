"""
The xml_simple_parser.py script is an xml parser that can parse a line separated xml text.

This xml parser will read a line seperated xml text and produce a tree of the xml with a root element.  Each element can
have an attribute table, children, a class name, parents, text and a link to the root element.

This example gets an xml tree for the xml file boolean.xml.  This example is run in a terminal in the folder which contains
boolean.xml and xml_simple_parser.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> file = open( 'boolean.xml', 'r' )
>>> xmlText = file.read()
>>> file.close()
>>> from xml_simple_parser import XMLSimpleParser
>>> xmlParser = XMLSimpleParser( xmlText )
>>> print( xmlParser )
  ?xml, {'version': '1.0'}
  ArtOfIllusion, {'xmlns:bf': '//babelfiche/codec', 'version': '2.0', 'fileversion': '3'}
  Scene, {'bf:id': 'theScene'}
  materials, {'bf:elem-type': 'java.lang.Object', 'bf:list': 'collection', 'bf:id': '1', 'bf:type': 'java.util.Vector'}
..
many more lines of the xml tree
..

"""


from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import euclidean
from skeinforge_tools.skeinforge_utilities import gcodec

__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__credits__ = 'Nophead <http://hydraraptor.blogspot.com/>\nArt of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


class XMLElement:
	"An xml element."
	def __init__( self ):
		"Add empty lists."
		self.attributeTable = {}
		self.children = []
		self.className = ''
		self.parents = []
		self.rootElement = None
		self.text = ''

	def __repr__( self ):
		"Get the string representation of this XML element."
		stringRepresentation = '%s%s, %s' % ( '  ' * len( self.parents ), self.className, self.attributeTable )
		if len( self.text ):
			stringRepresentation += '\n%s%s' % ( '  ' * len( self.parents ), self.text )
		for child in self.children:
			stringRepresentation += '\n%s' % child
		return stringRepresentation

	def addAttribute( self, word ):
		"Set the attribute table to the split line."
		indexOfEqualSign = word.find( '=' )
		key = word[ : indexOfEqualSign ]
		afterEqualSign = word[ indexOfEqualSign + 1 : ]
		afterEqualSign = afterEqualSign.lstrip()
		value = afterEqualSign[ 1 : - 1 ]
		self.attributeTable[ key ] = value

	def getChildrenWithClassName( self, className ):
		"Get the children which have the given class name."
		childrenWithClassName = []
		for child in self.children:
			if className == child.className:
				childrenWithClassName.append( child )
		return childrenWithClassName

	def getFirstChildWithClassName( self, className ):
		"Get the first child which has the given class name."
		childrenWithClassName = self.getChildrenWithClassName( className )
		if len( childrenWithClassName ) < 1:
			return None
		return childrenWithClassName[ 0 ]

	def getSubChildWithID( self, idReference ):
		"Get the child which has the idReference."
		for child in self.children:
			if 'bf:id' in child.attributeTable:
				if child.attributeTable[ 'bf:id' ] == idReference:
					return child
			subChildWithID = child.getSubChildWithID( idReference )
			if subChildWithID != None:
				return subChildWithID
		return None

	def parseReplacedLine( self, line, parents ):
		"Parse replaced line."
		if line[ : len( '</' ) ] == '</':
			del parents[ - 1 ]
			return
		self.className = line[ 1 : line.replace( '>', ' ' ).find( ' ' ) ]
		indexOfEndOfTheBeginTag = - 1
		lastWord = line[ - 2 : ]
		splitLine = line.replace( '">', '" > ' ).split()
		if lastWord == '/>':
			indexOfEndOfTheBeginTag = len( splitLine ) - 1
		elif '>' in splitLine:
			indexOfEndOfTheBeginTag = splitLine.index( '>' )
		for word in splitLine[ 1 : indexOfEndOfTheBeginTag ]:
			self.addAttribute( word )
		self.parents = parents
		if len( self.parents ) > 0:
			parents[ - 1 ].children.append( self )
		if lastWord == '/>':
			return
		tagEnd = '</%s>' % self.className
		if line[ - len( tagEnd ) : ] == tagEnd:
			untilTagEnd = line[ : - len( tagEnd ) ]
			lastGreaterThanIndex = untilTagEnd.rfind( '>' )
			self.text = untilTagEnd[ lastGreaterThanIndex + 1 : ]
			return
		parents.append( self )


class XMLSimpleParser:
	"A simple xml parser."
	def __init__( self, xmlText ):
		"Add empty lists."
		self.isInComment = False
		self.parents = []
		self.rootElement = None
		self.lines = gcodec.getTextLines( xmlText )
		for line in self.lines:
			self.parseLine( line )
	
	def __repr__( self ):
		"Get the string representation of this parser."
		return str( self.rootElement )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the inset skein."
		lineStripped = line.lstrip()
		if len( lineStripped ) < 1:
			return
		if lineStripped[ : len( '<!--' ) ] == '<!--':
			self.isInComment = True
		if self.isInComment:
			if lineStripped.find( '-->' ) != - 1:
				self.isInComment = False
				return
		if self.isInComment:
			return
		xmlElement = XMLElement()
		xmlElement.parseReplacedLine( lineStripped, self.parents )
		if self.rootElement == None:
			self.rootElement = xmlElement
		xmlElement.rootElement = self.rootElement
