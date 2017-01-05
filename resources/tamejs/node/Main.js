/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

const readline = require('readline');

var debug = false;
var trace = false;

// Read commandline.
var args = process.argv;

for (var x in argv) if (argv.hasOwnProperty(x))
{
	var arg = argv[x];
	
	if (arg == '--debug')
		debug = true;
	else if (arg == '--trace')
		trace = true;
}

// Create context.
var tamectx = TAME.newContext();

const rl = readline.createInterface({
	input: process.stdin,
	output: process.stdout,
	prompt: '> '
});

var stop = false;

/**
 * Handles a TAME response.
 * @param response the TAME response object.
 */
function handleResponse(response) 
{
	
}

// Initialize.
handleResponse(TAME.initialize(tamectx, trace));

// Loop.
rl.on('line', function(line){
	handleResponse(TAME.interpret(tamectx, line.trim(), trace));
	if (stop)
		rl.close();
	else
		rl.prompt();
}).on('close', function(){
	process.exit(0);
});

// start loop.
if (!stop)
	rl.prompt();

