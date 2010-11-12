(Beginning of end.txt)
M109 S95 T3 (set heated-build-platform temperature)
G53
G1 X0 Y54 F3300.0 (move platform to ejection position)
M102 T4 (Extruder on, reverse)
G04 P2000 (Wait t/1000 seconds)
M103 T4 (Extruder off)
G04 P90000 (wait t/1000 seconds)
M106 T3 (conveyor on)
G04 P2000 (wait t/1000 seconds)
M104 S225 T3 (set extruder temperature)
M104 S225 T4 (set extruder temperature)
M109 S130 T3 (set heated-build-platform temperature)
G04 P7000 (wait t/1000 seconds)
M107 T3 (conveyor off)
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
G53
G1 X0 Y0 F3300.0 (move nozzle to center)
G1 X0 Y0 Z0 F3300.0 (move nozzle to origin)
M104 S0 T3 (set extruder temperature)
M104 S0 T4 (set extruder temperature)
M109 S0 T3 (set heated-build-platform temperature)
(end of end.txt)

