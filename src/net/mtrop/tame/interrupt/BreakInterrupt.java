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
 * Throwable that is thrown by a "break" statement.
 * @author Matthew Tropiano
 */
public class BreakInterrupt extends CancelInterrupt
{
	private static final long serialVersionUID = -8027918516813618299L;

	public BreakInterrupt()
	{
		super("A break interrupt was thrown.");
	}
	
	public BreakInterrupt(String message)
	{
		super(message);
	}
}
