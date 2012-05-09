(**** This is a build-platform calibration script for a Dual-Head MakerBot Replicator ****)
(**** Do not run this code on any other kind of machine! ****)
G21 (set units to mm)
G90 (set positioning to absolute)
M71 (Find the 4 knobs on the botton of the   platform and tightenfour or five turns.)
M71 (I'm going to move   the extruder to     various positions   for adjustment.    )
M71 (In each position,   we will need to     adjust 2 knobs at   the same time.      )
M71 (Nozzles are at the  right height when   you can just slide asheet of paper     )
M71 (between the nozzle  and the platform.   Grab a sheet of     paper to assist us.)
M70 ( Please wait)
(**** begin homing ****)
G162 X Y F2500 (home XY axes maximum)
G161 Z F1100 (home Z axis minimum)
G92 Z-5 (set Z to -5)
G1 Z0.0 (move Z to "0")
G161 Z F100 (home Z axis minimum)
G92 X152 Y75 Z0 
(M132 X Y Z A B (Recall stored home offsets for XYZAB axis)
(**** end homing ****)

M70 ( Please wait)
G1 Z5 F3300.0
G1 X16.5 Y-74 (Move to front of platform)
G0 Z0
M71 (Adjust the front twoknobs until paper   just slides between nozzle and platform )

M70 ( Please wait)
G1 Z5 F3300.0
G1 X16.5 Y72 (Move to back of platform)
G0 Z0
M71 (Adjust the back two knobs until paper   just slides between nozzle and platform )

M70 ( Please wait)
G1 Z5 F3300.0
G1 Y0 X106.5
G0 Z0
M71 (Adjust the right twoknobs until paper   just slides between nozzle and platform )

M70 ( Please wait)
G1 Z5 F3300.0
G1 X-73.5
G0 Z0
M71 (Adjust the left two knobs until paper   just slides between nozzle and platform )

M70 ( Please wait)
G1 Z5 F3300.0
G1 Y0 X16.5
G0 Z0
M71 (Check that paper    just slides between nozzle and platform )
