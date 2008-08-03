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


### -- SETUP WORK DIR -------------------------------------------

if test -d work 
then
  BUILD_PREPROC=false
else
  BUILD_PREPROC=true

  # needs to make the dir because of packaging goofiness
  echo Setting up directories to build under Linux
  mkdir -p work/classes/processing/app/syntax
  mkdir -p work/classes/processing/app/tools
  mkdir -p work/lib/build

  cp dist/replicatorg work/
fi

echo Copying shared and core files...
cp -r ../shared/* work
rm -rf work/dist

echo Copying dist files...
cp -r dist/lib work/

### -- START BUILDING -------------------------------------------

# move to root 'arduino' directory
cd ../..

### -- BUILD PDE ------------------------------------------------

cd app

echo Building the PDE...

# compile the code as java 1.3, so that the application will run and
# show the user an error, rather than crapping out with some strange
# "class not found" crap
#jikes -classpath ../build/linux/work/classes:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/lib/registry.jar:../build/linux/work/lib/RXTXcomm.jar:../build/linux/work/lib/mrj.jar:$CLASSPATH -d ../build/linux/work/classes tools/*.java preproc/*.java syntax/*.java *.java 
javac -source 1.4 -target 1.4 -classpath ../build/linux/work/class:../build/linux/work/lib/antlr.jar:../build/linux/work/lib/oro.jar:../build/linux/work/lib/registry.jar:../build/linux/work/lib/RXTXcomm.jar:../build/linux/work/lib/mrj.jar:../build/linux/work/lib/vecmath.jar:../build/linux/work/lib/j3dcore.jar:../build/linux/work/lib/j3dutils.jar:$CLASSPATH -d ../build/linux/work/classes drivers/*.java ../core/*.java tools/*.java syntax/*java *.java

cd ../build/linux/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .
cd ../..

echo
echo Done.
