/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

/**
 * Creates a new response handler (mostly for aiding in browser functions).
 * Handles all standard cues and provides a way via a function to handle other cues.
 * This accumulates the results of contiguous "text" and "textf" cues before "print()" is called.
 * @param TAME reference to TAME engine.
 * @param options options object.
 * 		print: fn(text): called when a string needs printing (may contain HTML or other things).
 * 		onStart: fn(): called before cues start processing.
 * 			Should disable input.
 * 		onEnd: fn(): called when cues stop processing, and the end is reached.
 * 			Should enable input.
 * 		onSuspend: fn(): called when a call to process a cue initiates a suspension (processing stops, but not due to a pause).
 * 			Should disable input.
 * 		onResume: fn(): called when cues process again.
 * 			Should disable input.
 * 		onPause: fn(): called after a "pause" cue is processed. 
 * 			Should prompt for continuation somehow, then call resume() after the user "continues."
 * 		onQuit: fn(): called after a "quit" cue is processed.
 * 			Should stop input and prevent further input. 
 * 		onError: fn(message): called after an "error" cue is processed.
 * 			Should make an error message appear on screen. Dismissable. 
 * 		onFatal: fn(message): called after a "fatal" cue is processed.
 * 			Should make a message appear on screen, and stop input as though a "quit" occurred. 
 * 		onOtherCue: fn(cueType, cueContent): called when a cue that is not handled by this handler needs processing. 
 * 			Should return boolean. true = keep going, false = suspend (until resume() is called).
 * 		onStartFormatTag: fn(tagname, accum): called when a formatted string starts a tag.
 * 		onEndFormatTag: fn(tagname, accum): called when a formatted string ends a tag.
 * 		onFormatText: fn(text, accum): called when a formatted string needs to process text.
 */
var TBrowserHandler = function(TAMEENGINE, options)
{
	let self = this;
	let BLANK_FUNCTION = function(){};

	// Fill Options
	this.defaultOptions = 
	{
		"print": BLANK_FUNCTION,
		"onStart": BLANK_FUNCTION,
		"onEnd": BLANK_FUNCTION,
		"onSuspend": BLANK_FUNCTION,
		"onResume": BLANK_FUNCTION,
		"onQuit": BLANK_FUNCTION,
		"onPause": BLANK_FUNCTION,
		"onError": BLANK_FUNCTION,
		"onFatal": BLANK_FUNCTION,
		"onOtherCue": BLANK_FUNCTION,
		"onStartFormatTag": BLANK_FUNCTION,
		"onEndFormatTag": BLANK_FUNCTION, 
		"onFormatText": function(text, accum) 
		{
			accum.push(text);
		}
	};
	
	if (!options)
		options = {};
	
	let combinedOptions = {};
	for (let x in this.defaultOptions) if (this.defaultOptions.hasOwnProperty(x)) 
		combinedOptions[x] = options[x] || this.defaultOptions[x];
	this.options = combinedOptions;
	
	// cue name -> function(content)
	this.cueHandlers =
	{
		"quit": function()
		{
			self.stop = true;
			return false;
		},
		
		"text": function(content)
		{
			self.textBuffer.push(content);
			return true;
		},
		
		"textf": function(content)
		{
			self.textBuffer.push(TAMEENGINE.parseFormatted(
				content, 
				self.options.onStartFormatTag, 
				self.options.onEndFormatTag, 
				self.options.onFormatText
			));
			return true;
		},
			
		"wait": function(content)
		{
			setTimeout(function(){self.resume();}, parseInt(content, 10));
			return false;
		},

		"pause": function()
		{
			self.pause = true;
			return false;
		},

		"trace": function()
		{
			// Ignore trace.
			return true;
		},

		"error": function(content)
		{
			self.options.onError(content);
			return true;
		},

		"fatal": function(content)
		{
			self.options.onFatal(content);
			self.stop = true;
			return false;
		}
		
	};

};

/**
 * Resets the cue read state.
 */
TBrowserHandler.prototype.reset = function()
{
	this.stop = false;
	this.pause = false;
	this.textBuffer = [];	
};

/**
 * Prepares the response for read.
 * @param response the response from an initialize or interpret call.
 */
TBrowserHandler.prototype.prepare = function(response)
{
	this.reset();
	this.response = response;
	this.nextCue = 0;
};

/**
 * Reads the response.
 * Set with prepareResponse().
 * @return true if more unprocessed cues remain, or false if not. 
 */
TBrowserHandler.prototype.resume = function()
{
	if (!this.response)
		throw new Error('resume() before prepare()!');

	if (this.nextCue === 0)
		this.options.onStart();
	else
		this.options.onResume();

	// clear pause if set.
	if (this.pause)
		this.pause = false;
	
	// Process Cue Loop
	var keepGoing = true;
	while ((this.nextCue < this.response.responseCues.length) && keepGoing) 
	{
		var cue = this.response.responseCues[this.nextCue++];
		
		var cueType = cue.type.toLowerCase();
		var cueContent = cue.content;
		
		if (cueType !== 'text' && cueType !== 'textf') 
		{
			this.options.print(this.textBuffer.join(''));
			this.textBuffer.length = 0;
		}

		if (this.cueHandlers[cueType])
			keepGoing = this.cueHandlers[cueType](cueContent);
		else
			keepGoing = this.options.onOtherCue(cueType, cueContent);
	}

	if (this.textBuffer.length > 0) 
	{
		this.options.print(this.textBuffer.join(''));
		this.textBuffer.length = 0;
	}
	
	if (this.pause)
	{
		this.options.onPause();
	}
	
	// if stop, halt completely.
	if (this.stop)
	{
		// unload response.
		this.response = null;
		this.options.onQuit();
		this.options.onEnd();
		return false;
	}
	
	// If halted before end...
	if (!this.pause && this.nextCue < this.response.responseCues.length)
	{
		this.options.onSuspend();
		return true;
	}
	else
	{
		this.options.onEnd();
		return false;
	}
	
};

/**
 * Prepares the response for read and calls resume.
 * Calls prepare(response) and then resume().
 * @param response the response from an initialize or interpret call.
 */
TBrowserHandler.prototype.process = function(response)
{
	this.prepare(response);
	this.resume();
};

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TBrowserHandler;
// =========================================================================

