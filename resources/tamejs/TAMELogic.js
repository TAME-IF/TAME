/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('./Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('./TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('./objects/TValue.js') : null);
var TRequest = TRequest || ((typeof require) !== 'undefined' ? require('./objects/TRequest.js') : null);
var TResponse = TResponse || ((typeof require) !== 'undefined' ? require('./objects/TResponse.js') : null);
var TArithmeticFunctions = TArithmeticFunctions || ((typeof require) !== 'undefined' ? require('./logic/TArithmeticFunctions.js') : null);
var TCommandFunctions = TCommandFunctions || ((typeof require) !== 'undefined' ? require('./logic/TCommandFunctions.js') : null);
// ======================================================================================================

//##[[CONTENT-START

var TLogic = {};

//##[[CONTENT-INCLUDE logic/TArithmeticFunctions.js
//##[[CONTENT-INCLUDE logic/TCommandFunctions.js

/****************************************************************************
 * Main logic junk.
 ****************************************************************************/

/**
 * Turns a command into a readable string.
 * @param cmdObject (Object) the command object.
 * @return a string.
 */
TLogic.commandToString = function(cmdObject)
{
	var out = TCommandFunctions[cmdObject.opcode].name;
	if (cmdObject.operand0 != null)
		out += ' ' + cmdObject.operand0.toString();
	if (cmdObject.operand1 != null)
		out += ' ' + cmdObject.operand1.toString();
	if (cmdObject.initBlock != null)
		out += " [INIT]";
	if (cmdObject.conditionalBlock != null)
		out += " [CONDITIONAL]";
	if (cmdObject.stepBlock != null)
		out += " [STEP]";
	if (cmdObject.successBlock != null)
		out += " [SUCCESS]";
	if (cmdObject.failureBlock != null)
		out += " [FAILURE]";
	
	return out;
};

/**
 * Executes a block of commands.
 * @param command (Object) the command object.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeBlock = function(block, request, response, blockLocal)
{
	response.trace(request, "Start block.");
	Util.each(this.commandList, function(command){
		response.trace(request, "CALL "+TLogic.commandToString(command));
		TLogic.executeCommand(command, request, response, blockLocal);
	});
	response.trace(request, "End block.");
};

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param command (Object) the command object.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeCommand = function(command, request, response, blockLocal)
{
	TCommandFunctions[command.opcode].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted();
}

TLogic.callConditional()

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
	var time = Date.now();

	try 
	{
		TLogic.initializeContext(request, response);
		TLogic.processActionLoop(request, response);
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

	time = (Date.now() - time) * 1000000; // ms to ns
	
	response.requestNanos = time;

	return response;
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

	var time = Date.now();
	var interpreterContext = TLogic.interpret(request);
	response.interpretNanos = (Date.now() - time) * 1000000; 

	time = Date.now();
	
	try 
	{
		TLogic.enqueueInterpretedAction(request, response, interpreterContext);
		TLogic.processActionLoop(request, response);
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
	
	response.requestNanos = (Date.now() - time) * 1000000;
	return response;
};

/**
 * Performs the necessary tasks for calling an object block.
 * Ensures that the block is called cleanly.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param elementContext (object) the context that the block is executed through.
 * @param block [Object, ...] the block to execute.
 * @param localValues (object) the local values to set on invoke.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBlock = function(request, response, elementContext, block, localValues)
{
	response.trace(request, "Pushing Context:"+elementContext.identity+"...");
	request.pushContext(elementContext);
	
	var blockLocal = {};
	// set locals
	Util.each(localValues, function(value, key){
		response.trace(request, "Setting local variable \""+key+"\" to \""+value+"\"");
		blockLocal.put(key, value);
	});

	try {
		TLogic.executeBlock(block, request, response, blockLocal);
	} catch (t) {
		throw t;
	} finally {
		response.trace(request, "Popping Context:"+elementContext.identity+"...");
		request.popContext();
	}
	
	request.checkStackClear();
	
};


/**
 * Interprets the input on the request.
 * @param request (TRequest) the request.
 * @return a new interpreter context using the input.
 */
TLogic.interpret = function(request)
{
	var tokens = request.inputMessage.toLowerCase().split(/\s+/);
	var interpreterContext = 
	{
		"tokens": tokens,
		"tokenOffset": 0,
		"objects": [],
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

	var moduleContext = request.moduleContext;
	TLogic.interpretAction(moduleContext, interpreterContext);

	var action = moduleContext.module.actions[interpreterContext.action];
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
			TLogic.interpretObject1(moduleContext, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.DITRANSITIVE:
			if (TLogic.interpretObject1(moduleContext, interpreterContext))
				if (TLogic.interpretConjugate(action, interpreterContext))
					TLogic.interpretObject2(moduleContext, interpreterContext);
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
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;

		var next = module.getActionByName(sb);
		if (next != null)
		{
			interpreterContext.action = next.identity;
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
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;

		interpreterContext.modeLookedUp = true;
		var next = sb;
		
		if (action.extraStrings.indexOf(sb) >= 0)
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
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	
	while (index < tokens.length)
	{
		interpreterContext.targetLookedUp = true;
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
	}
	
	interpreterContext.target = sb.length > 0 ? sb : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretConjugate = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	var out = false;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.conjugateLookedUp = true;
		if (action.extraStrings.indexOf(sb) >= 0)
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
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object1LookedUp = true;
		var out = TLogic.findAccessibleObjectsByName(moduleContext, sb, interpreterContext.objects, 0);
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
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object2LookedUp = true;
		var out = TLogic.findAccessibleObjectsByName(moduleContext, sb, interpreterContext.objects, 0);
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
 * Returns all objects in the accessible area by an object name read from the interpreter.
 * The output stops if the size of the output array is reached.
 * @param moduleContext the module context.
 * @param name the name from the interpreter.
 * @param outputArray the output vector of found objects.
 * @param arrayOffset the starting offset into the array to put them.
 * @return the amount of objects found.
 */
TLogic.findAccessibleObjectsByName = function(moduleContext, name, outputArray, arrayOffset)
{
	// TODO: Finish this.
};

TLogic.enqueueInterpretedAction = function(request, response, interpreterContext) 
{
	// TODO: Finish this.
};

TLogic.processActionLoop = function(request, response) 
{
	// TODO: Finish this.
};

TLogic.initializeContext = function(request, response) 
{
	// TODO: Finish this.
};

/*
doAfterRequest(TAMERequest, TAMEResponse)
doUnknownAction(TAMERequest, TAMEResponse)
doActionGeneral(TAMERequest, TAMEResponse, TAction)
doActionOpen(TAMERequest, TAMEResponse, TAction, String)
doActionModal(TAMERequest, TAMEResponse, TAction, String)
doActionTransitive(TAMERequest, TAMEResponse, TAction, TObject)
doActionDitransitive(TAMERequest, TAMEResponse, TAction, TObject, TObject)
callAmbiguousAction(TAMERequest, TAMEResponse, TAction)
callCheckActionForbidden(TAMERequest, TAMEResponse, TAction)
callBadAction(TAMERequest, TAMEResponse, TAction)
callWorldBadActionBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerBadActionBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callActionIncomplete(TAMERequest, TAMEResponse, TAction)
callWorldActionIncompleteBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerActionIncompleteBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callActionFailed(TAMERequest, TAMEResponse, TAction)
callWorldActionFailBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerActionFailBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callPlayerActionForbiddenBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callRoomActionForbiddenBlock(TAMERequest, TAMEResponse, TAction, TRoomContext)
callInitOnContexts(TAMERequest, TAMEResponse, Iterator<? extends TElementContext<?>>)
callInitBlock(TAMERequest, TAMEResponse, TElementContext<?>)
 */

//TODO: Finish

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

