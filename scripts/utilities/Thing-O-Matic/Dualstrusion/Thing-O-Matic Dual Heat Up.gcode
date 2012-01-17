(**** beginning of start.gcode ****)
(This file is for a MakerBot Thing-O-Matic)
(**** begin initialization commands ****)
G21 (set units to mm)
G90 (set positioning to absolute)
M108 T0 R1.98 (set extruder speed)
M108 T1 R1.98
M103 T0(Make sure extruder is off)
M103 T1
M104 S235 T0(set extruder temperature)
M104 S235 T1
M109 S125 T1(set heated-build-platform temperature)
M6 T0(wait for toolhead parts, nozzle, HBP, etc., to reach temperature)
M6 T1