==============================================================================
Building this Project
TAME
(C) Matt Tropiano, All rights reserved.
http://github.com/MTrop/TAME
==============================================================================

This project is built via Apache Ant. You'll need to download Ant from here:
https://ant.apache.org/bindownload.cgi

The build script (build.xml) contains multiple targets of note, including:

dependencies
	Pulls dependencies into the dependency folders and adds them to 
	build.properties for "dev.base" and "lib.base".
clean 
	Cleans the build directory contents.
compile
	Compiles the Java source to classes.
javadoc
	Creates the Javadocs for this library.
jar
	JARs up the binaries, source, and docs into separate JARs.
zip
	ZIPs up the project contents into separate ZIP files (binaries, source, 
	and docs).
release
	Synonymous with "zip".

The build script also contains multiple properties of note, including:

exe.version
	The version written to the EXEs. Must be x.y.z.w
	Default: "0.0.0.0"
build.version.number
	Version number of the build. Written to the JARs.
	Default: Current time formatted as "yyyy.MM.dd.HHmmssSSS".
build.version.appendix
	Type of build (usually "BUILD" or "RELEASE" or "STABLE" or "SNAPSHOT").
	Default: "SNAPSHOT".
dev.base
	The base directory for other projects that need including for the build.
	Default: ".."
lib.base
	The base directory for other binaries of projects that need including for
	the build.
	Default: ".."
build.dir
	The base directory for built resources.
	Default: "build"
dependencies.base:
	The base directory for other binaries of projects that need including for 
	the build.
	Default: "deps"
launch4j.dir
	The base directory for Launch4J (for Win32 EXEs).
	Default: "${lib.base}/launch4j"
launch4j.lib.jar
	The Launch4J JAR filename.
	Default: "launch4j.jar"
launch4j.xstream.lib.jar
	The Launch4J XStream JAR path.
	Default: "lib/xstream.jar"
common.lib
	The location of the Black Rook Commons Library binaries (for build 
	classpath).
	Default: "${dev.base}/Common/bin"
common.io.lib
	The location of the Black Rook Common I/O binaries (for build 
	classpath).
	Default: "${dev.base}/CommonIO/bin"
common.lang.lib
	The location of the Black Rook Common Lang binaries (for build 
	classpath).
	Default: "${dev.base}/CommonLang/bin"
