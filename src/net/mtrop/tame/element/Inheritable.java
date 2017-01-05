/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.element;

/**
 * Objects that use this interface may have an implied lineage.
 * Some logic processes will trace through an object's lineage to find a valid
 * block or whatever.
 * @author Matthew Tropiano
 * @param <E> the parent type.
 */
public interface Inheritable<E>
{
	/**
	 * Gets this object's parent.
	 * The ordering is in the order they were added.
	 * @return an iterator for this object's lineage.
	 * @see #setParent(Object)
	 */
	public E getParent();
	
	/**
	 * Sets a parent on this object.
	 * @param parent the parent object to set.
	 */
	public void setParent(E parent);

}
