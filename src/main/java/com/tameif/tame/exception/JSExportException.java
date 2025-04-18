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

import java.io.IOException;

/**
 * Thrown when a JS Exporter exception occurs.
 * @author Matthew Tropiano
 */
public class JSExportException extends IOException
{
	private static final long serialVersionUID = 1132950289635623481L;

	public JSExportException()
	{
		super();
	}

	public JSExportException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public JSExportException(String message) 
	{
		super(message);
	}

	public JSExportException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public JSExportException(Throwable cause)
	{
		super(cause);
	}

}
