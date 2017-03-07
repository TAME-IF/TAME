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
 * 		"onStart": Called before first cue is handled.
 *		"onPause": Called when a pause occurs (after a cue function).
 *		"onResume": Called on a resume (before cues are processed again).
 *		"onCue": Called to process a cue (should return false to halt handling (true to keep going)).
 *		"onEnd": Called after last cue is handled.
 */
var TResponseReader = function(tresponse, eventFunctionMap)
{
	this.nextCue = 0;
	this.currentResponse = tresponse;
	this.eventFunctionMap = eventFunctionMap;
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
		if (this.eventFunctionMap['onStart'])
			this.eventFunctionMap['onStart']();
	}
	else
	{
		if (this.eventFunctionMap['onResume'])
			this.eventFunctionMap['onResume']();
	}
	
	var keepGoing = true;
	while (this.hasMoreCues() && keepGoing) 
	{
		var cue = this.currentResponse.responseCues[this.nextCue++];
		if (this.eventFunctionMap['onCue'])
			this.eventFunctionMap['onCue'](cue);
	}
	
	if (this.hasMoreCues())
	{
		if (this.eventFunctionMap['onPause'])
			this.eventFunctionMap['onPause']();
		return true;
	}
	else
	{
		if (this.eventFunctionMap['onEnd'])
			this.eventFunctionMap['onEnd']();
		return false;
	}
};

//##[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEResponseHandler;
//=========================================================================
