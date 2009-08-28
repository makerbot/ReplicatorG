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

Changes in 0007:

0007 ReplicatorG

* Fix for issue 15 (credit to Charles Pax)
* Adding constants for SD card write/playback commands
* Basic testing support for SD card builds
* Adding card capture api
* Adding pausability to remote builds
* Switching out gif for png with transparency
* Refactor/prep for SD uploading
* Button bar revision, interface prep for SD card prints
* Fixed up startup window positioning
* Removed preferences store/load race condition that was screwing up window sizing
* moved some shutdown code out of handleQuit2 into proper shutdown hook
* hide drivers marked 'experimental'
* added preference for viewing experimental machine profiles
* busy cursor on long loads
* reduced load time for files in large directory
* proper error messages for SD operations
* added feature check to buttons (only display for 1.3)
* removing skeinforge from 0007, now distributed seperately

People who have worked on ReplicatorG include:

Zach 'Hoeken' Smith (http://www.zachhoeken.com)
Marius Kintel (http://reprap.soup.io)
Adam Mayer (http://makerbot.com)

