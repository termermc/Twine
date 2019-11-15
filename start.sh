#!/bin/sh

# Starts Twine with `dependencies/` included in the classpath.
# Starting without the `--classpath-loaded` flag would create a new process with these options.
java -classpath 'twine-1.2-alpha.jar:dependencies/*:modules/*' net.termer.twine.Twine -s --classpath-loaded