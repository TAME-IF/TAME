/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

/*****************************************************************************
 Exception handling.
 *****************************************************************************/
var TAMEError = function(type, message)
{
	this.type = type;
	this.message = message;
};

TAMEError.Type = 
{
	"Module": "Module",
	"ModuleExecution": "ModuleExecution",
	"ModuleState": "ModuleState",
	"Arithmetic": "Arithmetic",
	"ArithmeticStackState": "ArithmeticStackState",
	"RunawayRequest": "RunawayRequest",
	"UnexpectedValueType": "UnexpectedValueType",
	"BadParameter": "BadParameter"
};

TAMEError.prototype.toString = function()
{
	return "TAMEError: "+ this.type + ": " + this.message;
};

// Convenience Constructors

TAMEError.Module = function(message) {return new TAMEError(TAMEError.Type.Module, message);};
TAMEError.ModuleExecution = function(message) {return new TAMEError(TAMEError.Type.ModuleExecution, message);};
TAMEError.ModuleState = function(message) {return new TAMEError(TAMEError.Type.ModuleState, message);};
TAMEError.Arithmetic = function(message) {return new TAMEError(TAMEError.Type.Arithmetic, message);};
TAMEError.ArithmeticStackState = function(message) {return new TAMEError(TAMEError.Type.ArithmeticStackState, message);};
TAMEError.RunawayRequest = function(message) {return new TAMEError(TAMEError.Type.RunawayRequest, message);};
TAMEError.UnexpectedValueType = function(message) {return new TAMEError(TAMEError.Type.UnexpectedValueType, message);};
TAMEError.BadParameter = function(message) {return new TAMEError(TAMEError.Type.BadParameter, message);};

//[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEError;
// =========================================================================
