/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
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
public class UnexpectedValueException extends TAMEFatalException
{
	private static final long serialVersionUID = 9084390457262102600L;

	public UnexpectedValueException()
	{
		super();
	}

	public UnexpectedValueException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnexpectedValueException(String message) 
	{
		super(message);
	}

	public UnexpectedValueException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public UnexpectedValueException(Throwable cause)
	{
		super(cause);
	}

}
