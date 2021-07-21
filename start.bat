# Starts Twine with `dependencies/` and `modules/` included in the classpath.
# Starting without the `--classpath-loaded` flag would create a new process with these options.
java -classpath "twine-2.1.jar:dependencies/*:modules/*" net.termer.twine.Twine -s --classpath-loaded %*