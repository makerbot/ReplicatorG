#!/bin/sh

REVISION=`head -c 4 todo.txt`

rm -r dist
ant clean
ant -Dreplicatorg.version=$REVISION -lib jarbundler-1.8.1.jar dist
