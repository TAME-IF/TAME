/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
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
 * Throwable that is thrown by a "continue" statement.
 * @author Matthew Tropiano
 */
public class ContinueInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -8027918516813618299L;

	public ContinueInterrupt()
	{
		super("A continue interrupt was thrown.");
	}
	
	public ContinueInterrupt(String message)
	{
		super(message);
	}
}
