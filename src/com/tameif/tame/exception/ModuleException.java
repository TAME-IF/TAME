/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
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
 * Thrown when a generic module exception occurs.
 * @author Matthew Tropiano
 */
public class ModuleException extends TAMEFatalException
{
	private static final long serialVersionUID = 7860423798911413272L;

	public ModuleException()
	{
		super();
	}

	public ModuleException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleException(String message) 
	{
		super(message);
	}

	public ModuleException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleException(Throwable cause)
	{
		super(cause);
	}

}
