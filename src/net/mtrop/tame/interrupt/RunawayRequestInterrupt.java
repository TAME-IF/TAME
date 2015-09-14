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
 * This type of interrupt is thrown too many commands are executed on
 * one request, in order to catch infinite loops. This will terminate a
 * request abruptly, so the context may be left in an undesirable state.
 * But then again, maybe you should fix those infinite loops!
 * @author Matthew Tropiano
 */
public class RunawayRequestInterrupt extends TAMEInterrupt
{
	private static final long serialVersionUID = -4340182530454717686L;

	public RunawayRequestInterrupt()
	{
		super("A runaway request interrupt was thrown.");
	}
	
	public RunawayRequestInterrupt(String message)
	{
		super(message);
	}
}
