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
var TAME = new (function(moduleData){

//[[EXPORTJS-GENERATE version

//[[EXPORTJS-INCLUDE engine/Util.js
	
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
//[[EXPORTJS-INCLUDE engine/TAMEBrowserHandler.js

	let tameSelf = this;
	let tameModule = TBinaryReader.readModule(moduleBin64);

	/**
	 * Creates a new response handler (mostly for aiding in browser functions).
	 * Handles all standard cues and provides a way via a function to handle other cues.
	 * This accumulates the results of contiguous "text" and "textf" cues before "print()" is called.
	 * @param TAME reference to TAME engine.
	 * @param options options object.
	 * 		print: fn(text): called when a string needs printing (may contain HTML or other things).
	 * 		onStart: fn(): called before cues start processing.
	 * 			Should disable input.
	 * 		onEnd: fn(): called when cues stop processing, and the end is reached.
	 * 			Should enable input.
	 * 		onSuspend: fn(): called when a call to process a cue initiates a suspension (processing stops, but not due to a pause).
	 * 			Should disable input.
	 * 		onResume: fn(): called when cues process again.
	 * 			Should disable input.
	 * 		onPause: fn(): called after a "pause" cue is processed. 
	 * 			Should prompt for continuation somehow, then call resume() after the user "continues."
	 * 		onQuit: fn(): called after a "quit" cue is processed.
	 * 			Should stop input and prevent further input. 
	 * 		onError: fn(message): called after an "error" cue is processed.
	 * 			Should make an error message appear on screen. Dismissable. 
	 * 		onFatal: fn(message): called after a "fatal" cue is processed.
	 * 			Should make a message appear on screen, and stop input as though a "quit" occurred. 
	 * 		onOtherCue: fn(cueType, cueContent): called when a cue that is not handled by this handler needs processing. 
	 * 			Should return boolean. true = keep going, false = suspend (until resume() is called).
	 * 		onStartFormatTag: fn(tagname, accum): called when a formatted string starts a tag.
	 * 		onEndFormatTag: fn(tagname, accum): called when a formatted string ends a tag.
	 * 		onFormatText: fn(text, accum): called when a formatted string needs to process text.
	 */
	this.newBrowserHandler = function(options) 
	{
		return new TBrowserHandler(tameSelf, options);
	};

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
	
})(
//[[EXPORTJS-GENERATE binary
);

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
