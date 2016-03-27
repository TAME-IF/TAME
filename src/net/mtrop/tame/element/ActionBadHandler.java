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
 * Attached to classes that use "Bad Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionBadHandler
{
	/** 
	 * Get this element's bad action table. 
	 * @return the action table that handles specific bad actions.
	 */
	public ActionTable getBadActionTable();

	/** 
	 * Gets this element's "onBadAction" block. 
	 * @return the block that handles non-specific bad actions.
	 */
	public Block getBadActionBlock();

	/** 
	 * Sets this element's "onBadAction" block. 
	 * @param block the block that handles non-specific bad actions.
	 */
	public void setBadActionBlock(Block block);	

}
