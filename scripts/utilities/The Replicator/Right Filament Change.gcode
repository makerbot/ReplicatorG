(**** The Replicator Utility ****)
(* Change the Filament in Toolhead T0, Right Toolhead *)

M01  (Time to change the filament in the RIGHT extruder.  Press OK when you have removed the black feeding hose so we can begin (to remove, just push down on the grey ring and pull up on the black hose).)
M01  (I am going to start heating up the extruder head so we can remove the filament currently in the extruder!  Press OK to start heating.)
M70 P5 (Heating for            filament release)
G21 (set units to mm)
G90 (set positioning to absolute)
M108 R3.0 T0 (set extruder speed left)
M103 T0 (Make sure extruder is off)

M73 P0 (enable build progress 'Change Filament')

M104 S225 T0 (set extruder temperature)

M6 T0 (wait for toolhead parts, nozzle, HBP, etc., to reach temperature)


M70 P90(  Ejecting Filament )
M01 (Heating Complete!  I am now pushing the filament OUT of the extruder, please grab it as it comes out.  Press OK to start.)
M102 T0 (Extruder on, reverse)
G04 P90000 (Wait t/1000 seconds)
M01 (Time to load the new filament!  Start pushing the filament into the extruder so my motor catches it and begins to push it through.  Press OK to start.)
M101 T0 (Extruder on, forward)
M70 P120(  Loading Filament )
G04 P120000 (Wait t/1000 seconds)

M70 P5 (   Loading Script         Complete )

M103 T0 (Extruder off)
M104 S0 T0 (set extruder temperature)
M01 (And you're done!  If you didn't manage to get any filament into the extruder, feel free to run this script again!  Also, please don't forget to reattach the black feeing hose!)
M73 P100 (build end notification)
