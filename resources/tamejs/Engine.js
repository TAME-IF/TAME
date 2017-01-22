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

//##[[EXPORTJS-GENERATE jsheader
var TAME = new (function()
{

//##[[EXPORTJS-GENERATE version

//##[[EXPORTJS-INCLUDE engine/Util.js
	
Util.nanoTime = (function(){
	// Webkit Browser
	if (window && window.performance)
	{
		return function() 
		{
			// ms to ns (us res)
			return parseInt(window.performance.now() * 1e6, 10);
		};	
	}
	else
	{
		return function()
		{
			// ms to ns (ms res)
			return Date.now() * 1e6;
		};
	}
})();

// Must be like this in order to avoid Illegal Invocation errors.
Util.toBase64 = function(text){return btoa(text);};
Util.fromBase64 = function(data){return atob(data);};


//##[[EXPORTJS-INCLUDE engine/TAMEConstants.js
//##[[EXPORTJS-INCLUDE engine/TAMEError.js
//##[[EXPORTJS-INCLUDE engine/TAMEInterrupt.js
//##[[EXPORTJS-INCLUDE engine/objects/TValue.js
//##[[EXPORTJS-INCLUDE engine/objects/TRequest.js
//##[[EXPORTJS-INCLUDE engine/objects/TResponse.js
//##[[EXPORTJS-INCLUDE engine/objects/TAction.js
//##[[EXPORTJS-INCLUDE engine/objects/TModule.js
//##[[EXPORTJS-INCLUDE engine/objects/TModuleContext.js
//##[[EXPORTJS-INCLUDE engine/TAMELogic.js

	/**
	 * Constructs a new module.
	 * @param moduleData the module data (header, actions, elements).
	 * @return a module object to use for context creation.
	 */
	this.createModule = function(moduleData)
	{
		return new TModule(moduleData.header, moduleData.actions, moduleData.elements);
	};
	
	/**
	 * Creates a new context for a constructed module.
	 * @param tmodule the TAME module to create a context for.
	 * @return a new module context.
	 */
	this.newContext = function(tmodule) 
	{
		return new TModuleContext(tmodule);
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
	
})();

//##[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
