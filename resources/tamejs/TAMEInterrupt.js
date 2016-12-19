/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/*****************************************************************************
 Interrupt handling.
 *****************************************************************************/
var TAMEInterrupt = function(type)
{
	this.type = type;
};

TAMEInterrupt.Type = 
{
	"Break": "Break",
	"Continue": "Continue",
	"Error": "Error",
	"End": "End",
	"Quit": "Quit"
};

TAMEInterrupt.prototype.toString = function()
{
	return "TAMEInterrupt: "+ this.type;
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEInterrupt;
// =========================================================================
