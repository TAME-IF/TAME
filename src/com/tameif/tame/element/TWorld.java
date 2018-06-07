/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element;

import java.io.IOException;
import java.io.InputStream;

import com.tameif.tame.TAMEConstants;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.BlockEntry;
import com.tameif.tame.lang.BlockEntryType;
import com.tameif.tame.lang.FunctionEntry;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TElement implements ObjectContainer
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
	
	@Override
	public final boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case START:
			case ONACTION:
			case ONACTIONWITH:
			case ONACTIONWITHANCESTOR:
			case ONACTIONWITHOTHER:
			case ONMALFORMEDCOMMAND:
			case ONMODALACTION:
			case ONUNHANDLEDACTION:
			case ONINCOMPLETECOMMAND:
			case ONAMBIGUOUSCOMMAND:
			case ONUNKNOWNCOMMAND:
			case AFTERSUCCESSFULCOMMAND:
			case AFTERFAILEDCOMMAND:
			case AFTEREVERYCOMMAND:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void setParent(TElement parent)
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
	
	@Override
	public FunctionEntry resolveFunction(String functionName)
	{
		return getFunction(functionName);
	}

}
