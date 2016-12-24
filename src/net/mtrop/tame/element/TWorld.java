/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TElement implements Inheritable<TWorld>
{
	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super();
		setIdentityForced(TAMEConstants.IDENTITY_CURRENT_WORLD);
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TWorld create(InputStream in) throws IOException
	{
		TWorld out = new TWorld();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Checks if this object handles a particular entry type.
	 * @param type the entry type to check.
	 * @return true if so, false if not.
	 */
	public static boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case PROCEDURE:
			case ONACTION:
			case ONBADACTION:
			case ONMODALACTION:
			case ONFAILEDACTION:
			case ONINCOMPLETEACTION:
			case ONAMBIGUOUSACTION:
			case ONUNKNOWNACTION:
			case AFTERREQUEST:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void setParent(TWorld parent)
	{
		throw new ModuleException("Worlds cannot have a lineage!");
	}
	
	@Override
	public void setArchetype(boolean archetype) 
	{
		if (archetype)
			throw new ModuleException("Worlds cannot be archetypes!");
	}
	
	@Override
	public TWorld getParent()
	{
		return null;
	}
	
	@Override
	public Block resolveBlock(BlockEntry blockEntry)
	{
		return getBlock(blockEntry);
	}
	
}
