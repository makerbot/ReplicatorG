ReplicatorG is an open-source GCode based controller for RepRap / CNC machines.  It has 3 main goals:

1. Be as simple to use, and as easy to install as possible.
2. Be driver oriented and abstract the GCode away, allowing users to easily create drivers for their own machine.
3. Support as much of the GCode specification as possible.

For more information, see the website at: http://www.replicat.org

INSTALLATION

Windows: http://www.replicat.org/windows-installation
Mac OSX: http://www.replicat.org/mac-osx-installation
Linux:   http://www.replicat.org/linux-installation

CREDITS

ReplicatorG is an open source project, owned by nobody and supported by many.

The project is descended from the wonderful Arduino host software (http://www.arduino.cc)
Arduino is descended from the also wonderful Processing environment (http://www.processing.org)

ReplicatorG was forked from Arduino in August 2008.

Changes in 0006:

0006 ReplicatorG

* Removed editor status bar
* 64-bit Mac OS X fix (courtesy Andreas Fuchs)
* Pass a GCode file in as a command-line parameter (courtesy Andreas Fuchs)
* Moved machine status below buttons and cleaned up display
* Ensure abort signal is sent to S3G on stop
* Display temperature of nozzle during builds
* Add pref for temperature display
* Simulator2D optimizations, faster draws
* Reenabled Ctrl-J shortcut for control panel
* Use port names specified in XML by default, fall back to autoscan
  if not present
* Allow autoscan disabling for machines with scan problems
* Added support for writing onboard configuration data to the machine EEPROM
* Numerous small bug fixes

People who have worked on ReplicatorG include:

Zach 'Hoeken' Smith (http://www.zachhoeken.com)
Marius Kintel (http://reprap.soup.io)
Adam Mayer (http://makerbot.com)

