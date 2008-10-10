#!/bin/sh

PATH=lib:$PATH
CLASSPATH=\"java\\lib\\rt.jar\;lib\\vecmath.jar\;lib\\j3dcore.jar\;lib\\j3dutils.jar\;lib\\RXTXcomm.jar\;lib\;lib\\build\;lib\\pde.jar\;lib\\mrj.jar\;lib\\antlr.jar\;lib\\oro.jar\;lib\\registry.jar\"
export PATH
export CLASSPATH

cd work && java processing.app.Base
#cd work && /cygdrive/c/jdk-1.3.1_11/bin/java PdeBase
