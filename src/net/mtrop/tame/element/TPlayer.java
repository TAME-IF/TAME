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
import net.mtrop.tame.lang.ActionSet;
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
{
	/** Set function for action list. */
	protected ActionSet actionListFunction;
	/** List of actions that are either restricted or excluded. */
	protected Hash<String> actionList;

	/** Table used for modal actions. */
	protected ActionModeTable modalActionTable;
	/** Blocks executed on action disallow. */
	protected ActionTable actionForbidTable;
	/** Blocks executed on action failure. */
	protected ActionTable actionFailTable;
	
	/** Code block ran upon default action disallow. */
	protected Block actionForbidBlock;
	/** Code block ran upon default action fail. */
	protected Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	protected Block actionAmbiguityBlock;
	/** Code block ran upon bad action. */
	protected Block badActionBlock;
	/** Code block ran upon focusing on this. */
	protected Block focusBlock;
	/** Code block ran upon focusing away from this. */
	protected Block unfocusBlock;

	/**
	 * Constructs an instance of a game world.
	 */
	public TPlayer()
	{
		super();
		setIdentity(TAMEConstants.IDENTITY_CURRENT_WORLD);
		
		this.actionListFunction = ActionSet.EXCLUDE;
		this.actionList = new Hash<String>(2);
		
		this.modalActionTable = new ActionModeTable();
		this.actionForbidTable = new ActionTable();
		this.actionFailTable = new ActionTable();
		
		this.actionForbidBlock = null;
		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
		this.badActionBlock = null;
		this.focusBlock = null;
		this.unfocusBlock = null;
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
	 * Gets a list of excluded/restricted actions, if any.
	 */
	public Hash<String> getActionList()
	{
		return actionList;
	}
	
	/**
	 * Adds an action to the action list to be excluded/restricted.
	 */
	public void addAction(TAction action)
	{
		actionList.put(action.getIdentity());
	}
	
	/**
	 * Returns if an action is allowed for this player.
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
	 * Get this player's action forbid table for specific actions. 
	 */
	public ActionTable getActionForbidTable()
	{
		return actionForbidTable;
	}

	/** 
	 * Get this player's action fail table. 
	 */
	public ActionTable getActionFailTable()
	{
		return actionFailTable;
	}

	/** 
	 * Get this player's "onBadAction" block. 
	 */
	public Block getBadActionBlock()
	{
		return badActionBlock;
	}
	
	/** 
	 * Set this player's "onBadAction" block. 
	 */
	public void setBadActionBlock(Block block)
	{
		badActionBlock = block;
	}

	/** 
	 * Get this player's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}
	
	/** 
	 * Set this player's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block block)
	{
		actionAmbiguityBlock = block;
	}

	/** 
	 * Get this player's default "onActionForbid" block. 
	 */
	public Block getActionForbidBlock()
	{
		return actionForbidBlock;
	}
	
	/** 
	 * Set this player's default "onActionForbid" block. 
	 */
	public void setActionForbidBlock(Block block)
	{
		actionForbidBlock = block;
	}

	/** 
	 * Get this player's default "onActionFail" block. 
	 */
	public Block getActionFailBlock()
	{
		return actionFailBlock;
	}
	
	/** 
	 * Set this player's default "onActionFail" block. 
	 */
	public void setActionFailBlock(Block block)
	{
		actionFailBlock = block;
	}

	/** 
	 * Get this player's "onFocus" block. 
	 */
	public Block getFocusBlock()
	{
		return focusBlock;
	}
	
	/** 
	 * Set this player's "onFocus" block. 
	 */
	public void setFocusBlock(Block block)
	{
		focusBlock = block;
	}
	
	/** 
	 * Get this player's "onUnfocus" block. 
	 */
	public Block getUnfocusBlock()
	{
		return unfocusBlock;
	}
	
	/** 
	 * Set this player's "onUnfocus" block. 
	 */
	public void setUnfocusBlock(Block block)
	{
		unfocusBlock = block;
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
		actionFailTable.writeBytes(out);

		sw.writeBit(actionForbidBlock != null);
		sw.writeBit(actionFailBlock != null);
		sw.writeBit(actionAmbiguityBlock != null);
		sw.writeBit(badActionBlock != null);
		sw.writeBit(focusBlock != null);
		sw.writeBit(unfocusBlock != null);
		sw.flushBits();
		
		if (actionForbidBlock != null)
			actionForbidBlock.writeBytes(out);
		if (actionFailBlock != null)
			actionFailBlock.writeBytes(out);
		if (actionAmbiguityBlock != null)
			actionAmbiguityBlock.writeBytes(out);
		if (badActionBlock != null)
			badActionBlock.writeBytes(out);
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
		actionFailTable = ActionTable.create(in);
		
		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			actionForbidBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			actionFailBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			actionAmbiguityBlock = Block.create(in);
		if ((blockbits & 0x08) != 0)
			badActionBlock = Block.create(in);
		if ((blockbits & 0x10) != 0)
			focusBlock = Block.create(in);
		if ((blockbits & 0x20) != 0)
			unfocusBlock = Block.create(in);
	}

}
