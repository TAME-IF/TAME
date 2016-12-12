/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

var TAMEConstants = {
	
	"ActionType": {
		"GENERAL": 1,
		"TRANSITIVE": 2,
		"DITRANSITIVE": 3,
		"MODAL": 4,
		"OPEN": 5
	},

	"RestrictionType": {
		"FORBID": 1,
		"ALLOW": 2
	},

};

//##[[CONTENT-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEConstants;
// =========================================================================