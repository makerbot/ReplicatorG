#!/bin/sh

REVISION=`head -c 4 ../../todo.txt`

# check to see if the version number in the app is correct
# so that mikkel doesn't kick my ass
VERSIONED=`cat ../../app/Base.java | grep $REVISION`
if [ -z "$VERSIONED" ]
then
  echo Fix the revision number in Base.java
  exit
fi

./make.sh

echo Creating ReplicatorG distribution for revision $REVISION...
echo

# remove any old boogers
rm -rf replicatorg
rm -rf replicatorg-*

# use 'shared' files as starting point
cp -r ../shared replicatorg
#mkdir replicatorg
#cp -r work/lib/targets replicatorg/lib

# new style examples thing ala reas
#cd replicatorg
#mkdir examples
#unzip -d examples -q dist/examples.zip
#rm dist/examples.zip
#rm -rf dist
#cd ..

rm -rf replicatorg/dist

# extract reference
#cd replicatorg
#unzip reference.zip
#rm reference.zip
#cd ..

# copy stuff from work/
cp work/readme.txt replicatorg
cp -r work/drivers replicatorg
#cp -r work/examples replicatorg

# directories used by the app
#mkdir replicatorg/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar replicatorg/lib/
cp work/java/lib/rt.jar replicatorg/lib/
#cp work/lib/core.jar replicatorg/lib/
#cp -r work/lib/export replicatorg/lib/
#rm -rf replicatorg/lib/export/CVS

# get jikes and depedencies
#gunzip < dist/jikes.gz > replicatorg/jikes.exe
cp dist/jikes.exe replicatorg/
chmod +x replicatorg/jikes.exe

#cp dist/ICE_JNIRegistry.dll replicatorg/
#chmod +x replicatorg/ICE_JNIRegistry.dll
#cp work/rxtxSerial.dll replicatorg/
#chmod +x replicatorg/rxtxSerial.dll
cp work/*.dll replicatorg
chmod +x replicatorg/*.dll

# get platform-specific goodies from the dist dir
cp launcher/ReplicatorG.exe replicatorg/
cp dist/run.bat replicatorg/
chmod +x replicatorg/run.bat

# convert notes.txt to windows LFs
# the 2> is because the app is a little chatty
unix2dos replicatorg/revisions.txt 2> /dev/null
unix2dos replicatorg/lib/preferences.txt 2> /dev/null
unix2dos replicatorg/lib/keywords.txt 2> /dev/null
rm -f replicatorg/*.bak
rm -f replicatorg/lib/*.bak

# remove boogers
find replicatorg -name "*~" -exec rm -f {} ';'
find replicatorg -name ".DS_Store" -exec rm -f {} ';'
find replicatorg -name "._*" -exec rm -f {} ';'
find replicatorg -name "Thumbs.db" -exec rm -f {} ';'

# chmod +x the crew
find replicatorg -name "*.dll" -exec chmod +x {} ';'
find replicatorg -name "*.exe" -exec chmod +x {} ';'
find replicatorg -name "*.html" -exec chmod +x {} ';'

# clean out the svn entries
find replicatorg -name ".svn" -exec rm -rf {} ';' 2> /dev/null

# zip it all up for release
echo Packaging standard release...
echo
P5=replicatorg-$REVISION
mv replicatorg $P5
zip -rq $P5-windows.zip $P5
# nah, keep the new directory around
#rm -rf $P5

# zip up another for experts
#echo Packaging expert release...
#echo

#cp -a $P5 $P5-expert

# can't use the run.bat that's tied to a local jre
#rm $P5-expert/run.bat
#cp dist/run-expert.bat $P5-expert/run.bat
#chmod +x $P5-expert/run.bat

# remove enormous java runtime
#rm -rf $P5-expert/java
#zip -rq $P5-expert.zip $P5-expert

echo Done.

