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
	this.hash = null;
};

TBlockEntry.create = function(type, values)
{
	var out = new TBlockEntry(type, values);
	out.hash = type+':';
	for (var x in this.values) if (this.values.hasOwnProperty(x))
		out.hash += x.toString() + ':';
	return out;
};

/*
TBlockEntry.Type = {
	"INIT": "INIT",
	"AFTERREQUEST": "AFTERREQUEST",
	"PROCEDURE(1, ArgumentType.VALUE),
	"ONACTION(1, ArgumentType.ACTION),
	"ONACTIONWITH(2, ArgumentType.ACTION, ArgumentType.OBJECT),
	"ONACTIONWITHOTHER(1, ArgumentType.ACTION),
	"ONMODALACTION(2, ArgumentType.ACTION, ArgumentType.VALUE),
	"ONWORLDBROWSE(0),
	"ONROOMBROWSE(0),
	"ONPLAYERBROWSE(0),
	"ONCONTAINERBROWSE(0),
	"ONAMBIGUOUSACTION(0, ArgumentType.ACTION),
	"ONBADACTION(0, ArgumentType.ACTION),
	"ONINCOMPLETEACTION(0, ArgumentType.ACTION),
	"ONUNKNOWNACTION(0, ArgumentType.ACTION),
	"ONFAILEDACTION(0, ArgumentType.ACTION),
	"ONFORBIDDENACTION(0, ArgumentType.ACTION),
};
*/

//TODO: Finish

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TBlockEntry;
// =========================================================================
