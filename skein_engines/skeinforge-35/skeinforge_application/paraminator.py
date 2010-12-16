#!/usr/bin/python
#
# Copyright (c) 2010 MakerBot Industries
# 
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
#
"""Paraminator.py

The purpose of this script is to generate a print run that tests a series of values for a parameter.  The print will be a series of objects each with a slightly different value for the parameter.  The objects themselves will each be a letter of the alphabet.  This gives you 26 unique objects to select from.  Adding numbers would bring it up to 36.

For example, if you wanted to test various Speed settings, you could do something like this:

python parameter_tester.py --prefs="thingomatic" --file="speed.csv" --parameter="Feedrate (mm/s)" --start=30.0 --end=40.0 --increment=1.0

This would create a file called speed_tester.gcode.  This file would be a series of 11 objects to be printed on an ABP.  Each object would be slightly different according to the chart below.

Letter - Speed
A - 30
B - 31
C - 32
D - 33
E - 34
F - 35
G - 36
H - 37
I - 38
J - 39
K - 40

All you need to do in order to test a parameter is to find the file, set some conditions, and then run the generated gcode.  Yay!

Usage: python paraminator.py [options]

Options:
  -h, --help			show this help
  --file				the preferences file to be tested
  --parameter			the parameter to be tested
  --start				the initial setting of the parameter
  --end					the final setting of the parameter
  --increment			the increment between objects

"""

import sys, os, getopt, math, tempfile, shutil, re, fileinput

class Paraminator:
	"Class to handle generating parameter testing code."
	def __init__(self, prefsFile, parameter, start, end, increment, output):

		self.prefsFile = prefsFile
		self.prefsDir = os.path.dirname(prefsFile)
		self.parameter = parameter
		self.start = start				
		self.end = end
		self.increment = increment
		self.output = output

		self.rows = int((end-start)/increment)+1

	def getParameter(self, i):
		return self.start + self.increment*i

	def getCommand(self, i, prefs):
		return "python skeinforge.py --prefdir=%s test-objects/%s.stl" % (prefs, chr(97+i)) 

	def tweakFile(self, prefsFile, param):
		for line in fileinput.input(prefsFile, inplace=1):
			pattern = "%s" % (re.escape(self.parameter))
			if re.match(pattern, line):
				print "%s:\t%.2f" % (self.parameter, param)
			else:
				print line.rstrip()
		#print "diff ", prefsFile, " ", self.prefsFile

	def generateObject(self, i):

		# copy the prefs to a temp dir
		tempDir = tempfile.mkdtemp('', 'temp/tmp', './')
		os.rmdir(tempDir)
		shutil.copytree(self.prefsDir, tempDir)

		#edit our prefs in place.
		tempPrefs = "%s/%s" % (tempDir, os.path.basename(self.prefsFile))
		self.tweakFile(tempPrefs, self.getParameter(i))

		#generate our gcode file
		command = self.getCommand(i, tempDir)
		os.system(command)

		#add it to our main file
		singleFile = "test-objects/%s.gcode" % (chr(97+i))
		#os.system("cat %s >> %s" % (singleFile, self.output))
		#os.remove(singleFile)
		infile = open(singleFile)
		contents = infile.read()
		outfile = open(self.output, 'a')
		outfile.write(contents)
		outfile.write(os.linesep)
		outfile.close()
		
		
	def generate(self):
		"Generate the actual GCode"

		#create our temporary tree
		if (os.path.exists("temp")):	
			shutil.rmtree("temp")
		os.mkdir("temp")

		#create our output 
		fpOut = open(self.output, 'w')
		fpOut.write("(--------- PARAMINATOR v1.0 ---------)\n")
		fpOut.write("(Automated test of the '%s' parameter // File: %s)\n" % (self.parameter, self.prefsFile))
		fpOut.write("(Values from %.2f to %.2f in increments of %.2f)\n" % (self.start, self.end, self.increment))
		fpOut.write("(Total object count is %d)\n" % (self.rows))
		fpOut.write("(Command: %s)\n" % (" ".join(sys.argv)))
		fpOut.write("(After you run the script, use the reference below to determine the parameter for each object.)\n")
		for i in range(self.rows):
			fpOut.write("(%s = %.2f)\n" % (chr(65+i), self.getParameter(i)))
		fpOut.close()
		
		for i in range(self.rows):
			print "Generating object %d of %d" % (i+1, self.rows)
			self.generateObject(i)
			
		print "GCode Complete.  File located at %s" % (self.output)

def main(argv):
	
	try:
		opts, args = getopt.getopt(argv, '', [
			'help',
			'parameter=',
			'file=',
			'start=',
			'end=',
			'increment=',
			'output='
		])
	except getopt.GetoptError:
		usage()
		sys.exit(2)
        
	prefsFile = ""
	parameter = ""
	start = 0.0
	end = 0.0
	increment = 0.0
	output = "temp/output.gcode"
	
	print "Argv raw= ", str(argv)
	print "Options= ", str(opts)
	print "Arguments= ", str(args)

	for opt, arg in opts:
		if opt in ("-h", "--help"):
			usage()
			sys.exit()
		elif opt in ("--file"):
			prefsFile = arg
		elif opt in ("--parameter"):
			parameter = arg
		elif opt in ("--start"):
			start = float(arg)
		elif opt in ("--end"):
			end = float(arg)
		elif opt in ("--increment"):
			increment = float(arg)
		elif opt in ("--output"):
			output = arg
		else:
			print "FAIL!!!!"
			
	testy = Paraminator(prefsFile, parameter, start, end, increment, output)
	testy.generate()

def usage():
    print __doc__

if __name__ == "__main__":
	main(sys.argv[1:])
