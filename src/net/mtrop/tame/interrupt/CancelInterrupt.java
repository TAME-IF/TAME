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
package net.mtrop.tame.interrupt;

/**
 * Interrupt for cancelling out of a block.
 * @author Matthew Tropiano
 */
public class CancelInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -3803861797843170253L;

	public CancelInterrupt()
	{
		super("A command interrupt was thrown.");
	}
	
	public CancelInterrupt(String message)
	{
		super(message);
	}

}
