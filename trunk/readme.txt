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

UPDATES

0001 - 08/??/2008

* The first release of the IDE.  It will run your GCode files.  It's ugly, but it works.
* convert from Arduino to ReplicatorG
* get new app to build
* convert .pde to .gcode
* get GUI working reliably
* create gcode running thread
* create gcode simulation thread
* create gcode simulation driver
* get text-highlighting up and running
* get machine driver-loading based on XML
* update RunButtonWatcher to work with our running and/or simulating thread (obsolete)
* move percentage / line count to bottom of window.
* remove HandleNewLibrary
* change 'Run' to 'Build'
* get help menu up and running properly (send everyone to website)
* add our ReplicatorG header to every file
* implement support for various gcode pauses / prompts / etc.
* change message dialog to yes/no dialog that allows you to cancel the operation
* test build process on linux
* test serialpassthrough driver
* add simple simulation window
* reformat GUI with different button ordering
* create and add 'pause' button
* update 'build' button graphic to be rotated 90 deg.
* add support for rest of low-hanging m codes
* finish implementation of ToolDrivers
* add a processing driver to calculate build time, look for errors, etc.
* finish gcode parsing for proper coordinates / machine status / etc.
* change colors to RepRap green (thanks nick bilton!)
* add elapsed time to build process
* add confirmation alert after print finishes
* add dispose after print finish
* update serialpassthroughdriver to pull all config from XML file
* update nulldriver to pull more config from XML file
* tweak play / pause button behavior
* get multi-print working solidly
* fix random freezing bug (it seems to be working)
* get inch support working
* add support for rest of low-hanging g codes
* make windows icons

