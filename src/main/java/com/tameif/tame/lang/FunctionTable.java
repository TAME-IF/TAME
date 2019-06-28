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
import java.util.Map;

import com.tameif.tame.struct.CaseInsensitiveStringMap;
import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

/**
 * The table of function name to function entry.
 * @author Matthew Tropiano
 */
public class FunctionTable implements Saveable
{
	/** Map itself. */
	private CaseInsensitiveStringMap<FunctionEntry> functionMap;
	
	/**
	 * Creates an empty function table.
	 */
	public FunctionTable()
	{
		this.functionMap = new CaseInsensitiveStringMap<>(8);
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static FunctionTable create(InputStream in) throws IOException
	{
		FunctionTable out = new FunctionTable();
		out.readBytes(in);
		return out;
	}

	/**
	 * Adds a function to this function table.
	 * @param name the name of the function.
	 * @param entry the function entry to associate.
	 */
	public void add(String name, FunctionEntry entry)
	{
		functionMap.put(name, entry);
	}

	/**
	 * Gets a matching function entry for this function name.
	 * @param name the function name.
	 * @return the associated block or null if not found.
	 */
	public FunctionEntry get(String name)
	{
		return functionMap.get(name);
	}
	
	/**
	 * @return an iterable structure for all entries in this table.
	 */
	public Iterable<Map.Entry<String, FunctionEntry>> getEntries()
	{
		return functionMap;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, functionMap.size());
		for (Map.Entry<String, FunctionEntry> entry : functionMap)
		{
			sw.writeString(out, entry.getKey(), "UTF-8");
			entry.getValue().writeBytes(out);
		}

	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		functionMap.clear();
		int size = sr.readInt(in);
		while (size-- > 0)
			add(sr.readString(in, "UTF-8"), FunctionEntry.create(in));
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
