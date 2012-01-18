(**** This is a build-platform calibration script for a Dual-Head MakerBot Replicator ****)
(**** Do not run this code on any other kind of machine! ****)
G21
G90
M71 (Tighten down all    thumbnuts, please!                       Press to Continue)
G161 Z
G92 Z0
M70 (Homing axis X+Y                                                 Please wait)
G1 Z5
G162 X Y
G92 X135 Y75 Z5 (Set location of endstops)
;M131 X Y (Save location to eeprom for future reference)
M70 (Returning Home                                                  Please wait)
G1 X0 Y0
M70 (Moving to next      Positon)
G1 Y-74
G1 Z0
M71 (Adjust front two    thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X95
G1 Z0
M71 (Adjust right two    thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X-98
G1 Z0
M71 (Adjust left two     thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X0 Y72
G1 Z0
M71 (Adjust back two     thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X95
G1 Z0
M71 (Adjust right two    thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X-97
G1 Z0
M71 (Adjust left two     thumbnuts until     nozzles touch        Press to Continue)
M70 (Moving to next      Positon)
G1 Z5
G1 X0 Y0
G1 Z0
M71 (Check all           thumbnuts!                             Press to Continue)
M70 (Moving to next      Positon)
M71 (Plate    calibration      complete!                          Press to Continue)
