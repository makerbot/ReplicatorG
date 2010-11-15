"""
Square path.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = LineDerivation()
		derivation.setToXMLElement(xmlElement)
	endMinusStart = derivation.end - derivation.start
	endMinusStartLength = abs(endMinusStart)
	if endMinusStartLength <= 0.0:
		print('Warning, end is the same as start in getGeometryOutput in line for:')
		print(derivation.start)
		print(derivation.end)
		print(xmlElement)
		return None
	typeStringTwoCharacters = derivation.typeString.lower()[: 2]
	xmlElement.attributeDictionary['closed'] = str(derivation.closed)
	if derivation.step == None and derivation.steps == None:
		return lineation.getGeometryOutputByLoop(lineation.SideLoop([derivation.start, derivation.end]), xmlElement)
	loop = [derivation.start]
	if derivation.step != None and derivation.steps != None:
		stepVector = derivation.step / endMinusStartLength * endMinusStart
		derivation.end = derivation.start + stepVector * derivation.steps
		return getGeometryOutputByStep(derivation.end, loop, derivation.steps, stepVector, xmlElement)
	if derivation.step == None:
		stepVector = endMinusStart / derivation.steps
		return getGeometryOutputByStep(derivation.end, loop, derivation.steps, stepVector, xmlElement)
	endMinusStartLengthOverStep = endMinusStartLength / derivation.step
	if typeStringTwoCharacters == 'av':
		derivation.steps = max(1.0, round(endMinusStartLengthOverStep))
		stepVector = derivation.step / endMinusStartLength * endMinusStart
		derivation.end = derivation.start + stepVector * derivation.steps
		return getGeometryOutputByStep(derivation.end, loop, derivation.steps, stepVector, xmlElement)
	if typeStringTwoCharacters == 'ma':
		derivation.steps = math.ceil(endMinusStartLengthOverStep)
		if derivation.steps < 1.0:
			return lineation.getGeometryOutputByLoop(lineation.SideLoop([derivation.start, derivation.end]), xmlElement)
		stepVector = endMinusStart / derivation.steps
		return getGeometryOutputByStep(derivation.end, loop, derivation.steps, stepVector, xmlElement)
	if typeStringTwoCharacters == 'mi':
		derivation.steps = math.floor(endMinusStartLengthOverStep)
		if derivation.steps < 1.0:
			return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)
		stepVector = endMinusStart / derivation.steps
		return getGeometryOutputByStep(derivation.end, loop, derivation.steps, stepVector, xmlElement)
	print('Warning, the step type was not one of (average, maximum or minimum) in getGeometryOutput in line for:')
	print(derivation.typeString)
	print(xmlElement)
	loop.append(derivation.end)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['start', 'end', 'step'], arguments, xmlElement)
	return getGeometryOutput(None, xmlElement)

def getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement):
	"Get line geometry output by the end, loop, steps and stepVector."
	stepsFloor = int(math.floor(abs(steps)))
	for stepIndex in xrange(1, stepsFloor):
		loop.append(loop[stepIndex - 1] + stepVector)
	loop.append(end)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(None, xmlElement), xmlElement)


class LineDerivation:
	"Class to hold line variables."
	def __init__(self):
		'Set defaults.'
		self.closed = False
		self.end = Vector3()
		self.step = None
		self.steps = None
		self.start = Vector3()
		self.typeString = 'minimum'

	def __repr__(self):
		"Get the string representation of this LineDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.closed = evaluate.getEvaluatedBooleanDefault(False, 'closed', xmlElement)
		self.end = evaluate.getVector3ByPrefix(self.end, 'end', xmlElement)
		self.start = evaluate.getVector3ByPrefix(self.start, 'start', xmlElement)
		self.step = evaluate.getEvaluatedFloatDefault(self.step, 'step', xmlElement)
		self.steps = evaluate.getEvaluatedFloatDefault(self.steps, 'steps', xmlElement)
		self.typeString = evaluate.getEvaluatedStringDefault(self.typeString, 'type', xmlElement)
