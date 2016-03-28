/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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
	 * @return the table that handles specific incomplete actions. 
	 */
	public ActionTable getActionIncompleteTable();

	/** 
	 * Gets this element's "onIncompleteAction" block. 
	 * @return the block that handles non-specific incomplete actions. 
	 */
	public Block getActionIncompleteBlock();

	/** 
	 * Sets this element's "onIncompleteAction" block. 
	 * @param block the block that handles non-specific incomplete actions. 
	 */
	public void setActionIncompleteBlock(Block block);	

}
