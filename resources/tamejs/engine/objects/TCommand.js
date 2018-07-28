/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

/****************************************************
 See com.tameif.tame.TAMECommand
 ****************************************************/
var TCommand = function(action, target, object1, object2)
{
	this.action = action; 
	this.target = target; 
	this.object1 = object1; 
	this.object2 = object2;
};

// Convenience constructors.

TCommand.create = function(action) { return new TCommand(action); };
TCommand.createModal = function(action, target) { return new TCommand(action, target); };
TCommand.createObject = function(action, object1) { return new TCommand(action, null, object1); };
TCommand.createObject2 = function(action, object1, object2) { return new TCommand(action, null, object1, object2); };

TCommand.prototype.toString = function()
{
	var sb = new TStringBuilder();
	sb.append("Command ");
	sb.append("[");
	if (this.action)
		sb.append(this.action.identity);

	if (this.target)
		sb.append(", ").append(this.target);

	if (this.object1)
		sb.append(", ").append(this.object1.identity);

	if (this.object2)
	{
		if (this.object1.identity)
			sb.append(", ");
		sb.append(this.object2.identity);
	}
	
	sb.append("]");
	
	return sb.toString();
};


//##[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommand;
// =========================================================================
