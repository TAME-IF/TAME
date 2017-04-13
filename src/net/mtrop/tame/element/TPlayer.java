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
import java.io.OutputStream;

import net.mtrop.tame.lang.PermissionType;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;
import net.mtrop.tame.lang.FunctionEntry;

import com.blackrook.commons.hash.Hash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * The viewpoint inside a world.
 * The viewpoint can travel to different rooms, changing view aspects.
 * @author Matthew Tropiano
 */
public class TPlayer extends TElement implements ForbiddenHandler, Inheritable<TPlayer>, ObjectContainer
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
		this.permissionType = PermissionType.FORBID;
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
	
	@Override
	public final boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case ONACTION:
			case ONBADACTION:
			case ONMODALACTION:
			case ONFAILEDACTION:
			case ONFORBIDDENACTION:
			case ONROOMFORBIDDENACTION:
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
