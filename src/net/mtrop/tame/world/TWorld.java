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

import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.struct.ActionTable;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TElement
{
	/** List of actions. */
	private CaseInsensitiveHashMap<TAction> actionList;
	/** Maps action common names to action objects. */
	private CaseInsensitiveHashMap<TAction> actionNameTable;
	/** Blocks executed on action failure. */
	private ActionTable actionFailTable;

	/** Code block ran upon bad action. */
	private Block badActionBlock;
	/** Code block ran upon default action fail. */
	private Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	private Block actionAmbiguityBlock;
	
	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super("world");

		this.actionList = new CaseInsensitiveHashMap<TAction>(20);
		this.actionNameTable = new CaseInsensitiveHashMap<TAction>(15);
		this.actionFailTable = new ActionTable();

		this.badActionBlock = null;
		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
	}

	/**
	 * Add an action to this world. World loaders will use this.
	 * @param a	the Action to add.
	 */
	public void addAction(TAction a)
	{
		actionList.put(a.getIdentity(), a);
		for (String s : a.getNames())
			actionNameTable.put(s, a);
	}

	/**
	 * Retrieves an Action's id by name, case-insensitively.
	 * @param name the Action's name.
	 * @return the corresponding Action, or null if not found.
	 */
	public TAction getActionByName(String name)
	{
		return actionNameTable.get(name);
	}

	/**
	 * Retrieves an Action by identity, case-insensitively.
	 * @param identity the Action's identity.
	 * @return the corresponding Action, or null if not found.
	 */
	public TAction getActionByIdentity(String identity)
	{
		return actionList.get(identity);
	}

	/**
	 * Gets the reference to the list of actions.
	 */
	public CaseInsensitiveHashMap<TAction> getActionList()
	{
		return actionList;
	}

	/** 
	 * Get this module's action fail table. 
	 */
	public ActionTable getActionFailTable()
	{
		return actionFailTable;
	}

	/** 
	 * Get this module's "onBadAction" block. 
	 */
	public Block getBadActionBlock()
	{
		return badActionBlock;
	}

	/** 
	 * Set this module's "onBadAction" block. 
	 */
	public void setBadActionBlock(Block eab)	
	{
		badActionBlock = eab;
	}

	/** 
	 * Get this module's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}

	/** 
	 * Set this module's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block eab)
	{
		actionAmbiguityBlock = eab;
	}

	/** 
	 * Get this module's default "onActionFail" block. 
	 */
	public Block getActionFailBlock()
	{
		return actionFailBlock;
	}

	/** 
	 * Set this module's default "onActionFail" block. 
	 */
	public void setActionFailBlock(Block eab)
	{
		actionFailBlock = eab;
	}

}
