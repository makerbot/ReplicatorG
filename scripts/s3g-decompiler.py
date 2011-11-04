#!/usr/bin/python
import struct
import sys

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
