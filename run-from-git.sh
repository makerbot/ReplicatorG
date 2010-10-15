#!/bin/sh

# This script will only work on POSIX systems.

x="`echo $(readlink -f $0)`"

cd ${x%`echo $(basename $0)`}

git fetch upstream; git merge upstream/master; ant run

