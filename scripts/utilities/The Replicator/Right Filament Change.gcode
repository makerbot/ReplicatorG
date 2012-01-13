(**** The Replicator Utility ****)
(* Change the Filament in Toolhead T0, Right Toolhead *)

M01  (Filament Load Script. Yes to  Continue )
M70 P5 (Heating for            filament release)
G21 (set units to mm)
G90 (set positioning to absolute)
M108 R3.0 T0 (set extruder speed left)
M103 T0 (Make sure extruder is off)

M73 P0 (enable build progress 'Change Filament')

M104 S225 T0 (set extruder temperature)

M6 T0 (wait for toolhead parts, nozzle, HBP, etc., to reach temperature)

M70 P90(  Ejecting Filament )
M102 T0 (Extruder on, reverse)
G04 P90000 (Wait t/1000 seconds)
M01 ( Remove old Filament Load new Filament   Yes to Continue )
M101 T0 (Extruder on, forward)
M70 P120(  Loading Filament )
G04 P120000 (Wait t/1000 seconds)

M70 P5 (   Loading Script         Complete )

M103 T0 (Extruder off)
M104 S0 T0 (set extruder temperature)
M01 (Toolhead temperature set to 0. Yes to Continue )
M73 P100 (build end notification)
