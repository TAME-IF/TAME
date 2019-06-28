/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

/*****************************************************************************
 Arithmetic function entry points.
 *****************************************************************************/
var TArithmeticFunctions = 
[
 	/* ABSOLUTE */
	{
		"name": 'ABSOLUTE',
		"symbol": '+',
		"binary": false,
		"doOperation": TValue.absolute
	},
	
 	/* NEGATE */
	{
		"name": 'NEGATE',
		"symbol": '-',
		"binary": false,
		"doOperation": TValue.negate
	},
	
 	/* LOGICAL NOT */
	{
		"name": 'LOGICAL_NOT',
		"symbol": '!',
		"binary": false,
		"doOperation": TValue.logicalNot
	},
	
 	/* ADD */
	{
		"name": 'ADD',
		"symbol": '+',
		"binary": true,
		"doOperation": TValue.add
	},
	
 	/* SUBTRACT */
	{
		"name": 'SUBTRACT',
		"symbol": '-',
		"binary": true,
		"doOperation": TValue.subtract
	},
	
 	/* MULTIPLY */
	{
		"name": 'MULTIPLY',
		"symbol": '*',
		"binary": true,
		"doOperation": TValue.multiply
	},
	
 	/* DIVIDE */
	{
		"name": 'DIVIDE',
		"symbol": '/',
		"binary": true,
		"doOperation": TValue.divide
	},
	
 	/* MODULO */
	{
		"name": 'MODULO',
		"symbol": '%',
		"binary": true,
		"doOperation": TValue.modulo
	},
	
 	/* POWER */
	{
		"name": 'POWER',
		"symbol": '**',
		"binary": true,
		"doOperation": TValue.power
	},
	
 	/* LOGICAL AND */
	{
		"name": 'LOGICAL_AND',
		"symbol": '&',
		"binary": true,
		"doOperation": TValue.logicalAnd
	},
	
 	/* LOGICAL OR */
	{
		"name": 'LOGICAL_OR',
		"symbol": '|',
		"binary": true,
		"doOperation": TValue.logicalOr
	},
	
 	/* LOGICAL XOR */
	{
		"name": 'LOGICAL_XOR',
		"symbol": '^',
		"binary": true,
		"doOperation": TValue.logicalXOr
	},
	
 	/* EQUALS */
	{
		"name": 'EQUALS',
		"symbol": '==',
		"binary": true,
		"doOperation": TValue.equals
	},
	
 	/* NOT EQUALS */
	{
		"name": 'NOT_EQUALS',
		"symbol": '!=',
		"binary": true,
		"doOperation": TValue.notEquals
	},
	
 	/* STRICT EQUALS */
	{
		"name": 'STRICT_EQUALS',
		"symbol": '===',
		"binary": true,
		"doOperation": TValue.strictEquals
	},
	
 	/* STRICT NOT EQUALS */
	{
		"name": 'STRICT_NOT_EQUALS',
		"symbol": '!==',
		"binary": true,
		"doOperation": TValue.strictNotEquals
	},
	
 	/* LESS */
	{
		"name": 'LESS',
		"symbol": '<',
		"binary": true,
		"doOperation": TValue.less
	},
	
 	/* LESS OR EQUAL */
	{
		"name": 'LESS_OR_EQUAL',
		"symbol": '<=',
		"binary": true,
		"doOperation": TValue.lessOrEqual
	},
	
 	/* GREATER */
	{
		"name": 'GREATER',
		"symbol": '>',
		"binary": true,
		"doOperation": TValue.greater
	},
	
 	/* GREATER_OR_EQUAL */
	{
		"name": 'GREATER_OR_EQUAL',
		"symbol": '>=',
		"binary": true,
		"doOperation": TValue.greaterOrEqual
	},
	
];

/* Type enumeration. */
TArithmeticFunctions.Type = 
{
	"ABSOLUTE": 0,
	"NEGATE": 1,
	"LOGICAL_NOT": 2,
	"ADD": 3,
	"SUBTRACT": 4,
	"MULTIPLY": 5,
	"DIVIDE": 6,
	"MODULO": 7,
	"POWER": 8,
	"LOGICAL_AND": 9,
	"LOGICAL_OR": 10,
	"LOGICAL_XOR": 11,
	"EQUALS": 12,
	"NOT_EQUALS": 13,
	"STRICT_EQUALS": 14,
	"STRICT_NOT_EQUALS": 15,
	"LESS": 16,
	"LESS_OR_EQUAL": 17,
	"GREATER": 18,
	"GREATER_OR_EQUAL": 19
};

TArithmeticFunctions.COUNT = TArithmeticFunctions.Type.length; 

//[[EXPORTJS-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TArithmeticFunctions;
// =========================================================================
