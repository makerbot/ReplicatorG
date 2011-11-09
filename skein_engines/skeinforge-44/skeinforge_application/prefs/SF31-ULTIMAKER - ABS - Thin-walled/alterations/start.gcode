(beginning of start.txt)
G21 (Ultimaker profile - ABS Quality print from alterations/start.gcode)
;T0 M104 S250 (Extruder Temperature to 250 Celsius)
G21 (Metric: The unit is a millimeter)
G90 (Absolute Positioning)
G92 X0 Y0 Z0 (set origin to current position)
(T0 M109 S135 set heated-build-platform temperature)
G91
G1 Z35 F400 (lower platform for cleaning nozzle)
G92 E0 (zero the extruded length)
G1 E260 F1000 (extrude some to get the flow going)
G1 E-20 F3000 (reverse a little)
G92 E0 (zero the extruded length)
M1 (Clean the nozzle and press YES to continue...)
G1 Z-1 F100 (rise platform again)
G1 Z-34 F400 (rise platform again)
G90 
G1 Z0.4
(end of start.txt)
