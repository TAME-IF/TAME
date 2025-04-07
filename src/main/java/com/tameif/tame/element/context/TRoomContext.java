/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element.context;

import com.tameif.tame.element.TRoom;

/**
 * Room context.
 * @author Matthew Tropiano
 */
public class TRoomContext extends TElementContext<TRoom>
{
	/**
	 * Creates a room context. 
	 * @param room the room reference.
	 */
	public TRoomContext(TRoom room)
	{
		super(room);
	}

}
