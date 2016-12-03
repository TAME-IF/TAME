/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.exception;

import net.mtrop.tame.TAMEFatalException;

/**
 * Thrown when the arithmetic stack ends up in a way that should not be.
 * @author Matthew Tropiano
 */
public class ArithmeticStackStateException extends TAMEFatalException
{
	private static final long serialVersionUID = -776603083012446190L;

	public ArithmeticStackStateException()
	{
		super();
	}

	public ArithmeticStackStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ArithmeticStackStateException(String message) 
	{
		super(message);
	}

	public ArithmeticStackStateException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ArithmeticStackStateException(Throwable cause)
	{
		super(cause);
	}

}
