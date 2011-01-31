#!/bin/bash

if [ $# != 1 ]; then exit; fi

echo "using override file $1"

echo "Building new-profile"
rm -rf new-profile
cp -r ../skein_engines/skeinforge-35/skeinforge_application/prefs/SF35-Thingomatic-baseline new-profile
echo "Applying overrides"
./apply_overrides.py -p new-profile -o $1
