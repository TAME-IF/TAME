/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

/****************************************************
 See com.tameif.tame.TAMEModuleContext
 ****************************************************/
var TModuleContext = function(module)
{
	this.module = module;			// module reference
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
	
	var mc = this;
	
	// create element contexts.
	Util.each(m.elements, function(element, identity)
	{
		identity = identity.toLowerCase();
		
		if (element.archetype)
			return;
		if (s.elements[identity])
			throw TAMEError.Module("Another element already has the identity "+identity);
		s.elements[identity] = {
			"identity": identity,
			"variables": {}
		};
		
		// just for objects
		if (element.tameType === 'TObject')
		{
			s.names[identity] = {};
			s.tags[identity] = {};
			Util.each(element.names, function(name)
			{
				mc.addObjectName(element.identity, name);
			});
			Util.each(element.tags, function(tag)
			{
				mc.addObjectTag(element.identity, tag);
			});		
		}
		
	});
	
	var cr = parseInt(module.header.tame_runaway_max, 10);
	var fd = parseInt(module.header.tame_funcdepth_max, 10);
	
	this.operationRunawayMax = cr <= 0 || isNaN(cr) ? TAMEConstants.DEFAULT_RUNAWAY_THRESHOLD : cr;
	this.functionDepthMax = fd <= 0 || isNaN(fd) ? TAMEConstants.DEFAULT_FUNCTION_DEPTH : fd;
	
};

/**
 * Returns a serialized version of the context state.
 * @return a string that represents the state.
 */
TModuleContext.prototype.stateSave = function()
{
	/*
	 * tvalue: TAME value structure.
	 * atomicref: is an array with one index (integer reference: next refid).
	 * referenceSet (WeakMap): value -> integer - reference to id map
	 * targetRefmap (object): integer -> value - output set to add to state.
	 * Returns: resultant value to write in place of a value. 
	 */
	var REFADD = function(tvalue, atomicRef, referenceSet, targetRefmap) 
	{
		// if referential
		if (TValue.isReferenceCopied(tvalue))
		{
			// if we've not seen the object yet,
			if (!referenceSet.has(tvalue.value))
			{
				// handle lists recursively.
				if (tvalue.type === TValue.Type.LIST)
				{
					let ls = tvalue.value;
					for (let i = 0; i < ls.length; i++)
						ls[i] = REFADD(ls[i], atomicRef, referenceSet, targetRefmap);
				}
				
				let id = (atomicRef[0] += 1);
				targetRefmap[id] = tvalue;
				referenceSet.set(tvalue.value, id);
				return {"refid": id};
			}
			// else if we have,
			else
			{
				return {"refid": referenceSet.get(tvalue.value)};
			}
		}
		// if not reference-copied, return itself. (no change)
		else
		{
			return tvalue;
		}
	};
	
	let state = this.state;
	state.refMap = {};
	let curRef = [0];
	let vmap = new WeakMap();

	// For each element context...
	for (let identity in state.elements) if (state.elements.hasOwnProperty(identity))
	{
		let element = state.elements[identity];

		// For each variable in the element context...
		for (let valueName in element.variables) if (element.variables.hasOwnProperty(valueName))
		{
			element.variables[valueName] = REFADD(element.variables[valueName], curRef, vmap, state.refMap);
		}
	}

	let output = JSON.stringify(state);
	
	// horrible hack incoming
	this.stateRestore(output);
	
	return output;
};

/**
 * Restores the context from a serialized version of the context state.
 * @param stateData the state data to use for restoration.
 */
TModuleContext.prototype.stateRestore = function(stateData)
{
	this.state = JSON.parse(stateData);
	let state = this.state;
	
	if (!state.refMap)
		throw TAMEError.ModuleState("Context state is missing required member: refMap");

	/*
	 * tvalue: TAME value structure.
	 * refmap (object): integer -> value map.
	 * Returns: resultant value to set in place of a value. 
	 */
	var REFSWAP = function(tvalue, refmap) 
	{
		if (tvalue.refid)
		{
			let out = refmap[tvalue.refid];

			// handle lists recursively.
			if (out.type === TValue.Type.LIST)
			{
				let ls = out.value;
				for (let i = 0; i < ls.length; i++)
					ls[i] = REFSWAP(ls[i], refmap);
			}
			return out;
		}
		else
		{
			return tvalue;
		}
	};
	
	for (let identity in state.elements) if (state.elements.hasOwnProperty(identity))
	{
		let element = state.elements[identity];
		for (let valueName in element.variables) if (element.variables.hasOwnProperty(valueName))
		{
			element.variables[valueName] = REFSWAP(element.variables[valueName], state.refMap);
		}
	}
		
	delete state.refMap;
	
	console.log(JSON.stringify(state));
};

/**
 * Sets the current player.
 * @param playerIdentity the player identity, or null.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.setCurrentPlayer = function(playerIdentity) 
{
	let contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	playerIdentity = playerIdentity.toLowerCase();
	
	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	
	contextState.player = playerIdentity;
};

/**
 * Removes a player from all rooms.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.removePlayer = function(playerIdentity) 
{
	let contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	playerIdentity = playerIdentity.toLowerCase();
	
	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();
	roomIdentity = roomIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.roomStacks[playerIdentity])
		return;

	let out = contextState.roomStacks[playerIdentity].pop();
	if (!contextState.roomStacks[playerIdentity].length)
		delete contextState.roomStacks[playerIdentity];
	return out;
};

/**
 * Checks if a player is in a room (or if the room is in the player's room stack).
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @return true if the room is in the player's stack, false if not, or the player is in no room.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkPlayerHasRoomInStack = function(playerIdentity, roomIdentity) 
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();
	roomIdentity = roomIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
	let roomstack = contextState.roomStacks[playerIdentity];
	if (!roomstack)
		return false;
	else
		return roomstack.indexOf(roomIdentity) >= 0;
};

/**
 * Removes an object from its owner.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObject = function(objectIdentity) 
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	let elementIdentity = contextState.objectOwners[objectIdentity];
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();
	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	this.removeObject(objectIdentity);
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");

	elementIdentity = elementIdentity.toLowerCase();
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();
	
	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
	let arr = contextState.owners[elementIdentity];
	if (!arr)
		return [];
	else
		return arr.slice(); // return copy of full array.
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();

	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
	let arr = contextState.owners[elementIdentity];
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	let object = this.getElement(objectIdentity);
	
	name = Util.replaceAll(name.trim().toLowerCase(), "\\s+", " ");
	Util.objectStringAdd(contextState.names, objectIdentity, name);
	Util.each(object.determiners, function(determiner)
	{
		determiner = Util.replaceAll(determiner.trim().toLowerCase(), "\\s+", " ");
		Util.objectStringAdd(contextState.names, objectIdentity, determiner + ' ' + name);
	});
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");

	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	var object = this.getElement(objectIdentity);
	
	name = Util.replaceAll(name.trim().toLowerCase(), "\\s+", " ");
	Util.objectStringRemove(contextState.names, objectIdentity, name);
	Util.each(object.determiners, function(determiner)
	{
		determiner = Util.replaceAll(determiner.trim().toLowerCase(), "\\s+", " ");
		Util.objectStringRemove(contextState.names, objectIdentity, determiner + ' ' + name);
	});
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
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	name = name.toLowerCase();
	return Util.objectStringContains(contextState.names, objectIdentity, name);
};

/**
 * Adds a tag to an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * Does nothing if the object already has the tag.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectTag = function(objectIdentity, tag)
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	tag = tag.toLowerCase();
	Util.objectStringAdd(contextState.tags, objectIdentity, tag);
};

/**
 * Removes a tag from an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * Does nothing if the object does not have the tag.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObjectTag = function(objectIdentity, tag)
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	tag = tag.toLowerCase();
	Util.objectStringRemove(contextState.tags, objectIdentity, tag);
};

/**
 * Checks for a tag on an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @return true if it exists, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasTag = function(objectIdentity, tag) 
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	tag = tag.toLowerCase();
	return Util.objectStringContains(contextState.tags, objectIdentity, tag);
};

/**
 * Gets an element by its identity.
 * @return the element or null.
 */
TModuleContext.prototype.getElement = function(elementIdentity)
{
	return this.module.elements[elementIdentity.toLowerCase()];
};

/**
 * Gets an element context by its identity.
 * @return the element context or null.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getElementContext = function(elementIdentity)
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	return contextState.elements[elementIdentity.toLowerCase()];
};

/**
 * Gets the current player.
 * @return player element, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentPlayer = function()
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");

	if (!contextState.player)
		return null;
	
	return this.getElement(contextState.player);
};

/**
 * Gets the current player context.
 * @return player context, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentPlayerContext = function()
{
	let player = this.getCurrentPlayer();
	if (!player)
		return null;
	
	return this.getElementContext(player.identity);
};

/**
 * Gets the current room. Influenced by current player.
 * @return room element, or null if no current room (or no current player).
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentRoom = function(playerIdentity)
{
	let contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	if (!playerIdentity)
		playerIdentity = contextState.player;
	else
		playerIdentity = playerIdentity.toLowerCase(); 
	
	if (!playerIdentity)
		return null;

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);

	let stack = contextState.roomStacks[playerIdentity];

	if (!stack)
		return null;

	return this.getElement(stack[stack.length - 1]);
};

/**
 * Gets the current room context.
 * @return room context, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentRoomContext = function(playerIdentity)
{
	let room = this.getCurrentRoom(playerIdentity);
	if (!room)
		return null;
	
	return this.getElementContext(room.identity);
};

/**
 * Resolves an action by its action identity.
 * @param actionIdentity the action identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveAction = function(actionIdentity)
{	
	let out = this.module.actions[actionIdentity];
	if (!out)
		throw TAMEError.ModuleExecution("Action is missing from module: "+actionIdentity);		
	return this.module.actions[actionIdentity];
};

/**
 * Resolves an element by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context or if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElement = function(elementIdentity)
{	
	var element = null;
	elementIdentity = elementIdentity.toLowerCase();
	
	// current player
	if (elementIdentity === 'player')
	{
		element = this.getCurrentPlayer();
		if (!element)
			throw TAMEError.ModuleExecution("Current player context called with no current player!");
		return element;
	}
	// current room
	else if (elementIdentity === 'room')
	{
		let player = this.getCurrentPlayer();
		if (!player)
			throw TAMEError.ModuleExecution("Current room context called with no current player!");
		
		element = this.getCurrentRoom();
		if (!element)
			throw TAMEError.ModuleExecution("Current room context called with no current room!");
		return element;
	}
	else
	{
		element = this.getElement(elementIdentity);
		if (!element)
			throw TAMEError.ModuleExecution("Expected element '"+elementIdentity+"' in module!");
		return element;
	}
};

/**
 * Resolves an element context by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context or if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElementContext = function(elementIdentity)
{	
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");

	var element = this.resolveElement(elementIdentity);

	var ident = element.identity.toLowerCase();
	
	if (!contextState.elements[ident])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+element.identity);
	
	return contextState.elements[ident];
};


/**
 * Gets the entry block name for a block type and values.
 * This is used to resolve blocks.
 * @param blockType the block entry type.
 * @param blockValues the values for matching the block.
 * @return the resultant name.
 */
TModuleContext.prototype.resolveBlockName = function(blockType, blockValues)
{
	var blockname =	blockType + "(";
	if (blockValues) for (var i = 0; i < blockValues.length; i++)
	{
		blockname += TValue.toString(blockValues[i]);
		if (i < blockValues.length - 1)
			blockname += ",";
	}
	blockname += ")";
	return blockname;
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
	var blockname = this.resolveBlockName(blockType, blockValues);
	var element = this.resolveElement(elementIdentity); 
	
	while (element)
	{
		var out = element.blockTable[blockname];
		if (out)
			return out;
		if (element.parent)
			element = this.resolveElement(element.parent);
		else
			element = null;
	}

	return null;
};

/**
 * Resolves a qualifying function by name starting from an element.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the starting element identity.
 * @param functionName the name of the function.
 * @return the first qualifying function in the lineage, or null if no matching entry.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveFunction = function(elementIdentity, functionName)
{
	var element = this.resolveElement(elementIdentity); 
	functionName = functionName.toLowerCase();
	
	while (element)
	{
		var out = element.functionTable[functionName];
		if (out)
			return out;
		if (element.parent)
			element = this.resolveElement(element.parent);
		else
			element = null;
	}

	return null;	
};

/**
 * Returns all objects in the accessible area by an object name read from the interpreter.
 * The output stops if the size of the output array is reached.
 * @param name the name from the interpreter.
 * @param outputArray the output vector of found objects.
 * @param arrayOffset the starting offset into the array to put them.
 * @return the amount of objects found.
 */
TModuleContext.prototype.getAccessibleObjectsByName = function(name, outputArray, arrayOffset)
{
	var playerContext = this.getCurrentPlayerContext();
	var roomContext = this.getCurrentRoomContext();
	var worldContext = this.getElementContext("world");

	var start = arrayOffset;
	var arr = null;
	
	if (playerContext !== null)
	{
		arr = this.getObjectsOwnedByElement(playerContext.identity);
		for (let x in arr) if (arr.hasOwnProperty(x))
		{
			var objectIdentity = arr[x];
			if (this.checkObjectHasName(objectIdentity, name))
			{
				outputArray[arrayOffset++] = this.getElement(objectIdentity);
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	}
	if (roomContext !== null) 
	{
		arr = this.getObjectsOwnedByElement(roomContext.identity);
		for (let x in arr) if (arr.hasOwnProperty(x))
		{
			let objectIdentity = arr[x];
			if (this.checkObjectHasName(objectIdentity, name))
			{
				outputArray[arrayOffset++] = this.getElement(objectIdentity);
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	}
	
	arr = this.getObjectsOwnedByElement(worldContext.identity);
	for (let x in arr) if (arr.hasOwnProperty(x))
	{
		let objectIdentity = arr[x];
		if (this.checkObjectHasName(objectIdentity, name))
		{
			outputArray[arrayOffset++] = this.getElement(objectIdentity);
			if (arrayOffset == outputArray.length)
				return arrayOffset - start;
		}
	}
	
	return arrayOffset - start;
};


//[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModuleContext;
// =========================================================================
