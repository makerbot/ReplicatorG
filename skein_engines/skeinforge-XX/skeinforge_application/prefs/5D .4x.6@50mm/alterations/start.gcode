(**** beginning of start.txt ****)
(This file has been sliced using Skeinforge 35)
(**** begin initilization commands ****)
G21 (set units to mm)
G90 (set positioning to absolute)
M104 S230 T0 (set extruder temperature)
M109 S125 T0 (set heated-build-platform temperature)
(**** end initilization commands ****)
(**** begin homing ****)
G1 F2400.0
G92 X0 Y0 Z0 E0 (You are now at 0,0,0)
G1 F2400.0
G1 Z15 F2400.0 (Move up for warmup)
(**** end homing ****)
(**** begin pre-wipe commands ****)
M6 T0 (Wait for tool to heat up)
G1 F50.0
G1 X0.0 Y0.0 Z15.0 E8 F50.0
G1 F2400.0
G1 X0 Y0 Z0 Z0.0 E8 F2400.0   (Go back to zero.)
G92 E0 (You are now at E0 again)
(**** end pre-wipe commands ****)
M106 (fan on)
(**** end of start.txt ****)
