/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('./Util.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

var TResponseHandler = function(options)
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
		"onTrace": BLANK_FUNCTION,
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
			self.textBuffer.push(Util.parseFormatted(
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
TResponseHandler.prototype.reset = function()
{
	this.stop = false;
	this.pause = false;
	this.textBuffer = [];	
};

/**
 * Prepares the response for read.
 * @param response the response from an initialize or interpret call.
 */
TResponseHandler.prototype.prepare = function(response)
{
	this.reset();
	this.response = response;
	this.nextCue = 0;
};

/**
 * Reads the response.
 * Set with prepare().
 * @return true if more unprocessed cues remain, or false if not. 
 */
TResponseHandler.prototype.resume = function()
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
	let keepGoing = true;
	while ((this.nextCue < this.response.responseCues.length) && keepGoing) 
	{
		let cue = this.response.responseCues[this.nextCue++];
		
		let cueType = cue.type.toLowerCase();
		let cueContent = cue.content;
		
		if (cueType !== 'text' && cueType !== 'textf') 
		{
			this.options.print(this.textBuffer.join(''));
			this.textBuffer.length = 0;
		}
		
		if (cueType.startsWith('trace-'))
		{
			let type = cueType.substring('trace-'.length);
			this.options.onTrace(type, cueContent);
		}
		else if (this.cueHandlers[cueType])
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
	if (!this.pause)
	{
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
	}
	else
	{
		return false;
	}
	
};

/**
 * Prepares the response for read and calls resume.
 * Calls prepare(response) and then resume().
 * @param response the response from an initialize or interpret call.
 * @return true if more unprocessed cues remain, or false if not. 
 */
TResponseHandler.prototype.process = function(response)
{
	this.prepare(response);
	return this.resume();
};

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TResponseHandler;
// =========================================================================

