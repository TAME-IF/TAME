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
var TAME = new (function(theader, tactions, telements){

//##[[EXPORTJS-GENERATE version

//##[[EXPORTJS-INCLUDE engine/Util.js
	
Util.nanoTime = (function(){
	// Webkit Browser
	if (performance)
	{
		return function() 
		{
			// ms to ns (us res)
			return parseInt(performance.now() * 1e6, 10);
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

//Must be like this in order to avoid Illegal Invocation errors.
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
//##[[EXPORTJS-INCLUDE engine/objects/TResponseReader.js
//##[[EXPORTJS-INCLUDE engine/TAMELogic.js

	var tameModule = new TModule(theader, tactions, telements);

	/**
	 * Creates a new context for the embedded module.
	 */
	this.newContext = function() 
	{
		return new TModuleContext(tameModule);
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

	/**
	 * Creates a new iterable response handler.
	 * @param response the response to handle.
	 * @param eventFunctionMap a map of functions to call on certain events.
	 * 		"start": Called before first cue is handled.
	 *		"pause": Called when a pause occurs (after a cue function).
	 *		"resume": Called on a resume (before cues are processed again).
	 *		"end": Called after last cue is handled.
	 * @param cueFunction A default function that take two parameters (cue type, cue content), or a map of cue type to functions. 
	 *		For the map:
	 *			Cue type must be lowercase. 
	 *			Function should accept one parameter: parameter as content. 
	 *		Called function should return false to halt handling (true to keep going).
	 */
	this.createResponseReader = function(response, eventFunctionMap, cueFunction)
	{
		return new TResponseReader(response, eventFunctionMap, cueFunction);
	};

	/**
	 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
	 * @param sequence the character sequence to parse.
	 * @param tagStartFunc the function called on tag start. Should take one argument: the tag name.  
	 * @param tagEndFunc the function called on tag end. Should take one argument: the tag name.  
	 * @param textFunc the function called on tag contents (does not include tags - it is recommended to maintain a stack). Should take one argument: the text read inside tags.  
	 */
	this.parseFormatted = function(sequence, tagStartFunc, tagEndFunc, textFunc)
	{
		return Util.parseFormatted(sequence, tagStartFunc, tagEndFunc, textFunc);
	};

	return this;
	
})(
//##[[EXPORTJS-GENERATE header, actions, elements
);

//##[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
