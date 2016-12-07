/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('./TAMEError.js') : null);
// ======================================================================================================

//##[[CONTENT-BEGIN

/*****************************************************************************
 See net.mtrop.tame.lang.Value
 *****************************************************************************/
var TValue = function(type, value)
{
	if (!type)
		throw new TAMEError(TAMEError.Type.UnexpectedValueType, "Invalid value type in TValue()");
	if (typeof value === 'undefined' || value == null)
		throw new TAMEError(TAMEError.Type.UnexpectedValueType, "Value cannot be undefined or null in TValue()");
	this.type = type;
	this.value = value;
};

/* Type Constants */
TValue.Type = {
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

// Convenience constructors.
TValue.createBoolean = function(value) {return new TValue(TValue.Type.BOOLEAN, Boolean(value));}
TValue.createInteger = function(value) {return new TValue(TValue.Type.INTEGER, parseInt(value));}
TValue.createFloat = function(value) {return new TValue(TValue.Type.FLOAT, parseFloat(value));}
TValue.createString = function(value) {return new TValue(TValue.Type.STRING, String(value));}
TValue.createWorld = function() {return new TValue(TValue.Type.WORLD, "world");}
TValue.createObject = function(value) {return new TValue(TValue.Type.OBJECT, String(value));}
TValue.createContainer = function(value) {return new TValue(TValue.Type.CONTAINER, String(value));}
TValue.createPlayer = function(value) {return new TValue(TValue.Type.PLAYER, String(value));}
TValue.createRoom = function(value) {return new TValue(TValue.Type.ROOM, String(value));}
TValue.createAction = function(value) {return new TValue(TValue.Type.ACTION, String(value));}
TValue.createVariable = function(value) {return new TValue(TValue.Type.VARIABLE, String(value));}
TValue.createNaN = function() {return new TValue(TValue.Type.FLOAT, NaN);}
TValue.createInfinity = function() {return new TValue(TValue.Type.FLOAT, Infinity);}
TValue.createNegativeInfinity = function() {return new TValue(TValue.Type.FLOAT, -Infinity);}

/**
 * Returns if this value is equal to another, value-wise.
 * If they are literals, they are compared by their string values.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return true if so, false if not.
 */
TValue.areEqualIgnoreType = function(v1, v2)
{
	if (v1.isLiteral() && v2.isLiteral())
	{
		if (v1.isString() && v2.isString())
			return v1.value == v2.value;
		else
			return v1.asDouble() == v2.asDouble();
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
	if (!v1.isLiteral() || !v2.isLiteral())
	{
		d1 = v1.asString();
		d2 = v2.asString();
	}
	else if (v1.isString() || v2.isString())
	{
		d1 = v1.asString();
		d2 = v2.asString();
	}
	else if (v1.isFloatingPoint() || v2.isFloatingPoint())
	{
		d1 = v1.asDouble();
		d2 = v2.asDouble();
	}
	else if (v1.isInteger() || v2.isInteger())
	{
		d1 = v1.asLong();
		d2 = v2.asLong();
	}
	else if (v1.isBoolean() || v2.isBoolean())
	{
		d1 = v1.asBoolean();
		d2 = v2.asBoolean();
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
	if (value1.isInteger())
		return TValue.createInteger(Math.abs(value1.asLong()));
	else if (value1.isNumeric())
		return TValue.createFloat(Math.abs(value1.asDouble()));
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
	if (value1.isInteger())
		return TValue.createInteger(-value1.asLong());
	else if (value1.isNumeric())
		return TValue.createFloat(-value1.asDouble());
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
	if (value1.isLiteral())
		return TValue.createBoolean(!value1.asBoolean());
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
	if (value1.isInfinite())
		return TValue.createInteger(-1);
	else if (value1.isNaN())
		return TValue.createInteger(-1);
	else if (value1.isBoolean())
		return TValue.createBoolean(!value1.asBoolean());
	else if (value1.isNumeric())
		return TValue.createInteger(~value1.asLong());
	else if (value1.isString())
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic,"These values can't be added: " + value1 + ", " + value2);

	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 || v2);
	}
	else if (value1.isString() || value2.isString())
	{
		var v1 = value1.asString();
		var v2 = value2.asString();
		return TValue.createString(v1 + v2);
	}
	else if (value1.isInteger() && value2.isInteger())
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
		return TValue.createInteger(v1 + v2);
	}
	else
	{
		var v1 = value1.asDouble();
		var v2 = value2.asDouble();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic,"These values can't be subtracted: " + value1 + ", " + value2);

	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 && !v2);
	}
	else if (value1.isInteger() && value2.isInteger())
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
		return TValue.createInteger(v1 - v2);
	}
	else
	{
		var v1 = value1.asDouble();
		var v2 = value2.asDouble();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic,"These values can't be multiplied: " + value1 + ", " + value2);

	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 && v2);
	}
	else if (value1.isInteger() && value2.isInteger())
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
		return TValue.createInteger(v1 * v2);
	}
	else
	{
		var v1 = value1.asDouble();
		var v2 = value2.asDouble();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be divided: " + value1 + ", " + value2);

	if (value1.isInteger() && value2.isInteger())
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
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
		var v1 = value1.asDouble();
		var v2 = value2.asDouble();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be modulo divided: " + value1 + ", " + value2);

	if (value1.isInteger() && value2.isInteger())
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
		if (v2 == 0)
			return TValue.createNaN();
		else
			return TValue.createInteger(v1 % v2);
	}
	else
	{
		var v1 = value1.asDouble();
		var v2 = value2.asDouble();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be modulo divided: " + value1 + ", " + value2);

	var v1 = value1.asDouble();
	var v2 = value2.asDouble();
	var p = Math.pow(v1, v2);
	if (v1 == 0 && v2 < 0)
		return TValue.createInfinity();
	else if (value1.isInteger() && value2.isInteger())
		return TValue.createInteger(p);
	else
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be bitwise and'ed: " + value1 + ", " + value2);
	
	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 && v2);
	}
	else
	{
		// 
		var v1 = value1.asLong();
		var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be bitwise or'ed: " + value1 + ", " + value2);
	
	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 || v2);
	}
	else
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be bitwise xor'ed: " + value1 + ", " + value2);
	if (value1.isBoolean() && value2.isBoolean())
	{
		var v1 = value1.asBoolean();
		var v2 = value2.asBoolean();
		return TValue.createBoolean(v1 ^ v2);
	}
	else
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be shifted: " + value1 + ", " + value2);
	
	var v1 = value1.asLong();
	var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be shifted: " + value1 + ", " + value2);
	
	var v1 = value1.asLong();
	var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be shifted: " + value1 + ", " + value2);
	
	if (value2.isNaN() || value2.asLong() == 0)
		return TValue.createInteger(value1.asLong());
	else
	{
		var v1 = value1.asLong();
		var v2 = value2.asLong();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = value1.asBoolean();
	var v2 = value2.asBoolean();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = value1.asBoolean();
	var v2 = value2.asBoolean();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = value1.asBoolean();
	var v2 = value2.asBoolean();
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be compared: " + value1 + ", " + value2);
	else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(value1.compareTo(value2) < 0);
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be compared: " + value1 + ", " + value2);
	else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(value1.compareTo(value2) <= 0);
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be compared: " + value1 + ", " + value2);
	else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(value1.compareTo(value2) > 0);
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
	if (!(value1.isLiteral() || value2.isLiteral()))
		throw new TAMEError(TAMEError.Type.Arithmetic, "These values can't be compared: " + value1 + ", " + value2);
	else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(value1.compareTo(value2) >= 0);
};

/**
 * Returns if this value is a boolean value.
 * @return true if so, false if not.
 */
TValue.prototype.isBoolean = function()
{
	return this.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value is an integer.
 * @return true if so, false if not.
 */
TValue.prototype.isInteger = function()
{
	return this.type === TValue.Type.INTEGER;
};

/**
 * Returns if this value is a floating-point number.
 * @return true if so, false if not.
 */
TValue.prototype.isFloatingPoint = function()
{
	return this.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a number.
 * @return true if so, false if not.
 */
TValue.prototype.isNumeric = function()
{
	return this.type === TValue.Type.INTEGER
		|| this.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a string value.
 * @return true if so, false if not.
 */
TValue.prototype.isString = function()
{
	return this.type === TValue.Type.STRING;
};

/**
 * Returns if this value is a literal value.
 * @return true if so, false if not.
 */
TValue.prototype.isLiteral = function()
{
	return this.type === TValue.Type.BOOLEAN
		|| this.type === TValue.Type.INTEGER
		|| this.type === TValue.Type.FLOAT
		|| this.type === TValue.Type.STRING;
};

/**
 * Returns if this value represents an element.
 * @return true if so, false if not.
 */
TValue.prototype.isElement = function()
{
	return this.type === TValue.Type.OBJECT
		|| this.type === TValue.Type.PLAYER
		|| this.type === TValue.Type.ROOM
		|| this.type === TValue.Type.CONTAINER
		|| this.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object container.
 * @return true if so, false if not.
 */
TValue.prototype.isObjectContainer = function()
{;
	return this.type === TValue.Type.PLAYER
		|| this.type === TValue.Type.ROOM
		|| this.type === TValue.Type.CONTAINER
		|| this.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an action.
 * @return true if so, false if not.
 */
TValue.prototype.isAction = function()
{
	return this.type === TValue.Type.ACTION;
};

/**
 * Returns if this value represents a variable.
 * @return true if so, false if not.
 */
TValue.prototype.isVariable = function()
{
	return this.type === TValue.Type.VARIABLE;
};

/**
 * Returns if this value represents a boolean.
 * @return true if so, false if not.
 */
TValue.prototype.isBoolean = function()
{
	return this.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value evaluates to <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.prototype.isNaN = function()
{
	return Number.isNaN(this.asDouble());
};

/**
 * Returns if this value is floating point and literally <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.prototype.isStrictlyNaN = function()
{
	return this.isFloatingPoint() && this.isNaN();
};

/**
 * Returns if this value evaluates to positive or negative infinity.
 * @return true if so, false if not.
 */
TValue.prototype.isInfinite = function()
{
	var v = this.asDouble();
	return v === Infinity || v === -Infinity;
};

/**
 * Returns this value as a long value.
 * @return the long value of this value.
 */
TValue.prototype.asLong = function()
{
	if (this.isInfinite() || this.isNaN())
		return 0;
	else if (this.isBoolean())
		return this.asBoolean() ? 1 : 0;
	else if (this.isInteger())
		return this.value;
	else if (this.isFloatingPoint())
		return parseInt(this.value);
	else if (this.isString())
		return parseInt(this.asString());
	else
		return 0;
};

/**
 * Returns the double value of this value.
 * @return the double value of this value, or {@link Double#NaN} if not parsable as a number.
 */
TValue.prototype.asDouble = function()
{
	if (this.isBoolean())
		return this.asBoolean() ? 1.0 : 0.0;
	else if (this.isInteger())
		return parseFloat(this.value);
	else if (this.isFloatingPoint())
		return this.value;
	else if (this.isString())
	{
		var vlower = this.value.toLowerCase();
		if (vlower === "nan")
			return NaN;
		else if (vlower === "infinity")
			return Infinity;
		else if (vlower === "-infinity")
			return -Infinity;
		else
			return parseFloat(this.asString());
	}
	else
		return NaN;
};

/**
 * Returns the String value of this value (not the same as toString()!!).
 * @return the String value of this value.
 */
TValue.prototype.asString = function()
{
	if (this.isFloatingPoint() && this.asDouble() % 1 == 0)
		return this.value+".0";
	else
		return ""+this.value;
};

/**
 * Returns this value as a boolean value.
 * @return true if this evaluates true, false if not.
 */
TValue.prototype.asBoolean = function()
{
	if (this.isBoolean())
		return this.value;
	else if (this.isFloatingPoint())
	{
		if (this.isInfinite())
			return true;
		else if (Number.isNaN(this.value))
			return false;
		else
			return this.asDouble() != 0;
	}
	else if (this.isInteger())
		return this.asLong() != 0;
	else if (this.isString())
		return this.value.length !== 0;
	else
		return true; // all objects are true
};

/**
 * Returns if this value evaluates to "true".
 * @return true if so, false if not.
 */
TValue.prototype.isTrue = function()
{
	return this.asBoolean();
};
    
/**
 * @return a string representation of this value (for debug, usually).
 */
TValue.prototype.toString = function()
{
	return this.type + "[" + this.asString() + "]";
};

/**
 * Returns if this value is equal to another, value-wise.
 * If they are literals, they are compared by their string values.
 * @param otherValue the other value.
 * @return true if so, false if not.
 */
TValue.prototype.equalsIgnoreType = function(otherValue)
{
	return TValue.areEqualIgnoreType(this, otherValue);
};

/**
 * Returns if this value is equal to another: PERFECTLY EQUAL, type strict.
 * @param otherValue the other value.
 * @return true if so, false if not.
 */
TValue.prototype.equals = function(otherValue)
{
	return TValue.areEqual(this, otherValue);
};

/**
 * Compares this value with another.
 * @param v the other value.
 */
TValue.prototype.compareTo = function(v)
{
	return TValue.compare(this, v);
};

//##[[CONTENT-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TValue;
// =========================================================================
