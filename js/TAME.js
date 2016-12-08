/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

var TAME = function() {

//##[[CONTENT-INCLUDE ./objects/TAMEError.js
//##[[CONTENT-INCLUDE ./objects/TValueHash.js
//##[[CONTENT-INCLUDE ./objects/TValue.js
//##[[CONTENT-INCLUDE ./objects/TRequest.js
//##[[CONTENT-INCLUDE ./objects/TResponse.js
//##[[CONTENT-INCLUDE ./objects/TArithmeticFunctions.js
//##[[CONTENT-INCLUDE ./objects/TCommandFunctions.js
//##[[CONTENT-INCLUDE ./objects/TCommand.js
//##[[CONTENT-INCLUDE ./objects/TBlock.js
//##[[CONTENT-INCLUDE ./objects/TAMEModule.js
//##[[CONTENT-INCLUDE ./objects/TAMEModuleContext.js

	this.module = new TAMEModule(/* TODO: Add stuff. */);

	// export: creates a new context.
	this.newContext = function(){
		return new TAMEContext(module);
	};
	
	// export: initializes a new context.
	this.initContext = function(context){
		return new TAMEContext(module);
	};

};

//##[[CONTENT-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEError;
// =========================================================================
