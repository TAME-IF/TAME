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

/**
 * Attached to classes that use "Action Unknown" blocks.
 * @author Matthew Tropiano
 */
public interface ActionUnknownHandler
{

	/** 
	 * Gets this element's "onUnknownAction" block. 
	 */
	public Block getUnknownActionBlock();

	/** 
	 * Sets this element's "onUnknownAction" block. 
	 */
	public void setUnknownActionBlock(Block block);	

}