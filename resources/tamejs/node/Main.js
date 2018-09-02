/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

//##[[EXPORTJS-INCLUDE Libs.js

var readline = require('readline');
var fs = require('fs');

var inspect = false;
var debug = false;
var trace = false;
var tracelist = null;

// Read commandline.
var args = process.argv;

for (let x in args) if (args.hasOwnProperty(x))
{
	let arg = args[x];
	
	if (arg == '--inspect')
	{
		trace = false;
		inspect = true;
	}
	else if (arg == '--debug')
	{
		trace = false;
		debug = true;
	}
	else if (arg == '--trace')
	{
		trace = true;
		tracelist = [];
	}
	else if (trace)
		tracelist.push(arg.toUpperCase());
}

if (tracelist !== null && tracelist.length === 0)
	tracelist = true;

// Create context.
var tamectx = TAME.newContext();

var rl = readline.createInterface ({
	input: process.stdin,
	output: process.stdout
});

var stop = false;
var pause = false;
var textBuffer = '';
var lastColumn = 0;

function startFormatTag(tag)
{
	// Nothing
}

function endFormatTag(tag)
{
	// Nothing
}

function formatText(text) 
{
	textBuffer += text;
}

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function debugCue(cue)
{
	println('['+cue.type+'] '+withEscChars(cue.content));
	let type = cue.type.toLowerCase();
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

/**
 * Handles a TAME cue.
 * @return true to continue handling, false to halt.
 */
function doCue(cue)
{
	var type = cue.type.toLowerCase();
	var content = cue.content;
	
	if (type !== 'text' && type !== 'textf')
	{
		lastColumn = printWrapped(textBuffer, lastColumn, process.stdout.columns);
		textBuffer = '';
	}
	
	switch (type)
	{
	
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

var currentResponseCue = 0;
var currentResponse = null;

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

function responseRead()
{
    var keepGoing = true;
    while (currentResponseCue < currentResponse.responseCues.length && keepGoing) 
    {
        var cue = currentResponse.responseCues[currentResponseCue++];
        if (debug)
        	keepGoing = debugCue(cue);
        else
        	keepGoing = doCue(cue);
    }

    return currentResponseCue < currentResponse.responseCues.length;
}

/**
 * Handles a new TAME response.
 * @param response the TAME response object.
 */
function startResponse(response) 
{
	currentResponseCue = 0;
	currentResponse = response;
	
	var handler = debug ? debugCue : doCue;
	println();
	if (debug)
	{
		println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
		println('Request time: '+(response.requestNanos/1000000.0)+' ms');
		println('Operations: '+response.operationsExecuted);
		println('Cues: '+response.responseCues.length);
	}
		
	responseStop(responseRead());
}

const COMMAND_SAVE = '!save';
const COMMAND_LOAD = '!load';
const COMMAND_QUIT = '!quit';

// Loop.
rl.on('line', function(line){
	line = line.trim();
	if (pause) {
		pause = false;
		responseStop(responseRead());
	} else {
		if (inspect && line.substring(0,1) == '?')
		{
			let split = line.substring(1).split(/\./);
			if (split.length < 1 || !split[0] || !split[0].trim())
			{
				println("?> Must specify a variable.");
			}
			else
			{
				let result = TAME.inspect(tamectx, split[0], split[1]);
				if (result === null)
				{
					println("?> Context \""+split[0]+"\" not found.");
				}
				else
				{
					for (let key in result) if (result.hasOwnProperty(key))
						println("?> "+key+" = "+result[key]);			
				}
			}
			rl.prompt();
		}
		else if (COMMAND_SAVE == line.substring(0, COMMAND_SAVE.length))
		{
			let name = line.substring(COMMAND_SAVE.length).trim();
			try {
				fs.writeFileSync(name+'.json', tamectx.stateSave(), {"encoding": 'utf8'});
				println("State saved: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else if (COMMAND_LOAD == line.substring(0, COMMAND_LOAD.length))
		{
			let name = line.substring(COMMAND_LOAD.length).trim();
			try {
				var stateData = fs.readFileSync(name+'.json', {"encoding": 'utf8'});
				tamectx.stateRestore(stateData);
				println("State loaded: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else if (COMMAND_QUIT == line.substring(0, COMMAND_QUIT.length))
		{
			stop = true;
			rl.close();
		}
		else
			startResponse(TAME.interpret(tamectx, line.trim(), tracelist));
	}
}).on('close', function(){
	process.exit(0);
});

//Initialize.
startResponse(TAME.initialize(tamectx, tracelist));

// start loop.
if (!stop)
	rl.prompt();

//##[[EXPORTJS-END

