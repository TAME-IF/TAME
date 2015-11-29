/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame.exception;

/**
 * Thrown when a module has unexpected structures.
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
