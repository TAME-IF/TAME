/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.tameif.tame.struct.CaseInsensitiveStringMap;
import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;
import com.tameif.tame.struct.Sizable;

/**
 * Convenience class for Value lookup tables.
 * All variable names are case-insensitive!
 * @author Matthew Tropiano
 */
public class ValueSet implements ReferenceSaveable, Iterable<Map.Entry<String, Value>>, Sizable
{
	private CaseInsensitiveStringMap<Value> valueMap; 
	
	/**
	 * Creates a new ValueHash.
	 */
	public ValueSet()
	{
		this.valueMap = new CaseInsensitiveStringMap<>(4);
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param referenceMap the reference map to use for "seen" value references.
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static ValueSet create(Map<Long, Value> referenceMap, InputStream in) throws IOException
	{
		ValueSet out = new ValueSet();
		out.readReferentialBytes(referenceMap, in);
		return out;
	}

	/**
	 * Copies values from one value hash to this one.
	 * @param hash the source hash to copy values from.
	 */
	public void copyFrom(ValueSet hash)
	{
		for (Map.Entry<String, Value> pair : hash)
			put(pair.getKey(), Value.create(pair.getValue()));
	}

	/**
	 * Clears this hash.
	 */
	public void clear() 
	{
		valueMap.clear();
	}

	/**
	 * Gets a value on this hash.
	 * @param variableName the variable name.
	 * @return the corresponding value, or null.
	 */
	public Value get(String variableName)
	{
		return valueMap.get(variableName);
	}

	/**
	 * Sets a value on this hash.
	 * @param variableName the variable name.
	 * @param value the value.
	 */
	public void put(String variableName, Value value) 
	{
		valueMap.put(variableName, value);
	}

	/**
	 * Clears a variable from the hash.
	 * @param variableName the name of the variable. 
	 */
	public void remove(String variableName)
	{
		valueMap.remove(variableName);
	}

	/**
	 * Checks if a variable is defined on this hash.
	 * @param variableName the name of the variable. 
	 * @return if the variable exists or not.
	 */
	public boolean containsKey(String variableName)
	{
		return valueMap.containsKey(variableName);
	}

	/**
	 * @return a list of all values.
	 */
	public List<String> names()
	{
		return valueMap.keys();
	}

	@Override
	public Iterator<Entry<String, Value>> iterator()
	{
		return valueMap.iterator();
	}

	@Override
	public void writeReferentialBytes(AtomicLong referenceCounter, Map<Object, Long> referenceSet, OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, size());
		for (Map.Entry<String, Value> hp : this)
		{
			sw.writeString(out, hp.getKey(), "UTF-8");
			hp.getValue().writeReferentialBytes(referenceCounter, referenceSet, out);
		}
	}

	@Override
	public void readReferentialBytes(Map<Long, Value> referenceMap, InputStream in) throws IOException
	{
		clear();
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		int x = sr.readInt(in);
		for (int i = 0; i < x; i++)
		{
			String name = sr.readString(in, "UTF-8");
			Value value = Value.read(referenceMap, in);
			put(name, value);
		}
	}

	@Override
	public int size()
	{
		return valueMap.size();
	}

	@Override
	public boolean isEmpty() 
	{
		return size() == 0;
	}

}
