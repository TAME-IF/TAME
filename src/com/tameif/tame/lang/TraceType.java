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
	/** Value changes. */
	VALUE;
	
	final String cueSuffix;
	
	private TraceType()
	{
		this.cueSuffix = name().toLowerCase();
	}

	public String getCueSuffix() 
	{
		return cueSuffix;
	}
	
}
