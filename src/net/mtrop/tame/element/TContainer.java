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

import java.io.IOException;
import java.io.InputStream;

import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;
import net.mtrop.tame.lang.FunctionEntry;

/**
 * Container that just holds objects. It cannot be actioned on.
 * @author Matthew Tropiano
 */
public class TContainer extends TElement implements Inheritable<TContainer>, ObjectContainer
{
	/** The parent container. */
	private TContainer parent;	

	private TContainer()
	{
		super();
	}
	
	/**
	 * Creates an empty container.
	 * @param identity its main identity.
	 * @param parent the container's parent object.
	 */
	public TContainer(String identity, TContainer parent) 
	{
		this();
		setIdentity(identity);
		setParent(parent);
	}

	/**
	 * Creates an empty container.
	 * @param identity its main identity.
	 */
	public TContainer(String identity) 
	{
		this(identity, null);
	}

	@Override
	public final boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
				return true;
			default:
				return false;
		}
	}

	private boolean hasCircularParentReference(TContainer parent)
	{
		if (this.parent != null)
			return this.parent == parent || this.parent.hasCircularParentReference(parent);
		else
			return false;
	}
	
	@Override
	public void setParent(TContainer parent)
	{
		if (hasCircularParentReference(parent))
			throw new ModuleException("Circular lineage detected.");
		if (this.parent != null && this.parent != parent)
			throw new ModuleException("Parent elements cannot be reassigned once set.");
		this.parent = parent;
	}

	@Override
	public TContainer getParent()
	{
		return parent;
	}
	
	@Override
	public Block resolveBlock(BlockEntry blockEntry)
	{
		Block out = getBlock(blockEntry);
		return out != null ? out : (parent != null ? parent.resolveBlock(blockEntry) : null);
	}
	
	@Override
	public FunctionEntry resolveFunction(String functionName)
	{
		FunctionEntry out = getFunction(functionName);
		return out != null ? out : (parent != null ? parent.resolveFunction(functionName) : null);
	}

	/**
	 * Creates this Container from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TContainer create(InputStream in) throws IOException
	{
		TContainer out = new TContainer();
		out.readBytes(in);
		return out;
	}

	
}
