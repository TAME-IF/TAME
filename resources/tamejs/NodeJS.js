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

/****************************************
 * NodeJS Shell
 ****************************************/

//##[[EXPORTJS-INCLUDE node/Main.js

//##[[EXPORTJS-END
