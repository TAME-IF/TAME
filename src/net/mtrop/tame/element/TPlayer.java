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

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.lang.PermissionType;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionModeTable;
import net.mtrop.tame.struct.ActionTable;

import com.blackrook.commons.hash.Hash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * The viewpoint inside an adventure game.
 * The viewpoint can travel to different rooms, changing view aspects.
 * @author Matthew Tropiano
 */
public class TPlayer extends TActionableElement 
	implements ActionForbiddenHandler, ActionFailedHandler, ActionModalHandler, 
	ActionUnknownHandler, ActionAmbiguousHandler
{
	/** Set function for action list. */
	protected PermissionType permissionType;
	/** List of actions that are either restricted or excluded. */
	protected Hash<String> permittedActionList;

	/** Table used for modal actions. */
	protected ActionModeTable modalActionTable;
	/** Blocks executed on action disallow. */
	protected ActionTable actionForbiddenTable;
	/** Blocks executed on action failure. */
	protected ActionTable actionFailedTable;
	/** Blocks ran when an action is ambiguous. */
	protected ActionTable actionAmbiguousTable;
	
	/** Code block ran upon default action disallow. */
	protected Block actionForbiddenBlock;
	/** Code block ran upon default action fail. */
	protected Block actionFailedBlock;
	/** Code block ran when an action is ambiguous. */
	protected Block actionAmbiguityBlock;
	/** Code block ran upon bad action. */
	protected Block unknownActionBlock;

	/**
	 * Constructs an instance of a game world.
	 */
	public TPlayer()
	{
		super();
		setIdentity(TAMEConstants.IDENTITY_CURRENT_WORLD);
		
		this.permissionType = PermissionType.EXCLUDE;
		this.permittedActionList = new Hash<String>(2);
		
		this.modalActionTable = new ActionModeTable();
		this.actionForbiddenTable = new ActionTable();
		this.actionFailedTable = new ActionTable();
		this.actionAmbiguousTable = new ActionTable();
		
		this.actionForbiddenBlock = null;
		this.actionFailedBlock = null;
		this.actionAmbiguityBlock = null;
		this.unknownActionBlock = null;
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

	@Override
	public ActionModeTable getModalActionTable()
	{
		return modalActionTable;
	}

	@Override
	public ActionTable getActionForbiddenTable()
	{
		return actionForbiddenTable;
	}

	@Override
	public ActionTable getActionFailedTable()
	{
		return actionFailedTable;
	}

	@Override
	public ActionTable getAmbiguousActionTable()
	{
		return actionAmbiguousTable;
	}
	
	@Override
	public Block getUnknownActionBlock()
	{
		return unknownActionBlock;
	}

	@Override
	public void setUnknownActionBlock(Block block)
	{
		unknownActionBlock = block;
	}

	@Override
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}
	
	@Override
	public void setAmbiguousActionBlock(Block block)
	{
		actionAmbiguityBlock = block;
	}

	@Override
	public Block getActionForbiddenBlock()
	{
		return actionForbiddenBlock;
	}
	
	@Override
	public void setActionForbiddenBlock(Block block)
	{
		actionForbiddenBlock = block;
	}

	@Override
	public Block getActionFailedBlock()
	{
		return actionFailedBlock;
	}
	
	@Override
	public void setActionFailedBlock(Block block)
	{
		actionFailedBlock = block;
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
		
		modalActionTable.writeBytes(out);
		actionForbiddenTable.writeBytes(out);
		actionFailedTable.writeBytes(out);
		actionAmbiguousTable.writeBytes(out);

		sw.writeBit(actionForbiddenBlock != null);
		sw.writeBit(actionFailedBlock != null);
		sw.writeBit(actionAmbiguityBlock != null);
		sw.writeBit(unknownActionBlock != null);
		sw.flushBits();
		
		if (actionForbiddenBlock != null)
			actionForbiddenBlock.writeBytes(out);
		if (actionFailedBlock != null)
			actionFailedBlock.writeBytes(out);
		if (actionAmbiguityBlock != null)
			actionAmbiguityBlock.writeBytes(out);
		if (unknownActionBlock != null)
			unknownActionBlock.writeBytes(out);
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
		actionForbiddenTable = ActionTable.create(in);
		actionFailedTable = ActionTable.create(in);
		actionAmbiguousTable = ActionTable.create(in);
		
		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			actionForbiddenBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			actionFailedBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			actionAmbiguityBlock = Block.create(in);
		if ((blockbits & 0x08) != 0)
			unknownActionBlock = Block.create(in);
	}

}
