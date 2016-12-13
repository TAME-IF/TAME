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

var TAME = (function(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers){

//##[[CONTENT-INCLUDE Util.js
//##[[CONTENT-INCLUDE TAMEConstants.js
//##[[CONTENT-INCLUDE TAMEError.js
//##[[CONTENT-INCLUDE objects/TValue.js
//##[[CONTENT-INCLUDE objects/TRequest.js
//##[[CONTENT-INCLUDE objects/TResponse.js
//##[[CONTENT-INCLUDE objects/TBlockEntry.js
//##[[CONTENT-INCLUDE logic/TLogic.js
//##[[CONTENT-INCLUDE logic/TArithmeticFunctions.js
//##[[CONTENT-INCLUDE logic/TCommandFunctions.js
//##[[CONTENT-INCLUDE objects/TCommand.js
//##[[CONTENT-INCLUDE objects/TBlock.js
//##[[CONTENT-INCLUDE objects/TModule.js

	this.module = new TModule(theader, tactions, tworld, tobjects, tplayers, trooms, tcontainers);
	this.newContext = function() {
		return this.module.createContext();
	};

})(
//##[[CONTENT-GENERATE header, actions, world, objects, players, rooms, containers
);

//##[[CONTENT-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAME;
// =========================================================================
