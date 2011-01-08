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


def getGeometryOutput(xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	start = evaluate.getVector3ByPrefix('start', Vector3(), xmlElement)
	end = evaluate.getVector3ByPrefix('end', Vector3(), xmlElement)
	endMinusStart = end - start
	endMinusStartLength = abs(endMinusStart)
	if endMinusStartLength <= 0.0:
		print('Warning, end is the same as start in getGeometryOutput in line for:')
		print(start)
		print(end)
		print(xmlElement)
		return None
	steps = evaluate.getEvaluatedFloatDefault(None, 'steps', xmlElement)
	step = evaluate.getEvaluatedFloatDefault(None, 'step', xmlElement)
	xmlElement.attributeDictionary['closed'] = str(evaluate.getEvaluatedBooleanDefault(False, 'closed', xmlElement))
	if step == None and steps == None:
		return lineation.getGeometryOutputByLoop(lineation.SideLoop([start, end]), xmlElement)
	loop = [start]
	if step != None and steps != None:
		stepVector = step / endMinusStartLength * endMinusStart
		end = start + stepVector * steps
		return getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement)
	if step == None:
		stepVector = endMinusStart / steps
		return getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement)
	typeString = evaluate.getEvaluatedStringDefault('minimum', 'type', xmlElement)
	endMinusStartLengthOverStep = endMinusStartLength / step
	if typeString == 'average':
		steps = max(1.0, round(endMinusStartLengthOverStep))
		stepVector = step / endMinusStartLength * endMinusStart
		end = start + stepVector * steps
		return getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement)
	if typeString == 'maximum':
		steps = math.ceil(endMinusStartLengthOverStep)
		if steps < 1.0:
			return lineation.getGeometryOutputByLoop(lineation.SideLoop([start, end]), xmlElement)
		stepVector = endMinusStart / steps
		return getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement)
	if typeString == 'minimum':
		steps = math.floor(endMinusStartLengthOverStep)
		if steps < 1.0:
			return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)
		stepVector = endMinusStart / steps
		return getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement)
	print('Warning, the step type was not one of (average, maximum or minimum) in getGeometryOutput in line for:')
	print(typeString)
	print(xmlElement)
	loop.append(end)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['start', 'end', 'step'], arguments, xmlElement)
	return getGeometryOutput(xmlElement)

def getGeometryOutputByStep(end, loop, steps, stepVector, xmlElement):
	"Get line geometry output by the end, loop, steps and stepVector."
	stepsFloor = int(math.floor(abs(steps)))
	for stepIndex in xrange(1, stepsFloor):
		loop.append(loop[stepIndex - 1] + stepVector)
	loop.append(end)
	return lineation.getGeometryOutputByLoop(lineation.SideLoop(loop), xmlElement)

def processXMLElement(xmlElement):
	"Process the xml element."
	lineation.processXMLElementByGeometry(getGeometryOutput(xmlElement), xmlElement)
