/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.interrupt;

import net.mtrop.tame.TAMEInterrupt;

/**
 * Throwable that is thrown by a "quit" statement.
 * @author Matthew Tropiano
 */
public class QuitInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -8027918516813618299L;

	public QuitInterrupt()
	{
		super("A quit interrupt was thrown.");
	}
	
	public QuitInterrupt(String message)
	{
		super(message);
	}
}
