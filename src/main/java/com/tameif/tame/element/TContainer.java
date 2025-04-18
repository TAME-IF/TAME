/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element;

import java.io.IOException;
import java.io.InputStream;

import com.tameif.tame.lang.BlockEntryType;

/**
 * Container that just holds objects. It cannot be actioned on.
 * @author Matthew Tropiano
 */
public class TContainer extends TElement implements ObjectContainer
{
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

	@Override
	public TContainer getParent()
	{
		return (TContainer)(super.getParent());
	}

}
