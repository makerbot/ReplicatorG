#!/bin/bash

#
# Profile cascading script
# 
# Cascade hierarchy:
# 
# Makerbot-baseline 
# +--Thingomatic-baseline
# |  +--Thingomatic-non-heated
# |     +--Thingomatic-HBP
# |     |  +--Thingomatic-HBP-Stepstruder
# |     +--Thingomatic-ABP
# |        +--Thingomatic-HBP-Stepstruder
# +--Cupcake-baseline
#    +--Cupcake-non-heated
#       +--Cupcake-HBP
#       +--Cupcake-ABP
# 
# Examples of how to generate new diffs (NB! these must be manually examined before use):
# diff -P -u -x "*~" -x "\.*" -r SF35-Makerbot-baseline SF35-Thingomatic-baseline > Thingomatic.diff
# diff -P -u -x "*~" -x "\.*" -r SF35-Makerbot-baseline SF35-cupcake-baseline > cupcake.diff
# diff -P -u -x "*~" -x "\.*" -r SF35-Thingomatic-non-heated SF35-Thingomatic-HBP > HBP.diff
# diff -P -u -x "*~" -x "\.*" -r SF35-Thingomatic-non-heated SF35-Thingomatic-ABP > Thingomatic-ABP.diff
# diff -P -u -x "*~" -x "\.*" -r SF35-Thingomatic-HBP SF35-Thingomatic-HBP-Stepstruder > Stepstruder.diff
# diff -P -u -x "*~" -x "\.*" -r SF35-cupcake-non-heated SF35-cupcake-ABP > cupcake-ABP.diff
#

if [ $# != 2 ]; then
  echo "Usage: $0 <skeinforge-version> <diffdir>"
  exit
fi

VERSION=$1
DIFFDIR=$2
SCRIPTDIR=$PWD

echo "Using skeinforge version $VERSION"
echo "Patching using $DIFFDIR"

DIFFS=$PWD/$DIFFDIR
cd skeinforge-$VERSION/skeinforge_application/prefs-experimental

echo "Building profiles:"

echo "o SF$VERSION-Thingomatic-baseline"
rm -rf SF$VERSION-Thingomatic-baseline
cp -r SF$VERSION-Makerbot-baseline SF$VERSION-Thingomatic-baseline
cd SF$VERSION-Thingomatic-baseline; patch -s -p1 < $DIFFS/Thingomatic.diff; cd ..

echo "o SF$VERSION-Thingomatic-non-heated"
rm -rf SF$VERSION-Thingomatic-non-heated
cp -r SF$VERSION-Thingomatic-baseline SF$VERSION-Thingomatic-non-heated

echo "o SF$VERSION-Thingomatic-HBP"
rm -rf SF$VERSION-Thingomatic-HBP
cp -r SF$VERSION-Thingomatic-non-heated SF$VERSION-Thingomatic-HBP
cd SF$VERSION-Thingomatic-HBP; patch -s -p1 < $DIFFS/HBP.diff; cd ..

echo "o SF$VERSION-Thingomatic-ABP"
rm -rf SF$VERSION-Thingomatic-ABP
cp -r SF$VERSION-Thingomatic-non-heated SF$VERSION-Thingomatic-ABP
cd SF$VERSION-Thingomatic-ABP; patch -s -p1 < $DIFFS/Thingomatic-ABP.diff; cd ..

echo "o SF$VERSION-Thingomatic-HBP-Stepstruder"
rm -rf SF$VERSION-Thingomatic-HBP-Stepstruder
cp -r SF$VERSION-Thingomatic-HBP SF$VERSION-Thingomatic-HBP-Stepstruder
cd SF$VERSION-Thingomatic-HBP-Stepstruder; patch -s -p1 < $DIFFS/Stepstruder.diff; cd ..

echo "o SF$VERSION-Thingomatic-ABP-Stepstruder"
rm -rf SF$VERSION-Thingomatic-ABP-Stepstruder
cp -r SF$VERSION-Thingomatic-ABP SF$VERSION-Thingomatic-ABP-Stepstruder
cd SF$VERSION-Thingomatic-ABP-Stepstruder; patch -s -p1 < $DIFFS/Stepstruder.diff; cd ..

echo "o SF$VERSION-cupcake-baseline"
rm -rf SF$VERSION-cupcake-baseline
cp -r SF$VERSION-Makerbot-baseline SF$VERSION-cupcake-baseline
cd SF$VERSION-cupcake-baseline; patch -s -p1 < $DIFFS/cupcake.diff; cd ..

echo "o SF$VERSION-cupcake-non-heated"
rm -rf SF$VERSION-cupcake-non-heated
cp -r SF$VERSION-cupcake-baseline SF$VERSION-cupcake-non-heated

echo "o SF$VERSION-cupcake-HBP"
rm -rf SF$VERSION-cupcake-HBP
cp -r SF$VERSION-cupcake-baseline SF$VERSION-cupcake-HBP
cd SF$VERSION-cupcake-HBP; patch -s -p1 < $DIFFS/HBP.diff; cd ..

echo "o SF$VERSION-cupcake-ABP"
rm -rf SF$VERSION-cupcake-ABP
cp -r SF$VERSION-cupcake-non-heated SF$VERSION-cupcake-ABP
cd SF$VERSION-cupcake-ABP; patch -s -p1 < $DIFFS/cupcake-ABP.diff; cd ..

echo "Building overrides:"
echo "o Combined-Raftless HBP"
$SCRIPTDIR/generate_override.sh SF35-Thingomatic-HBP-Stepstruder $DIFFS/Combined-Raftless.override SF35-Thingomatic-HBP-Stepstruder-Combined
echo "o Combined-Raftless ABP"
$SCRIPTDIR/generate_override.sh SF35-Thingomatic-ABP-Stepstruder $DIFFS/Combined-Raftless.override SF35-Thingomatic-ABP-Stepstruder-Combined
