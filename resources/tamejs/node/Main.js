/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

const readline = require('readline');

var debug = false;
var trace = false;

// Read commandline.
var args = process.argv;

for (var x in args) if (args.hasOwnProperty(x))
{
	var arg = args[x];
	
	if (arg == '--debug')
		debug = true;
	else if (arg == '--trace')
		trace = true;
}

// Create context.
var tamectx = TAME.newContext();

const rl = readline.createInterface
({
	input: process.stdin,
	output: process.stdout,
	prompt: '] '
});

var stop = false;
var wait = null;
var currentResponse = null;
var currentCue = 0;

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function debugCue(cue)
{
	var content = cue.content;
	var type = cue.type.toLowerCase();
	
	console.log('['+cue.type+'] '+content);
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

var handleCueFunc = debug ? debugCue : debugCue;

/**
 * Resumes handling a TAME response.
 * @param response the TAME response object.
 */
function resumeResponse() 
{
	responseLoop();
}

function responseLoop()
{
	while (currentCue < currentResponse.responseCues.length && handleCueFunc(currentResponse.responseCues[currentCue++]))
		/* Do nothing. */;
	
	if (wait)
		setTimeout(resumeResponse, wait);
	else if (currentCue == currentResponse.responseCues.length)
	{
		currentResponse = null;
		currentCue = 0;
		if (!stop)
		{
			rl.setPrompt('] ');
			rl.prompt();
		}
		else
			rl.close();
	}
}

/**
 * Handles a new TAME response.
 * @param response the TAME response object.
 */
function handleResponse(response) 
{
	if (debug)
	{
		console.log('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
		console.log('Request time: '+(response.requestNanos/1000000.0)+' ms');
		console.log('Commands: '+response.commandsExecuted);
		console.log('Cues: '+response.responseCues.length);
	}
	currentResponse = response;
	currentCue = 0;
	responseLoop();
}


// Initialize.
handleResponse(TAME.initialize(tamectx, trace));

// Loop.
rl.on('line', function(line){
	handleResponse(TAME.interpret(tamectx, line.trim(), trace));
	if (stop)
		rl.close();
}).on('close', function(){
	process.exit(0);
});

// start loop.
if (!stop)
	rl.prompt();

//##[[EXPORTJS-END

