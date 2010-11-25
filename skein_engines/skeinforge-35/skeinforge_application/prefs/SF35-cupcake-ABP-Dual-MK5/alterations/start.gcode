(beginning of start.txt)
G21 (set units to mm)
G90 (set positioning to absolute)
G92 X0 Y0 Z0 (set origin to current position)
G10 P1 X14
G10 P2 X-14
M108 S255 T3 (set extruder speed to maximum)
M108 S255 T4 (set extruder speed to maximum)
M104 S220 T3 (set extruder temperature)
M104 S220 T4 (set extruder temperature)
M109 S150 T3 (set heated-build-platform temperature)
G55
G1 X-54 Y-15 Z6 F3300.0 (move to waiting position)
M6 T3 (wait for tool to heat up)
M6 T4 (wait for tool to heat up)
G04 P85000 (Wait t/1000 seconds)
M101 T3 (Extruder on, forward)
M101 T4 (Extruder on, forward)
G04 P6500 (Wait t/1000 seconds)
M103 T3 (Extruder off)
M103 T4 (Extruder off)
(start wipe)
G54
G1 X-54.0 Y-15.0 Z6.0 F2000.0
G1 X-54.0 Y15.0 Z6.0 F2000.0
G1 X-35.0 Y15.0 Z6.0 F2000.0
G55
G1 X-54.0 Y-15.0 Z6.0 F2000.0
G1 X-54.0 Y15.0 Z6.0 F2000.0
G1 X-35.0 Y15.0 Z6.0 F2000.0
(end wipe)
(end of start.txt)

