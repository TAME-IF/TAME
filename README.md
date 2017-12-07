# TAME (Text Adventure Module Engine)

Copyright (c) 2015-2017 Matt Tropiano. All rights reserved.  

### Required Libraries

Black Rook Commons 2.31.0+  
[https://github.com/BlackRookSoftware/Common](https://github.com/BlackRookSoftware/Common)

Black Rook Common I/O 2.5.0+  
[https://github.com/BlackRookSoftware/CommonIO](https://github.com/BlackRookSoftware/CommonIO)

Black Rook Common Lang 2.9.1+  
[https://github.com/BlackRookSoftware/CommonLang](https://github.com/BlackRookSoftware/CommonLang)

### Introduction

This library reads, compiles, and executes TAME modules.

### Library

Contained in this release is a series of libraries that allow reading, compiling,
writing, and executing TAME module files/archives, found in the **net.mtrop.tame**
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
	
To make a JAR of just TAME's classes (no bundled dependencies), and the stand-alones (both main programs
with entry points and necessary dependencies) type:

	ant jar

Jars will be placed in the *build/jar* directory by default.

Win32 EXEs can be made via **launch4j**. To make Win32 executables of both programs, type:

	ant win32

EXEs will be placed in the *build/win32* directory by default.
	
To make Win32 executables and deploy them to a directory of your choosing via 
etting the *win32.deploy.dir* property, type:

	ant win32.deploy

The EXEs will not be deployed if the *win32.deploy.dir* property is not set!

### Quick Language Rundown

***TODO***

### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 
