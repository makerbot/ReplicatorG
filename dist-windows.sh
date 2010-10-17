#!/bin/sh

#REVISION=`head -c 4 todo.txt`
REVISION=`head -n 1 changelog.txt | sed 's/[^0-9a-zA-Z-]//g'`

rm -rf dist/windows
ant clean
ant -Dreplicatorg.version=$REVISION dist-windows

echo "To publish: # rsync --progress -va dist/replicatorg-0020*windows.zip erikdebruijn.nl@web1.lowvoice.nl:/home/www/klanten/e/erikdebruijn.nl/ftproot/webroot/replicatorg/"
