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

/********************************************
 * TAME (Text Adventure Module Engine)
 * (C) Matt Tropiano 2016-2017
 * Standalone Module Library for NodeJS
 ********************************************/
module.exports = new (function()
{

//##[[EXPORTJS-GENERATE version

//##[[EXPORTJS-INCLUDE engine/Util.js
	
	Util.nanoTime = function()
	{
		// s,ns to ns (ns res)
		var t = process.hrtime();
		return t[0] * 1e9 + t[1];
	};

	Util.toBase64 = (function()
	{
		if (Buffer.from)
		{
			return function(text) {
				return Buffer.from(text).toString('base64');
			};
		}
		else
		{
			return function(text) {
				return (new Buffer(text)).toString('base64');
			};
		}
	})();

	Util.fromBase64 = (function()
	{
		if (Buffer.from)
		{
			return function(data) {
				return Buffer.from(data, 'base64').toString('utf8');
			};
		}
		else
		{
			return function(data) {
				return (new Buffer(data, 'base64')).toString('utf8');
			};
		}
	})();

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
