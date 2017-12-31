/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.interrupt;

import net.mtrop.tame.TAMEInterrupt;

/**
 * Throwable that is thrown by a "break" statement.
 * @author Matthew Tropiano
 */
public class BreakInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = 9182225306806559066L;

	public BreakInterrupt()
	{
		super("A break interrupt was thrown.");
	}
	
	public BreakInterrupt(String message)
	{
		super(message);
	}
}
