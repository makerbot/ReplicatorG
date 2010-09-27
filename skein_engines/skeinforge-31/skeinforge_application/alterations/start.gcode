G21 ;metric is good!
G90 ;absolute positioning
T0; select new extruder
M104 S200
G92 E0 ;zero the extruded length
G1 Z8 F90 ;clear the bolt heads
G28 Y0 ;go home
G28 X0
G1 X135 F3000
G28 Z0
G92 Z0 ;Adjust Z height for optimum 1st layer adhesion
G1 Z8 F90
M109 S210
M113 S0.75
G1 Z0.4 F90
G1 X10 E360 F1500
G1 E150 F12000 
G92 E0
