#!/usr/bin/python

# Here are some G and M  codes you may find useful.
# G1 X5 Y-5 Z6 F3300.0 (Move to postion <x,y,z>=<5,-5,6> at speed 3300.0)
# G10 L2 P0 X20 Y0 (set preset work coordinate system 0, G54, offset to add 20 to the x axis and 0 to the y axis)
# G10 L2 P1 X20 Y0 (set preset work coordinate system 1, G55, offset to add 20 to the x axis and 0 to the y axis)
# G21 (set units to mm)
# G53 (use the machine coordinate system)
# G54 (use preset work coordinate system 1)
# G55 (use preset work coordinate system 2)
# G56 (use preset work coordinate system 3)
# G57 (use preset work coordinate system 4)
# G58 (use preset work coordinate system 5)
# G59 (use preset work coordinate system 6)
# G90 (set positioning to absolute)
# G92 X0 Y0 Z0 (set current position to <x,y,z>=<0,0,0>)
# M101 (set toolhead 0 on, forward)
# M102 (set toolhead 0 on, reverse)
# M103 (set toolhead 0 off)
# M104 S220 T0 (set toolhead 0 temperature to 220)
# M108 S255 (set toolhead 0 speed to 255)
# M109 S135 T0 (set toolhead 0 heated-build-platform temperature to 135)
# M201 (set toolhead 1 on, forward)
# M202 (set toolhead 1 on, reverse)
# M203 (set toolhead 1 off)
# M204 S220 T1 (set toolhead 1 temperature to 220)
# M208 S255 (set toolhead 1 speed to 255)
# M209 S135 T1 (set toolhead 1 heated-build-platform temperature)


# The nozzles are 19.2374 mm apart along the x axis.

import os, sys
usage = "usage: %s <inputfile>" %         os.path.basename(sys.argv[0])

objectToolHead = "T3 (G54)"
supportToolHead = "T4 (G55)"

supportToObject = """
M102 T4 (set toolhead T on, reverse)
M101 T3 (Extruder on, forward)
G04 P2200 (Wait t/1000 seconds)
M103 T4 (set toolhead T off)
M103 T3 Extruder off)
G54
"""
objectToSupport = """
M102 T3 (set toolhead 1 on, reverse)
M101 T4 (Extruder on, forward)
G04 P2200 (Wait t/1000 seconds)
M103 T3 (set toolhead 1 off)
M103 T4 (Extruder off)
G55
"""

supportOpening = "M108 S510.0"
supportReplace = "M108 S255 support replaced"
objectOpening = "M108 S255.0"
objectReplace = "M108 S255 object replaced"



if len(sys.argv) == 2:
    if os.path.isfile(sys.argv[1]) == True:
        print "Switcherooing tool heads..."
        input = open(sys.argv[1])
        output = open("switcherood-"+sys.argv[1], 'w')
        for s in input:
            output.write((s.replace(supportOpening, objectToSupport + supportReplace)).replace(objectOpening, supportToObject + objectReplace))
#        for s in output:
#            output.write(s.replace(objectOpening, supportToObject + objectReplace))
        input.close()
        output.close()
        print "Toolheads switcherood"
        print objectToolHead + ": object material toolhead."
        print supportToolHead + ": support material toolhead."
    else:
        print sys.argv[1] + " is not a file or does not exist."
else:
        print usage


