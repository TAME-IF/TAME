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

/**
 * Creates a new iterable response handler.
 * @param tresponse the response to handle.
 * @param eventFunctionMap a map of functions to call on certain events.
 * 		"start": Called before first cue is handled.
 *		"pause": Called when a pause occurs (after a cue function).
 *		"resume": Called on a resume (before cues are processed again).
 *		"end": Called after last cue is handled.
 * @param cueFunction A default function that take two parameters (cue type, cue content), or a map of cue type to functions. 
 *		For the map:
 *			Cue type must be lowercase. 
 *			Function should accept one parameter: parameter as content. 
 *		Called function should return false to halt handling (true to keep going).
 */
var TResponseReader = function(tresponse, eventFunctionMap, cueFunction)
{
	this.nextCue = 0;
	this.currentResponse = tresponse;
	this.eventFunctionMap = eventFunctionMap;
	this.cueFunctionDefault = null;
	this.cueFunctionMap = null;

	if (Object.prototype.toString.call(cueFunction) === '[object Function]')
		this.cueFunctionDefault = cueFunction;
	else
		this.cueFunctionMap = cueFunction;
}

/**
 * Checks if there are more cues to read.
 * @return true if so, false if not.
 */
TResponseReader.prototype.hasMoreCues = function()
{
	return this.nextCue < this.currentResponse.responseCues.length;
};

/**
 * Resumes handling a response.
 * @return false if, on return, there are no more cues to process, or true if so (resume() should then be called to resume).
 */
TResponseReader.prototype.read = function()
{
	if (!this.currentResponse)
		throw 'resume() before handleResponse()!';
	
	if (this.nextCue == 0)
	{
		if (this.eventFunctionMap['start'])
			this.eventFunctionMap['start']();
	}
	else
	{
		if (this.eventFunctionMap['resume'])
			this.eventFunctionMap['resume']();
	}
	
	var keepGoing = true;
	while (this.hasMoreCues() && keepGoing) 
	{
		var cue = this.currentResponse.responseCues[this.nextCue++];
		if (this.cueFunctionDefault)
			keepGoing = this.cueFunctionDefault(cue.type.toLowerCase(), cue.content);
		else
			keepGoing = this.cueFunctionMap[cue.type.toLowerCase()](cue.content);
	}
	
	if (this.hasMoreCues())
	{
		if (this.eventFunctionMap['pause'])
			this.eventFunctionMap['pause']();
		return true;
	}
	else
	{
		if (this.eventFunctionMap['end'])
			this.eventFunctionMap['end']();
		return false;
	}
};

//##[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEResponseHandler;
//=========================================================================
