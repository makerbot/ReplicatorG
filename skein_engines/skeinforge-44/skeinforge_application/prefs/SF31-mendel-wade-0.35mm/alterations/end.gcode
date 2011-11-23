(end of the file, cooldown routines)
M104 S0 T0 (temp zero)
G92 Z0 (zero our z axis - hack b/c skeinforge mangles gcodes in end.txt)
G1 Z10 (go up 10 b/c it was zeroed earlier.)
G28 Y0
G1 X0 Y0 Z10 (go to 0,0,z)
G1 Y170 (present platform for part removal)

