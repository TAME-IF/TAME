# TAME (Text Adventure Module Engine)

Copyright (c) 2015-2018 Matt Tropiano. All rights reserved.  

### Required Libraries

Black Rook Commons 2.31.2+  
[https://github.com/BlackRookSoftware/Common](https://github.com/BlackRookSoftware/Common)

Black Rook Common I/O 2.5.1+  
[https://github.com/BlackRookSoftware/CommonIO](https://github.com/BlackRookSoftware/CommonIO)

Black Rook Common Lang 2.9.1+  
[https://github.com/BlackRookSoftware/CommonLang](https://github.com/BlackRookSoftware/CommonLang)

Launch4J 3.x+  
[http://launch4j.sourceforge.net/](http://launch4j.sourceforge.net/)  
[https://sourceforge.net/p/launch4j/git/ci/master/tree/](https://sourceforge.net/p/launch4j/git/ci/master/tree/)

### Required Java Modules

[java.base](https://docs.oracle.com/javase/10/docs/api/java.base-summary.html)

### Introduction

This library reads, compiles, and executes TAME modules.

**WARNING:** TAME is in an incomplete state. The commands in the language, its structure, 
and their behavior may change before version 1.0, but it is completely functional and consistent
in both the Java and JS implementations.

Until otherwise specified, any changes to the TAME commands set may invalidate TAME modules that are compiled
to the serialized format or JS modules (but not embedded ones - those are safe).

### Known Issues

* Serialization of game state that has circular references in its structure is not handled as gracefully as it should be.

### Library

Contained in this release is a series of libraries that allow reading, compiling,
writing, and executing TAME module files/archives, found in the **com.tameif.tame**
packages.

### Compiling with Ant

To download the dependencies for this project (if you didn't set that up yourself already), type:

	ant dependencies

A *build.properties* file will be created/appended to with the *dev.base, libs.base,* 
and *launch4j.dir* properties set.
	
To compile this project, type:

	ant compile

Shell scripts (CMD/Bash) will also be created in the project directory that will run the main
entry points of the compiled code, depending on your OS.
	
To make a JAR of just TAME's classes (no bundled dependencies), and the stand-alones 
(both main programs with entry points and necessary dependencies) type:

	ant jar

Jars will be placed in the *build/jar* directory by default.

Win32 EXEs can be made via **launch4j**. To make Win32 executables of both programs, type:

	ant win32

EXEs will be placed in the *build/win32* directory by default.
	
To make Win32 executables and deploy them to a directory of your choosing via 
etting the *win32.deploy.dir* property, type:

	ant win32.deploy

The EXEs will not be deployed if the *win32.deploy.dir* property is not set!

### Other Files

There is a Notepad++ Syntax Highlighter config in `docs/UDL.xml`.

The `docs/TAME.md` file is not only incomplete, but obsolete due to the construction
of the main documentation at [https://github.com/TAME-IF/TAMEDocs](https://github.com/TAME-IF/TAMEDocs).

The files in the `js` directory are executed via NodeJS.

### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 
