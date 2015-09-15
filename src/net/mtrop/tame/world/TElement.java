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

import com.blackrook.commons.Common;

import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Common engine element object.
 * @author Matthew Tropiano
 */
public abstract class TElement
{
	/** Element's primary identity. */
	private String identity;
	
	/** Code block ran upon world initialization. */
	private Block initBlock;

	/** Table of associated actions. */
	private ActionTable actionTable;
	
	/**
	 * Set on initial creation. This flag is checked to ensure all objects were defined.
	 * If this is set on any object after the time of compile, then an object was only
	 * prototyped, but not completely defined. It is set by default and must be cleared.
	 */
	private boolean prototyped;
	
	protected TElement(String identity)
	{
		if (Common.isEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		
		this.identity = identity;
		this.actionTable = new ActionTable();
		this.initBlock = new Block();
		this.prototyped = true;
	}

	/**
	 * Sets if this is prototyped.
	 * This is set on initial creation. This flag is checked to ensure all objects were defined.
	 * If this is set on any object after the time of compile, then an object was only
	 * prototyped, but not completely defined. It is set by default and must be cleared.
	 */
	public void setPrototyped(boolean prototyped)
	{
		this.prototyped = prototyped;
	}
	
	/** 
	 * Checks if the prototype flag is set. 
	 */
	public boolean isPrototyped()
	{
		return prototyped;
	}
	
	/** 
	 * Gets the identity (primary identifier name). 
	 */
	public String getIdentity()
	{
		return identity;
	}
	
	/** 
	 * Sets the identity (primary identifier name). 
	 */
	public void setIdentity(String identity)
	{
		this.identity = identity;
	}
	
	/** 
	 * Get this element's initialization block. 
	 */
	public Block getInitBlock()
	{
		return initBlock;
	}
	
	/** 
	 * Set this element's initialization block. 
	 */
	public void setInitBlock(Block eab)
	{
		initBlock = eab;
	}

	/**
	 * Gets the action table for "onAction" calls (general).
	 */
	public ActionTable getActionTable()
	{
		return actionTable;
	}
	
	@Override
	public int hashCode()
	{
		return identity.hashCode();
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object instanceof TElement)
			return equals((TElement)object);
		else 
			return super.equals(object);
	}
	
	/**
	 * Compares elements. Uses identity.
	 */
	public boolean equals(TElement object)
	{
		return getClass().equals(object.getClass()) && identity.equals(object.identity);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + getIdentity() + "]";
	}
	
}
