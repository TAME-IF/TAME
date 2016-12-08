/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('./TAMEError.js') : null);
var TArithmeticFunctions = TArithmeticFunctions || ((typeof require) !== 'undefined' ? require('./TArithmeticFunctions.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('./TValue.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/*****************************************************************************
 Arithmetic function entry points.
 *****************************************************************************/
var TCommandFunctions = 
[
 	/* NOOP */
	{
		"name": 'NOOP',
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish.
		}
	},
	
];

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (TValueHash) the local variables on the block call.
 * @param command (TCommand) the command object.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TCommandFunctions.execute = function(index, request, response, blockLocal, command)
{
	TCommandFunctions[index].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted();
}

/* Type enumeration. */
TCommandFunctions.Type = 
{
};

//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommandFunctions;
// =========================================================================
