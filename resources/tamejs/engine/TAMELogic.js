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
var TCommandFunctions = TCommandFunctions || ((typeof require) !== 'undefined' ? require('./logic/TCommandFunctions.js') : null);
var TStringBuilder = TStringBuilder || ((typeof require) !== 'undefined' ? require('./TStringBuilder.js') : null);
// ======================================================================================================

//##[[EXPORTJS-START

var TLogic = {};

//##[[EXPORTJS-INCLUDE logic/TArithmeticFunctions.js
//##[[EXPORTJS-INCLUDE logic/TCommandFunctions.js

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
 * Turns a command into a readable string.
 * @param cmdObject (Object) the command object.
 * @return a string.
 */
TLogic.commandToString = function(cmdObject)
{
	var sb = new TStringBuilder();
	sb.append(TCommandFunctions[cmdObject.opcode].name);
	if (cmdObject.operand0 != null)
		sb.append(' ').append(TValue.toString(cmdObject.operand0));
	if (cmdObject.operand1 != null)
		sb.append(' ').append(TValue.toString(cmdObject.operand1));
	if (cmdObject.initBlock != null)
		sb.append(" [INIT]");
	if (cmdObject.conditionalBlock != null)
		sb.append(" [CONDITIONAL]");
	if (cmdObject.stepBlock != null)
		sb.append(" [STEP]");
	if (cmdObject.successBlock != null)
		sb.append(" [SUCCESS]");
	if (cmdObject.failureBlock != null)
		sb.append(" [FAILURE]");
	return sb.toString();
};

/**
 * Turns an element into a readable string.
 * @param elemObject (Object) the command object.
 * @return a string.
 */
TLogic.elementToString = function(elemObject)
{
	return elemObject.tameType + "[" + elemObject.identity + "]";
};

/**
 * Executes a block of commands.
 * @param block (Array) the block of commands.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeBlock = function(block, request, response, blockLocal)
{
	response.trace(request, "Start block.");
	Util.each(block, function(command){
		response.trace(request, "CALL "+TLogic.commandToString(command));
		TLogic.executeCommand(request, response, blockLocal, command);
	});
	response.trace(request, "End block.");
};

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param command (Object) the command object.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeCommand = function(request, response, blockLocal, command)
{
	TCommandFunctions[command.opcode].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted(request.moduleContext.commandRunawayMax);
}

/**
 * Calls the conditional block on a command, returning the result as a .
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param command (Object) the command object.
 * @return true if result is equivalent to true, false if not.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.callConditional = function(commandName, request, response, blockLocal, command)
{
	// block should contain arithmetic commands and a last push.
	var conditional = command.conditionalBlock;
	if (!conditional)
		throw TAMEError.ModuleExecution("Conditional block for "+commandName+" does NOT EXIST!");
	
	response.trace(request, "Calling "+commandName+" conditional...");
	TLogic.executeBlock(conditional, request, response, blockLocal);

	// get remaining expression value.
	var value = request.popValue();
	
	if (!TValue.isLiteral(value))
		throw TAMEError.UnexpectedValueType("Expected literal type after "+commandName+" conditional block execution.");

	var result = TValue.asBoolean(value);
	response.trace(request, "Result "+TValue.toString(value)+" evaluates "+result+".");
	return result;
}


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
	if (action == null)
	{
		response.trace(request, "Performing unknown command.");
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
					response.trace(request, "Performing general action "+action.identity+", but has extra tokens!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.create(action));
					return true;
				}
			}

			case TAMEConstants.ActionType.OPEN:
			{
				if (!interpreterContext.targetLookedUp)
				{
					response.trace(request, "Performing open action "+action.identity+" with no target (incomplete)!");
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createModal(action, interpreterContext.target));
					return true;
				}
			}

			case TAMEConstants.ActionType.MODAL:
			{
				if (!interpreterContext.modeLookedUp)
				{
					response.trace(request, "Performing modal action "+action.identity+" with no mode (incomplete)!");
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.mode == null)
				{
					response.trace(request, "Performing modal action "+action.identity+" with an unknown mode!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, "Performing modal action "+action.identity+", but has extra tokens!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createModal(action, interpreterContext.mode));
					return true;
				}
			}

			case TAMEConstants.ActionType.TRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, "Ambiguous command for action "+action.identity+".");
					if (!TLogic.callAmbiguousCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, "Performing transitive action "+action.identity+" with no object (incomplete)!");
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 == null)
				{
					response.trace(request, "Performing transitive action "+action.identity+" with an unknown object!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, "Performing transitive action "+action.identity+", but has extra tokens!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createObject(action, interpreterContext.object1));
					return true;
				}
			}
	
			case TAMEConstants.ActionType.DITRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, "Ambiguous command for action "+action.identity+".");
					if (!TLogic.callAmbiguousCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with no first object (incomplete)!");
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 == null)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown first object!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.conjugateLookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" as a transitive one...");
					request.addActionItem(TAction.createObject(action, interpreterContext.object1));
					return true;
				}
				else if (!interpreterContext.conjugateFound)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown conjugate!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object2LookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with no second object (incomplete)!");
					if (!TLogic.callIncompleteCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object2 == null)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown second object!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else if (action.strict && interpreterContext.tokenOffset < interpreterContext.tokens.length)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+", but has extra tokens!");
					if (!TLogic.callMalformedCommand(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createObject2(action, interpreterContext.object1, interpreterContext.object2));
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
 * @param tameAction (TAction) the action to process.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processAction = function(request, response, tameAction) 
{
	try {
		
		switch (tameAction.action.type)
		{
			default:
			case TAMEConstants.ActionType.GENERAL:
				TLogic.doActionGeneral(request, response, tameAction.action);
				break;
			case TAMEConstants.ActionType.OPEN:
				TLogic.doActionOpen(request, response, tameAction.action, tameAction.target);
				break;
			case TAMEConstants.ActionType.MODAL:
				TLogic.doActionModal(request, response, tameAction.action, tameAction.target);
				break;
			case TAMEConstants.ActionType.TRANSITIVE:
				TLogic.doActionTransitive(request, response, tameAction.action, tameAction.object1);
				break;
			case TAMEConstants.ActionType.DITRANSITIVE:
				if (tameAction.object2 == null)
					TLogic.doActionTransitive(request, response, tameAction.action, tameAction.object1);
				else
					TLogic.doActionDitransitive(request, response, tameAction.action, tameAction.object1, tameAction.object2);
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
TLogic.doAllActionItems = function(request, response) 
{
	while (request.hasActionItems())
		TLogic.processAction(request, response, request.nextActionItem());
};

/**
 * Does an action loop: this keeps processing queued actions 
 * until there is nothing left to process.
 * @param request the request object.
 * @param response the response object.
 * @param afterSuccessfulCommand if true, executes the "after successful command" block.
 * @param afterFailedCommand if true, executes the "after failed command" block.
 * @param afterEveryCommand if true, executes the "after every command" block.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processActionLoop = function(request, response, afterSuccessfulCommand, afterFailedCommand, afterEveryCommand) 
{
	TLogic.doAllActionItems(request, response);
	if (afterSuccessfulCommand)
	{
		TLogic.doAfterSuccessfulCommand(request, response);
		TLogic.doAllActionItems(request, response);
	}
	if (afterFailedCommand)
	{
		TLogic.doAfterFailedCommand(request, response);
		TLogic.doAllActionItems(request, response);
	}
	if (afterEveryCommand)
	{
		TLogic.doAfterEveryCommand(request, response);
		TLogic.doAllActionItems(request, response);
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
		TLogic.processActionLoop(request, response, false, false, false);
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
	var interpreterContext = TLogic.interpret(context, TLogic.tokenizeInput(context, inputMessage));
	response.interpretNanos = Util.nanoTime() - time; 

	time = Util.nanoTime();
	
	try 
	{
		var good = TLogic.enqueueInterpretedAction(request, response, interpreterContext);
		TLogic.processActionLoop(request, response, good, !good, true);
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
 * Creates a viable blocklocal object for use in callBlock.
 * @param localValues (object) map of name to value. 
 */
TLogic.createBlockLocal = function(request, response, localValues)
{
	var out = {};
	// set locals
	Util.each(localValues, function(value, key){
		response.trace(request, "Setting local variable \""+key+"\" to \""+TValue.toString(value)+"\"");
		TLogic.setValue(out, key, value);
	});

	return out;
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
	response.trace(request, "Pushing Context:"+elementContext.identity+"...");
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
		response.trace(request, "Popping Context:"+elementContext.identity+"...");
		request.popContext();
	}
	
	if (!isFunctionBlock)
		request.checkStackClear();
	
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
	if (entry == null)
		throw TAMEError.Module("No such function ("+functionName+") in lineage of element " + TLogic.elementToString(element));

	response.trace(request, "Calling function \""+functionName+"\"...");
	var blockLocal = {};
	var args = entry.arguments;
	for (var i = args.length - 1; i >= 0; i--)
	{
		var localValue = request.popValue();
		response.trace(request, "Setting local variable \""+args[i]+"\" to \""+TValue.toString(localValue)+"\"");
		blockLocal[args[i]] = localValue;
	}
	
	response.incrementAndCheckFunctionDepth(request.moduleContext.functionDepthMax);
	TLogic.callBlock(request, response, originContext, entry.block, true, blockLocal);
	response.decrementFunctionDepth();

	return TLogic.getValue(blockLocal, TAMEConstants.RETURN_VARIABLE);
}


/**
 * Interprets the input on the request.
 * @param context (TModuleContext) the module context.
 * @param tokens (string) the input tokens.
 * @return a new interpreter context using the input.
 */
TLogic.interpret = function(context, tokens)
{
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

	TLogic.interpretAction(context, interpreterContext);

	var action = interpreterContext.action;
	if (action == null)
		return interpreterContext;

	switch (action.type)
	{
		default:
		case TAMEConstants.ActionType.GENERAL:
			return interpreterContext;
		case TAMEConstants.ActionType.OPEN:
			TLogic.interpretOpen(interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.MODAL:
			TLogic.interpretMode(action, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.TRANSITIVE:
			TLogic.interpretObject1(context, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.DITRANSITIVE:
			if (TLogic.interpretObject1(context, interpreterContext))
				if (TLogic.interpretConjugate(action, interpreterContext))
					TLogic.interpretObject2(context, interpreterContext);
			return interpreterContext;
	}
	
};

/**
 * Interprets an action from the input line.
 * @param moduleContext (TModuleContext) the module context.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretAction = function(moduleContext, interpreterContext)
{
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

		var next = module.getActionByName(sb.toString());
		if (next != null)
		{
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
TLogic.interpretMode = function(action, interpreterContext)
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
		
		if (action.extraStrings.indexOf(next) >= 0)
		{
			interpreterContext.mode = next;
			interpreterContext.tokenOffset = index;
		}
		
	}
	
};

/**
 * Interprets open target.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretOpen = function(interpreterContext)
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
	interpreterContext.target = out.length > 0 ? out : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretConjugate = function(action, interpreterContext)
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
		if (action.extraStrings.indexOf(sb.toString()) >= 0)
		{
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
TLogic.interpretObject1 = function(moduleContext, interpreterContext)
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
		
		interpreterContext.object1LookedUp = true;
		var out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.objects, 0);
		if (out > 1)
		{
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object1 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object1 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object1 != null;
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
TLogic.interpretObject2 = function(moduleContext, interpreterContext)
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
		
		interpreterContext.object2LookedUp = true;
		var out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.objects, 0);
		if (out > 1)
		{
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object2 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object2 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object2 != null;
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
	var context = request.moduleContext;
	var world = context.getElement('world');

	response.trace(request, "Check world for "+objectIdentity+"...");
	if (context.checkElementHasObject(world.identity, objectIdentity))
	{
		response.trace(request, "Found.");
		return true;
	}

	response.trace(request, "Check "+playerIdentity+" for "+objectIdentity+"...");
	if (context.checkElementHasObject(playerIdentity, objectIdentity))
	{
		response.trace(request, "Found.");
		return true;
	}

	var currentRoom = context.getCurrentRoom(playerIdentity);
	
	if (currentRoom != null)
	{
		response.trace(request, "Check "+currentRoom.identity+" for "+objectIdentity+"...");
		if (context.checkElementHasObject(currentRoom.identity, objectIdentity))
		{
			response.trace(request, "Found.");
			return true;
		}
	}
	
	response.trace(request, "Not found.");
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
	response.trace(request, "Function is " + operator.name);
	
	if (operator.binary)
	{
		var v2 = request.popValue();
		var v1 = request.popValue();
		request.pushValue(operator.doOperation(v1, v2));
	}
	else
	{
		var v1 = request.popValue();
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
	response.trace(request, "Setting current player to " + playerIdentity);
	context.setCurrentPlayer(playerIdentity);
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
	response.trace(request, "Popping top room from "+playerIdentity+".");
	context.popRoomFromPlayer(playerIdentity);
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
	response.trace(request, "Pushing "+roomIdentity+" on "+playerIdentity+".");
	context.pushRoomOntoPlayer(playerIdentity, roomIdentity);
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
	response.trace(request, "Leaving rooms for "+playerIdentity+".");

	// pop all rooms on the stack.
	while (context.getCurrentRoom(playerIdentity) != null)
		TLogic.doRoomPop(request, response, playerIdentity);

	// push new room on the stack and call focus.
	TLogic.doRoomPush(request, response, playerIdentity, roomIdentity);
};

/**
 * Attempts to perform a player browse.
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
	var blockEntryTypeName = null;
	
	if (element.tameType === 'TContainer')
		blockEntryTypeName = 'ONCONTAINERBROWSE';
	else if (element.tameType === 'TPlayer')
		blockEntryTypeName = 'ONPLAYERBROWSE';
	else if (element.tameType === 'TRoom')
		blockEntryTypeName = 'ONROOMBROWSE';
	else if (element.tameType === 'TWorld')
		blockEntryTypeName = 'ONWORLDBROWSE';
	else
		throw TAMEError.UnexpectedValueType("INTERNAL ERROR IN BROWSE.");

	response.trace(request, "Start browse "+TLogic.elementToString(element)+".");

	Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity)
	{
		var object = context.getElement(objectIdentity);
		var objectContext = context.getElementContext(objectIdentity);
		
		if (tag != null && !context.checkObjectHasTag(objectIdentity, tag))
			return;
		
		var objtostr = TLogic.elementToString(object);
		response.trace(request, "Check "+objtostr+" for browse block.");
		var block = context.resolveBlock(objectIdentity, blockEntryTypeName);
		if (block != null)
		{
			response.trace(request, "Found! Calling "+blockEntryTypeName+" block.");
			TLogic.callBlock(request, response, objectContext, block);
		}
	});
};

/**
 * Attempts to call the after successful command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterSuccessfulCommand = function(request, response)
{
	response.trace(request, "Finding \"after successful command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTERSUCCESSFULCOMMAND")) != null)
	{
		response.trace(request, "Found \"after successful command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after successful command\" block to call.");
};

/**
 * Attempts to call the after failed command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterFailedCommand = function(request, response)
{
	response.trace(request, "Finding \"after failed command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTERFAILEDCOMMAND")) != null)
	{
		response.trace(request, "Found \"after failed command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after failed command\" block to call.");
};

/**
 * Attempts to call the after every command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterEveryCommand = function(request, response)
{
	response.trace(request, "Finding \"after every command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTEREVERYCOMMAND")) != null)
	{
		response.trace(request, "Found \"after every command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after every command\" block to call.");
};

/**
 * Call after module init block on the world.
 */
TLogic.callStartBlock = function(request, response)
{
	var context = request.moduleContext;
	response.trace(request, "Attempt to call start block on world.");
	var worldContext = context.getElementContext('world');

	if ((initBlock = context.resolveBlock('world', "START")) != null)
	{
		response.trace(request, "Calling start block from Context:"+worldContext.identity+".");
		TLogic.callBlock(request, response, worldContext, initBlock);
	}
	else
	{
		response.trace(request, "No start block on world.");
	}
};

/**
 * Call init on a single context.
 */
TLogic.callInitBlock = function(request, response, context)
{
	var elementIdentity = context.identity;
	response.trace(request, "Attempt init from Context:"+elementIdentity+".");
	var element = request.moduleContext.resolveElement(elementIdentity);
	
	var initBlock = request.moduleContext.resolveBlock(elementIdentity, "INIT");
	if (initBlock != null)
	{
		response.trace(request, "Calling init block from Context:"+elementIdentity+".");
		TLogic.callBlock(request, response, context, initBlock);
	}
	else
	{
		response.trace(request, "No init block.");
	}
};

/**
 * Call init on iterable contexts.
 */
TLogic.callInitOnContexts = function(request, response, contextList)
{
	Util.each(contextList, function(context)
	{
		TLogic.callInitBlock(request, response, context);
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
	
	response.trace(request, "Starting init...");

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
		TLogic.callInitBlock(request, response, context.resolveElementContext("world"));
		TLogic.callStartBlock(request, response);
	} catch (err) {
		// catch finish interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.Finish)
			throw err;
	}
};

/**
 * Calls the appropriate ambiguous command blocks if they exist on the world.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldAmbiguousCommandBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	var blockToCall = null;

	// get specific block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSCOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific ambiguous command block on world for action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSCOMMAND")) != null)
	{
		response.trace(request, "Found default ambiguous command block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	return false;
};

/**
 * Calls the appropriate ambiguous command blocks if they exist on a player.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param playerContext the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerAmbiguousCommandBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;
	var blockToCall = null;
	
	// get specific block on player.
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONAMBIGUOUSCOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific ambiguous command block in player "+playerContext.identity+" lineage for action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	// get block on player.
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONAMBIGUOUSCOMMAND")) != null)
	{
		response.trace(request, "Found default ambiguous command block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}
	
	return false;
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
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	if (currentPlayerContext != null && TLogic.callPlayerAmbiguousCommandBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	return TLogic.callWorldAmbiguousCommandBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate malformed command block on the world if it exists.
 * Malformed commands are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldMalformedCommandBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	var world = context.getElement('world');
	
	var blockToCall = null;

	if ((blockToCall = context.resolveBlock('world', "ONMALFORMEDCOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific malformed command block on world with action %s.", action.getIdentity());
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONMALFORMEDCOMMAND")) != null)
	{
		response.trace(request, "Found default malformed command block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No malformed command block on world.");
	return false;
};

/**
 * Calls the appropriate malformed command block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param playerContext the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerMalformedCommandBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONMALFORMEDCOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific malformed command block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONMALFORMEDCOMMAND")) != null)
	{
		response.trace(request, "Found default malformed command block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No malformed command block on player.");
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
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	// try malformed command on player.
	if (currentPlayerContext != null && TLogic.callPlayerMalformedCommandBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try malformed command on world.
	return TLogic.callWorldMalformedCommandBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate incomplete command block on the world if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldIncompleteCommandBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock('world', "ONINCOMPLETECOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific incomplete command block on world, action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONINCOMPLETECOMMAND")) != null)
	{
		response.trace(request, "Found default incomplete command block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No incomplete command block on world.");
	return false;
};

/**
 * Calls the appropriate incomplete command block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerIncompleteCommandBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONINCOMPLETECOMMAND", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific incomplete command block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONINCOMPLETECOMMAND")) != null)
	{
		response.trace(request, "Found default incomplete command block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No incomplete command block on player.");
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
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	// try incomplete on player.
	if (currentPlayerContext != null && TLogic.callPlayerIncompleteCommandBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try incomplete on world.
	return TLogic.callWorldIncompleteCommandBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate action unhandled block on the world if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldActionUnhandledBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock('world', "ONUNHANDLEDACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action unhandled block on world, action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONUNHANDLEDACTION")) != null)
	{
		response.trace(request, "Found default action unhandled block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No action unhandled block on world.");
	return false;
};

/**
 * Calls the appropriate action unhandled block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionUnhandledBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONUNHANDLEDACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action unhandled block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONUNHANDLEDACTION")) != null)
	{
		response.trace(request, "Found default action unhandled block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No action unhandled block on player.");
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

	// try fail on player.
	if (currentPlayerContext != null && TLogic.callPlayerActionUnhandledBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try fail on world.
	return TLogic.callWorldActionUnhandledBlock(request, response, action, worldContext);
};


/**
 * Attempts to call the after request block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doAfterRequest = function(request, response)
{
	response.trace(request, "Finding after request block...");
	var context = request.moduleContext;
	var world = context.getElement('world');
	
	// get block on world.
	var blockToCall;

	if ((blockToCall = context.resolveBlock('world', 'AFTERREQUEST')) != null)
	{
		var worldContext = context.getElementContext('world');
		response.trace(request, "Found after request block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No after request block to call.");
};

/**
 * Attempts to call the unknown command blocks.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callUnknownCommand = function(request, response)
{
	response.trace(request, "Finding unknown command blocks...");
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();
		response.trace(request, "For current player "+TLogic.elementToString(currentPlayer)+"...");

		// get block on player.
		// find via inheritance.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONUNKNOWNCOMMAND"))  != null)
		{
			response.trace(request, "Found unknown command block on player.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return true;
		}
	}
	
	var worldContext = context.getElementContext('world');

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONUNKNOWNCOMMAND"))  != null)
	{
		response.trace(request, "Found unknown command block on player.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

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
	var context = request.moduleContext;
	response.trace(request, "Performing general/open action "+TLogic.elementToString(action));

	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();

			// get general action on room.
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
			{
				response.trace(request, "Found general action block on room.");
				if (openTarget != null)
				{
					// just get the first one.
					var localmap = {};
					localmap[action.extraStrings[0]] = TValue.createString(openTarget);
					TLogic.callBlock(request, response, currentRoomContext, blockToCall, false, TLogic.createBlockLocal(request, response, localmap));
				}
				else
					TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No general action block on room.");
		}
		
		// get general action on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
		{
			response.trace(request, "Found general action block on player.");
			if (openTarget != null)
			{
				// just get the first one.
				var localmap = {};
				localmap[action.extraStrings[0]] = TValue.createString(openTarget);
				TLogic.callBlock(request, response, currentPlayerContext, blockToCall, false, TLogic.createBlockLocal(request, response, localmap));
			}
			else
				TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
		
		response.trace(request, "No general action block on player.");
	}
	
	var worldContext = context.getElementContext('world');
	var world = context.getElement('world');

	// get general action on world.
	if ((blockToCall = context.resolveBlock(world.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found general action block on world.");
		if (openTarget != null)
		{
			// just get the first one.
			var localmap = {};
			localmap[action.extraStrings[0]] = TValue.createString(openTarget);
			TLogic.callBlock(request, response, worldContext, blockToCall, false, TLogic.createBlockLocal(request, response, localmap));
		}
		else
			TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

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
	var context = request.moduleContext;
	response.trace(request, "Performing modal action "+TLogic.elementToString(action)+", \""+mode+"\"");

	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();

			// get modal action on room.
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
			{
				response.trace(request, "Found modal action block on room.");
				TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No modal action block on room.");
		}
		
		// get modal action on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
		{
			response.trace(request, "Found modal action block on player.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
		
		response.trace(request, "No modal action block on player.");
	}
	
	var worldContext = context.getElementContext('world');
	var world = context.getElement('world');

	// get modal action on world.
	if ((blockToCall = context.resolveBlock(world.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
	{
		response.trace(request, "Found modal action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

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
	var context = request.moduleContext;
	response.trace(request, "Performing transitive action "+TLogic.elementToString(action)+" on "+TLogic.elementToString(object));
	
	var currentObjectContext = context.getElementContext(object.identity);
	var blockToCall = null;
	var actionValue = TValue.createAction(action.identity);
	
	// call action on object.
	if ((blockToCall = context.resolveBlock(object.identity, "ONACTION", [actionValue])) != null)
	{
		response.trace(request, "Found action block on object.");
		TLogic.callBlock(request, response, currentObjectContext, blockToCall);
		return;
	}
	
	var currentPlayerContext = context.getCurrentPlayerContext();

	// Call onActionWith(action, object) on current room, then player.
	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();
		var objectValue = TValue.createObject(object.identity);

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();

			// get on action with block on room.
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONACTIONWITH", [actionValue, objectValue])) != null)
			{
				response.trace(request, "Found \"action with\" block on lineage of room "+currentRoom.identity+".");
				TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No \"action with\" block on room.");
		}
		
		// get on action with block on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONACTIONWITH", [actionValue, objectValue])) != null)
		{
			response.trace(request, "Found \"action with\" block on lineage of player "+currentPlayer.identity+".");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
		
		response.trace(request, "No \"action with\" block on player.");
	}

	// Call onActionWithAncestor(action, object) on current room, then player.
	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();
			if (TLogic.doActionAncestorSearch(request, response, actionValue, currentRoom, object))
				return;
		}
		
		if (TLogic.doActionAncestorSearch(request, response, actionValue, currentPlayer, object))
			return;
	}

	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};


/**
 * Attempts to perform a ditransitive action for the ancestor search.
 * @param request the request object.
 * @param response the response object.
 * @param actionValue the action that is being called (value).
 * @param element the element to call the block on.
 * @param start the object to start the search from.
 * @return true if a block was found an called.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionAncestorSearch = function(request, response, actionValue, element, start)
{
	var blockToCall = null;
	var context = request.moduleContext;
	var ancestor = start.parent != null ? context.getElement(start.parent) : null;
	var elementContext = context.getElementContext(element.identity);

	while (ancestor != null)
	{
		if ((blockToCall = context.resolveBlock(element.identity, "ONACTIONWITHANCESTOR", [actionValue, TValue.createObject(ancestor.identity)])) != null)
		{
			response.trace(request, "Found \"action with ancestor\" block in element "+TLogic.elementToString(element)+" lineage - ancestor is "+TLogic.elementToString(ancestor)+".");
			TLogic.callBlock(request, response, elementContext, blockToCall);
			return true;
		}
		ancestor = ancestor.parent != null ? context.getElement(ancestor.parent) : null;
	}
	
	response.trace(request, "No matching \"action with ancestor\" block in element "+TLogic.elementToString(element)+" lineage.");
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
	var context = request.moduleContext;
	response.trace(request, "Performing ditransitive action "+TLogic.elementToString(action)+" on "+TLogic.elementToString(object1)+" with "+TLogic.elementToString(object2));

	var currentObject1Context = context.getElementContext(object1.identity);
	var currentObject2Context = context.getElementContext(object2.identity);
	var blockToCall = null;

	var actionValue = TValue.createAction(action.identity);
	
	var call12 = !action.strict || !action.reversed;
	var call21 = !action.strict || action.reversed;

	// call action on each object. one or both need to succeed for no failure.
	if (call12 && (blockToCall = context.resolveBlock(object1.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object2.identity)])) != null)
	{
		response.trace(request, "Found \"action with\" block in object "+TLogic.elementToString(object1)+" lineage with "+TLogic.elementToString(object2));
		TLogic.callBlock(request, response, currentObject1Context, blockToCall);
		return;
	}
	else
		response.trace(request, "No matching \"action with\" block in object "+TLogic.elementToString(object1)+" lineage with "+TLogic.elementToString(object2));

	if (call21 && (blockToCall = context.resolveBlock(object2.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object1.identity)])) != null)
	{
		response.trace(request, "Found \"action with\" block in object "+TLogic.elementToString(object2)+" lineage with "+TLogic.elementToString(object1));
		TLogic.callBlock(request, response, currentObject2Context, blockToCall);
		return;
	}
	else
		response.trace(request, "No matching \"action with\" block in object "+TLogic.elementToString(object2)+" lineage with "+TLogic.elementToString(object1));

	// call action with ancestor on each object. one or both need to succeed for no failure.
	if (call12 && TLogic.doActionAncestorSearch(request, response, actionValue, object1, object2))
		return;
	if (call21 && TLogic.doActionAncestorSearch(request, response, actionValue, object2, object1))
		return;
	
	// attempt action with other on both objects.
	if (call12 && (blockToCall = context.resolveBlock(object1.identity, "ONACTIONWITHOTHER", [actionValue])) != null)
	{
		response.trace(request, "Found \"action with other\" block in object "+TLogic.elementToString(object1)+" lineage.");
		TLogic.callBlock(request, response, currentObject1Context, blockToCall);
		return;
	}
	else
		response.trace(request, "No matching \"action with other\" block in object "+TLogic.elementToString(object1)+" lineage.");

	if (call21 && (blockToCall = context.resolveBlock(object2.identity, "ONACTIONWITHOTHER", [actionValue])) != null)
	{
		response.trace(request, "Found \"action with other\" block in object "+TLogic.elementToString(object2)+" lineage.");
		TLogic.callBlock(request, response, currentObject2Context, blockToCall);
		return;
	}
	else
		response.trace(request, "No matching \"action with other\" block in object "+TLogic.elementToString(object2)+" lineage.");
	
	// if we STILL can't do it...
	response.trace(request, "No blocks called in ditransitive action call.");
	if (!TLogic.callActionUnhandled(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
};


//##[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

