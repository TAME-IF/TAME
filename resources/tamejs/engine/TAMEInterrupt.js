/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

/*****************************************************************************
 Interrupt handling.
 *****************************************************************************/
var TAMEInterrupt = function(type, message)
{
	this.type = type;
	this.message = message;
};

TAMEInterrupt.Type = 
{
	"Break": "Break",
	"Continue": "Continue",
	"Error": "Error",
	"End": "End",
	"Quit": "Quit",
	"Finish": "Finish"
};

// Convenience Constructors
TAMEInterrupt.Break = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Break, "A break interrupt was thrown!"); };
TAMEInterrupt.Continue = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Continue, "A continue interrupt was thrown!"); };
TAMEInterrupt.Error = function(message) { return new TAMEInterrupt(TAMEInterrupt.Type.Error, message); };
TAMEInterrupt.End = function() { return new TAMEInterrupt(TAMEInterrupt.Type.End, "An end interrupt was thrown!"); };
TAMEInterrupt.Quit = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Quit, "A quit interrupt was thrown!"); };
TAMEInterrupt.Finish = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Finish, "A finish interrupt was thrown!"); };


TAMEInterrupt.prototype.toString = function()
{
	return "TAMEInterrupt: "+ this.type + ": " + this.message;
};

//##[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEInterrupt;
// =========================================================================
