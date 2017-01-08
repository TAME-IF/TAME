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

//##[[EXPORTJS-INCLUDE Libs.js

const readline = require('readline');

function print(text)
{
	if (text)
		process.stdout.write(text);
}

function println(text)
{
	if (!text)
		process.stdout.write('\n');
	else
		process.stdout.write(text + '\n');
}

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

const rl = readline.createInterface ({
	input: process.stdin,
	output: process.stdout,
	prompt: '] '
});

var stop = false;
var pause = false;
var currentResponse = null;
var currentCue = 0;
var textBuffer = '';

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function debugCue(cue) {
	var content = cue.content;
	var type = cue.type.toLowerCase();
	
	println('['+cue.type+'] '+content);
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}


function startFormatTag(tag) {
	// Nothing
}

function endFormatTag(tag) {
	// Nothing
}

function formatText(text) {
	textBuffer += text;
}

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function doCue(cue) {
	
	var content = cue.content;
	var type = cue.type.toLowerCase();
	
	if (type !== 'text' && type !== 'textf') {
		print(textBuffer);
		textBuffer = '';
	}
	
	switch (type) {
	
		case 'quit':
			stop = true;
			return false;
		
		case 'text':
			textBuffer += content;
			return true;
		
		case 'textf':
			(new FormatParser(startFormatTag, endFormatTag, formatText)).parse(content);
			return true;
			
		case 'wait':
			sleep(parseInt(content, 10));
			return true;

		case 'pause':
			pause = true;
			return false;

		case 'trace':
			// Ignore trace.
			return true;

		case 'tip':	
			println('(TIP: '+content+')');
			return true;

		case 'info':	
			println('INFO: '+content);
			return true;

		case 'error':	
			println('\n!ERROR! '+content);
			return true;

		case 'fatal':
			println('\n!!FATAL!! '+content);
			stop = true;
			return false;
	}
	
}

var handleCueFunc = debug ? debugCue : doCue;

function responseLoop() {
	
	while (currentCue < currentResponse.responseCues.length && handleCueFunc(currentResponse.responseCues[currentCue++])) {
		/* Do nothing. */
	}

	if (textBuffer.length > 0)
	{
		print(textBuffer);
		textBuffer = '';
	}

	if (stop)
		rl.close();
	else if (pause) {
		rl.setPrompt('(Enter to CONTINUE)');
		rl.prompt();
	} else if (currentCue == currentResponse.responseCues.length) {
		currentResponse = null;
		currentCue = 0;
		if (!stop) {
			rl.setPrompt('] ');
			println();
			rl.prompt();
		} else
			rl.close();
	}
}

/**
 * Handles a new TAME response.
 * @param response the TAME response object.
 */
function handleResponse(response) 
{
	if (debug) {
		println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
		println('Request time: '+(response.requestNanos/1000000.0)+' ms');
		println('Commands: '+response.commandsExecuted);
		println('Cues: '+response.responseCues.length);
	}
	println();
	currentResponse = response;
	currentCue = 0;
	responseLoop();
}

// Loop.
rl.on('line', function(line){
	if (pause) {
		pause = false;
		responseLoop();
	} else
		handleResponse(TAME.interpret(tamectx, line.trim(), trace));
}).on('close', function(){
	process.exit(0);
});

//Initialize.
handleResponse(TAME.initialize(tamectx, trace));

// start loop.
if (!stop)
	rl.prompt();

//##[[EXPORTJS-END

