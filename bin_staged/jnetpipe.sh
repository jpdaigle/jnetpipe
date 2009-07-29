#!/bin/sh
cd `dirname $0`
java -cp "./bsh-2.0b4.jar;./jnetpipe.jar;./log4j-1.2.9.jar" com.solacesystems.testtool.jnetpipe.JNetPipe $@

