/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
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
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockTable;
import net.mtrop.tame.lang.Saveable;

/**
 * Common engine element object.
 * @author Matthew Tropiano
 */
public abstract class TElement implements Saveable
{	
	/** Element's primary identity. */
	private String identity;
	/** Element block map. */
	private BlockTable blockTable;

	/**
	 * Prepares a new element.
	 */
	protected TElement()
	{
		this.blockTable = new BlockTable();
	}
	
	/** 
	 * Gets the identity (primary identifier name).
	 * @return this element's identity. 
	 */
	public String getIdentity()
	{
		return identity;
	}
	
	/** 
	 * Sets the identity (primary identifier name). 
	 * @param identity the identity to set.
	 */
	protected void setIdentity(String identity)
	{
		if (Common.isEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		this.identity = identity;
	}
		
	/**
	 * Adds/replaces a block and block entry to this element.
	 * @param blockEntry the block entry to associate with a block. 
	 * @param block the block to assign.
	 */
	public void addBlock(BlockEntry blockEntry, Block block)
	{
		blockTable.add(blockEntry, block);
	}
	
	/**
	 * Gets a block using a block entry on this element.
	 * @param blockEntry the block entry to use.
	 * @return the associated block, or null if no associated block.
	 */
	public Block getBlock(BlockEntry blockEntry)
	{
		return blockTable.get(blockEntry);
	}
	
	/**
	 * Resolves a block using a block entry on this element 
	 * by going backwards through its lineage.
	 * @param blockEntry the block entry to use.
	 * @return an associated block, or null if no associated block.
	 */
	public abstract Block resolveBlock(BlockEntry blockEntry);

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
	 * @param element the other element.
	 * @return true if equal, false if not.
	 */
	public boolean equals(TElement element)
	{
		return getClass().equals(element.getClass()) && identity.equals(element.identity);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getIdentity() + "]";
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeString(identity, "UTF-8");
		blockTable.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		setIdentity(sr.readString("UTF-8"));
		blockTable = BlockTable.create(in);
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
