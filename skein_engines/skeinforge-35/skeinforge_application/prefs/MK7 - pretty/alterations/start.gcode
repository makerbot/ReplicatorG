(**** beginning of start.gcode ****)
(This file is for a MakerBot Thing-O-Matic)
(**** begin initialization commands ****)
G21 (set units to mm)
G90 (set positioning to absolute)
M108 R5.0 (set extruder speed)
M103 (Make sure extruder is off)
M104 S225 T0 (set extruder temperature)
M109 S110 T0 (set heated-build-platform temperature)
(**** end initialization commands ****)
(**** begin homing ****)
G162 Z F500 (home Z axis maximum)
G92 Z10 (set Z to 10)
G1 Z0 (move Z down 0)
G162 Z F100 (home Z axis maximum)
G161 X Y F2500 (home XY axes minimum)
M132 X Y Z A B (Recall stored home offsets for XYZAB axis)
(**** end homing ****)
(**** begin pre-wipe commands ****)
G1 X52 Y-57.0 Z10 F3300.0 (move to waiting position)
M6 T0 (wait for toolhead parts, nozzle, HBP, etc., to reach temperature)
G04 P60000 (Wait t/1000 seconds)
(**** Start Acceleration ****)
(Note: nozzles smaller than 0.5mm might require adjustments to this acceleration routine)
M108 R1.0
G04 P15000
M101
M108 R2.0
G04 P10000
M101
M108 R3.0
G04 P5000
M101
M108 R4.0
G04 P5000
M101
M108 R5.0
G04 P2500
(**** End Acceleration ****)
M101 (Extruder on, forward)
G04 P5000 (Wait t/1000 seconds)
M103 (Extruder off)
(**** end pre-wipe commands ****)
(**** end of start.gcode ****)
