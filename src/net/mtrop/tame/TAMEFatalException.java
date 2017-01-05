/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame;

/**
 * Thrown when an unexpected error occurs in TAME.
 * @author Matthew Tropiano
 */
public class TAMEFatalException extends RuntimeException
{
	private static final long serialVersionUID = -2158136537645075334L;

	public TAMEFatalException()
	{
		super();
	}

	public TAMEFatalException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public TAMEFatalException(String message) 
	{
		super(message);
	}

	public TAMEFatalException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public TAMEFatalException(Throwable cause)
	{
		super(cause);
	}

}
