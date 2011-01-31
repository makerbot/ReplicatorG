#!/bin/bash

if [ $# != 2 ]; then
  echo "Usage: $0 <skeinforge-version> <overridefile>"
  exit
fi

VERSION=$1
OVERRIDES=$2

echo "Using skeinforge version $VERSION"
echo "Using override file $OVERRIDES"

echo "Building new-profile"
rm -rf new-profile
cp -r ../skein_engines/skeinforge-$VERSION/skeinforge_application/prefs/SF$VERSION-Thingomatic-baseline new-profile
echo "Applying overrides"
./apply_overrides.py -p new-profile -o $OVERRIDES
