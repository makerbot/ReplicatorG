"""
Gear couple.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities.geometry.creation import extrude
from fabmetheus_utilities.geometry.creation import lineation
from fabmetheus_utilities.geometry.creation import shaft
from fabmetheus_utilities.geometry.creation import teardrop
from fabmetheus_utilities.geometry.manipulation_evaluator import matrix
from fabmetheus_utilities.geometry.geometry_utilities import evaluate
from fabmetheus_utilities.geometry.solids import trianglemesh
from fabmetheus_utilities.vector3 import Vector3
from fabmetheus_utilities.vector3index import Vector3Index
from fabmetheus_utilities import euclidean
import math


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = "$Date: 2008/02/05 $"
__license__ = 'GPL 3.0'


def addBevelGear(derivation, extrudeDerivation, pitchRadius, positives, teeth, vector3GearProfile):
	"Get extrude output for a cylinder gear."
	totalPitchRadius = derivation.pitchRadiusGear + derivation.pitchRadius
	totalTeeth = derivation.teethPinion + derivation.teethGear
	portionDirections = extrude.getSpacedPortionDirections(extrudeDerivation.interpolationDictionary)
	loopLists = extrude.getLoopListsByPath(extrudeDerivation, None, vector3GearProfile[0], portionDirections)
	firstLoopList = loopLists[0]
	gearOverPinion = float(totalTeeth - teeth) / float(teeth)
	thirdLayerThickness = 0.33333333333 * evaluate.getLayerThickness(derivation.xmlElement)
	pitchRadian = math.atan(math.sin(derivation.operatingRadian) / (gearOverPinion + math.cos(derivation.operatingRadian)))
	coneDistance = pitchRadius / math.sin(pitchRadian)
	apex = Vector3(0.0, 0.0, math.sqrt(coneDistance * coneDistance - pitchRadius * pitchRadius))
	cosPitch = apex.z / coneDistance
	sinPitch = math.sin(pitchRadian)
	for loop in firstLoopList:
		for point in loop:
			alongWay = point.z / coneDistance
			oneMinusAlongWay = 1.0 - alongWay
			pointComplex = point.dropAxis()
			pointComplexLength = abs(pointComplex)
			deltaRadius = pointComplexLength - pitchRadius
			cosDeltaRadius = cosPitch * deltaRadius
			sinDeltaRadius = sinPitch * deltaRadius
			pointComplex *= (cosDeltaRadius + pitchRadius) / pointComplexLength
			point.x = pointComplex.real
			point.y = pointComplex.imag
			point.z += sinDeltaRadius
			point.x *= oneMinusAlongWay
			point.y *= oneMinusAlongWay
	addBottomLoop(-thirdLayerThickness, firstLoopList)
	topLoop = firstLoopList[-1]
	topAddition = []
	topZ = euclidean.getTopPath(topLoop) + thirdLayerThickness
	oldIndex = topLoop[-1].index
	for point in topLoop:
		oldIndex += 1
		topAddition.append(Vector3Index(oldIndex, 0.8 * point.x, 0.8 * point.y, topZ))
	firstLoopList.append(topAddition)
	translation = Vector3(0.0, 0.0, -euclidean.getBottomPaths(firstLoopList))
	euclidean.translateVector3Paths(firstLoopList, translation)
	geometryOutput = trianglemesh.getPillarsOutput(loopLists)
	positives.append(geometryOutput)

def addBottomLoop(deltaZ, loops):
	"Add bottom loop to loops."
	bottomLoop = loops[0]
	bottomAddition = []
	bottomZ = euclidean.getBottomPath(bottomLoop) + deltaZ
	for point in bottomLoop:
		bottomAddition.append(Vector3Index(len(bottomAddition), point.x, point.y, bottomZ))
	loops.insert(0, bottomAddition)
	numberOfVertexes = 0
	for loop in loops:
		for point in loop:
			point.index = numberOfVertexes
			numberOfVertexes += 1

def addCollarShaft(collarThickness, derivation, negatives, positives, xmlElement):
	'Add collar.'
	if collarThickness <= 0.0:
		addShaft(derivation, negatives, positives)
		return
	connectionEnd = Vector3(0.0, 0.0, derivation.pinionThickness + collarThickness)
	collarDerivation = extrude.ExtrudeDerivation()
	collarDerivation.offsetPathDefault = [Vector3(0.0, 0.0, derivation.pinionThickness), connectionEnd]
	addCollarShaftSetDerivation(collarDerivation, collarThickness, derivation, negatives, positives, xmlElement)

def addCollarShaftSetDerivation(collarDerivation, collarThickness, derivation, negatives, positives, xmlElement):
	'Add collar and shaft.'
	collarDerivation.setToXMLElement(derivation.copyShallow)
	collarSides = evaluate.getSidesMinimumThreeBasedOnPrecision(derivation.shaftRimRadius, xmlElement)
	collarProfile = euclidean.getComplexPolygon(complex(), derivation.shaftRimRadius, collarSides)
	vector3CollarProfile = euclidean.getVector3Path(collarProfile)
	extrude.addNegativesPositives(collarDerivation, negatives, [vector3CollarProfile], positives)
	addShaft(derivation, negatives, positives)
	drillZ = derivation.pinionThickness + 0.5 * collarThickness
	drillEnd = Vector3(0.0, derivation.shaftRimRadius, drillZ)
	drillStart = Vector3(0.0, 0.0, drillZ)
	teardrop.addNegativesByRadius(drillEnd, negatives, derivation.keywayRadius, drillStart, xmlElement)

def addLighteningHoles(derivation, gearHolePaths, negatives, pitchRadius, positives):
	"Add lightening holes."
	extrudeDerivation = extrude.ExtrudeDerivation()
	positiveVertexes = matrix.getVertexes(positives)
	bottomPath = euclidean.getTopPath(positiveVertexes)
	topPath = euclidean.getBottomPath(positiveVertexes)
	extrudeDerivation.offsetPathDefault = [Vector3(0.0, 0.0, bottomPath), Vector3(0.0, 0.0, topPath)]
	extrudeDerivation.setToXMLElement(derivation.copyShallow)
	vector3LighteningHoles = getLighteningHoles(derivation, gearHolePaths, pitchRadius)
	extrude.addNegativesPositives(extrudeDerivation, negatives, vector3LighteningHoles, positives)

def addRackHole(derivation, vector3RackProfiles, x, xmlElement):
	"Add rack hole to vector3RackProfiles."
	rackHole = euclidean.getComplexPolygon(complex(x, -derivation.rackHoleBelow), derivation.rackHoleRadius, -13)
	vector3RackProfiles.append(euclidean.getVector3Path(rackHole))

def addRackHoles(derivation, vector3RackProfiles, xmlElement):
	"Add rack holes to vector3RackProfiles."
	if len(derivation.gearHolePaths) > 0:
		vector3RackProfiles += derivation.gearHolePaths
		return
	if derivation.rackHoleRadius <= 0.0:
		return
	addRackHole(derivation, vector3RackProfiles, 0.0, xmlElement)
	rackHoleMargin = derivation.rackHoleRadius + derivation.rackHoleRadius
	rackHoleSteps = int(math.ceil((derivation.rackDemilength - rackHoleMargin) / derivation.rackHoleStep))
	for rackHoleIndex in xrange(1, rackHoleSteps):
		x = float(rackHoleIndex) * derivation.rackHoleStep
		addRackHole(derivation, vector3RackProfiles, -x, xmlElement)
		addRackHole(derivation, vector3RackProfiles, x, xmlElement)

def addShaft(derivation, negatives, positives):
	"Add shaft."
	if len(derivation.shaftPath) < 3:
		return
	extrudeDerivation = extrude.ExtrudeDerivation()
	positiveVertexes = matrix.getVertexes(positives)
	bottomPath = euclidean.getTopPath(positiveVertexes)
	topPath = euclidean.getBottomPath(positiveVertexes)
	extrudeDerivation.offsetPathDefault = [Vector3(0.0, 0.0, bottomPath), Vector3(0.0, 0.0, topPath)]
	extrudeDerivation.setToXMLElement(derivation.copyShallow)
	extrude.addNegativesPositives(extrudeDerivation, negatives, [derivation.shaftPath], positives)

def getAxialMargin(circleRadius, numberOfSides, polygonRadius):
	'Get axial margin.'
	return polygonRadius * math.sin(math.pi / float(numberOfSides)) - circleRadius

def getBevelPath(begin, bevel, center, end):
	'Get bevel path.'
	centerMinusBegin = center - begin
	centerMinusBeginLength = abs(centerMinusBegin)
	endMinusCenter = end - center
	endMinusCenterLength = abs(endMinusCenter)
	endMinusCenter /= endMinusCenterLength
	maximumExtensionLength = 0.333333333 * endMinusCenterLength
	if centerMinusBeginLength <= bevel * 1.5:
		extensionLength = min(maximumExtensionLength, centerMinusBeginLength)
		return [complex(center.real, center.imag) + extensionLength * endMinusCenter]
	centerMinusBegin *= (centerMinusBeginLength - bevel) / centerMinusBeginLength
	extensionLength = min(maximumExtensionLength, bevel)
	bevelPath = [complex(center.real, center.imag) + extensionLength * endMinusCenter]
	bevelPath.append(begin + centerMinusBegin)
	return bevelPath

def getGearPaths(derivation, pitchRadius, teeth, toothProfile):
	'Get gear paths.'
	if teeth < 0:
		return getGearProfileAnnulus(derivation, pitchRadius, teeth, toothProfile)
	if teeth == 0:
		return [getGearProfileRack(derivation, toothProfile)]
	return [getGearProfileCylinder(teeth, toothProfile)]

def getGearProfileAnnulus(derivation, pitchRadius, teeth, toothProfile):
	'Get gear profile for an annulus gear.'
	gearProfileCylinder = getGearProfileCylinder(teeth, toothProfile)
	annulusRadius = derivation.dedendum + derivation.rimWidth - pitchRadius
	return [euclidean.getComplexPolygon(complex(), annulusRadius, -teeth, 0.5 * math.pi), gearProfileCylinder]

def getGearProfileCylinder(teeth, toothProfile):
	'Get gear profile for a cylinder gear.'
	gearProfile = []
	toothAngleRadian = 2.0 * math.pi / float(teeth)
	totalToothAngle = 0.0
	for toothIndex in xrange(abs(teeth)):
		for toothPoint in toothProfile:
			gearProfile.append(toothPoint * euclidean.getWiddershinsUnitPolar(totalToothAngle))
		totalToothAngle += toothAngleRadian
	return gearProfile

def getGearProfileRack(derivation, toothProfile):
	'Get gear profile for rack.'
	derivation.extraRackDemilength = 0.0
	for complexPoint in derivation.helixPath:
		derivation.extraRackDemilength = max(abs(derivation.helixThickness * complexPoint.imag), derivation.extraRackDemilength)
	rackDemilengthPlus = derivation.rackDemilength
	if derivation.pinionThickness > 0.0:
		derivation.extraRackDemilength *= 1.1
		rackDemilengthPlus += derivation.extraRackDemilength
	teethRack = int(math.ceil(rackDemilengthPlus / derivation.wavelength))
	gearProfile = []
	for toothIndex in xrange(-teethRack, teethRack + 1):
		translateComplex = complex(-toothIndex * derivation.wavelength, 0.0)
		translatedPath = euclidean.getTranslatedComplexPath(toothProfile, translateComplex)
		gearProfile += translatedPath
	gearProfile = euclidean.getHorizontallyBoundedPath(rackDemilengthPlus, -rackDemilengthPlus, gearProfile)
	firstPoint = gearProfile[0]
	lastPoint = gearProfile[-1]
	rackWidth = derivation.rackWidth
	minimumRackWidth = 1.1 * derivation.dedendum
	if rackWidth < minimumRackWidth:
		rackWidth = minimumRackWidth
		print('Warning, rackWidth is too small in getGearProfileRack in gear.')
		print('RackWidth will be set to a bit more than the dedendum.')
	gearProfile += [complex(lastPoint.real, -rackWidth),complex(firstPoint.real, -rackWidth)]
	return gearProfile

def getGeometryOutput(derivation, xmlElement):
	"Get vector3 vertexes from attribute dictionary."
	if derivation == None:
		derivation = GearDerivation()
		derivation.setToXMLElement(xmlElement)
	creationFirst = derivation.creationType.lower()[: 1]
	toothProfileGear = getToothProfile(derivation, derivation.pitchRadiusGear, derivation.teethGear)
	gearProfileFirst = getGearProfileCylinder(derivation.teethPinion, derivation.pinionToothProfile)
	gearPaths = getGearPaths(derivation, derivation.pitchRadiusGear, derivation.teethGear, toothProfileGear)
	vector3GearProfileFirst = euclidean.getVector3Path(gearProfileFirst)
	vector3GearPaths = euclidean.getVector3Paths(gearPaths)
	translation = Vector3()
	moveFirst = derivation.moveType.lower()[: 1]
	if moveFirst != 'n':
		distance = derivation.pitchRadius + derivation.pitchRadiusGear
		if moveFirst != 'm':
			decimalPlaces = 1 - int(math.floor(math.log10(derivation.pitchRadius + abs(derivation.pitchRadiusGear))))
			distance += derivation.halfWavelength + derivation.halfWavelength
			distance = round(1.15 * distance, decimalPlaces)
		translation = Vector3(0.0, -distance)
	if derivation.pinionThickness <=0.0:
		return getPathOutput(
			creationFirst, derivation, translation, vector3GearProfileFirst, vector3GearPaths, xmlElement)
	pitchRadius = derivation.pitchRadius
	teeth = derivation.teethPinion
	twist = derivation.helixThickness / derivation.pitchRadius
	extrudeOutputFirst = getOutputCylinder(
		derivation.pinionCollarThickness, derivation, None, pitchRadius, teeth, twist, [vector3GearProfileFirst], xmlElement)
	if creationFirst == 'f':
		return extrudeOutputFirst
	teeth = derivation.teethGear
	extrudeOutputSecond = None
	if teeth == 0:
		extrudeOutputSecond = getOutputRack(derivation, vector3GearPaths[0], xmlElement)
	else:
		twist = -derivation.helixThickness / derivation.pitchRadiusGear
		extrudeOutputSecond = getOutputCylinder(
			derivation.gearCollarThickness,
			derivation,
			derivation.gearHolePaths,
			derivation.pitchRadiusGear,
			teeth,
			twist,
			vector3GearPaths,
			xmlElement)
	if creationFirst == 's':
		return extrudeOutputSecond
	gearVertexes = matrix.getConnectionVertexes(extrudeOutputSecond)
	if moveFirst == 'v':
		translation = Vector3(0.0, 0.0, euclidean.getTopPath(gearVertexes))
		euclidean.translateVector3Path(matrix.getConnectionVertexes(extrudeOutputFirst), translation)
	else:
		euclidean.translateVector3Path(gearVertexes, translation)
	return {'group' : {'shapes' : [extrudeOutputFirst, extrudeOutputSecond]}}

def getGeometryOutputByArguments(arguments, xmlElement):
	"Get vector3 vertexes from attribute dictionary by arguments."
	return getGeometryOutput(None, xmlElement)

def getHalfwave(pitchRadius, teeth):
	'Get tooth halfwave.'
	return pitchRadius * math.pi / float(teeth)

def getHelixComplexPath(derivation, xmlElement):
	'Set gear helix path.'
	helixTypeFirstCharacter = derivation.helixType.lower()[: 1]
	if helixTypeFirstCharacter == 'b':
		return [complex(), complex(1.0, 1.0)]
	if helixTypeFirstCharacter == 'h':
		return [complex(), complex(0.5, 0.5), complex(1.0, 0.0)]
	if helixTypeFirstCharacter == 'p':
		helixComplexPath = []
		x = 0.0
		xStep = evaluate.getLayerThickness(xmlElement) / derivation.pinionThickness
		justBelowOne = 1.0 - 0.5 * xStep
		while x < justBelowOne:
			distanceFromCenter = 0.5 - x
			parabolicTwist = 0.25 - distanceFromCenter * distanceFromCenter
			helixComplexPath.append(complex(x, parabolicTwist))
			x += xStep
		helixComplexPath.append(complex(1.0, 0.0))
		return helixComplexPath
	print('Warning, the helix type was not one of (basic, herringbone or parabolic) in getHelixComplexPath in gear for:')
	print(derivation.helixType)
	print(derivation.xmlElement)

def getLiftedOutput(derivation, geometryOutput, xmlElement):
	"Get extrude output for a rack."
	if derivation.moveType.lower()[: 1] == 'm':
		return geometryOutput
	geometryOutputVertexes = matrix.getConnectionVertexes(geometryOutput)
	translation = Vector3(0.0, 0.0, -euclidean.getBottomPath(geometryOutputVertexes))
	euclidean.translateVector3Path(geometryOutputVertexes, translation)
	return geometryOutput

def getLighteningHoles(derivation, gearHolePaths, pitchRadius):
	'Get cutout circles.'
	if gearHolePaths != None:
		if len(gearHolePaths) > 0:
			return gearHolePaths
	innerRadius = abs(pitchRadius) - derivation.dedendum
	lighteningHoleOuterRadius = innerRadius - derivation.rimWidth
	shaftRimRadius = max(derivation.shaftRimRadius, (lighteningHoleOuterRadius) * (0.5 - math.sqrt(0.1875)))
	lighteningHoleRadius = 0.5 * (lighteningHoleOuterRadius - derivation.shaftRimRadius)
	if lighteningHoleRadius < derivation.lighteningHoleMinimumRadius:
		return []
	lighteningHoles = []
	numberOfLighteningHoles = 3
	polygonRadius = lighteningHoleOuterRadius - lighteningHoleRadius
	rimDemiwidth = 0.5 * derivation.lighteningHoleMargin
	axialMargin = getAxialMargin(lighteningHoleRadius, numberOfLighteningHoles, polygonRadius)
	if axialMargin < rimDemiwidth:
		while axialMargin < rimDemiwidth:
			lighteningHoleRadius *= 0.999
			if lighteningHoleRadius < derivation.lighteningHoleMinimumRadius:
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

def getOutputCylinder(
		collarThickness, derivation, gearHolePaths, pitchRadius, teeth, twist, vector3GearProfile, xmlElement):
	"Get extrude output for a cylinder gear."
	extrudeDerivation = extrude.ExtrudeDerivation()
	extrudeDerivation.offsetPathDefault = [Vector3(), Vector3(0.0, 0.0, derivation.pinionThickness)]
	extrudeDerivation.setToXMLElement(derivation.copyShallow)
	negatives = []
	positives = []
	if twist != 0.0:
		twistDegrees = math.degrees(twist)
		extrudeDerivation.twistPathDefault = []
		for complexPoint in derivation.helixPath:
			extrudeDerivation.twistPathDefault.append(Vector3(complexPoint.real, twistDegrees * complexPoint.imag))
		extrude.insertTwistPortions(extrudeDerivation, xmlElement)
	if derivation.operatingAngle != 180.0:
		addBevelGear(derivation, extrudeDerivation, pitchRadius, positives, teeth, vector3GearProfile)
		addCollarShaft(collarThickness, derivation, negatives, positives, xmlElement)
		return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)
	if pitchRadius > 0:
		extrude.addNegativesPositives(extrudeDerivation, negatives, vector3GearProfile, positives)
		addLighteningHoles(derivation, gearHolePaths, negatives, pitchRadius, positives)
		addCollarShaft(collarThickness, derivation, negatives, positives, xmlElement)
		return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)
	if derivation.plateThickness <= 0.0:
		extrude.addNegativesPositives(extrudeDerivation, negatives, vector3GearProfile, positives)
		return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)
	portionDirections = extrude.getSpacedPortionDirections(extrudeDerivation.interpolationDictionary)
	outerGearProfile = vector3GearProfile[0]
	outerLoopLists = extrude.getLoopListsByPath(extrudeDerivation, None, outerGearProfile, portionDirections)
	addBottomLoop(-derivation.plateClearance, outerLoopLists[0])
	geometryOutput = trianglemesh.getPillarsOutput(outerLoopLists)
	positives.append(geometryOutput)
	innerLoopLists = extrude.getLoopListsByPath(extrudeDerivation, None, vector3GearProfile[1], portionDirections)
	addBottomLoop(-derivation.plateClearance, innerLoopLists[0])
	geometryOutput = trianglemesh.getPillarsOutput(innerLoopLists)
	negatives.append(geometryOutput)
	connectionStart = Vector3(0.0, 0.0, -derivation.plateThickness)
	plateDerivation = extrude.ExtrudeDerivation()
	plateDerivation.offsetPathDefault = [connectionStart, Vector3(0.0, 0.0, -derivation.plateClearance)]
	plateDerivation.setToXMLElement(derivation.copyShallow)
	extrude.addNegativesPositives(plateDerivation, negatives, [outerGearProfile], positives)
	vector3LighteningHoles = getLighteningHoles(derivation, gearHolePaths, pitchRadius)
	extrude.addNegativesPositives(plateDerivation, negatives, vector3LighteningHoles, positives)
	addShaft(derivation, negatives, positives)
	connectionEnd = Vector3(0.0, 0.0, derivation.pinionThickness)
	positiveOutput = trianglemesh.getUnifiedOutput(positives)
	annulusPlateOutput = {'difference' : {'shapes' : [positiveOutput] + negatives}}
	if collarThickness <= 0.0:
		outputCylinder = extrude.getGeometryOutputByConnection(connectionEnd, connectionStart, annulusPlateOutput, xmlElement)
		return getLiftedOutput(derivation, outputCylinder, xmlElement)
	negatives = []
	positives = []
	connectionEnd = Vector3(0.0, 0.0, derivation.pinionThickness + collarThickness)
	collarDerivation = extrude.ExtrudeDerivation()
	collarDerivation.offsetPathDefault = [Vector3(0.0, 0.0, -derivation.plateClearance), connectionEnd]
	addCollarShaftSetDerivation(collarDerivation, collarThickness, derivation, negatives, positives, xmlElement)
	collarOutput = {'difference' : {'shapes' : positives + negatives}}
	cylinderOutput = {'union' : {'shapes' : [annulusPlateOutput, collarOutput]}}
	outputCylinder = extrude.getGeometryOutputByConnection(connectionEnd, connectionStart, cylinderOutput, xmlElement)
	return getLiftedOutput(derivation, outputCylinder, xmlElement)

def getOutputRack(derivation, vector3GearProfile, xmlElement):
	"Get extrude output for a rack."
	extrudeDerivation = extrude.ExtrudeDerivation()
	extrudeDerivation.offsetPathDefault = []
	for complexPoint in derivation.helixPath:
		point = Vector3(derivation.helixThickness * complexPoint.imag, 0.0, derivation.pinionThickness * complexPoint.real)
		extrudeDerivation.offsetPathDefault.append(point)
	extrudeDerivation.setToXMLElement(derivation.copyShallow)
	negatives = []
	positives = []
	vector3RackProfiles = [vector3GearProfile]
	if derivation.extraRackDemilength > 0.0:
		yMaximum = -912345678.0
		yMinimum = 912345678.0
		for point in vector3GearProfile:
			yMaximum = max(point.y, yMaximum)
			yMinimum = min(point.y, yMinimum)
		muchLessThanWidth = 0.01 * derivation.rackWidth
		yMaximum += muchLessThanWidth
		yMinimum -= muchLessThanWidth
		extraRackLength = derivation.extraRackDemilength + derivation.extraRackDemilength
		rackDemilengthPlus = derivation.rackDemilength + extraRackLength
		leftNegative = [
			Vector3(-derivation.rackDemilength, yMaximum),
			Vector3(-derivation.rackDemilength, yMinimum),
			Vector3(-rackDemilengthPlus, yMinimum),
			Vector3(-rackDemilengthPlus, yMaximum)]
		vector3RackProfiles.append(leftNegative)
		rightNegative = [
			Vector3(rackDemilengthPlus, yMaximum),
			Vector3(rackDemilengthPlus, yMinimum),
			Vector3(derivation.rackDemilength, yMinimum),
			Vector3(derivation.rackDemilength, yMaximum)]
		vector3RackProfiles.append(rightNegative)
	addRackHoles(derivation, vector3RackProfiles, xmlElement)
	extrude.addNegativesPositives(extrudeDerivation, negatives, vector3RackProfiles, positives)
	return extrude.getGeometryOutputByNegativesPositives(extrudeDerivation, negatives, positives, xmlElement)

def getPathOutput(creationFirst, derivation, translation, vector3GearProfileFirst, vector3GearPaths, xmlElement):
	"Get gear path output."
	vector3GearProfileFirst = lineation.getPackedGeometryOutputByLoop(lineation.SideLoop(vector3GearProfileFirst), xmlElement)
	if creationFirst == 'f':
		return vector3GearProfileFirst
	packedGearGeometry = []
	for vector3GearPath in vector3GearPaths:
		packedGearGeometry += lineation.getPackedGeometryOutputByLoop(lineation.SideLoop(vector3GearPath), xmlElement)
	if creationFirst == 's':
		return packedGearGeometry
	euclidean.translateVector3Paths(packedGearGeometry, translation)
	return vector3GearProfileFirst + packedGearGeometry

def getToothProfile(derivation, pitchRadius, teeth):
	'Get profile for one tooth.'
	if teeth < 0:
		return getToothProfileAnnulus(derivation, pitchRadius, teeth)
	if teeth == 0:
		return getToothProfileRack(derivation)
	return getToothProfileCylinder(derivation, pitchRadius, teeth)

def getToothProfileAnnulus(derivation, pitchRadius, teeth):
	'Get profile for one tooth of an annulus.'
	toothProfileHalf = []
	toothProfileHalfCylinder = getToothProfileHalfCylinder(derivation, pitchRadius)
	pitchRadius = -pitchRadius
	innerRadius = pitchRadius - derivation.addendum
	# tooth is multiplied by 1.02 because at around 1.01 for a 7/-23/20.0 test case, there is intersection since the paths are bending together
	for point in getWidthMultipliedPath(toothProfileHalfCylinder, 1.02 / derivation.toothWidthMultiplier):
		if abs(point) >= innerRadius:
			toothProfileHalf.append(point)
	profileFirst = toothProfileHalf[0]
	profileSecond = toothProfileHalf[1]
	firstMinusSecond = profileFirst - profileSecond
	remainingAddendum = abs(profileFirst) - innerRadius
	firstMinusSecond *= remainingAddendum / abs(firstMinusSecond)
	extensionPoint = profileFirst + firstMinusSecond
	if derivation.tipBevel > 0.0:
		unitPolar = euclidean.getWiddershinsUnitPolar(2.0 / float(teeth) * math.pi)
		mirrorPoint = complex(-extensionPoint.real, extensionPoint.imag) * unitPolar
		bevelPath = getBevelPath(profileFirst, derivation.tipBevel, extensionPoint, mirrorPoint)
		toothProfileHalf = bevelPath + toothProfileHalf
	else:
		toothProfileHalf.insert(0, extensionPoint)
	profileLast = toothProfileHalf[-1]
	profilePenultimate = toothProfileHalf[-2]
	lastMinusPenultimate = profileLast - profilePenultimate
	remainingDedendum = pitchRadius - abs(profileLast) + derivation.dedendum
	lastMinusPenultimate *= remainingDedendum / abs(lastMinusPenultimate)
	extensionPoint = profileLast + lastMinusPenultimate
	if derivation.rootBevel > 0.0:
		mirrorPoint = complex(-extensionPoint.real, extensionPoint.imag)
		bevelPath = getBevelPath(profileLast, derivation.rootBevel, extensionPoint, mirrorPoint)
		bevelPath.reverse()
		toothProfileHalf += bevelPath
	else:
		toothProfileHalf.append(extensionPoint)
	toothProfileAnnulus = euclidean.getMirrorPath(toothProfileHalf)
	toothProfileAnnulus.reverse()
	return toothProfileAnnulus

def getToothProfileCylinder(derivation, pitchRadius, teeth):
	'Get profile for one tooth of a cylindrical gear.'
	toothProfileHalfCylinder = getToothProfileHalfCylinder(derivation, pitchRadius)
	toothProfileHalfCylinder = getWidthMultipliedPath(toothProfileHalfCylinder, derivation.toothWidthMultiplier)
	toothProfileHalf = []
	innerRadius = pitchRadius - derivation.dedendum
	for point in toothProfileHalfCylinder:
		if abs(point) >= innerRadius:
			toothProfileHalf.append(point)
	return getToothProfileCylinderByProfile(derivation, pitchRadius, teeth, toothProfileHalf)

def getToothProfileCylinderByProfile(derivation, pitchRadius, teeth, toothProfileHalf):
	'Get profile for one tooth of a cylindrical gear.'
	profileFirst = toothProfileHalf[0]
	profileSecond = toothProfileHalf[1]
	firstMinusSecond = profileFirst - profileSecond
	remainingDedendum = abs(profileFirst) - pitchRadius + derivation.dedendum
	firstMinusSecond *= remainingDedendum / abs(firstMinusSecond)
	extensionPoint = profileFirst + firstMinusSecond
	if derivation.rootBevel > 0.0:
		unitPolar = euclidean.getWiddershinsUnitPolar(-2.0 / float(teeth) * math.pi)
		mirrorPoint = complex(-extensionPoint.real, extensionPoint.imag) * unitPolar
		bevelPath = getBevelPath(profileFirst, derivation.rootBevel, extensionPoint, mirrorPoint)
		toothProfileHalf = bevelPath + toothProfileHalf
	else:
		toothProfileHalf.insert(0, extensionPoint)
	if derivation.tipBevel > 0.0:
		profileLast = toothProfileHalf[-1]
		profilePenultimate = toothProfileHalf[-2]
		mirrorPoint = complex(-profileLast.real, profileLast.imag)
		bevelPath = getBevelPath(profilePenultimate, derivation.tipBevel, profileLast, mirrorPoint)
		bevelPath.reverse()
		toothProfileHalf = toothProfileHalf[: -1] + bevelPath
	return euclidean.getMirrorPath(toothProfileHalf)

def getToothProfileHalfCylinder(derivation, pitchRadius):
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
	a = derivation.tanPressure * derivation.tanPressure + 1.0
	b = -derivation.tanPressure - derivation.tanPressure
	cEnd = derivation.cosPressure * (4.0 - derivation.cosPressure) - 3.0
	yEnd = (-b - math.sqrt(b*b - 4 * a * cEnd)) * 0.5 / a
	yEnd *= derivation.pitchRadius / abs(pitchRadius)
	yEnd -= derivation.clearance / abs(pitchRadius)
	# to prevent intersections, yBegin is moved towards the base circle, giving a thinner tooth
	yBegin = -yEnd
	if pitchRadius > 0.0:
		yBegin = 0.5 * derivation.sinPressure + 0.5 * yBegin
	beginComplex = complex(1.0 - yBegin * derivation.tanPressure, yBegin)
	endComplex = complex(1.0 - yEnd * derivation.tanPressure, yEnd)
	endMinusBeginComplex = endComplex - beginComplex
	wholeAngle = -abs(endMinusBeginComplex) / derivation.cosPressure
	wholeAngleIncrement = wholeAngle / float(derivation.profileSurfaces)
	stringStartAngle = abs(beginComplex - complex(1.0, 0.0)) / derivation.cosPressure
	wholeDepthIncrementComplex = endMinusBeginComplex / float(derivation.profileSurfaces)
	for profileIndex in xrange(derivation.profileSurfaces + 1):
		contactPoint = beginComplex + wholeDepthIncrementComplex * float(profileIndex)
		stringAngle = stringStartAngle + wholeAngleIncrement * float(profileIndex)
		angle = math.atan2(contactPoint.imag, contactPoint.real) - stringAngle
		angle += 0.5 * math.pi - derivation.quarterWavelength / abs(pitchRadius)
		toothPoint = abs(contactPoint) * euclidean.getWiddershinsUnitPolar(angle) * abs(pitchRadius)
		toothProfile.append(toothPoint)
	return toothProfile

def getToothProfileRack(derivation):
	'Get profile for one rack tooth.'
	addendumSide = derivation.quarterWavelength - derivation.addendum * derivation.tanPressure
	addendumComplex = complex(addendumSide, derivation.addendum)
	dedendumSide = derivation.quarterWavelength + derivation.dedendum * derivation.tanPressure
	dedendumComplex = complex(dedendumSide, -derivation.dedendum)
	toothProfile = [dedendumComplex]
	if derivation.rootBevel > 0.0:
		mirrorPoint = complex(derivation.wavelength - dedendumSide, -derivation.dedendum)
		toothProfile = getBevelPath(addendumComplex, derivation.rootBevel, dedendumComplex, mirrorPoint)
	if derivation.tipBevel > 0.0:
		mirrorPoint = complex(-addendumComplex.real, addendumComplex.imag)
		bevelPath = getBevelPath(dedendumComplex, derivation.tipBevel, addendumComplex, mirrorPoint)
		bevelPath.reverse()
		toothProfile += bevelPath
	else:
		toothProfile.append(addendumComplex)
	return euclidean.getMirrorPath(getWidthMultipliedPath(toothProfile, derivation.toothWidthMultiplier))

def getWidthMultipliedPath(path, widthMultiplier):
	"Get width multiplied path."
	for pointIndex, point in enumerate(path):
		path[pointIndex] = complex(point.real * widthMultiplier, point.imag)
	return path

def processXMLElement(xmlElement):
	"Process the xml element."
	geometryOutput = getGeometryOutput(None, xmlElement)
	if geometryOutput.__class__ == list:
		lineation.processXMLElementByGeometry(geometryOutput, xmlElement)
	else:
		xmlElement.getXMLProcessor().convertXMLElement(geometryOutput, xmlElement)
		xmlElement.getXMLProcessor().processXMLElement(xmlElement)


class GearDerivation:
	"Class to hold gear variables."
	def __init__(self):
		'Set defaults.'
		self.clearance = None
		self.clearanceOverWavelength = 0.1
		self.collarWidth = None
		self.collarWidthOverShaftRadius = 1.0
		self.copyShallow = None
		self.creationType = 'both'
		self.gearCollarThickness = None
		self.gearCollarThicknessOverThickness = 0.0
		self.gearHolePaths = None
		self.helixAngle = 0.0
		self.helixPath = None
		self.helixType = 'basic'
		self.keywayRadius = None
		self.keywayRadiusOverRadius = 0.5
		self.lighteningHoleMargin = None
		self.lighteningHoleMarginOverRimWidth = 1.0
		self.lighteningHoleMinimumRadius = 1.0
		self.moveType = 'separate'
		self.operatingAngle = 180.0
		self.pinionCollarThickness = None
		self.pinionCollarThicknessOverThickness = 0.0
		self.plateClearance = None
		self.plateClearanceOverThickness = 0.2
		self.plateThickness = None
		self.plateThicknessOverThickness = 0.5
		self.pinionThickness = 10.0
		self.pitchRadius = 20.0
		self.pressureAngle = 20.0
		self.profileSurfaces = 11
		self.rackHoleBelow = None
		self.rackHoleBelowOverWidth = 0.6
		self.rackHoleRadius = None
		self.rackHoleRadiusOverWidth = 0.0
		self.rackHoleStep = None
		self.rackHoleStepOverWidth = 1.0
		self.rackLength = None
		self.rackLengthOverRadius = math.pi + math.pi
		self.rackWidth = None
		self.rackWidthOverThickness = 1.0
		self.rimWidth = None
		self.rimWidthOverRadius = 0.2
		self.rootBevel = None
		self.rootBevelOverClearance = 0.5
		self.shaftDepthBottom = None
		self.shaftDepthBottomOverRadius = 0.0
		self.shaftDepthTop = None
		self.shaftDepthTopOverRadius = 0.0
		self.shaftPath = None
		self.shaftRadius = None
		self.shaftRadiusOverPitchRadius = 0.0
		self.shaftSides = 4
		self.teethPinion = 7
		self.teethGear = 17
		self.tipBevel = None
		self.tipBevelOverClearance = 0.1
		# tooth multiplied by 0.99999 to avoid an intersection
		self.toothWidthMultiplier = 0.99999
		self.xmlElement = None

	def __repr__(self):
		"Get the string representation of this GearDerivation."
		return str(self.__dict__)

	def setToXMLElement(self, xmlElement):
		"Set to the xmlElement."
		self.clearanceOverWavelength = evaluate.getEvaluatedFloatDefault(
			self.clearanceOverWavelength, 'clearanceOverWavelength', xmlElement)
		self.collarWidthOverShaftRadius = evaluate.getEvaluatedFloatDefault(
			self.collarWidthOverShaftRadius, 'collarWidthOverShaftRadius', xmlElement)
		self.copyShallow = xmlElement.getCopyShallow()
		self.creationType = evaluate.getEvaluatedStringDefault(self.creationType, 'creationType', xmlElement)
		self.gearCollarThicknessOverThickness = evaluate.getEvaluatedFloatDefault(
			self.gearCollarThicknessOverThickness, 'gearCollarThicknessOverThickness', xmlElement)
		self.helixAngle = evaluate.getEvaluatedFloatDefault(self.helixAngle, 'helixAngle', xmlElement)
		self.helixType = evaluate.getEvaluatedStringDefault(self.helixType, 'helixType', xmlElement)
		self.keywayRadiusOverRadius = evaluate.getEvaluatedFloatDefault(
			self.keywayRadiusOverRadius, 'keywayRadiusOverRadius', xmlElement)
		self.lighteningHoleMarginOverRimWidth = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMarginOverRimWidth, 'lighteningHoleMarginOverRimWidth', xmlElement)
		self.lighteningHoleMinimumRadius = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMinimumRadius, 'lighteningHoleMinimumRadius', xmlElement)
		self.moveType = evaluate.getEvaluatedStringDefault(self.moveType, 'moveType', xmlElement)
		self.operatingAngle = evaluate.getEvaluatedFloatDefault(self.operatingAngle, 'operatingAngle', xmlElement)
		self.pinionCollarThicknessOverThickness = evaluate.getEvaluatedFloatDefault(
			self.pinionCollarThicknessOverThickness, 'pinionCollarThicknessOverThickness', xmlElement)
		self.pinionThickness = evaluate.getEvaluatedFloatDefault(self.pinionThickness, 'pinionThickness', xmlElement)
		self.pinionThickness = evaluate.getEvaluatedFloatDefault(self.pinionThickness, 'thickness', xmlElement)
		self.pitchRadius = evaluate.getEvaluatedFloatDefault(self.pitchRadius, 'pitchRadius', xmlElement)
		self.plateClearanceOverThickness = evaluate.getEvaluatedFloatDefault(
			self.plateClearanceOverThickness, 'plateClearanceOverThickness', xmlElement)
		self.plateThicknessOverThickness = evaluate.getEvaluatedFloatDefault(
			self.plateThicknessOverThickness, 'plateThicknessOverThickness', xmlElement)
		self.pressureAngle = evaluate.getEvaluatedFloatDefault(self.pressureAngle, 'pressureAngle', xmlElement)
		self.profileSurfaces = evaluate.getEvaluatedIntDefault(self.profileSurfaces, 'profileSurfaces', xmlElement)
		self.rackHoleRadiusOverWidth = evaluate.getEvaluatedFloatDefault(
			self.rackHoleRadiusOverWidth, 'rackHoleRadiusOverWidth', xmlElement)
		self.rackHoleBelowOverWidth = evaluate.getEvaluatedFloatDefault(
			self.rackHoleBelowOverWidth, 'rackHoleBelowOverWidth', xmlElement)
		self.rackHoleStep = evaluate.getEvaluatedFloatDefault(
			self.rackHoleStep, 'rackHoleStep', xmlElement)
		self.rackLengthOverRadius = evaluate.getEvaluatedFloatDefault(self.rackLengthOverRadius, 'rackLengthOverRadius', xmlElement)
		self.rackWidthOverThickness = evaluate.getEvaluatedFloatDefault(
			self.rackWidthOverThickness, 'rackWidthOverThickness', xmlElement)
		self.rimWidthOverRadius = evaluate.getEvaluatedFloatDefault(self.rimWidthOverRadius, 'rimWidthOverRadius', xmlElement)
		self.rootBevelOverClearance = evaluate.getEvaluatedFloatDefault(
			self.rootBevelOverClearance, 'rootBevelOverClearance', xmlElement)
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
		self.teethGear = evaluate.getEvaluatedIntDefault(self.teethGear, 'teethGear', xmlElement)
		self.tipBevelOverClearance = evaluate.getEvaluatedFloatDefault(self.tipBevelOverClearance, 'tipBevelOverClearance', xmlElement)
		self.toothWidthMultiplier = evaluate.getEvaluatedFloatDefault(self.toothWidthMultiplier, 'toothWidthMultiplier', xmlElement)
		# Set absolute variables.
		self.wavelength = self.pitchRadius * 2.0 * math.pi / float(self.teethPinion)
		if self.clearance == None:
			self.clearance = self.wavelength * self.clearanceOverWavelength
		self.clearance = evaluate.getEvaluatedFloatDefault(self.clearance, 'clearance', xmlElement)
		if self.gearCollarThickness == None:
			self.gearCollarThickness = self.pinionThickness * self.gearCollarThicknessOverThickness
		self.gearCollarThickness = evaluate.getEvaluatedFloatDefault(self.gearCollarThickness, 'gearCollarThickness', xmlElement)
		if self.gearHolePaths == None:
			self.gearHolePaths = evaluate.getTransformedPathsByKey('gearHolePaths', xmlElement)
		if self.pinionCollarThickness == None:
			self.pinionCollarThickness = self.pinionThickness * self.pinionCollarThicknessOverThickness
		self.pinionCollarThickness = evaluate.getEvaluatedFloatDefault(self.pinionCollarThickness, 'pinionCollarThickness', xmlElement)
		if self.plateThickness == None:
			self.plateThickness = self.pinionThickness * self.plateThicknessOverThickness
		self.plateThickness = evaluate.getEvaluatedFloatDefault(self.plateThickness, 'plateThickness', xmlElement)
		if self.plateClearance == None:
			self.plateClearance = self.plateThickness * self.plateClearanceOverThickness
		self.plateClearance = evaluate.getEvaluatedFloatDefault(self.plateClearance, 'plateClearance', xmlElement)
		if self.rackLength == None:
			self.rackLength = self.pitchRadius * self.rackLengthOverRadius
		self.rackLength = evaluate.getEvaluatedFloatDefault(self.rackLength, 'rackLength', xmlElement)
		self.rackDemilength = 0.5 * self.rackLength
		if self.rackWidth == None:
			self.rackWidth = self.pinionThickness * self.rackWidthOverThickness
		self.rackWidth = evaluate.getEvaluatedFloatDefault(self.rackWidth, 'rackWidth', xmlElement)
		if self.rimWidth == None:
			self.rimWidth = self.pitchRadius * self.rimWidthOverRadius
		self.rimWidth = evaluate.getEvaluatedFloatDefault(self.rimWidth, 'rimWidth', xmlElement)
		if self.rootBevel == None:
			self.rootBevel = self.clearance * self.rootBevelOverClearance
		self.rootBevel = evaluate.getEvaluatedFloatDefault(self.rootBevel, 'rootBevel', xmlElement)
		if self.shaftRadius == None:
			self.shaftRadius = self.pitchRadius * self.shaftRadiusOverPitchRadius
		self.shaftRadius = evaluate.getEvaluatedFloatDefault(self.shaftRadius, 'shaftRadius', xmlElement)
		if self.collarWidth == None:
			self.collarWidth = self.shaftRadius * self.collarWidthOverShaftRadius
		self.collarWidth = evaluate.getEvaluatedFloatDefault(self.collarWidth, 'collarWidth', xmlElement)
		if self.keywayRadius == None:
			self.keywayRadius = self.shaftRadius * self.keywayRadiusOverRadius
		self.keywayRadius = lineation.getFloatByPrefixBeginEnd('keywayRadius', 'keywayDiameter', self.keywayRadius, xmlElement)
		if self.lighteningHoleMargin == None:
			self.lighteningHoleMargin = self.rimWidth * self.lighteningHoleMarginOverRimWidth
		self.lighteningHoleMargin = evaluate.getEvaluatedFloatDefault(
			self.lighteningHoleMargin, 'lighteningHoleMargin', xmlElement)
		if self.rackHoleBelow == None:
			self.rackHoleBelow = self.rackWidth * self.rackHoleBelowOverWidth
		self.rackHoleBelow = evaluate.getEvaluatedFloatDefault(self.rackHoleBelow, 'rackHoleBelow', xmlElement)
		if self.rackHoleRadius == None:
			self.rackHoleRadius = self.rackWidth * self.rackHoleRadiusOverWidth
		self.rackHoleRadius = lineation.getFloatByPrefixBeginEnd('rackHoleRadius', 'rackHoleDiameter', self.rackHoleRadius, xmlElement)
		if self.rackHoleStep == None:
			self.rackHoleStep = self.rackWidth * self.rackHoleStepOverWidth
		self.rackHoleStep = evaluate.getEvaluatedFloatDefault(self.rackHoleStep, 'rackHoleStep', xmlElement)
		if self.shaftDepthBottom == None:
			self.shaftDepthBottom = self.shaftRadius * self.shaftDepthBottomOverRadius
		self.shaftDepthBottom = evaluate.getEvaluatedFloatDefault(self.shaftDepthBottom, 'shaftDepthBottom', xmlElement)
		if self.shaftDepthTop == None:
			self.shaftDepthTop = self.shaftRadius * self.shaftDepthTopOverRadius
		self.shaftDepthTop = evaluate.getEvaluatedFloatDefault(self.shaftDepthTop, 'shaftDepth', xmlElement)
		self.shaftDepthTop = evaluate.getEvaluatedFloatDefault(self.shaftDepthTop, 'shaftDepthTop', xmlElement)
		if self.shaftPath == None:
			self.shaftPath = evaluate.getTransformedPathByKey('shaftPath', xmlElement)
		if len(self.shaftPath) < 3:
			self.shaftPath = shaft.getShaftPath(self.shaftDepthBottom, self.shaftDepthTop, self.shaftRadius, -self.shaftSides)
		if self.tipBevel == None:
			self.tipBevel = self.clearance * self.tipBevelOverClearance
		self.tipBevel = evaluate.getEvaluatedFloatDefault(self.tipBevel, 'tipBevel', xmlElement)
		# Set derived values.
		self.helixRadian = math.radians(self.helixAngle)
		if self.teethGear <= 0.0 and self.operatingAngle != 180.0:
			print('Warning, an operatingAngle other than 180 degrees can only work with a positive number of gear teeth.')
			print('Therefore the operatingAngle will be reset to 180 degrees.')
			self.operatingAngle = 180.0
		self.tanHelix = math.tan(self.helixRadian)
		self.helixThickness = self.tanHelix * self.pinionThickness
		self.operatingRadian = math.radians(self.operatingAngle)
		self.pitchRadiusGear = self.pitchRadius * float(self.teethGear) / float(self.teethPinion)
		self.pressureRadian = math.radians(self.pressureAngle)
		self.cosPressure = math.cos(self.pressureRadian)
		self.sinPressure = math.sin(self.pressureRadian)
		self.tanPressure = math.tan(self.pressureRadian)
		self.halfWavelength = 0.5 * self.wavelength
		if self.helixPath == None:
			self.helixPath = euclidean.getComplexPath(evaluate.getTransformedPathByKey('helixPath', xmlElement))
		if len(self.helixPath) < 1:
			self.helixPath = getHelixComplexPath(self, xmlElement)
		self.quarterWavelength = 0.25 * self.wavelength
		self.shaftRimRadius = self.shaftRadius + self.collarWidth
		self.toothProfileHalf = getToothProfileHalfCylinder(self, self.pitchRadius)
		self.toothProfileHalf = getWidthMultipliedPath(self.toothProfileHalf, self.toothWidthMultiplier)
		self.addendum = self.toothProfileHalf[-1].imag - self.pitchRadius
		self.dedendum = abs(self.toothProfileHalf[-1]) - self.pitchRadius + self.clearance
		self.pinionToothProfile = getToothProfileCylinderByProfile(self, self.pitchRadius, self.teethPinion, self.toothProfileHalf)
		self.xmlElement = xmlElement
