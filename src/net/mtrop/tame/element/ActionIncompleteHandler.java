/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Action Incomplete" blocks, 
 * called when a (di)transitive, modal, or open block does not have a target.
 * @author Matthew Tropiano
 */
public interface ActionIncompleteHandler
{
	/** 
	 * Get this element's action incomplete table for specific actions. 
	 */
	public ActionTable getActionIncompleteTable();

	/** 
	 * Gets this element's "onIncompleteAction" block. 
	 */
	public Block getActionIncompleteBlock();

	/** 
	 * Sets this element's "onIncompleteAction" block. 
	 */
	public void setActionIncompleteBlock(Block block);	

}
