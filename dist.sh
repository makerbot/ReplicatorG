#!/bin/sh

REVISION=`head -c 4 todo.txt`

ant clean
ant -Dreplicatorg.version=$REVISION -lib jarbundler-1.8.1.jar dist
