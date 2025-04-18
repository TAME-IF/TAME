/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.interrupt;

import com.tameif.tame.TAMEInterrupt;

/**
 * Interrupt for ending a block call or returning from a function.
 * @author Matthew Tropiano
 */
public class EndInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -3518666904445530889L;

	public EndInterrupt()
	{
		super("An end interrupt was thrown.");
	}
	
	public EndInterrupt(String message)
	{
		super(message);
	}

}
