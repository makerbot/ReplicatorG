#!/bin/bash

if [ $# != 2 ]; then
  echo "Usage: $0 <baseline> <overridefile>"
  exit
fi

BASELINE=$1
OVERRIDES=$2

echo "Using skeinforge baseline $BASELINE"
echo "Using override file $OVERRIDES"

echo "Building new-profile"
rm -rf new-profile
cp -r $BASELINE new-profile
echo "Applying overrides"
./apply_overrides.py -p new-profile -o $OVERRIDES
