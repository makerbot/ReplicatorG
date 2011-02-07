#!/usr/bin/env python

import sys
import getopt
import os

def processCSV(filename, overrides):
    csvlines = [line for line in open(filename, "r")]
    try:
        csvfile = open(filename, 'w')
    except IOError:
        print("The file " + filename + " cannot be opened for writing.")
        return False

    for line in csvlines:
        splitline = line.split('\t', 1)
        if len(splitline) == 2:
            if (splitline[0] in overrides): 
                splitline[1] = overrides[splitline[0]] + "\n"
                del overrides[splitline[0]]
            line = '\t'.join(splitline)
        csvfile.write(line)
    for newvalue in overrides:
        csvfile.write(newvalue + "\t" + overrides[newvalue] + "\n")
    csvfile.close()

def usage():
    print >> sys.stderr, "Usage: " + sys.argv[0] + " -p <prefsdir> -o <overridefile>"
    print >> sys.stderr, "Options:"
    print >> sys.stderr, "   -p, --prefsdir=<dir>    Modify the given profile"
    print >> sys.stderr, "   -o, --overrides=<file>  Use these overrides"

if __name__ == "__main__":

    # Handle command-line arguments
    try:
        opts, args = getopt.getopt(sys.argv[1:], 
                                   "p:o:", ["prefsdir=", "overrides="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    prefsdir = None
    overrides = None
    for opt, arg in opts:
        if opt in ("-p", "--prefsdir"): 
            prefsdir = arg
        if opt in ("-o", "--overrides"): 
            overrides = arg

    if len(args) != 0:
        usage()
        sys.exit(2)

    if not prefsdir: 
        print >> sys.stderr, "Error: No prefsdir given"
        usage()
        sys.exit(2)

    if not overrides: 
        print >> sys.stderr, "Error: No overrides given"
        usage()
        sys.exit(2)

    # Recursively scan prefsdir for all csv files
    csvfiles = {}
    for root, _, files in os.walk(prefsdir):
        for filename in files:
            csvfiles[filename] = os.path.join(root, filename)


    # Build dict from overrides
    try:
        overridelines = [line.rstrip() for line in open(overrides, "r")]
    except IOError:
        print "Unable to open file " + overrides
        sys.exit(1)

    overridedict = {} # file.csv -> {name: value}
    for override in overridelines:
        splitline = override.split(":", 1)
        if len(splitline) == 2:
            (moduleName,override) = splitline
            splitoverride = override.split("=",1)
            if len(splitoverride) == 2:
                if not moduleName in overridedict: overridedict[moduleName] = {}
                overridedict[moduleName][splitoverride[0]] = splitoverride[1]

#    print "Updating",
    for csvfile in overridedict:
        if not csvfile in csvfiles:
            print "CSV file not found: " + csvfile
            break
#        print csvfile,
        processCSV(csvfiles[csvfile], overridedict[csvfile])
#    print "\n"

    sys.exit(0)
