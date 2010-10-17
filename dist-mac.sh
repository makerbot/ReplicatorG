#!/bin/sh

#REVISION=`head -c 4 todo.txt`
REVISION=`head -n 1 changelog.txt | sed 's/[^0-9]//g'`
REVISION=`head -n 1 changelog.txt | sed 's/[^0-9a-zA-Z-]//g'`

rm -rf dist/macosx
ant clean
ant -Dreplicatorg.version=$REVISION -lib build/macosx/jarbundler-2.1.0.jar dist-macosx
