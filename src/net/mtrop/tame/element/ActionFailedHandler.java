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
 * Attached to classes that use "Action Failed" blocks.
 * @author Matthew Tropiano
 */
public interface ActionFailedHandler
{

	/** 
	 * Get this element's action failure table. 
	 */
	public ActionTable getActionFailedTable();

	/** 
	 * Get this element's default "onFailedAction" block. 
	 */
	public Block getActionFailedBlock();

	/** 
	 * Set this element's default "onFailedAction" block. 
	 */
	public void setActionFailedBlock(Block block);

}
