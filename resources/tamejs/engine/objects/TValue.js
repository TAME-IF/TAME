/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
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
 See net.mtrop.tame.lang.Value
 *****************************************************************************/
var TValue = {};

/* Type Constants */
TValue.Type = 
{
	"BOOLEAN": "BOOLEAN",
	"INTEGER": "INTEGER",
	"FLOAT": "FLOAT",
	"STRING": "STRING",
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
	if (typeof value === 'undefined' || value == null)
		throw TAMEError.UnexpectedValueType("Value cannot be undefined or null in TValue()");

	var out = {};
	out.type = type;
	out.value = value;
	return out;
};

// Convenience constructors.
TValue.createBoolean = function(value) {return TValue.create(TValue.Type.BOOLEAN, Boolean(value));}
TValue.createInteger = function(value) {return TValue.create(TValue.Type.INTEGER, parseInt(value, 10));}
TValue.createFloat = function(value) {return TValue.create(TValue.Type.FLOAT, parseFloat(value));}
TValue.createString = function(value) {return TValue.create(TValue.Type.STRING, Util.toBase64(String(value)));}
TValue.createWorld = function() {return TValue.create(TValue.Type.WORLD, Util.toBase64("world"));}
TValue.createObject = function(value) {return TValue.create(TValue.Type.OBJECT, Util.toBase64(String(value)));}
TValue.createContainer = function(value) {return TValue.create(TValue.Type.CONTAINER, Util.toBase64(String(value)));}
TValue.createPlayer = function(value) {return TValue.create(TValue.Type.PLAYER, Util.toBase64(String(value)));}
TValue.createRoom = function(value) {return TValue.create(TValue.Type.ROOM, Util.toBase64(String(value)));}
TValue.createAction = function(value) {return TValue.create(TValue.Type.ACTION, Util.toBase64(String(value)));}
TValue.createVariable = function(value) {return TValue.create(TValue.Type.VARIABLE, Util.toBase64(String(value)));}
TValue.createNaN = function() {return TValue.create(TValue.Type.FLOAT, NaN);}
TValue.createInfinity = function() {return TValue.create(TValue.Type.FLOAT, Infinity);}
TValue.createNegativeInfinity = function() {return TValue.create(TValue.Type.FLOAT, -Infinity);}

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
		if (TValue.isString(v1) && TValue.isString(v2))
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
	// complete equality.
	if (TValue.areEqual(v1, v2))
		return 0;
	
	var d1 = null;
	var d2 = null;

	// one is not a literal
	if (!TValue.isLiteral(v1) || !TValue.isLiteral(v2))
	{
		d1 = TValue.asString(v1);
		d2 = TValue.asString(v2);
	}
	else if (TValue.isString(v1) || TValue.isString(v2))
	{
		d1 = TValue.asString(v1);
		d2 = TValue.asString(v2);
	}
	else if (TValue.isFloatingPoint(v1) || TValue.isFloatingPoint(v2))
	{
		d1 = TValue.asDouble(v1);
		d2 = TValue.asDouble(v2);
	}
	else if (TValue.isInteger(v1) || TValue.isInteger(v2))
	{
		d1 = TValue.asLong(v1);
		d2 = TValue.asLong(v2);
	}
	else if (TValue.isBoolean(v1) || TValue.isBoolean(v2))
	{
		d1 = TValue.asBoolean(v1);
		d2 = TValue.asBoolean(v2);
		// special case
		return d1 === d2 ? 0 : (!d1 ? -1 : 1);
	}
	
	return d1 === d2 ? 0 : (d1 < d2 ? -1 : 1);
	
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
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.add = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be added: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 || v2);
	}
	else if (TValue.isString(value1) || TValue.isString(value2))
	{
		var v1 = TValue.asString(value1);
		var v2 = TValue.asString(value2);
		return TValue.createString(v1 + v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 + v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 + v2);
	}
};

/**
 * Returns the subtraction of the second literal value from the first.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.subtract = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be subtracted: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && !v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 - v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 - v2);
	}
};

/**
 * Returns the multiplication of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.multiply = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be multiplied: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 * v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
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
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be divided: " + value1 + ", " + value2);

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		if (v2 == 0)
		{
			if (v1 != 0)
				return v1 < 0 ? TValue.createNegativeInfinity() : TValue.createInfinity();
			else
				return TValue.createNaN();
		}
		else
			return TValue.createInteger(v1 / v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		if (v2 == 0.0)
		{
			if (!Number.isNaN(v1) && v1 != 0.0)
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
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be modulo divided: " + value1 + ", " + value2);

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		if (v2 == 0)
			return TValue.createNaN();
		else
			return TValue.createInteger(v1 % v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		if (v2 == 0.0)
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
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be raised to a power: " + value1 + ", " + value2);

	var v1 = TValue.asDouble(value1);
	var v2 = TValue.asDouble(value2);
	var p = Math.pow(v1, v2);
	return TValue.createFloat(p);
};

/**
 * Returns the "bitwise and" of two literals.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.and = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be bitwise and'ed: " + value1 + ", " + value2);
	
	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && v2);
	}
	else
	{
		// 
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 & v2);
	}
};

/**
 * Returns the "bitwise or" of two literals.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.or = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be bitwise or'ed: " + value1 + ", " + value2);
	
	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 || v2);
	}
	else
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 | v2);
	}
};

/**
 * Returns the "bitwise xor" of two literals.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.xor = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be bitwise xor'ed: " + value1 + ", " + value2);
	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 ^ v2);
	}
	else
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 ^ v2);
	}
};

/**
 * Returns the left shift of the first value shifted X units by the second.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.leftShift = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be shifted: " + value1 + ", " + value2);
	
	var v1 = TValue.asLong(value1);
	var v2 = TValue.asLong(value2);
	return TValue.createInteger(v1 << v2);	
};

/**
 * Returns the right shift of the first value shifted X units by the second.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.rightShift = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be shifted: " + value1 + ", " + value2);
	
	var v1 = TValue.asLong(value1);
	var v2 = TValue.asLong(value2);
	return TValue.createInteger(v1 >> v2);	
};

/**
 * Returns the right padded shift of the first value shifted X units by the second.
 * Strings and floats are converted to longs, and the longs are converted 
 * to 32-bit integers, in order to keep consistency between Java and JS compilation.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.rightShiftPadded = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be shifted: " + value1 + ", " + value2);
	
	if (TValue.isNaN(value2) || TValue.asLong(value2) == 0)
		return TValue.createInteger(TValue.asLong(value1));
	else
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 >>> v2);	
	}
};

/**
 * Returns the "logical and" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalAnd = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 && v2);
};

/**
 * Returns the "logical or" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalOr = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 || v2);
};

/**
 * Returns the "logical xor" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalXOr = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 ^ v2);
};

/**
 * Returns if two values are equal, no type safety if they are literals.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
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
 * @throws Arithmetic If an arithmetic exception occurs.
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
 * @throws Arithmetic If an arithmetic exception occurs.
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
 * @throws Arithmetic If an arithmetic exception occurs.
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
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.less = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
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
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.lessOrEqual = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
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
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.greater = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
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
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.greaterOrEqual = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
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
	return value.type === TValue.Type.INTEGER
		|| value.type === TValue.Type.FLOAT;
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
 * Returns if this value is a literal value.
 * @return true if so, false if not.
 */
TValue.isLiteral = function(value)
{
	return value.type === TValue.Type.BOOLEAN
		|| value.type === TValue.Type.INTEGER
		|| value.type === TValue.Type.FLOAT
		|| value.type === TValue.Type.STRING;
};

/**
 * Returns if this value represents an element.
 * @return true if so, false if not.
 */
TValue.isElement = function(value)
{
	return value.type === TValue.Type.OBJECT
		|| value.type === TValue.Type.PLAYER
		|| value.type === TValue.Type.ROOM
		|| value.type === TValue.Type.CONTAINER
		|| value.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object container.
 * @return true if so, false if not.
 */
TValue.isObjectContainer = function(value)
{
	return value.type === TValue.Type.PLAYER
		|| value.type === TValue.Type.ROOM
		|| value.type === TValue.Type.CONTAINER
		|| value.type === TValue.Type.WORLD;
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
	var v = TValue.asDouble(value);
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
		return parseInt(TValue.asString(value).toLowerCase(), 10);
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
		var vlower = Util.fromBase64(value.value).toLowerCase();
		if (vlower === "nan")
			return NaN;
		else if (vlower === "infinity")
			return Infinity;
		else if (vlower === "-infinity")
			return -Infinity;
		else
			return parseFloat(value.value);
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
	if (TValue.isString(value) || TValue.isElement(value) || TValue.isVariable(value) || TValue.isAction(value))
		return Util.fromBase64(value.value);
	else if (TValue.isInfinite(value) || TValue.isNaN(value))
		return ""+value.value;
	else if (TValue.isFloatingPoint(value))
	{
		// make it equal to Java/C#
		var d = TValue.asDouble(value);
		if (Math.abs(d) == 0.0)
			return "0.0";
		else if (Math.abs(d) < 0.001 || Math.abs(d) >= 10000000)
		{
			var out = d.toExponential().toUpperCase().replace('+','');
			if (out.indexOf('.') < 0)
			{
				var ie = out.indexOf('E');
				return out.substring(0, ie) + ".0" + out.substring(ie);
			}
			else
				return out;
		}
		else if (d % 1 == 0)		
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
			return TValue.asDouble(value) != 0;
	}
	else if (TValue.isInteger(value))
		return TValue.asLong(value) != 0;
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
	return value.type + "[" + Util.withEscChars(TValue.asString(value)) + "]";
};

//##[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TValue;
// =========================================================================
