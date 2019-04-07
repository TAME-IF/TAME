/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

//[[EXPORTJS-GENERATE jsheader
//[[EXPORTJS-INCLUDEALL EngineDocs.js
let TAME = (function(_TAMEENVCTX)
{

//[[EXPORTJS-GENERATE version

	let tameSelf = this;

//[[EXPORTJS-INCLUDE engine/Util.js
	
	Util.nanoTime = (function(CTX){
		// NodeJS
		if (CTX.process)
		{
			return function() 
			{
				// s,ns to ns (ns res)
				let t = process.hrtime();
				return t[0] * 1e9 + t[1];
			};	
		}
		// Webkit Browser
		else if (CTX.performance)
		{
			return function() 
			{
				// ms to ns (us res)
				return parseInt(CTX.performance.now() * 1e6, 10);
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
	})(_TAMEENVCTX);

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
//[[EXPORTJS-INCLUDE engine/TAMEResponseHandler.js
	
	this.newResponseHandler = function(options) 
	{
		return new TResponseHandler(options);
	};
	
	this.readModule = function(dataView)
	{
		return TBinaryReader.readModule(dataView);
	};

	this.newContext = function(module) 
	{
		return module ? new TModuleContext(module) : null;
	};

	this.initialize = function(context, traceTypes) 
	{
		return TLogic.handleInit(context, traceTypes);
	};
	
	this.interpret = function(context, inputMessage, traceTypes) 
	{
		return TLogic.handleRequest(context, inputMessage, traceTypes);
	};

	this.inspect = function(context, elementIdentity, variable)
	{
		return TLogic.inspect(context, elementIdentity, variable);
	};
	
	return this;
	
})(this);

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
