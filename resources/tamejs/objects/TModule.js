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
			this.actionNameTable[name.toLowerCase()] = action.identity;
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

//##[[CONTENT-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TModule;
// =========================================================================
