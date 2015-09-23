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

import net.mtrop.tame.lang.ActionSet;
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
public class TRoom extends TElement
{
	/** Set function for action list. */
	protected ActionSet actionListFunction;
	/** List of actions that are either restricted or excluded. */
	protected Hash<String> actionList;

	/** Table used for modal actions. (actionName, mode) -> block. */
	protected ActionModeTable modalActionTable;
	
	/** Blocks executed on action disallow. */
	protected ActionTable actionForbidTable;
	/** Code block ran upon default action disallow. */
	protected Block actionForbidBlock;

	/** Code block ran upon focusing on this. */
	protected Block focusBlock;
	/** Code block ran upon focusing away from this. */
	protected Block unfocusBlock;
	
	/**
	 * Constructs an instance of a game world.
	 */
	public TRoom()
	{
		super();
		
		actionListFunction = ActionSet.EXCLUDE;
		actionList = new Hash<String>(2);
		
		modalActionTable = new ActionModeTable();
		actionForbidTable = new ActionTable();
		
		actionForbidBlock = null;
		focusBlock = null;
		unfocusBlock = null;
	}

	/**
	 * Gets the action list function.
	 */
	public ActionSet getActionListFunction()
	{
		return actionListFunction;
	}

	/**
	 * Sets the action list function.
	 */
	public void setActionListFunction(ActionSet actionListFunction)
	{
		this.actionListFunction = actionListFunction;
	}

	/**
	 * Adds an action to the action list to be excluded/restricted.
	 */
	public void addAction(TAction action)
	{
		actionList.put(action.getIdentity());
	}
	
	/**
	 * Returns if an action is allowed for this room.
	 */
	public boolean allowsAction(TAction action)
	{
		if (actionListFunction == ActionSet.EXCLUDE)
			return !actionList.contains(action.getIdentity());
		else
			return actionList.contains(action.getIdentity());
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
	public ActionTable getActionForbidTable()
	{
		return actionForbidTable;
	}
	
	/** 
	 * Get this room's default "onActionForbid" block. 
	 */
	public Block getActionForbidBlock()
	{
		return actionForbidBlock;
	}
	
	/** 
	 * Gets this room's default "onActionForbid" block. 
	 */
	public void setActionForbidBlock(Block block)
	{
		actionForbidBlock = block;
	}

	/** 
	 * Get this room's "onFocus" block. 
	 */
	public Block getFocusBlock()
	{
		return focusBlock;
	}
	
	/** 
	 * Set this room's "onFocus" block. 
	 */
	public void setFocusBlock(Block block)
	{
		focusBlock = block;
	}
	
	/** 
	 * Get this room's "onUnfocus" block. 
	 */
	public Block getUnfocusBlock()
	{
		return unfocusBlock;
	}
	
	/** 
	 * Set this room's "onUnfocus" block. 
	 */
	public void setUnfocusBlock(Block block)
	{
		unfocusBlock = block;
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
		sw.writeByte((byte)actionListFunction.ordinal());

		sw.writeInt(actionList.size());
		for (String actionIdentity : actionList)
			sw.writeString(actionIdentity, "UTF-8");
		
		modalActionTable.writeBytes(out);
		actionForbidTable.writeBytes(out);
		
		sw.writeBit(actionForbidBlock != null);
		sw.writeBit(focusBlock != null);
		sw.writeBit(unfocusBlock != null);
		sw.flushBits();

		if (actionForbidBlock != null)
			actionForbidBlock.writeBytes(out);
		if (focusBlock != null)
			focusBlock.writeBytes(out);
		if (unfocusBlock != null)
			unfocusBlock.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		actionListFunction = ActionSet.values()[sr.readByte()];
		
		actionList.clear();
		int size = sr.readInt();
		while (size-- > 0)
			actionList.put(sr.readString("UTF-8"));
	
		modalActionTable = ActionModeTable.create(in);
		actionForbidTable = ActionTable.create(in);
		
		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			actionForbidBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			focusBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			unfocusBlock = Block.create(in);
	}

}
