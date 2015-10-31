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
package net.mtrop.tame.element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.Common;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Saveable;

/**
 * Common engine element object.
 * @author Matthew Tropiano
 */
public abstract class TElement implements Saveable
{
	/** Element's primary identity. */
	private String identity;
	
	/** Code block ran upon world initialization. */
	private Block initBlock;

	/**
	 * Set on initial creation. This flag is checked to ensure all objects were defined.
	 * If this is set on any object after the time of compile, then an object was only
	 * prototyped, but not completely defined. It is set by default and must be cleared.
	 */
	private boolean prototyped;

	protected TElement()
	{
		this.identity = null;
		this.initBlock = null;
		this.prototyped = true;
	}
	
	/**
	 * Sets if this is prototyped.
	 * This is set on initial creation. This flag is checked to ensure all objects were defined.
	 * If this is set on any object after the time of compile, then an object was only
	 * prototyped, but not completely defined. It is set by default and must be cleared.
	 */
	public void setPrototyped(boolean prototyped)
	{
		this.prototyped = prototyped;
	}
	
	/** 
	 * Checks if the prototype flag is set. 
	 */
	public boolean isPrototyped()
	{
		return prototyped;
	}
	
	/** 
	 * Gets the identity (primary identifier name). 
	 */
	public String getIdentity()
	{
		return identity;
	}
	
	/** 
	 * Sets the identity (primary identifier name). 
	 */
	public void setIdentity(String identity)
	{
		if (Common.isEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		this.identity = identity;
	}
	
	/** 
	 * Get this element's initialization block. 
	 */
	public Block getInitBlock()
	{
		return initBlock;
	}
	
	/** 
	 * Set this element's initialization block. 
	 */
	public void setInitBlock(Block eab)
	{
		initBlock = eab;
	}

	@Override
	public int hashCode()
	{
		return identity.hashCode();
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object instanceof TElement)
			return equals((TElement)object);
		else 
			return super.equals(object);
	}
	
	/**
	 * Compares elements. Uses identity.
	 */
	public boolean equals(TElement object)
	{
		return getClass().equals(object.getClass()) && identity.equals(object.identity);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + getIdentity() + "]";
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeString(identity, "UTF-8");
		sw.writeBit(initBlock != null);
		sw.flushBits();
		if (initBlock != null)
			initBlock.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		setIdentity(sr.readString("UTF-8"));
		byte blockbits = sr.readByte();
		if ((blockbits & 0x01) != 0)
			initBlock = Block.create(in);
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