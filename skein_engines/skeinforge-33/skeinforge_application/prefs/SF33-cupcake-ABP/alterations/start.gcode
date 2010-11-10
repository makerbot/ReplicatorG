(beginning of start.txt)
G21 (set units to mm)
G90 (set positioning to absolute)
G92 X0 Y0 Z0 (set origin to current position)
G10 P1 X9.6187
G10 P2 X-9.6187
M108 T3 S255 (set extruder speed to maximum)
M108 T4 S255 (set extruder speed to maximum)
M104 S220 T3 (set extruder temperature)
M104 S220 T4 (set extruder temperature)
M109 S135 T0 (set heated-build-platform temperature)
G57
G1 X-54 Y-15 Z6 F3300.0 (move to waiting position)
M6 T3 (wait for tool to heat up)
M6 T4 (wait for tool to heat up)
G04 P85000 (Wait t/1000 seconds)
M101 T3(Extruder on, forward)
M101 T4(Extruder on, forward)
G04 P6500 (Wait t/1000 seconds)
M103 T3 (Extruder off)
G04 P6500 (Wait t/1000 seconds)
(start wipe)
G1 X-54 Y-15 Z6 F2000.0
G1 X-54 Y15 Z6 F2000.0
G1 X-35 Y15 Z6 F2000.0
(end wipe)
G58
(start wipe)
G1 X-54 Y-15 Z6 F2000.0
G1 X-54 Y15 Z6 F2000.0
G1 X-35 Y15 Z6 F2000.0
(end wipe)
(end of start.txt)

