/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TAMEConstants = TAMEConstants || (typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null;
// ======================================================================================================

//##[[CONTENT-START

/*****************************************************************************
 See net.mtrop.tame.TAMEResponse
 *****************************************************************************/
var TResponse = function()
{
    this.responseCues = [];
    this.commandsExecuted = 0;
    this.requestNanos = 0;
    this.interpretNanos = 0;
};

/**
 * Adds a cue to the response.
 * @param type the cue type name.
 * @param content the cue content.
 */
TResponse.prototype.addCue = function(type, content)
{
	if ((typeof content) === 'unefined' || content == null)
		content = "";
	this.responseCues.push({"type": type, "content": content});
};

/**
 * Adds a TRACE cue to the response, if tracing is enabled.
 * @param request (TRequest) the request object.
 * @param content the content to add.
 */
TResponse.prototype.trace = function(request, content)
{
	if (request.tracing)
		this.addCue("TRACE", content);
};

/**
 * Increments and checks if command amount breaches the threshold.
 * @throw TAMEError if a breach is detected.
 */
TResponse.prototype.incrementAndCheckCommandsExecuted = function()
{
	this.commandsExecuted++;
	if (this.commandsExecuted >= TAMEConstants.RUNAWAY_THRESHOLD)
		throw TAMEError.RunawayRequest("Too many commands executed - possible infinite loop.");
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TResponse;
// =========================================================================

