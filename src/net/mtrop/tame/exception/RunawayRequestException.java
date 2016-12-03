/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.exception;

/**
 * This type of interrupt is thrown too many commands are executed on
 * one request, in order to catch infinite loops. This will terminate a
 * request abruptly, so the context may be left in an undesirable state.
 * But then again, maybe you should fix those infinite loops!
 * @author Matthew Tropiano
 */
public class RunawayRequestException extends TAMEFatalException
{
	private static final long serialVersionUID = -4340182530454717686L;

	public RunawayRequestException()
	{
		super("A runaway request interrupt was thrown. Too many commands processed for one request: possible infinite loop.");
	}
	
	public RunawayRequestException(String message)
	{
		super(message);
	}
}
