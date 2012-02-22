(**** This is a build-platform calibration script for a Dual-Head MakerBot Replicator ****)
(**** Do not run this code on any other kind of machine! ****)
G21
G90
M70 (Tighten down all    thumbnuts, please!)
M01 (Find the 4 knobs at the bottom of the platform and tighten (turn them counter-clockwise) them each.  We will be using a sheet of paper to determine if the nozzles are close enough to the platform, so go grab a sheet of paper!)
G161 Z
G92 Z0
M70 (Homing axis X/Y)
G1 Z5
G162 X Y
G92 X135 Y75 Z5 (Set location of endstops)
;M131 X Y (Save location to eeprom for future reference)
G1 X0 Y0
G1 Y-74
G1 Z0
M70 (Adjust front two    thumbnuts until     nozzles touch)
M01 (Adjust the front two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X95
G1 Z0
M70 (Adjust right two    thumbnuts until     nozzles touch)
M01 (Adjust the right two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X-98
G1 Z0
M70 (Adjust left two     thumbnuts until     nozzles touch)
M01 (Adjust the left two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X0 Y72
G1 Z0
M70 (Adjust back two     thumbnuts until     nozzles touch)
M01 (Adjust the back two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X95
G1 Z0
M70 (Adjust right two    thumbnuts until     nozzles touch)
M01 (Adjust the right two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X-97
G1 Z0
M70 (Adjust left two     thumbnuts until     nozzles touch)
M01 (Adjust the left two thumbnuts until a sheet of paper slides between the nozzles and the platform.  Press OK to Continue)
G1 Z5
G1 X0 Y0
G1 Z0
M70 (Check All Thumbnuts)
M01 (Check all the thumbnuts!  Ensure a sheet of paper can slide between the nozzles and the platform.  Press OK to Continue)
M70 P5(Calibration Complete Press To Continue)
M01 (Plate calibration complete!  Press OK to Continue)
M73 P100
