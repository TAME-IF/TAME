/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.interrupt;

import com.tameif.tame.TAMEInterrupt;

/**
 * Throwable that is thrown by a "quit" statement.
 * @author Matthew Tropiano
 */
public class QuitInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -8310668453377717262L;

	public QuitInterrupt()
	{
		super("A quit interrupt was thrown.");
	}
	
	public QuitInterrupt(String message)
	{
		super(message);
	}
}
