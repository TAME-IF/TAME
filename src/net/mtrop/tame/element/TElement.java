/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.exception.ModuleException;
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
	private static final CaseInsensitiveHash BAD_IDENTITIES = new CaseInsensitiveHash()
	{{
		put("player");
		put("room");
		put("world");
	}};
	
	/** Element's primary identity. */
	private String identity;
	/** Element block map. */
	private BlockTable blockTable;
	/** Element is an archetype (defines behavior, is not physical). */
	private boolean archetype;

	/**
	 * Prepares a new element.
	 */
	protected TElement()
	{
		this.blockTable = new BlockTable();
		this.archetype = false;
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
		if (BAD_IDENTITIES.contains(identity))
			throw new IllegalArgumentException("Identity cannot be \"player\" or \"room\" or \"world\".");
		this.identity = identity;
	}
		
	/** 
	 * Sets the identity (primary identifier name) without doing an illegal identity check. 
	 * @param identity the identity to set.
	 */
	protected void setIdentityForced(String identity)
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
	 * @return an iterable structure for all block entries in this table.
	 */
	public Iterable<ObjectPair<BlockEntry, Block>> getBlockEntries()
	{
		return blockTable.getEntries();
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

	/**
	 * Checks if this element is an Archetype.
	 * Archetypes do not hold state nor have contexts - they only define code.
	 * Archetypes can be inherited from, but can never be owned, nor own things, nor be in expressions nor commands.
	 * @return true if so, false if not.
	 */
	public boolean isArchetype() 
	{
		return archetype;
	}
	
	/**
	 * Sets if this element is an Archetype.
	 * Archetypes do not hold state nor have contexts - they only define code.
	 * Archetypes can be inherited from, but can never be owned, nor own things, nor be in expressions nor commands.
	 * @param archetype true if so, false if not.
	 * @throws ModuleException if this element cannot be an archetype and <code>archetype</code> is <code>true</code>, or 
	 * if you attempt to set this to <code>false</code> on an object that has this already set to <code>true</code>.
	 */
	public void setArchetype(boolean archetype) 
	{
		if (this.archetype && !archetype)
			throw new ModuleException("You cannot set \"archetype\" to false if this is already true!");
		this.archetype = archetype;
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
		sw.writeBoolean(archetype);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		identity = sr.readString("UTF-8");
		blockTable = BlockTable.create(in);
		archetype = sr.readBoolean();
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
