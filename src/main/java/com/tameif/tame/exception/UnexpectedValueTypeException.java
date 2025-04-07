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
 * Thrown when an operation reads a value that has an unexpected type (usually because it
 * was pre-checked at compile-time).
 * @author Matthew Tropiano
 */
public class UnexpectedValueTypeException extends TAMEFatalException
{
	private static final long serialVersionUID = 3977795940884710775L;

	public UnexpectedValueTypeException()
	{
		super();
	}

	public UnexpectedValueTypeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnexpectedValueTypeException(String message) 
	{
		super(message);
	}

	public UnexpectedValueTypeException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public UnexpectedValueTypeException(Throwable cause)
	{
		super(cause);
	}

}
