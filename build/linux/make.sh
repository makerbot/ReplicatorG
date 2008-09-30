#!/bin/sh

# Part of the Arduino project
# http://arduino.berlios.de
#
# this is derived from the processing project
# http://www.processing.org
#
# This file is subjected to the GPL License

# NOTE: before running this script, you must set CLASSPATH to
# your standard Java classpath, and DIRAVR to the root of your
# avr-gcc installation.

#JAVA_OPTIONS="-Xlint:deprecation"
JAVA_OPTIONS="" 

### -- SETUP WORK DIR -------------------------------------------

#always start fresh
if test -d work 
then
  rm -rf work
fi  

# needs to make the dir because of packaging goofiness
echo "Setting up work folder."
mkdir work
mkdir work/classes

#export stuff from svn.
svn --quiet export dist/replicatorg work/replicatorg 
svn --quiet export ../shared/lib work/lib
svn --quiet export ../shared/machines.xml work/machines.xml
svn --quiet export dist/lib/librxtxSerial.so work/lib/librxtxSerial.so

### -- START BUILDING -------------------------------------------

# move to root 'arduino' directory
cd ../../app

### -- BUILD PDE ------------------------------------------------

echo Building the PDE...

javac -source 1.4 -target 1.4 $JAVA_OPTIONS -classpath ../build/linux/work/class:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/lib/registry.jar:../build/linux/work/lib/RXTXcomm.jar:../build/linux/work/lib/mrj.jar:../build/linux/work/lib/vecmath.jar:../build/linux/work/lib/j3dcore.jar:../build/linux/work/lib/j3dutils.jar:$CLASSPATH -d ../build/linux/work/classes drivers/*.java models/*.java exceptions/*.java ../core/*.java tools/*.java syntax/*java *.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..

echo
echo Done.
