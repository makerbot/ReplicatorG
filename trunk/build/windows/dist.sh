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
rm -rf ReplicatorG
rm -rf ReplicatorG-*

# use 'shared' files as starting point
cp -r ../shared ReplicatorG
#mkdir ReplicatorG
#cp -r work/lib/targets ReplicatorG/lib

# new style examples thing ala reas
#cd ReplicatorG
#mkdir examples
#unzip -d examples -q dist/examples.zip
#rm dist/examples.zip
#rm -rf dist
#cd ..

rm -rf ReplicatorG/dist

# extract reference
#cd ReplicatorG
#unzip reference.zip
#rm reference.zip
#cd ..

# add java (jre) files
unzip -q -d ReplicatorG jre.zip

# copy stuff from work/
cp work/readme.txt ReplicatorG
cp -r work/drivers ReplicatorG
#cp -r work/examples ReplicatorG

# directories used by the app
#mkdir ReplicatorG/lib/build

# grab pde.jar and export from the working dir
cp work/lib/pde.jar ReplicatorG/lib/
cp work/java/lib/rt.jar ReplicatorG/lib/
#cp work/lib/core.jar ReplicatorG/lib/
#cp -r work/lib/export ReplicatorG/lib/
#rm -rf ReplicatorG/lib/export/CVS

# get jikes and depedencies
#gunzip < dist/jikes.gz > ReplicatorG/jikes.exe
cp dist/jikes.exe ReplicatorG/
chmod +x ReplicatorG/jikes.exe

#cp dist/ICE_JNIRegistry.dll ReplicatorG/
#chmod +x ReplicatorG/ICE_JNIRegistry.dll
#cp work/rxtxSerial.dll ReplicatorG/
#chmod +x ReplicatorG/rxtxSerial.dll
cp work/*.dll ReplicatorG
chmod +x ReplicatorG/*.dll

# get platform-specific goodies from the dist dir
cp launcher/ReplicatorG.exe ReplicatorG/
cp dist/run.bat ReplicatorG/
chmod +x ReplicatorG/run.bat

# convert notes.txt to windows LFs
# the 2> is because the app is a little chatty
unix2dos ReplicatorG/revisions.txt 2> /dev/null
unix2dos ReplicatorG/lib/preferences.txt 2> /dev/null
unix2dos ReplicatorG/lib/keywords.txt 2> /dev/null
rm -f ReplicatorG/*.bak
rm -f ReplicatorG/lib/*.bak

# remove boogers
find ReplicatorG -name "*~" -exec rm -f {} ';'
find ReplicatorG -name ".DS_Store" -exec rm -f {} ';'
find ReplicatorG -name "._*" -exec rm -f {} ';'
find ReplicatorG -name "Thumbs.db" -exec rm -f {} ';'

# chmod +x the crew
find ReplicatorG -name "*.dll" -exec chmod +x {} ';'
find ReplicatorG -name "*.exe" -exec chmod +x {} ';'
find ReplicatorG -name "*.html" -exec chmod +x {} ';'

# clean out the svn entries
find ReplicatorG -name ".svn" -exec rm -rf {} ';' 2> /dev/null

# zip it all up for release
echo Packaging standard release...
echo
P5=ReplicatorG-$REVISION
mv ReplicatorG $P5
zip -rq $P5.zip $P5
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

