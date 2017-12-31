/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.element.context;

import net.mtrop.tame.element.TPlayer;

/**
 * Player context.
 * @author Matthew Tropiano
 */
public class TPlayerContext extends TElementContext<TPlayer>
{
	/**
	 * Creates a player context. 
	 * @param player the player reference.
	 */
	public TPlayerContext(TPlayer player)
	{
		super(player);
	}

}
