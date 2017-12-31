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
import java.io.OutputStream;

import com.blackrook.commons.hash.Hash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.lang.BlockEntryType;
import com.tameif.tame.lang.PermissionType;

/**
 * Environmental instance of every adventure game.
 * Adventure games have rooms. It is where the game takes place and players interact with
 * things. Objects can be contained in this.
 * Players can travel from room to room freely, provided that the world allows them to.
 * @author Matthew Tropiano
 */
public class TRoom extends TElement implements ForbiddenHandler, ObjectContainer
{
	/** Set function for action list. */
	private PermissionType permissionType;
	/** List of actions that are either restricted or excluded. */
	private Hash<String> permittedActionList;

	private TRoom()
	{
		super();
		this.permissionType = PermissionType.FORBID;
		this.permittedActionList = new Hash<String>(2);
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
			case ONUNHANDLEDACTION:
				return true;
			default:
				return false;
		}
	}

	@Override
	public PermissionType getPermissionType()
	{
		return permissionType;
	}

	@Override
	public void setPermissionType(PermissionType permissionType)
	{
		this.permissionType = permissionType;
	}

	@Override
	public void addPermissionAction(TAction action)
	{
		permittedActionList.put(action.getIdentity());
	}

	@Override
	public Iterable<String> getPermissionActions()
	{
		return permittedActionList;
	}

	@Override
	public boolean allowsAction(TAction action)
	{
		if (permissionType == PermissionType.ALLOW)
			return permittedActionList.contains(action.getIdentity());
		else if (action.isRestricted())
			return false;
		else if (permissionType == PermissionType.FORBID)
			return !permittedActionList.contains(action.getIdentity());
		else
			throw new ModuleException("Bad or unknown permission type found: "+permissionType);
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

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		super.writeBytes(out);
		sw.writeByte((byte)permissionType.ordinal());

		sw.writeInt(permittedActionList.size());
		for (String actionIdentity : permittedActionList)
			sw.writeString(actionIdentity, "UTF-8");		
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		permissionType = PermissionType.VALUES[sr.readByte()];
		
		permittedActionList.clear();
		int size = sr.readInt();
		while (size-- > 0)
			permittedActionList.put(sr.readString("UTF-8"));
	}

}
