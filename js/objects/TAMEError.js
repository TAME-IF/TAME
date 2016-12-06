/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/*****************************************************************************
 Exception handling.
 *****************************************************************************/
var TAMEError = function(type, message)
{
	this.type = type;
	this.message = message;
};

TAMEError.Type = {
	"Module": "Module",
	"ModuleExecution": "ModuleExecution",
	"ModuleState": "ModuleState",
	"Arithmetic": "Arithmetic",
	"RunawayRequest": "RunawayRequest",
	"UnexpectedValue": "UnexpectedValue",
	"UnexpectedValueType": "UnexpectedValueType"
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEError;
// =========================================================================
