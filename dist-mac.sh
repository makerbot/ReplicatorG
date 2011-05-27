#!/bin/sh

REVISION=`head -n 1 changelog.txt | cut -f 1 -d " "`

rm -rf dist
ant clean
ant -Dreplicatorg.version=$REVISION -lib build/macosx/jarbundler-2.2.0.jar dist-macosx
