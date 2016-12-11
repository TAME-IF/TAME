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
// ======================================================================================================

//##[[CONTENT-START

var TLogic = {};

/**
 * Gets a value by variable name from a specific element context.
 * Variable names are converted to lower-case - they resolve case-insensitively.
 * @param moduleContext the module context.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @return the corresponding value or BOOLEAN[false] if not found.
 * @throws TAMEError if no such element context.
 */
TLogic.getValue = function(moduleContext, elementIdentity, variableName)
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
 * @param moduleContext the module context.
 * @param elementIdentity the element identity.
 * @param variableName the variable name.
 * @param value the new value.
 * @throws TAMEError if no such element context.
 */
TLogic.setValue = function(moduleContext, elementIdentity, variableName, value)
{
	if (!context.elements[elementIdentity])
		throw new TAMEError(TAMEError.Type.ModuleState, "Element is missing from context: "+elementName);
	context.elements[elementIdentity][variableName.toLowerCase()] = value;
};

// TODO: Finish

/*
removeObject(moduleContext, TObjectIdentity)
removePlayer(moduleContext, TPlayerIdentity)
addObjectToElement(moduleContext, ElementIdentity, TObjectIdentity)
pushRoomOntoPlayer(moduleContext, TPlayerIdentity, TRoomIdentity)
popRoomFromPlayer(moduleContext, TPlayerIdentity)
getCurrentRoom(moduleContext, TPlayerIdentity)
checkElementHasObject(moduleContext, ElementIdentity, TObjectIdentity)
checkObjectHasNoOwner(moduleContext, TObjectIdentity)
checkPlayerIsInRoom(moduleContext, TPlayerIdentity, TRoomIdentity)
getObjectsOwnedByElement(moduleContext, ElementIdentity)
getObjectsOwnedByElementCount(moduleContext, ElementIdentity)
addObjectName(moduleContext, TObjectIdentity, String)
removeObjectName(moduleContext, TObjectIdentity, String)
checkObjectHasName(moduleContext, TObjectIdentity, String)
addObjectTag(moduleContext, TObjectIdentity, String)
removeObjectTag(moduleContext, TObjectIdentity, String)
checkObjectHasTag(moduleContext, TObjectIdentity, String)
 */

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

