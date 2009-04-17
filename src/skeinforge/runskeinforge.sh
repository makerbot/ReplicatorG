  1 #!/bin/sh
    2 
    3 #
    4 # Utility script for keeping track of which preference settings a file was processed
    5 # with, by copying the current preferences to a date-tagged directory together
    6 # with the output files.
    7 #
    8 # Usage: runskeinforge.sh <model.gts>
    9 #
   10 
   11 dir=`dirname $1`
   12 file=`basename $1`
   13 
   14 for s in .gts .GTS .stl .STL; do
   15   if [ ! `basename $file $s` = $file ]; then suffix=$s; fi
   16 done
   17 
   18 if [ -n $suffix ]; then
   19   filename=`basename $file $suffix`
   20   newdir=$filename-`date +%m%d%H%M`
   21   mkdir -p $newdir/skeinforge-prefs
   22   cp $1 $newdir
   23   cp ~/.skeinforge/*.csv $newdir/skeinforge-prefs
   24   python skeinforge.py $newdir/$filename$suffix
   25   echo $PWD/$newdir/${filename}_export.gcode
   26 fi