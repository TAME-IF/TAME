/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

/**
 * Constructor for TStringBuilder.
 */
var TStringBuilder = function()
{
	this.buffer = [];
};

/**
 * Appends a string to this.
 * @param value the value to add. Must be defined and not null.
 */
TStringBuilder.prototype.append = function(value)
{
	if (typeof value !== 'undefined' && value !== null)
		this.buffer.push(value.toString());
	return this;
};

/**
 * Clears the builder.
 * @param value the value to add. Must be defined and not null.
 */
TStringBuilder.prototype.clear = function()
{
	this.buffer.length = 0;
};

/**
 * @return the resultant string.
 */
TStringBuilder.prototype.toString = function()
{
	return this.buffer.join("");
};

/**
 * @return the length of the resultant string.
 */
TStringBuilder.prototype.length = function()
{
	return this.toString().length;
};

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TStringBuilder;
// =========================================================================
