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
 * Interrupt for ending the current action.
 * @author Matthew Tropiano
 */
public class FinishInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -3803861797843170253L;

	public FinishInterrupt()
	{
		super("A finish interrupt was thrown.");
	}
	
	public FinishInterrupt(String message)
	{
		super(message);
	}

}
