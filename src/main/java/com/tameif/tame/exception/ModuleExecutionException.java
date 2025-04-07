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

/**
 * Thrown if a problem happens during module execution.
 * @author Matthew Tropiano
 */
public class ModuleExecutionException extends ModuleException
{
	private static final long serialVersionUID = 8661622499394705573L;

	public ModuleExecutionException()
	{
		super();
	}

	public ModuleExecutionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleExecutionException(String message) 
	{
		super(message);
	}

	public ModuleExecutionException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleExecutionException(Throwable cause)
	{
		super(cause);
	}

}
