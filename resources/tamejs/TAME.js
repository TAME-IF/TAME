/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TModule = TModule || ((typeof require) !== 'undefined' ? require('./objects/TModule.js') : null);
// ======================================================================================================

//##[[CONTENT-START

var TAME = new (function(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers){

//##[[CONTENT-GENERATE version

//##[[CONTENT-INCLUDE Util.js
//##[[CONTENT-INCLUDE TAMEConstants.js
//##[[CONTENT-INCLUDE TAMEError.js
//##[[CONTENT-INCLUDE TAMEInterrupt.js
//##[[CONTENT-INCLUDE objects/TValue.js
//##[[CONTENT-INCLUDE objects/TRequest.js
//##[[CONTENT-INCLUDE objects/TResponse.js
//##[[CONTENT-INCLUDE objects/TBlockEntry.js
//##[[CONTENT-INCLUDE objects/TAction.js
//##[[CONTENT-INCLUDE objects/TModule.js
//##[[CONTENT-INCLUDE objects/TModuleContext.js
//##[[CONTENT-INCLUDE TAMELogic.js

	var module = new TModule(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers);
	
	this.newContext = function() 
	{
		return new TModuleContext(this.module);
	};

	/**
	 * Initializes a context. Must be called after a new context and game is started.
	 * @param context the module context.
	 * @param tracing if true, add trace cues.
	 * @return (TResponse) the response from the initialize.
	 */
	this.initialize = function(context, tracing) 
	{
		return TLogic.handleInit(context, tracing);
	};
	
	/**
	 * Interprets and performs actions.
	 * @param context the module context.
	 * @param inputMessage the input message to interpret.
	 * @param tracing if true, add trace cues.
	 * @return (TResponse) the response.
	 */
	this.interpret = function(context, inputMessage, tracing) 
	{
		return TLogic.handleRequest(context, inputMessage, tracing);
	};

	return this;
	
})(
//##[[CONTENT-GENERATE header, actions, world, objects, players, rooms, containers
);

//##[[CONTENT-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
