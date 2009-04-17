#!/bin/sh

#REVISION=`head -c 4 todo.txt`
REVISION=`head -n 1 changelog.txt | sed 's/[^0-9]//g'`

rm -rf dist
ant clean
ant -Dreplicatorg.version=$REVISION dist-windows
