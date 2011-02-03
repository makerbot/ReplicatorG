(**** Beginning of end.gcode ****)
M104 S0 T0 (set extruder temperature)
M109 S0 T0 (set heated-build-platform temperature)
M102 (Extruder on, reverse)
G04 P2000 (Wait t/1000 seconds)
M103 (Extruder off)

(**** begin move to cooling position ****)
G91
G1 Z10
(**** end move to cooling position ****)
(**** end of end.gcode ****)
