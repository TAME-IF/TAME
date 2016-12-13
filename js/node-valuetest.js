var TValue = require("../resources/tamejs/objects/TValue.js");
var TArithmeticFunctions = require("../resources/tamejs/logic/TArithmeticFunctions.js");

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
	console.log(v1.toString() + " > BOOLEAN > " +v1.asBoolean());
}

function printInteger(v1)
{
	console.log(v1.toString() + " > INT > " +v1.asLong());
}

function printFloat(v1)
{
	var s = v1.asDouble() % 1 == 0 ? v1.asDouble()+'.0' : v1.asDouble()+''
	console.log(v1.toString() + " > FLOAT > " +(s));
}

function printString(v1)
{
	console.log(v1.toString() + " > STRING > \"" +v1.asString()+ "\"");
}

function print1(op, opSign, v1)
{
	console.log(opSign +" "+ v1.toString() + " = " + op(v1).toString());
}

function print2(op, opSign, v1, v2)
{
	console.log(v1.toString() + " " + opSign + " " + v2.toString() + " = " + op(v1, v2).toString());
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

	// output is inconsistently represented between Java and JS
	if (x == 'POWER')
		continue;
	
	
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

