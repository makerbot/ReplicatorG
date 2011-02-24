(**** Beginning of end.txt ****)
M106 (fan on)
G92 X0 Y0 Z0 E3 (You are now at 0,0,0,3)
G1 F4800.0
G1 X0 Y0 Z0 E0 F4800.0 (backup filament 2mm)
G1 F300
G1 X0 Y-30 Z10 E0 F300.0 (go up 10 and "forward" 30)
M18 (turn off steppers.)
(**** begin cool for safety ****)
M104 S0 T0 (set extruder temperature)
M109 S0 T0 (set heated-build-platform temperature)
(**** end cool for safety ****)
(**** end of end.txt ****)
