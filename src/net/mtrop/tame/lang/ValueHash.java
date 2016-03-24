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

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;


/**
 * Convenience class for Value lookup tables.
 * @author Matthew Tropiano
 */
public class ValueHash extends CaseInsensitiveHashMap<Value> implements Saveable
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
	 */
	public void copyFrom(ValueHash hash)
	{
		for (ObjectPair<String, Value> pair : hash)
			put(pair.getKey(), Value.create(pair.getValue()));
	}

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static ValueHash create(InputStream in) throws IOException
	{
		ValueHash out = new ValueHash();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(size());
		for (ObjectPair<String, Value> hp : this)
		{
			sw.writeString(hp.getKey(), "UTF-8");
			hp.getValue().writeBytes(out);
		}
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		clear();
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		int x = sr.readInt();
		for (int i = 0; i < x; i++)
			put(sr.readString("UTF-8"), Value.create(in));
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

}
