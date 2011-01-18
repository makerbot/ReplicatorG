"""
Analyze is a script to access the plugins which analyze a gcode file.

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from fabmetheus_utilities import gcodec
from fabmetheus_utilities import settings
from skeinforge_application.skeinforge_utilities import skeinforge_polyfile
import os


__author__ = 'Enrique Perez (perez_enrique@yahoo.com)'
__date__ = '$Date: 2008/21/04 $'
__license__ = 'GPL 3.0'


def getNewRepository():
	"Get the repository constructor."
	return AnalyzeRepository()

def getPluginFileNames():
	"Get analyze plugin fileNames."
	return gcodec.getPluginFileNamesFromDirectoryPath( getPluginsDirectoryPath() )

def getPluginsDirectoryPath():
	"Get the plugins directory path."
	return gcodec.getAbsoluteFolderPath( os.path.dirname( __file__ ), os.path.join('skeinforge_plugins', 'analyze_plugins') )

def writeOutput( fileName, fileNameSuffix, gcodeText = ''):
	"Analyze a gcode file."
	gcodeText = gcodec.getTextIfEmpty(fileName, gcodeText)
	pluginFileNames = getPluginFileNames()
	for pluginFileName in pluginFileNames:
		analyzePluginsDirectoryPath = getPluginsDirectoryPath()
		pluginModule = gcodec.getModuleWithDirectoryPath( analyzePluginsDirectoryPath, pluginFileName )
		if pluginModule != None:
			try:
				pluginModule.writeOutput( fileName, fileNameSuffix, gcodeText )
			except:
				print('Warning, the tool %s could not analyze the output.' % pluginFileName )


class AnalyzeRepository:
	"A class to handle the analyze settings."
	def __init__(self):
		"Set the default settings, execute title & settings fileName."
		settings.addListsToRepository('skeinforge_application.skeinforge_utilities.skeinforge_analyze.html', '', self )
		self.fileNameInput = settings.FileNameInput().getFromFileName( [ ('Gcode text files', '*.gcode') ], 'Open File for Analyze', self, '')
		importantFileNames = ['skeinview', 'behold', 'statistic']
		settings.getRadioPluginsAddPluginFrame( getPluginsDirectoryPath(), importantFileNames, getPluginFileNames(), self )
		self.executeTitle = 'Analyze'

	def execute(self):
		"Analyze button has been clicked."
		fileNames = skeinforge_polyfile.getFileOrDirectoryTypesUnmodifiedGcode( self.fileNameInput.value, [], self.fileNameInput.wasCancelled )
		for fileName in fileNames:
			writeOutput( fileName, fileName )
