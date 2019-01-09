/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// TODO: Add a version of this that loads arbitrary module binaries.

//[[EXPORTJS-START

//[[EXPORTJS-INCLUDE Libs.js

const readline = require('readline');
const fs = require('fs');
const os = require('os');
const rl = readline.createInterface ({
	input: process.stdin,
	output: process.stdout
});

const COMMAND_SAVE = '!save';
const COMMAND_LOAD = '!load';
const COMMAND_QUIT = '!quit';

function printVersion()
{
	println("TAME NodeJS Console Shell v"+TAME.version+" by Matt Tropiano");
	println("Running on: "+os.type()+" "+os.arch()+", NodeJS v"+process.versions.node);
	
	if (EmbeddedData && (EmbeddedData.header.title || EmbeddedData.header.author))
	{
		print("Embedded module is");
		if (EmbeddedData.header.title)
			print(" \"" + EmbeddedData.header.title + "\"");
		if (EmbeddedData.header.author)
			print(" by " + EmbeddedData.header.author);
		println();
	}
}

function printSplash(filename)
{
	printVersion();
	println("Type `node \""+filename+"\" --help` for help.");
}

function printHelp(filename, requirefile)
{
	printVersion();
	print("Usage: node \"");
	print(filename);
	print("\"");
	if (requirefile)
		print(" [modulefile]");
	print(" [switches]");
	println();
	
	println();
	if (requirefile)
	{
		println("[modulefile]:");
		println("     The compiled TAME module file.");
		println();
	}
	println("[switches]:");
	println();
	println("    --debug                  Show received cues.");
	println();
	println("    --inspect                Enable the Inspector.");
	println();
	println("    --trace [type ...]       If debug, also show trace cues (no types = all).");
	println();
	println("                             interpreter - trace INTERPRETER scanning.");
	println("                             context     - trace CONTEXT changes.");
	println("                             entry       - trace ENTRY point calls.");
	println("                             control     - trace CONTROL statement flow.");
	println("                             function    - trace FUNCTION calls.");
	println("                             internal    - trace INTERNAL function calls.");
	println("                             value       - trace VALUE set/clear, local or");
	println("                                           context.");
	println();
	println("    While running this, there a few more commands at your disposal:");
	println();
	println("    !save [filename]         Save this module's state.");
	println("    !load [filename]         Loads this module's state.");
	println("    !quit                    Quits the module.");
	println();
	println("About the Inspector:");
	println("    The inspector is a debugging tool that is useful for querying the values");
	println("    of various variables and contexts. While the module is running, you may");
	println("    type '?' followed by a non-archetype element identifier to query its");
	println("    current value.");
	println();
	println("  Examples:");
	println("    ?p_mainplayer         Get all persisted values on element 'p_mainplayer'.");
	println("    ?p_mainplayer.steps   Get value of 'steps' on element 'p_mainplayer'.");
	println("    ?world                Get all persisted values on element 'world'.");
}

// Main
(function(){
	
	let inspect = false;
	let debug = false;
	let trace = false;
	let tracelist = null;
	let filename = null;

	// Read commandline.
	let args = process.argv;

	for (let x in args) if (args.hasOwnProperty(x))
	{
		// skip first two.
		if (x <= 1)
			continue;
		
		let arg = args[x];
		
		if (arg == '--help' || arg == '-h' || arg == '/?')
		{
			trace = false;
			printHelp(args[1], EmbeddedData ? false : true);
			process.exit(0);
		}
		else if (arg == '--inspect')
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
		else
			filename = arg;
	}

	if (tracelist !== null && tracelist.length === 0)
		tracelist = true;

	let tamectx = null;
	let stop = false;
	let pause = false;
	let lastColumn = 0;

	function startFormatTag(tag)
	{
		// Nothing
	}

	function endFormatTag(tag)
	{
		// Nothing
	}

	function formatText(text, accum) 
	{
		accum.push(text);
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

	let TEXTBUFFER = [];

	/**
	 * Handles a TAME cue.
	 * @return true to continue handling, false to halt.
	 */
	function doCue(cue)
	{
		let type = cue.type.toLowerCase();
		let content = cue.content;
		
		if (type !== 'text' && type !== 'textf')
		{
			lastColumn = printWrapped(TEXTBUFFER.join(''), lastColumn, process.stdout.columns);
			TEXTBUFFER.length = 0;
		}
		
		switch (type)
		{
			case 'quit':
				stop = true;
				return false;
			
			case 'text':
				TEXTBUFFER.push(content);
				return true;
			
			case 'textf':
				TEXTBUFFER.push(TAME.parseFormatted(content, startFormatTag, endFormatTag, formatText));
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

	let currentResponseCue = 0;
	let currentResponse = null;

	function responseStop(notDone)
	{
		if (TEXTBUFFER.length > 0)
		{
			printWrapped(TEXTBUFFER.join(''), lastColumn, process.stdout.columns);
			lastColumn = 0;
			TEXTBUFFER.length = 0;
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
	    let keepGoing = true;
	    while (currentResponseCue < currentResponse.responseCues.length && keepGoing) 
	    {
	        let cue = currentResponse.responseCues[currentResponseCue++];
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
		
		let handler = debug ? debugCue : doCue;
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
					let stateData = fs.readFileSync(name+'.json', {"encoding": 'utf8'});
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

	if (EmbeddedData)
	{
		tamectx = null;
		try {
			let module = TAME.readModule(base64ToDataView(EmbeddedData.data));
			tamectx = TAME.newContext(module);
		} catch (Err) {
			println("ERROR: "+Err.toString());
			process.exit(2);
		}
	}
	else if (filename)
	{
		try {
			let buffer = fs.readFileSync(filename);
			let out = new DataView(new ArrayBuffer(buffer.length));
			let i = 0;
			for (i = 0; i < buffer.length; i++)
				out.setUint8(i, buffer.readUInt8(i));
			let module = TAME.readModule(out);
			tamectx = TAME.newContext(module);
		} catch (Err) {
			println("ERROR: "+Err.toString());
			process.exit(1);
		}
	}
	else
	{
		printSplash(args[1]);
		process.exit(0);
	}
	
	//Initialize.
	startResponse(TAME.initialize(tamectx, tracelist));

	// start loop.
	if (!stop)
		rl.prompt();
	
})();

//[[EXPORTJS-END

