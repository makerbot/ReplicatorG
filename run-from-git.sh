#!/bin/bash

# Will update to the latest version from upstream git and run
# Give a branch name as a parameter to run a specific branch

if [ $# -ge 1 ]; then
  branch=$1
else
  branch=master
fi

cd `dirname $0`

git fetch upstream
git co -B $branch upstream/$branch
git merge upstream/$branch
ant run
