/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element;

import net.mtrop.tame.struct.ActionModeTable;

/**
 * Attached to classes that use "Modal Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionModalHandler
{
	/** 
	 * Gets the modal action table. 
	 */
	public ActionModeTable getModalActionTable();

}
