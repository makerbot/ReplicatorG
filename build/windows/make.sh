#!/bin/sh

#JAVA_OPTIONS="-Xlint:deprecation"
JAVA_OPTIONS="" 

### -- SETUP WORK DIR -------------------------------------------

if test -d work
then
  BUILD_PREPROC=false
else
  echo Setting up directories to build ReplicatorG...
  BUILD_PREPROC=true
  cp -r ../shared work
  cp  ../../machines.xml work
  rm -f work/.DS_Store 
  #cp ../../lib/*.dll work
  cp ../shared/nativelibs/*.dll work
  cp dist/*.dll work
  cp dist/run.bat work
  chmod 755 work/run.bat
  cp ../../readme.txt work
  unix2dos work/readme.txt
  
  # needs to make the dir because of packaging goofiness
  mkdir -p work/classes/arduino/app/drivers
  mkdir -p work/classes/arduino/app/exceptions
  mkdir -p work/classes/arduino/app/models
  mkdir -p work/classes/arduino/app/syntax
  mkdir -p work/classes/arduino/app/tools

  #echo Extracting reference...
  #cd work
  #unzip reference.zip
  # necessary for launching reference from shell/command prompt
  # which is done internally to view reference
  #chmod +x reference/*.html
  # needed by 'help' menu
  #chmod +x reference/environment/*.html
  # chmod -R +x *.html doesn't seem to work
  #rm reference.zip
  #cd ..

  mkdir work/lib/build
  #mkdir work/classes

  echo Compiling ReplicatorG.exe
  cd launcher
  make && cp ReplicatorG.exe ../work/
  cd ..

  # get jikes and depedencies
  cp dist/jikes.exe work/
  #chmod +x work/jikes.exe

  cp dist/ICE_JNIRegistry.dll work/

  mkdir work/drivers
  cp -r dist/drivers/* work/drivers/

  # chmod +x the crew
  find work -name "*.dll" -exec chmod +x {} ';'
  find work -name "*.exe" -exec chmod +x {} ';'
  find work -name "*.html" -exec chmod +x {} ';'
fi

cd ../..

### -- BUILD PDE ------------------------------------------------

cd src/replicatorg/app

JAVA_BIN="/cygdrive/f/Program Files/Java/jdk1.6.0_10/bin";
PATH="$PATH:$JAVA_BIN"

#CLASSPATH=dirname `which javaw.exe`
CLASSPATH="..\\build\\windows\\work\\lib\\vecmath.jar;..\\build\\windows\\work\\lib\\j3dcore.jar;..\\build\\windows\\work\\lib\\j3dutils.jar;..\\build\\windows\\work\\lib\\RXTXcomm.jar;..\\build\\windows\\work\\lib\\mrj.jar;..\\build\\windows\\work\\lib\antlr.jar;..\\build\\windows\\work\\lib\\oro.jar;..\\build\\windows\\work\\lib\\registry.jar;..\\build\\windows\\work\\classes"

# compile the code as java 1.3, so that the application will run and
# show the user an error, rather than crapping out with some strange
# "class not found" crap
# need to do this twice because otherwise dependencies aren't resolved right.
#../build/windows/work/jikes -target 1.3 +D -classpath "$CLASSPATH;..\\build\\windows\\work\\classes" -d ..\\build\\windows\\work\\classes ../core/*.java drivers/*.java syntax/*.java exceptions/*.java models/*.java tools/*.java *.java
#../build/windows/work/jikes -target 1.3 +D -classpath "$CLASSPATH;..\\build\\windows\\work\\classes" -d ..\\build\\windows\\work\\classes ../core/*.java drivers/*.java syntax/*.java exceptions/*.java models/*.java tools/*.java *.java
#javac -classpath "$CLASSPATH" -d ..\\build\\windows\\work\\classes ../core/*.java drivers/*.java syntax/*.java exceptions/*.java models/*.java tools/*.java *.java
javac -source 1.6 -target 1.6 $JAVA_OPTIONS -classpath $CLASSPATH -d ../build/windows/work/classes drivers/*.java models/*.java exceptions/*.java ../core/*.java tools/*.java syntax/*java *.java

cd ../build/windows/work/classes
rm -f ../lib/pde.jar
zip -0rq ../lib/pde.jar .

# back to build/windows
cd ../..

echo
echo Done.

