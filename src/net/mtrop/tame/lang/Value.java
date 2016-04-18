/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.exception.ModuleExecutionException;

import com.blackrook.commons.Common;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * All values in the interpreter are of this type, which stores a type.
 * TODO: Verify to ECMAScript standard.
 * @author Matthew Tropiano
 */
public class Value implements Comparable<Value>, Saveable
{
	/** Value type. */
	protected ValueType type;
	/** Value itself. */
	protected Object value;
	
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
	 * @param value the boolean value.
	 * @return the new value.
	 */
	public static Value create(boolean value)
	{
		Value out = new Value();
		out.set(ValueType.BOOLEAN, value);
		return out;
	}

	/**
	 * Creates an integer value.
	 * @param value the integer value.
	 * @return the new value.
	 */
	public static Value create(int value)
	{
		Value out = new Value();
		out.set(ValueType.INTEGER, new Long(value));
		return out;
	}

	/**
	 * Creates a long integer value.
	 * @param value the long value.
	 * @return the new value.
	 */
	public static Value create(long value)
	{
		Value out = new Value();
		out.set(ValueType.INTEGER, value);
		return out;
	}

	/**
	 * Creates a value typed as float.
	 * @param value the float value.
	 * @return the new value.
	 */
	public static Value create(float value)
	{
		Value out = new Value();
		out.set(ValueType.FLOAT, new Double(value));
		return out;
	}

	/**
	 * Creates a value typed as float.
	 * @param value the double value.
	 * @return the new value.
	 */
	public static Value create(double value)
	{
		Value out = new Value();
		out.set(ValueType.FLOAT, value);
		return out;
	}

	/**
	 * Creates a value typed as string.
	 * @param value the string value.
	 * @return the new value.
	 */
	public static Value create(String value)
	{
		Value out = new Value();
		out.set(ValueType.STRING, value);
		return out;
	}

	/**
	 * Creates an object value.
	 * @param identity the object identity.
	 * @return the new value.
	 */
	public static Value createObject(String identity)
	{
		Value out = new Value();
		out.set(ValueType.OBJECT, identity);
		return out;
	}

	/**
	 * Creates a container value.
	 * @param identity the container identity.
	 * @return the new value.
	 */
	public static Value createContainer(String identity)
	{
		Value out = new Value();
		out.set(ValueType.CONTAINER, identity);
		return out;
	}

	/**
	 * Creates a room value.
	 * @param identity the room identity.
	 * @return the new value.
	 */
	public static Value createRoom(String identity)
	{
		Value out = new Value();
		out.set(ValueType.ROOM, identity);
		return out;
	}

	/**
	 * Creates a player value.
	 * @param identity the player identity.
	 * @return the new value.
	 */
	public static Value createPlayer(String identity)
	{
		Value out = new Value();
		out.set(ValueType.PLAYER, identity);
		return out;
	}

	/**
	 * Creates a action value.
	 * @param identity the action identity.
	 * @return the new value.
	 */
	public static Value createAction(String identity)
	{
		Value out = new Value();
		out.set(ValueType.ACTION, identity);
		return out;
	}

	/**
	 * Creates a world value.
	 * @return the new value that represents the world.
	 */
	public static Value createWorld()
	{
		Value out = new Value();
		out.set(ValueType.WORLD, TAMEConstants.IDENTITY_CURRENT_WORLD);
		return out;
	}

	/**
	 * Creates a variable reference value.
	 * @param name the variable name.
	 * @return the new value that represents a variable reference.
	 */
	public static Value createVariable(String name)
	{
		Value out = new Value();
		out.set(ValueType.VARIABLE, name);
		return out;
	}

	/**
	 * Creates a copy of a value.
	 * @param inputValue the input value.
	 * @return the new value that is a copy of the input value.
	 */
	public static Value create(Value inputValue)
	{
		switch (inputValue.type)
		{
			case BOOLEAN:
				return create((Boolean)inputValue.value);
			case INTEGER:
				return create((Long)inputValue.value);
			case FLOAT:
				return create((Double)inputValue.value);
			case STRING:
				return create((String)inputValue.value);
			case OBJECT:
				return createObject((String)inputValue.value);
			case PLAYER:
				return createPlayer((String)inputValue.value);
			case CONTAINER:
				return createContainer((String)inputValue.value);
			case ROOM:
				return createRoom((String)inputValue.value);
			case WORLD:
				return createWorld();
			case ACTION:
				return createAction((String)inputValue.value);
			case VARIABLE:
				return createVariable((String)inputValue.value);
			default:
				throw new ModuleExecutionException("Unknown variable type.");
		}
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
		out.readBytes(in);
		return out;
	}

	/**
	 * Sets the value type and value.
	 * @param type the value type.
	 * @param value the underlying object value.
	 */
	private void set(ValueType type, Object value)
	{
		this.type = type;
		this.value = value;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode() + 31 * value.toString().hashCode();
	}

	/**
	 * Returns if this value is equal to another, value-wise.
	 * If they are literals, they are compared by their string values.
	 * @param otherValue the other value.
	 * @return true if so, false if not.
	 */
	public boolean equalsIgnoreType(Value otherValue)
	{
		if (isLiteral() && otherValue.isLiteral())
		{
			if (isString() && otherValue.isString())
				return value.equals(otherValue.value);
			else
				return asDouble() == otherValue.asDouble();
		}
		else
			return equals(otherValue); 
	}

	/**
	 * Returns if this value is equal to another: PERFECTLY EQUAL, type strict.
	 * @param otherValue the other value.
	 * @return true if so, false if not.
	 */
	public boolean equals(Value otherValue)
	{
		return type == otherValue.type && value.equals(otherValue.value);
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if (obj instanceof Value)
			return equals(((Value)obj));
		else
			return super.equals(obj);
	}

	@Override
	public int compareTo(Value v)
	{
		if (equals(v))
			return 0;
		
		if (!isLiteral() || !v.isLiteral())
			return asString().compareTo(v.asString());
		
		if (isString() || v.isString())
			return asString().compareTo(v.asString());
		if (isFloatingPoint() || v.isFloatingPoint())
		{
			double d1 = asDouble();
			double d2 = v.asDouble();
			return d1 == d2 ? 0 : (d1 < d2 ? -1 : 1);
		}
		if (isInteger() || v.isInteger())
		{
			long d1 = asLong();
			long d2 = v.asLong();
			return d1 == d2 ? 0 : (d1 < d2 ? -1 : 1);
		}
		if (isBoolean() || v.isBoolean())
		{
			boolean d1 = asBoolean();
			boolean d2 = v.asBoolean();
			return d1 == d2 ? 0 : (!d1 ? -1 : 1);
		}
		
		return 0;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeByte((byte)type.ordinal());
		
		switch (type)
		{
			case BOOLEAN:
				sw.writeBoolean((Boolean)value);
				break;
			case INTEGER:
				sw.writeLong((Long)value);
				break;
			case FLOAT:
				sw.writeDouble((Double)value);
				break;
			default:
			case STRING:
			case OBJECT:
			case CONTAINER:
			case PLAYER:
			case ROOM:
			case WORLD:
			case ACTION:
			case VARIABLE:
				sw.writeString(value.toString(), "UTF-8");
				break;
		}
		
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		type = ValueType.values()[sr.readByte()];
		
		switch (type)
		{
			case BOOLEAN:
				value = sr.readBoolean();
				break;
			case INTEGER:
				value = sr.readLong();
				break;
			case FLOAT:
				value = sr.readDouble();
				break;
			case STRING:
			case OBJECT:
			case CONTAINER:
			case PLAYER:
			case ROOM:
			case WORLD:
			case ACTION:
			case VARIABLE:
				value = sr.readString("UTF-8");
				break;
			default:
				throw new ModuleException("Bad value type. Internal error!");
		}

	}

	@Override
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	@Override
	public void fromBytes(byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
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
		return isTrue();
	}
	
	/**
	 * Returns this value as a long value.
	 * @return the long value of this value.
	 */
	public long asLong()
	{
		if (isBoolean())
			return asBoolean() ? 1L : 0L;
		if (isInteger())
			return (Long)value;
		if (isFloatingPoint())
			return (long)(double)(Double)value;
		if (isInfinite())
			return 0L;
		if (isNaN())
			return 0L;
		try {
			return Long.parseLong(asString());
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
	 * @return the double value of this value, or {@link Double#NaN} if not parsable as a number.
	 */
	public double asDouble()
	{
		if (isBoolean())
			return asBoolean() ? 1.0 : 0.0;
		if (isInteger())
			return (double)(Long)value;
		if (isFloatingPoint())
			return (Double)value;
		if (isString())
		{
			if (((String)value).equalsIgnoreCase("NaN"))
				return Double.NaN;
			if (((String)value).equalsIgnoreCase("Infinity"))
				return Double.POSITIVE_INFINITY;
			try {
				return Double.parseDouble(asString());
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}
		
		return Double.NaN;
	}

	/**
	 * Returns the String value of this value.
	 * @return the String value of this value.
	 */
	public String asString()
	{
		return String.valueOf(value);
	}

	/**
	 * Returns if this value evaluates to <code>true</code>.
	 * @return true if so, false if not.
	 */
	public boolean isTrue()
	{
		if (isBoolean())
			return (Boolean)value;
		else if (isNumeric())
		{
			double d = asDouble();
			return Double.isInfinite(d) || (!Double.isNaN(d) && d != 0.0);
		}
		else if (isString())
			return ((String)value).length() != 0;
		else
			return true;
	}
	
	/**
	 * Returns if this value evaluates to <code>NaN</code>.
	 * @return true if so, false if not.
	 */
	public boolean isNaN()
	{
		return Double.isNaN(asDouble());
	}
	
	/**
	 * Returns if this value evaluates to positive or negative infinity.
	 * @return true if so, false if not.
	 */
	public boolean isInfinite()
	{
		return Double.isInfinite(asDouble());
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
	 * Returns if this value is a floating-point number.
	 * @return true if so, false if not.
	 */
	public boolean isFloatingPoint()
	{
		return type == ValueType.FLOAT;
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
	 * Returns if this value is a string value.
	 * @return true if so, false if not.
	 */
	public boolean isString()
	{
		return type == ValueType.STRING;
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
	 * Returns if this value represents an element.
	 * @return true if so, false if not.
	 */
	public boolean isElement()
	{
		return 
			type == ValueType.OBJECT
			|| type == ValueType.PLAYER
			|| type == ValueType.ROOM
			|| type == ValueType.CONTAINER
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
		return type + "[" + Common.withEscChars(String.valueOf(value)) + "]";
	}
	
	/**
	 * Returns the absolute value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value absolute(Value value1)
	{
		if (value1.isInteger())
			return create(Math.abs(value1.asLong()));
		else if (value1.isNumeric())
			return create(Math.abs(value1.asDouble()));
		else
			return create(Double.NaN);
	}
	
	/**
	 * Returns the negative value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value negate(Value value1)
	{
		if (value1.isInteger())
			return create(-value1.asLong());
		else if (value1.isNumeric())
			return create(-value1.asDouble());
		else
			return create(Double.NaN);
	}
	
	/**
	 * Returns the "logical not" value of a literal value.
	 * @param value1 the first operand.
	 * @return the resultant value as a boolean value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value not(Value value1)
	{
		if (value1.isInfinite())
			return create(-1);
		else if (value1.isNaN())
			return create(-1);
		else if (value1.isBoolean())
			return create(!value1.asBoolean());
		else if (value1.isNumeric())
			return create(~value1.asLong());
		else if (value1.isString())
			return create(-1);
		else
			return create(Double.NaN);
	}
	
	/**
	 * Returns the addition of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
		else if (value1.isString() || value2.isString())
		{
			String v1 = value1.asString();
			String v2 = value2.asString();
			return create(v1 + v2);
		}
		else if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 + v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 + v2);
		}
	}
	
	/**
	 * Returns the subtraction of the second literal value from the first.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value subtract(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be subtracted: " + value1 + ", " + value2);

		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && !v2);
		}
		else if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 - v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 - v2);
		}
	}
	
	/**
	 * Returns the multiplication of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value multiply(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be multiplied: " + value1 + ", " + value2);

		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && v2);
		}
		else if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 * v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			return create(v1 * v2);
		}
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

		if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			if (v2 == 0L)
				return create(Double.POSITIVE_INFINITY * v1);
			else
				return create(v1 / v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			if (v2 == 0.0)
				return create(Double.POSITIVE_INFINITY * v1);
			else
				return create(v1 / v2);
		}
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

		if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			if (v2 == 0L)
				return create(Double.NaN);
			else
				return create(v1 % v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			if (v2 == 0.0)
				return create(Double.NaN);
			else
				return create(v1 % v2);
		}
	}
	
	/**
	 * Returns the result of one value raised to a certain power. 
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value power(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be modulo divided: " + value1 + ", " + value2);

		double v1 = value1.asDouble();
		double v2 = value2.asDouble();
		double p = Math.pow(v1, v2);
		if (value1.isInteger() && value2.isInteger())
			return create((long)p);
		else
			return create(p);
	}
	
	/**
	 * Returns the "bitwise and" of two literals.
	 * Strings and doubles are converted to longs.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value and(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise and'ed: " + value1 + ", " + value2);
		
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 && v2);
		}
		else
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 & v2);
		}
	}

	/**
	 * Returns the "bitwise or" of two literals.
	 * Strings and doubles are converted to longs.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value or(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise or'ed: " + value1 + ", " + value2);
		
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 || v2);
		}
		else
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 | v2);
		}
	}

	/**
	 * Returns the "bitwise xor" of two literals.
	 * Strings and doubles are converted to longs.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value xor(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be bitwise xor'ed: " + value1 + ", " + value2);
		if (value1.isBoolean() && value2.isBoolean())
		{
			boolean v1 = value1.asBoolean();
			boolean v2 = value2.asBoolean();
			return create(v1 ^ v2);
		}
		else
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			return create(v1 ^ v2);
		}
	}

	/**
	 * Returns the left shift of the first value shifted X units by the second.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value leftShift(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.asLong();
		long v2 = value2.asLong();
		return create(v1 << v2);	
	}

	/**
	 * Returns the right shift of the first value shifted X units by the second.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value rightShift(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.asLong();
		long v2 = value2.asLong();
		return create(v1 >> v2);	
	}

	/**
	 * Returns the right padded shift of the first value shifted X units by the second.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value rightShiftPadded(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			throw new ArithmeticException("These values can't be shifted: " + value1 + ", " + value2);
		
		long v1 = value1.asLong();
		long v2 = value2.asLong();
		return create(v1 >>> v2);	
	}

	/**
	 * Returns the "logical and" of two literal values.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
	 * @throws ArithmeticException If an arithmetic exception occurs.
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
