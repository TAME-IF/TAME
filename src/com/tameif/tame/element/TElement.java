/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.commons.util.ObjectUtils;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.BlockEntry;
import com.tameif.tame.lang.BlockEntryType;
import com.tameif.tame.lang.BlockTable;
import com.tameif.tame.lang.FunctionEntry;
import com.tameif.tame.lang.FunctionTable;
import com.tameif.tame.lang.Saveable;

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
	/** Element is an archetype (defines behavior, is not physical). */
	private boolean archetype;
	/** Element parent. */
	private TElement parent;
	/** Element block map. */
	private BlockTable blockTable;
	/** Function map. */
	private FunctionTable functionTable;

	/**
	 * Prepares a new element.
	 */
	protected TElement()
	{
		this.archetype = false;
		this.blockTable = new BlockTable();
		this.functionTable = new FunctionTable();
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
		if (ValueUtils.isStringEmpty(identity))
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
		if (ValueUtils.isStringEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		this.identity = identity;
	}
		
	/**
	 * Checks if this element handles a particular entry type.
	 * @param type the entry type to check.
	 * @return true if so, false if not.
	 */
	public abstract boolean isValidEntryType(BlockEntryType type);

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
	public Block resolveBlock(BlockEntry blockEntry)
	{
		Block out = getBlock(blockEntry);
		return out != null ? out : (parent != null ? parent.resolveBlock(blockEntry) : null);
	}
	
	/**
	 * Adds/replaces a block and block entry to this element.
	 * @param functionName the function name to associate with the table.
	 * @param entry the entry to associate.
	 */
	public void addFunction(String functionName, FunctionEntry entry)
	{
		functionTable.add(functionName, entry);
	}
	
	/**
	 * @return an iterable structure for all function entries in this table.
	 */
	public Iterable<ObjectPair<String, FunctionEntry>> getFunctionEntries()
	{
		return functionTable.getEntries();
	}
	
	/**
	 * Gets a function entry using a function name on this element.
	 * @param functionName the function name to use.
	 * @return the associated entry, or null if no associated entry.
	 */
	public FunctionEntry getFunction(String functionName)
	{
		return functionTable.get(functionName);
	}
	
	/**
	 * Resolves a function entry using a function name on this element 
	 * by going backwards through its lineage.
	 * @param functionName the function name to use.
	 * @return an associated entry, or null if no associated entry.
	 */
	public FunctionEntry resolveFunction(String functionName)
	{
		FunctionEntry out = getFunction(functionName);
		return out != null ? out : (parent != null ? parent.resolveFunction(functionName) : null);
	}

	/**
	 * Checks for a circular reference.
	 */
	private boolean hasCircularParentReference(TElement parent)
	{
		if (this.parent != null)
			return this.parent == parent || this.parent.hasCircularParentReference(parent);
		else
			return false;
	}
	
	/**
	 * Sets a parent on this object.
	 * @param parent the parent object to set.
	 */
	public void setParent(TElement parent)
	{
		if (hasCircularParentReference(parent))
			throw new ModuleException("Circular lineage detected.");
		if (this.parent != null && this.parent != parent)
			throw new ModuleException("Parent elements cannot be reassigned once set.");
		this.parent = parent;
	}
	
	/**
	 * Gets this object's parent.
	 * The ordering is in the order they were added.
	 * @return an iterator for this object's lineage.
	 * @see #setParent(TElement)
	 */
	public TElement getParent()
	{
		return parent;
	}

	/**
	 * Checks if this element is an Archetype.
	 * Archetypes do not hold state nor have contexts - they only define code.
	 * Archetypes can be inherited from, but can never be owned, nor own things, nor be in expressions nor operations.
	 * @return true if so, false if not.
	 */
	public boolean isArchetype() 
	{
		return archetype;
	}
	
	/**
	 * Sets if this element is an Archetype.
	 * Archetypes do not hold state nor have contexts - they only define code.
	 * Archetypes can be inherited from, but can never be owned, nor own things, nor be in expressions nor operations.
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
		return getIdentity();
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeString(identity, "UTF-8");
		sw.writeBoolean(archetype);
		blockTable.writeBytes(out);
		functionTable.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		identity = sr.readString("UTF-8");
		archetype = sr.readBoolean();
		blockTable = BlockTable.create(in);
		functionTable = FunctionTable.create(in);
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
