/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.TAMEConstants;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.exception.UnexpectedValueTypeException;

/**
 * All values in the interpreter are of this type, which stores a type.
 * @author Matthew Tropiano
 */
public class Value implements Comparable<Value>, Saveable
{
	private static AtomicLong NEXTREFID = new AtomicLong(0); 
	
	/** Reference id - used for tracking values with same memory reference. */
	private long refid;
	/** Value type. */
	protected ValueType type;
	/** Value itself. */
	protected Object value;
	
	/** Generated hashcode - created only when necessary. */
	private int hash;
	
	/**
	 * Creates a blank value.
	 */
	private Value()
	{
		this.refid = NEXTREFID.getAndIncrement();
		this.type = null;
		this.value = null;
		this.hash = 0;
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
		// Get rid of -0.0.
		if (value == -0.0f)
			value = 0.0f;
		out.set(ValueType.FLOAT, (double)value);
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
		// Get rid of -0.0.
		if (value == -0.0)
			value = 0.0;
		out.set(ValueType.FLOAT, value);
		return out;
	}

	/**
	 * Creates a value typed as string.
	 * @param value the string value.
	 * @return the new value.
	 * @throws IllegalArgumentException if value is null.
	 */
	public static Value create(String value)
	{
		Value out = new Value();
		out.set(ValueType.STRING, value);
		return out;
	}

	/**
	 * Creates an empty list value.
	 * @return the new value.
	 */
	public static Value createEmptyList()
	{
		Value out = new Value();
		out.set(ValueType.LIST, new List<Value>());
		return out;
	}

	/**
	 * Creates an empty list value.
	 * @param capacity the initial capacity.
	 * @return the new value.
	 */
	public static Value createEmptyList(int capacity)
	{
		Value out = new Value();
		out.set(ValueType.LIST, new List<Value>(capacity));
		return out;
	}

	/**
	 * Creates a list value.
	 * @param values the values to set in the list (converted to {@link Value}s).
	 * @return the new value.
	 */
	public static Value createList(Value ... values)
	{
		Value out = createEmptyList(values.length);
		for (Value v : values)
			out.listAdd(Value.create(v));
		return out;
	}

	/**
	 * Creates an object value.
	 * @param identity the object identity.
	 * @return the new value.
	 * @throws IllegalArgumentException if identity is null.
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
	 * @throws IllegalArgumentException if identity is null.
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
	 * @throws IllegalArgumentException if identity is null.
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
	 * @throws IllegalArgumentException if identity is null.
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
	 * @throws IllegalArgumentException if identity is null.
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
	 * List-typed values only copy their reference. This does NOT create a new list!
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
			case LIST:
				Value out = new Value();
				out.set(ValueType.LIST, inputValue.value);
				return out;
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
				throw new UnexpectedValueTypeException("Unknown value type.");
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
	 * @throws IllegalArgumentException if value is null.
	 */
	private void set(ValueType type, Object value)
	{
		if (value == null)
			throw new IllegalArgumentException("Value cannot be null!");
		this.type = type;
		this.value = value;
		this.hash = 0;
	}

	/**
	 * Gets this value's reference id (generated on construction).
	 * Only useful during serialization.
	 * @return the reference id.
	 */
	public long getRefId()
	{
		return refid;
	}
	
	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			hash = type.ordinal();
			if (isList())
			{
				int len = length();
				for (int i = 0; i < len; i++)
					hash += 31 * listGet(i).hashCode(); 
			}
			else
				hash += 31 * String.valueOf(value).hashCode();
		}
		return hash;
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
			if (isList() || otherValue.isList())
				return equals(otherValue); 
			else if (isString() && otherValue.isString())
				return value.equals(otherValue.value);
			else
				return asDouble() == otherValue.asDouble();
		}
		else
			return equals(otherValue); 
	}

	/**
	 * Returns if this value is equal to another: PERFECTLY EQUAL, type strict.
	 * Note: NaN is never equal to anything, even itself.
	 * @param otherValue the other value.
	 * @return true if so, false if not.
	 */
	public boolean equals(Value otherValue)
	{
		if (isStrictlyNaN())
			return false;
		else if (otherValue.isStrictlyNaN())
			return false;
		else if (isList())
			return false;
		else if (otherValue.isList())
			return false;
		
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
		if (this.equals(v))
			return 0;

		if (!isLiteral() || !v.isLiteral())
			return Integer.MIN_VALUE;
		
		if (isList() || v.isList())
			return Integer.MIN_VALUE;
		
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
	@SuppressWarnings("unchecked")
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeByte((byte)type.ordinal());
		
		switch (type)
		{
			default:
				throw new IOException("Unimplemented value type serialization.");
			case BOOLEAN:
				sw.writeBoolean((Boolean)value);
				break;
			case INTEGER:
				sw.writeLong((Long)value);
				break;
			case FLOAT:
				sw.writeDouble((Double)value);
				break;
			case LIST:
				List<Value> list = (List<Value>)value;
				sw.writeInt(list.size());
				for (Value v : list)
					v.writeBytes(out);
				break;
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
		type = ValueType.VALUES[sr.readByte()];
		
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
			case LIST:
			{
				int len = sr.readInt();
				value = new List<Value>(len);
				while (len-- > 0)
				{
					listAdd(create(in));
				}
				break;
			}
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
	 * @return the underlying object wrapping the value.
	 */
	public Object getValue() 
	{
		return value;
	}
	
	/**
	 * Returns this value as a boolean value.
	 * @return true if this evaluates true, false if not.
	 */
	public boolean asBoolean()
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
	 * Returns this value as a long value.
	 * @return the long value of this value.
	 */
	public long asLong()
	{
		if (isInfinite() || isNaN())
			return 0L;
		if (isBoolean())
			return asBoolean() ? 1L : 0L;
		if (isInteger())
			return (Long)value;
		if (isFloatingPoint())
			return (long)(double)(Double)value;
		if (isString())
		{
			try {
				return Long.parseLong(asString());
			} catch (NumberFormatException e) {
				return (long)asDouble();
			}
		}
		return 0L;
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
			if (((String)value).equalsIgnoreCase("-Infinity"))
				return Double.NEGATIVE_INFINITY;
			try {
				return Double.parseDouble(asString());
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}
		
		return Double.NaN;
	}

	/**
	 * Returns the String value of this value (not the same as toString()!!).
	 * @return the String value of this value.
	 */
	public String asString()
	{
		if (isList())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			@SuppressWarnings("unchecked")
			List<Value> list = (List<Value>)value;
			Iterator<Value> it = list.iterator();
			while (it.hasNext())
			{
				Value v = it.next();
				sb.append(v.asString());
				if (it.hasNext())
					sb.append(", ");
			}
			sb.append("]");
			return sb.toString();
		}
		else
			return String.valueOf(value);
	}

	/**
	 * Returns if this value evaluates to <code>true</code>.
	 * @return true if so, false if not.
	 */
	public boolean isTrue()
	{
		return asBoolean();
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
	 * Returns if this value is strictly a floating point value that is <code>NaN</code>.
	 * @return true if so, false if not.
	 */
	public boolean isStrictlyNaN()
	{
		return isFloatingPoint() && isNaN();
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
	 * Returns if this value is a string value.
	 * @return true if so, false if not.
	 */
	public boolean isList()
	{
		return type == ValueType.LIST;
	}
	
	/**
	 * Returns if this value is a literal value (or list).
	 * @return true if so, false if not.
	 */
	public boolean isLiteral()
	{
		return 
			type == ValueType.BOOLEAN
			|| type == ValueType.INTEGER
			|| type == ValueType.FLOAT
			|| type == ValueType.STRING
			|| type == ValueType.LIST;
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
	 * Returns if this value represents an object container.
	 * @return true if so, false if not.
	 */
	public boolean isObjectContainer()
	{
		return 
			type == ValueType.PLAYER
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

	/**
	 * Gets if this value is "empty."
	 * <br>If boolean, this returns true if and only if this is false.
	 * <br>If numeric, this returns true if and only if the value is 0 or NaN.
	 * <br>If string, this returns true if and only if this value, trimmed, is length 0.
	 * <br>If list, this returns true if and only if this list is length 0.
	 * <br>Otherwise, false.
	 * @return true if this value is "empty", false if not.
	 */
	public boolean isEmpty()
	{
		if (isStrictlyNaN())
			return true;
		else if (isBoolean())
			return !asBoolean();
		else if (isNumeric())
			return asDouble() == 0.0;
		else if (isString())
			return asString().trim().length() == 0;
		else if (isList())
		{
			@SuppressWarnings("unchecked")
			List<Value> list = (List<Value>)value;
			return list.isEmpty();
		}
		else
			return false;
	}
	
	/**
	 * Gets the length of this value.
	 * <br>If string, this returns the string length in characters.
	 * <br>If list, this returns the cardinality.
	 * <br>Otherwise, 1.
	 * @return the length.
	 */
	public int length()
	{
		if (isList())
		{
			@SuppressWarnings("unchecked")
			List<Value> list = (List<Value>)value;
			return list.size();
		}
		else if (isString())
		{
			return asString().length();
		}
		else
			return 1;
	}
	
	/**
	 * Adds a value to this, if this is a list.
	 * @param v the value to add.
	 * @return true if added, false if not.
	 */
	public boolean listAdd(Value v)
	{
		if (!isList())
			return false;
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		list.add(create(v));
		return true;
	}
	
	/**
	 * Adds a value to this at a specific index, if this is a list.
	 * @param i the index to add the value at.
	 * @param v the value to add.
	 * @return true if added, false if not.
	 */
	public boolean listAddAt(int i, Value v)
	{
		if (!isList())
			return false;
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		list.add(i, create(v));
		return true;
	}
	
	/**
	 * Sets a value on this at a specific index, if this is a list.
	 * @param i the index to set.
	 * @param v the value to set.
	 * @return true if set, false if not (index is out of range).
	 */
	public boolean listSet(int i, Value v)
	{
		if (!isList())
			return false;
		
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		if (i < 0 || i >= list.size())
			return false;
		
		list.replace(i, create(v));
		return true;
	}
	
	/**
	 * Gets a value on this at a specific index, if this is a list.
	 * @param i the index to get.
	 * @return the value (new instance via {@link #create(Value)}) or false if not found.
	 */
	public Value listGet(int i)
	{
		if (!isList())
			return create(false);
		
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		if (i < 0 || i >= list.size())
			return create(false);
		
		return create(list.getByIndex(i));
	}

	/**
	 * Removes a value from inside this value, if this is a list.
	 * Remember, list-typed values are compared by reference!
	 * @param v the value to remove.
	 * @return true if a value was removed or false if not found.
	 */
	public boolean listRemove(Value v)
	{
		if (!isList())
			return false;
		
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		return list.remove(v);
	}

	/**
	 * Removes a value from this at a specific index, if this is a list.
	 * @param i the index to get.
	 * @return the value removed (new instance via {@link #create(Value)}) or false if not found.
	 */
	public Value listRemoveAt(int i)
	{
		if (!isList())
			return create(false);
		
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		if (i < 0 || i >= list.size())
			return create(false);
		
		return create(list.removeIndex(i));
	}

	/**
	 * Gets the index of a value from this, if this is a list.
	 * Remember, list-typed values are compared by reference!
	 * @param v the value to search for.
	 * @return the index of the matching value, or -1 if not found.
	 */
	public int listIndexOf(Value v)
	{
		if (!isList())
			return -1;
		
		@SuppressWarnings("unchecked")
		List<Value> list = (List<Value>)value;
		return list.getIndexOf(v);
	}

	/**
	 * Checks if this value contains a value, if this is a list.
	 * Remember, list-typed values are compared by reference!
	 * @param v the value to search for.
	 * @return true if so, false if not.
	 */
	public boolean listContains(Value v)
	{
		return listIndexOf(v) >= 0;
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
	 */
	public static Value logicalNot(Value value1)
	{
		if (value1.isLiteral())
			return create(!value1.asBoolean());
		else
			return create(Double.NaN);
	}
	
	/**
	 * Returns the addition of two literal values.
	 * NaN is returned if the values cannot be multiplied.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 */
	public static Value add(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

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
	 * NaN is returned if the values cannot be multiplied.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 */
	public static Value subtract(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

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
	 * NaN is returned if the values cannot be multiplied.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 */
	public static Value multiply(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

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
	 * NaN is returned if the values cannot be divided.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value divide(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

		if (value1.isInteger() && value2.isInteger())
		{
			long v1 = value1.asLong();
			long v2 = value2.asLong();
			if (v2 == 0L)
			{
				if (v1 != 0L)
					return create(Double.POSITIVE_INFINITY * v1);
				else
					return create(Double.NaN);
			}
			else
				return create(v1 / v2);
		}
		else
		{
			double v1 = value1.asDouble();
			double v2 = value2.asDouble();
			if (v2 == 0.0)
			{
				if (v1 != 0.0)
					return create(Double.POSITIVE_INFINITY * v1);
				else
					return create(Double.NaN);
			}
			else
				return create(v1 / v2);
		}
	}
	
	/**
	 * Returns the modulo of one literal value using another.
	 * NaN is returned if the values cannot be modularly-divided.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value modulo(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

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
	 * NaN is returned if the values cannot be power.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value.
	 * @throws ArithmeticException an arithmetic exception, if any (or divide by zero).
	 */
	public static Value power(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(Double.NaN);

		double v1 = value1.asDouble();
		double v2 = value2.asDouble();
		double p = Math.pow(v1, v2);
		return create(p);
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
	 * False is returned if the values cannot be compared.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value less(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(false);
		else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
			return create(false);
		else
			return create(value1.compareTo(value2) < 0);
	}

	/**
	 * Returns if the first literal value is less than or equal to the second.
	 * If either are strings, they are compared lexicographically.
	 * False is returned if the values cannot be compared.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value lessOrEqual(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(false);
		else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
			return create(false);
		else
			return create(value1.compareTo(value2) <= 0);
	}

	/**
	 * Returns if the first literal value is greater than the second.
	 * If either are strings, they are compared lexicographically.
	 * False is returned if the values cannot be compared.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value greater(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(false);
		else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
			return create(false);
		else
			return create(value1.compareTo(value2) > 0);
	}

	/**
	 * Returns if the first literal value is greater than or equal to the second.
	 * If either are strings, they are compared lexicographically.
	 * False is returned if the values cannot be compared.
	 * @param value1 the first operand.
	 * @param value2 the second operand.
	 * @return the resultant value, as a boolean.
	 * @throws ArithmeticException If an arithmetic exception occurs.
	 */
	public static Value greaterOrEqual(Value value1, Value value2)
	{
		if (!(value1.isLiteral() || value2.isLiteral()))
			return create(false);
		else if (value1.isList() || value2.isList())
			return create(false);
		else if (value1.isStrictlyNaN() || value2.isStrictlyNaN())
			return create(false);
		else
			return create(value1.compareTo(value2) >= 0);
	}
	
}
