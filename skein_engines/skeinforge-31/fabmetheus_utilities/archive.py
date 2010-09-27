"""
Boolean geometry utilities.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import gcodec
import os
import sys
import traceback


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__credits__ = 'Art of Illusion <http://www.artofillusion.org/>'
__date__ = '$Date: 2008/02/05 $'
__license__ = 'GPL 3.0'


def addToNamePathDictionary(directoryPath, namePathDictionary):
	'Add to the name path dictionary.'
	pluginFileNames = gcodec.getPluginFileNamesFromDirectoryPath(directoryPath)
	for pluginFileName in pluginFileNames:
		namePathDictionary[pluginFileName.lstrip('_')] = os.path.join(directoryPath, pluginFileName)

def getDocumentationPath(subName=''):
	'Get the documentation file path.'
	return getJoinedPath(getFabmetheusPath('documentation'), subName)

def getElementsPath(subName=''):
	'Get the evaluate_elements directory path.'
	return getJoinedPath(getGeometryUtilitiesPath('evaluate_elements'), subName)

def getFabmetheusPath(subName=''):
	'Get the fabmetheus directory path.'
	return getJoinedPath(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), subName)

def getFabmetheusUtilitiesPath(subName=''):
	'Get the fabmetheus utilities directory path.'
	return getJoinedPath(getFabmetheusPath('fabmetheus_utilities'), subName)

def getFundamentalsPath(subName=''):
	'Get the evaluate_fundamentals directory path.'
	return getJoinedPath(getGeometryUtilitiesPath('evaluate_fundamentals'), subName)

def getGeometryDictionary(folderName):
	'Get to the geometry name path dictionary.'
	geometryDictionary={}
	geometryDirectory = getGeometryPath()
	addToNamePathDictionary(os.path.join(geometryDirectory, folderName), geometryDictionary)
	geometryPluginsDirectory = getFabmetheusUtilitiesPath('geometry_plugins')
	addToNamePathDictionary(os.path.join(geometryPluginsDirectory, folderName), geometryDictionary)
	return geometryDictionary

def getGeometryPath(subName=''):
	'Get the geometry directory path.'
	return getJoinedPath(getFabmetheusUtilitiesPath('geometry'), subName)

def getGeometryToolsPath(subName=''):
	'Get the geometry tools directory path.'
	return getJoinedPath(getGeometryPath('geometry_tools'), subName)

def getGeometryUtilitiesPath(subName=''):
	'Get the geometry_utilities directory path.'
	return getJoinedPath(getGeometryPath('geometry_utilities'), subName)

def getJoinedPath(path, subName=''):
	'Get the joined file path.'
	if subName == '':
		return path
	return os.path.join(path, subName)

def getProfilesPath(subName=''):
	'Get the profiles directory path, which is the settings directory joined with profiles.'
	return getJoinedPath(getSettingsPath('profiles'), subName)

def getSettingsPath(subName=''):
	'Get the settings directory path, which is the home directory joined with .skeinforge.'
	return getJoinedPath(os.path.join(os.path.expanduser('~'), '.skeinforge'), subName)

def getSkeinforgePath(subName=''):
	'Get the skeinforge directory path.'
	return getJoinedPath(getFabmetheusPath('skeinforge_application'), subName)
