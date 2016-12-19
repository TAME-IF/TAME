/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/****************************************************
 See net.mtrop.tame.TAMEAction
 ****************************************************/
var TAction = function(initial, actionIdentity, target, object1Identity, object2Identity)
{
	this.initial = initial;
	this.actionIdentity = actionIdentity; 
	this.target = target; 
	this.object1Identity = object1Identity; 
	this.object2Identity = object2Identity;
};

// Convenience constructors.

TAction.create = function(actionIdentity) { return new TAction(false, actionIdentity); };
TAction.createModal = function(actionIdentity, target) { return new TAction(false, actionIdentity, target); };
TAction.createObject = function(actionIdentity, object1Identity) { return new TAction(false, actionIdentity, null, object1Identity); };
TAction.createObject2 = function(actionIdentity, object1Identity, object2Identity) { return new TAction(false, actionIdentity, null, object1Identity, object2Identity); };
TAction.createInitial = function(actionIdentity) { return new TAction(true, actionIdentity); };
TAction.createInitialModal = function(actionIdentity, target) { return new TAction(true, actionIdentity, target); };
TAction.createInitialObject = function(actionIdentity, object1Identity) { return new TAction(true, actionIdentity, null, object1Identity); };
TAction.createInitialObject2 = function(actionIdentity, object1Identity, object2Identity) { return new TAction(true, actionIdentity, null, object1Identity, object2Identity); };

TAction.prototype.toString = function()
{
	var out = "ActionItem ";
	if (this.initial)
		out += "INITIAL ";
	
	out += "[";
	if (this.actionIdentity)
		out += this.actionIdentity + ", ";

	if (this.target)
		out += this.target;

	if (this.object1Identity)
		out += this.object1Identity;

	if (this.object2Identity)
	{
		if (this.object1Identity)
			out += ", ";
		out += this.object2Identity;
	}
	
	out += "]";
	
	return out;
};


//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAction;
// =========================================================================
