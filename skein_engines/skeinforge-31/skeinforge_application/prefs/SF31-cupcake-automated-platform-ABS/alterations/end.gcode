(Beginning of end.txt)
M109 S100 T0 (set heated-build-platform temperature)
G1 X0 Y50 F3300.0 (move to ejection position)
M102 (Extruder on, reverse)
G04 P4000 (Wait t/1000 seconds)
M103 (Extruder off)
G04 P80000 (wait t/1000 seconds)
M106 (conveyor on)
G04 P35000 (wait t/1000 seconds)
M107 (conveyor off)
(start wipe)
G1 X-54 Y-15 Z6 F2000.0
G1 X-54 Y15 Z6 F2000.0
G1 X-35 Y15 Z6 F2000.0
(end wipe)
G1 X0 Y0 F3300.0 (recenter platform)
G1 X0 Y0 Z0 F3300.0 (recenter platform)
M104 S220 T0 (set extruder temperature)
M109 S95 T0 (set heated-build-platform temperature)
(end of end.txt)

