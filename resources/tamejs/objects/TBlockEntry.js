/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/****************************************************
 See net.mtrop.tame.lang.BlockEntry
 ****************************************************/
var TBlockEntry = function(type, values)
{
	this.type = type;
	this.values = values;
	this.entryString = null;
};

/**
 * Convenience method for creating block entries.
 * @param type the blockentry type.
 * @param values an array of values.
 * @return a new TBlockEntry. 
 */
TBlockEntry.create = function(type, values)
{
	var out = new TBlockEntry(type, values);
	
	out.entryString = out.type + ":";
	for (var i = 0; i < out.values.length; i++)
	{
		out.entryString += out.values[i].toString();
		if (i < out.values.length - 1)
			out.entryString += ",";
	}
	return out;
};

TBlockEntry.Type = 
{
	"INIT": "Init",
	"AFTERREQUEST": "AfterRequest",
	"PROCEDURE": "Procedure",
	"ONACTION": "OnAction",
	"ONACTIONWITH": "OnActionWith",
	"ONACTIONWITHOTHER": "OnActionWithOther",
	"ONMODALACTION": "OnModalAction",
	"ONWORLDBROWSE": "OnWorldBrowse",
	"ONROOMBROWSE": "OnRoomBrowse",
	"ONPLAYERBROWSE": "OnPlayerBrowse",
	"ONCONTAINERBROWSE": "OnContainerBrowse",
	"ONAMBIGUOUSACTION": "OnAmbiguousAction",
	"ONBADACTION": "OnBadAction",
	"ONINCOMPLETEACTION": "OnIncompleteAction",
	"ONUNKNOWNACTION": "OnUnknownAction",
	"ONFAILEDACTION": "OnFailedAction",
	"ONFORBIDDENACTION": "OnForbiddenAction"
};

/**
 * Returns a string representation of this block entry.
 * This is its "entry string" representation.
 */
TBlockEntry.prototype.toString = function()
{
	return this.entryString;
};


//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TBlockEntry;
// =========================================================================
