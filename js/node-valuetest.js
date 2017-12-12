var TValue = require("../resources/tamejs/engine/objects/TValue.js");
var TArithmeticFunctions = require("../resources/tamejs/engine/logic/TArithmeticFunctions.js");

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
	TValue.createList([]),
	TValue.createList([TValue.createBoolean(true), TValue.createInteger(3), TValue.createFloat(5.0), TValue.createString("orange")]),
];

function printEmpty(v1)
{
	console.log(TValue.toString(v1) + " > EMPTY? > " + TValue.isEmpty(v1));
}

function printLength(v1)
{
	console.log(TValue.toString(v1) + " > LENGTH > " + TValue.length(v1));
}

function printBoolean(v1)
{
	console.log(TValue.toString(v1) + " > BOOLEAN > " + TValue.asBoolean(v1));
}

function printInteger(v1)
{
	console.log(TValue.toString(v1) + " > INT > " + TValue.asLong(v1));
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
	printEmpty(TEST_VALUES[i]);
console.log("-------------------------------");
for (i = 0; i < TEST_VALUES.length; i++)
	printLength(TEST_VALUES[i]);
console.log("-------------------------------");
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
