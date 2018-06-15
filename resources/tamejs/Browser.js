/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
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

//##[[EXPORTJS-INCLUDE engine/TStringBuilder.js
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
//##[[EXPORTJS-INCLUDE engine/TAMEBrowserHandler.js

	var tameSelf = this;
	var tameModule = new TModule(theader, tactions, telements);

	/**
	 * Creates a new response handler suitable for browsers.
	 * Handles most cues and has entry points for other cues.
	 * @param options options object.
	 * 		print: fn(text): called when a string needs printing.
	 * 		onStart: fn(): called before cues start processing.
	 * 		onEnd: fn(): called before cues stop processing, and the end is reached.
	 * 		onSuspend: fn(): called when a call to process a cue initiates a suspension.
	 * 		onResume: fn(): called when cues process again.
	 * 		onPauseCue: fn(): called when a "pause" cue is processed. 
	 * 			Should prompt for continuation, then call resume().
	 * 		onQuitCue: fn(): called when a "quit" cue is encountered.
	 * 			Should stop input. 
	 * 		onErrorCue: fn(message): called when an "error" cue is encountered.
	 * 			Should make a message appear on screen. Dismissable. 
	 * 		onFatalCue: fn(message): called when a "fatal" cue is encountered.
	 * 			Should make a message appear on screen, and halt input. 
	 * 		onUnknownCue: fn(cueType, cueContent): called when a cue that is not handled by this handler needs processing. 
	 * 			Should return boolean. true = keep going, false = suspend.
	 * 		onStartFormatTag: fn(tagname): called when a formatted string starts a tag.
	 * 		onEndFormatTag: fn(tagname): called when a formatted string ends a tag.
	 * 		onFormatText: fn(tag): called when a formatted string needs to process text.
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
