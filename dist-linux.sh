#!/bin/sh

REVISION=`head -n 1 changelog.txt | cut -f 1 -d " "`

rm -rf dist
ant clean
ant -Dreplicatorg.version=$REVISION dist-linux
echo '#!/bin/sh' >replicatorg
echo cd ./dist/linux/replicatorg-$REVISION >>replicatorg
echo ./replicatorg \& >>replicatorg
chmod a+x replicatorg
