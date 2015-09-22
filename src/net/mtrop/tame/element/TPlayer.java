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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
 * The viewpoint inside an adventure game.
 * The viewpoint can travel to different rooms, changing view aspects.
 * @author Matthew Tropiano
 */
public class TPlayer extends TElement
{
	/** Set function for action list. */
	protected ActionSet actionListFunction;
	/** List of actions that are either restricted or excluded. */
	protected Hash<TAction> actionList;

	/** Table used for modal actions. */
	protected ActionModeTable modalActionTable;

	/** Blocks executed on action disallow. */
	protected ActionTable actionForbidTable;
	/** Code block ran upon default action disallow. */
	protected Block actionForbidBlock;

	/** Code block ran upon bad action. */
	protected Block badActionBlock;

	/** Blocks executed on action failure. */
	protected ActionTable actionFailTable;
	/** Code block ran upon default action fail. */
	protected Block actionFailBlock;

	/** Code block ran when an action is ambiguous. */
	protected Block actionAmbiguityBlock;

	/** Code block ran upon focusing on this. */
	protected Block focusBlock;
	/** Code block ran upon focusing away from this. */
	protected Block unfocusBlock;

	public TPlayer(String id)
	{
		super(id);
		this.actionListFunction = ActionSet.EXCLUDE;
		this.actionList = new Hash<TAction>(2);
		this.modalActionTable = new ActionModeTable();
		this.badActionBlock = new Block();
		this.actionForbidTable = new ActionTable();
		this.actionForbidBlock = new Block();
		this.actionFailTable = new ActionTable();
		this.actionFailBlock = new Block();
		this.actionAmbiguityBlock = new Block();
		this.focusBlock = new Block();
		this.unfocusBlock = new Block();
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
	public Hash<TAction> getActionList()
	{
		return actionList;
	}
	
	/**
	 * Returns if an action is allowed for this player.
	 */
	public boolean allowsAction(TAction action)
	{
		if (actionListFunction == ActionSet.EXCLUDE)
			return !actionList.contains(action);
		else
			return actionList.contains(action);
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
		super.writeBytes(out);
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		// TODO: Finish this.
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		super.readBytes(in);
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		// TODO: Finish this.
	}

	@Override
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	@Override
	public void fromBytes(byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
	}
	
}
