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
var Util = Util || ((typeof require) !== 'undefined' ? require('./Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('./TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('./objects/TValue.js') : null);
var TRequest = TRequest || ((typeof require) !== 'undefined' ? require('./objects/TRequest.js') : null);
var TResponse = TResponse || ((typeof require) !== 'undefined' ? require('./objects/TResponse.js') : null);
var TArithmeticFunctions = TArithmeticFunctions || ((typeof require) !== 'undefined' ? require('./logic/TArithmeticFunctions.js') : null);
var TOperationFunctions = TOperationFunctions || ((typeof require) !== 'undefined' ? require('./logic/TOperationFunctions.js') : null);
var TStringBuilder = TStringBuilder || ((typeof require) !== 'undefined' ? require('./TStringBuilder.js') : null);
// ======================================================================================================

//##[[EXPORTJS-START

var TLogic = {};

//##[[EXPORTJS-INCLUDE logic/TArithmeticFunctions.js
//##[[EXPORTJS-INCLUDE logic/TOperationFunctions.js

/****************************************************************************
 * Main logic junk.
 ****************************************************************************/

/**
 * Sets a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 * @param value the value.
 */
TLogic.setValue = function(valueHash, variableName, value)
{
	variableName = variableName.toLowerCase();
	valueHash[variableName] = value;
};

/**
 * Sets a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 * @return the corresponding value or TValue.createBoolean(false) if no value.
 */
TLogic.getValue = function(valueHash, variableName)
{
	variableName = variableName.toLowerCase();
	if (!valueHash[variableName])
		return TValue.createBoolean(false);
	else
		return valueHash[variableName];
};

/**
 * Checks that a value exists on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 * @return true if so, false if not.
 */
TLogic.containsValue = function(valueHash, variableName)
{
	variableName = variableName.toLowerCase();
	return valueHash[variableName] ? true : false;
};

/**
 * Clears a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 */
TLogic.clearValue = function(valueHash, variableName)
{
	variableName = variableName.toLowerCase();
	delete valueHash[variableName];
};

/**
 * Turns a operation into a readable string.
 * @param cmdObject (Object) the operation object.
 * @return a string.
 */
TLogic.operationToString = function(operationObject)
{
	var sb = new TStringBuilder();
	sb.append(TOperationFunctions[operationObject.opcode].name);
	if (operationObject.operand0)
		sb.append(' ').append(TValue.toString(operationObject.operand0));
	if (operationObject.operand1)
		sb.append(' ').append(TValue.toString(operationObject.operand1));
	return sb.toString();
};

/**
 * Turns an element into a readable string.
 * @param elemObject (Object) the element object.
 * @return a string.
 */
TLogic.elementToString = function(elemObject)
{
	return elemObject.tameType + "[" + elemObject.identity + "]";
};

/**
 * Executes a block of operations.
 * @param block (Array) the block of operations.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeBlock = function(block, request, response, blockLocal)
{
	Util.each(block, function(operation) {
		response.trace(request, TAMEConstants.TraceType.FUNCTION, Util.format("CALL {0}", TLogic.operationToString(operation))); 
		TLogic.executeOperation(request, response, blockLocal, operation);
	});
};

/**
 * Increments the runaway operation counter and calls the operation.  
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param operation (Object) the operation object.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeOperation = function(request, response, blockLocal, operation)
{
	TOperationFunctions[operation.opcode].doOperation(request, response, blockLocal, operation);
	response.incrementAndCheckOperationsExecuted(request.moduleContext.operationRunawayMax);
};


/**
 * Calls the conditional block on a operation, returning the result as a .
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param operation (Object) the operation object.
 * @return true if result is equivalent to true, false if not.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.callConditional = function(operationName, request, response, blockLocal, operation)
{
	// block should contain arithmetic operations and a last push.
	let conditional = operation.conditionalBlock;
	if (!conditional)
		throw TAMEError.ModuleExecution("Conditional block for "+operationName+" does NOT EXIST!");
	
	response.trace(request, TAMEConstants.TraceType.CONTROL, operationName+" Conditional");
	TLogic.executeBlock(conditional, request, response, blockLocal);

	// get remaining expression value.
	let value = request.popValue();
	
	if (!TValue.isLiteral(value))
		throw TAMEError.UnexpectedValueType("Expected literal type after "+operationName+" conditional block execution.");

	let result = TValue.asBoolean(value);
	response.trace(request, TAMEConstants.TraceType.CONTROL, Util.format(operationName+" Conditional {0} is {1}", TValue.toString(value), result));
	return result;
};


/**
 * Enqueues an action based on how it is interpreted.
 * @param request the request object.
 * @param response the response object.
 * @param interpreterContext the interpreter context (left after interpretation).
 * @return true if interpret was good and an action was enqueued, false if error.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.enqueueInterpretedAction = function(request, response, interpreterContext) 
{
	var action = interpreterContext.action;
	if (action === null)
	{
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "UNKNOWN ACTION");
		if (!TLogic.callUnknownCommand(request, response))
			response.addCue(TAMEConstants.Cue.ERROR, "UNKNOWN COMMAND (make a better in-universe handler!).");
		return false;
	}
	else
	{
		switch (action.type)
		{
			default:
			case TAMEConstants.ActionType.GENERAL:
			{
				if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("STRICT GENERAL ACTION {0}: Extra Tokens (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addCommand(TCommand.create(action));
					return true;
				}
				break;
			}

			case TAMEConstants.ActionType.OPEN:
			{
				if (!interpreterContext.targetLookedUp)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("OPEN ACTION {0}: No Target (INCOMPLETE)", action.identity));
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addCommand(TCommand.createModal(action, interpreterContext.target));
					return true;
				}
				break;
			}

			case TAMEConstants.ActionType.MODAL:
			{
				if (!interpreterContext.modeLookedUp)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("MODAL ACTION {0}: No Mode (INCOMPLETE)", action.identity));
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.mode === null)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("MODAL ACTION {0}: Unknown Mode (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("STRICT MODAL ACTION {0}: Extra Tokens (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addCommand(TCommand.createModal(action, interpreterContext.mode));
					return true;
				}
				break;
			}

			case TAMEConstants.ActionType.TRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("TRANSITIVE ACTION {0} (AMBIGUOUS)", action.identity));
					if (!TLogic.callAmbiguousCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("TRANSITIVE ACTION {0}: No Object (INCOMPLETE)", action.identity));
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 === null)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("TRANSITIVE ACTION {0}: Unknown Object (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("STRICT TRANSITIVE ACTION {0}: Extra Tokens (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addCommand(TCommand.createObject(action, interpreterContext.object1));
					return true;
				}
				break;
			}
	
			case TAMEConstants.ActionType.DITRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0} (AMBIGUOUS)", action.identity));
					if (!TLogic.callAmbiguousCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: No First Object (INCOMPLETE)", action.identity));
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 === null)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: Unknown First Object (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.conjugateLookedUp)
				{
					if (action.strict)
					{
						response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("STRICT DITRANSITIVE ACTION {0}: No Conjunction (INCOMPLETE)", action.identity));
						if (!TLogic.callIncompleteCommand(request, response, action))
							response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else
					{
						response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: Unknown Conjunction (MALFORMED)", action.identity));
						request.addCommand(TCommand.createObject(action, interpreterContext.object1));
						return true;
					}
				}
				else if (!interpreterContext.conjugateFound)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: Unknown Conjunction (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object2LookedUp)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: No Second Object (INCOMPLETE)", action.identity));
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object2 === null)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("DITRANSITIVE ACTION {0}: Unknown Second Object (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, TAMEConstants.TraceType.INTERPRETER, Util.format("STRICT DITRANSITIVE ACTION {0}: Extra Tokens (MALFORMED)", action.identity));
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addCommand(TCommand.createObject2(action, interpreterContext.object1, interpreterContext.object2));
					return true;
				}
			}
		}
	}
};

/**
 * Does an action loop: this keeps processing queued actions 
 * until there is nothing left to process.
 * @param request the request context.
 * @param response the response object.
 * @param tameCommand (TCommand) the action to process.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processCommand = function(request, response, tameCommand) 
{
	try {
		
		switch (tameCommand.action.type)
		{
			default:
			case TAMEConstants.ActionType.GENERAL:
				TLogic.doActionGeneral(request, response, tameCommand.action);
				break;
			case TAMEConstants.ActionType.OPEN:
				TLogic.doActionOpen(request, response, tameCommand.action, tameCommand.target);
				break;
			case TAMEConstants.ActionType.MODAL:
				TLogic.doActionModal(request, response, tameCommand.action, tameCommand.target);
				break;
			case TAMEConstants.ActionType.TRANSITIVE:
				TLogic.doActionTransitive(request, response, tameCommand.action, tameCommand.object1);
				break;
			case TAMEConstants.ActionType.DITRANSITIVE:
				if (tameCommand.object2 === null)
					TLogic.doActionTransitive(request, response, tameCommand.action, tameCommand.object1);
				else
					TLogic.doActionDitransitive(request, response, tameCommand.action, tameCommand.object1, tameCommand.object2);
				break;
		}
		
	} catch (err) {
		// catch finish interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.Finish)
			throw err;
	} 
	
	request.checkStackClear();
	
};

/**
 * Does an action loop: this keeps processing queued actions until there is nothing left to process.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAllCommands = function(request, response) 
{
	while (request.hasCommands())
		TLogic.processCommand(request, response, request.nextCommand());
};

/**
 * Does a command loop: this keeps processing queued commands 
 * until there is nothing left to process.
 * @param request the request object.
 * @param response the response object.
 * @param afterSuccessfulCommand if true, executes the "after successful command" block.
 * @param afterFailedCommand if true, executes the "after failed command" block.
 * @param afterEveryCommand if true, executes the "after every command" block.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processCommandLoop = function(request, response, afterSuccessfulCommand, afterFailedCommand, afterEveryCommand) 
{
	TLogic.doAllCommands(request, response);
	let worldContext = request.moduleContext.getElementContext('world');
	if (afterSuccessfulCommand)
	{
		TLogic.callElementBlock(request, response, worldContext, "AFTERSUCCESSFULCOMMAND");
		TLogic.doAllCommands(request, response);
	}
	if (afterFailedCommand)
	{
		TLogic.callElementBlock(request, response, worldContext, "AFTERFAILEDCOMMAND");
		TLogic.doAllCommands(request, response);
	}
	if (afterEveryCommand)
	{
		TLogic.callElementBlock(request, response, worldContext, "AFTEREVERYCOMMAND");
		TLogic.doAllCommands(request, response);
	}		
	
};

/**
 * Handles initializing a context. Must be called after a new context and game is started.
 * @param context the module context.
 * @param tracing if true, add trace cues.
 * @return (TResponse) the response from the initialize.
 */
TLogic.handleInit = function(context, tracing) 
{
	var request = new TRequest(context, "[INITIALIZE]", tracing);
	var response = new TResponse();
	
	response.interpretNanos = 0;
	var time = Util.nanoTime();

	try 
	{
		TLogic.initializeContext(request, response);
		TLogic.processCommandLoop(request, response, false, false, false);
	} 
	catch (err) 
	{
		if (err instanceof TAMEInterrupt)
		{
			if (err.type != TAMEInterrupt.Type.Quit)
				response.addCue(TAMEConstants.Cue.ERROR, err.type+" interrupt was thrown.");
		}
		else if (err instanceof TAMEError)
			response.addCue(TAMEConstants.Cue.FATAL, err.message);
		else
			response.addCue(TAMEConstants.Cue.FATAL, err);
	}

	response.requestNanos = Util.nanoTime() - time;
	return response;
};


/**
 * Tokenizes the input string into tokens based on module settings.
 * @param context (object) the module context to use (for object availability).
 * @param inputMessage (string) the input message to tokenize.
 * @return (string[]) the tokens to parse.
 */
TLogic.tokenizeInput = function(context, input)
{
	return input.trim().toLowerCase().split(/\s+/);
};

/**
 * Handles interpretation and performs actions.
 * @param context (object) the module context.
 * @param inputMessage (string) the input message to interpret.
 * @param tracing (boolean) if true, add trace cues.
 * @return (TResponse) the response.
 */
TLogic.handleRequest = function(context, inputMessage, tracing)
{
	var request = new TRequest(context, inputMessage, tracing);
	var response = new TResponse();

	var time = Util.nanoTime();
	var interpreterContext = TLogic.interpret(request, response);
	response.interpretNanos = Util.nanoTime() - time; 

	time = Util.nanoTime();
	
	try 
	{
		var good = TLogic.enqueueInterpretedAction(request, response, interpreterContext);
		TLogic.processCommandLoop(request, response, good, !good, true);
	} 
	catch (err) 
	{
		if (err instanceof TAMEInterrupt)
		{
			if (err.type != TAMEInterrupt.Type.Quit)
				response.addCue(TAMEConstants.Cue.ERROR, err.type+" interrupt was thrown.");
		}
		else if (err instanceof TAMEError)
			response.addCue(TAMEConstants.Cue.FATAL, err.message);
		else
			response.addCue(TAMEConstants.Cue.FATAL, err);
	}
	
	response.requestNanos = Util.nanoTime() - time;
	return response;
};

/**
 * Performs the necessary tasks for calling an object block.
 * Ensures that the block is called cleanly.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param elementContext (object) the context that the block is executed through.
 * @param block [Object, ...] the block to execute.
 * @param isFunctionBlock (boolean) if true, this is a function call (changes some logic).
 * @param blockLocal (object) the initial block-local values to set on invoke.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBlock = function(request, response, elementContext, block, isFunctionBlock, blockLocal)
{
	response.trace(request, TAMEConstants.TraceType.CONTEXT, Util.format("PUSH {0}", "Context:"+elementContext.identity));
	request.pushContext(elementContext);
	
	if (!blockLocal)
		blockLocal = {};
	
	try {
		TLogic.executeBlock(block, request, response, blockLocal);
	} catch (err) {
		// catch end interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.End)
			throw err;
	} finally {
		response.trace(request, TAMEConstants.TraceType.CONTEXT, Util.format("POP {0}", "Context:"+elementContext.identity));
		request.popContext();
	}
	
	if (!isFunctionBlock)
		request.checkStackClear();
	
};

/**
 * Increments the runaway operation counter and calls the operation.  
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param elementContext (Object) the element context to search from.
 * @param blockTypeName (string) the name of the block type.
 * @param blockTypeValues (Array) list of values.
 * @param localname (string) [OPTIONAL] the name of the local variable to set.
 * @param targetValue (TValue Object) [OPTIONAL] the value to assign.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.callElementBlock = function(request, response, elementContext, blockTypeName, blockTypeValues, localname, targetValue)
{
	let context = request.moduleContext;
	let blockEntryName = context.resolveBlockName(blockTypeName, blockTypeValues);
	response.trace(request, TAMEConstants.TraceType.ENTRY, Util.format("RESOLVE {0}.{1}", elementContext.identity, blockEntryName));
	let block = context.resolveBlock(elementContext.identity, blockTypeName, blockTypeValues);
	if (block !== null)
	{
		response.trace(request, TAMEConstants.TraceType.ENTRY, Util.format("CALL {0}.{1}", elementContext.identity, blockEntryName));

		if (targetValue && localname)
		{
			// set locals
			let blockLocal = {};
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET LOCAL {0} {1}", localname, TValue.toString(targetValue)));
			TLogic.setValue(blockLocal, localname, targetValue);
			TLogic.callBlock(request, response, elementContext, block, false, blockLocal);
		}
		else
			TLogic.callBlock(request, response, elementContext, block);
		return true;
	}
	
	return false;
};

/**
 * Calls a function from an arbitrary context, using the bound element as a lineage search point.
 * @param request the request object.
 * @param response the response object.
 * @param functionName the function to execute.
 * @param originContext the origin context (and then element).
 * @throws TAMEInterrupt if an interrupt occurs.
 * @return the return value from the function call. if no return, returns false.
 */
TLogic.callElementFunction = function(request, response, functionName, originContext)
{
	var context = request.moduleContext;
	var element = context.resolveElement(originContext.identity);

	var entry = context.resolveFunction(originContext.identity, functionName);
	if (entry === null)
		throw TAMEError.Module("No such function ("+functionName+") in lineage of element " + TLogic.elementToString(element));

	response.trace(request, TAMEConstants.TraceType.FUNCTION, "CALL "+functionName);
	var blockLocal = {};
	var args = entry.arguments;
	for (var i = args.length - 1; i >= 0; i--)
	{
		var localValue = request.popValue();
		response.trace(request, TAMEConstants.TraceType.FUNCTION, Util.format("SET LOCAL {0} {1}", args[i], TValue.toString(localValue)));
		blockLocal[args[i]] = localValue;
	}
	
	response.incrementAndCheckFunctionDepth(request.moduleContext.functionDepthMax);
	TLogic.callBlock(request, response, originContext, entry.block, true, blockLocal);
	response.decrementFunctionDepth();

	return TLogic.getValue(blockLocal, TAMEConstants.RETURN_VARIABLE);
};


/**
 * Interprets the input on the request.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @return a new interpreter context using the input.
 */
TLogic.interpret = function(request, response)
{
	let context = request.moduleContext;
	let tokens = TLogic.tokenizeInput(context, request.inputMessage);
	
	var interpreterContext = 
	{
		"tokens": tokens,
		"tokenOffset": 0,
		"objects": [null, null],
		"action": null,
		"modeLookedUp": false,
		"mode": null,
		"targetLookedUp": false,
		"target": null,
		"conjugateLookedUp": false,
		"conjugate": null,
		"object1LookedUp": false,
		"object1": null,
		"object2LookedUp": false,
		"object2": null,
		"objectAmbiguous": false
	};

	TLogic.interpretAction(request, response, interpreterContext);

	let action = interpreterContext.action;
	if (action === null)
		return interpreterContext;

	switch (action.type)
	{
		default:
		case TAMEConstants.ActionType.GENERAL:
			return interpreterContext;
		case TAMEConstants.ActionType.OPEN:
			TLogic.interpretOpen(request, response, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.MODAL:
			TLogic.interpretMode(request, response, action, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.TRANSITIVE:
			TLogic.interpretObject1(request, response, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.DITRANSITIVE:
			if (TLogic.interpretObject1(request, response, interpreterContext))
				if (TLogic.interpretConjugate(request, response, action, interpreterContext))
					TLogic.interpretObject2(request, response, interpreterContext);
			return interpreterContext;
	}
	
};

/**
 * Interprets an action from the input line.
 * @param moduleContext (TModuleContext) the module context.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretAction = function(request, response, interpreterContext)
{
	let moduleContext = request.moduleContext;
	var module = moduleContext.module;
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;

		let name = sb.toString();
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "TEST ACTION "+name);
		var next = module.getActionByName(name);
		if (next !== null)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED ACTION "+next.identity);
			interpreterContext.action = next;
			interpreterContext.tokenOffset = index;
		}
	
	}
	
};

/**
 * Interprets an action mode from the input line.
 * @param action (object:action) the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretMode = function(request, response, action, interpreterContext)
{
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;

		interpreterContext.modeLookedUp = true;
		var next = sb.toString();
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "TEST MODE "+name);
		if (action.extraStrings.indexOf(next) >= 0)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED MODE "+next);
			interpreterContext.mode = next;
			interpreterContext.tokenOffset = index;
		}
		
	}
	
};

/**
 * Interprets open target.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretOpen = function(request, response, interpreterContext)
{
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	
	while (index < tokens.length)
	{
		interpreterContext.targetLookedUp = true;
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;
	}
	
	var out = sb.toString();
	response.trace(request, TAMEConstants.TraceType.INTERPRETER, "READ OPEN TARGET "+out);
	interpreterContext.target = out.length > 0 ? out : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretConjugate = function(request, response, action, interpreterContext)
{
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	var out = false;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;
		
		interpreterContext.conjugateLookedUp = true;
		let name = sb.toString();
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "TEST CONJUNCTION "+name);
		if (action.extraStrings.indexOf(name) >= 0)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED CONJUNCTION "+name);
			interpreterContext.tokenOffset = index;
			out = true;
		}
		
	}

	interpreterContext.conjugateFound = out;
	return out;
};

/**
 * Interprets the first object from the input line.
 * This is context-sensitive, as its priority is to match objects on the current
 * player's person, as well as in the current room. These checks are skipped if
 * the player is null, or the current room is null.
 * The priority order is player inventory, then room contents, then world.
 * @param moduleContext (TModuleContext) the module context.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretObject1 = function(request, response, interpreterContext)
{
	let moduleContext = request.moduleContext;
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;
		
		let name = sb.toString();
		interpreterContext.object1LookedUp = true;
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "TEST OBJECT 1 "+name);
		let out = moduleContext.getAccessibleObjectsByName(name, interpreterContext.objects, 0);
		if (out > 1)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED MULTIPLE OBJECTS");
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object1 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED OBJECT 1 "+interpreterContext.objects[0].identity);
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object1 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object1 !== null;
};

/**
 * Interprets the second object from the input line.
 * This is context-sensitive, as its priority is to match objects on the current
 * player's person, as well as in the current room. These checks are skipped if
 * the player is null, or the current room is null.
 * The priority order is player inventory, then room contents, then world.
 * @param moduleContext the module context.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretObject2 = function(request, response, interpreterContext)
{
	let moduleContext = request.moduleContext;
	var sb = new TStringBuilder();
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb.append(' ');
		sb.append(tokens[index]);
		index++;
		
		let name = sb.toString();
		interpreterContext.object2LookedUp = true;
		response.trace(request, TAMEConstants.TraceType.INTERPRETER, "TEST OBJECT 2 "+name);
		let out = moduleContext.getAccessibleObjectsByName(name, interpreterContext.objects, 0);
		if (out > 1)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED MULTIPLE OBJECTS");
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object2 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			response.trace(request, TAMEConstants.TraceType.INTERPRETER, "MATCHED OBJECT 2 "+interpreterContext.objects[0].identity);
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object2 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object2 !== null;
};

/**
 * Checks if an object is accessible to a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player viewpoint identity.
 * @param objectIdentity the object to check's identity.
 * @return true if the object is considered "accessible," false if not.
 */
TLogic.checkObjectAccessibility = function(request, response, playerIdentity, objectIdentity) 
{
	let context = request.moduleContext;
	let world = context.getElement('world');

	if (context.checkElementHasObject(world.identity, objectIdentity))
		return true;

	if (context.checkElementHasObject(playerIdentity, objectIdentity))
		return true;

	let currentRoom = context.getCurrentRoom(playerIdentity);
	if (currentRoom !== null && context.checkElementHasObject(currentRoom.identity, objectIdentity))
		return true;
	
	return false;
};


/**
 * Performs an arithmetic function on the stack.
 * @param request the request context.
 * @param response the response object.
 * @param functionType the function type (index).
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doArithmeticStackFunction = function(request, response, functionType)
{
	if (functionType < 0 || functionType >= TArithmeticFunctions.COUNT)
		throw TAMEError.ModuleExecution("Expected arithmetic function type, got illegal value "+functionType+".");
	
	var operator = TArithmeticFunctions[functionType];
	response.trace(request, TAMEConstants.TraceType.FUNCTION, "Operator is " + operator.name);
	
	if (operator.binary)
	{
		let v2 = request.popValue();
		let v1 = request.popValue();
		request.pushValue(operator.doOperation(v1, v2));
	}
	else
	{
		let v1 = request.popValue();
		request.pushValue(operator.doOperation(v1));
	}
};

/**
 * Attempts to perform a player switch.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the next player identity.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doPlayerSwitch = function(request, response, playerIdentity)
{
	var context = request.moduleContext;
	context.setCurrentPlayer(playerIdentity);
	response.trace(request, TAMEConstants.TraceType.CONTEXT, "CURRENT PLAYER: "+playerIdentity);
};

/**
 * Attempts to perform a room stack pop for a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity to pop a room context from.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomPop = function(request, response, playerIdentity)
{
	var context = request.moduleContext;
	let roomIdentity = context.popRoomFromPlayer(playerIdentity);
	if (roomIdentity)
		response.trace(request, TAMEConstants.TraceType.CONTEXT, Util.format("POP ROOM {0} FROM PLAYER {1}", roomIdentity, playerIdentity));
};

/**
 * Attempts to perform a room stack push for a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity to push a room context onto.
 * @param roomIdentity the room identity to push.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomPush = function(request, response, playerIdentity, roomIdentity)
{
	var context = request.moduleContext;
	context.pushRoomOntoPlayer(playerIdentity, roomIdentity);
	response.trace(request, TAMEConstants.TraceType.CONTEXT, Util.format("PUSH ROOM {0} ON PLAYER {1}", roomIdentity, playerIdentity));
};

/**
 * Attempts to perform a room switch.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity that is switching rooms.
 * @param roomIdentity the target room identity.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomSwitch = function(request, response, playerIdentity, roomIdentity)
{
	var context = request.moduleContext;

	// pop all rooms on the stack.
	while (context.getCurrentRoom(playerIdentity) !== null)
		TLogic.doRoomPop(request, response, playerIdentity);

	// push new room on the stack and call focus.
	TLogic.doRoomPush(request, response, playerIdentity, roomIdentity);
};

/**
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param element (Object) the element being browsed.
 * @param objectContext (Object) the object context to call a browse block on.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doBrowseBlockSearch = function(request, response, element, objectContext)
{
	var context = request.moduleContext;

	// special case for world - no hierarchy.
	if (element.tameType === 'TWorld')
		return TLogic.callElementBlock(request, response, objectContext, "ONWORLDBROWSE");

	var next = element;
	while (next !== null)
	{
		let blockEntryName;
		let blockEntryValues;
		
		// aspect search.
		if (next.tameType === 'TContainer')
		{
			blockEntryName = "ONELEMENTBROWSE";
			blockEntryValues = [TValue.createContainer(next.identity)];
		}
		else if (next.tameType === 'TRoom')
		{
			blockEntryName = "ONELEMENTBROWSE"; 
			blockEntryValues = [TValue.createRoom(next.identity)];
		}
		else if (next.tameType === 'TPlayer')
		{
			blockEntryName = "ONELEMENTBROWSE";
			blockEntryValues = [TValue.createPlayer(next.identity)];
		}
		else
			throw TAMEError.UnexpectedValueType("Bad object container type in hierarchy.");

		if (TLogic.callElementBlock(request, response, objectContext, blockEntryName, blockEntryValues))
			return true;
		
		next = next.parent ? context.getElement(next.parent) : null;
	}
	
	// base fallback.
	if (element.tameType === 'TContainer')
		return TLogic.callElementBlock(request, response, objectContext, "ONCONTAINERBROWSE");
	else if (element.tameType === 'TRoom')
		return TLogic.callElementBlock(request, response, objectContext, "ONROOMBROWSE");
	else if (element.tameType === 'TPlayer')
		return TLogic.callElementBlock(request, response, objectContext, "ONPLAYERBROWSE");
	else
		throw TAMEError.UnexpectedValueType("Bad object container type in hierarchy.");
};


/**
 * Attempts to perform a browse.
 * @param request the request object.
 * @param response the response object.
 * @param blockEntryTypeName the block entry type name.
 * @param elementIdentity the element identity to browse through.
 * @param tag the tag to filter by.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doBrowse = function(request, response, elementIdentity, tag)
{
	var context = request.moduleContext;
	var element = context.resolveElement(elementIdentity);

	Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity)
	{
		var objectContext = context.getElementContext(objectIdentity);
		
		if (tag && !context.checkObjectHasTag(objectIdentity, tag))
			return;
		
		TLogic.doBrowseBlockSearch(request, response, element, objectContext);
	});
};

/**
 * Call init on iterable contexts.
 */
TLogic.callInitOnContexts = function(request, response, contextList)
{
	Util.each(contextList, function(context)
	{
		TLogic.callElementBlock(request, response, context, "INIT");
	});
};

/**
 * Initializes a newly-created context by executing each initialization block on each object.
 * Order is Containers, Objects, Rooms, Players, and the World.
 * @param request the request object containing the module context.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt is thrown.
 * @throws TAMEFatalException if something goes wrong during execution.
 */
TLogic.initializeContext = function(request, response) 
{
	var context = request.moduleContext;
	
	var containerContexts = [];
	var objectContexts = [];
	var roomContexts = [];
	var playerContexts = [];
	
	Util.each(context.state.elements, function(elementContext)
	{
		var element = context.resolveElement(elementContext.identity);
		if (element.tameType === 'TContainer')
			containerContexts.push(elementContext);
		else if (element.tameType === 'TObject')
			objectContexts.push(elementContext);
		else if (element.tameType === 'TPlayer')
			playerContexts.push(elementContext);
		else if (element.tameType === 'TRoom')
			roomContexts.push(elementContext);
	});
	
	try {
		TLogic.callInitOnContexts(request, response, containerContexts);
		TLogic.callInitOnContexts(request, response, objectContexts);
		TLogic.callInitOnContexts(request, response, roomContexts);
		TLogic.callInitOnContexts(request, response, playerContexts);
		let worldContext = context.resolveElementContext("world");
		TLogic.callElementBlock(request, response, worldContext, "INIT");
		TLogic.callElementBlock(request, response, worldContext, "START");
	} catch (err) {
		// catch finish interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.Finish)
			throw err;
	}
};

/**
 * Attempts to call the ambiguous command blocks.
 * @param request the request object.
 * @param response the response object.
 * @param action the action used.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callAmbiguousCommand = function(request, response, action)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity)];
	
	if (currentPlayerContext !== null)
	{
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONAMBIGUOUSCOMMAND", actionValues))
			return true;
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONAMBIGUOUSCOMMAND"))
			return true;
	}

	let worldContext = context.getElementContext('world');
	
	if (TLogic.callElementBlock(request, response, worldContext, "ONAMBIGUOUSCOMMAND", actionValues))
		return true;
	if (TLogic.callElementBlock(request, response, worldContext, "ONAMBIGUOUSCOMMAND"))
		return true;

	return false;
};

/**
 * Calls the appropriate malformed command blocks if they exist.
 * Malformed commands are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callMalformedCommand = function(request, response, action)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity)];
	
	if (currentPlayerContext !== null)
	{
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONMALFORMEDCOMMAND", actionValues))
			return true;
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONMALFORMEDCOMMAND"))
			return true;
	}

	let worldContext = context.getElementContext('world');
	
	if (TLogic.callElementBlock(request, response, worldContext, "ONMALFORMEDCOMMAND", actionValues))
		return true;
	if (TLogic.callElementBlock(request, response, worldContext, "ONMALFORMEDCOMMAND"))
		return true;

	return false;
};

/**
 * Calls the appropriate incomplete command blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callIncompleteCommand = function(request, response, action)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity)];
	
	if (currentPlayerContext !== null)
	{
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONINCOMPLETECOMMAND", actionValues))
			return true;
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONINCOMPLETECOMMAND"))
			return true;
	}

	let worldContext = context.getElementContext('world');
	
	if (TLogic.callElementBlock(request, response, worldContext, "ONINCOMPLETECOMMAND", actionValues))
		return true;
	if (TLogic.callElementBlock(request, response, worldContext, "ONINCOMPLETECOMMAND"))
		return true;

	return false;
};

/**
 * Calls the appropriate action unhandled blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callActionUnhandled = function(request, response, action)
{
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity)];
	
	if (currentPlayerContext !== null)
	{
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONUNHANDLEDACTION", actionValues))
			return true;
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONUNHANDLEDACTION"))
			return true;
	}

	let worldContext = context.getElementContext('world');
	
	if (TLogic.callElementBlock(request, response, worldContext, "ONUNHANDLEDACTION", actionValues))
		return true;
	if (TLogic.callElementBlock(request, response, worldContext, "ONUNHANDLEDACTION"))
		return true;

	return false;
};


/**
 * Attempts to call the unknown command blocks.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callUnknownCommand = function(request, response)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	if (currentPlayerContext !== null)
	{
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONUNKNOWNCOMMAND"))
			return true;
	}
	
	let worldContext = context.getElementContext('world');

	if (TLogic.callElementBlock(request, response, worldContext, "ONUNKNOWNCOMMAND"))
		return true;

	return false;
};

/**
 * Attempts to perform a general action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionGeneral = function(request, response, action)
{
	TLogic.doActionOpen(request, response, action, null);
};

/**
 * Attempts to perform a general action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param openTarget if not null, added as a target variable.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionOpen = function(request, response, action, openTarget)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity)];
	let localValue = action.extraStrings ? action.extraStrings[0] : null;
	
	if (currentPlayerContext !== null)
	{
		let currentRoomContext = context.getCurrentRoomContext();

		if (currentRoomContext !== null && TLogic.callElementBlock(request, response, currentRoomContext, "ONACTION", actionValues, localValue, openTarget))
			return;

		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONACTION", actionValues, localValue, openTarget))
			return;
	}
	
	let worldContext = context.getElementContext('world');

	if (TLogic.callElementBlock(request, response, worldContext, "ONACTION", actionValues, localValue, openTarget))
		return;

	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};

/**
 * Attempts to perform a modal action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param mode the mode to process.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionModal = function(request, response, action, mode)
{
	let context = request.moduleContext;
	let currentPlayerContext = context.getCurrentPlayerContext();

	let actionValues = [TValue.createAction(action.identity), TValue.createString(mode)];
	
	if (currentPlayerContext !== null)
	{
		// try current room.
		let currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext !== null && TLogic.callElementBlock(request, response, currentRoomContext, "ONMODALACTION", actionValues))
			return;
		
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONMODALACTION", actionValues))
			return;
	}
	
	let worldContext = context.getElementContext('world');
	if (TLogic.callElementBlock(request, response, worldContext, "ONMODALACTION", actionValues))
		return;

	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};

/**
 * Attempts to perform a transitive action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param object the target object for the action.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionTransitive = function(request, response, action, object)
{
	let context = request.moduleContext;
	let currentObjectContext = context.getElementContext(object.identity);
	let actionValue = TValue.createAction(action.identity);
	
	if (TLogic.callElementBlock(request, response, currentObjectContext, "ONACTION", [actionValue]))
		return;
	
	let objectValue = TValue.createObject(object.identity);
	let currentPlayerContext = context.getCurrentPlayerContext();

	// Call onActionWith(action, object) on current room, then player.
	if (currentPlayerContext !== null)
	{
		let currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext !== null)
		{
			if (TLogic.callElementBlock(request, response, currentRoomContext, "ONACTIONWITH", [actionValue, objectValue]))
				return;
			else if (TLogic.doActionAncestorSearch(request, response, actionValue, currentRoomContext, object))
				return;
			else if (TLogic.callElementBlock(request, response, currentRoomContext, "ONACTIONWITHOTHER", [actionValue]))
				return;
		}
		
		if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONACTIONWITH", [actionValue, objectValue]))
			return;
		else if (TLogic.doActionAncestorSearch(request, response, actionValue, currentPlayerContext, object))
			return;
		else if (TLogic.callElementBlock(request, response, currentPlayerContext, "ONACTIONWITHOTHER", [actionValue]))
			return;
	}

	let worldContext = context.getElementContext('world');

	if (TLogic.callElementBlock(request, response, worldContext, "ONACTIONWITH", [actionValue, objectValue]))
		return;
	else if (TLogic.doActionAncestorSearch(request, response, actionValue, worldContext, object))
		return;
	else if (TLogic.callElementBlock(request, response, worldContext, "ONACTIONWITHOTHER", [actionValue]))
		return;

	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};


/**
 * Attempts to perform a ditransitive action for the ancestor search.
 * @param request the request object.
 * @param response the response object.
 * @param actionValue the action that is being called (value).
 * @param elementContext the element context to call the block on.
 * @param start the object to start the search from.
 * @return true if a block was found an called.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionAncestorSearch = function(request, response, actionValue, elementContext, start)
{
	var context = request.moduleContext;
	var ancestor = start.parent ? context.getElement(start.parent) : null;
	while (ancestor !== null)
	{
		if (TLogic.callElementBlock(request, response, elementContext, "ONACTIONWITHANCESTOR", [actionValue, TValue.createObject(ancestor.identity)]))
			return true;
		ancestor = ancestor.parent ? context.getElement(ancestor.parent) : null;
	}
	
	return false;
};

/**
 * Attempts to perform a ditransitive action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param object1 the first object for the action.
 * @param object2 the second object for the action.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionDitransitive = function(request, response, action, object1, object2)
{
	let context = request.moduleContext;
	let currentObject1Context = context.getElementContext(object1.identity);
	let currentObject2Context = context.getElementContext(object2.identity);

	let actionValue = TValue.createAction(action.identity);
	let object1Value = TValue.createObject(object1.identity);
	let object2Value = TValue.createObject(object2.identity);
	
	var call12 = !action.strict || !action.reversed;
	var call21 = !action.strict || action.reversed;

	// call action on each object. one or both need to succeed for no failure.
	if (call12 && TLogic.callElementBlock(request, response, currentObject1Context, "ONACTIONWITH", [actionValue, object2Value]))
		return;
	if (call21 && TLogic.callElementBlock(request, response, currentObject2Context, "ONACTIONWITH", [actionValue, object1Value]))
		return;

	// call action with ancestor on each object. one or both need to succeed for no failure.
	if (call12 && TLogic.doActionAncestorSearch(request, response, actionValue, currentObject1Context, object2))
		return;
	if (call21 && TLogic.doActionAncestorSearch(request, response, actionValue, currentObject2Context, object1))
		return;
	
	// attempt action with other on both objects.

	if (call12 && TLogic.callElementBlock(request, response, currentObject1Context, "ONACTIONWITHOTHER", [actionValue]))
		return;
	if (call21 && TLogic.callElementBlock(request, response, currentObject2Context, "ONACTIONWITHOTHER", [actionValue]))
		return;

	// if we STILL can't do it...
	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};


//##[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

