(end of the file, cooldown routines)
M104 S0 T0 (temp zero)
M109 S0 T0 (platform off)
M106 (fan on)
G92 Z0 (zero our z axis - hack b/c skeinforge mangles gcodes in end.txt)
G1 Z10 (go up 10 b/c it was zeroed earlier.)
G1 X0 Y0 Z10 (go to 0,0,z)
M18 (turn off steppers.)

