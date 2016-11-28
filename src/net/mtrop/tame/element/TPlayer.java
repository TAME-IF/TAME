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
import java.io.OutputStream;

import net.mtrop.tame.lang.PermissionType;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;

import com.blackrook.commons.hash.Hash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * The viewpoint inside a world.
 * The viewpoint can travel to different rooms, changing view aspects.
 * @author Matthew Tropiano
 */
public class TPlayer extends TElement implements ForbiddenHandler, Inheritable<TPlayer>
{
	/** The parent player. */
	private TPlayer parent;
	
	/** Set function for action list. */
	protected PermissionType permissionType;
	/** List of actions that are either restricted or excluded. */
	protected Hash<String> permittedActionList;

	private TPlayer()
	{
		super();
		this.parent = null;
		this.permissionType = PermissionType.EXCLUDE;
		this.permittedActionList = new Hash<String>(2);
	}

	/**
	 * Creates an empty player.
	 * @param identity its main identity.
	 */
	public TPlayer(String identity)
	{
		this();
		setIdentity(identity);
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
			case ONFORBIDDENACTION:
			case ONINCOMPLETEACTION:
			case ONAMBIGUOUSACTION:
			case ONUNKNOWNACTION:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void setParent(TPlayer parent)
	{
		this.parent = parent;
	}
	
	@Override
	public TPlayer getParent()
	{
		return parent;
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
	public boolean allowsAction(TAction action)
	{
		if (permissionType == PermissionType.EXCLUDE)
			return !permittedActionList.contains(action.getIdentity());
		else if (permissionType == PermissionType.RESTRICT)
			return permittedActionList.contains(action.getIdentity());
		else
			throw new ModuleException("Bad or unknown permission type found: "+permissionType);
	}
	
	@Override
	public Block resolveBlock(BlockEntry blockEntry)
	{
		Block out = getBlock(blockEntry);
		return out != null ? out : (parent != null ? parent.resolveBlock(blockEntry) : null);
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
		permissionType = PermissionType.values()[sr.readByte()];
		
		permittedActionList.clear();
		int size = sr.readInt();
		while (size-- > 0)
			permittedActionList.put(sr.readString("UTF-8"));
	}

}
