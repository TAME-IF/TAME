/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/****************************************************
 Constructor for the TAME Module.
 ****************************************************/

function TModule(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers)
{	
	// Fields --------------------
	this.header = theader;
	this.actions = Util.mapify(tactions, "identity");
	this.objects = Util.mapify(tobjects, "identity");
	this.players = Util.mapify(tplayers, "identity");
	this.rooms = Util.mapify(trooms, "identity");
	this.containers = Util.mapify(tcontainers, "identity");
	this.world = tworld;
	this.actionNameTable = {};
	
	Util.each(this.actions, function(action){
		Util.each(action.names, function(name){
			this.actionNameTable[name] = action.identity;
		});
	});
	// ---------------------------

};

/**
 * Creates a new context for the current module.
 * The context in the JS implementation is a regular object - its functions are a part of the module.
 */
TModule.prototype.createContext = function()
{
	var out = 
	{
		"elements": {}, 	// element-to-variables
		"owners": {}, 		// element-to-objects
		"object": {},   	// object-to-element
		"roomStacks": {},	// player-to-rooms
		"names": {},		// object-to-names
		"tags": {},			// object-to-tags
	};

	// create object contexts.
	Util.each(this.objects, function(element, identity){
		if (out.elements[identity])
			throw new TAMEError(TAMEError.Type.Module, "Another element already has the identity "+elem.identity);
		out.elements[identity] = {"identity": identity, "variables": {}};

		out.names[identity] = {};
		Util.each(element.names, function(name){
			out.names[identity][name] = true;
		});
		Util.each(element.tags, function(tag){
			out.tags[identity][tag] = true;
		});
	});
	
	var CONTEXTFUNC = function(element, identity){
		if (out.elements[identity])
			throw new TAMEError(TAMEError.Type.Module, "Another element already has the identity "+elem.identity);
		out.elements[identity] = {"identity": identity, "variables": {}};
	};
	
	// create player contexts.
	Util.each(this.players, CONTEXTFUNC);
	// create room contexts.
	Util.each(this.rooms, CONTEXTFUNC);
	// create container contexts.
	Util.each(this.containers, CONTEXTFUNC);
	// create world context.
	out.elements["world"] = {"identity": "world", "variables": {}};
	
};


//##[[CONTENT-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModule;
// =========================================================================
