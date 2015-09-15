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
package net.mtrop.tame.struct;

import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.world.TAction;

import com.blackrook.commons.list.SortedMap;

/**
 * Holds actionId -> CommandBlock mappings for general action calls.
 * @author Matthew Tropiano
 */
public class ActionTable 
{
	/** Action map. */
	private SortedMap<TAction, Block> actionMap;
	
	public ActionTable()
	{
		actionMap = null;
	}
	
	/**
	 * Checks if a command block by action exists.
	 */
	public boolean contains(TAction action)
	{
		return get(action) != null;
	}

	/**
	 * Gets command block by action.
	 */
	public Block get(TAction action)
	{
		if (actionMap == null) 
			return null;
		else
			return actionMap.get(action);
	}

	/**
	 * Sets/replaces command block by action.
	 */
	public void add(TAction action, Block commandBlock)
	{
		if (actionMap == null)
			actionMap = new SortedMap<TAction, Block>(3, 3);
		actionMap.add(action, commandBlock);
	}
	
}
