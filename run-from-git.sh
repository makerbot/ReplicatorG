#!/bin/bash

# Will update to the latest version from upstream git and run
# Give a branch name as a parameter to run a specific branch

if [ $# -ge 1 ]; then
  branch=$1
else
  branch=master
fi

cd `dirname $0`

git fetch upstream                        # Get all remote changes
git checkout $branch                            # Switch to local branch if it exists
if [ $? -ne 0 ]; then                     # ..else get branch from upstream
  git checkout -b $branch upstream/$branch
fi
if [ $? -eq 0 ]; then
  git merge upstream/$branch              # Only merge if nothing went wrong
fi
if [ $? -eq 0 ]; then
  ant run                                 # Only run if nothing went wrong
fi

