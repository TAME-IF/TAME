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
package net.mtrop.tame.world;

import net.mtrop.tame.lang.ActionSet;
import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.struct.ActionModeTable;
import net.mtrop.tame.struct.ActionTable;

import com.blackrook.commons.hash.Hash;

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
	protected Hash<TAction> actionList;

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
	
	/** Creates a new Room instance. */
	public TRoom(String id)
	{
		super(id);
		actionListFunction = ActionSet.EXCLUDE;
		actionList = new Hash<TAction>(2);
		modalActionTable = new ActionModeTable();
		actionForbidTable = new ActionTable();
		actionForbidBlock = new Block();
		focusBlock = new Block();
		unfocusBlock = new Block();
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
	public void addActionToList(TAction action)
	{
		actionList.put(action);
	}
	
	/**
	 * Returns if an action is allowed for this room.
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

}
