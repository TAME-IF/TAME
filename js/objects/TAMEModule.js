/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/****************************************************
 Constructor for the TAME Module.
 ****************************************************/

function TAMEModule(header, tactions, tworld, tobjects, tplayers, trooms, tcontainers)
{
	// Smarter foreach.
	var _each = function(obj, func)
	{
		for (x in obj) 
			if (obj.hasOwnProperty(x)) 
				func(obj[x], x, obj.length);
	}

	// Mapify - [object, ...] to {object.memberKey -> object, ...}
	var _mapify = function(objlist, memberKey, multi) 
	{
		var out = {}; 
		for (x in objlist) 
			if (objlist.hasOwnProperty(x))
			{				
				var chain = out[objlist[x][memberKey]];
				if (multi && chain)
				{
					if (Object.prototype.toString.call(chain) === '[object Array]')
						chain.push(objlist[x]); 
					else
						out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x]]; 
				}
				else
					out[objlist[x][memberKey]] = objlist[x]; 
			}
		return out;
	}

	// Pairify - [object, ...] to {object.memberKey -> object.memberValue, ...}
	var _pairify = function(objlist, memberKey, memberValue, multi) 
	{
		var out = {}; 
		for (x in objlist) 
			if (objlist.hasOwnProperty(x))
			{				
				var chain = out[objlist[x][memberKey]];
				if (multi && chain)
				{
					if (Object.prototype.toString.call(chain) === '[object Array]')
						chain.push(objlist[x][memberValue]); 
					else
						out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x][memberValue]]; 
				}
				else
					out[objlist[x][memberKey]] = objlist[x][memberValue]; 
			}
		return out;
	}
	
	// Fields --------------------
	this.header = theader;
	this.actions = _mapify(tactions, "identity");
	this.objects = _mapify(tobjects, "identity");
	this.players = _mapify(tplayers, "identity");
	this.rooms = _mapify(trooms, "identity");
	this.containers = _mapify(tcontainers, "identity");
	this.world = tworld;
	this.actionNameTable = {};
	
	_each(actions, function(action){
		_each(action.names, function(name){
			this.actionNameTable[name] = action.identity;
		});
	});
	// ---------------------------
	
};

//##[[CONTENT-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEError;
// =========================================================================
