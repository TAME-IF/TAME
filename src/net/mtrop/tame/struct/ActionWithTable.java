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

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.lang.command.Block;

import com.blackrook.commons.hash.HashMap;

/**
 * Holds action, object to CommandBlock mappings for modal action calls.
 * @author Matthew Tropiano
 */
public class ActionWithTable
{
	/** Action:Mode map. */
	private HashMap<TAction, HashMap<TObject, Block>> actionMap;
	
	/**
	 * Creates a new ActionModeTable.
	 */
	public ActionWithTable()
	{
		actionMap = null;
	}
	
	/**
	 * Checks if a command block by action exists.
	 */
	public boolean contains(TAction action, TObject object)
	{
		return get(action, object) != null;
	}

	/**
	 * Gets command block by action.
	 */
	public Block get(TAction action, TObject object)
	{
		if (actionMap == null) 
			return null;		
		
		HashMap<TObject, Block> objectHash = actionMap.get(action);
		
		if (objectHash == null) 
			return null;
		else
			return objectHash.get(object);
	}

	/**
	 * Sets/replaces command block by action.
	 */
	public void add(TAction action, TObject object, Block commandBlock)
	{
		if (actionMap == null)
			actionMap = new HashMap<TAction, HashMap<TObject, Block>>(3,3);
		
		HashMap<TObject, Block> hash = null;
		hash = actionMap.get(action);
		if (hash == null) 
		{
			hash = new HashMap<TObject, Block>(2);
			actionMap.put(action, hash);
		}
		hash.put(object, commandBlock);
	}
	
}
