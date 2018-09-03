/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TStringBuilder = TStringBuilder || ((typeof require) !== 'undefined' ? require('../TStringBuilder.js') : null);
// ======================================================================================================
Util.nanoTime = function()
{
	// s,ns to ns (ns res)
	var t = process.hrtime();
	return t[0] * 1e9 + t[1];
};

Util.toBase64 = (function()
{
	if (Buffer.from)
	{
		return function(text) {
			return Buffer.from(text).toString('base64');
		};
	}
	else
	{
		return function(text) {
			return (new Buffer(text)).toString('base64');
		};
	}
})();

Util.fromBase64 = (function()
{
	if (Buffer.from)
	{
		return function(data) {
			return Buffer.from(data, 'base64').toString('utf8');
		};
	}
	else
	{
		return function(data) {
			return (new Buffer(data, 'base64')).toString('utf8');
		};
	}
})();

//##[[EXPORTJS-START

/*****************************************************************************
 See com.tameif.tame.lang.Value
 *****************************************************************************/
var TValue = {};

/* Type Constants */
TValue.Type = 
{
	"BOOLEAN": "BOOLEAN",
	"INTEGER": "INTEGER",
	"FLOAT": "FLOAT",
	"STRING": "STRING",
	"LIST": "LIST",
	"OBJECT": "OBJECT",
	"CONTAINER": "CONTAINER",
	"PLAYER": "PLAYER",
	"ROOM": "ROOM",
	"WORLD": "WORLD",
	"ACTION": "ACTION",
	"VARIABLE": "VARIABLE"
};

// Factory.
TValue.create = function(type, value)
{
	if (!type)
		throw TAMEError.UnexpectedValueType("Invalid value type in TValue()");
	if (typeof value === 'undefined' || value === null)
		throw TAMEError.UnexpectedValueType("Value cannot be undefined or null in TValue()");

	var out = {};
	out.type = type;
	out.value = value;
	return out;
};

// Convenience constructors.
TValue.createBoolean = function(value) {return TValue.create(TValue.Type.BOOLEAN, Boolean(value));};
TValue.createInteger = function(value) {return TValue.create(TValue.Type.INTEGER, parseInt(value, 10));};
TValue.createFloat = function(value) {return TValue.create(TValue.Type.FLOAT, parseFloat(value));};
TValue.createString = function(value) {return TValue.create(TValue.Type.STRING, Util.toBase64(String(value)));};
TValue.createList = function(value) {return TValue.create(TValue.Type.LIST, value);};
TValue.createWorld = function() {return TValue.create(TValue.Type.WORLD, Util.toBase64("world"));};
TValue.createObject = function(value) {return TValue.create(TValue.Type.OBJECT, Util.toBase64(String(value)));};
TValue.createContainer = function(value) {return TValue.create(TValue.Type.CONTAINER, Util.toBase64(String(value)));};
TValue.createPlayer = function(value) {return TValue.create(TValue.Type.PLAYER, Util.toBase64(String(value)));};
TValue.createRoom = function(value) {return TValue.create(TValue.Type.ROOM, Util.toBase64(String(value)));};
TValue.createAction = function(value) {return TValue.create(TValue.Type.ACTION, Util.toBase64(String(value)));};
TValue.createVariable = function(value) {return TValue.create(TValue.Type.VARIABLE, Util.toBase64(String(value)));};
TValue.createNaN = function() {return TValue.create(TValue.Type.FLOAT, NaN);};
TValue.createInfinity = function() {return TValue.create(TValue.Type.FLOAT, Infinity);};
TValue.createNegativeInfinity = function() {return TValue.create(TValue.Type.FLOAT, -Infinity);};
TValue.createValue = function(value) {return TValue.create(value.type, value.value);};

/**
 * Returns if this value is equal to another, value-wise.
 * If they are literals, they are compared by their string values.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return true if so, false if not.
 */
TValue.areEqualIgnoreType = function(v1, v2)
{
	if (TValue.isLiteral(v1) && TValue.isLiteral(v2))
	{
		if (TValue.isList(v1) || TValue.isList(v2))
			return TValue.areEqual(v1, v2);
		else if (TValue.isString(v1) && TValue.isString(v2))
			return v1.value == v2.value;
		else
			return TValue.asDouble(v1) == TValue.asDouble(v2);
	}
	else
		return TValue.areEqual(v1, v2); 
};

/**
 * Returns if this value is equal to another: PERFECTLY EQUAL, type strict.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return true if so, false if not.
 */
TValue.areEqual = function(v1, v2)
{
	if (TValue.isStrictlyNaN(v1))
		return false;
	else if (TValue.isStrictlyNaN(v2))
		return false;
	else if (TValue.isList(v1))
		return false;
	else if (TValue.isList(v2))
		return false;
	else
		return v1.type === v2.type && v1.value === v2.value;
};

/**
 * Compares two values.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return -1 if v1 < v2, 0 if equal (ignore type), 1 if v1 > v2.
 */
TValue.compare = function(v1, v2)
{
	if (TValue.areEqual(v1, v2))
		return 0;
	
	if (!TValue.isLiteral(v1) || !TValue.isLiteral(v2))
		return -Number.MAX_VALUE;

	if (TValue.isList(v1) || TValue.isList(v2))
		return -Number.MAX_VALUE;
	
	if (TValue.isString(v1) || TValue.isString(v2))
	{
		let d1 = TValue.asString(v1);
		let d2 = TValue.asString(v2);
		return Util.strcmp(d1, d2);
	}
	
	if (TValue.isFloatingPoint(v1) || TValue.isFloatingPoint(v2))
	{
		let d1 = TValue.asDouble(v1);
		let d2 = TValue.asDouble(v2);
		return d1 === d2 ? 0 : (d1 < d2 ? -1 : 1);
	}
	
	if (TValue.isInteger(v1) || TValue.isInteger(v2))
	{
		let d1 = TValue.asLong(v1);
		let d2 = TValue.asLong(v2);
		return d1 === d2 ? 0 : (d1 < d2 ? -1 : 1);
	}
	
	if (TValue.isBoolean(v1) || TValue.isBoolean(v2))
	{
		let d1 = TValue.asBoolean(v1);
		let d2 = TValue.asBoolean(v2);
		return d1 === d2 ? 0 : (!d1 ? -1 : 1);
	}
	
	return 0;
};

/**
 * Returns the absolute value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.absolute = function(value1)
{
	if (TValue.isInteger(value1))
		return TValue.createInteger(Math.abs(TValue.asLong(value1)));
	else if (TValue.isNumeric(value1))
		return TValue.createFloat(Math.abs(TValue.asDouble(value1)));
	else
		return TValue.createNaN();
};

/**
 * Returns the negative value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.negate = function(value1)
{
	if (TValue.isInteger(value1))
		return TValue.createInteger(-TValue.asLong(value1));
	else if (TValue.isNumeric(value1))
		return TValue.createFloat(-TValue.asDouble(value1));
	else
		return TValue.createNaN();
};

/**
 * Returns the "logical not" value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value as a boolean value.
 */
TValue.logicalNot = function(value1)
{
	if (TValue.isLiteral(value1))
		return TValue.createBoolean(!TValue.asBoolean(value1));
	else
		return TValue.createNaN();
};

/**
 * Returns the bitwise compliment value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.not = function(value1)
{
	if (TValue.isInfinite(value1))
		return TValue.createInteger(-1);
	else if (TValue.isNaN(value1))
		return TValue.createInteger(-1);
	else if (TValue.isBoolean(value1))
		return TValue.createBoolean(!TValue.asBoolean(value1));
	else if (TValue.isNumeric(value1))
		return TValue.createInteger(~TValue.asLong(value1));
	else if (TValue.isString(value1))
		return TValue.createInteger(-1);
	else
		return TValue.createNaN();
};

/**
 * Returns the addition of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 */
TValue.add = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		let v1 = TValue.asBoolean(value1);
		let v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 || v2);
	}
	else if (TValue.isString(value1) || TValue.isString(value2))
	{
		let v1 = TValue.asString(value1);
		let v2 = TValue.asString(value2);
		return TValue.createString(v1 + v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		let v1 = TValue.asLong(value1);
		let v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 + v2);
	}
	else
	{
		let v1 = TValue.asDouble(value1);
		let v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 + v2);
	}
};

/**
 * Returns the subtraction of the second literal value from the first.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 */
TValue.subtract = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();
	if (TValue.isList(value1) || TValue.isList(value2))
		return TValue.createNaN();
	if (TValue.isString(value1) || TValue.isString(value2))
		return TValue.createNaN();

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		let v1 = TValue.asBoolean(value1);
		let v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && !v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		let v1 = TValue.asLong(value1);
		let v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 - v2);
	}
	else
	{
		let v1 = TValue.asDouble(value1);
		let v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 - v2);
	}
};

/**
 * Returns the multiplication of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 */
TValue.multiply = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();
	if (TValue.isList(value1) || TValue.isList(value2))
		return TValue.createNaN();
	if (TValue.isString(value1) || TValue.isString(value2))
		return TValue.createNaN();

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		let v1 = TValue.asBoolean(value1);
		let v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		let v1 = TValue.asLong(value1);
		let v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 * v2);
	}
	else
	{
		let v1 = TValue.asDouble(value1);
		let v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 * v2);
	}
};

/**
 * Returns the division of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.divide = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();
	if (TValue.isList(value1) || TValue.isList(value2))
		return TValue.createNaN();
	if (TValue.isString(value1) || TValue.isString(value2))
		return TValue.createNaN();

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		let v1 = TValue.asLong(value1);
		let v2 = TValue.asLong(value2);
		if (v2 === 0)
		{
			if (v1 !== 0)
				return v1 < 0 ? TValue.createNegativeInfinity() : TValue.createInfinity();
			else
				return TValue.createNaN();
		}
		else
			return TValue.createInteger(v1 / v2);
	}
	else
	{
		let v1 = TValue.asDouble(value1);
		let v2 = TValue.asDouble(value2);
		if (v2 === 0.0)
		{
			if (!Number.isNaN(v1) && v1 !== 0.0)
				return v1 < 0.0 ? TValue.createNegativeInfinity() : TValue.createInfinity();
			else
				return TValue.createNaN();
		}
		else
			return TValue.createFloat(v1 / v2);
	}
};

/**
 * Returns the modulo of one literal value using another.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.modulo = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();
	if (TValue.isList(value1) || TValue.isList(value2))
		return TValue.createNaN();
	if (TValue.isString(value1) || TValue.isString(value2))
		return TValue.createNaN();

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		let v1 = TValue.asLong(value1);
		let v2 = TValue.asLong(value2);
		if (v2 === 0)
			return TValue.createNaN();
		else
			return TValue.createInteger(v1 % v2);
	}
	else
	{
		let v1 = TValue.asDouble(value1);
		let v2 = TValue.asDouble(value2);
		if (v2 === 0.0)
			return TValue.createNaN();
		else
			return TValue.createFloat(v1 % v2);
	}
};

/**
 * Returns the result of one value raised to a certain power. 
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.power = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createNaN();
	if (TValue.isList(value1) || TValue.isList(value2))
		return TValue.createNaN();
	if (TValue.isString(value1) || TValue.isString(value2))
		return TValue.createNaN();

	let v1 = TValue.asDouble(value1);
	let v2 = TValue.asDouble(value2);
	let p = Math.pow(v1, v2);
	return TValue.createFloat(p);
};

/**
 * Returns the "logical and" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.logicalAnd = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	
	let v1 = TValue.asBoolean(value1);
	let v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 && v2);
};

/**
 * Returns the "logical or" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.logicalOr = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	
	let v1 = TValue.asBoolean(value1);
	let v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 || v2);
};

/**
 * Returns the "logical xor" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.logicalXOr = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	
	let v1 = TValue.asBoolean(value1);
	let v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 ^ v2);
};

/**
 * Returns if two values are equal, no type safety if they are literals.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.equals = function(value1, value2)
{
	return TValue.createBoolean(TValue.areEqualIgnoreType(value1, value2));
};

/**
 * Returns if two values are not equal, no type safety if they are literals.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.notEquals = function(value1, value2)
{
	return TValue.createBoolean(!TValue.areEqualIgnoreType(value1, value2));
};

/**
 * Returns if two values are equal, with type strictness.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.strictEquals = function(value1, value2)
{
	return TValue.createBoolean(TValue.areEqual(value1, value2));
};

/**
 * Returns if two values are not equal, with type strictness.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.strictNotEquals = function(value1, value2)
{
	return TValue.createBoolean(!TValue.areEqual(value1, value2));
};

/**
 * Returns if the first literal value is less than the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.less = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) < 0);
};

/**
 * Returns if the first literal value is less than or equal to the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.lessOrEqual = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) <= 0);
};

/**
 * Returns if the first literal value is greater than the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.greater = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) > 0);
};

/**
 * Returns if the first literal value is greater than or equal to the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 */
TValue.greaterOrEqual = function(value1, value2)
{
	if (!TValue.isLiteral(value1) || !TValue.isLiteral(value2))
		return TValue.createBoolean(false);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) >= 0);
};

/**
 * Returns if this value is a boolean value.
 * @return true if so, false if not.
 */
TValue.isBoolean = function(value)
{
	return value.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value is an integer.
 * @return true if so, false if not.
 */
TValue.isInteger = function(value)
{
	return value.type === TValue.Type.INTEGER;
};

/**
 * Returns if this value is a floating-point number.
 * @return true if so, false if not.
 */
TValue.isFloatingPoint = function(value)
{
	return value.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a number.
 * @return true if so, false if not.
 */
TValue.isNumeric = function(value)
{
	return value.type === TValue.Type.INTEGER ||
		value.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a string value.
 * @return true if so, false if not.
 */
TValue.isString = function(value)
{
	return value.type === TValue.Type.STRING;
};

/**
 * Returns if this value is a list.
 * @return true if so, false if not.
 */
TValue.isList = function(value)
{
	return value.type === TValue.Type.LIST;
};

/**
 * Returns if this value is copy-by-reference.
 * @return true if so, false if not.
 */
TValue.isReferenceCopied = function(value)
{
	return value.type === TValue.Type.LIST;
};

/**
 * Returns if this value is a literal value (or list).
 * @return true if so, false if not.
 */
TValue.isLiteral = function(value)
{
	return (value.type === TValue.Type.BOOLEAN ||
		value.type === TValue.Type.INTEGER ||
		value.type === TValue.Type.FLOAT ||
		value.type === TValue.Type.STRING ||
		value.type === TValue.Type.LIST);
};

/**
 * Returns if this value represents an element.
 * @return true if so, false if not.
 */
TValue.isElement = function(value)
{
	return value.type === TValue.Type.OBJECT ||
		value.type === TValue.Type.PLAYER ||
		value.type === TValue.Type.ROOM ||
		value.type === TValue.Type.CONTAINER ||
		value.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object container.
 * @return true if so, false if not.
 */
TValue.isObjectContainer = function(value)
{
	return value.type === TValue.Type.PLAYER ||
		value.type === TValue.Type.ROOM ||
		value.type === TValue.Type.CONTAINER ||
		value.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object.
 * @return true if so, false if not.
 */
TValue.isObject = function(value)
{
	return value.type === TValue.Type.OBJECT;
};

/**
 * Returns if this value represents a room.
 * @return true if so, false if not.
 */
TValue.isRoom = function(value)
{
	return value.type === TValue.Type.ROOM;
};

/**
 * Returns if this value represents a player.
 * @return true if so, false if not.
 */
TValue.isPlayer = function(value)
{
	return value.type === TValue.Type.PLAYER;
};

/**
 * Returns if this value represents a container.
 * @return true if so, false if not.
 */
TValue.isContainer = function(value)
{
	return value.type === TValue.Type.CONTAINER;
};

/**
 * Returns if this value represents an action.
 * @return true if so, false if not.
 */
TValue.isAction = function(value)
{
	return value.type === TValue.Type.ACTION;
};

/**
 * Returns if this value represents a variable.
 * @return true if so, false if not.
 */
TValue.isVariable = function(value)
{
	return value.type === TValue.Type.VARIABLE;
};

/**
 * Returns if this value represents a boolean.
 * @return true if so, false if not.
 */
TValue.isBoolean = function(value)
{
	return value.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value evaluates to <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.isNaN = function(value)
{
	return Number.isNaN(TValue.asDouble(value));
};

/**
 * Returns if this value is floating point and literally <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.isStrictlyNaN = function(value)
{
	return TValue.isFloatingPoint(value) && TValue.isNaN(value);
};

/**
 * Returns if this value evaluates to positive or negative infinity.
 * @return true if so, false if not.
 */
TValue.isInfinite = function(value)
{
	let v = TValue.asDouble(value);
	return v === Infinity || v === -Infinity;
};

/**
 * Returns this value as a long value.
 * @return the long value of this value.
 */
TValue.asLong = function(value)
{
	if (TValue.isInfinite(value) || TValue.isNaN(value))
		return 0;
	else if (TValue.isBoolean(value))
		return TValue.asBoolean(value) ? 1 : 0;
	else if (TValue.isInteger(value))
		return value.value;
	else if (TValue.isFloatingPoint(value))
		return parseInt(value.value, 10);
	else if (TValue.isString(value))
		return parseInt(TValue.asDouble(value), 10);
	else
		return 0;
};

/**
 * Returns the double value of this value.
 * @return the double value of this value, or {@link Double#NaN} if not parsable as a number.
 */
TValue.asDouble = function(value)
{
	if (TValue.isBoolean(value))
		return TValue.asBoolean(value) ? 1.0 : 0.0;
	else if (TValue.isInteger(value))
		return parseFloat(value.value);
	else if (TValue.isFloatingPoint(value))
		return value.value;
	else if (TValue.isString(value))
	{
		let str = Util.fromBase64(value.value).toLowerCase();
		if (str === "nan")
			return NaN;
		else if (str === "infinity")
			return Infinity;
		else if (str === "-infinity")
			return -Infinity;
		else
			return parseFloat(str);
	}
	else
		return NaN;
};

/**
 * Returns the String value of this value (not the same as toString()!!).
 * @return the String value of this value.
 */
TValue.asString = function(value)
{
	if (TValue.isList(value))
	{
		let tsb = new TStringBuilder();
		tsb.append("[");
		for (let i = 0; i < value.value.length; i++)
		{
			tsb.append(TValue.asString(value.value[i]));
			if (i < value.value.length - 1)
				tsb.append(", ");
		}
		tsb.append("]");
		return tsb.toString();
	}
	else if (TValue.isString(value) || TValue.isElement(value) || TValue.isVariable(value) || TValue.isAction(value))
		return Util.fromBase64(value.value);
	else if (TValue.isInfinite(value) || TValue.isNaN(value))
		return ""+value.value;
	else if (TValue.isFloatingPoint(value))
	{
		// make it equal to Java/C#
		let d = TValue.asDouble(value);
		if (Math.abs(d) === 0.0)
			return "0.0";
		else if (Math.abs(d) < 0.001 || Math.abs(d) >= 10000000)
		{
			let out = d.toExponential().toUpperCase().replace('+','');
			if (out.indexOf('.') < 0)
			{
				let ie = out.indexOf('E');
				return out.substring(0, ie) + ".0" + out.substring(ie);
			}
			else
				return out;
		}
		else if (d % 1 === 0)		
			return value.value+".0";
		else
			return ""+value.value;
	}
	else
		return ""+value.value;
};

/**
 * Returns this value as a boolean value.
 * @return true if this evaluates true, false if not.
 */
TValue.asBoolean = function(value)
{
	if (TValue.isBoolean(value))
		return value.value;
	else if (TValue.isFloatingPoint(value))
	{
		if (TValue.isInfinite(value))
			return true;
		else if (Number.isNaN(value.value))
			return false;
		else
			return TValue.asDouble(value) !== 0;
	}
	else if (TValue.isInteger(value))
		return TValue.asLong(value) !== 0;
	else if (TValue.isString(value))
		return Util.fromBase64(value.value).length !== 0;
	else
		return true; // all objects are true
};

/**
 * Returns if this value evaluates to "true".
 * @return true if so, false if not.
 */
TValue.isTrue = function(value)
{
	return TValue.asBoolean(value);
};
    
/**
 * @return a string representation of this value (for debug, usually).
 */
TValue.toString = function(value)
{
	let sb = new TStringBuilder();
	sb.append(value.type);
	sb.append('[');
	if (TValue.isList(value))
	{
		sb.append('[');
		for (let i = 0; i < value.value.length; i++)
		{
			sb.append(TValue.toString(value.value[i]));
			if (i < value.value.length - 1)
				sb.append(', ');
		}
		sb.append(']');
	}
	else
	{
		sb.append(TValue.asString(value));
	}
	sb.append(']');
	return sb.toString();
};

/**
 * Gets if a value is "empty."
 * If boolean, this returns true if and only if it is false.
 * If numeric, this returns true if and only if the value is 0 or NaN.
 * If string, this returns true if and only if the value, trimmed, is length 0.
 * If list, this returns true if and only if the list is length 0.
 * Otherwise, false.
 * @return true if this value is "empty", false if not.
 */
TValue.isEmpty = function(value)
{
	if (TValue.isStrictlyNaN(value))
		return true;
	else if (TValue.isBoolean(value))
		return !TValue.asBoolean(value);
	else if (TValue.isNumeric(value))
		return TValue.asDouble(value) === 0.0;
	else if (TValue.isString(value))
		return TValue.asString(value).trim().length === 0;
	else if (TValue.isList(value))
		return value.value.length === 0;
	else
		return false;
};

/**
 * Gets the length of this value.
 * If string, this returns the string length in characters.
 * If list, this returns the cardinality.
 * Otherwise, 1.
 * @return the length.
 */
TValue.length = function(value)
{
	if (TValue.isList(value))
		return value.value.length;
	else if (TValue.isString(value))
		return TValue.asString(value).length;
	else
		return 1;
};

/**
 * Adds a value to this, if this is a list.
 * @param listValue the list value.
 * @param v the value to add.
 * @return true if added, false if not.
 */
TValue.listAdd = function(listValue, v)
{
	if (!TValue.isList(listValue))
		return false;
	listValue.value.push(TValue.createValue(v));
	return true;
};

/**
 * Adds a value to this at a specific index, if this is a list.
 * @param listValue the list value.
 * @param i the index to add the value at.
 * @param v the value to add.
 * @return true if added, false if not.
 */
TValue.listAddAt = function(listValue, i, v)
{
	if (!TValue.isList(listValue))
		return false;
	listValue.value.splice(i, 0, TValue.createValue(v));
	return true;
};

/**
 * Sets a value on this at a specific index, if this is a list.
 * @param listValue the list value.
 * @param i the index to set.
 * @param v the value to set.
 * @return true if set, false if not (index is out of range).
 */
TValue.listSet = function(listValue, i, v)
{
	if (!TValue.isList(listValue))
		return false;
	
	if (i < 0 || i >= listValue.value.length)
		return false;
	
	listValue.value[i] = TValue.createValue(v);
	return true;
};

/**
 * Gets a value on this at a specific index, if this is a list.
 * @param listValue the list value.
 * @param i the index to get.
 * @return the value (new instance via TValue.createValue()) or false if not found.
 */
TValue.listGet = function(listValue, i)
{
	if (!TValue.isList(listValue))
		return TValue.createBoolean(false);
	
	if (i < 0 || i >= listValue.value.length)
		return TValue.createBoolean(false);
	
	return TValue.createValue(listValue.value[i]);
};

/**
 * Gets the index of a value from this, if this is a list.
 * Remember, list-typed values are compared by reference!
 * @param listValue the list value.
 * @param v the value to search for.
 * @return the index of the matching value, or -1 if not found.
 */
TValue.listIndexOf = function(listValue, v)
{
	if (!TValue.isList(listValue))
		return -1;
	
	for (let i = 0; i < listValue.value.length; i++)
	{
		if (TValue.areEqual(v, listValue.value[i]))
			return i;
	}
	
	return -1;
};

/**
 * Checks if this value contains a value, if this is a list.
 * Remember, list-typed values are compared by reference!
 * @param listValue the list value.
 * @param v the value to search for.
 * @return true if so, false if not.
 */
TValue.listContains = function(listValue, v)
{
	return TValue.listIndexOf(listValue, v) >= 0;
};

/**
 * Removes a value from inside this value, if this is a list.
 * Remember, list-typed values are compared by reference!
 * @param listValue the list value.
 * @param v the value to remove.
 * @return true if a value was removed or false if not found.
 */
TValue.listRemove = function(listValue, v)
{
	if (!TValue.isList(listValue))
		return false;

	let i = TValue.listIndexOf(listValue, v);
	if (i < 0)
		return false;
	
	listValue.value.splice(i, 1);
	return true;
};

/**
 * Removes a value from this at a specific index, if this is a list.
 * @param listValue the list value.
 * @param i the index to get.
 * @return the value removed (new instance via {@link #create(Value)}) or false if not found.
 */
TValue.listRemoveAt = function(listValue, i)
{
	if (!TValue.isList(listValue))
		return TValue.createBoolean(false);

	if (i < 0 || i >= listValue.value.length)
		return TValue.createBoolean(false);
	
	return TValue.createValue(listValue.value.splice(i, 1)[0]);
};

//##[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TValue;
// =========================================================================
