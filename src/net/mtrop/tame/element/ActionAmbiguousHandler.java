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
 * Attached to classes that use "Ambiguous Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionAmbiguousHandler
{
	/**
	 * Gets the ambiguous action table for specific action handlings.
	 */
	public ActionTable getAmbiguousActionTable();

	/** 
	 * Get this player's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock();

	/** 
	 * Set this player's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block block);

}
