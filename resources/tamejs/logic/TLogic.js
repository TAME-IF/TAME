/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var TRequest = TRequest || ((typeof require) !== 'undefined' ? require('../objects/TRequest.js') : null);
var TResponse = TResponse || ((typeof require) !== 'undefined' ? require('../objects/TResponse.js') : null);
// ======================================================================================================

//##[[CONTENT-START

var TLogic = {};

/**
 * Gets a value by variable name from a specific element context.
 * Variable names are converted to lower-case - they resolve case-insensitively.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @return (TValue) the corresponding value or BOOLEAN[false] if not found.
 * @throws TAMEError if no such element context.
 */
TLogic.getValue = function(context, elementIdentity, variableName)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementName);
	
	var ectx = context.elements[elementIdentity];
	if (ectx.identity !== elementIdentity)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context mismatch detected for: "+elementIdentity+", got "+ectx.identity);
	if (!ectx.variables)
		throw new TAMEError(TAMEError.Type.ModuleState, "Element Context \""+elementIdentity+"\" is missing required member: variables");

	var out = ectx.variables[variableName.toLowerCase()];
	if (out != null)
		return out;
	else
		return TValue.createBoolean(false);
};

/**
 * Sets a value by variable name from a specific element context.
 * Variable names are converted to lower-case - they resolve case-insensitively.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @param value (TValue) the new value.
 * @throws TAMEError if no such element context.
 */
TLogic.setValue = function(context, elementIdentity, variableName, value)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	
	var ectx = context.elements[elementIdentity];
	
	if (ectx.identity !== elementIdentity)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context mismatch detected for: "+elementIdentity+", got "+ectx.identity);
	if (!ectx.variables)
		throw new TAMEError(TAMEError.Type.ModuleState, "Element Context \""+elementIdentity+"\" is missing required member: variables");
	
	ectx.variables[variableName.toLowerCase()] = value;
};


/**
 * 
 * Removes a player from all rooms.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TLogic.removePlayer = function(context, playerIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);
	delete context.roomStacks[playerIdentity];
};

/**
 * Pushes a room onto a player room stack.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @throws TAMEError if no such element context.
 */
TLogic.pushRoomOntoPlayer = function(context, playerIdentity, roomIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: roomStacks");
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);
	if (!context.elements[roomIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+roomIdentity);
	
	if (!context.roomStacks[playerIdentity])
		context.roomStacks[playerIdentity] = [];
	context.roomStacks[playerIdentity].push(roomIdentity);
};
	
/**
 * Pops a room off of a player room stack.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such element context.
 */
TLogic.popRoomFromPlayer = function(context, playerIdentity)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: roomStacks");
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);
	
	if (!context.roomStacks[playerIdentity])
		return;

	context.roomStacks[playerIdentity].pop();
	if (!context.roomStacks[playerIdentity].length)
		delete context.roomStacks[playerIdentity];
};

/**
 * Gets the current room identity for a player.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @return room identity, or null if no current room.
 * @throws TAMEError if no such element context.
 */
TLogic.getCurrentRoom = function(context, playerIdentity)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: roomStacks");
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);

	if (!context.roomStacks[playerIdentity])
		return null;

	var len = context.roomStacks[playerIdentity].length;
	return context.roomStacks[playerIdentity][len - 1];
};

/**
 * Checks if a player is in a room (or if the room is in the player's room stack).
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @return true if the room is in the player's stack, false if not, or the player is in no room.
 * @throws TAMEError if no such element context.
 */
TLogic.checkPlayerIsInRoom = function(context, playerIdentity, roomIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: roomStacks");
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);
	if (!context.elements[roomIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+roomIdentity);
	
	var roomstack = context.roomStacks[playerIdentity];
	if (!roomstack)
		return false;
	else
		roomstack.indexOf(roomIdentity) >= 0;
};

/**
 * Removes an object from its owner.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TLogic.removeObject = function(context, objectIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: objectOwners");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	var elementIdentity = context.objectOwners[objectIdentity];
	if (!elementIdentity)
		return;
	
	delete context.objectOwners[objectIdentity];
	Util.arrayRemove(context.owners[elementIdentity], objectIdentity);
};

/**
 * Adds an object to an element.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TLogic.addObjectToElement = function(context, elementIdentity, objectIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: objectOwners");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	TLogic.removeObject(context, objectIdentity);
	context.objectOwners[objectIdentity] = elementIdentity;
	
	if (!context.owners[elementIdentity])
		context.owners[elementIdentity] = [];
	context.owners[elementIdentity].push(objectIdentity);
};

/**
 * Checks if an object is owned by an element.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @return true if the element is the owner of the object, false if not.
 * @throws TAMEError if no such element context.
 */
TLogic.checkElementHasObject = function(context, elementIdentity, objectIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: objectOwners");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	return context.objectOwners[objectIdentity] == elementIdentity;
};


/**
 * Checks if an object has no owner.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @return true if the object is owned by nobody, false if it has an owner.
 * @throws TAMEError if no such element context.
 */
TLogic.checkObjectHasNoOwner = function(context, objectIdentity) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: objectOwners");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	return !context.objectOwners[objectIdentity];
};

/**
 * Gets a list of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element context.
 */
TLogic.getObjectsOwnedByElement = function(context, elementIdentity)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	
	var arr = context.owners[elementIdentity];
	if (!arr)
		return [];
	else
		return arr.splice(); // return copy of full array.
};

/**
 * Gets a count of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param context the module context.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element context.
 */
TLogic.getObjectsOwnedByElementCount = function(context, elementIdentity)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	
	var arr = context.owners[elementIdentity];
	if (!arr)
		return 0;
	else
		return arr.length;
};

/**
 * Adds a interpretable name to an object.
 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
 * Does nothing if the object already has the name.
 * More than one object with this name can result in "ambiguous" actions!
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TLogic.addObjectName = function(context, objectIdentity, name)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: names");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	var n = Util.replaceAll(n.toLowerCase(), "\\s+", " ");
	var arr = context.names[objectIdentity];
	if (!arr)
		arr = context.names[objectIdentity] = {};
	if (!arr[n])
		arr[n] = true;
};

/**
 * Removes an interpretable name from an object.
 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
 * Does nothing if the object does not have the name.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TLogic.removeObjectName = function(context, objectIdentity, name)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: names");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);

	var n = Util.replaceAll(n.toLowerCase(), "\\s+", " ");
	var arr = context.names[objectIdentity];
	if (!arr)
		return;
	if (arr[n])
		delete arr[n];
};

/**
 * Checks for an interpretable name on an object.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TLogic.checkObjectHasName = function(context, objectIdentity, name) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: names");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);

	var arr = context.names[objectIdentity];
	return (!arr && arr[name]);
};

/**
 * Adds a tag to an object.
 * Unlike names, tags undergo no conversion.
 * Does nothing if the object already has the tag.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TLogic.addObjectTag = function(context, objectIdentity, tag)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: tags");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	var arr = context.tags[objectIdentity];
	if (!arr)
		arr = context.tags[objectIdentity] = {};
	if (!arr[tag])
		arr[tag] = true;
};

/**
 * Removes a tag from an object.
 * Unlike names, tags undergo no conversion.
 * Does nothing if the object does not have the tag.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TLogic.removeObjectTag = function(context, objectIdentity, tag)
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: tags");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);

	var arr = context.tags[objectIdentity];
	if (!arr)
		return;
	if (arr[tag])
		delete arr[tag];
};

/**
 * Checks for a tag on an object.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TLogic.checkObjectHasTag = function(context, objectIdentity, name) 
{
	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!context.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: elements");
	if(!context.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: tags");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);

	var arr = context.tags[objectIdentity];
	return (!arr && arr[name]);
};


/**
 * Handles initializing a context. Must be called after a new context and game is started.
 * @param context the module context.
 * @param tracing if true, add trace cues.
 * @return (TResponse) the response from the initialize.
 */
TLogic.handleInit = function(context, tracing) 
{
	var request = TRequest.create(context, "[INITIALIZE]", tracing);
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
	var request = TRequest.create(context, inputMessage, tracing);
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
 * @param block (TBlock) the block to execute.
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
		block.execute(request, response, blockLocal);
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
	var tokens = request.inputMessage.toLowerCase().split("\\s+");
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
 * @param moduleContext (Object) the module context.
 * @param interpreterContext (Object) the TAMEInterpreterContext.
 */
TLogic.interpretAction = function(moduleContext, interpreterContext)
{
	var module = moduleContext.module;
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
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
 * @param interpreterContext (object) the interpreterContext.
 */
TLogic.interpretMode = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
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
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretOpen = function(interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	
	while (index < tokens.length)
	{
		interpreterContext.targetLookedUp = true;
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
	}
	
	interpreterContext.target = sb.length() > 0 ? sb : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretConjugate = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	var out = false;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
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
 * @param moduleContext the module context.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretObject1 = function(moduleContext, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
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
		if (sb.length() > 0)
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

/*
Context: 
{
	"module": moduleRef
	"player": null,									// current player
	"elements": {}, 								// element-to-variables
	"owners": {elemid:[objid, ..], }, 				// element-to-objects
	"objectOwners": {objid:elemid, },   			// object-to-element
	"roomStacks": {playerid: [roomid, ..], ...},	// player-to-rooms
	"names": {objid:{name:true, ...}, ...},			// object-to-names
	"tags": {objid:{name:true, ...}, ...},			// object-to-tags
}
*/

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

