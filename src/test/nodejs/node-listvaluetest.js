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
];

var listValue = TValue.createList([]);

for (i = 0; i < TEST_VALUES.length; i++)
	TValue.listAdd(listValue, TEST_VALUES[i]);

console.log(TValue.asString(listValue));

for (i = 0; i < TEST_VALUES.length; i++)
	TValue.listRemove(listValue, TEST_VALUES[i]);

console.log(TValue.asString(listValue));

for (i = 0; i < TEST_VALUES.length; i++)
	TValue.listAddAt(listValue, 0, TEST_VALUES[i]);

console.log(TValue.asString(listValue));

while (!TValue.isEmpty(listValue))
	TValue.listRemoveAt(listValue, 0);

console.log(TValue.asString(listValue));

TValue.listAdd(listValue, TValue.createBoolean(false));

for (i = 0; i < TEST_VALUES.length; i++)
	TValue.listSet(listValue, 0, TEST_VALUES[i]);

console.log(TValue.asString(listValue));

listValue = TValue.createList([]);
for (i = 0; i < TEST_VALUES.length; i++)
	TValue.listAdd(listValue, TEST_VALUES[i]);

console.log(TValue.asString(listValue));

for (i = 0; i < TEST_VALUES.length; i++)
	console.log(TValue.listIndexOf(listValue, TEST_VALUES[i]));

console.log(TValue.asString(listValue));
console.log(TValue.length(listValue));

