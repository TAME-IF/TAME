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
package net.mtrop.tame.element;

import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TElement
{
	/** Blocks executed on action failure. */
	private ActionTable actionFailTable;

	/** Code block ran upon bad action. */
	private Block badActionBlock;
	/** Code block ran upon default action fail. */
	private Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	private Block actionAmbiguityBlock;
	
	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super("world");

		this.actionFailTable = new ActionTable();

		this.badActionBlock = null;
		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
	}

	/** 
	 * Get this module's action fail table. 
	 */
	public ActionTable getActionFailTable()
	{
		return actionFailTable;
	}

	/** 
	 * Get this module's "onBadAction" block. 
	 */
	public Block getBadActionBlock()
	{
		return badActionBlock;
	}

	/** 
	 * Set this module's "onBadAction" block. 
	 */
	public void setBadActionBlock(Block eab)	
	{
		badActionBlock = eab;
	}

	/** 
	 * Get this module's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}

	/** 
	 * Set this module's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block eab)
	{
		actionAmbiguityBlock = eab;
	}

	/** 
	 * Get this module's default "onActionFail" block. 
	 */
	public Block getActionFailBlock()
	{
		return actionFailBlock;
	}

	/** 
	 * Set this module's default "onActionFail" block. 
	 */
	public void setActionFailBlock(Block eab)
	{
		actionFailBlock = eab;
	}

}
