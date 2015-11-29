/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.list.List;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.lang.Saveable;

/**
 * TAME Module Header.
 * @author Matthew Tropiano
 */
public class TAMEModuleHeader implements Saveable
{
	/** Module attributes. */
	private CaseInsensitiveHashMap<String> attributes;
	
	/**
	 * Creates a new module header.
	 */
	public TAMEModuleHeader()
	{
		this.attributes = new CaseInsensitiveHashMap<String>(4);
	}

	/**
	 * Adds an attribute to the module. Attributes are case-insensitive.
	 * There are a bunch of suggested ones that all clients/servers should read.
	 * @param attribute the attribute name.
	 * @param value the value.
	 */
	public void addAttribute(String attribute, String value)
	{
		attributes.put(attribute, value);
	}
	
	/**
	 * Gets an attribute value from the module. 
	 * Attributes are case-insensitive.
	 * There are a bunch of suggested ones that all clients/servers should read.
	 * @param attribute the attribute name.
	 * @return the corresponding value or null if not found.
	 */
	public String getAttribute(String attribute)
	{
		return attributes.get(attribute);
	}
	
	/**
	 * Gets all of this module's attributes.
	 * @return an array of all of the attributes. Never returns null.
	 */
	public String[] getAllAttributes()
	{
		List<String> outList = new List<>();
		Iterator<String> it = attributes.keyIterator();
		while (it.hasNext())
			outList.add(it.next());
		
		String[] out = new String[outList.size()];
		outList.toArray(out);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		
		sw.writeInt(attributes.size());
		for (ObjectPair<String, String> pair : attributes)
		{
			sw.writeString(pair.getKey(), "UTF-8");
			sw.writeString(pair.getValue(), "UTF-8");
		}
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		attributes.clear();
		int attribCount = sr.readInt();
		while(attribCount-- > 0)
			attributes.put(sr.readString("UTF-8"), sr.readString("UTF-8"));		
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
