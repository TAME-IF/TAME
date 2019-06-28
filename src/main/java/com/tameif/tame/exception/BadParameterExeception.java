/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.exception;

import com.tameif.tame.TAMEFatalException;

/**
 * Thrown when an operation deals with an author-made value that cannot be
 * implicitly handled.
 * @author Matthew Tropiano
 */
public class BadParameterExeception extends TAMEFatalException
{
	private static final long serialVersionUID = 3977795940884710775L;

	public BadParameterExeception()
	{
		super();
	}

	public BadParameterExeception(String message, Throwable cause)
	{
		super(message, cause);
	}

	public BadParameterExeception(String message) 
	{
		super(message);
	}

	public BadParameterExeception(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public BadParameterExeception(Throwable cause)
	{
		super(cause);
	}

}
