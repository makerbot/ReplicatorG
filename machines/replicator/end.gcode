(******* End.gcode*******)
M73 P100 (end  build progress )
G0 Z155
M18
M109 S0 T0 (Cool down HBP)
M104 S0 T0 (Cool down Right Extruder)
M104 S0 T1 (Cool down Left Extruder)
G162 X Y F2500
M18
M70 P5 ( We <3 Making Things!)
M72 P1  ( Play Ta-Da song )
(*********end End.gcode*******)
