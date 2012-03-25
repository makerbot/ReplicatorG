(**** This is a build-platform calibration script for a Dual-Head MakerBot Replicator ****)
(**** Do not run this code on any other kind of machine! ****)
G21
G90
M71 (Find the 4 knobs on the botton of the   platform and turn totighten completely.  )
M71 (I'm going to move   the extruder to     various positions   for adjustment.    )
M71 (In each position,   we will need to     adjust 2 knobs at   the same time.      )
M71 (Nozzles are at the  right height when   you can just slide asheet of paper     )
M71 (between the nozzle  and the platform.   Grab a sheet of     paper to assist us.)
G161 Z
G92 Z0
M70 (Please wait)
G1 Z5
G162 X Y
G92 X135 Y75 Z5 (Set location of endstops)
;M131 X Y (Save location to eeprom for future reference)
M70 ( Please wait)
G1 X0 Y0
G1 Y-74
G1 Z0
M71 (Adjust the front twoknobs until paper   just slides between nozzle and platform )
M70 ( Please wait)
G1 Z5
G1 X95
G1 Z0
M71 (Adjust the right twoknobs until paper   just slides between nozzle and platform )
M70 ( Please wait)
G1 Z5
G1 X-98
G1 Z0
M71 (Adjust the left two knobs until paper   just slides between nozzle and platform )
M70 ( Please wait)
G1 Z5
G1 X0 Y72
G1 Z0
M71 (Adjust the back two knobs until paper   just slides between nozzle and platform )
M70 ( Please wait)
G1 Z5
G1 X95
G1 Z0
M71 (Adjust the right twoknobs until paper   just slides between nozzle and platform )
M70 ( Please wait)
G1 Z5
G1 X-97
G1 Z0
M71 (Adjust the left two knobs until paper   just slides between nozzle and platform )
G1 Z5
G1 X0 Y0
G1 Z0
