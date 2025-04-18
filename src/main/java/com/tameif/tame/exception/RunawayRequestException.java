/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.exception;

import com.tameif.tame.TAMEFatalException;

/**
 * This type of interrupt is thrown too many operations are executed on
 * one request (in order to catch possible infinite loops), or when the 
 * function call stack gets too deep (to prevent stack overflows).
 * @author Matthew Tropiano
 */
public class RunawayRequestException extends TAMEFatalException
{
	private static final long serialVersionUID = -4340182530454717686L;

	public RunawayRequestException()
	{
		super("A runaway request interrupt was thrown. Too many operations processed for one request: possible infinite loop.");
	}
	
	public RunawayRequestException(String message)
	{
		super(message);
	}
}
