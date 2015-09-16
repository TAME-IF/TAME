/*******************************************************************************
 * Copyright (c) 2009-2013 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package net.mtrop.tame.struct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * All values in the interpreter are of this type, which stores a type.
 * @author Matthew Tropiano
 */
public class Value
{
	/** Value type. */
	protected ValueType type;
	/** Value itself. */
	protected String value;
	
	/**
	 * Creates a blank value.
	 */
	private Value()
	{
		type = null;
		value = null;
	}
	
	/**
	 * Creates a boolean value, typed as integer.
	 */
	public static Value create(boolean value)
	{
		Value out = new Value();
		out.set(ValueType.BOOLEAN, String.valueOf(value));
		return out;
	}

	/**
	 * Creates an integer value.
	 */
	public static Value create(int value)
	{
		Value out = new Value();
		out.set(ValueType.INTEGER, String.valueOf(value));
		return out;
	}

	/**
	 * Creates an integer value.
	 */
	public static Value create(long value)
	{
		Value out = new Value();
		out.set(ValueType.INTEGER, String.valueOf(value));
		return out;
	}

	/**
	 * Creates a value typed as float.
	 */
	public static Value create(float value)
	{
		Value out = new Value();
		out.set(ValueType.FLOAT, String.valueOf(value));
		return out;
	}

	/**
	 * Creates a value typed as float.
	 */
	public static Value create(double value)
	{
		Value out = new Value();
		out.set(ValueType.FLOAT, String.valueOf(value));
		return out;
	}

	/**
	 * Creates a value typed as string.
	 */
	public static Value create(String value)
	{
		Value out = new Value();
		out.set(ValueType.STRING, value);
		return out;
	}

	/**
	 * Creates a value with a type and string representation of the value.
	 */
	public static Value create(ValueType type, String value)
	{
		Value out = new Value();
		out.set(type, value);
		return out;
	}

	/**
	 * Reads a value from an input stream.
	 * @param in the stream to read from.
	 * @return a new value.
	 * @throws IOException if a value could not be read.
	 */
	public static Value create(InputStream in) throws IOException
	{
		Value out = new Value();
		SuperReader sr = new SuperReader(in,SuperReader.LITTLE_ENDIAN);
		out.set(ValueType.values()[sr.readByte()], sr.readString());
		return out;
	}

	/**
	 * Sets the value type and value.
	 * @param type
	 * @param value
	 */
	private void set(ValueType type, String value)
	{
		this.type = type;
		this.value = value;
	}

	/**
	 * Copies this value.
	 */
	public Value copy()
	{
		return create(type, new String(value));
	}

	@Override
	public int hashCode()
	{
		return value.toString().hashCode();
	}

	/**
	 * Returns if two values are equal, value-wise.
	 * If they are literals, they are compared by their string values.
	 * @return true if so, false if not.
	 */
	public boolean equalsIgnoreType(Value v)
	{
		if (isLiteral() && v.isLiteral())
			return value.equals(v.value);
		else
			return equals(v); 
	}

	/**
	 * Returns if two values are PERFECTLY EQUAL, type strict.
	 * @return true if so, false if not.
	 */
	public boolean equals(Value v)
	{
		return type == v.type && value.equals(value);
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if (obj instanceof Value)
			return equals(((Value)obj));
		else
			return super.equals(obj);
	}

	/**
	 * Reads a value from an input stream.
	 * @param in the stream to read from.
	 * @return a new value.
	 * @throws IOException if a value could not be read.
	 */
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		set(ValueType.values()[sr.readByte()], sr.readString());
	}
	
	/**
	 * Writes a value to an output stream.
	 * @param out the stream to write to.
	 * @throws IOException if a value could not be written.
	 */
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeByte((byte)type.ordinal());
		sw.writeString(value.toString());
	}
	
	/**
	 * Gets this value as a byte string.
	 * @return the byte representation of this
	 * @throws IOException
	 */
	public byte[] getBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	/**
	 * Returns this value's type.
	 * @return this value's type.
	 */
	public ValueType getType()
	{
		return type;
	}

	/**
	 * Returns this value as a boolean value.
	 * @return true if this evaluates true, false if not.
	 */
	public boolean asBoolean()
	{
		if (isBoolean())
			return "true".equals(value);
		else if (isNumeric())
		{
			double d = asDouble();
			return !Double.isNaN(d) && d != 0.0;
		}
		else if (isLiteral())
			return asString().length() != 0;
		else
			return true;
	}
	
	/**
	 * Returns this value as a long value.
	 * @return the long value of this value.
	 */
	public long asLong()
	{
		if (isBoolean())
			return asBoolean() ? 1L : 0L;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return (long)asDouble();
		}
	}

	/**
	 * Returns the bitwise long value (if this is a double).
	 * @return the long value of this value.
	 */
	public long asLongBits()
	{
		if (!isFloatingPoint())
			return asLong();
		else
			return Double.doubleToLongBits(asDouble());
	}

	/**
	 * Returns the double value of this value.
	 * @return the double value of this value.
	 */
	public double asDouble()
	{
		if (isBoolean())
			return asBoolean() ? 1.0 : 0.0;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/**
	 * Returns the String value of this value.
	 * @return the String value of this value.
	 */
	public String asString()
	{
		return value;
	}

	/**
	 * Returns if this value is a boolean value.
	 * @return true if so, false if not.
	 */
	public boolean isBoolean()
	{
		return type == ValueType.BOOLEAN;
	}
	
	/**
	 * Returns if this value is an integer.
	 * @return true if so, false if not.
	 */
	public boolean isInteger()
	{
		return type == ValueType.INTEGER;
	}
	
	/**
	 * Returns if this value is a number.
	 * @return true if so, false if not.
	 */
	public boolean isNumeric()
	{
		return 
			type == ValueType.INTEGER
			|| type == ValueType.FLOAT;
	}
	
	/**
	 * Returns if this value is a floating-point number.
	 * @return true if so, false if not.
	 */
	public boolean isFloatingPoint()
	{
		return type == ValueType.FLOAT;
	}
	
	/**
	 * Returns if this value is a literal value.
	 * @return true if so, false if not.
	 */
	public boolean isLiteral()
	{
		return 
			type == ValueType.BOOLEAN
			|| type == ValueType.INTEGER
			|| type == ValueType.FLOAT
			|| type == ValueType.STRING;
	}
	
	/**
	 * Returns if this value is a string value.
	 * @return true if so, false if not.
	 */
	public boolean isString()
	{
		return type == ValueType.STRING;
	}
	
	/**
	 * Returns if this value represents an element.
	 * @return true if so, false if not.
	 */
	public boolean isElement()
	{
		return 
			type == ValueType.OBJECT
			|| type == ValueType.PLAYER
			|| type == ValueType.ROOM
			|| type == ValueType.WORLD;
	}
	
	/**
	 * Returns if this value represents an action.
	 * @return true if so, false if not.
	 */
	public boolean isAction()
	{
		return type == ValueType.ACTION;
	}
	
	/**
	 * Returns if this value represents a variable.
	 * @return true if so, false if not.
	 */
	public boolean isVariable()
	{
		return type == ValueType.VARIABLE;
	}
	
	@Override
	public String toString()
	{
		return type + "[" + value + "]";
	}
	
	/**
	 * Returns the absolute value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value absolute(Value value1)
	{
		if (value1.isInteger())
			return create(Math.abs(value1.asLong()));
		else if (value1.isNumeric())
			return create(Math.abs(value1.asDouble()));
		else
			throw new ArithmeticException("Value is not numeric.");
	}
	
	/**
	 * Returns the negative value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value negate(Value value1)
	{
		if (value1.isInteger())
			return create(-value1.asLong());
		else if (value1.isNumeric())
			return create(-value1.asDouble());
		else
			throw new ArithmeticException("Value is not numeric.");
	}
	
	/**
	 * Returns the "logical not" value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value as a boolean value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value logicalNot(Value value1)
	{
		if (value1.isLiteral())
			return create(!value1.asBoolean());
		else
			throw new ArithmeticException("Value is not a literal that evaluates to a boolean.");
	}
	
	/**
	 * Returns the bitwise compliment value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value not(Value value1)
	{
		if (value1.isBoolean())
			return create(!value1.asBoolean());
		else if (value1.isInteger())
			return create(~value1.asLong());
		else if (value1.isNumeric())
			return create(Double.longBitsToDouble(~value1.asLongBits()));
		else
			throw new ArithmeticException("Value is not numeric or a boolean.");
	}
	
	/**
	 * Returns the addition of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value add(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be added: " + value1 + ", " + value2);

		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 || v2);
		}
		else if (value1.isBoolean() ^ value2.isBoolean())
		{
			throw new ArithmeticException("Can't add a boolean to another type.");
		}
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value2.asString();
			return create(v1 + v2);
		}
		else if (value1.isFloatingPoint() || value2.isFloatingPoint())
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 + v2);
		}
		else if (value1.isInteger() || value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 + v2);
		}
		else
			throw new ArithmeticException("These values can't be added: " + value1 + ", " + value2);
	}
	
	/**
	 * Returns the subtraction of the second literal value from the first.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value subtract(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be subtracted: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be subtracted: " + value1 + ", " + value2);

		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && !v2);
		}
		else if (value1.isBoolean() ^ value2.isBoolean())
		{
			throw new ArithmeticException("Can't subtract a boolean from another type.");
		}
		else if (value1.isFloatingPoint() || value2.isFloatingPoint())
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 - v2);
		}
		else if (value1.isInteger() || value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 - v2);
		}
		else
			throw new ArithmeticException("These values can't be subtracted: " + value1 + ", " + value2);
	}
	
	/**
	 * Returns the multiplication of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value multiply(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be multiplied: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be multiplied: " + value1 + ", " + value2);

		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && v2);
		}
		else if (value1.isBoolean() ^ value2.isBoolean())
		{
			throw new ArithmeticException("Can't multiply a boolean with another type.");
		}
		else if (value1.isFloatingPoint() || value2.isFloatingPoint())
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 * v2);
		}
		else if (value1.isInteger() || value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 * v2);
		}
		else
			throw new ArithmeticException("These values can't be multiplied: " + value1 + ", " + value2);
	}
	
	/**
	 * Returns the division of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value divide(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be divided: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be divided: " + value1 + ", " + value2);

		if (value1.isBoolean() || value2.isBoolean())
		{
			throw new ArithmeticException("Can't divide a boolean with another type.");
		}
		else if (value1.isFloatingPoint() || value2.isFloatingPoint())
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			if (v2 == 0.0)
				throw new ArithmeticException("Can't divide by 0.");
			else
				return create(v1 / v2);
		}
		else if (value1.isInteger() || value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			if (v2 == 0L)
				throw new ArithmeticException("Can't divide by 0.");
			else
				return create(v1 / v2);
		}
		else
			throw new ArithmeticException("These values can't be divided: " + value1 + ", " + value2);
	}
	
	/**
	 * Returns the modulo of one literal value using another.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value modulo(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be modulo divided: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be modulo divided: " + value1 + ", " + value2);

		if (value1.isBoolean() || value2.isBoolean())
		{
			throw new ArithmeticException("Can't modulo divide a boolean with another type.");
		}
		else if (value1.isFloatingPoint() || value2.isFloatingPoint())
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			if (v2 == 0.0)
				throw new ArithmeticException("Can't divide by 0.");
			else
				return create(v1 % v2);
		}
		else if (value1.isInteger() || value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			if (v2 == 0L)
				throw new ArithmeticException("Can't divide by 0.");
			else
				return create(v1 % v2);
		}
		else
			throw new ArithmeticException("These values can't be modulo divided: " + value1 + ", " + value2);
	}
	
	/**
	 * Returns the "bitwise and" of two non-string literals.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value and(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise and'ed: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be bitwise and'ed: " + value1 + ", " + value2);
		
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && v2);
		}
		else
		{
			long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
			long v2 = value2.isFloatingPoint() ? value2.asLongBits() : value2.asLong();
			long out = v1 & v2;
			if (value1.isFloatingPoint() || value2.isFloatingPoint())
				return create(Double.longBitsToDouble(out));
			else
				return create(out);
		}
	}

	/**
	 * Returns the "bitwise or" of two non-string literals.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value or(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise or'ed: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be bitwise or'ed: " + value1 + ", " + value2);
		
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 || v2);
		}
		else
		{
			long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
			long v2 = value2.isFloatingPoint() ? value2.asLongBits() : value2.asLong();
			long out = v1 | v2;
			if (value1.isFloatingPoint() || value2.isFloatingPoint())
				return create(Double.longBitsToDouble(out));
			else
				return create(out);
		}
	}

	/**
	 * Returns the "bitwise xor" of two non-string literals.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value xor(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise xor'ed: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
			throw new ArithmeticException("These values can't be bitwise xor'ed: " + value1 + ", " + value2);
		
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 ^ v2);
		}
		else
		{
			long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
			long v2 = value2.isFloatingPoint() ? value2.asLongBits() : value2.asLong();
			long out = v1 ^ v2;
			if (value1.isFloatingPoint() || value2.isFloatingPoint())
				return create(Double.longBitsToDouble(out));
			else
				return create(out);
		}
	}

	/**
	 * Returns the left shift of the first value shifted X units by the second.
	 * The first value must be numeric.
	 * The second value must be an integer.
	 * @param value1 the first operand.
	 * @param value2 the second operand (must be integer).
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value leftShift(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		else if (!(value1.isNumeric() && value2.isInteger()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
		long v2 = value2.asLong();
		
		if (value1.isFloatingPoint())
			return create(Double.longBitsToDouble(v1 << v2));
		else
			return create(v1 << v2);	
	}

	/**
	 * Returns the right shift of the first value shifted X units by the second.
	 * The first value must be numeric.
	 * The second value must be an integer.
	 * @param value1 the first operand.
	 * @param value2 the second operand (must be integer).
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value rightShift(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		else if (!(value1.isNumeric() && value2.isInteger()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
		long v2 = value2.asLong();
		
		if (value1.isFloatingPoint())
			return create(Double.longBitsToDouble(v1 >> v2));
		else
			return create(v1 >> v2);	
	}

	/**
	 * Returns the right padded shift of the first value shifted X units by the second.
	 * The first value must be numeric.
	 * The second value must be an integer.
	 * @param value1 the first operand.
	 * @param value2 the second operand (must be integer).
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value rightShiftPadded(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		else if (!(value1.isNumeric() && value2.isInteger()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.isFloatingPoint() ? value1.asLongBits() : value1.asLong();
		long v2 = value2.asLong();
		
		if (value1.isFloatingPoint())
			return create(Double.longBitsToDouble(v1 >>> v2));
		else
			return create(v1 >>> v2);	
	}

	/**
	 * Returns the "logical and" of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value logicalAnd(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be and'ed: " + value1 + ", " + value2);
		
		boolean v1 = value1.asBoolean();
		boolean v2 = value2.asBoolean();
		return create(v1 && v2);
	}

	/**
	 * Returns the "logical or" of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value logicalOr(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be and'ed: " + value1 + ", " + value2);
		
		boolean v1 = value1.asBoolean();
		boolean v2 = value2.asBoolean();
		return create(v1 || v2);
	}

	/**
	 * Returns the "logical xor" of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value logicalXOr(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be and'ed: " + value1 + ", " + value2);
		
		boolean v1 = value1.asBoolean();
		boolean v2 = value2.asBoolean();
		return create(v1 ^ v2);
	}

	/**
	 * Returns if two values are equal, no type safety if they are literals.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value equals(Value value1, Value value2)
	{
		return create(value1.equalsIgnoreType(value2));
	}

	/**
	 * Returns if two values are not equal, no type safety if they are literals.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value notEquals(Value value1, Value value2)
	{
		return create(!value1.equalsIgnoreType(value2));
	}

	/**
	 * Returns if two values are equal, with type strictness.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value strictEquals(Value value1, Value value2)
	{
		return create(value1.equals(value2));
	}

	/**
	 * Returns if two values are not equal, with type strictness.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value strictNotEquals(Value value1, Value value2)
	{
		return create(!value1.equals(value2));
	}

	/**
	 * Returns if the first literal value is less than the second.
	 * If either are strings, they are compared lexicographically.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value less(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be compared: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value1.asString();
			return create(v1.compareTo(v2) < 0);
		}
		else
			return create(value1.asDouble() < value2.asDouble());
	}

	/**
	 * Returns if the first literal value is less than or equal to the second.
	 * If either are strings, they are compared lexicographically.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value lessOrEqual(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be compared: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value1.asString();
			return create(v1.compareTo(v2) <= 0);
		}
		else
			return create(value1.asDouble() <= value2.asDouble());
	}

	/**
	 * Returns if the first literal value is greater than the second.
	 * If either are strings, they are compared lexicographically.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value greater(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be compared: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value1.asString();
			return create(v1.compareTo(v2) > 0);
		}
		else
			return create(value1.asDouble() > value2.asDouble());
	}

	/**
	 * Returns if the first literal value is greater than or equal to the second.
	 * If either are strings, they are compared lexicographically.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException an arithmetic exception, if any.
	 */
	public static Value greaterOrEqual(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be compared: " + value1 + ", " + value2);
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value1.asString();
			return create(v1.compareTo(v2) >= 0);
		}
		else
			return create(value1.asDouble() >= value2.asDouble());
	}
	
}
