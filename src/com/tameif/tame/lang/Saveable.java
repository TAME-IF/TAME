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

/**
 * Describes an object that can be saved/loaded.
 * @author Matthew Tropiano
 */
public interface Saveable
{
	/**
	 * Exports this object to bytes.
	 * @param out the output stream to write to.
	 * @throws IOException if a write problem occurs.
	 */
	public void writeBytes(OutputStream out) throws IOException;
	
	/**
	 * Imports this object from bytes.
	 * @param in the input stream to read from.
	 * @throws IOException if a read problem occurs.
	 */
	public void readBytes(InputStream in) throws IOException;
	
	/**
	 * Gets this object's representation as bytes.
	 * @return the byte array of state bytes.
	 * @throws IOException if a write problem occurs.
	 */
	default byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	/**
	 * Reads this object's representation from a byte array.
	 * @param data the data to read.
	 * @throws IOException if a read problem occurs.
	 */
	default void fromBytes(byte[] data) throws IOException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
	}
	
}
