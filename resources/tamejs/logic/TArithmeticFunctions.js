/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
// ======================================================================================================

//##[[CONTENT-START

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
	
 	/* NOT */
	{
		"name": 'NOT',
		"symbol": '~',
		"binary": false,
		"doOperation": TValue.not
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
	
 	/* AND */
	{
		"name": 'AND',
		"symbol": '&',
		"binary": true,
		"doOperation": TValue.and
	},
	
 	/* OR */
	{
		"name": 'OR',
		"symbol": '|',
		"binary": true,
		"doOperation": TValue.or
	},
	
 	/* XOR */
	{
		"name": 'XOR',
		"symbol": '^',
		"binary": true,
		"doOperation": TValue.xor
	},
	
 	/* LSHIFT */
	{
		"name": 'LSHIFT',
		"symbol": '<<',
		"binary": true,
		"doOperation": TValue.leftShift
	},
	
 	/* RSHIFT */
	{
		"name": 'RSHIFT',
		"symbol": '>>',
		"binary": true,
		"doOperation": TValue.rightShift
	},
	
 	/* RSHIFTPAD */
	{
		"name": 'RSHIFTPAD',
		"symbol": '>>>',
		"binary": true,
		"doOperation": TValue.rightShiftPadded
	},
	
 	/* LOGICAL AND */
	{
		"name": 'LOGICAL_AND',
		"symbol": '&&',
		"binary": true,
		"doOperation": TValue.logicalAnd
	},
	
 	/* LOGICAL OR */
	{
		"name": 'LOGICAL_OR',
		"symbol": '||',
		"binary": true,
		"doOperation": TValue.logicalOr
	},
	
 	/* LOGICAL XOR */
	{
		"name": 'LOGICAL_XOR',
		"symbol": '^^',
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
	"NOT": 3,
	"ADD": 4,
	"SUBTRACT": 5,
	"MULTIPLY": 6,
	"DIVIDE": 7,
	"MODULO": 8,
	"POWER": 9,
	"AND": 10,
	"OR": 11,
	"XOR": 12,
	"LSHIFT": 13,
	"RSHIFT": 14,
	"RSHIFTPAD": 15,
	"LOGICAL_AND": 16,
	"LOGICAL_OR": 17,
	"LOGICAL_XOR": 18,
	"EQUALS": 19,
	"NOT_EQUALS": 20,
	"STRICT_EQUALS": 21,
	"STRICT_NOT_EQUALS": 22,
	"LESS": 23,
	"LESS_OR_EQUAL": 24,
	"GREATER": 25,
	"GREATER_OR_EQUAL": 26
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TArithmeticFunctions;
// =========================================================================
