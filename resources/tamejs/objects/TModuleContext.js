/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/****************************************************
 See net.mtrop.tame.lang.Block
 ****************************************************/
var TModuleContext = function(module)
{
	this.module = module,		// module reference
	this.state = {};			// savable state.
	
	state.player = null;		// current player
	state.elements = {}; 		// element-to-variables
	state.owners = {}; 			// element-to-objects
	state.objectOwners = {};	// object-to-element
	state.roomStacks = {};		// player-to-rooms
	state.names = {};			// object-to-names
	state.tags = {};			// object-to-tags
	
	// create object contexts.
	Util.each(module.objects, function(element, identity){
		if (out.elements[identity])
			throw new TAMEError(TAMEError.Type.Module, "Another element already has the identity "+element.identity);
		state.elements[identity] = {"identity": identity, "variables": {}};

		state.names[identity] = {};
		Util.each(element.names, function(name){
			state.names[identity][name] = true;
		});
		Util.each(element.tags, function(tag){
			state.tags[identity][tag] = true;
		});
	});
	
	var CONTEXTFUNC = function(element, identity){
		if (state.elements[identity])
			throw new TAMEError(TAMEError.Type.Module, "Another element already has the identity "+element.identity);
		state.elements[identity] = {"identity": identity, "variables": {}};
	};
	
	// create player contexts.
	Util.each(module.players, CONTEXTFUNC);
	// create room contexts.
	Util.each(module.rooms, CONTEXTFUNC);
	// create container contexts.
	Util.each(module.containers, CONTEXTFUNC);
	// create world context.
	state.elements["world"] = {"identity": "world", "variables": {}};
	
};

/**
 * Gets a value by variable name from a specific element contextState.
 * Variable names are converted to lower-case - they resolve case-insensitively.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @return (TValue) the corresponding value or BOOLEAN[false] if not found.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.getValue = function(elementIdentity, variableName)
{
	var contextState = this.state;
	
	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementName);
	
	var ectx = contextState.elements[elementIdentity];
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
 * Sets a value by variable name from a specific element contextState.
 * Variable names are converted to lower-case - they resolve case-insensitively.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @param value (TValue) the new value.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.setValue = function(elementIdentity, variableName, value)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementIdentity);
	
	var ectx = contextState.elements[elementIdentity];
	
	if (ectx.identity !== elementIdentity)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context mismatch detected for: "+elementIdentity+", got "+ectx.identity);
	if (!ectx.variables)
		throw new TAMEError(TAMEError.Type.ModuleState, "Element Context \""+elementIdentity+"\" is missing required member: variables");
	
	ectx.variables[variableName.toLowerCase()] = value;
};


/**
 * 
 * Removes a player from all rooms.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.removePlayer = function(playerIdentity) 
{
	var contextState = this.state;

	if (!context)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is invalid or null");
	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if (!contextState.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+playerIdentity);
	delete contextState.roomStacks[playerIdentity];
};

/**
 * Pushes a room onto a player room stack.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.pushRoomOntoPlayer = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+roomIdentity);
	
	if (!contextState.roomStacks[playerIdentity])
		contextState.roomStacks[playerIdentity] = [];
	contextState.roomStacks[playerIdentity].push(roomIdentity);
};
	
/**
 * Pops a room off of a player room stack.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.popRoomFromPlayer = function(playerIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+playerIdentity);
	
	if (!contextState.roomStacks[playerIdentity])
		return;

	contextState.roomStacks[playerIdentity].pop();
	if (!contextState.roomStacks[playerIdentity].length)
		delete contextState.roomStacks[playerIdentity];
};

/**
 * Gets the current room identity for a player.
 * @param playerIdentity the player identity.
 * @return room identity, or null if no current room.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.getCurrentRoom = function(playerIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+playerIdentity);

	if (!contextState.roomStacks[playerIdentity])
		return null;

	var len = contextState.roomStacks[playerIdentity].length;
	return contextState.roomStacks[playerIdentity][len - 1];
};

/**
 * Checks if a player is in a room (or if the room is in the player's room stack).
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @return true if the room is in the player's stack, false if not, or the player is in no room.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.checkPlayerIsInRoom = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+roomIdentity);
	
	var roomstack = contextState.roomStacks[playerIdentity];
	if (!roomstack)
		return false;
	else
		roomstack.indexOf(roomIdentity) >= 0;
};

/**
 * Removes an object from its owner.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.removeObject = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: owners");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	var elementIdentity = contextState.objectOwners[objectIdentity];
	if (!elementIdentity)
		return;
	
	delete contextState.objectOwners[objectIdentity];
	Util.arrayRemove(contextState.owners[elementIdentity], objectIdentity);
};

/**
 * Adds an object to an element.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.addObjectToElement = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	TModuleContext.prototype.removeObject(context, objectIdentity);
	contextState.objectOwners[objectIdentity] = elementIdentity;
	
	if (!contextState.owners[elementIdentity])
		contextState.owners[elementIdentity] = [];
	contextState.owners[elementIdentity].push(objectIdentity);
};

/**
 * Checks if an object is owned by an element.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @return true if the element is the owner of the object, false if not.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.checkElementHasObject = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: objectOwners");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	return contextState.objectOwners[objectIdentity] == elementIdentity;
};


/**
 * Checks if an object has no owner.
 * @param objectIdentity the object identity.
 * @return true if the object is owned by nobody, false if it has an owner.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.checkObjectHasNoOwner = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: objectOwners");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	return !contextState.objectOwners[objectIdentity];
};

/**
 * Gets a list of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.getObjectsOwnedByElement = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementIdentity);
	
	var arr = contextState.owners[elementIdentity];
	if (!arr)
		return [];
	else
		return arr.splice(); // return copy of full array.
};

/**
 * Gets a count of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.getObjectsOwnedByElementCount = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+elementIdentity);
	
	var arr = contextState.owners[elementIdentity];
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
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.addObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	var n = Util.replaceAll(n.toLowerCase(), "\\s+", " ");
	var arr = contextState.names[objectIdentity];
	if (!arr)
		arr = contextState.names[objectIdentity] = {};
	if (!arr[n])
		arr[n] = true;
};

/**
 * Removes an interpretable name from an object.
 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
 * Does nothing if the object does not have the name.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.removeObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);

	var n = Util.replaceAll(n.toLowerCase(), "\\s+", " ");
	var arr = contextState.names[objectIdentity];
	if (!arr)
		return;
	if (arr[n])
		delete arr[n];
};

/**
 * Checks for an interpretable name on an object.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.checkObjectHasName = function(objectIdentity, name) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.names)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);

	var arr = contextState.names[objectIdentity];
	return (!arr && arr[name]);
};

/**
 * Adds a tag to an object.
 * Unlike names, tags undergo no conversion.
 * Does nothing if the object already has the tag.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.addObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);
	
	var arr = contextState.tags[objectIdentity];
	if (!arr)
		arr = contextState.tags[objectIdentity] = {};
	if (!arr[tag])
		arr[tag] = true;
};

/**
 * Removes a tag from an object.
 * Unlike names, tags undergo no conversion.
 * Does nothing if the object does not have the tag.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.removeObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);

	var arr = contextState.tags[objectIdentity];
	if (!arr)
		return;
	if (arr[tag])
		delete arr[tag];
};

/**
 * Checks for a tag on an object.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element contextState.
 */
TModuleContext.prototype.checkObjectHasTag = function(objectIdentity, name) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: elements");
	if(!contextState.tags)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context state: "+objectIdentity);

	var arr = contextState.tags[objectIdentity];
	return (!arr && arr[name]);
};

/*
	resolveCurrentPlayer()
	resolveCurrentPlayerContext()
	resolveCurrentRoom()
	resolveCurrentRoomContext()
	resolveAction(identity)
	resolveElement(identity)
	resolveElementContext(identity)
	resolveElementVariableValue(identity, variable)
	resolveBlock(identity, blockType, blockValues)
*/


//TODO: Finish this.

//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModuleContext;
// =========================================================================
