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
import com.tameif.tame.lang.BlockEntryType;

/**
 * The viewpoint inside a world.
 * The viewpoint can travel to different rooms, changing view aspects.
 * @author Matthew Tropiano
 */
public class TPlayer extends TElement implements ObjectContainer
{
	private TPlayer()
	{
		super();
	}

	/**
	 * Creates an empty player.
	 * @param identity its main identity.
	 * @param parent the player's parent object.
	 */
	public TPlayer(String identity, TPlayer parent)
	{
		this();
		setIdentity(identity);
		setParent(parent);
	}
	
	/**
	 * Creates an empty player.
	 * @param identity its main identity.
	 */
	public TPlayer(String identity)
	{
		this(identity, null);
	}
	
	@Override
	public final boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case ONACTION:
			case ONACTIONWITH:
			case ONACTIONWITHANCESTOR:
			case ONACTIONWITHOTHER:
			case ONMODALACTION:
			case ONUNHANDLEDACTION:
			case ONMALFORMEDCOMMAND:
			case ONINCOMPLETECOMMAND:
			case ONAMBIGUOUSCOMMAND:
			case ONUNKNOWNCOMMAND:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TPlayer create(InputStream in) throws IOException
	{
		TPlayer out = new TPlayer();
		out.readBytes(in);
		return out;
	}

	@Override
	public TPlayer getParent()
	{
		return (TPlayer)(super.getParent());
	}

}
