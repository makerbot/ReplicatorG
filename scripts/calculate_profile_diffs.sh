#!/bin/bash

DIFF_COMMAND='diff -P -u -x "*~" -x "\.*" -r '

PREVIOUS="replicatorg-0023-linux"
CURRENT="replicatorg-0024-linux"

OUTPUT_FILE="release_diffs"



echo "Profile differences between ${PREVIOUS} and ${CURRENT}" > ${OUTPUT_FILE}

SKEIN_ENGINES=( "skein_engines/skeinforge-35/skeinforge_application/prefs" "skein_engines/skeinforge-0006/prefs" )

for engine in ${SKEIN_ENGINES[@]}
do
  for profile in `cd ${CURRENT}/${engine}; ls -d */`
  do
    echo "" >> ${OUTPUT_FILE}
    echo "" >> ${OUTPUT_FILE}
    echo "Comparing ${profile} *****************************************************" >> ${OUTPUT_FILE}
    previous=${PREVIOUS}/${engine}/${profile}
    current=${CURRENT}/${engine}/${profile}
    ${DIFF_COMMAND} ${previous} ${current} >> ${OUTPUT_FILE}
  done
done
