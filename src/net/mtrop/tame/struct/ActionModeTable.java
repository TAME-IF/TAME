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

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.world.TAction;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;

/**
 * Holds action, object to CommandBlock mappings for modal action calls.
 * @author Matthew Tropiano
 */
public class ActionModeTable
{
	/** Action to Mode map. */
	private HashMap<TAction, CaseInsensitiveHashMap<Block>> actionMap;
	
	/**
	 * Creates a new ActionModeTable.
	 */
	public ActionModeTable()
	{
		actionMap = null;
	}

	/**
	 * Checks if a command block by action exists.
	 */
	public boolean contains(TAction action, String mode)
	{
		return get(action, mode) != null;
	}

	/**
	 * Gets command block by action.
	 */
	public Block get(TAction action, String mode)
	{
		if (actionMap == null) 
			return null;		
		
		CaseInsensitiveHashMap<Block> modeHash = actionMap.get(action);
		
		if (modeHash == null) 
			return null;
		else
			return modeHash.get(mode);
	}

	/**
	 * Sets/replaces command block by action.
	 */
	public void add(TAction action, String mode, Block commandBlock)
	{
		if (actionMap == null)
			actionMap = new HashMap<TAction, CaseInsensitiveHashMap<Block>>(3,3);
		
		CaseInsensitiveHashMap<Block> hash = null;
		hash = actionMap.get(action);
		if (hash == null) 
		{
			hash = new CaseInsensitiveHashMap<Block>(2);
			actionMap.put(action, hash);
		}
		hash.put(mode, commandBlock);
	}

}
