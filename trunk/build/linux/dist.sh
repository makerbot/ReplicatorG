#!/bin/sh

REVISION=`head -c 4 ../../todo.txt`

rm -rf work

./make.sh

echo Creating linux distribution for revision $REVISION...

rm -rf work/classes

# remove any old boogers
rm -rf replicatorg
rm -rf replicatorg-*

mv work replicatorg

# remove boogers
find replicatorg -name "*~" -exec rm -f {} ';'
find replicatorg -name ".DS_Store" -exec rm -f {} ';'
find replicatorg -name "._*" -exec rm -f {} ';'
find replicatorg -name "Thumbs.db" -exec rm -f {} ';'

# clean out the cvs entries
find replicatorg -name ".svn" -exec rm -rf {} ';' 2> /dev/null

# zip it all up for release
echo Creating tarball and finishing...
P5=replicatorg-$REVISION
mv replicatorg $P5

tar cfz $P5-linux.tgz $P5
# nah, keep the new directory around
#rm -rf $P5

#echo Done.
