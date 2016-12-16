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
 * @return the corresponding value or BOOLEAN[false] if not found.
 * @throws TAMEError if no such element context.
 */
TLogic.getValue = function(context, elementIdentity, variableName)
{
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
 * @param value the new value.
 * @throws TAMEError if no such element context.
 */
TLogic.setValue = function(context, elementIdentity, variableName, value)
{
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementIdentity);
	context.elements[elementIdentity][variableName.toLowerCase()] = value;
};


/**
 * Removes an object from its owner.
 * @param context the module context.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TLogic.removeObject = function(context, objectIdentity) 
{
	if (!context.elements[objectIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+objectIdentity);
	
	var elementIdentity = context.object[objectIdentity];
	if (!elementIdentity)
		return;
	
	delete context.object[objectIdentity];
	Util.arrayRemove(context.owners[elementIdentity], objectIdentity);
};

/**
 * Removes a player from all rooms.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TLogic.removePlayer = function(context, playerIdentity) 
{
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);
	delete context.roomStacks[playerIdentity];
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
 * Pushes a room onto a player room stack.
 * @param context the module context.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @throws TAMEError if no such element context.
 */
TLogic.pushRoomOntoPlayer = function(context, playerIdentity, roomIdentity) 
{
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
	if (!context.elements[playerIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+playerIdentity);

	if (!context.roomStacks[playerIdentity])
		return null;

	var len = context.roomStacks[playerIdentity].length;
	return context.roomStacks[playerIdentity][len - 1];
};

//TODO: Finish

TLogic.checkElementHasObject = function(context, elementIdentity, objectIdentity) {};
TLogic.checkObjectHasNoOwner = function(context, objectIdentity) {};
TLogic.checkPlayerIsInRoom = function(context, playerIdentity, roomIdentity) {};
TLogic.getObjectsOwnedByElement = function(context, elementIdentity) {};
TLogic.getObjectsOwnedByElementCount = function(context, elementIdentity) {};
TLogic.addObjectName = function(context, objectIdentity, name) {};
TLogic.removeObjectName = function(context, objectIdentity, name) {};
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

