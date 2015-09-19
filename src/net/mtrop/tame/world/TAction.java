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
import com.blackrook.commons.hash.CaseInsensitiveHash;

/**
 * This is a player-activated action.
 * @author Matthew Tropiano
 */
public class TAction implements Comparable<TAction>
{
	public static enum Type
	{
		/** Has no targets. */
		GENERAL,
		/** Has one target. */
		TRANSITIVE,
		/** Has up to two targets. */
		DITRANSITIVE,
		/** Has very specific targets. */
		MODAL,
		/** Has an open target. */
		OPEN;
	}

	/** Element's primary identity. */
	private String identity;

	/** 
	 * Action type. GENERAL, TRANSITIVE, DITRANSITIVE, MODAL, OPEN.
	 */
	private Type type;
	
	/** What is the group of names of this action? */
	private CaseInsensitiveHash names;
	
	/** 
	 * Addtional strings. 
	 * For DITRANSITIVE, this is the object separator conjunctions.
	 * For MODAL, these are the valid targets.
	 */
	private CaseInsensitiveHash extraStrings;
	
	/**
	 * Makes a blank action.
	 */
	public TAction(String identity)
	{
		if (Common.isEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		
		this.identity = identity;
		this.type = Type.GENERAL;
		this.names = new CaseInsensitiveHash();
		this.extraStrings = new CaseInsensitiveHash();
	}
	
	/** 
	 * Gets the identity (primary identifier name).
	 */
	public String getIdentity()
	{
		return identity;
	}
	
	/**
	 * Sets this action's identity.
	 */
	public void setIdentity(String identity)
	{
		this.identity = identity;
	}

	/**
	 * Gets the action type.
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Sets the action type.
	 */
	public void setType(Type value)
	{
		type = value;
	}

	/**
	 * Gets the names of this action.
	 */
	public CaseInsensitiveHash getNames()
	{
		return names;
	}

	/**
	 * Gets this action's extra strings.
	 */
	public CaseInsensitiveHash getExtraStrings()
	{
		return extraStrings;
	}
	
	@Override
	public int compareTo(TAction action)
	{
		return identity.compareTo(action.identity);
	}

	@Override
	public int hashCode()
	{
		return identity.hashCode();
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object instanceof TAction)
			return identity.equals(((TAction)object).identity);
		else 
			return super.equals(object);
	}
	
	/**
	 * Compares actions.
	 * Uses identity.
	 */
	public boolean equals(TAction object)
	{
		return identity.equals(object.identity);
	}

	@Override
	public String toString()
	{
		return "TAction ["+identity+"]";
	}
	
}
