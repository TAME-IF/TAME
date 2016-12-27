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
 See net.mtrop.tame.TAMEModuleContext
 ****************************************************/
var TModuleContext = function(module)
{
	this.module = module,			// module reference
	this.state = {};				// savable state.
	
	this.state.player = null;		// current player
	this.state.elements = {}; 		// element-to-contexts
	this.state.owners = {}; 		// element-to-objects
	this.state.objectOwners = {};	// object-to-element
	this.state.roomStacks = {};		// player-to-rooms
	this.state.names = {};			// object-to-names
	this.state.tags = {};			// object-to-tags
	
	var s = this.state;
	var m = this.module;
	
	var ELEMENTCONTEXT = function(element)
	{
		var id = element.identity;
		return {
			"identity": id,
			"variables": {}
		};
	};
	
	// create element contexts.
	Util.each(m.elements, function(element, identity)
	{
		if (element.archetype)
			return;
		if (s.elements[identity])
			throw TAMEError.Module("Another element already has the identity "+identity);
		s.elements[identity] = ELEMENTCONTEXT(element);
		
		// just for objects
		if (element.tameType === 'TObject')
		{
			s.names[identity] = {};
			s.tags[identity] = {};
			Util.each(element.names, function(name)
			{
				s.names[identity][name] = true;
			});
			Util.each(element.tags, function(tag)
			{
				s.tags[identity][tag] = true;
			});		
		}
		
	});
	
};

/**
 * Sets the current player.
 * @param playerIdentity the player identity, or null.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.setCurrentPlayer = function(playerIdentity) 
{
	var contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);
	
	contextState.player = playerIdentity;
};

/**
 * Removes a player from all rooms.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.removePlayer = function(playerIdentity) 
{
	var contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);
	delete contextState.roomStacks[playerIdentity];
};

/**
 * Pushes a room onto a player room stack.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.pushRoomOntoPlayer = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
	if (!contextState.roomStacks[playerIdentity])
		contextState.roomStacks[playerIdentity] = [];
	contextState.roomStacks[playerIdentity].push(roomIdentity);
};
	
/**
 * Pops a room off of a player room stack.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.popRoomFromPlayer = function(playerIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);
	
	if (!contextState.roomStacks[playerIdentity])
		return;

	contextState.roomStacks[playerIdentity].pop();
	if (!contextState.roomStacks[playerIdentity].length)
		delete contextState.roomStacks[playerIdentity];
};

/**
 * Checks if a player is in a room (or if the room is in the player's room stack).
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @return true if the room is in the player's stack, false if not, or the player is in no room.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkPlayerIsInRoom = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");
	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
	var roomstack = contextState.roomStacks[playerIdentity];
	if (!roomstack)
		return false;
	else
		roomstack.indexOf(roomIdentity) >= 0;
};

/**
 * Gets the current player.
 * @return room identity, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentPlayer = function()
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	return contextState.player;
};

/**
 * Gets the current room. Influenced by current player.
 * @return room identity, or null if no current room (or no current player).
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentRoom = function()
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	var playerIdentity = contextState.player; 
	
	if (!playerIdentity)
		return null;

	if (!contextState.elements[playerIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+playerIdentity);

	if (!contextState.roomStacks[playerIdentity])
		return null;

	var len = contextState.roomStacks[playerIdentity].length;
	return contextState.roomStacks[playerIdentity][len - 1];
};

/**
 * Removes an object from its owner.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObject = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectToElement = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkElementHasObject = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if (!contextState.elements[elementIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	return contextState.objectOwners[objectIdentity] == elementIdentity;
};


/**
 * Checks if an object has no owner.
 * @param objectIdentity the object identity.
 * @return true if the object is owned by nobody, false if it has an owner.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasNoOwner = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	return !contextState.objectOwners[objectIdentity];
};

/**
 * Gets a list of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.getObjectsOwnedByElement = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.getObjectsOwnedByElementCount = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");
	if (!contextState.elements[elementIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);

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
 * @return true if it exists, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasName = function(objectIdentity, name) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);

	var arr = contextState.names[objectIdentity];
	return (!arr && arr[name]);
};

/**
 * Adds a tag to an object.
 * Unlike names, tags undergo no conversion.
 * Does nothing if the object already has the tag.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);

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
 * @return true if it exists, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasTag = function(objectIdentity, name) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	if (!contextState.elements[objectIdentity])
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+objectIdentity);

	var arr = contextState.tags[objectIdentity];
	return (!arr && arr[name]);
};

/**
 * Resolves an action by its action identity.
 * @param actionIdentity the action identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveAction = function(actionIdentity)
{	
	var out = this.module.actions[actionIdentity];
	if (!out)
		throw TAMEModule.ModuleExecution("Action is missing from module: "+actionIdentity);		
	return this.module.actions[actionIdentity];
};

/**
 * Resolves an element by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 * @throws TAMEInterrupt if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElement = function(elementIdentity)
{	
	var out;
	
	// current player
	if (elementIdentity === 'player')
	{
		elementIdentity = this.getCurrentPlayer();
		if (!elementIdentity)
			throw TAMEInterrupt.Error("Current player context called with no current player!");
	}
	// current room
	else if (elementIdentity === 'room')
	{
		elementIdentity = this.getCurrentRoom();
		if (!elementIdentity)
			throw TAMEInterrupt.Error("Current room context called with no current room!");
	}
	
	out = this.module.elements[elementIdentity];
	
	if (!out)
		throw TAMEModule.ModuleExecution("Element is missing from module: "+elementIdentity);
	
	return out;
};

/**
 * Resolves an element context by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 * @throws TAMEInterrupt if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElementContext = function(elementIdentity)
{	
	var out;
	
	// current player
	if (elementIdentity === 'player')
	{
		elementIdentity = this.getCurrentPlayer();
		if (!elementIdentity)
			throw TAMEInterrupt.Error("Current player context called with no current player!");
	}
	// current room
	else if (elementIdentity === 'room')
	{
		elementIdentity = this.getCurrentRoom();
		if (!elementIdentity)
			throw TAMEInterrupt.Error("Current room context called with no current room!");
	}
	
	out = this.state.elements[elementIdentity];
	
	if (!out)
		throw TAMEModule.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
	return out;
};

/**
 * Resolves an element context by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @param variable the variable name.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 * @throws TAMEInterrupt if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElementVariableValue = function(elementIdentity, variable)
{
	var value = this.resolveElementContext(elementIdentity).variables[variable.toLowerCase()];
	if (!value)
		return TValue.createBoolean(false);
	else
		return value;
};

/**
 * Resolves a qualifying code block starting from an element.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the starting element identity.
 * @param blockType the block entry type.
 * @param blockValues the values for matching the block.
 * @return the first qualifying block in the lineage, or null if no matching block.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveBlock = function(elementIdentity, blockType, blockValues)
{
	var blockname =	blockType + "(";
	for (var i = 0; i < blockValues.length; i++)
	{
		blockname += TValue.toString(values[i]);
		if (i < blockValues.length - 1)
			blockname += ",";
	}
	blockname += ")";

	var qualifyingBlock = null;
	var element = this.resolveElement(elementIdentity); 
	
	while (element)
	{
		var out = element.blockTable[blockname];
		if (out)
			return out;
		if (element.parent)
			element = this.resolveElement(elementIdentity);
	}

	return null;
};


//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModuleContext;
// =========================================================================
