#!/usr/bin/python
import struct
import sys
import pprint
pp = pprint.PrettyPrinter(indent=4)

toolCommandTable = {
    3: ("<H", "Set target temperature to %i"),
    4: ("B", "Set motor 1 speed (pwm): %i"),
    10: ("B", "Toggle motor 1: %d"),
    12: ("B", "Turn fan on (1) or off (0): %d"),
    27: ("B", "Toggle ABP: %d"),
    31: ("<H", "Set build platform target temperature to %i"),
    129: ("<iiiI","Absolute move to (%i,%i,%i) at DDA %i"),
}

def parseToolAction():
    global s3gFile
    packetStr = s3gFile.read(3)
    if len(packetStr) != 3:
        raise "Incomplete s3g file during tool command parse"
    (index,command,payload) = struct.unpack("<BBB",packetStr)
    contents = s3gFile.read(payload)
    if len(contents) != payload:
        raise "Incomplete s3g file: tool packet truncated"
    return (index,command,contents)

def printToolAction(tuple):
    print "Tool %i: " % (tuple[0]),
    # command - tuple[1]
    # data - tuple[2]
    (parse, disp) = toolCommandTable[tuple[1]]
    if type(parse) == type(""):
        packetLen = struct.calcsize(parse)
        if len(tuple[2]) != packetLen:
            raise "Packet incomplete"
        parsed = struct.unpack(parse,tuple[2])
    else:
        parsed = parse()
    if type(disp) == type(""):
        print disp % parsed

def parseBuildStart():
    global s3gFile
    packetStr = s3gFile.read(4)
    if len(packetStr) != 4:
      raise "Incomplete s3g file during tool parse build start"
    (stepCount,) = struct.unpack("<I",packetStr)
    buildName = ""
    while (1):
      nextChar = s3gFile.read(1)
      if nextChar == "\0":
        break
      buildName += nextChar
    return (stepCount, buildName)

def printBuildStart(tuple):
  # pp.pprint(tuple)
  print "Build start (%i): %s" % (0, tuple[1])

def parseMessage():
    global s3gFile
    packetStr = s3gFile.read(4)
    if len(packetStr) != 4:
      raise "Incomplete s3g file during tool parse build start"
    (options,x,y,seconds) = struct.unpack("<bbbb",packetStr)
    message = ""
    while (1):
      nextChar = s3gFile.read(1)
      if nextChar == "\0":
        break
      message += nextChar
    return (options,x,y,seconds, message)

def printMessage(tuple):
  # pp.pprint(tuple)
  print "Display message (options %d, x: %d y: %d for %d seconds):\n  %s" % (tuple[0], tuple[1], tuple[2], tuple[3], tuple[4])

# Command table entries consist of:
# * The key: the integer command code
# * A tuple:
#   * idx 0: the python struct description of the rest of the data,
#            of a function that unpacks the remaining data from the
#            stream
#   * idx 1: either a format string that will take the tuple of unpacked
#            data, or a function that takes the tuple as input and returns
#            a string
# REMINDER: all values are little-endian. Struct strings with multibyte
# types should begin with "<".
# For a refresher on Python struct syntax, see here:
# http://docs.python.org/library/struct.html
commandTable = {    
    129: ("<iiiI","Absolute move to (%i,%i,%i) at DDA %i"),
    130: ("<iii","Machine position set as (%i,%i,%i)"),
    131: ("<BIH","Home minimum on %X, feedrate %i, timeout %i s"),
    132: ("<BIH","Home maximum on %X, feedrate %i, timeout %i s"),
    133: ("<I","Delay of %i us"),
    134: ("<B","Switch to tool %i"),
    135: ("<BHH","Wait for tool %i (%i ms between polls, %i s timeout"),
    136: (parseToolAction, printToolAction),
    137: ("<B", "Enable/disable axes %X"),
    138: ("<H", "User block on ID %i"),
    139: ("<iiiiiI","Absolute move to (%i,%i,%i,%i,%i) at DDA %i"),
    140: ("<iiiii","Extended Machine position set as (%i,%i,%i,%i,%i)"),
    141: ("<BHH","Wait for platform %i (%i ms between polls, %i s timeout)"),
    142: ("<iiiiiIB","Move to (%i,%i,%i,%i,%i) in %i us (relative: %X)"),
    143: ("<b","Store home position for axes %d"),
    144: ("<b","Recall home position for axes %d"),
    145: ("<bb","Set stepper #%ddigital pot value to %d"),
    146: ("<bbbb","Set LED strip color to R:%d G:%d B:%d Blink rate:%d Reserved:%d"),
    147: ("<HHb","Beep at %iHz for %i ms with Effect ID:%d"),
    148: ("<bHb","Pause for button with mask %X, timout in %i seconds, with options %d"),
    149: (parseMessage,printMessage),
    150: ("<bb","Set build to %d%% (%d)"),
    151: ("<b","Queue song id %d"),
    153: (parseBuildStart,printBuildStart),
    154: ("<b","End build (%d)"),
}

def parseNextCommand():
    """Parse and handle the next command.  Returns
    True for success, False on EOF, and raises an
    exception on corrupt data."""
    global s3gFile
    commandStr = s3gFile.read(1)
    if len(commandStr) == 0:
        print "EOF"
        return False
    (command) = struct.unpack("B",commandStr)
    # print command[0]
    (parse, disp) = commandTable[command[0]]
    if type(parse) == type(""):
        packetLen = struct.calcsize(parse)
        packetData = s3gFile.read(packetLen)
        if len(packetData) != packetLen:
            raise "Packet incomplete"
        parsed = struct.unpack(parse,packetData)
    else:
        parsed = parse()
    if type(disp) == type(""):
        print disp % parsed
    else:
        disp(parsed)
    return True

s3gFile = open(sys.argv[1],'rb')
while parseNextCommand():
    pass

