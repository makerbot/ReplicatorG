G91 (switch to relative positioning)
G1 Z10 (move z up a little)
G90 (back to absolute positioning)
G1 X45 (move to right, preparing to spit)
M6 T0 (wait for toolhead parts, nozzle, HBP, etc., to reach temperature)
G04 P3000 (Wait an extra 3 seconds for posterity)
(**** Start Acceleration ****)
(this section ramps up the speed driving the extruder motor for a nice spit.  If removed, be sure to set the extrusion speed before telling the motor to go forward)
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
M103 T0 (Extruder off)
(**** End Acceleration ****)
M108 R2.0 (set speed for extrusion)