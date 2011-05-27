#!/bin/sh

REVISION=`head -n 1 changelog.txt | cut -f 1 -d " "`

rm -rf dist
ant clean
ant -Dreplicatorg.version=$REVISION dist-windows
