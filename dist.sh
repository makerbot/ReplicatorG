#!/bin/sh

#REVISION=`head -c 4 todo.txt`
REVISION=`head -n 1 todo.txt | sed 's/[^0-9]//g'`

rm -rf dist
ant clean
ant -Dreplicatorg.version=$REVISION -lib jarbundler-1.8.1.jar dist
