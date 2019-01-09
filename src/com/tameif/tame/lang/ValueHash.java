/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
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
import java.util.concurrent.atomic.AtomicLong;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Convenience class for Value lookup tables.
 * @author Matthew Tropiano
 */
public class ValueHash extends CaseInsensitiveHashMap<Value> implements ReferenceSaveable
{
	
	/**
	 * Creates a new ValueHash.
	 */
	public ValueHash()
	{
		super(4);
	}
	
	/**
	 * Copies values from one value hash to this one.
	 * @param hash the source hash to copy values from.
	 */
	public void copyFrom(ValueHash hash)
	{
		for (ObjectPair<String, Value> pair : hash)
			put(pair.getKey(), Value.create(pair.getValue()));
	}

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param referenceMap the reference map to use for "seen" value references.
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static ValueHash create(AbstractMap<Long, Value> referenceMap, InputStream in) throws IOException
	{
		ValueHash out = new ValueHash();
		out.readReferentialBytes(referenceMap, in);
		return out;
	}

	@Override
	public void writeReferentialBytes(AtomicLong referenceCounter, AbstractMap<Object, Long> referenceSet, OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(size());
		for (ObjectPair<String, Value> hp : this)
		{
			sw.writeString(hp.getKey(), "UTF-8");
			hp.getValue().writeReferentialBytes(referenceCounter, referenceSet, out);
		}
	}
	
	@Override
	public void readReferentialBytes(AbstractMap<Long, Value> referenceMap, InputStream in) throws IOException
	{
		clear();
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		int x = sr.readInt();
		for (int i = 0; i < x; i++)
		{
			String name = sr.readString("UTF-8");
			Value value = Value.read(referenceMap, in);
			put(name, value);
		}
		
	}

	@Override
	public byte[] toReferentialBytes(AtomicLong referenceCounter, AbstractMap<Object, Long> referenceSet) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeReferentialBytes(referenceCounter, referenceSet, bos);
		return bos.toByteArray();
	}

	@Override
	public void fromReferentialBytes(AbstractMap<Long, Value> referenceMap, byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readReferentialBytes(referenceMap, bis);
		bis.close();
	}

}
