(beginning of start.txt)
G21 (Metric FTW)
G90 (Absolute Positioning)
G28 X0 Y0 Z0 (Home the Axis to Optos)
G92 X0 Y0 Z0 (Reset Home Points)
G0 X115 Y5 Z0 (Move to purge plate for warmup)
M104 S185 T0 (Extruder Temperature to 195 Celsius)
M105
G04 P20000
M105
G04 P20000
M105

G92 E0 (Reset extruder position)
G0 E1000 (Extrude 500mm onto purge plate)
G92 E0 (Reset extruder again for print)
G04 P5000 (Wait 5 seconds)
G0 X80 Y80 Z0    (Go back to zero.)
G92 X0 Y0 Z0 (Reset Home Points)
(end of start.txt)

