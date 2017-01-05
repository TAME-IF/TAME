/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.exception;

import net.mtrop.tame.TAMEFatalException;

/**
 * Thrown when a command reads a value that has an unexpected type.
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
