/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

/**
 * Enumeration of trace types in a command trace for debugging.
 * @author Matthew Tropiano
 */
public enum TraceType 
{
	/** Interpreter parsing. */
	INTERPRETER,
	/** Context changes. */
	CONTEXT,
	/** Entry point searches/calls. */
	ENTRY,
	/** Control/Branch changes. */
	CONTROL,
	/** Function calls. */
	FUNCTION,
	/** Internal function calls. */
	INTERNAL,
	/** Value changes. */
	VALUE;
	
	public static final TraceType[] VALUES = values();
	
}
