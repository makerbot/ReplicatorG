#!/bin/bash

if [ $# -lt 2 ]; then
  echo "Usage: $0 <baseline> <overridefile> [<new-profile>]"
  exit
fi

BASELINE=$1
OVERRIDES=$2
if [ $# == 3 ]; then
  NEWPROFILE=$3
else
  NEWPROFILE=new-profile
fi

#echo "Using skeinforge baseline $BASELINE"
#echo "Using override file $OVERRIDES"

#echo "Building $NEWPROFILE"
rm -rf "$NEWPROFILE"
cp -r "$BASELINE" "$NEWPROFILE"
#echo "Applying overrides"
`dirname $0`/apply_overrides.py -p "$NEWPROFILE" -o "$OVERRIDES"
