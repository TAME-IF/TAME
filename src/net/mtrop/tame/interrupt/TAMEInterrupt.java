/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.interrupt;

/**
 * A special throwable that interrupts a code block execution.
 * @author Matthew Tropiano
 */
public class TAMEInterrupt extends Throwable
{
	private static final long serialVersionUID = -2283138977009468353L;

	public TAMEInterrupt()
	{
		super("A generic game interrupt was thrown.");
	}
	
	public TAMEInterrupt(String message)
	{
		super(message);
	}

}
