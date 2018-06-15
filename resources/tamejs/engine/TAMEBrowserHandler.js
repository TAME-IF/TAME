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

/**
 * Creates a new response handler.
 * Handles most cues and has entry points for other cues.
 * @param TAME reference to TAME engine.
 * @param options options object.
 * 		print: fn(text): called when a string needs printing.
 * 		onStart: fn(): called before cues start processing.
 * 		onEnd: fn(): called when cues stop processing, and the end is reached.
 * 		onSuspend: fn(): called when a call to process a cue initiates a suspension.
 * 		onResume: fn(): called when cues process again.
 * 		onPauseCue: fn(): called when a "pause" cue is processed. 
 * 			Should prompt for continuation, then call resume().
 * 		onQuitCue: fn(): called when a "quit" cue is encountered.
 * 			Should stop input. 
 * 		onErrorCue: fn(message): called when an "error" cue is encountered.
 * 			Should make a message appear on screen. Dismissable. 
 * 		onFatalCue: fn(message): called when a "fatal" cue is encountered.
 * 			Should make a message appear on screen, and halt input. 
 * 		onUnknownCue: fn(cueType, cueContent): called when a cue that is not handled by this handler needs processing. 
 * 			Should return boolean. true = keep going, false = suspend.
 * 		onStartFormatTag: fn(tagname): called when a formatted string starts a tag.
 * 		onEndFormatTag: fn(tagname): called when a formatted string ends a tag.
 * 		onFormatText: fn(tag): called when a formatted string needs to process text.
 */
var TBrowserHandler = function(TAMEENGINE, options)
{
	var self = this;
	var BLANK_FUNCTION = function(){};

	// Fill Options
	this.defaultOptions = 
	{
		"print": BLANK_FUNCTION,
		"onStart": BLANK_FUNCTION,
		"onEnd": BLANK_FUNCTION,
		"onSuspend": BLANK_FUNCTION,
		"onResume": BLANK_FUNCTION,
		"onQuitCue": BLANK_FUNCTION,
		"onPauseCue": BLANK_FUNCTION,
		"onErrorCue": BLANK_FUNCTION,
		"onFatalCue": BLANK_FUNCTION,
		"onOtherCue": BLANK_FUNCTION,
		"onStartFormatTag": BLANK_FUNCTION,
		"onEndFormatTag": BLANK_FUNCTION, 
		"onFormatText": function(text) 
		{
			self.textBuffer += text;
		}
	};
	
	var combinedOptions = {};
	for (x in this.defaultOptions) if (this.defaultOptions.hasOwnProperty(x)) 
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
			self.textBuffer += content;
			return true;
		},
		
		"textf": function(content)
		{
			TAMEENGINE.parseFormatted(content, self.options.onStartFormatTag, self.options.onEndFormatTag, self.options.onFormatText);
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
			self.options.onErrorCue(content);
			return true;
		},

		"fatal": function(content)
		{
			self.options.onFatalCue(content);
			self.stop = true;
			return false;
		}
		
	};

}

/**
 * Resets the cue read state.
 */
TAMEBrowserHandler.prototype.reset = function()
{
	this.stop = false;
	this.pause = false;
	this.textBuffer = '';	
};

/**
 * Prepares the response for read.
 * @param response the response from an initialize or interpret call.
 */
TAMEBrowserHandler.prototype.prepare = function(response)
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
TAMEBrowserHandler.prototype.resume = function()
{
	if (!this.response)
		throw 'resume() before prepare()!';

	if (this.nextCue == 0)
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
			this.options.print(this.textBuffer);
			this.textBuffer = '';
		}

		if (this.cueHandlers[cueType])
			keepGoing = this.cueHandlers[cueType](cueContent);
		else
			keepGoing = this.options.onOtherCue(cueType, cueContent);
	}

	if (this.textBuffer.length > 0) 
	{
		this.options.print(this.textBuffer);
		this.textBuffer = '';
	}
	
	if (this.pause)
	{
		this.options.onPauseCue();
	}
	
	// if stop, halt completely.
	if (this.stop)
	{
		// unload response.
		this.response = null;
		this.options.onQuitCue();
		this.options.onEnd();
		return false;
	}
	
	// If halted before end...
	if (this.nextCue < this.response.responseCues.length)
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

//##[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEBrowserHandler;
// =========================================================================

