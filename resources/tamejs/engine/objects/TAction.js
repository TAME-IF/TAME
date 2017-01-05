/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[EXPORTJS-START

/****************************************************
 See net.mtrop.tame.TAMEAction
 ****************************************************/
var TAction = function(initial, action, target, object1, object2)
{
	this.initial = initial;
	this.action = action; 
	this.target = target; 
	this.object1 = object1; 
	this.object2 = object2;
};

// Convenience constructors.

TAction.create = function(action) { return new TAction(false, action); };
TAction.createModal = function(action, target) { return new TAction(false, action, target); };
TAction.createObject = function(action, object1) { return new TAction(false, action, null, object1); };
TAction.createObject2 = function(action, object1, object2) { return new TAction(false, action, null, object1, object2); };
TAction.createInitial = function(action) { return new TAction(true, action); };
TAction.createInitialModal = function(action, target) { return new TAction(true, action, target); };
TAction.createInitialObject = function(action, object1) { return new TAction(true, action, null, object1); };
TAction.createInitialObject2 = function(action, object1, object2) { return new TAction(true, action, null, object1, object2); };

TAction.prototype.toString = function()
{
	var out = "ActionItem ";
	if (this.initial)
		out += "INITIAL ";
	
	out += "[";
	if (this.action)
		out += this.action.identity + ", ";

	if (this.target)
		out += this.target;

	if (this.object1)
		out += this.object1.identity;

	if (this.object2)
	{
		if (this.object1.identity)
			out += ", ";
		out += this.object2.identity;
	}
	
	out += "]";
	
	return out;
};


//##[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAction;
// =========================================================================
