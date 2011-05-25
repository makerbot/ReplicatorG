(***  Thing-O-Matic calibration script  ***)
(***                                    ***)
(*** This script will guide you through ***)
(*** calibrating the start position     ***)
(*** on your Thing-O-Matic.             ***)

M18 (This disables the stepper motors.)
M01 (Move the build platform until the nozzle lies in the center, then turn the threaded rod until the nozzle just touches the surface without pressing into it. Then, press yes to continue.)
G92 X0 Y0 Z0 A0 B0 (Declare the current position to be (0,0,0,0,0))
G162 Z F500 (Home Z axis maximum; go until reaching the end stop.)
G161 X Y F2500 (Home X and Y axis minimum; go until reaching the end stop.)
M131 X Y Z A B (record the current coordinates to the motherboard)

M00 (Congratulations, your coordinates are now saved! To tweak them, use the 'Motherboard Onboard Preferences' dialog in the Machine menu. <br/>Note: You will need to re-generate your gcode files using a new profile in order to use these saved settings.)
