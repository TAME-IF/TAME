/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/*****************************************************************************
 See net.mtrop.tame.TAMEResponse
 *****************************************************************************/
var TValueHash = function()
{
    this.table = {};
};

/**
 * Adds a value.
 * Names are case-insensitive (stored lowercase).
 * @param name the name of the value.
 * @param value the value to put in.
 */
TValueHash.prototype.put = function(name, value)
{
	this.table[name.toLowerCase()] = value;
};

/**
 * Gets a value.
 * Names are case-insensitive (stored lowercase).
 * @param name the name of the value.
 * @return the corresponding value or null if not found.
 */
TValueHash.prototype.get = function(name)
{
	var out = this.table[name.toLowerCase()];
	if (out != null)
		return out;
	else
		return null;
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TValueHash;
// =========================================================================

