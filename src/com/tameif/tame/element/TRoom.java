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
 * Environmental instance of every adventure game.
 * Adventure games have rooms. It is where the game takes place and players interact with
 * things. Objects can be contained in this.
 * Players can travel from room to room freely, provided that the world allows them to.
 * @author Matthew Tropiano
 */
public class TRoom extends TElement implements ObjectContainer
{
	private TRoom()
	{
		super();
	}

	/**
	 * Creates an empty room.
	 * @param identity its main identity.
	 * @param parent the room's parent object.
	 */
	public TRoom(String identity, TRoom parent)
	{
		this();
		setIdentity(identity);
		setParent(parent);
	}
	
	/**
	 * Creates an empty room.
	 * @param identity its main identity.
	 */
	public TRoom(String identity)
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
			case ONMODALACTION:
			case ONACTIONWITH:
			case ONACTIONWITHANCESTOR:
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
	public static TRoom create(InputStream in) throws IOException
	{
		TRoom out = new TRoom();
		out.readBytes(in);
		return out;
	}

}
