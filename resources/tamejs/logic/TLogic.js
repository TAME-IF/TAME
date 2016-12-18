/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
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
	
	var out = context.elements[elementIdentity][variableName.toLowerCase()];
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
	context.elements[elementIdentity][variableName.toLowerCase()] = value;
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
	if(!context.object)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: object");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	var elementIdentity = context.object[objectIdentity];
	if (!elementIdentity)
		return;
	
	delete context.object[objectIdentity];
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
	if(!context.object)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: object");
	if(!context.owners)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: owners");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	TLogic.removeObject(context, objectIdentity);
	context.object[objectIdentity] = elementIdentity;
	
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
	if(!context.object)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: object");
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	return context.object[objectIdentity] == elementIdentity;
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
	if(!context.object)
		throw new TAMEError(TAMEError.Type.ModuleState, "Context is missing required member: object");
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	return !context.object[objectIdentity];
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
	
	var arr = context.names[objectIdentity];
	if (!arr)
		arr = context.names[objectIdentity] = [];
	if (arr.indexOf(name) >= 0)
		arr.push(name);
};

/**
 * Removes an interpretable name from an object.
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

	
};

//TODO: Finish

TLogic.checkObjectHasName = function(context, objectIdentity, name) {};
TLogic.addObjectTag = function(context, objectIdentity, tag) {};
TLogic.removeObjectTag = function(context, objectIdentity, tag) {};
TLogic.checkObjectHasTag = function(context, objectIdentity, tag) {};

/*
Context: 
{
	"elements": {}, 								// element-to-variables
	"owners": {elemid:[objid, ..], }, 				// element-to-objects
	"object": {objid:elemid, },   					// object-to-element
	"roomStacks": {playerid: [roomid, ..], ...},	// player-to-rooms
	"names": {objid:[name, ...], ...},				// object-to-names
	"tags": {objid:[name, ...], ...},				// object-to-tags
}
*/


//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

