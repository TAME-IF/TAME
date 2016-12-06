var TValue = require("./objects/TValue.js");

var TEST_VALUES = [
	TValue.createBoolean(false),
	TValue.createBoolean(true),
	TValue.createInfinity(),
	TValue.createNegativeInfinity(),
	TValue.createNaN(Double.NaN),
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

var w = TValue.createWorld();
var o = TValue.createObject("o_asdf");
var r = TValue.createRoom("r_asdf");
var p = TValue.createPlayer("p_asdf");
var c = TValue.createContainer("c_asdf");
var a = TValue.createAction("a_asdf");
var v = TValue.createVariable("butt");
console.log('===========================================================');
console.log(w);
console.log(o);
console.log(r);
console.log(p);
console.log(c);
console.log(a);
console.log(v);
console.log('===========================================================');

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
	console.log(v1.toString() + " > FLOAT > " +v1.asDouble());
}

function printString(v1)
{
	console.log(v1.toString() + " > STRING > \"" +v1.asString()+ "\"");
}

function print(op, opSign, v1)
{
	console.log(opSign +" "+ v1.toString() + " = " + op(v1).toString());
}

function print(op, opSign, v1, v2)
{
	console.log(v1.toString() + " " + opSign + " " + v2.toString() + " = " + op(v1, v2).toString());
}

// TODO: Finish.


