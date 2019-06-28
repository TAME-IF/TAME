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

/**
 * Thrown when a module state read/write exception occurs.
 * @author Matthew Tropiano
 */
public class ModuleStateException extends ModuleException
{
	private static final long serialVersionUID = 2838202334130697209L;

	public ModuleStateException()
	{
		super();
	}

	public ModuleStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleStateException(String message) 
	{
		super(message);
	}

	public ModuleStateException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleStateException(Throwable cause)
	{
		super(cause);
	}

}
