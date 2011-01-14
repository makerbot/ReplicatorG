"""
Gear couple.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import extrude
from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def addHorizontallyBoundedPoint(begin, center, end, horizontalBegin, horizontalEnd, path):
	'Add point if it is within the horizontal bounds.'
	if center.real >= horizontalEnd and center.real <= horizontalBegin:
		path.append(center)
		return
	if end != None:
		if center.real > horizontalBegin and end.real <= horizontalBegin:
			centerMinusEnd = center - end
			along = (center.real - horizontalBegin) / centerMinusEnd.real
			path.append(center - along * centerMinusEnd)
			return
	if begin != None:
		if center.real < horizontalEnd and begin.real >= horizontalEnd:
			centerMinusBegin = center - begin
			along = (center.real - horizontalEnd) / centerMinusBegin.real
			path.append(center - along * centerMinusBegin)

def getAxialMargin(circleRadius, numberOfSides, polygonRadius):
	'Get axial margin.'
	return polygonRadius * math.sin(math.pi / float(numberOfSides)) - circleRadius

def getGearProfile(gearDerivation, teeth, toothProfile):
	'Get gear profile.'
	if teeth == 0:
		return getGearProfileRack(gearDerivation, toothProfile)
	return getGearProfileCylinder(teeth, toothProfile)

def getGearProfileCylinder(teeth, toothProfile):
	'Get gear profile.'
	gearProfile = []
	toothAngleRadian = 2.0 * math.pi / float(teeth)
	totalToothAngle = 0.0
	for toothIndex in xrange(teeth):
		for toothPoint in toothProfile:
			gearProfile.append(toothPoint * euclidean.getWiddershinsUnitPolar(totalToothAngle))
		totalToothAngle += toothAngleRadian
	return gearProfile

def getGearProfileRack(gearDerivation, toothProfile):
	'Get gear profile for rack.'
	rackDemilength = 0.5 * gearDerivation.rackLength
	teethRack = int(math.ceil(rackDemilength / gearDerivation.wavelength))
	gearProfile = []
	for toothIndex in xrange(-teethRack, teethRack + 1):
		translateComplex = complex(-toothIndex * gearDerivation.wavelength, 0.0)
		translatedPath = euclidean.getTranslatedComplexPath(toothProfile, translateComplex)
		gearProfile += translatedPath
	gearProfile = getHorizontallyBoundedPath(rackDemilength, -rackDemilength, gearProfile)
	firstPoint = gearProfile[0]
	lastPoint = gearProfile[-1]
	gearProfile.append(complex(lastPoint.real, lastPoint.imag - gearDerivation.rackWidth))
	gearProfile.append(complex(firstPoint.real, firstPoint.imag - gearDerivation.rackWidth))
	return gearProfile

def getGeometryOutput(xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	gearDerivation = GearDerivation()
	gearDerivation.setToXMLElement(xmlElement)
	creationFirst = gearDerivation.creationType.lower()[: 1]
	pitchRadiusSecond = gearDerivation.pitchRadius * float(gearDerivation.teethSecond) / float(gearDerivation.teethPinion)
	toothProfileFirst = getToothProfileCylinder(gearDerivation, gearDerivation.pitchRadius, gearDerivation.teethPinion)
	toothProfileSecond = getToothProfile(gearDerivation, pitchRadiusSecond, gearDerivation.teethSecond)
	gearProfileFirst = getGearProfileCylinder(gearDerivation.teethPinion, toothProfileFirst)
	gearProfileSecond = getGearProfile(gearDerivation, gearDerivation.teethSecond, toothProfileSecond)
	vector3GearProfileFirst = euclidean.getVector3Path(gearProfileFirst)
	vector3GearProfileSecond = euclidean.getVector3Path(gearProfileSecond)
	translation = Vector3()
	moveFirst = gearDerivation.moveType.lower()[: 1]
	if moveFirst != 'n':
		distance = gearDerivation.pitchRadius + pitchRadiusSecond
		if moveFirst != 'm':
			decimalPlaces = 1 - int(math.floor(math.log10(distance)))
			distance += gearDerivation.halfWavelength + gearDerivation.halfWavelength
			distance = round(1.15 * distance, decimalPlaces)
		translation = Vector3(0.0, -distance)
	if gearDerivation.thickness <=0.0:
		return getPathOutput(
			creationFirst, gearDerivation, translation, vector3GearProfileFirst, vector3GearProfileSecond, xmlElement)
	shaftRimRadius = gearDerivation.shaftRadius + gearDerivation.collarWidth
	vector3ShaftPath = getShaftPath(gearDerivation)
	pitchRadius = gearDerivation.pitchRadius
	teeth = gearDerivation.teethPinion
	twist = gearDerivation.helixThickness / gearDerivation.pitchRadius
	extrudeOutputFirst = getOutputCylinder(
		gearDerivation, pitchRadius, shaftRimRadius, teeth, twist, vector3GearProfileFirst, vector3ShaftPath, xmlElement)
	if creationFirst == 'f':
		return extrudeOutputFirst
	pitchRadius = pitchRadiusSecond
	teeth = gearDerivation.teethSecond
	extrudeOutputSecond = None
	if teeth == 0:
		extrudeOutputSecond = getOutputRack(gearDerivation, vector3GearProfileSecond, xmlElement)
	else:
		twist = -gearDerivation.helixThickness / pitchRadiusSecond
		extrudeOutputSecond = getOutputCylinder(
			gearDerivation, pitchRadius, shaftRimRadius, teeth, twist, vector3GearProfileSecond, vector3ShaftPath, xmlElement)
	if creationFirst == 's':
		return extrudeOutputSecond
	if moveFirst == 'v':
		connectionVertexes = matrix.getConnectionVertexes(extrudeOutputSecond)
		translation = Vector3(0.0, 0.0, euclidean.getTop(connectionVertexes))
		euclidean.translateVector3Path(matrix.getConnectionVertexes(extrudeOutputFirst), translation)
	else:
		euclidean.translateVector3Path(matrix.getConnectionVertexes(extrudeOutputSecond), translation)
	return {'group' : {'shapes' : [extrudeOutputFirst, extrudeOutputSecond]}}

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	evaluate.setAttributeDictionaryByArguments(['radius', 'start', 'end', 'revolutions'], arguments, xmlElement)
	return getGeometryOutput(xmlElement)

def getHalfwave(pitchRadius, teeth):
	'Get tooth halfwave.'
	return pitchRadius * math.pi / float(teeth)

def getHelixComplexPath(gearDerivation, xmlElement):
	'Set gear helix path.'
	helixTypeFirstCharacter = gearDerivation.helixType.lower()[: 1]
	if helixTypeFirstCharacter == 'b':
		return [complex(), complex(1.0, 1.0)]
	if helixTypeFirstCharacter == 'h':
		return [complex(), complex(0.5, 0.5), complex(1.0, 0.0)]
	if helixTypeFirstCharacter == 'p':
		helixComplexPath = []
		x = 0.0
		xStep = evaluate.getLayerThickness(gearDerivation.xmlElement) / gearDerivation.thickness
		justBelowOne = 1.0 - 0.5 * xStep
		while x < justBelowOne:
			distanceFromCenter = 0.5 - x
			parabolicTwist = 0.25 - distanceFromCenter * distanceFromCenter
			helixComplexPath.append(complex(x, parabolicTwist))
			x += xStep
		helixComplexPath.append(complex(1.0, 0.0))
		return helixComplexPath
	print('Warning, the helix type was not one of (basic, herringbone or parabolic) in getHelixComplexPath in gear for:')
	print(gearDerivation.helixType)
	print(gearDerivation.xmlElement)

def getHorizontallyBoundedPath(horizontalBegin, horizontalEnd, path):
	'Get horizontally bounded path.'
	horizontallyBoundedPath = []
	for pointIndex, point in enumerate(path):
		begin = None
		previousIndex = pointIndex - 1
		if previousIndex >= 0:
			begin = path[previousIndex]
		end = None
		nextIndex = pointIndex + 1
		if nextIndex < len(path):
			end = path[nextIndex]
		addHorizontallyBoundedPoint(begin, point, end, horizontalBegin, horizontalEnd, horizontallyBoundedPath)
	return horizontallyBoundedPath

def getLighteningHoles(gearDerivation, pitchRadius, shaftRimRadius, teeth):
	'Get cutout circles.'
	innerRadius = pitchRadius - gearDerivation.dedendum
	lighteningHoleOuterRadius = innerRadius - gearDerivation.rimWidth
	shaftRimRadius = max(shaftRimRadius, (lighteningHoleOuterRadius) * (0.5 - math.sqrt(0.1875)))
	lighteningHoleRadius = 0.5 * (lighteningHoleOuterRadius - shaftRimRadius)
	if lighteningHoleRadius < gearDerivation.lighteningHoleMinimumRadius:
		return []
	lighteningHoles = []
	numberOfLighteningHoles = 3
	polygonRadius = lighteningHoleOuterRadius - lighteningHoleRadius
	rimDemiwidth = 0.5 * gearDerivation.lighteningHoleMargin
	axialMargin = getAxialMargin(lighteningHoleRadius, numberOfLighteningHoles, polygonRadius)
	if axialMargin < rimDemiwidth:
		while axialMargin < rimDemiwidth:
			lighteningHoleRadius *= 0.999
			if lighteningHoleRadius < gearDerivation.lighteningHoleMinimumRadius:
				return []
			axialMargin = getAxialMargin(lighteningHoleRadius, numberOfLighteningHoles, polygonRadius)
	else:
		newNumberOfLighteningHoles = numberOfLighteningHoles
		while axialMargin > rimDemiwidth:
			numberOfLighteningHoles = newNumberOfLighteningHoles
			newNumberOfLighteningHoles += 2
			axialMargin = getAxialMargin(lighteningHoleRadius, newNumberOfLighteningHoles, polygonRadius)
	sideAngle = 2.0 * math.pi / float(numberOfLighteningHoles)
	startAngle = 0.0
	for lighteningHoleIndex in xrange(numberOfLighteningHoles):
		unitPolar = euclidean.getWiddershinsUnitPolar(startAngle)
		lighteningHole = euclidean.getComplexPolygon(unitPolar * polygonRadius, lighteningHoleRadius, -13)
		lighteningHoles.append(lighteningHole)
		startAngle += sideAngle
	return euclidean.getVector3Paths(lighteningHoles)

def getMirrorPath(path):
	"Get mirror path."
	close = 0.001 * euclidean.getPathLength(path)
	pathLimit = len(path) - 1
	for pointIndex in xrange(pathLimit, -1, -1):
		point = path[pointIndex]
		flipPoint = complex(-point.real, point.imag)
		if abs(flipPoint - path[-1]) > close:
			path.append(flipPoint)
	return path

def getOutputCylinder(gearDerivation, pitchRadius, shaftRimRadius, teeth, twist, vector3GearProfile, vector3ShaftPath, xmlElement):
	"Get extrude output for a cylinder gear."
	vector3LighteningHoles = getLighteningHoles(gearDerivation, pitchRadius, shaftRimRadius, teeth)
	extrudeDerivation = extrude.ExtrudeDerivation()
	extrudeDerivation.offsetPathDefault = [Vector3(), Vector3(0.0, 0.0, gearDerivation.thickness)]
	extrudeDerivation.setToXMLElement(xmlElement)
	negatives = []
	positives = []
	extrude.addNegativesPositives(extrudeDerivation, negatives, vector3LighteningHoles + [vector3ShaftPath], positives)
	if twist != 0.0:
		twistDegrees = math.degrees(twist)
		extrudeDerivation.twistPathDefault = []
		for complexPoint in getHelixComplexPath(gearDerivation, xmlElement):
			extrudeDerivation.twistPathDefault.append(Vector3(complexPoint.real, twistDegrees * complexPoint.imag))
		extrude.insertTwistPortions(extrudeDerivation, xmlElement)
	extrude.addNegativesPositives(extrudeDerivation, negatives, [vector3GearProfile], positives)
	return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)

def getOutputRack(gearDerivation, vector3GearProfile, xmlElement):
	"Get extrude output for a rack."
	extrudeDerivation = extrude.ExtrudeDerivation()
	helixComplexPath = getHelixComplexPath(gearDerivation, xmlElement)
	extrudeDerivation.offsetPathDefault = []
	for complexPoint in getHelixComplexPath(gearDerivation, xmlElement):
		point = Vector3(gearDerivation.helixThickness * complexPoint.imag, 0.0, gearDerivation.thickness * complexPoint.real)
		extrudeDerivation.offsetPathDefault.append(point)
	extrudeDerivation.setToXMLElement(xmlElement)
	negatives = []
	positives = []
	extrude.addNegativesPositives(extrudeDerivation, negatives, [vector3GearProfile], positives)
	return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)

def getPathOutput(creationFirst, gearDerivation, translation, vector3GearProfileFirst, vector3GearProfileSecond, xmlElement):
	"Get gear path output."
	vector3GearProfileFirst = lineation.getGeometryOutputByLoop(lineation.SideLoop(vector3GearProfileFirst), xmlElement)
	if creationFirst == 'f':
		return vector3GearProfileFirst
	vector3GearProfileSecond = lineation.getGeometryOutputByLoop(lineation.SideLoop(vector3GearProfileSecond), xmlElement)
	if creationFirst == 's':
		return vector3GearProfileSecond
	euclidean.translateVector3Path(vector3GearProfileSecond, translation)
	return [vector3GearProfileFirst, vector3GearProfileSecond]

def getShaftPath(gearDerivation):
	'Get shaft with the option of a flat on the top and/or bottom.'
	sideAngle = 2.0 * math.pi / float(gearDerivation.shaftSides)
	startAngle = 0.5 * sideAngle
	endAngle = math.pi - 0.1 * sideAngle
	shaftProfile = []
	while startAngle < endAngle:
		unitPolar = euclidean.getWiddershinsUnitPolar(startAngle)
		shaftProfile.append(unitPolar * gearDerivation.shaftRadius)
		startAngle += sideAngle
	if gearDerivation.shaftSides % 2 == 1:
		shaftProfile.append(complex(-gearDerivation.shaftRadius, 0.0))
	horizontalBegin = gearDerivation.shaftRadius - gearDerivation.shaftDepthTop
	horizontalEnd = gearDerivation.shaftDepthBottom - gearDerivation.shaftRadius
	shaftProfile = getHorizontallyBoundedPath(horizontalBegin, horizontalEnd, shaftProfile)
	for shaftPointIndex, shaftPoint in enumerate(shaftProfile):
		shaftProfile[shaftPointIndex] = complex(shaftPoint.imag, shaftPoint.real)
	return euclidean.getVector3Path(getMirrorPath(shaftProfile))

def getToothProfile(gearDerivation, pitchRadius, teeth):
	'Get profile for one tooth.'
	if teeth == 0:
		return getToothProfileRack(gearDerivation)
	return getToothProfileCylinder(gearDerivation, pitchRadius, teeth)

def getToothProfileCylinder(gearDerivation, pitchRadius, teeth):
	'Get profile for one tooth of a cylindrical gear.'
	toothProfile = getToothProfileHalfCylinder(gearDerivation, pitchRadius, teeth)
	profileFirst = toothProfile[0]
	profileSecond = toothProfile[1]
	firstMinusSecond = profileFirst - profileSecond
	remainingDedendum = abs(profileFirst) - pitchRadius + gearDerivation.dedendum
	firstMinusSecond *= remainingDedendum / abs(firstMinusSecond)
	extensionPoint = profileFirst + firstMinusSecond
	if gearDerivation.bevel <= 0.0:
		toothProfile.insert(0, extensionPoint)
		return getMirrorPath(toothProfile)
	unitPolar = euclidean.getWiddershinsUnitPolar(-2.0 / float(teeth) * math.pi)
	mirrorExtensionPoint = complex(-extensionPoint.real, extensionPoint.imag) * unitPolar
	mirrorMinusExtension = euclidean.getNormalized(mirrorExtensionPoint - extensionPoint)
	if remainingDedendum <= gearDerivation.bevel:
		toothProfile.insert(0, complex(extensionPoint.real, extensionPoint.imag) + remainingDedendum * mirrorMinusExtension)
		return getMirrorPath(toothProfile)
	firstMinusSecond *= (remainingDedendum - gearDerivation.bevel) / abs(firstMinusSecond)
	toothProfile.insert(0, profileFirst + firstMinusSecond)
	toothProfile.insert(0, complex(extensionPoint.real, extensionPoint.imag) + gearDerivation.bevel * mirrorMinusExtension)
	return getMirrorPath(toothProfile)

def getToothProfileHalfCylinder(gearDerivation, pitchRadius, teeth):
	'Get profile for half of a one tooth of a cylindrical gear.'
	toothProfile=[]
#	x = -y * tan(p) + 1
#	x*x + y*y = (2-cos(p))^2
#	y*y*t*t-2yt+1+y*y=4-4c-c*c
#	y*y*(t*t+1)-2yt=3-4c-c*c
#	y*y*(t*t+1)-2yt-3+4c-c*c=0
#	a=tt+1
#	b=-2t
#	c=c(4-c)-3
	a = gearDerivation.tanPressure * gearDerivation.tanPressure + 1.0
	b = -gearDerivation.tanPressure - gearDerivation.tanPressure
	cEnd = gearDerivation.cosPressure * (4.0 - gearDerivation.cosPressure) - 3.0
	yEnd = (-b - math.sqrt(b*b - 4 * a * cEnd)) * 0.5 / a
	yBegin = -1.02 * yEnd
	beginComplex = complex(1.0 - yBegin * gearDerivation.tanPressure, yBegin)
	endComplex = complex(1.0 - yEnd * gearDerivation.tanPressure, yEnd)
	endMinusBeginComplex = endComplex - beginComplex
	wholeAngle = -abs(endMinusBeginComplex)
	wholeAngleIncrement = wholeAngle / float(gearDerivation.profileDefinitionSurfaces)
	stringStartAngle = abs(beginComplex - complex(1.0, 0.0))
	wholeDepthIncrementComplex = endMinusBeginComplex / float(gearDerivation.profileDefinitionSurfaces)
	for profileIndex in xrange(gearDerivation.profileDefinitionSurfaces + 1):
		contactPoint = beginComplex + wholeDepthIncrementComplex * float(profileIndex)
		stringAngle = stringStartAngle + wholeAngleIncrement * float(profileIndex)
		angle = math.atan2(contactPoint.imag, contactPoint.real) - stringAngle
		angle += 0.5 * math.pi - gearDerivation.quarterWavelength / pitchRadius
		toothPoint = abs(contactPoint) * euclidean.getWiddershinsUnitPolar(angle) * pitchRadius
		toothPoint = complex(toothPoint.real * gearDerivation.xToothMultiplier, toothPoint.imag)
		toothProfile.append(toothPoint)
	return toothProfile

def getToothProfileRack(gearDerivation):
	'Get profile for one rack tooth.'
	toothQuaterwidth = gearDerivation.quarterWavelength
	sinPressure = math.sin(gearDerivation.pressureRadian)
	multiplier = 1.0 - sinPressure * sinPressure * sinPressure
	tanPressure = math.tan(gearDerivation.pressureRadian)
	rackAddendumComplex = complex(-gearDerivation.addendum * gearDerivation.tanPressure, gearDerivation.addendum)
	rackDedendumComplex = complex(gearDerivation.dedendum * gearDerivation.tanPressure, -gearDerivation.dedendum)
	toothProfile = [complex(multiplier*(toothQuaterwidth + rackDedendumComplex.real), -gearDerivation.dedendum)]
	toothProfile.append(complex(multiplier*(toothQuaterwidth + rackAddendumComplex.real), gearDerivation.addendum))
	return getMirrorPath(toothProfile) ###
#	virtualTeeth = 599
#	virtualPitch = gearDerivation.pitchRadius * float(virtualTeeth) / gearDerivation.teethPinion
#	toothProfile = getToothProfileCylinder(gearDerivation, virtualPitch, virtualTeeth)
#	for pointIndex, point in enumerate(toothProfile):
#		toothProfile[pointIndex]=complex(point.real, point.imag - virtualPitch)

def processXMLElement(xmlElement):
	"Process the xml element."
	geometryOutput = getGeometryOutput(xmlElement)
	if geometryOutput.__class__ == list:
		lineation.processXMLElementByGeometry(geometryOutput, xmlElement)
	else:
		xmlElement.getXMLProcessor().convertXMLElement(geometryOutput, xmlElement)
		xmlElement.getXMLProcessor().processXMLElement(xmlElement)


class GearDerivation:
	"Class to hold gear variables."
	def __init__(self):
		'Set defaults.'
		self.bevel = None
		self.bevelOverClearance = 0.25
		self.clearance = None
		self.clearanceOverWavelength = 0.15
		self.collarWidth = None
		self.collarWidthOverShaftRadius = 1.0
		self.creationType = 'both'
		self.helixAngle = 0.0
		self.helixType = 'basic'
		self.lighteningHoleMargin = None
		self.lighteningHoleMarginOverRimWidth = 1.0
		self.lighteningHoleMinimumRadius = 1.0
		self.moveType = 'vertical'
		self.pitchRadius = 20.0
		self.pressureAngle = 20.0
		self.profileDefinitionSurfaces = 11
		self.rackLength = None
		self.rackLengthOverRadius = math.pi + math.pi
		self.rackWidth = None
		self.rackWidthOverThickness = 1.0
		self.rimWidth = None
		self.rimWidthOverRadius = 0.2
		self.shaftRadius = None
		self.shaftRadiusOverPitchRadius = 0.2
		self.shaftSides = 4
		self.shaftDepthBottom = None
		self.shaftDepthBottomOverRadius = 0.0
		self.shaftDepthTop = None
		self.shaftDepthTopOverRadius = 0.0
		self.teethPinion = 7
		self.teethSecond = 23
		self.thickness = 10.0

	def __repr__(self):
		"Get the string representation of this GearDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.bevelOverClearance = evaluate.getEvaluatedFloatDefault(self.bevelOverClearance, 'bevelOverClearance', xmlElement)
		self.clearanceOverWavelength = evaluate.getEvaluatedFloatDefault(
			self.clearanceOverWavelength, 'clearanceOverWavelength', xmlElement)
		self.collarWidthOverShaftRadius = evaluate.getEvaluatedFloatDefault(
			self.collarWidthOverShaftRadius, 'collarWidthOverShaftRadius', xmlElement)
		self.creationType = evaluate.getEvaluatedStringDefault(self.creationType, 'creationType', xmlElement)
		self.helixAngle = evaluate.getEvaluatedFloatDefault(self.helixAngle, 'helixAngle', xmlElement)
		self.helixType = evaluate.getEvaluatedStringDefault(self.helixType, 'helixType', xmlElement)
		self.lighteningHoleMarginOverRimWidth = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMarginOverRimWidth, 'lighteningHoleMarginOverRimWidth', xmlElement)
		self.lighteningHoleMinimumRadius = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMinimumRadius, 'lighteningHoleMinimumRadius', xmlElement)
		self.moveType = evaluate.getEvaluatedStringDefault(self.moveType, 'moveType', xmlElement)
		self.pitchRadius = evaluate.getEvaluatedFloatDefault(self.pitchRadius, 'pitchRadius', xmlElement)
		self.pressureAngle = evaluate.getEvaluatedFloatDefault(self.pressureAngle, 'pressureAngle', xmlElement)
		self.profileDefinitionSurfaces = evaluate.getEvaluatedIntDefault(
			self.profileDefinitionSurfaces, 'profileDefinitionSurfaces', xmlElement)
		self.rackLengthOverRadius = evaluate.getEvaluatedFloatDefault(self.rackLengthOverRadius, 'rackLengthOverRadius', xmlElement)
		self.rackWidthOverThickness = evaluate.getEvaluatedFloatDefault(
			self.rackWidthOverThickness, 'rackWidthOverThickness', xmlElement)
		self.rimWidthOverRadius = evaluate.getEvaluatedFloatDefault(self.rimWidthOverRadius, 'rimWidthOverRadius', xmlElement)
		self.shaftDepthBottomOverRadius = evaluate.getEvaluatedFloatDefault(
			self.shaftDepthBottomOverRadius, 'shaftDepthBottomOverRadius', xmlElement)
		self.shaftDepthTopOverRadius = evaluate.getEvaluatedFloatDefault(
			self.shaftDepthTopOverRadius, 'shaftDepthOverRadius', xmlElement)
		self.shaftDepthTopOverRadius = evaluate.getEvaluatedFloatDefault(
			self.shaftDepthTopOverRadius, 'shaftDepthTopOverRadius', xmlElement)
		self.shaftRadiusOverPitchRadius = evaluate.getEvaluatedFloatDefault(
			self.shaftRadiusOverPitchRadius, 'shaftRadiusOverPitchRadius', xmlElement)
		self.shaftSides = evaluate.getEvaluatedIntDefault(self.shaftSides, 'shaftSides', xmlElement)
		self.teethPinion = evaluate.getEvaluatedIntDefault(self.teethPinion, 'teeth', xmlElement)
		self.teethPinion = evaluate.getEvaluatedIntDefault(self.teethPinion, 'teethPinion', xmlElement)
		self.teethSecond = evaluate.getEvaluatedIntDefault(self.teethSecond, 'teethSecond', xmlElement)
		self.thickness = evaluate.getEvaluatedFloatDefault(self.thickness, 'thickness', xmlElement)
		# Set absolute variables.
		self.wavelength = self.pitchRadius * 2.0 * math.pi / float(self.teethPinion)
		if self.clearance == None:
			self.clearance = self.wavelength * self.clearanceOverWavelength
		self.clearance = evaluate.getEvaluatedFloatDefault(self.clearance, 'clearance', xmlElement)
		if self.bevel == None:
			self.bevel = self.clearance * self.bevelOverClearance
		self.bevel = evaluate.getEvaluatedFloatDefault(self.bevel, 'bevel', xmlElement)
		if self.rackLength == None:
			self.rackLength = self.pitchRadius * self.rackLengthOverRadius
		self.rackLength = evaluate.getEvaluatedFloatDefault(self.rackLength, 'rackLength', xmlElement)
		if self.rackWidth == None:
			self.rackWidth = self.thickness * self.rackWidthOverThickness
		self.rackWidth = evaluate.getEvaluatedFloatDefault(self.rackWidth, 'rackWidth', xmlElement)
		if self.rimWidth == None:
			self.rimWidth = self.pitchRadius * self.rimWidthOverRadius
		self.rimWidth = evaluate.getEvaluatedFloatDefault(self.rimWidth, 'rimWidth', xmlElement)
		if self.shaftRadius == None:
			self.shaftRadius = self.pitchRadius * self.shaftRadiusOverPitchRadius
		self.shaftRadius = evaluate.getEvaluatedFloatDefault(self.shaftRadius, 'shaftRadius', xmlElement)
		if self.collarWidth == None:
			self.collarWidth = self.shaftRadius * self.collarWidthOverShaftRadius
		self.collarWidth = evaluate.getEvaluatedFloatDefault(self.collarWidth, 'collarWidth', xmlElement)
		if self.lighteningHoleMargin == None:
			self.lighteningHoleMargin = self.rimWidth * self.lighteningHoleMarginOverRimWidth
		self.lighteningHoleMargin = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMargin, 'lighteningHoleMargin', xmlElement)
		if self.shaftDepthBottom == None:
			self.shaftDepthBottom = self.shaftRadius * self.shaftDepthBottomOverRadius
		self.shaftDepthBottom = evaluate.getEvaluatedFloatDefault(self.shaftDepthBottom, 'shaftDepthBottom', xmlElement)
		if self.shaftDepthTop == None:
			self.shaftDepthTop = self.shaftRadius * self.shaftDepthTopOverRadius
		self.shaftDepthTop = evaluate.getEvaluatedFloatDefault(self.shaftDepthTop, 'shaftDepth', xmlElement)
		self.shaftDepthTop = evaluate.getEvaluatedFloatDefault(self.shaftDepthTop, 'shaftDepthTop', xmlElement)
		# Set derived values.
		self.helixRadian = math.radians(self.helixAngle)
		self.tanHelix = math.tan(self.helixRadian)
		self.helixThickness = self.tanHelix * self.thickness
		self.pressureRadian = math.radians(self.pressureAngle)
		self.cosPressure = math.cos(self.pressureRadian)
		self.sinPressure = math.sin(self.pressureRadian)
		self.tanPressure = math.tan(self.pressureRadian)
		# tooth multiplied by 0.99 is because at greater than 0.99 there is an intersection
		self.xToothMultiplier = 0.99 - 0.01 * self.tanHelix
		self.halfWavelength = 0.5 * self.wavelength
		self.quarterWavelength = 0.25 * self.wavelength
		self.pinionToothProfile = getToothProfileHalfCylinder(self, self.pitchRadius, self.teethPinion)
		self.addendum = self.pinionToothProfile[-1].imag - self.pitchRadius
		self.dedendum = abs(self.pinionToothProfile[-1]) - self.pitchRadius + self.clearance
		self.xmlElement = xmlElement
