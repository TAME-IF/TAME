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
package net.mtrop.tame.context;

import net.mtrop.tame.world.TObject;

import com.blackrook.commons.hash.Hash;

/**
 * Object context.
 * @author Matthew Tropiano
 */
public class TObjectContext extends TElementContext<TObject>
{
	/** Element's names. */
	protected Hash<String> currentObjectNames;
	
	/**
	 * Creates a player context. 
	 */
	public TObjectContext(TObject ref)
	{
		super(ref);
		currentObjectNames = new Hash<String>(2);
	}

	/** 
	 * Adds a name.
	 */
	public void addName(String name) 
	{
		currentObjectNames.put(name);
	}

	/** 
	 * Removes a name. 
	 */
	public void removeName(String name) 
	{
		currentObjectNames.remove(name);
	}

	/**
	 * Returns true if this contains a particular name.
	 */
	public boolean containsName(String name)
	{
		return currentObjectNames.contains(name);
	}

}
