(**** Beginning of end.gcode ****)
M104 S0 T0 (set extruder temperature)
M109 S0 T0 (set heated-build-platform temperature)
M102 (Extruder on, reverse)
G04 P2000 (Wait t/1000 seconds)
M103 (Extruder off)

G91
G1 Z10
(**** end of end.gcode ****)
