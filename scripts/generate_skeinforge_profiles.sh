#!/bin/bash

if [ $# != 2 ]; then
  echo "Usage: $0 <skeinforge-version> <diffdir>"
  exit
fi

VERSION=$1
DIFFDIR=$2

echo "Using skeinforge version $VERSION"
echo "Patching using $DIFFDIR"

SOURCE=$PWD/$DIFFDIR
cd ../skein_engines/skeinforge-$VERSION/skeinforge_application/prefs

echo "Building SF$VERSION-Thingomatic-non-heated"
rm -rf SF$VERSION-Thingomatic-non-heated
cp -r SF$VERSION-Thingomatic-baseline SF$VERSION-Thingomatic-non-heated

echo "Building SF$VERSION-Thingomatic-HBP"
rm -rf SF$VERSION-Thingomatic-HBP
cp -r SF$VERSION-Thingomatic-non-heated SF$VERSION-Thingomatic-HBP

echo "Building SF$VERSION-Thingomatic-ABP"
rm -rf SF$VERSION-Thingomatic-ABP
cp -r SF$VERSION-Thingomatic-HBP SF$VERSION-Thingomatic-ABP
cd SF$VERSION-Thingomatic-ABP; patch -s -p1 < $SOURCE/ABP.diff; cd ..

echo "Building SF$VERSION-Thingomatic-HBP-Stepstruder"
rm -rf SF$VERSION-Thingomatic-HBP-Stepstruder
cp -r SF$VERSION-Thingomatic-HBP SF$VERSION-Thingomatic-HBP-Stepstruder
cd SF$VERSION-Thingomatic-HBP-Stepstruder; patch -s -p1 < $SOURCE/Stepstruder.diff; cd ..

echo "Building SF$VERSION-Thingomatic-ABP-Stepstruder"
rm -rf SF$VERSION-Thingomatic-ABP-Stepstruder
cp -r SF$VERSION-Thingomatic-ABP SF$VERSION-Thingomatic-ABP-Stepstruder
cd SF$VERSION-Thingomatic-ABP-Stepstruder; patch -s -p1 < $SOURCE/Stepstruder.diff; cd ..
