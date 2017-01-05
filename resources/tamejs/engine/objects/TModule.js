/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
// ======================================================================================================

//##[[EXPORTJS-START

/****************************************************
 Constructor for the TAME Module.
 ****************************************************/

function TModule(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers)
{	
	// Fields --------------------
	this.header = theader;
	this.actions = Util.mapify(tactions, "identity");
	this.elements = {};
	this.actionNameTable = {};
	
	var elem = this.elements;
	var act = this.actions;
	var antbl = this.actionNameTable;

	var ELEMACCUM = function(element, identity) 
	{
		if (elem[identity] || act[identity])
			throw TAMEError.Module("Another element already has the identity "+identity);
		elem[identity] = element;
	};
	
	Util.each(Util.mapify(tobjects, "identity"), ELEMACCUM);
	Util.each(Util.mapify(tplayers, "identity"), ELEMACCUM);
	Util.each(Util.mapify(trooms, "identity"), ELEMACCUM);
	Util.each(Util.mapify(tcontainers, "identity"), ELEMACCUM);
	ELEMACCUM(tworld, tworld.identity);
	
	Util.each(this.actions, function(action){
		Util.each(action.names, function(name){
			antbl[name.toLowerCase()] = action.identity;
		});
	});

};

TModule.prototype.getActionByName = function(name)
{
	var identity = this.actionNameTable[name.toLowerCase()];
	if (!identity)
		return null;
	return this.actions[identity];
}

//##[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModule;
// =========================================================================
