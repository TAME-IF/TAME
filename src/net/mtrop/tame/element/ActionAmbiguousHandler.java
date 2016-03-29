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
 * Attached to classes that use "Ambiguous Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionAmbiguousHandler
{
	/**
	 * Gets the ambiguous action table for specific action handling.
	 * @return the action table that handles ambiguous actions.
	 */
	public ActionTable getAmbiguousActionTable();

	/** 
	 * Gets the default "onAmbiguousAction" block. 
	 * @return the block that handles non-specific ambiguous actions.
	 */
	public Block getAmbiguousActionBlock();

	/** 
	 * Sets the default "onAmbiguousAction" block.
	 * @param block the block to use for handling non-specific ambiguous actions. 
	 */
	public void setAmbiguousActionBlock(Block block);

}
