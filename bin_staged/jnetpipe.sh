#!/bin/sh
cd `dirname $0`
if [[ $OSTYPE == 'cygwin' ]]; then sep=';'
else sep=':'
fi

# Build a classpath with our jarfiles, using the platform-specific path sep
jarcp="./bsh-2.0b4.jar$sep./jnetpipe.jar$sep./log4j-1.2.9.jar"
java -cp "$jarcp" com.solacesystems.testtool.jnetpipe.JNetPipe $@

