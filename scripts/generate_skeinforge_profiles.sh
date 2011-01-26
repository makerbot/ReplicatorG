#!/bin/bash

SOURCE=$PWD/SF35
cd ../skein_engines/skeinforge-35/skeinforge_application/prefs

echo "Building SF35-Thingomatic-non-heated"
rm -rf SF35-Thingomatic-non-heated
cp -r SF35-Thingomatic-baseline SF35-Thingomatic-non-heated

echo "Building SF35-Thingomatic-HBP"
rm -rf SF35-Thingomatic-HBP
cp -r SF35-Thingomatic-non-heated SF35-Thingomatic-HBP

echo "Building SF35-Thingomatic-ABP"
rm -rf SF35-Thingomatic-ABP
cp -r SF35-Thingomatic-HBP SF35-Thingomatic-ABP
cd SF35-Thingomatic-ABP; patch -s -p1 < $SOURCE/ABP.diff; cd ..

echo "Building SF35-Thingomatic-HBP-Stepstruder"
rm -rf SF35-Thingomatic-HBP-Stepstruder
cp -r SF35-Thingomatic-HBP SF35-Thingomatic-HBP-Stepstruder
cd SF35-Thingomatic-HBP-Stepstruder; patch -s -p1 < $SOURCE/Stepstruder.diff; cd ..

echo "Building SF35-Thingomatic-ABP-Stepstruder"
rm -rf SF35-Thingomatic-ABP-Stepstruder
cp -r SF35-Thingomatic-ABP SF35-Thingomatic-ABP-Stepstruder
cd SF35-Thingomatic-ABP-Stepstruder; patch -s -p1 < $SOURCE/Stepstruder.diff; cd ..
