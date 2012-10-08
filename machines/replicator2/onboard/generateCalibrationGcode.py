#!/usr/bin/python
#
# loads s3g files and prints them to tables that are readable by a microcontroller
# in a C format

# usage python loadDataFile.py [filename] [output table name]
# filename is a string representing the name of the file to open

y = -60.6;
xStart = -70;
xEnd = -100;
e = 0.5;

print "G1 X%d Y%.2f Z0.9 F3300.0" %  (xStart, y) 
print "G1 F798.0"
print "G1 E0.3"
print "G1 F3300.0"
print "M101"

for n in range(0,13):

	print "G1 X%d Y%.2f Z0.6 F3300 E%.2f" % (xStart, y, e)
	print "G1 E%.2f" % (e+0.3)
	print "G1 X%.d Y%.2f Z0.6 F760 E%.2f" % (xEnd, y, e+4)
	print
	y += 10.1;
	e += 3.5;

x = -30.6;
yStart = -20;
yEnd = 10;

print "G1 X%.2f Y%d Z0.9 F3300.0 E%.2f" %  (x, yStart, e) 
e += 0.5;

for n in range(0,13):
	print "G1 X%.2f Y%d Z0.6 F3300 E%.2f" % (x, yStart, e)
	print "G1 E%.2f" % (e+0.3)
	print "G1 X%.2f Y%d Z0.6 F760 E%.2f" % (x, yEnd, e+4)
	print
	x += 10.1;
	e += 3.5;

print "G1 F798.0"
print "G1 E%.2f" % (e)
print "G1 F743.802"
print "M103"
print "G55"
print "M108 T1"
print "M18 A B"
print "G92 E0"

y = -60;
xStart = -70;
xEnd = -40;
e = 0.5;

print "G1 X%d Y%d Z0.9 F3300.0" %  (xStart, y) 
print "G1 F798.0"
print "G1 E0.3"
print "G1 F3300.0"
print "M101"

for n in range(0,13):

	print "G1 X%d Y%d Z0.6 F3300 E%.2f" % (xStart, y, e)
	print "G1 E%.2f" % (e+0.3)
	print "G1 X%d Y%d Z0.6 F760 E%.2f" % (xEnd, y, e+4)
	print
	y += 10;
	e += 3.5;

x = -30;
yStart = -50;
yEnd = -20;

print "G1 X%d Y%d Z0.9 F3300.0E%.2f" %  (x, yStart, e) 
e += 0.5;

for n in range(0,13):
	print "G1 X%d Y%d Z0.6 F3300 E%.2f" % (x, yStart, e)
	print "G1 E%.2f" % (e+0.3)
	print "G1 X%d Y%d Z0.6 F760 E%.2f" % (x, yEnd, e+4)
	print
	x += 10;
	e += 3.5;

