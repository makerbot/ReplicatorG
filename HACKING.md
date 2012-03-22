# Requirements

dbus-java 2.7

# Ubuntu

> sudo apt-get install libdbus-java

# Windows

The dbus-java jars are now included in the ReplicatorG repository.
A windows installer for dbus exists, though we haven't tested it much. It can be found at http://code.google.com/p/dbus-windows-installer/

# Running a Printer Process

> ant run -Drun.arguments="printer --bus-name com.makerbot.Printer.printer0 --machine-name 'The Replicator Dual' --port /dev/ttyACM0"

The --bus-name, --machine-name, and --port arguments are optional.
If --bus-name is specified the printer will register with D-Bus with the given name.
Otherwise it D-Bus will assign the printer a unique name (which is not very useful).
If --machine-name or --port options are omitted the printer will use the machine name and serial port stored in the ReplicatorG preferences.

The ReplicatorG '--alternate-prefs' mechanism is supported.
If specified it must occur *before* the 'printer' command on the command-line:

> ant run -Drun.arguments="--alternate-prefs xyzzy printer ..."

# Invoking a Job by D-Bus

> ant run -Drun.arguments="build --bus-name com.makerbot.Printer.printer0 /home/you/ReplicatorG/examples/20mm_Calibration_Box.gcode"

# Invoking a Job by a Third-Party D-Bus Client

Use d-feet.

> sudo apt-get install d-feet

1. Run d-feet.
2. Connect to the session bus.
3. Pick "com.makerbot.Printer.printer0" (or whatever --bus-name you specified).
4. Expand the com.makerbot.Printer interface.
5. Execute the 'Build' method. It takes a single argument which should by a Python literal string containing a full path to a .gcode file (i.e., '/home/you/ReplicatorG/examples/20mm_Calibration_Box.gcode')
6. The method will not return any results, but the printer process will start a build.

You can accomplish the same thing with dbus-send, but it's not user-friendly.

# Session Bus?

The printer currently associates with the session bus.
This is temporary.
