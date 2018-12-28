/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

//[[EXPORTJS-GENERATE jsheader
module.exports = new (function()
{

//[[EXPORTJS-GENERATE version

//[[EXPORTJS-INCLUDE engine/Util.js
	
	Util.nanoTime = function()
	{
		// s,ns to ns (ns res)
		var t = process.hrtime();
		return t[0] * 1e9 + t[1];
	};

//[[EXPORTJS-INCLUDE engine/TStringBuilder.js
//[[EXPORTJS-INCLUDE engine/TAMEConstants.js
//[[EXPORTJS-INCLUDE engine/TAMEError.js
//[[EXPORTJS-INCLUDE engine/TAMEInterrupt.js
//[[EXPORTJS-INCLUDE engine/objects/TValue.js
//[[EXPORTJS-INCLUDE engine/objects/TRequest.js
//[[EXPORTJS-INCLUDE engine/objects/TResponse.js
//[[EXPORTJS-INCLUDE engine/objects/TCommand.js
//[[EXPORTJS-INCLUDE engine/objects/TModule.js
//[[EXPORTJS-INCLUDE engine/objects/TModuleContext.js
//[[EXPORTJS-INCLUDE engine/TBinaryReader.js
//[[EXPORTJS-INCLUDE engine/TAMELogic.js

	/**
	 * Creates a new module from a DataView.
	 * @param dataView (DataView) the a DataView containing the serialized module.
	 * @return (TModule) a deserialized module. 
	 */
	this.readModule = function(dataView)
	{
		return TBinaryReader.readModule(dataView);
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
	 * @param traceTypes 
	 * 		(boolean) if true, add all trace types, false for none.
	 * 		(Array) list of tracing types (case-insensitive).
	 * 		(Object) map of tracing types (case-insensitive).
	 * @return (TResponse) the response from the initialize.
	 */
	this.initialize = function(context, traceTypes) 
	{
		return TLogic.handleInit(context, traceTypes);
	};
	
	/**
	 * Interprets and performs actions.
	 * @param context (Object) the module context.
	 * @param inputMessage (string) the input message to interpret.
	 * @param traceTypes 
	 * 		(boolean) if true, add all trace types, false for none.
	 * 		(Array) list of tracing types (case-insensitive).
	 * 		(Object) map of tracing types (case-insensitive).
	 * @return (TResponse) the response.
	 */
	this.interpret = function(context, inputMessage, traceTypes) 
	{
		return TLogic.handleRequest(context, inputMessage, traceTypes);
	};

	/**
	 * Inspects an element or element's value.
	 * @param context (object) the module context.
	 * @param elementIdentity (string) the identity of a non-archetype element.
	 * @param variable (string) [OPTIONAL] the name of the variable to inspect. 
	 * @return (Object) the queried identifiers and values as debug strings. 
	 */
	this.inspect = function(context, elementIdentity, variable)
	{
		return TLogic.inspect(context, elementIdentity, variable);
	};
	
	/**
	 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
	 * The target functions passed in are provided an accumulator array to push generated text into. 
	 * On return, this function returns the accumulator's contents joined into a string. 
	 * @param sequence (string) the character sequence to parse.
	 * @param tagStartFunc (Function) the function called on tag start. arguments: tagName (string), accumulator (Array)  
	 * @param tagEndFunc (Function) the function called on tag end. arguments: tagName (string), accumulator (Array)
	 * @param textFunc (Function) the function called on tag contents. arguments: text (string), accumulator (Array)
	 * @return the full accumulated result.  
	 */
	this.parseFormatted = function(sequence, tagStartFunc, tagEndFunc, textFunc)
	{
		return Util.parseFormatted(sequence, tagStartFunc, tagEndFunc, textFunc);
	};

	return this;
	
})();

//[[EXPORTJS-END
