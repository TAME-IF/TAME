/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TAMEConstants = TAMEConstants || (typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null;
// ======================================================================================================

//[[EXPORTJS-START

/*****************************************************************************
 See com.tameif.tame.TAMEResponse
 *****************************************************************************/
var TResponse = function()
{
    this.responseCues = [];
    this.operationsExecuted = 0;
    this.functionDepth = 0;
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
	if ((typeof content) === 'undefined' || content === null)
		content = "";
	else
		content = String(content);
	this.responseCues.push({"type": type, "content": content});
};

/**
 * Adds a TRACE cue to the response, if tracing is enabled.
 * @param request (TRequest) the request object.
 * @param traceType (string) the trace type.
 * @param content the content to add.
 */
TResponse.prototype.trace = function(request, traceType, content)
{
	if (request.traces(traceType))
		this.addCue("TRACE-"+traceType, content);
};

/**
 * Increments and checks if operation amount breaches the threshold.
 * @throw TAMEError if a breach is detected.
 */
TResponse.prototype.incrementAndCheckOperationsExecuted = function(maxOperations)
{
	this.operationsExecuted++;
	if (this.operationsExecuted >= maxOperations)
		throw TAMEError.RunawayRequest("Too many operations executed - possible infinite loop.");
};

/**
 * Increments and checks if function depth breaches the threshold.
 * @throw TAMEError if a breach is detected.
 */
TResponse.prototype.incrementAndCheckFunctionDepth = function(maxDepth)
{
	this.functionDepth++;
	if (this.functionDepth >= maxDepth)
		throw TAMEError.RunawayRequest("Too many function calls deep - possible stack overflow.");
};

/**
 * Decrements the function depth.
 */
TResponse.prototype.decrementFunctionDepth = function()
{
	this.functionDepth--;
};

//[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TResponse;
// =========================================================================

