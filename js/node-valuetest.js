var TValue = require("../resources/tamejs/objects/TValue.js");
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


var TEST_VALUES = [
	TValue.createBoolean(false),
	TValue.createBoolean(true),
	TValue.createInfinity(),
	TValue.createNegativeInfinity(),
	TValue.createNaN(),
	TValue.createInteger(0),
	TValue.createFloat(0.0),
	TValue.createInteger(10),
	TValue.createInteger(3),
	TValue.createFloat(10.0),
	TValue.createFloat(3.0),
	TValue.createFloat(10.5),
	TValue.createFloat(3.5),
	TValue.createInteger(-10),
	TValue.createInteger(-3),
	TValue.createFloat(-10.0),
	TValue.createFloat(-3.0),
	TValue.createFloat(-10.5),
	TValue.createFloat(-3.5),
	TValue.createString(""),
	TValue.createString("0"),
	TValue.createString("0.0"),
	TValue.createString("10"),
	TValue.createString("3"),
	TValue.createString("10.0"),
	TValue.createString("3.0"),
	TValue.createString("10.5"),
	TValue.createString("3.5"),
	TValue.createString("-10"),
	TValue.createString("-3"),
	TValue.createString("-10.0"),
	TValue.createString("-3.0"),
	TValue.createString("-10.5"),
	TValue.createString("-3.5"),
	TValue.createString("apple"),
	TValue.createString("banana"),
];

function printBoolean(v1)
{
	console.log(TValue.toString(v1) + " > BOOLEAN > " +TValue.asBoolean(v1));
}

function printInteger(v1)
{
	console.log(TValue.toString(v1) + " > INT > " +TValue.asLong(v1));
}

function printFloat(v1)
{
	var s = TValue.asDouble(v1) % 1 == 0 ? TValue.asDouble(v1)+'.0' : TValue.asDouble(v1)+''
	console.log(TValue.toString(v1) + " > FLOAT > " +(s));
}

function printString(v1)
{
	console.log(TValue.toString(v1) + " > STRING > \"" +TValue.asString(v1)+ "\"");
}

function print1(op, opSign, v1)
{
	console.log(opSign +" "+ TValue.toString(v1) + " = " + TValue.toString(op(v1)));
}

function print2(op, opSign, v1, v2)
{
	console.log(TValue.toString(v1) + " " + opSign + " " + TValue.toString(v2) + " = " + TValue.toString(op(v1, v2)));
}

var i;
for (i = 0; i < TEST_VALUES.length; i++)
	printBoolean(TEST_VALUES[i]);
console.log("-------------------------------");
for (i = 0; i < TEST_VALUES.length; i++)
	printInteger(TEST_VALUES[i]);
console.log("-------------------------------");
for (i = 0; i < TEST_VALUES.length; i++)
	printFloat(TEST_VALUES[i]);
console.log("-------------------------------");
for (i = 0; i < TEST_VALUES.length; i++)
	printString(TEST_VALUES[i]);
console.log("-------------------------------");

for (var x in TArithmeticFunctions.Type) if (TArithmeticFunctions.Type.hasOwnProperty(x))
{
	var afunc = TArithmeticFunctions[TArithmeticFunctions.Type[x]];

	if (afunc.binary)
	{
		for (var i = 0; i < TEST_VALUES.length; i++)
			for (var j = 0; j < TEST_VALUES.length; j++)
				print2(afunc.doOperation, afunc.symbol, TEST_VALUES[i], TEST_VALUES[j]);
	}
	else
	{
		for (var i = 0; i < TEST_VALUES.length; i++)
			print1(afunc.doOperation, afunc.symbol, TEST_VALUES[i]);
	}
	console.log("-------------------------------");
}

