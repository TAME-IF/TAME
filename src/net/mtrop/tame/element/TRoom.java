/*******************************************************************************
 * Copyright (c) 2009-2013 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.lang.PermissionType;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionModeTable;
import net.mtrop.tame.struct.ActionTable;

import com.blackrook.commons.hash.Hash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Environmental instance of every adventure game.
 * 
 * Adventure games have rooms. It is where the game takes place and players interact with
 * things. Objects and people (who are also considered "objects") can be found here.
 * Players can travel from room to room freely, provided that the world allows them to.
 * 
 * @author Matthew Tropiano
 *
 */
public class TRoom extends TActionableElement implements ActionForbiddenHandler, ActionModalHandler
{
	/** Set function for action list. */
	protected PermissionType permissionType;
	/** List of actions that are either restricted or excluded. */
	protected Hash<String> permittedActionList;

	/** Table used for modal actions. (actionName, mode) -> block. */
	protected ActionModeTable modalActionTable;
	
	/** Blocks executed on action disallow. */
	protected ActionTable actionForbidTable;
	/** Code block ran upon default action disallow. */
	protected Block actionForbidBlock;

	private TRoom()
	{
		super();
		
		permissionType = PermissionType.EXCLUDE;
		permittedActionList = new Hash<String>(2);
		
		modalActionTable = new ActionModeTable();
		actionForbidTable = new ActionTable();
		
		actionForbidBlock = null;
	}

	/**
	 * Creates an empty room.
	 * @param identity its main identity.
	 */
	public TRoom(String identity)
	{
		this();
		setIdentity(identity);
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
	public void addPermittedAction(TAction action)
	{
		permittedActionList.put(action.getIdentity());
	}
	
	@Override
	public boolean allowsAction(TAction action)
	{
		if (permissionType == PermissionType.EXCLUDE)
			return !permittedActionList.contains(action.getIdentity());
		else
			return permittedActionList.contains(action.getIdentity());
	}
	
	/** 
	 * Gets the modal action table. 
	 */
	public ActionModeTable getModalActionTable()
	{
		return modalActionTable;
	}
	
	/**
	 * Gets the action forbidden table for specific actions. 
	 */
	public ActionTable getActionForbiddenTable()
	{
		return actionForbidTable;
	}
	
	@Override
	public Block getActionForbiddenBlock()
	{
		return actionForbidBlock;
	}
	
	@Override
	public void setActionForbiddenBlock(Block block)
	{
		actionForbidBlock = block;
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
		
		modalActionTable.writeBytes(out);
		actionForbidTable.writeBytes(out);
		
		sw.writeBit(actionForbidBlock != null);
		sw.flushBits();

		if (actionForbidBlock != null)
			actionForbidBlock.writeBytes(out);
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
	
		modalActionTable = ActionModeTable.create(in);
		actionForbidTable = ActionTable.create(in);
		
		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			actionForbidBlock = Block.create(in);
	}

}
