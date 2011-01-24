#!/bin/sh

BASE="$1"
SRC="$2"
DEST="$3"
VOLUME="$4"

echo Base Directory $1
echo Source $2
echo Destination $3
echo Volume $4

cd $BASE

hdiutil create $VOLUME.dmg -srcfolder $SRC -format UDZO -volname $VOLUME
