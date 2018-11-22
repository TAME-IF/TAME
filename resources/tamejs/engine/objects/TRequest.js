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
var TAMEConstants = TAMEConstants || ((typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

/*****************************************************************************
 See com.tameif.tame.TAMERequest
 *****************************************************************************/
var TRequest = function(context, inputMessage, traceTypesMap)
{
	this.moduleContext = context;
    this.inputMessage = inputMessage;
    this.traceTypesMap = traceTypesMap;
 
	// Stacks and Queues
    this.commandQueue = [];
    this.valueStack = [];
    this.contextStack = [];
};

/**
 * Gets the request's input message.
 * This gets interpreted by the TAME virtual machine.
 * @return the message used in the request.
 */
TRequest.prototype.getInputMessage = function()
{
	return this.inputMessage;
};

/**
 * Does this trace a specific type?
 * @param traceType the tracing type name. 
 * @return true if so, false if not.
 */
TRequest.prototype.traces = function(traceType)
{
	return this.traceTypesMap[traceType];
};

/**
 * Adds a command to the queue to be processed later.
 * @param item the command to add.
 */
TRequest.prototype.addCommand = function(item)
{
	this.commandQueue.push(item);
};

/**
 * Checks if this still has commands to process.
 * @return true if so, false if not.
 */
TRequest.prototype.hasCommands = function()
{
	return this.commandQueue.length !== 0;
};

/**
 * Dequeues a command from the queue to be processed later.
 * @return the next command to process.
 */
TRequest.prototype.nextCommand = function()
{
	return this.commandQueue.shift();
};

/**
 * Pushes an element context value onto the context stack.
 * @param context the context to push.
 */
TRequest.prototype.pushContext = function(context)
{
	this.contextStack.push(context);
};

/**
 * Removes an element context value off of the context stack and returns it.
 * @return the element context on the stack or null if none in the stack.
 */
TRequest.prototype.popContext = function()
{
	return this.contextStack.pop();
};

/**
 * Looks at the top of the element context stack.
 * @return the top of the context stack, or null if the stack is empty.
 */
TRequest.prototype.peekContext = function()
{
	return this.contextStack[this.contextStack.length - 1];
};

/**
 * Pushes a value onto the arithmetic stack.
 * @param value the value to push.
 */
TRequest.prototype.pushValue = function(value)
{
	this.valueStack.push(value);
};

/**
 * Removes the topmost value off the arithmetic stack.
 * @return the value popped off the stack or null if the stack is empty.
 * @throws ArithmeticStackStateException if the stack is empty.
 */
TRequest.prototype.popValue = function()
{
	if (this.valueStack.length === 0)
		throw TAMEError.ArithmeticStackState("Attempt to pop an empty arithmetic stack.");
	return this.valueStack.pop();
};

/**
 * Checks if the arithmetic stack is empty.
 * Should be called after a full request is made.
 * @throws ArithmeticStackStateException if the stack is NOT empty.
 */
TRequest.prototype.checkStackClear = function()
{
	if (this.valueStack.length !== 0)
		throw TAMEError.ArithmeticStackState("Arithmetic stack is not empty.");
};


//[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TRequest;
// =========================================================================

