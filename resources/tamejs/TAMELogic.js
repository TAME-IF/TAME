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
 * Turns a command into a readable string.
 * @param cmdObject (Object) the command object.
 * @return a string.
 */
TLogic.commandToString = function(cmdObject)
{
	var out = TCommandFunctions[cmdObject.opcode].name;
	if (cmdObject.operand0 != null)
		out += ' ' + TValue.toString(cmdObject.operand0);
	if (cmdObject.operand1 != null)
		out += ' ' + TValue.toString(cmdObject.operand1);
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
 * Turns an element into a readable string.
 * @param elemObject (Object) the command object.
 * @return a string.
 */
TLogic.elementToString = function(elemObject)
{
	return elemObject.tameType + "[" + elemObject.identity + "]";
};

/**
 * Checks if an action is allowed on an element (player or room).
 * @param element the element to check.
 * @param action the action that is being called.
 * @return true if allowed, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.allowsAction = function(element, action)
{
	if (element.permissionType == TAMEConstants.RestrictionType.ALLOW)
		return element.permittedActionList.indexOf(action.identity) >= 0;
	else if (action.restricted)
		return false;
	else if (element.permissionType == TAMEConstants.RestrictionType.FORBID)
		return permittedActionList.indexOf(action.identity) < 0;
	else
		throw TAMEError.Module("Bad or unknown permission type found: "+permissionType);
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
	response.incrementAndCheckCommandsExecuted();
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
		var out = moduleContext.getAccessibleObjectsByName(sb, interpreterContext.objects, 0);
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
		var out = moduleContext.getAccessibleObjectsByName(sb, interpreterContext.objects, 0);
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
		throw TAMEError.UnexpectedValue("Expected arithmetic function type, got illegal value "+functionType+".");
	
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
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doBrowse = function(request, response, blockEntryTypeName, elementIdentity)
{
	var context = request.moduleContext;
	
	var element = context.resolveElement(elementIdentity);
	response.trace(request, "Start browse "+TLogic.elementToString(element)+".");

	Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity)
	{
		var object = moduleContext.getElement(objectIdentity);
		var objectContext = moduleContext.getElementContext(objectIdentity);
		
		var objtostr = TLogic.elementToString(object);
		response.trace(request, "Check "+objtostr+" for browse block.");
		var block = context.resolveBlock(objectIdentity, blockEntryTypeName, []);
		if (block != null)
		{
			response.trace(request, "Calling "+objtostr+" browse block.");
			TLogic.callBlock(request, response, objectContext, block);
		}
	});
};

/**
 * Call after module init block on the world.
 */
TLogic.callAfterModuleInitBlock = function(request, response)
{
	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');

	if ((initBlock = context.resolveBlock('world', "AFTERMODULEINIT", [])) != null)
	{
		response.trace(request, "Calling after module init block from Context:"+worldContext.identity+".");
		TLogic.callBlock(request, response, worldContext, initBlock);
	}
	else
	{
		response.trace(request, "No after module init block on world.");
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
	
	var initBlock = request.moduleContext.resolveBlock(elementIdentity, "INIT", []);
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
	
	Util.each(context.module.elements, function(element)
	{
		var elementContext = context.resolveElementContext(element.identity);
		if (element.tameType === 'TContainer')
			containerContexts.push(elementContext);
		else if (element.tameType === 'TObject')
			objectContexts.push(elementContext);
		else if (element.tameType === 'TPlayer')
			playerContexts.push(elementContext);
		else if (element.tameType === 'TRoom')
			roomContexts.push(elementContext);
	});
	
	TLogic.callInitOnContexts(request, response, containerContexts);
	TLogic.callInitOnContexts(request, response, objectContexts);
	TLogic.callInitOnContexts(request, response, roomContexts);
	TLogic.callInitOnContexts(request, response, playerContexts);
	TLogic.callInitBlock(request, response, context.resolveElementContext("world"));
	TLogic.callAfterModuleInitBlock(request, response);
};

/**
 * Attempts to call the ambiguous action blocks.
 * @param request the request object.
 * @param response the response object.
 * @param action the action used.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callAmbiguousAction = function(request, response, action)
{
	response.trace(request, "Finding ambiguous action blocks...");

	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getElement(currentPlayerContext.identity);
		response.trace(request, "For current player "+TLogic.elementToString(currentPlayer)+"...");

		// get specific block on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONAMBIGUOUSACTION", [TValue.createAction(action.identity)])) != null)
		{
			response.trace(request, "Found specific ambiguous action block in player "+currentPlayer.identity+" lineage for action "+action.identity+".");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return true;
		}

		// get block on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONAMBIGUOUSACTION", [])) != null)
		{
			response.trace(request, "Found default ambiguous action block in player "+currentPlayer.identity+" lineage.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return true;
		}
	}

	var worldContext = context.getElementContext('world');

	// get specific block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific ambiguous action block on world for action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSACTION", [])) != null)
	{
		response.trace(request, "Found default ambiguous action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	return false;
};

/**
 * Calls the appropriate bad action block on the world if it exists.
 * Bad actions are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldBadActionBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	var world = context.getElement('world');
	
	var blockToCall = null;

	if ((blockToCall = context.resolveBlock('world', "ONBADACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific bad action block on world with action %s.", action.getIdentity());
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONBADACTION", [])) != null)
	{
		response.trace(request, "Found default bad action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No bad action block on world.");
	return false;
};

/**
 * Calls the appropriate bad action block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerBadActionBlock = function(request, response, action, playerContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate bad action blocks if they exist.
 * Bad actions are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBadAction = function(request, response, action)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action forbidden block on a player.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionForbiddenBlock = function(request, response, action, playerContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action forbidden block on a room.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the room context.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callRoomActionForbiddenBlock = function(request, response, action, roomContext)
{
	// TODO: Finish this.
};

/**
 * Checks and calls the action forbidden blocks.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a forbidden block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callCheckActionForbidden = function(request, response, action)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action incomplete block on the world if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldActionIncompleteBlock = function(request, response, action, worldContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action incomplete block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionIncompleteBlock = function(request, response, action, playerContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action incomplete blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callActionIncomplete = function(request, response, action)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate bad action block on the world if it exists.
 * Bad actions are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldActionFailBlock = function(request, response, action, worldContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate bad action block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionFailBlock = function(request, response, TAction, playerContext)
{
	// TODO: Finish this.
};

/**
 * Calls the appropriate action fail blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callActionFailed = function(request, response, action)
{
	// TODO: Finish this.
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

	if ((blockToCall = context.resolveBlock('world', 'AFTERREQUEST', [])) != null)
	{
		var worldContext = context.getElementContext('world');
		response.trace(request, "Found after request block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No after request block to call.");
};

/**
 * Attempts to call the bad action blocks.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doUnknownAction = function(request, response)
{
	response.trace(request, "Finding unknown action blocks...");
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();
		response.trace(request, "For current player "+TLogic.elementToString(currentPlayer)+"...");

		// get block on player.
		// find via inheritance.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONUNKNOWNACTION", []))  != null)
		{
			response.trace(request, "Found unknown action block on player.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
	}
	
	var worldContext = context.getElementContext('world');

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONUNKNOWNACTION", []))  != null)
	{
		response.trace(request, "Found unknown action block on player.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

	response.trace(request, "No unknown action block to call. Sending error.");
	response.addCue(TAMEConstants.Cue.ERROR, "ACTION IS UNKNOWN! (make a better in-universe handler!).");
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
	var context = request.getModuleContext();
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
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONACTION", TValue.createAction(action.identity))) != null)
			{
				response.trace(request, "Found general action block on room.");
				if (openTarget != null)
				{
					// just get the first one.
					var localmap = {};
					localmap[action.extraStrings[0]] = TValue.createString(openTarget);
					TLogic.callBlock(request, response, currentRoomContext, blockToCall, localmap);
				}
				else
					TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No general action block on room.");
		}
		
		// get general action on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONACTION", TValue.createAction(action.identity))) != null)
		{
			response.trace(request, "Found general action block on player.");
			if (openTarget != null)
			{
				// just get the first one.
				var localmap = {};
				localmap[action.extraStrings[0]] = TValue.createString(openTarget);
				TLogic.callBlock(request, response, currentPlayerContext, blockToCall, localmap);
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
	if ((blockToCall = context.resolveBlock(world.identity, "ONACTION", TValue.createAction(action.identity))) != null)
	{
		response.trace(request, "Found general action block on world.");
		if (openTarget != null)
		{
			// just get the first one.
			var localmap = {};
			localmap[action.extraStrings[0]] = TValue.createString(openTarget);
			TLogic.callBlock(request, response, worldContext, blockToCall, localmap);
		}
		else
			TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

	// try fail on player.
	if (currentPlayerContext != null && TLogic.callPlayerActionFailBlock(request, response, action, currentPlayerContext))
		return;

	// try fail on world.
	TLogic.callWorldActionFailBlock(request, response, action, worldContext);
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
	var context = request.getModuleContext();
	response.trace(request, "Performing modal action "+TLogic.elementToString(action)+", \""+mode+"\"");

	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = currentPlayerContext.getElement();

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

	if (!TLogic.callActionFailed(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
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

	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	// call action on object.
	if ((blockToCall = TLogic.resolveBlock(object.identity, "ONACTION", TValue.createAction(action.identity))) != null)
	{
		response.trace(request, "Found action block on object.");
		TLogic.callBlock(request, response, currentObjectContext, blockToCall);
		return;
	}
	
	if (!TLogic.callActionFailed(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
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
	var currentObject2Context = context.getElementContext(object1.identity);
	var blockToCall = null;

	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	var success = false;
	var actionValue = TValue.createAction(action.identity);
	
	// call action on each object. one or both need to succeed for no failure.
	if ((blockToCall = context.resolveBlock(object1.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object2.identity)])) != null)
	{
		response.trace(request, "Found action block in object "+TLogic.elementToString(object1)+" lineage with "+TLogic.elementToString(object2));
		TLogic.callBlock(request, response, currentObject1Context, blockToCall);
		success = true;
	}
	if ((blockToCall = context.resolveBlock(object2.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object1.identity)])) != null)
	{
		response.trace(request, "Found action block in object "+TLogic.elementToString(object2)+" lineage with "+TLogic.elementToString(object1));
		TLogic.callBlock(request, response, currentObject2Context, blockToCall);
		success = true;
	}
	
	// attempt action with other on both objects.
	if (!success)
	{
		if ((blockToCall = context.resolveBlock(object1, "ONACTIONWITHOTHER", [actionValue])) != null)
		{
			response.trace(request, "Found action with other block in object "+TLogic.elementToString(object1)+" lineage.");
			TLogic.callBlock(request, response, currentObject1Context, blockToCall);
			success = true;
		}
		if ((blockToCall = context.resolveBlock(object2, "ONACTIONWITHOTHER", [actionValue])) != null)
		{
			response.trace(request, "Found action with other block in object "+TLogic.elementToString(object2)+" lineage.");
			TLogic.callBlock(request, response, currentObject2Context, blockToCall);
			success = true;
		}
	}

	// if we STILL can't do it...
	if (!success)
	{
		response.trace(request, "No blocks called in ditransitive action call.");
		if (!TLogic.callActionFailed(request, response, action))
			response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
	}
};

TLogic.enqueueInterpretedAction = function(request, response, interpreterContext) 
{
	// TODO: Finish this.
};

/**
 * Does an action loop: this keeps processing queued actions 
 * until there is nothing left to process.
 * @param request the request context.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processActionLoop = function(request, response) 
{
	// TODO: Finish this.
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

