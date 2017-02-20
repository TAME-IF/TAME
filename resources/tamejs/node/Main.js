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
const fs = require('fs');

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
	output: process.stdout
});

var stop = false;
var pause = false;
var textBuffer = '';
var lastColumn = 0;

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
function debugCue(type, content) {

	type = type.toLowerCase();
	println('['+type+'] '+withEscChars(content));
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function doCue(type, content) {
	
	type = type.toLowerCase();
	
	if (type !== 'text' && type !== 'textf')
	{
		lastColumn = printWrapped(textBuffer, lastColumn, process.stdout.columns);
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
			TAME.parseFormatted(content, startFormatTag, endFormatTag, formatText);
			return true;
			
		case 'wait':
			sleep(parseInt(content, 10));
			return true;

		case 'pause':
			pause = true;
			lastColumn = 0;
			return false;

		case 'trace':
			// Ignore trace.
			return true;

		case 'tip':	
			println('(TIP: '+content+')');
			lastColumn = 0;
			return true;

		case 'info':	
			println('INFO: '+content);
			lastColumn = 0;
			return true;

		case 'error':	
			println('\n!ERROR! '+content);
			lastColumn = 0;
			return true;

		case 'fatal':
			println('\n!!FATAL!! '+content);
			lastColumn = 0;
			stop = true;
			return false;
	}
	
}

var handler = TAME.createResponseHandler({}, debug ? debugCue : doCue);

function responseStop(notDone)
{
	if (textBuffer.length > 0)
	{
		printWrapped(textBuffer, lastColumn, process.stdout.columns);
		lastColumn = 0;
		textBuffer = '';
	}

	if (stop)
		rl.close();
	else if (pause) 
	{
		rl.setPrompt('(CONTINUE) ');
		rl.prompt();
	} 
	else if (!notDone) 
	{
		if (!stop) 
		{
			rl.setPrompt('] ');
			println();
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
function startResponse(response) 
{
	if (debug) {
		println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
		println('Request time: '+(response.requestNanos/1000000.0)+' ms');
		println('Commands: '+response.commandsExecuted);
		println('Cues: '+response.responseCues.length);
	}
	println();
	responseStop(handler.handleResponse(response));
}

const COMMAND_SAVE = '!save';
const COMMAND_LOAD = '!load';

// Loop.
rl.on('line', function(line){
	line = line.trim();
	if (pause) {
		pause = false;
		responseStop(handler.resume());
	} else {
		if (COMMAND_SAVE == line.substring(0, COMMAND_SAVE.length))
		{
			var name = line.substring(COMMAND_SAVE.length).trim();
			try {
				fs.writeFileSync(name+'.json', JSON.stringify(tamectx.state), {"encoding": 'utf8'});
				println("State saved: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else if (COMMAND_LOAD == line.substring(0, COMMAND_LOAD.length))
		{
			var name = line.substring(COMMAND_LOAD.length).trim();
			try {
				var stateData = fs.readFileSync(name+'.json', {"encoding": 'utf8'});
				tamectx.state = JSON.parse(stateData);
				println("State loaded: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else
			startResponse(TAME.interpret(tamectx, line.trim(), trace));
	}
}).on('close', function(){
	process.exit(0);
});

//Initialize.
startResponse(TAME.initialize(tamectx, trace));

// start loop.
if (!stop)
	rl.prompt();

//##[[EXPORTJS-END

