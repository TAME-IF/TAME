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
public class ValueHash extends CaseInsensitiveHashMap<Value>
{
	public ValueHash()
	{
		super(4);
	}
	
	/**
	 * Reads a value hash from an input stream.
	 * @param in the stream to read from.
	 * @throws IOException if a read error occurs.
	 */
	public void readBytes(InputStream in) throws IOException
	{
		clear();
		SuperReader sr = new SuperReader(in,SuperReader.LITTLE_ENDIAN);
		int x = sr.readInt();
		for (int i = 0; i < x; i++)
			put(sr.readString(), Value.create(in));
	}
	
	/**
	 * Writes a value hash to an output stream.
	 * @param out the stream to write to.
	 * @throws IOException if a read error occurs.
	 */
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out,SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(size());
		for (ObjectPair<String, Value> hp : this)
		{
			sw.writeString(hp.getKey());
			hp.getValue().writeBytes(out);
		}
	}
	
	/**
	 * Copies values from one value hash to this one.
	 */
	public void copyFrom(ValueHash hash)
	{
		for (ObjectPair<String, Value> pair : hash)
			put(pair.getKey(), pair.getValue().copy());
	}
	
}
